import { redirect } from "next/navigation";

import { REVOCATION_FLOW_ROUTE } from "@/lib/routes";

export default function IdentityVerificationPage() {
  redirect(REVOCATION_FLOW_ROUTE);
}
