import Image from "next/image";

export function SiteHeader() {
  return (
    <header className="site-header">
      <div className="header-inner">
        <div className="brand">
          <Image
            className="brand-logo"
            src="/images/reniec-logo.png"
            alt="RENIEC - Registro Nacional de Identificación y Estado Civil"
            width={129}
            height={50}
            style={{ width: 180, height: 70 }}
            priority
            unoptimized
          />
        </div>
        <div className="header-context">
          <HeaderItem icon={<ShieldIcon />} title="Servicio ciudadano" text="Cancelación de certificados digitales" />
        </div>
      </div>
    </header>
  );
}

function HeaderItem({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return <div className="header-item"><span>{icon}</span><p><strong>{title}</strong><small>{text}</small></p></div>;
}
function ShieldIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5.5 6v5.2c0 4.2 2.5 7.7 6.5 9.8 4-2.1 6.5-5.6 6.5-9.8V6L12 3Z"/><path d="m9 12 2 2 4-4"/></svg>; }
