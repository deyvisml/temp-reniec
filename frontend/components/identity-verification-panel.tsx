"use client";

import Image from "next/image";
import { useRef, useState } from "react";

import { CancellationStepper } from "@/components/cancellation-stepper";
import {
  IdentityCallbackAlert,
  type IdentityCallbackOutcome,
} from "@/components/identity-callback-alert";
import { startIdentityVerification } from "@/lib/api/identity-verifications";
import { HttpClientError } from "@/lib/http-client";

type IdentityView = "ready" | "starting";

export function IdentityVerificationPanel({
  callbackOutcome,
  identityVerified = false,
  onContinue,
}: {
  callbackOutcome?: IdentityCallbackOutcome;
  identityVerified?: boolean;
  onContinue?: () => void;
}) {
  const [view, setView] = useState<IdentityView>("ready");
  const [outcome, setOutcome] = useState<IdentityCallbackOutcome | undefined>(callbackOutcome);
  const inFlight = useRef(false);

  async function begin() {
    if (inFlight.current) return;
    inFlight.current = true;
    setView("starting");

    try {
      const result = await startIdentityVerification();
      if (!result.data?.authorizationUrl) throw new Error("invalid response");
      window.location.assign(result.data.authorizationUrl);
    } catch (error) {
      inFlight.current = false;
      setView("ready");
      setOutcome(mapStartError(error));
    }
  }

  return (
    <section className="mx-auto w-full max-w-[1040px]" aria-labelledby="identity-title" aria-busy={view === "starting"}>
      {outcome ? (
        <IdentityCallbackAlert outcome={outcome} onAcknowledge={() => setOutcome(undefined)} />
      ) : null}
      <div className="px-2 sm:px-8 lg:px-14">
        <CancellationStepper
          currentStep={1}
          navigableSteps={identityVerified ? [2] : []}
          onNavigate={identityVerified ? onContinue : undefined}
        />
      </div>
      <div className="mx-2 mt-6 rounded-[22px] border border-[#dfe7f3] bg-white/95 px-6 py-8 shadow-[0_24px_70px_-36px_#001b6066] sm:mx-8 sm:px-10 lg:mx-14 lg:px-14">
        <div className="mx-auto max-w-[760px] text-center">
          <p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">PASO 1 DE 5</p>
          <h1 id="identity-title" className="mt-4 text-3xl font-black tracking-[-0.025em] text-[#061a50] sm:text-4xl">Verifica tu identidad</h1>
          <p className="mx-auto mt-3 max-w-[620px] text-pretty text-base leading-7 text-[#52678f]">
            Validamos tu identidad de forma segura mediante ID Perú. En este paso todavía no se cancelará ningún certificado.
          </p>

          <div className="mx-auto mt-7 grid max-w-[680px] gap-4 text-left sm:grid-cols-3 sm:gap-0" aria-label="Características de la verificación">
            <TrustFeature icon={<ShieldCheckIcon />} title="Seguro" text="Tus datos están protegidos" />
            <TrustFeature icon={<BoltIcon />} title="Rápido" text="Solo toma unos segundos" separated />
            <TrustFeature icon={<OfficialIcon />} title="Oficial" text="Validación mediante ID Perú" separated />
          </div>

          <div className="relative mx-auto mt-6 min-h-[230px] max-w-[620px] overflow-hidden rounded-2xl border border-[#cdddf6] bg-[radial-gradient(circle_at_center,#edf4ff_0%,#ffffff_70%)] sm:min-h-[270px]">
            <WavePattern />
            <span className="absolute left-1/2 top-1/2 z-10 size-[170px] -translate-x-1/2 -translate-y-1/2 rounded-full border border-[#d8e6fb] sm:size-[205px]" aria-hidden="true" />
            <span className="absolute left-1/2 top-1/2 z-10 size-[205px] -translate-x-1/2 -translate-y-1/2 rounded-full border border-[#e5eefc] sm:size-[245px]" aria-hidden="true" />
            <ScanCorner position="left-5 top-5 border-l-2 border-t-2 sm:left-10 sm:top-8" />
            <ScanCorner position="right-5 top-5 border-r-2 border-t-2 sm:right-10 sm:top-8" />
            <ScanCorner position="bottom-5 left-5 border-b-2 border-l-2 sm:bottom-8 sm:left-10" />
            <ScanCorner position="bottom-5 right-5 border-b-2 border-r-2 sm:bottom-8 sm:right-10" />
            <Image
              className="absolute -bottom-1 left-1/2 z-20 w-[220px] -translate-x-1/2 mix-blend-multiply contrast-110 saturate-125 sm:w-[260px]"
              src="/images/person.png"
              alt="Representación facial para la verificación de identidad"
              width={1254}
              height={1254}
              priority
            />
          </div>

          {identityVerified ? (
            <button type="button" onClick={onContinue} className="mx-auto mt-6 flex min-h-[56px] w-full max-w-[520px] cursor-pointer items-center justify-center gap-3 rounded-lg bg-[linear-gradient(100deg,#c3004b,#950037)] px-6 font-extrabold text-white transition-[filter] hover:brightness-95 active:brightness-90 focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#f4b400] motion-reduce:transition-none">
              Continuar a selección de certificados <ForwardIcon />
            </button>
          ) : (
            <button type="button" disabled={view === "starting"} onClick={() => void begin()} className="mx-auto mt-6 flex min-h-[56px] w-full max-w-[520px] cursor-pointer items-center justify-center gap-3 rounded-lg bg-[linear-gradient(100deg,#c3004b,#950037)] px-6 font-extrabold text-white transition-[filter] hover:not-disabled:brightness-95 active:not-disabled:brightness-90 disabled:cursor-default disabled:opacity-70 focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#f4b400] motion-reduce:transition-none">
              <FaceScanIcon />
              {view === "starting" ? "Conectando con ID Perú…" : "Iniciar verificación facial con ID Perú"}
            </button>
          )}
        </div>
      </div>
    </section>
  );
}

