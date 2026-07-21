"use client";

import {
  GoogleReCaptchaCheckbox,
  GoogleReCaptchaProvider,
  useGoogleReCaptcha,
} from "@google-recaptcha/react";
import { useCallback, useEffect, useRef } from "react";

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
  const handleProviderError = useCallback(async () => onError(), [onError]);

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
      <GoogleReCaptchaProvider
        type="v2-checkbox"
        siteKey={RECAPTCHA_SITE_KEY}
        language="es"
        scriptProps={{ async: true, defer: true }}
        onError={handleProviderError}
      >
        <ResettableCheckbox
          resetKey={resetKey}
          onToken={onToken}
          onExpired={onExpired}
          onError={onError}
        />
      </GoogleReCaptchaProvider>
    </div>
  );
}

function ResettableCheckbox({
  resetKey,
  onToken,
  onExpired,
  onError,
}: Omit<RecaptchaCheckboxProps, "disabled">) {
  const { reset } = useGoogleReCaptcha();
  const previousResetKey = useRef(resetKey);

  useEffect(() => {
    if (previousResetKey.current === resetKey) return;
    previousResetKey.current = resetKey;
    reset?.();
  }, [reset, resetKey]);

  return (
    <GoogleReCaptchaCheckbox
      id="initial-query-recaptcha"
      language="es"
      onChange={onToken}
      onExpired={onExpired}
      onError={onError}
    />
  );
}
