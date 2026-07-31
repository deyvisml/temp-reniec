import { afterEach, describe, expect, it, vi } from "vitest";

import {
  getConfirmedCancellationReview,
  previewCurrentCancellation,
} from "@/lib/api/cancellation-confirmation";
import { getCurrentCertificates } from "@/lib/api/certificate-listing";

describe("lecturas de la operación actual", () => {
  afterEach(() => vi.restoreAllMocks());

  it("comparte la consulta de certificados mientras sigue en curso", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ certificates: [] }), { status: 200 }));

    const [first, second] = await Promise.all([getCurrentCertificates(), getCurrentCertificates()]);

    expect(first.data).toEqual(second.data);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("comparte la previsualización del mismo borrador mientras sigue en curso", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ confirmed: false }), { status: 200 }),
    );
    const draft = {
      certificateUuid: "11111111-1111-4111-8111-111111111111",
      reasonCode: "THEFT" as const,
    };

    const [first, second] = await Promise.all([
      previewCurrentCancellation(draft),
      previewCurrentCancellation(draft),
    ]);

    expect(first.data).toEqual(second.data);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("comparte la recuperación confirmada mientras sigue en curso", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ confirmed: true }), { status: 200 }),
    );

    const [first, second] = await Promise.all([
      getConfirmedCancellationReview(),
      getConfirmedCancellationReview(),
    ]);

    expect(first.data).toEqual(second.data);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

});
