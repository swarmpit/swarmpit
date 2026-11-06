#!/usr/bin/env node
import { execFile } from 'node:child_process';
import { readFile, stat } from 'node:fs/promises';
import { randomBytes } from 'node:crypto';
import { promisify } from 'node:util';
import { pathToFileURL } from 'node:url';

const docker = promisify(execFile);
const appName = 'swarmpit-react-review-app';
const daemonName = 'swarmpit-react-review-dind';
const fixture = 'review-demo';
const configText = 'worker_processes 1;\nevents { worker_connections 64; }\nhttp { access_log /dev/stdout; error_log /dev/stderr; server { listen 80; location / { default_type text/plain; return 200 "Swarmpit review demo\\n"; } } }\n';
const secretText = 'Public dummy review value; never a real credential.';

export function validateOrigin(value) {
  const url = new URL(value);
  if (url.origin !== 'http://127.0.0.1:58019' || url.pathname !== '/' || url.username || url.password || url.search || url.hash)
    throw new Error('Review URL must be exactly http://127.0.0.1:58019');
  return url;
}

export function validateApp(app) {
  const ports = Object.values(app.NetworkSettings?.Ports || {}).flat().filter(Boolean);
  if (app.Name !== '/' + appName || !app.State?.Running ||
      app.Config?.Labels?.['swarmpit.react.review'] !== 'true' ||
      !app.Config?.Env?.includes('DOCKER_HOST=tcp://' + daemonName + ':2375') ||
      !ports.some((p) => p.HostIp === '127.0.0.1' && p.HostPort === '58019') ||
      ports.some((p) => p.HostPort === '58019' && p.HostIp !== '127.0.0.1'))
    throw new Error('Review app isolation guard failed');
}

export function hasMetricSamples(series, metric) {
  return series?.time?.length >= 2 && (series[metric] || []).filter(Number.isFinite).length >= 2;
}

export function reviewCompose(revision) {
  return `# swarmpit-review-seed:v1 — public dummy review resources
version: "3.8"
services:
  web:
    image: nginx:alpine
    environment:
      REVIEW_REVISION: "${revision}"
      REVIEW_MESSAGE: "Hello from the review stack"
    labels:
      swarmpit.react.review: "true"
      review.component: "web"
    networks: [demo]
    configs:
      - source: review-demo-config
        target: /etc/nginx/nginx.conf
    secrets: [review-demo-secret]
    healthcheck:
      test: ["CMD", "wget", "-q", "-O", "/dev/null", "http://127.0.0.1/"]
      interval: 10s
      timeout: 3s
      retries: 3
    deploy:
      replicas: 2
      labels:
        swarmpit.react.review: "true"
        review.fixture: "review-demo"
      resources:
        limits: { cpus: "0.10", memory: 48M }
      update_config: { parallelism: 1, order: stop-first }
  cache:
    image: redis:alpine
    command: ["redis-server", "--appendonly", "yes", "--maxmemory", "16mb", "--maxmemory-policy", "allkeys-lru"]
    environment: { REVIEW_REVISION: "${revision}" }
    labels: { swarmpit.react.review: "true", review.component: "cache" }
    networks: [demo]
    volumes: [cache-data:/data]
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3
    deploy:
      replicas: 1
      labels: { swarmpit.react.review: "true", review.fixture: "review-demo" }
      resources:
        limits: { cpus: "0.10", memory: 64M }
  worker:
    image: alpine:3.20
    entrypoint: ["/bin/sh", "-c"]
    command:
      - |
        dd if=/dev/zero of=/scratch/load bs=1M count=16 2>/dev/null
        while true; do
          echo "review-demo heartbeat $$(date -u +%FT%TZ)"
          wget -q -O /dev/null http://web/ || true
          end=$$(($$(date +%s) + 1))
          while [ "$$(date +%s)" -lt "$$end" ]; do sha256sum /scratch/load >/dev/null; done
          sleep 9
        done
    environment: { REVIEW_REVISION: "${revision}" }
    labels: { swarmpit.react.review: "true", review.component: "worker" }
    networks: [demo]
    volumes:
      - type: tmpfs
        target: /scratch
        tmpfs: { size: 25165824 }
    healthcheck:
      test: ["CMD-SHELL", "test -s /scratch/load"]
      interval: 10s
      timeout: 3s
      retries: 3
    deploy:
      replicas: 1
      labels: { swarmpit.react.review: "true", review.fixture: "review-demo" }
      resources:
        limits: { cpus: "0.15", memory: 64M }
networks:
  demo:
    driver: overlay
    attachable: true
    labels: { swarmpit.react.review: "true" }
volumes:
  cache-data:
    labels: { swarmpit.react.review: "true" }
configs:
  review-demo-config:
    external: true
secrets:
  review-demo-secret:
    external: true
`;
}

