"use client";

import Image from "next/image";
import { useEffect, useState } from "react";

import { RevocationStepper } from "@/components/revocation-stepper";
import { FlowStepContent } from "@/components/flow-step-content";
import {
  REVOCATION_RECEIPT_PATH,
} from "@/lib/api/contracts";
import {
  getCurrentRevocationOutcome,
  type RevocationExecution,
} from "@/lib/api/revocation-confirmation";
import { requestBlob } from "@/lib/http-client";

const formatter = new Intl.DateTimeFormat("es-PE", {
  dateStyle: "short", timeStyle: "short", hour12: false,
});

export function RevocationReceiptTransition({ dni, initialData }: {
  dni: string;
  initialData?: RevocationExecution;
}) {
  const [data, setData] = useState(initialData);
  const [error, setError] = useState<string>();
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (data) return;
    void getCurrentRevocationOutcome()
      .then(result => {
        if (!result.data || result.data.state !== "SUCCEEDED") {
          throw new Error("Receipt unavailable");
        }
        setData(result.data);
      })
      .catch(() => setError("No pudimos recuperar la constancia."));
  }, [data]);

  const download = async () => {
    if (downloading) return;
    setDownloading(true);
    setError(undefined);
    try {
      const result = await requestBlob(REVOCATION_RECEIPT_PATH, {
        headers: { Accept: "application/pdf" },
      });
      if (!result.data) throw new Error("Download failed");
      const url = URL.createObjectURL(result.data);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `constancia-${data?.receipt?.code ?? "revocacion"}.pdf`;
      document.body.append(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } catch {
      setError("No pudimos descargar la constancia. Inténtalo nuevamente.");
    } finally {
      setDownloading(false);
    }
  };

  return (
    <section className="mx-auto w-full max-w-[1040px]" aria-labelledby="receipt-title">
      <div className="px-2 sm:px-8 lg:px-14">
        <RevocationStepper currentStep={5} />
      </div>
      <div className="mx-2 mt-6 rounded-2xl bg-white px-4 py-7 sm:mx-8 sm:px-8 sm:py-9 lg:mx-14">
        {!data && !error ? <Loading /> : null}
        {error && !data ? (
          <div className="grid min-h-[420px] place-items-center text-center" role="alert">
            <div><h1 className="text-2xl font-black text-[#061a50]">{error}</h1>
              <button className="mt-6 rounded-lg bg-reniec-red px-6 py-3 font-bold text-white" onClick={() => window.location.reload()}>Reintentar</button>
            </div>
          </div>
        ) : null}
        {data ? (
          <FlowStepContent>
            <header className="text-center">
              <p className="inline-flex rounded-full bg-[#fae9f0] px-4 py-1.5 text-xs font-black text-reniec-red">PASO 5 DE 5</p>
              <h1 id="receipt-title" className="mt-4 text-2xl font-black tracking-[-0.025em] text-[#061a50] sm:text-3xl">Constancia generada correctamente</h1>
              <p className="mt-3 text-sm leading-6 text-[#52678f] sm:text-base">La credencial verificable seleccionada fue revocada correctamente.</p>
            </header>
            <ReceiptIllustration />
            <dl className="mt-4 overflow-hidden rounded-xl bg-white ring-1 ring-[#e5e9f0] shadow-[0_5px_18px_rgba(22,46,91,0.08)]">
              <ResultRow icon="id" label="DNI" value={dni} />
              {data.firstName ? <ResultRow icon="person" label="Nombre" value={data.firstName} /> : null}
              <ResultRow icon="cert" label="Credencial" value={`Índice ${data.digitalCredential.statusListIndex}`} />
              <ResultRow icon="shield" label="Motivo" value={data.reasonLabel} />
              <ResultRow icon="calendar" label="Fecha y hora" value={formatDate(data.completedAt)} />
              <ResultRow icon="document" label="Código de constancia" value={data.receipt?.code ?? "No disponible"} />
            </dl>
            <div className="mt-4 flex gap-4 rounded-xl border-l-2 border-reniec-red bg-[#fff7fa] px-4 py-3.5 text-sm leading-6 text-[#52627f] sm:px-5">
              <span className="mt-0.5 grid size-7 shrink-0 place-items-center rounded-full border border-[#d83a76] text-reniec-red" aria-hidden="true"><InfoIcon /></span>
              <div>
                <p className="font-bold text-[#173568]">Se revocó únicamente la credencial verificable seleccionada.</p>
                <p>Esta acción no afecta tu DNI ni tu identidad civil.</p>
              </div>
            </div>
            {error ? <p className="mt-4 text-center text-sm font-semibold text-[#9c1745]" role="alert">{error}</p> : null}
            <div className="mt-6 flex justify-center">
              <button type="button" onClick={() => void download()} disabled={downloading}
                className="inline-flex min-h-12 w-full max-w-[300px] items-center justify-center gap-2 rounded-lg bg-reniec-red px-6 font-bold text-white hover:bg-[#a8003f] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0755df] disabled:opacity-60">
                <DownloadIcon /> {downloading ? "Descargando…" : "Descargar constancia"}
              </button>
            </div>
            <p className="mt-5 text-center text-xs leading-5 text-[#64779d]">Guarda esta constancia para tus registros.</p>
          </FlowStepContent>
        ) : null}
      </div>
    </section>
  );
}

