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

export const getCurrentCertificates = (signal?: AbortSignal) =>
  requestJson<CertificateList>(CURRENT_CERTIFICATES_PATH, { signal });

export const replaceCertificateSelection = (certificateUuids: string[], signal?: AbortSignal) =>
  requestJson<CertificateList>(CERTIFICATE_SELECTION_PATH, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ certificateUuids } satisfies CertificateSelectionContract),
    signal,
  });
