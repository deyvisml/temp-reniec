import type { ReactNode } from "react";
import { InternalFlowHeader } from "@/components/internal-flow-header";
import { requireServerFlowSession } from "@/lib/server-flow-session";

export default async function AuthorizationLayout({ children }: { children: ReactNode }) {
  const session = await requireServerFlowSession("/autorizacion");
  return <><InternalFlowHeader session={session} />{children}</>;
}
