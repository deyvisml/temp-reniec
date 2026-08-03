import { resolve } from "node:path";

import type { AppConfig } from "./types.js";

const DNI_PATTERN = /^\d{8}$/;

export function loadConfig(environment: NodeJS.ProcessEnv = process.env): AppConfig {
  const port = Number(environment.PORT ?? "8081");
  const apiKey = environment.API_KEY ?? "local-credential-provider-key";
  const personalTestDni = environment.PERSONAL_TEST_DNI?.trim();
  const additionalTestDni = environment.ADDITIONAL_TEST_DNI?.trim();

  if (!Number.isInteger(port) || port < 1 || port > 65_535) {
    throw new Error("PORT must be an integer between 1 and 65535");
  }
  if (apiKey.trim().length < 12) {
    throw new Error("API_KEY must contain at least 12 characters");
  }
  if (personalTestDni && !DNI_PATTERN.test(personalTestDni)) {
    throw new Error("PERSONAL_TEST_DNI must contain exactly 8 digits");
  }
  if (additionalTestDni && !DNI_PATTERN.test(additionalTestDni)) {
    throw new Error("ADDITIONAL_TEST_DNI must contain exactly 8 digits");
  }
  if (personalTestDni && additionalTestDni === personalTestDni) {
    throw new Error("PERSONAL_TEST_DNI and ADDITIONAL_TEST_DNI must be different");
  }

  return {
    host: environment.HOST ?? "0.0.0.0",
    port,
    apiKey,
    seedFile: resolve(environment.SEED_FILE ?? "fixtures/credentials.seed.json"),
    dataFile: resolve(environment.DATA_FILE ?? "data/credentials.json"),
    ...(personalTestDni ? { personalTestDni } : {}),
    ...(additionalTestDni ? { additionalTestDni } : {}),
  };
}
