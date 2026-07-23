import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { CancellationFlow } from "@/components/cancellation-flow";
import { CANCELLATION_FLOW_ROUTE, usesLocalIdentityRoute } from "@/lib/routes";

export const metadata: Metadata = { title: "Verificación de identidad" };

export default function IdPeruLocalReturnPage() {
  if (!usesLocalIdentityRoute()) redirect(CANCELLATION_FLOW_ROUTE);

  return <CancellationFlow initialRoute="identity" />;
}
