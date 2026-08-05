"use client";

import Image from "next/image";
import { usePathname } from "next/navigation";

export function SiteHeader() {
  const pathname = usePathname();
  const isInternalFlow = pathname === "/revocacion" || pathname === "/autorizacion";

  return (
    <header className="relative z-5 bg-[linear-gradient(110deg,#001440,#002b79_52%,#003ba1)] text-white shadow-[0_8px_30px_#00143c1f]">
      <div
        className={`mx-auto flex min-h-[84px] w-[min(700px,calc(100%_-_28px))] items-center gap-3 min-[801px]:min-h-[104px] min-[801px]:w-[min(1320px,calc(100%_-_40px))] ${
          isInternalFlow ? "justify-between" : "justify-center min-[481px]:justify-between"
        }`}
      >
        <div className="flex shrink-0 items-center">
          <Image
            className="block h-[50px] w-[129px] object-contain"
            src="/images/reniec-logo.png"
            alt="RENIEC - Registro Nacional de Identificación y Estado Civil"
            width={129}
            height={50}
            priority
            unoptimized
          />
        </div>

        {isInternalFlow ? (
          <div
            id="internal-flow-header-slot"
            className="flex min-h-11 items-center"
          />
        ) : (
          <div className="hidden items-center min-[801px]:flex">
            <HeaderItem
              icon={<ShieldIcon />}
              title="Servicio del ciudadano"
              text="Revocación de credenciales verificables"
            />
          </div>
        )}
      </div>
    </header>
  );
}

function HeaderItem({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return (
    <div className="flex items-center gap-3 px-[26px]">
      <span className="size-9 [&_svg]:size-full">{icon}</span>
      <p>
        <strong className="block text-sm">{title}</strong>
        <small className="mt-1 block text-xs text-[#dce8ff]">{text}</small>
      </p>
    </div>
  );
}

function ShieldIcon() {
  return (
    <svg
      className="fill-none stroke-current stroke-[1.5] [stroke-linecap:round] [stroke-linejoin:round]"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <path d="M12 3 5.5 6v5.2c0 4.2 2.5 7.7 6.5 9.8 4-2.1 6.5-5.6 6.5-9.8V6L12 3Z" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  );
}
