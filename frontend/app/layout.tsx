import type { Metadata } from "next";
import type { ReactNode } from "react";

import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Cancelación de certificados digitales",
    template: "%s | Cancelación de certificados digitales",
  },
  description: "Base técnica del sistema de cancelación de certificados digitales.",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="es">
      <body>
        <a className="skip-link" href="#main-content">
          Ir al contenido principal
        </a>

        <div className="flex min-h-dvh flex-col">
          <SiteHeader />

          <div
            id="global-messages"
            className="sr-only"
            aria-live="polite"
            aria-atomic="true"
          />

          <main
            id="main-content"
            tabIndex={-1}
            className="flex flex-1 items-center py-10 sm:py-14 lg:py-18"
          >
            <div className="mx-auto w-full max-w-6xl px-4 sm:px-6 lg:px-8">
              {children}
            </div>
          </main>

          <SiteFooter />
        </div>
      </body>
    </html>
  );
}