async function dockerText(args) {
  try {
    return (await docker('docker', args, { timeout: 20000, maxBuffer: 2 * 1024 * 1024 })).stdout.trim();
  } catch {
    // Docker inspect can contain credentials: never reflect stdout/stderr or arguments.
    throw new Error('Review Docker operation failed');
  }
}

async function run() {
  const verifyOnly = process.argv.includes('--verify');
  if (process.argv.slice(2).some((x) => x !== '--verify'))
    throw new Error('Usage: node dev/review/seed.mjs [--verify]');
  const base = validateOrigin(process.env.SWARMPIT_REVIEW_URL || 'http://127.0.0.1:58019');
  validateApp(JSON.parse(await dockerText(['inspect', appName]))[0]);
  const nodeIds = (await dockerText(['exec', daemonName, 'docker', 'node', 'ls', '--format', '{{.ID}}'])).split(/\s+/).filter(Boolean);
  if (nodeIds.length !== 1) throw new Error('Review daemon must contain exactly one isolated Swarm node');
  const credentialPath = process.env.SWARMPIT_REVIEW_CREDENTIALS_FILE;
  if (!credentialPath) throw new Error('Set SWARMPIT_REVIEW_CREDENTIALS_FILE to a private username:password file');
  const file = await stat(credentialPath);
  if (!file.isFile() || (file.mode & 0o077)) throw new Error('Credential file must be a regular file readable only by its owner (mode 600)');
  const credentials = (await readFile(credentialPath, 'utf8')).replace(/\r?\n$/, '');
  const colon = credentials.indexOf(':');
  if (colon < 1 || colon === credentials.length - 1 || /[\r\n]/.test(credentials))
    throw new Error('Credential file must contain one username:password line');

  let token;
  let phase = 'authentication';
  async function request(path, { method = 'GET', body, authorization = token } = {}) {
    const headers = { Accept: 'application/json' };
    if (authorization) headers.Authorization = authorization;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    let response;
    try {
      response = await fetch(new URL(path, base), {
        method, headers, redirect: 'error', signal: AbortSignal.timeout(45000),
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      throw new Error(phase + ': request failed or timed out');
    }
    if (!response.ok) throw new Error(phase + ': ' + method + ' ' + path.split('?')[0] + ' returned HTTP ' + response.status);
    const text = await response.text();
    if (!text) return undefined;
    try { return JSON.parse(text); } catch { throw new Error(phase + ': invalid JSON response'); }
  }
  const post = (path, body) => request(path, { method: 'POST', body });
  async function poll(label, get, matches, timeout = 120000) {
    const until = Date.now() + timeout;
    do {
      const value = await get();
      if (matches(value)) return value;
      await new Promise((resolve) => setTimeout(resolve, 2000));
    } while (Date.now() < until);
    throw new Error(phase + ': timed out waiting for ' + label);
  }
  try {
    const version = await request('/version');
    if (version.initialized !== true) throw new Error('Review app must already be initialized; this script never replaces the admin');
    token = (await request('/login', { method: 'POST', authorization: 'Basic ' + Buffer.from(credentials).toString('base64') })).token;
    if (typeof token !== 'string' || !token.startsWith('Bearer ')) throw new Error('Login returned no complete Bearer authorization value');
    const me = await request('/api/me');
    if (me.role !== 'admin') throw new Error('Review seeding requires the existing admin');
    const nodes = await request('/api/nodes');
    if (nodes.length !== nodeIds.length || nodes.some((n) => !nodeIds.includes(n.id)))
      throw new Error('Refusing mutations: app node IDs do not match the isolated review daemon');
    console.log('Verified isolated app, daemon and existing admin');

    if (!verifyOnly) {
      phase = 'dummy config and secret';
      const configs = await request('/api/configs');
      const config = configs.find((c) => c.configName === fixture + '-config');
      if (config) {
        const detail = await request('/api/configs/' + encodeURIComponent(config.id));
        if (detail.data !== Buffer.from(configText).toString('base64')) throw new Error('Review config name is occupied by different content');
      } else await post('/api/configs', { configName: fixture + '-config', data: Buffer.from(configText).toString('base64') });
      const secrets = await request('/api/secrets');
      if (!secrets.some((s) => s.secretName === fixture + '-secret'))
        await post('/api/secrets', { secretName: fixture + '-secret', data: Buffer.from(secretText).toString('base64') });

      phase = 'stack and deployment history';
      const stacks = await request('/api/stacks');
      const existing = stacks.find((s) => s.stackName === fixture);
      let stored;
      if (existing) {
        stored = await request('/api/stacks/' + fixture + '/file');
        if (![reviewCompose(1), reviewCompose(2)].includes(stored.spec?.compose))
          throw new Error('Review stack has different stored compose; refusing to overwrite edits');
      } else {
        await post('/api/stacks?comment=review-demo-initial', { name: fixture, spec: { compose: reviewCompose(1) }, skipImageResolve: true });
      }
      await poll('four healthy review service tasks', () => request('/api/stacks/' + fixture + '/services'),
        (rows) => rows.length === 3 && rows.every((s) => (s.status?.tasks?.running || 0) >= (s.serviceName.endsWith('_web') ? 2 : 1)));
      if (stored?.spec?.compose !== reviewCompose(2))
        await post('/api/stacks/' + fixture + '?comment=review-demo-revision-2', { name: fixture, spec: { compose: reviewCompose(2) }, skipImageResolve: true });
      if (!stacks.some((s) => s.stackName === fixture + '-saved'))
        await post('/api/stacks/' + fixture + '-saved/file', {
          name: fixture + '-saved',
          spec: { compose: '# swarmpit-review-seed:v1 — inactive example\nversion: "3.8"\nservices:\n  example:\n    image: alpine:3.20\n    command: ["sleep", "3600"]\n    deploy:\n      replicas: 0\n' },
          skipImageResolve: true,
        });

      phase = 'review accounts';
      const users = await request('/api/admin/users');
      for (const role of ['user', 'viewer']) {
        const username = fixture + '-' + role;
        if (!users.some((u) => u.username === username))
          await post('/api/admin/users', { username, password: randomBytes(32).toString('base64url'), email: username + '@example.invalid', role });
      }

      phase = 'local registry';
      const registryUrl = 'http://' + daemonName + ':5000';
      const registries = await request('/api/registry/v2');
      if (!registries.some((r) => r.url === registryUrl))
        await post('/api/registry/v2', { name: fixture + '-registry', url: registryUrl, public: true, withAuth: false, customApi: false });

      phase = 'dashboard pins';
      const services = await request('/api/stacks/' + fixture + '/services');
      if (!(me['node-dashboard'] || []).includes(nodes[0].id))
        await post('/api/nodes/' + encodeURIComponent(nodes[0].id) + '/dashboard');
      for (const service of services.filter((s) => /_(web|cache)$/.test(s.serviceName))) {
        if (!(me['service-dashboard'] || []).includes(service.id))
          await post('/api/services/' + encodeURIComponent(service.id) + '/dashboard');
      }
      console.log('Review fixtures ready: stack/history, saved compose, config, secret, users, registry and pins');
    }

    if (!verifyOnly) {
      phase = 'real history aggregation';
      await poll('two real node history samples', () => request('/api/nodes/ts'),
        (rows) => rows.some((r) => hasMetricSamples(r, 'cpu') && hasMetricSamples(r, 'memory')));
      const selected = await request('/api/services/ts/cpu');
      if (!selected.some((row) => row.service?.startsWith(fixture + '_') && hasMetricSamples(row, 'cpu'))) {
        const influxName = 'swarmpit-react-review-influx';
        const influx = JSON.parse(await dockerText(['inspect', influxName]))[0];
        if (influx.Config?.Labels?.['swarmpit.react.review'] !== 'true' ||
            !influx.NetworkSettings?.Networks?.['swarmpit-react-review'])
          throw new Error('Refusing to aggregate history outside the review InfluxDB');
        // Run the existing continuous-query formulas over collected samples, without inventing history.
        const queries = [
          'SELECT MAX(cpuUsage)/100 AS cpu, MAX(memoryUsed) AS memory INTO a_day.downsampled_tasks FROM an_hour.task_stats WHERE time > now() - 15m GROUP BY time(1m), task, service',
          'SELECT MAX(cpuUsage) AS cpu, MAX(memoryUsage) AS memory INTO a_day.downsampled_hosts FROM an_hour.host_stats WHERE time > now() - 15m GROUP BY time(1m), host',
          'SELECT SUM(cpu) AS cpu, SUM(memory) AS memory INTO a_day.downsampled_services FROM a_day.downsampled_tasks WHERE time > now() - 15m GROUP BY time(1m), service',
          'SELECT MAX(cpu) AS max_cpu, MAX(memory) AS max_memory INTO a_day.downsampled_max_usage_services FROM a_day.downsampled_services WHERE time > now() - 15m GROUP BY time(30m), service',
        ];
        for (const query of queries)
          await dockerText(['exec', influxName, 'influx', '-database', 'swarmpit', '-execute', query]);
        // Clear the backend's 30-minute empty service ranking memo; keep all volumes and node IDs.
        await dockerText(['restart', appName]);
        await poll('review app after cache refresh', async () => {
          try { return await request('/version'); } catch { return undefined; }
        }, (value) => value?.initialized && value?.statistics, 60000);
        console.log('Aggregated real history and refreshed only the review app cache');
      }
    }

    phase = 'verification';
    const services = await poll('running revision 2 services', () => request('/api/stacks/' + fixture + '/services'),
      (rows) => rows.length === 3 && rows.every((s) => (s.status?.tasks?.running || 0) >= (s.serviceName.endsWith('_web') ? 2 : 1) && s.status?.update !== 'updating'));
    const file = await request('/api/stacks/' + fixture + '/file');
    if (file.spec?.compose !== reviewCompose(2)) throw new Error('Stored compose does not match review revision 2');
    const history = await request('/api/stacks/' + fixture + '/history');
    if (!history.some((h) => h.comment === 'review-demo-initial') || !history.some((h) => h.comment === 'review-demo-revision-2'))
      throw new Error('Review deployment history is incomplete');
    const worker = services.find((s) => s.serviceName.endsWith('_worker'));
    const logs = await poll('worker heartbeat logs', () => request('/api/services/' + encodeURIComponent(worker.id) + '/logs?since=15m'),
      (rows) => rows.some((l) => l.line?.includes('review-demo heartbeat')));
    await poll('real node metric samples', () => request('/api/nodes/ts'), (rows) => rows.some((r) => hasMetricSamples(r, 'cpu') && hasMetricSamples(r, 'memory')));
    for (const metric of ['cpu', 'memory']) {
      await poll('real service ' + metric + ' samples', () => request('/api/services/ts/' + metric),
        (rows) => rows.some((r) => {
          return r.service?.startsWith(fixture + '_') && hasMetricSamples(r, metric);
        }));
    }
    const summary = await request('/api/stats');
    if (!summary || !summary.resources?.[nodes[0].id] || !Number.isFinite(summary.cpu?.usage))
      throw new Error('Cluster statistics are not ready');
    console.log('Verified: 3 stack services / 4 running replicas; ' + history.length + ' history entries; ' + logs.length + ' worker log lines; real node and service metric samples');
  } finally {
    if (token) {
      phase = 'session logout';
      await post('/logout');
    }
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  run().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
