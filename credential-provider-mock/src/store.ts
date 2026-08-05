import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import { dirname } from "node:path";

import type { Credential, CredentialData } from "./types.js";

const DNI_PATTERN = /^\d{8}$/;
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

export interface RevocationResult {
  found: boolean;
  alreadyRevoked: boolean;
  credentialStatus?: 1;
}

export class CredentialStore {
  private data: CredentialData | undefined;
  private mutationQueue: Promise<void> = Promise.resolve();
  private readonly seedFile: string;
  private readonly dataFile: string;

  constructor(seedFile: string, dataFile: string) {
    this.seedFile = seedFile;
    this.dataFile = dataFile;
  }

  async initialize(): Promise<void> {
    try {
      this.data = parseData(await readFile(this.dataFile, "utf8"));
    } catch (error) {
      if (!isMissingFile(error)) throw error;
      await this.reset();
    }
  }

  hasCredentials(dni: string): boolean {
    return this.credentialsFor(dni).length > 0;
  }

  listCredentials(dni: string): Credential[] {
    return structuredClone(this.credentialsFor(dni)).sort(
      (left, right) => left.statusListIndex - right.statusListIndex,
    );
  }

  async revoke(dni: string, uuid: string, statusListIndex: number): Promise<RevocationResult> {
    return this.mutate(async () => {
      const credential = this.credentialsFor(dni).find(
        (item) => item.listCredential === uuid && item.statusListIndex === statusListIndex,
      );
      if (!credential) return { found: false, alreadyRevoked: false };
      if (credential.credentialStatus === 1) {
        return { found: true, alreadyRevoked: true, credentialStatus: 1 };
      }

      credential.credentialStatus = 1;
      credential.revocateDate = limaLocalDateTime(new Date());
      await this.persist();
      return { found: true, alreadyRevoked: false, credentialStatus: 1 };
    });
  }

  async reset(): Promise<void> {
    await this.mutate(async () => {
      this.data = parseData(await readFile(this.seedFile, "utf8"));
      await this.persist();
    });
  }

  private credentialsFor(dni: string): Credential[] {
    if (!this.data) throw new Error("Credential store has not been initialized");
    return this.data.citizens[dni] ?? [];
  }

  private async persist(): Promise<void> {
    if (!this.data) throw new Error("Credential store has not been initialized");
    await mkdir(dirname(this.dataFile), { recursive: true });
    const temporaryFile = `${this.dataFile}.${process.pid}.${Date.now()}.tmp`;
    await writeFile(temporaryFile, `${JSON.stringify(this.data, null, 2)}\n`, "utf8");
    await rename(temporaryFile, this.dataFile);
  }

  private async mutate<T>(operation: () => Promise<T>): Promise<T> {
    const previous = this.mutationQueue;
    let release!: () => void;
    this.mutationQueue = new Promise<void>((resolve) => { release = resolve; });
    await previous;
    try {
      return await operation();
    } finally {
      release();
    }
  }
}

function parseData(raw: string): CredentialData {
  const parsed: unknown = JSON.parse(raw);
  if (!isRecord(parsed) || parsed.version !== 1 || !isRecord(parsed.citizens)) {
    throw new Error("Credential data has an invalid structure");
  }

  const citizens: Record<string, Credential[]> = {};
  for (const [dni, value] of Object.entries(parsed.citizens)) {
    if (!DNI_PATTERN.test(dni) || !Array.isArray(value)) throw new Error("Credential data contains an invalid DNI");
    const indexes = new Set<number>();
    const identities = new Set<string>();
    citizens[dni] = value.map((candidate) => {
      if (!isCredential(candidate)) throw new Error("Credential data contains an invalid credential");
      const identity = `${candidate.listCredential}:${candidate.statusListIndex}`;
      if (indexes.has(candidate.statusListIndex) || identities.has(identity)) {
        throw new Error("Credential data contains duplicate indexes or credential identities for a DNI");
      }
      indexes.add(candidate.statusListIndex);
      identities.add(identity);
      return structuredClone(candidate);
    });
  }
  return { version: 1, citizens };
}

function isCredential(value: unknown): value is Credential {
  if (!isRecord(value)) return false;
  const status = value.credentialStatus;
  const revokedAt = value.revocateDate;
  return value.credentialType === "DniPeruanoCredential"
    && typeof value.listCredential === "string" && UUID_PATTERN.test(value.listCredential)
    && Number.isInteger(value.statusListIndex) && Number(value.statusListIndex) >= 0
    && typeof value.issuanceDate === "string" && isLocalDateTime(value.issuanceDate)
    && (status === 0 || status === 1)
    && ((status === 0 && revokedAt === null)
      || (status === 1 && typeof revokedAt === "string" && isLocalDateTime(revokedAt)));
}

function isLocalDateTime(value: string): boolean {
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/.test(value) && !Number.isNaN(Date.parse(`${value}-05:00`));
}

function limaLocalDateTime(date: Date): string {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Lima",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  }).formatToParts(date);
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((item) => item.type === type)?.value;
  return `${part("year")}-${part("month")}-${part("day")}T${part("hour")}:${part("minute")}:${part("second")}`;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isMissingFile(error: unknown): boolean {
  return isRecord(error) && error.code === "ENOENT";
}
