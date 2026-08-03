import { redirect } from "next/navigation";

import { REVOCATION_FLOW_ROUTE } from "@/lib/routes";

export default function IdentityReturnPage() {
  redirect(REVOCATION_FLOW_ROUTE);
}
