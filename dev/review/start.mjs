#!/usr/bin/env node
// Augment only the existing, isolated local review environment. Never removes its data.
import { execFileSync } from "node:child_process";
import { setTimeout as delay } from "node:timers/promises";

const app = "swarmpit-react-review-app";
const dind = "swarmpit-react-review-dind";
const db = "swarmpit-react-review-db";
const influx = "swarmpit-react-review-influx";
const network = "swarmpit-react-review";
const agent = "review-metrics-agent";
const registry = "review-demo-registry";
const base = "http://127.0.0.1:58019";
function docker(args, input) {
  return execFileSync("docker", args, { encoding: "utf8", input, timeout: 120000, stdio: ["pipe", "pipe", "pipe"] }).trim();
}
const nested = (...args) => docker(["exec", dind, "docker", ...args]);
const inspect = (name) => JSON.parse(docker(["inspect", name]))[0];
function exists(name) {
  return docker(["container", "ls", "-a", "--filter", "name=^/" + name + "$", "--format", "{{.Names}}"]) === name;
}
function check(ok, message) { if (!ok) throw new Error(message); }
async function ready() {
  for (let attempt = 0; attempt < 45; attempt++) {
    try {
      const response = await fetch(base + "/version", { signal: AbortSignal.timeout(2000) });
      const version = await response.json();
      if (response.ok && version.initialized && version.statistics) return;
    } catch {}
    await delay(1000);
  }
  throw new Error("Review app is not ready with initialized account and statistics.");
}

try {
  const current = inspect(app);
  check(current.Config.Labels?.["swarmpit.react.review"] === "true", "Expected labelled local review app.");
  const port = current.HostConfig.PortBindings?.["8080/tcp"];
  check(port?.length === 1 && port[0].HostIp === "127.0.0.1" && port[0].HostPort === "58019", "Review app must use loopback port 58019.");
  const env = current.Config.Env || [];
  check(env.includes("DOCKER_HOST=tcp://" + dind + ":2375") && env.includes("SWARMPIT_DB=http://" + db + ":5984"), "Unexpected review database or Docker endpoint.");
  const original = { [db]: inspect(db).Id, [dind]: inspect(dind).Id };
  const daemon = inspect(dind);
  check(daemon.HostConfig.Privileged && !daemon.Mounts.some((mount) => mount.Destination === "/var/run/docker.sock"), "Expected isolated Docker-in-Docker, without a mounted host socket.");
  check(JSON.parse(nested("info", "--format", "{{json .Swarm}}")).LocalNodeState === "active", "Nested daemon must already be an active review Swarm.");
  if (!exists(influx)) {
    docker(["run", "-d", "--name", influx, "--label", "swarmpit.react.review=true", "--network", network,
      "--memory", "256m", "--cpus", "0.25", "-e", "INFLUXDB_REPORTING_DISABLED=true",
      "-v", influx + ":/var/lib/influxdb", "influxdb:1.8"]);
  } else {
    const state = inspect(influx);
    check(state.Config.Labels?.["swarmpit.react.review"] === "true" && state.NetworkSettings.Networks[network], "Existing InfluxDB is not the review instance.");
    if (!state.State.Running) docker(["start", influx]);
  }
  console.log("Review InfluxDB is running; existing database and Swarm are preserved.");
  const additions = ["SWARMPIT_INFLUXDB=http://" + influx + ":8086", "SWARMPIT_AGENT_URL=http://" + dind + ":58020"];
  const wantedImage = JSON.parse(docker(["image", "inspect", current.Config.Image]))[0].Id;
  if (!additions.every((value) => env.includes(value)) || current.Image !== wantedImage) {
    const backup = app + "-before-review-data-" + Date.now();
    docker(["stop", app]);
    docker(["rename", app, backup]);
    try {
      const names = new Set(additions.map((value) => value.split("=")[0]));
      const keep = env.filter((value) => !names.has(value.split("=")[0]));
      docker(["run", "-d", "--name", app, "--label", "swarmpit.react.review=true",
        "--network", network, "-p", "127.0.0.1:58019:8080",
        ...keep.concat(additions).flatMap((value) => ["-e", value]), current.Config.Image]);
      await ready();
      docker(["rm", backup]);
    } catch (error) {
      if (exists(app)) docker(["rm", "-f", app]);
      docker(["rename", backup, app]);
      docker(["start", app]);
      throw error;
    }
  } else {
    if (!current.State.Running) docker(["start", app]);
    await ready();
  }
  const address = inspect(app).NetworkSettings.Networks[network].IPAddress;
  const serviceNames = nested("service", "ls", "--format", "{{.Name}}").split("\n");
  if (!serviceNames.includes(agent)) {
    nested("service", "create", "--name", agent, "--mode", "global",
      "--label", "swarmpit.agent=true", "--label", "swarmpit.react.review=true",
      "--host", app + ":" + address, "--env", "DOCKER_API_VERSION=1.44",
      "--env", "EVENT_ENDPOINT=http://" + app + ":8080/events",
      "--env", "HEALTH_CHECK_ENDPOINT=http://" + app + ":8080/version",
      "--env", "STATS_FREQUENCY=5",
      "--mount", "type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock,readonly",
      "--publish", "published=58020,target=8080,mode=host",
      "--limit-cpu", "0.10", "--limit-memory", "96M", "--detach", "swarmpit/agent:latest");
  } else {
    const service = JSON.parse(nested("service", "inspect", agent))[0];
    check(service.Spec.Labels?.["swarmpit.react.review"] === "true", "Existing agent is not review-owned.");
    const mapping = address + " " + app;
    const old = service.Spec.TaskTemplate.ContainerSpec.Hosts || [];
    if (!old.includes(mapping)) {
      nested("service", "update", "--detach", ...old.filter((value) => value.endsWith(" " + app)).flatMap(() => ["--host-rm", app]), "--host-add", app + ":" + address, agent);
    }
  }
  if (!serviceNames.includes(registry)) {
    nested("service", "create", "--name", registry, "--label", "swarmpit.react.review=true",
      "--publish", "published=5000,target=5000,mode=host",
      "--mount", "type=volume,source=review-demo-registry-data,target=/var/lib/registry",
      "--limit-cpu", "0.10", "--limit-memory", "96M", "--detach", "registry:2");
  } else {
    check(JSON.parse(nested("service", "inspect", registry))[0].Spec.Labels?.["swarmpit.react.review"] === "true", "Existing registry is not review-owned.");
  }
  for (const [name, id] of Object.entries(original)) check(inspect(name).Id === id, "Review data container changed unexpectedly.");
  console.log("Agent collects real metrics every 5 seconds. Registry stays inside the isolated Docker network.");
  console.log("Ready: " + base + " — refresh the browser to load the statistics capability.");
} catch (error) {
  // Docker errors can echo arguments containing environment values; only show explicit safe errors.
  console.error(error?.status !== undefined ? "Review setup Docker command failed; existing data volumes were preserved." : error.message);
  process.exitCode = 1;
}
