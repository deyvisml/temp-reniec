import type { Metadata } from "next";
import type { ReactNode } from "react";

import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

import "sweetalert2/dist/sweetalert2.min.css";
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
      <body className="min-h-dvh bg-[radial-gradient(circle_at_20%_18%,#edf4ff_0,transparent_32rem)] bg-[#f9fbff] font-sans text-[#071847] antialiased">
        <a
          className="fixed top-3 left-3 z-[100] -translate-y-[160%] rounded-lg bg-white px-4 py-3 font-extrabold text-[#081a45] shadow-[0_12px_32px_#00143c33] transition-transform focus:translate-y-0 focus-visible:outline-3 focus-visible:outline-offset-3 focus-visible:outline-[#f4b400]"
          href="#main-content"
        >
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

          <main id="main-content" tabIndex={-1} className="flex-1">{children}</main>

          <SiteFooter />
        </div>
      </body>
    </html>
  );
}
