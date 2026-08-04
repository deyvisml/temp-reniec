import { afterEach, describe, expect, it, vi } from "vitest";

import {
  getConfirmedRevocationReview,
  previewCurrentRevocation,
} from "@/lib/api/revocation-confirmation";
import { getCurrentDigitalCredentials } from "@/lib/api/digital-credential-listing";

describe("lecturas de la operación actual", () => {
  afterEach(() => vi.restoreAllMocks());

  it("comparte la consulta de credenciales mientras sigue en curso", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ digitalCredentials: [] }), { status: 200 }));

    const [first, second] = await Promise.all([getCurrentDigitalCredentials(), getCurrentDigitalCredentials()]);

    expect(first.data).toEqual(second.data);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("comparte la previsualización del mismo borrador mientras sigue en curso", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ confirmed: false }), { status: 200 }),
    );
    const draft = {
      digitalCredentialUuid: "11111111-1111-4111-8111-111111111111",
      statusListIndex: 31,
      reasonCode: "THEFT" as const,
    };

    const [first, second] = await Promise.all([
      previewCurrentRevocation(draft),
      previewCurrentRevocation(draft),
    ]);

    expect(first.data).toEqual(second.data);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("comparte la recuperación confirmada mientras sigue en curso", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ confirmed: true }), { status: 200 }),
    );

    const [first, second] = await Promise.all([
      getConfirmedRevocationReview(),
      getConfirmedRevocationReview(),
    ]);

    expect(first.data).toEqual(second.data);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

});
