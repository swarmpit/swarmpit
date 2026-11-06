import assert from 'node:assert/strict';
import { test } from 'node:test';
import { validateOrigin, validateApp, reviewCompose, hasMetricSamples } from './seed.mjs';

test('seed refuses origins and Docker apps outside the isolated review target', () => {
  assert.equal(validateOrigin('http://127.0.0.1:58019').origin, 'http://127.0.0.1:58019');
  for (const url of ['http://localhost:58019', 'http://127.0.0.1:8080', 'https://127.0.0.1:58019', 'http://127.0.0.1:58019/api', 'http://user:pass@127.0.0.1:58019', 'http://127.0.0.1:58019/?x=1']) {
    assert.throws(() => validateOrigin(url));
  }
  const app = {
    Name: '/swarmpit-react-review-app',
    State: { Running: true },
    Config: { Labels: { 'swarmpit.react.review': 'true' }, Env: ['DOCKER_HOST=tcp://swarmpit-react-review-dind:2375'] },
    NetworkSettings: { Ports: { '8080/tcp': [{ HostIp: '127.0.0.1', HostPort: '58019' }] } },
  };
  assert.doesNotThrow(() => validateApp(app));
  for (const edit of [
    (a) => { a.Config.Labels = {}; },
    (a) => { a.Config.Env = ['DOCKER_HOST=unix:///var/run/docker.sock']; },
    (a) => { a.NetworkSettings.Ports['8080/tcp'][0].HostIp = '0.0.0.0'; },
    (a) => { a.Name = '/production'; },
    (a) => { a.State.Running = false; },
  ]) {
    const other = structuredClone(app);
    edit(other);
    assert.throws(() => validateApp(other));
  }
});

test('metric verification requires two real finite samples, not empty time buckets', () => {
  assert.equal(hasMetricSamples({ time: ['a', 'b'], cpu: [null, null] }, 'cpu'), false);
  assert.equal(hasMetricSamples({ time: ['a', 'b'], cpu: [0.1, null] }, 'cpu'), false);
  assert.equal(hasMetricSamples({ time: ['a', 'b'], cpu: [0, 0.1] }, 'cpu'), true);
  assert.equal(hasMetricSamples({ time: ['a', 'b'], memory: [16, 16] }, 'memory'), true);
});

test('compose preserves source revisions and bounded dummy workloads', () => {
  const first = reviewCompose(1);
  const second = reviewCompose(2);
  assert.notEqual(first, second);
  assert.match(first, /REVIEW_REVISION: "1"/);
  assert.match(second, /REVIEW_REVISION: "2"/);
  assert.match(first, /replicas: 2/);
  assert.match(first, /type: tmpfs/);
  assert.match(first, /review-demo-secret:\n    external: true/);
  assert.match(first, /review-demo-config:\n    external: true/);
  assert.doesNotMatch(first, /published:|privileged:|\/var\/run\/docker.sock/);
});
