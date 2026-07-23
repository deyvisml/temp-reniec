import { redirect } from "next/navigation";

import { CANCELLATION_FLOW_ROUTE } from "@/lib/routes";

export default function IdentityVerificationPage() {
  redirect(CANCELLATION_FLOW_ROUTE);
}
