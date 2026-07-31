import { requestJson } from "@/lib/http-client";
import {
  CURRENT_CERTIFICATES_PATH,
  type CertificateItemContract,
  type CertificateListContract,
} from "@/lib/api/contracts";

export type CertificateItem = CertificateItemContract;
export type CertificateList = CertificateListContract;

let currentCertificatesRequest: ReturnType<typeof requestJson<CertificateList>> | undefined;

/**
 * Comparte únicamente la lectura que está en curso. Esto evita que un
 * remontaje de React en desarrollo cancele una consulta y cree otra idéntica.
 * La promesa se libera al terminar, por lo que cada entrada posterior vuelve
 * a consultar el estado vigente del trámite.
 */
export function getCurrentCertificates() {
  currentCertificatesRequest ??= requestJson<CertificateList>(CURRENT_CERTIFICATES_PATH)
    .finally(() => { currentCertificatesRequest = undefined; });
  return currentCertificatesRequest;
}
