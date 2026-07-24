"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { createPortal } from "react-dom";

import { logoutFlowSession, type CurrentFlowSession } from "@/lib/api/flow-session";

export function InternalFlowHeader({ session }: { session: CurrentFlowSession }) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [logoutFailed, setLogoutFailed] = useState(false);
  const [portalTarget, setPortalTarget] = useState<HTMLElement | null>(null);

  useEffect(() => {
    setPortalTarget(document.getElementById("internal-flow-header-slot"));
  }, []);

  useEffect(() => {
    if (!("BroadcastChannel" in window)) return;
    const channel = new BroadcastChannel("cancelacion-flow-session");
    channel.onmessage = event => {
      if (event.data === "logout") router.replace("/");
    };
    return () => channel.close();
  }, [router]);

  async function logout() {
    if (pending) return;
    setLogoutFailed(false);
    setPending(true);
    try {
      await logoutFlowSession();
      if ("BroadcastChannel" in window) {
        const channel = new BroadcastChannel("cancelacion-flow-session");
        channel.postMessage("logout");
        channel.close();
      }
      router.replace("/");
      router.refresh();
    } catch {
      setLogoutFailed(true);
      setPending(false);
    }
  }

  if (!portalTarget) return null;

  return createPortal(
    <InternalFlowHeaderActions
      dni={session.dni}
      pending={pending}
      logoutFailed={logoutFailed}
      onLogout={() => void logout()}
    />,
    portalTarget,
  );
}

export function InternalFlowHeaderActions({
  dni,
  pending,
  logoutFailed = false,
  onLogout,
}: {
  dni: string;
  pending: boolean;
  logoutFailed?: boolean;
  onLogout: () => void;
}) {
  return (
    <div className="flex items-center gap-3 min-[520px]:gap-5">
      <div className="flex items-center gap-2.5" aria-label={`Perfil del ciudadano, DNI ${dni}`}>
        <span className="grid size-9 shrink-0 place-items-center rounded-full bg-white/12 text-white ring-1 ring-inset ring-white/25 min-[520px]:size-10" aria-hidden="true">
          <UserIcon />
        </span>
        <span className="hidden items-baseline gap-2 min-[420px]:flex">
          <span className="text-xs font-medium text-[#d7e3f8]">DNI</span>
          <strong className="text-sm tracking-[0.04em] text-white">{dni}</strong>
        </span>
        <strong className="text-xs tracking-[0.03em] text-white min-[420px]:hidden">{dni}</strong>
      </div>
      <button
        type="button"
        onClick={onLogout}
        disabled={pending}
        aria-describedby={logoutFailed ? "logout-error" : undefined}
        className="inline-flex cursor-pointer items-center gap-1.5 border-0 bg-transparent px-1 py-2 text-xs font-semibold text-[#d7e3f8] transition-colors hover:text-white focus-visible:rounded-sm focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#f4b400] disabled:cursor-not-allowed disabled:opacity-60 min-[520px]:gap-2 min-[520px]:text-sm"
      >
        <LogoutIcon />
        <span className="hidden min-[420px]:inline">{pending ? "Saliendo…" : logoutFailed ? "Reintentar salida" : "Cerrar sesión"}</span>
        <span className="min-[420px]:hidden">{pending ? "Saliendo…" : logoutFailed ? "Reintentar" : "Salir"}</span>
      </button>
      {logoutFailed ? <span id="logout-error" className="sr-only" role="alert">No pudimos cerrar la sesión. Inténtalo nuevamente.</span> : null}
    </div>
  );
}

function UserIcon() {
  return <svg className="size-5 fill-none stroke-current stroke-[1.8] [stroke-linecap:round] [stroke-linejoin:round]" viewBox="0 0 24 24"><circle cx="12" cy="8" r="3.5"/><path d="M5.5 20c.6-4 2.8-6 6.5-6s5.9 2 6.5 6"/></svg>;
}

function LogoutIcon() {
  return <svg className="size-4 shrink-0 fill-none stroke-current stroke-[1.8]" viewBox="0 0 24 24" aria-hidden="true"><path d="M14 8V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h7a2 2 0 0 0 2-2v-3M10 12h11M18 9l3 3-3 3" strokeLinecap="round" strokeLinejoin="round"/></svg>;
}
