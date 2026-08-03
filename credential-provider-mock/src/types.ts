export type CredentialStatus = 0 | 1;

export interface Credential {
  credentialType: string;
  listCredential: string;
  statusListIndex: number;
  issuanceDate: string;
  revocateDate: string | null;
  credentialStatus: CredentialStatus;
}

export interface CredentialData {
  version: 1;
  citizens: Record<string, Credential[]>;
}

export interface AppConfig {
  host: string;
  port: number;
  apiKey: string;
  seedFile: string;
  dataFile: string;
  personalTestDni?: string;
  additionalTestDni?: string;
}
