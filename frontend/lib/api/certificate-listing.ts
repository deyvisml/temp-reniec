import { requestJson } from "@/lib/http-client";
import {
  CERTIFICATE_SELECTION_PATH,
  CURRENT_CERTIFICATES_PATH,
  type CertificateItemContract,
  type CertificateListContract,
  type CertificateSelectionContract,
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

export const replaceCertificateSelection = (certificateUuid: string, signal?: AbortSignal) =>
  requestJson<CertificateList>(CERTIFICATE_SELECTION_PATH, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ certificateUuid } satisfies CertificateSelectionContract),
    signal,
  });
