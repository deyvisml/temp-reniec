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
  description: "Consulta e inicia la cancelación de certificados digitales asociados a tu DNI.",
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

          <main id="main-content" tabIndex={-1} className="main-content">{children}</main>

          <SiteFooter />
        </div>
      </body>
    </html>
  );
}
