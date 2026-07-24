export const CANCELLATION_FLOW_ROUTE = "/cancelacion";
export const LOCAL_IDENTITY_ROUTE = "/autorizacion";

export function usesLocalIdentityRoute(
  appEnvironment = process.env.NEXT_PUBLIC_APP_ENV,
  nodeEnvironment = process.env.NODE_ENV,
): boolean {
  if (appEnvironment) return appEnvironment.toLowerCase() === "local";
  return nodeEnvironment === "development";
}

export function activeFlowRoute(
  appEnvironment = process.env.NEXT_PUBLIC_APP_ENV,
  nodeEnvironment = process.env.NODE_ENV,
): string {
  return usesLocalIdentityRoute(appEnvironment, nodeEnvironment)
    ? LOCAL_IDENTITY_ROUTE
    : CANCELLATION_FLOW_ROUTE;
}
