import { resolve } from "node:path";

import type { AppConfig } from "./types.js";

export function loadConfig(environment: NodeJS.ProcessEnv = process.env): AppConfig {
  const port = Number(environment.PORT ?? "8081");
  const apiKey = environment.API_KEY ?? "app_revocaciones_reniec.RENIEC2026";

  if (!Number.isInteger(port) || port < 1 || port > 65_535) {
    throw new Error("PORT must be an integer between 1 and 65535");
  }
  if (apiKey.trim().length < 12) {
    throw new Error("API_KEY must contain at least 12 characters");
  }
  return {
    host: environment.HOST ?? "0.0.0.0",
    port,
    apiKey,
    seedFile: resolve(environment.SEED_FILE ?? "fixtures/credentials.seed.json"),
    dataFile: resolve(environment.DATA_FILE ?? "data/credentials.json"),
  };
}