export function mapStartError(error: unknown): IdentityCallbackOutcome {
  if (!(error instanceof HttpClientError)) return "ERROR";
  if (error.code === "IDENTITY_PROVIDER_TIMEOUT") return "TIMEOUT";
  if (["IDENTITY_PROVIDER_UNAVAILABLE", "IDENTITY_NOT_CONFIGURED"].includes(error.code)) return "UNAVAILABLE";
  return "ERROR";
}

function ScanCorner({ position }: { position: string }) {
  return <span className={`absolute z-30 size-7 border-[#0755df] ${position}`} aria-hidden="true" />;
}

function WavePattern() {
  const paths = [
    "M0 135 C65 86 115 184 185 135 S305 86 375 135 S505 184 620 135",
    "M0 126 C65 88 115 164 185 126 S305 88 375 126 S505 164 620 126",
    "M0 144 C65 106 115 182 185 144 S305 106 375 144 S505 182 620 144",
    "M0 117 C65 92 115 142 185 117 S305 92 375 117 S505 142 620 117",
    "M0 153 C65 128 115 178 185 153 S305 128 375 153 S505 178 620 153",
  ];

  return (
    <svg className="absolute inset-0 z-0 h-full w-full opacity-45" viewBox="0 0 620 270" preserveAspectRatio="none" aria-hidden="true">
      <defs>
        <mask id="identity-wave-sides" maskUnits="userSpaceOnUse" x="0" y="0" width="620" height="270">
          <rect width="620" height="270" fill="white" />
          <ellipse cx="310" cy="142" rx="112" ry="134" fill="black" />
        </mask>
      </defs>
      <g mask="url(#identity-wave-sides)">
        {paths.map((path) => (
          <path key={path} d={path} fill="none" stroke="#77a7ff" strokeWidth="1.4" strokeDasharray="1 10" strokeLinecap="round" />
        ))}
      </g>
    </svg>
  );
}

function TrustFeature({ icon, title, text, separated = false }: { icon: React.ReactNode; title: string; text: string; separated?: boolean }) { return <div className={`flex items-center gap-3 px-4 py-1 ${separated ? "sm:border-l sm:border-[#d9e2f0]" : ""}`}><span className="grid size-11 shrink-0 place-items-center rounded-full bg-[#edf4ff] text-[#0755df] [&_svg]:size-6" aria-hidden="true">{icon}</span><span><strong className="block text-sm text-[#061a50]">{title}</strong><span className="mt-0.5 block text-xs text-[#607199]">{text}</span></span></div>; }
const iconClass = "fill-none stroke-current stroke-[1.8] [stroke-linecap:round] [stroke-linejoin:round]";
function ShieldCheckIcon() { return <svg className={iconClass} viewBox="0 0 24 24"><path d="M12 3 5.5 6v5.2c0 4.2 2.5 7.7 6.5 9.8 4-2.1 6.5-5.6 6.5-9.8V6L12 3Z"/><path d="m9.2 12 1.8 1.8 3.8-4"/></svg>; }
function BoltIcon() { return <svg className={iconClass} viewBox="0 0 24 24"><path d="m13.5 2-8 12h6l-1 8 8-12h-6l1-8Z"/></svg>; }
function OfficialIcon() { return <svg className={iconClass} viewBox="0 0 24 24"><path d="m12 3 2 2.2 3-.2.8 2.9 2.6 1.5-1.1 2.8 1.1 2.8-2.6 1.5-.8 2.9-3-.2L12 21l-2-2.2-3 .2-.8-2.9-2.6-1.5 1.1-2.8-1.1-2.8 2.6-1.5L7 5l3 .2L12 3Z"/><path d="m9.5 12 1.7 1.7 3.5-3.7"/></svg>; }
function FaceScanIcon() { return <svg className={`${iconClass} size-6 shrink-0`} viewBox="0 0 24 24" aria-hidden="true"><path d="M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3"/><circle cx="12" cy="10" r="2.5"/><path d="M7.5 17c.6-2.7 2.1-4 4.5-4s3.9 1.3 4.5 4"/></svg>; }
function ForwardIcon() { return <svg className={`${iconClass} size-5 shrink-0`} viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14m-5-5 5 5-5 5"/></svg>; }
