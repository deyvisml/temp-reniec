"use client";

import { useCallback, useEffect, useRef } from "react";
import ReCAPTCHA from "react-google-recaptcha";

type RecaptchaCheckboxProps = {
  resetKey: number;
  disabled: boolean;
  onToken: (token: string) => void;
  onExpired: () => void;
  onError: () => void;
};

export const RECAPTCHA_SITE_KEY = process.env.NEXT_PUBLIC_RECAPTCHA_SITE_KEY?.trim() ?? "";

export function RecaptchaCheckbox({
  resetKey,
  disabled,
  onToken,
  onExpired,
  onError,
}: RecaptchaCheckboxProps) {
  const widgetRef = useRef<ReCAPTCHA>(null);
  const previousResetKey = useRef(resetKey);

  const handleChange = useCallback(
    (token: string | null) => {
      if (token) {
        onToken(token);
        return;
      }
      onExpired();
    },
    [onExpired, onToken],
  );

  useEffect(() => {
    if (previousResetKey.current === resetKey) return;

    previousResetKey.current = resetKey;
    widgetRef.current?.reset();
  }, [resetKey]);

  if (!RECAPTCHA_SITE_KEY) {
    return (
      <p className="m-0 rounded-lg border border-[#e5a6bd] bg-[#fff4f7] px-4 py-3 text-sm text-[#8d0035]" role="alert">
        La verificación de seguridad no está disponible. Inténtalo nuevamente más tarde.
      </p>
    );
  }

  return (
    <div
      className={`mx-auto flex min-h-[78px] max-w-full justify-center overflow-hidden ${disabled ? "pointer-events-none opacity-65" : ""}`}
      aria-label="Verificación de seguridad reCAPTCHA"
      aria-disabled={disabled}
    >
      <ReCAPTCHA
        ref={widgetRef}
        sitekey={RECAPTCHA_SITE_KEY}
        hl="es"
        onChange={handleChange}
        onExpired={onExpired}
        onErrored={onError}
      />
    </div>
  );
}
