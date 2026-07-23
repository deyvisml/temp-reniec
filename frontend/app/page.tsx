import { redirect } from "next/navigation";

import { CANCELLATION_FLOW_ROUTE } from "@/lib/routes";

export default function HomePage() {
  redirect(CANCELLATION_FLOW_ROUTE);
}
