import { redirect } from "next/navigation";

import { RevocationFlow } from "@/components/revocation-flow";
import { REVOCATION_FLOW_ROUTE, usesLocalIdentityRoute } from "@/lib/routes";

export default function IdPeruLocalReturnPage() {
  if (!usesLocalIdentityRoute()) redirect(REVOCATION_FLOW_ROUTE);

  return <RevocationFlow />;
}
