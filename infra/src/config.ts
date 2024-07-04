const environments = ["hahtuva", "dev", "qa", "prod"] as const;
type EnvironmentName = (typeof environments)[number];

const defaultConfig = {
  minCapacity: 0,
  maxCapacity: 0,
};

export type Config = typeof defaultConfig;

export function getEnvironment(): EnvironmentName {
  const env = process.env.ENV;
  if (!env) {
    throw new Error("ENV environment variable is not set");
  }
  if (!contains(environments, env)) {
    throw new Error(`Invalid environment name: ${env}`);
  }
  return env as EnvironmentName;
}

function contains(arr: readonly string[], value: string): boolean {
  return arr.includes(value);
}

export function getConfig(): Config {
  const env = getEnvironment();
  return { hahtuva, dev, qa, prod }[env];
}

export const hahtuva: Config = {
  ...defaultConfig,
  minCapacity: 1,
  maxCapacity: 2,
};

export const dev: Config = {
  ...defaultConfig,
};

export const qa: Config = {
  ...defaultConfig,
};

export const prod: Config = {
  ...defaultConfig,
};