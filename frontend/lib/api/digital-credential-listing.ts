import { requestJson } from "@/lib/http-client";
import {
  CURRENT_DIGITAL_CREDENTIALS_PATH,
  type DigitalCredentialItemContract,
  type DigitalCredentialListContract,
} from "@/lib/api/contracts";

export type DigitalCredentialItem = DigitalCredentialItemContract;
export type DigitalCredentialList = DigitalCredentialListContract;

let currentDigitalCredentialsRequest: ReturnType<typeof requestJson<DigitalCredentialList>> | undefined;

/**
 * Comparte únicamente la lectura que está en curso. Esto evita que un
 * remontaje de React en desarrollo cancele una consulta y cree otra idéntica.
 * La promesa se libera al terminar, por lo que cada entrada posterior vuelve
 * a consultar el estado vigente del trámite.
 */
export function getCurrentDigitalCredentials() {
  currentDigitalCredentialsRequest ??= requestJson<DigitalCredentialList>(CURRENT_DIGITAL_CREDENTIALS_PATH)
    .finally(() => { currentDigitalCredentialsRequest = undefined; });
  return currentDigitalCredentialsRequest;
}
