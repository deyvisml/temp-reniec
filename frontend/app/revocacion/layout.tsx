import type { ReactNode } from "react";
import { InternalFlowHeader } from "@/components/internal-flow-header";
import { requireServerFlowSession } from "@/lib/server-flow-session";

export default async function RevocationLayout({ children }: { children: ReactNode }) {
  const session = await requireServerFlowSession("/revocacion");
  return <><InternalFlowHeader session={session} />{children}</>;
}
