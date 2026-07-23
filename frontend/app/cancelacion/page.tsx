import type { Metadata } from "next";

import { CancellationFlow } from "@/components/cancellation-flow";
import { LOCAL_IDENTITY_ROUTE, usesLocalIdentityRoute } from "@/lib/routes";

export const metadata: Metadata = { title: "Cancelación de certificados digitales" };

export default function CancellationPage() {
  return (
    <CancellationFlow
      identityRoute={usesLocalIdentityRoute() ? LOCAL_IDENTITY_ROUTE : undefined}
    />
  );
}
