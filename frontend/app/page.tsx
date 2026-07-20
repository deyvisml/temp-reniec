import Image from "next/image";

import { DniEligibilityForm } from "@/components/dni-eligibility-form";

export default function HomePage() {
  return (
    <div className="home-shell">
      <section className="hero" aria-labelledby="service-title">
        <div className="hero-copy">
          <p className="service-badge"><LockIcon /> Servicio digital seguro</p>
          <h1 id="service-title">Cancelación de <span>certificados digitales</span></h1>
          <div className="title-mark" aria-hidden="true" />
          <p className="hero-description">
            Cancela los certificados digitales asociados a tu DNI frente a pérdida, robo,
            cambio de equipo o sospecha de uso no autorizado.
          </p>
        </div>
        <div className="hero-visual" aria-hidden="true">
          <Image
            className="hero-image"
            src="/images/home-image.png"
            alt=""
            width={441}
            height={335}
            priority
            unoptimized
          />
        </div>
      </section>

      <section className="consultation-card" aria-label="Consulta de certificados digitales">
        <DniEligibilityForm />
      </section>

      <section className="benefits" aria-label="Características del servicio">
        <Benefit icon={<ShieldIcon />} title="Protegido" text="Tratamos tu información solo para este proceso." />
        <Benefit icon={<BoltIcon />} title="Inmediato" text="La consulta inicial toma solo unos momentos." />
        <Benefit icon={<SealIcon />} title="Servicio RENIEC" text="Canal ciudadano de cancelación de certificados." />
      </section>
    </div>
  );
}

function Benefit({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return <article className="benefit"><div className="benefit-icon">{icon}</div><div><h2>{title}</h2><p>{text}</p></div></article>;
}

function LockIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="6" y="10" width="12" height="10" rx="2"/><path d="M8.5 10V7.5a3.5 3.5 0 0 1 7 0V10M12 14v2"/></svg>; }
function ShieldIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5.5 6v5.2c0 4.2 2.5 7.7 6.5 9.8 4-2.1 6.5-5.6 6.5-9.8V6L12 3Z"/><path d="m9 12 2 2 4-4"/></svg>; }
function BoltIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m13.5 2-8 12h6l-1 8 8-12h-6l1-8Z"/></svg>; }
function SealIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="10" r="6"/><path d="m9 15-1 7 4-2 4 2-1-7M12 7v6M9 10h6"/></svg>; }
