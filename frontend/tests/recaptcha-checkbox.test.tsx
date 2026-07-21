import { readFileSync } from "node:fs";
import { join } from "node:path";
import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { RecaptchaCheckbox } from "@/components/recaptcha-checkbox";

describe("reCAPTCHA checkbox wrapper", () => {
  it("fails closed with an accessible message when the public key is absent", () => {
    const html = renderToStaticMarkup(
      <RecaptchaCheckbox resetKey={0} disabled={false} onToken={() => {}} onExpired={() => {}} onError={() => {}} />,
    );
    expect(html).toContain('role="alert"');
    expect(html).toContain("verificación de seguridad no está disponible");
  });

  it("wires token, expiration, error and a stable reset lifecycle without browser persistence", () => {
    const source = readFileSync(join(process.cwd(), "components", "recaptcha-checkbox.tsx"), "utf8");
    expect(source).toContain('type="v2-checkbox"');
    expect(source).toContain("onChange={onToken}");
    expect(source).toContain("onExpired={onExpired}");
    expect(source).toContain("onError={onError}");
    expect(source).toContain("useGoogleReCaptcha");
    expect(source).toContain("reset?.()");
    expect(source).not.toContain("key={resetKey}");
    expect(source).not.toMatch(/localStorage|sessionStorage|document\.cookie|test-recaptcha-valid/);
  });
});
