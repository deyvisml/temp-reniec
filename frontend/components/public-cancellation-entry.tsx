"use client";

import { useRouter } from "next/navigation";
import { CancellationEntry } from "@/components/cancellation-entry";
import { activeFlowRoute } from "@/lib/routes";

export function PublicCancellationEntry() {
  const router = useRouter();
  return <CancellationEntry onContinue={() => router.push(activeFlowRoute())} />;
}