function ResultRow({ label, value, icon }: { label: string; value: string; icon: string }) {
  return <div className="grid grid-cols-[36px_1fr] items-center gap-x-3 border-b border-[#edf0f5] px-4 py-3 last:border-0 sm:grid-cols-[36px_180px_1fr] sm:px-5">
    <span className="grid size-8 place-items-center rounded-lg bg-[#fff0f5] text-[#9f365f]" aria-hidden="true"><MiniIcon kind={icon} /></span>
    <dt className="font-bold text-[#0a2259]">{label}</dt>
    <dd className="col-start-2 mt-1 break-words text-[#425b8e] sm:col-start-3 sm:mt-0">{value}</dd>
  </div>;
}

function ReceiptIllustration() {
  return (
    <div className="mx-auto mt-4 w-full max-w-[340px]" aria-hidden="true">
      <Image
        src="/images/receipt-confirmation-complete-v2.png"
        alt=""
        width={460}
        height={228}
        sizes="(max-width: 640px) 82vw, 340px"
        className="h-auto w-full object-contain opacity-80 saturate-50"
        priority
      />
    </div>
  );
}

function MiniIcon({ kind }: { kind: string }) {
  const paths: Record<string, string> = { id: "M4 6h16v12H4zM7 10h3m-3 3h5m3-4h2m-2 4h2", person: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm7 8a7 7 0 0 0-14 0", cert: "M6 3h9l3 3v15H6zM9 10h6m-6 4h4", shield: "M12 3 5 6v5c0 5 3 8 7 10 4-2 7-5 7-10V6l-7-3Zm-3 8 2 2 4-4", calendar: "M5 5h14v15H5zM8 3v4m8-4v4M5 9h14", document: "M6 3h9l3 3v15H6zM9 11h6m-6 4h6" };
  return <svg viewBox="0 0 24 24" className="size-4 fill-none stroke-current stroke-2" aria-hidden="true"><path d={paths[kind] ?? paths.document} strokeLinecap="round" strokeLinejoin="round"/></svg>;
}

function formatDate(value?: string | null) {
  if (!value) return "Fecha no disponible";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Fecha no disponible" : formatter.format(date);
}
function Loading() { return <div className="grid min-h-[420px] place-items-center" aria-busy="true"><span className="size-10 animate-spin rounded-full border-4 border-[#d6e2f7] border-t-reniec-red" /></div>; }
function DownloadIcon() { return <svg viewBox="0 0 24 24" className="size-5 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M12 3v12m-5-5 5 5 5-5M5 20h14"/></svg>; }
function InfoIcon() { return <svg viewBox="0 0 24 24" className="size-4 fill-none stroke-current stroke-2" aria-hidden="true"><path d="M12 11v6m0-10v.01" strokeLinecap="round"/></svg>; }
