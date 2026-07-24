import type { Metadata } from "next";

import { CancellationFlow } from "@/components/cancellation-flow";

export const metadata: Metadata = { title: "Cancelación de certificados digitales" };

export default function CancellationPage() {
  return <CancellationFlow />;
}
