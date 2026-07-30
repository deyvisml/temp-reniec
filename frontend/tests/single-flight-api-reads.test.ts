import { afterEach, describe, expect, it, vi } from "vitest";

import { getCurrentCertificates } from "@/lib/api/certificate-listing";
import { getCurrentCancellationReason } from "@/lib/api/cancellation-reason";

describe("lecturas de la operación actual", () => {
  afterEach(() => vi.restoreAllMocks());

  it("comparte la consulta de certificados mientras sigue en curso", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ certificates: [] }), { status: 200 }));

    const [first, second] = await Promise.all([getCurrentCertificates(), getCurrentCertificates()]);

    expect(first.data).toEqual(second.data);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("comparte la consulta del motivo mientras sigue en curso", async () => {
    const fetchMock = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ reasonCode: null, otherReason: null }), { status: 200 }));

    const [first, second] = await Promise.all([getCurrentCancellationReason(), getCurrentCancellationReason()]);

    expect(first.data).toEqual(second.data);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
