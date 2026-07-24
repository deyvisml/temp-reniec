import { redirect } from "next/navigation";
import { PublicCancellationEntry } from "@/components/public-cancellation-entry";
import { activeFlowRoute } from "@/lib/routes";
import { hasServerRefreshToken, readServerFlowSession } from "@/lib/server-flow-session";

export default async function HomePage() {
  const flowRoute = activeFlowRoute();
  if (await readServerFlowSession()) redirect(flowRoute);
  if (await hasServerRefreshToken()) {
    redirect(`/api/session/refresh?returnTo=${encodeURIComponent(flowRoute)}`);
  }
  return <PublicCancellationEntry />;
}
