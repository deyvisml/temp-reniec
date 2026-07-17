export function SiteHeader() {
  return (
    <header className="site-header">
      <div className="header-inner">
        <div className="brand" aria-label="RENIEC">
          <strong>RENIEC</strong>
          <span>Registro Nacional de Identificación<br />y Estado Civil</span>
          <div className="brand-mark" aria-hidden="true"><i /><i /><i /></div>
        </div>
        <div className="header-context">
          <HeaderItem icon={<ShieldIcon />} title="Servicio ciudadano" text="Cancelación de certificados digitales" />
          <HeaderItem icon={<HelpIcon />} title="¿Necesitas ayuda?" text="Revisa los mensajes durante el proceso" />
        </div>
      </div>
    </header>
  );
}

function HeaderItem({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return <div className="header-item"><span>{icon}</span><p><strong>{title}</strong><small>{text}</small></p></div>;
}
function ShieldIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5.5 6v5.2c0 4.2 2.5 7.7 6.5 9.8 4-2.1 6.5-5.6 6.5-9.8V6L12 3Z"/><path d="m9 12 2 2 4-4"/></svg>; }
function HelpIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M9.7 9a2.5 2.5 0 1 1 3.5 2.3c-.8.4-1.2.9-1.2 1.7M12 17h.01"/></svg>; }
