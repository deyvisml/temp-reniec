import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { DigitalCredentialSelectionView } from "@/components/digital-credential-selection-transition";
import { RevocationStepper } from "@/components/revocation-stepper";
import type { DigitalCredentialItem } from "@/lib/api/digital-credential-listing";

const digitalCredentials: DigitalCredentialItem[] = [
  {
    statusListIndex: 31,
    emissionCreatedAt: "2026-07-20T10:00:00Z",
    digitalCredentialUuid: "10000000-0000-4000-8000-000000000001",
    status: "ACTIVE",
    revokedAt: null,
    selected: false,
  },
  {
    statusListIndex: 32,
    emissionCreatedAt: "2026-07-21T11:30:00Z",
    digitalCredentialUuid: "10000000-0000-4000-8000-000000000002",
    status: "ACTIVE",
    revokedAt: null,
    selected: false,
  },
  {
    statusListIndex: 33,
    emissionCreatedAt: "2026-07-22T09:15:00Z",
    digitalCredentialUuid: "10000000-0000-4000-8000-000000000003",
    status: "REVOKED",
    revokedAt: "2026-07-25T14:45:00Z",
    selected: false,
  },
];

describe("selección de credenciales", () => {
  it("renderiza las credenciales como un grupo de selección exclusiva accesible", () => {
    const markup = renderToStaticMarkup(
      <DigitalCredentialSelectionView digitalCredentials={digitalCredentials} selected={null}
        submitting={false} onSelect={() => undefined}
        onSubmit={() => undefined} onBack={() => undefined} />,
    );

    expect(markup).not.toContain("PASO 2 DE 5");
    expect(markup).toContain("Selecciona una credencial");
    expect(markup).toContain("Elige cuál deseas revocar para continuar.");
    expect(markup).not.toContain("asociado a tu DNI para incluirlo en esta solicitud");
    expect(markup).toContain("Índice de credencial");
    expect(markup).toContain(">31<");
    expect(markup).toContain(">32<");
    expect(markup).toContain("Credencial digital vigente 01");
    expect(markup).toContain("Credencial digital vigente 02");
    expect(markup).not.toMatch(/>UUID</);
    expect(markup).not.toContain("Emisión asociada a tu DNI");
    expect(markup.match(/viewBox="0 0 56 64"/g)?.length).toBe(3);
    expect(markup.match(/type="radio"/g)?.length).toBe(2);
    expect(markup).toContain('type="radio"');
    expect(markup).not.toContain("Seleccionar todos");
    expect(markup).toContain("Seleccionar credencial con índice 31");
    expect(markup).toContain("Regresar");
    expect(markup).toContain("Debes seleccionar una credencial");
    expect(markup.match(/sm:w-\[280px\]/g)?.length).toBe(2);
    expect(markup).toContain("disabled");
  });

  it("muestra la cantidad exacta y habilita continuar con una selección", () => {
    const markup = renderToStaticMarkup(
		<DigitalCredentialSelectionView digitalCredentials={digitalCredentials}
			selected={digitalCredentials[0]} submitting={false}
        onSelect={() => undefined} onSubmit={() => undefined}
        onBack={() => undefined} />,
    );

    expect(markup).toContain("1 credencial seleccionada");
    expect(markup).toContain("checked");
    expect(markup).toContain('data-selected="true"');
    expect(markup).toContain('data-selected="false"');
    expect(markup).toContain("border-[#1768f2] bg-[#f5f8ff]");
    expect(markup).toContain("hover:border-[#8eafe9]");
    expect(markup).toContain("font-semibold leading-6 text-[#061a50]");
    expect(markup).toContain("grid-cols-[64px_minmax(0,1fr)_160px_96px]");
    expect(markup.match(/md:whitespace-nowrap/g)?.length).toBeGreaterThanOrEqual(4);
    expect(markup).toContain("font-medium text-[#173568]");
    expect(markup).not.toContain("font-semibold text-[#0a2259]");
    expect(markup).not.toContain("border-[#c93b72]");
    expect(markup).not.toContain('disabled=""');
  });

  it("separa el historial revocado, muestra su fecha y no permite seleccionarlo", () => {
    const markup = renderToStaticMarkup(
      <DigitalCredentialSelectionView digitalCredentials={digitalCredentials} selected={null}
        submitting={false} onSelect={() => undefined} onSubmit={() => undefined} />,
    );

    expect(markup).toContain("Credenciales digitales");
    expect(markup).toContain("2 vigentes · 1 revocadas");
    expect(markup).toContain("Disponibles para revocar");
    expect(markup).toContain("Credenciales revocadas");
    expect(markup).toContain("Credencial digital revocada 01");
    expect(markup).toContain("Revocada el");
    expect(markup).toContain("25/07/2026");
    expect(markup).toContain(">33<");
    expect(markup).toContain("Se muestran únicamente como historial");
    expect(markup).not.toContain("Seleccionar credencial con índice 33");
    expect(markup.match(/type="radio"/g)?.length).toBe(2);
  });

  it("mantiene la selección explícita con un único credencial", () => {
    const markup = renderToStaticMarkup(
      <DigitalCredentialSelectionView digitalCredentials={[digitalCredentials[0]]} selected={null}
        submitting={false} onSelect={() => undefined}
        onSubmit={() => undefined} onBack={() => undefined} />,
    );

    expect(markup).toContain("Credenciales digitales");
    expect(markup).toContain(">31<");
    expect(markup).toContain("Ninguna credencial seleccionada");
    expect(markup).not.toContain("checked");
    expect(markup).toContain("disabled");
  });

  it("bloquea un segundo envío mientras guarda la selección", () => {
    const markup = renderToStaticMarkup(
		<DigitalCredentialSelectionView digitalCredentials={digitalCredentials}
			selected={digitalCredentials[1]} submitting
        onSelect={() => undefined} onSubmit={() => undefined}
        onBack={() => undefined} />,
    );

    expect(markup).toContain("1 credencial seleccionada");
    expect(markup).toContain("Continuando…");
    expect(markup).toContain("disabled");
  });

  it("anula visualmente una selección local que ya corresponde a una credencial revocada", () => {
    const markup = renderToStaticMarkup(
		<DigitalCredentialSelectionView digitalCredentials={digitalCredentials}
			selected={digitalCredentials[2]} submitting={false}
        onSelect={() => undefined} onSubmit={() => undefined} />,
    );

    expect(markup).toContain("Ninguna credencial seleccionada");
    expect(markup).not.toContain("checked");
    expect(markup).toContain("disabled");
  });

  it("distingue dos credenciales con el mismo UUID mediante su Ã­ndice", () => {
    const repeatedUuid = [
      digitalCredentials[0],
      { ...digitalCredentials[1], digitalCredentialUuid: digitalCredentials[0].digitalCredentialUuid },
    ];
    const markup = renderToStaticMarkup(
      <DigitalCredentialSelectionView digitalCredentials={repeatedUuid}
        selected={repeatedUuid[1]} submitting={false}
        onSelect={() => undefined} onSubmit={() => undefined} />,
    );

    expect(markup.match(/type="radio"/g)?.length).toBe(2);
    expect(markup.match(/checked/g)?.length).toBe(1);
    expect(markup).toContain(`${repeatedUuid[0].digitalCredentialUuid}:31`);
    expect(markup).toContain(`${repeatedUuid[0].digitalCredentialUuid}:32`);
  });

  it("diferencia el paso completado, actual y pendiente sin permitir volver a autenticación", () => {
    const markup = renderToStaticMarkup(
      <RevocationStepper currentStep={2} />,
    );

    expect(markup).toContain("Paso completado 1: Autenticación");
    expect(markup).toContain("Paso actual 2: Selección");
    expect(markup).toContain("Paso pendiente 3: Motivo");
    expect(markup).toContain('aria-current="step"');
    expect(markup.match(/disabled/g)?.length).toBe(5);
  });

  it("permite volver del paso 3 solamente al paso 2", () => {
    const markup = renderToStaticMarkup(
      <RevocationStepper currentStep={3} navigableSteps={[2]} onNavigate={() => undefined} />,
    );

    expect(markup).toContain("Paso completado 1: Autenticación");
    expect(markup).toContain("Paso completado 2: Selección");
    expect(markup).toContain("Paso actual 3: Motivo");
    expect(markup).toContain("Paso pendiente 4: Confirmación");
    expect(markup.match(/disabled/g)?.length).toBe(4);
  });

  it("no persiste UUID ni selección en almacenamiento o URL del navegador", () => {
    const source = readFileSync(resolve(process.cwd(), "components/digital-credential-selection-transition.tsx"), "utf8");
    expect(source).not.toMatch(/localStorage|sessionStorage|URLSearchParams/);
    expect(source).toContain("disabled={!hasValidSelection || submitting}");
    expect(source).toContain("DIGITAL_CREDENTIAL_LIST_TIMEOUT");
    expect(source).toContain("DIGITAL_CREDENTIAL_LIST_UNAVAILABLE");
    expect(source).toContain("DIGITAL_CREDENTIAL_LIST_INVALID_RESPONSE");
    expect(source).toContain("DIGITAL_CREDENTIAL_LIST_IN_PROGRESS");
    expect(source).toContain("getCurrentDigitalCredentials()");
    expect(source).not.toContain("MAX_LISTING_IN_PROGRESS_RETRIES");
    expect(source).not.toContain("replaceDigitalCredentialSelection");
    expect(source).not.toContain("Guardando selección");
  });

	it("regresa al paso 2 y limpia solo la selección cuando la vigencia cambió", () => {
		const flow = readFileSync(resolve(process.cwd(), "components/revocation-flow.tsx"), "utf8");
		const review = readFileSync(resolve(process.cwd(), "components/revocation-review-transition.tsx"), "utf8");

		expect(review).toContain("DIGITAL_CREDENTIAL_SELECTION_STALE");
		expect(review).toContain("onSelectionStale()");
		expect(flow).toContain('setView({ kind: "selection", selectionStale: true })');
		expect(flow).toContain("digitalCredentialUuid: null");
		expect(flow).toContain("statusListIndex: null");
		expect(flow).not.toContain("reasonCode: null,\n\t\t\t\t\t\t\totherReason");
	});
});
