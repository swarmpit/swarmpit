export interface Pair {
  name: string;
  value: string;
}
export interface Version {
  version: string;
  revision: string;
  initialized: boolean;
  statistics: boolean;
  instanceName?: string;
  docker: { api: number; engine: string };
}
export interface Me {
  username: string;
  email: string;
  role: "admin" | "user" | "viewer";
  "api-token"?: { jti: string; mask: string };
  "node-dashboard"?: string[];
  "service-dashboard"?: string[];
}
export interface User extends Me {
  _id: string;
  _rev: string;
  type: string;
}
export interface Resources {
  cpu: number;
  memory: number;
}
export interface Deploy {
  parallelism: number;
  delay: number;
  order: string;
  failureAction: string;
}
export interface Healthcheck {
  test?: string[];
  interval?: number;
  timeout?: number;
  startPeriod?: number;
  retries?: number;
}
export interface Service {
  id: string;
  version: number;
  serviceName: string;
  createdAt?: string;
  updatedAt?: string;
  repository: {
    name: string;
    tag: string;
    image?: string;
    imageDigest?: string;
  };
  mode: string;
  replicas?: number;
  state?: string;
  stack?: string;
  agent?: boolean;
  immutable?: boolean;
  status?: {
    tasks?: { running: number; total: number };
    update?: string;
    message?: string;
  };
  ports: {
    containerPort: number;
    hostPort: number;
    protocol: string;
    mode: string;
  }[];
  mounts: {
    containerPath: string;
    host?: string;
    type: string;
    readOnly: boolean;
    volumeOptions?: {
      labels?: Record<string, string>;
      driver?: { name: string; options?: Pair[] };
    };
  }[];
  networks: { id?: string; networkName: string; serviceAliases?: string[] }[];
  secrets: {
    id?: string;
    secretName: string;
    secretTarget: string;
    uid?: number;
    gid?: number;
    mode?: number;
  }[];
  configs: {
    id?: string;
    configName: string;
    configTarget: string;
    uid?: number;
    gid?: number;
    mode?: number;
  }[];
  hosts: Pair[];
  variables: Pair[];
  labels: Pair[];
  containerLabels?: Pair[];
  sysctls?: Pair[];
  links?: Pair[];
  command?: string[];
  entrypoint?: string[];
  hostname?: string;
  isolation?: string;
  user?: string;
  dir?: string;
  tty?: boolean;
  healthcheck?: Healthcheck;
  logdriver: { name: string; opts: Pair[] };
  resources: { reservation: Resources; limit: Resources };
  deployment: {
    update: Deploy;
    rollback: Deploy;
    restartPolicy: {
      condition: string;
      delay: number;
      window?: number;
      attempts: number;
    };
    autoredeploy: boolean;
    rollbackAllowed?: boolean;
    maxReplicas?: number | null;
    placement: { rule: string }[];
    forceUpdate?: number;
  };
}
export interface NodeStats {
  cpu?: { cores?: number; usedPercentage?: number };
  memory?: { used?: number; total?: number; usedPercentage?: number };
  disk?: { used?: number; total?: number; usedPercentage?: number };
}
export interface TaskStats {
  cpu?: number;
  cpuLimit?: number;
  cpuPercentage?: number;
  memory?: number;
  memoryLimit?: number;
  memoryPercentage?: number;
}
export interface Node {
  stats?: NodeStats | null;
  plugins?: { networks?: string[]; volumes?: string[] };
  id: string;
  version: number;
  nodeName: string;
  role: string;
  availability: string;
  state: string;
  labels: Pair[];
  address: string;
  engine: string;
  arch: string;
  os: string;
  leader: boolean;
  resources: Resources;
}
export interface Task {
  stats?: TaskStats | null;
  logdriver?: string;
  id: string;
  taskName: string;
  serviceName: string;
  nodeId: string;
  nodeName: string;
  state: string;
  desiredState: string;
  createdAt?: string;
  updatedAt?: string;
  repository?: { image: string; imageDigest?: string };
  status?: { error?: string };
  resources?: { reservation: Resources; limit: Resources };
}
export interface Network {
  id: string;
  networkName: string;
  driver: string;
  scope?: string;
  internal: boolean;
  attachable: boolean;
  ingress: boolean;
  enableIPv6: boolean;
  labels?: Record<string, string>;
  options: Pair[];
  ipam: { subnet: string; gateway: string };
  stack?: string;
  created?: string;
}
export interface Volume {
  id?: string;
  volumeName: string;
  driver: string;
  scope?: string;
  mountpoint?: string;
  size?: number;
  options: Pair[];
  labels?: Record<string, string>;
  stack?: string;
}
export interface Secret {
  id: string;
  secretName: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}
export interface Config {
  id: string;
  configName: string;
  data?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}
export interface Stack {
  stackName: string;
  state?: string;
  stackFile?: boolean;
  services: Service[];
  networks: Network[];
  volumes: (Volume & { host?: string })[];
  secrets: { id?: string; secretName: string }[];
  configs: { id?: string; configName: string }[];
  tasks?: Task[];
  createdAt?: string;
  updatedAt?: string;
}
export interface Compose {
  name: string;
  spec: { compose: string };
  previousSpec?: { compose: string };
  skipImageResolve?: boolean;
}
export interface History {
  at: string;
  by: string;
  comment?: string;
  spec: { compose: string };
  trigger?: { kind?: string; service?: string; digest?: string };
}
export interface LogLine {
  line: string;
  timestamp?: string;
  task?: string;
}
export interface Stats {
  hosts: number;
  cpu: { usage: number };
  memory: { usage: number; used: number; total: number };
  disk: { usage: number; used: number; total: number };
}
export interface Timeseries {
  name?: string;
  service?: string;
  task?: string;
  time: string[];
  cpu?: number[];
  memory?: number[];
}
export type RegistryType = "dockerhub" | "v2" | "ecr" | "acr" | "gitlab";
export const registryTypes: RegistryType[] = [
  "dockerhub",
  "v2",
  "ecr",
  "acr",
  "gitlab",
];
export interface Registry {
  _id: string;
  _rev?: string;
  name?: string;
  username?: string;
  namespace?: string;
  url?: string;
  public: boolean;
  owner?: string;
  type: RegistryType;
  withAuth?: boolean;
  customApi?: boolean;
  region?: string;
  accessKey?: string;
  secretKey?: string;
  clientId?: string;
  clientSecret?: string;
  registryName?: string;
  password?: string;
  token?: string;
  [key: string]: unknown;
}
