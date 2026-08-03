"use client";

import { useRouter } from "next/navigation";
import { RevocationEntry } from "@/components/revocation-entry";
import { activeFlowRoute } from "@/lib/routes";

export function PublicRevocationEntry() {
  const router = useRouter();
  return <RevocationEntry onContinue={() => router.push(activeFlowRoute())} />;
}
