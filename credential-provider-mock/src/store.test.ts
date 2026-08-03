import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { resolve } from "node:path";

import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { CredentialStore } from "./store.js";

describe("CredentialStore", () => {
  let directory: string;
  let dataFile: string;
  const seedFile = resolve("fixtures/credentials.seed.json");

  beforeEach(async () => {
    directory = await mkdtemp(resolve(tmpdir(), "credential-store-"));
    dataFile = resolve(directory, "credentials.json");
  });

  afterEach(async () => {
    await rm(directory, { recursive: true, force: true });
  });

  it("loads persisted revocations after creating a new store", async () => {
    const first = new CredentialStore(seedFile, dataFile);
    await first.initialize();
    await first.revoke("00000021", "44444444-4444-4444-8444-444444444444", 21);

    const restarted = new CredentialStore(seedFile, dataFile);
    await restarted.initialize();

    expect(restarted.listCredentials("00000021")[0]).toMatchObject({
      credentialStatus: 1,
    });
    expect(restarted.hasActiveCredentials("00000021")).toBe(false);
  });

  it("serializes concurrent revocations and returns one effective mutation", async () => {
    const store = new CredentialStore(seedFile, dataFile);
    await store.initialize();

    const results = await Promise.all([
      store.revoke("00000021", "44444444-4444-4444-8444-444444444444", 21),
      store.revoke("00000021", "44444444-4444-4444-8444-444444444444", 21),
    ]);

    expect(results.filter((result) => !result.alreadyRevoked)).toHaveLength(1);
    expect(results.filter((result) => result.alreadyRevoked)).toHaveLength(1);
    expect(results.every((result) => result.credentialStatus === 1)).toBe(true);
  });
});
