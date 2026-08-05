"use client";

import Image from "next/image";

import { DniAvailabilityForm } from "@/components/dni-availability-form";

const iconStroke =
    "fill-none stroke-current stroke-[1.8] [stroke-linecap:round] [stroke-linejoin:round]";

export function RevocationEntry({ onContinue }: { onContinue: () => void }) {
    return (
        <div className="before:top-[50px] after:top-[340px] after:right-[-320px] before:left-[-350px] before:absolute after:absolute relative before:shadow-[0_0_0_35px_#c6d8fa22,0_0_0_70px_#c6d8fa17] after:shadow-[0_0_0_35px_#c6d8fa22,0_0_0_70px_#c6d8fa17] px-5 max-[480px]:px-[14px] pt-[52px] max-[480px]:pt-7 max-[800px]:pt-[34px] pb-[38px] before:border after:border before:border-[#bad0fb55] after:border-[#bad0fb55] before:rounded-full after:rounded-full before:size-[520px] after:size-[450px] overflow-hidden before:content-[''] after:content-[''] before:pointer-events-none after:pointer-events-none">
            <section
                className="z-1 relative items-center gap-7 max-[800px]:gap-2 grid grid-cols-[1.3fr_0.7fr] max-[800px]:grid-cols-1 mx-auto w-full max-w-[920px] min-h-[280px] max-[800px]:text-center"
                aria-labelledby="service-title"
            >
                <div className="max-[800px]:flex max-[800px]:flex-col max-[800px]:items-center">
                    <h1
                        id="service-title"
                        className="[&_span]:block m-0 max-w-[620px] font-black text-[#061d59] [&_span]:text-reniec-red max-[480px]:text-3xl max-[800px]:text-4xl text-5xl leading-none tracking-tight min-[801px]:[&_span]:whitespace-nowrap"
                    >
                        Revocación de <span>credenciales verificables</span>
                    </h1>
                    <div
                        className="bg-reniec-red my-[18px] min-[801px]:mt-[22px] rounded-sm w-9 h-1"
                        aria-hidden="true"
                    />
                </div>

                <div
                    className="flex justify-end max-[800px]:justify-center max-[800px]:mt-5 max-[800px]:mb-8 w-full"
                    aria-hidden="true"
                >
                    <Image
                        className="block w-full max-w-[230px] max-[480px]:max-w-[187px] h-auto object-contain"
                        src="/images/peru-pass-logo.png"
                        alt=""
                        width={875}
                        height={880}
                        priority
                        unoptimized
                    />
                </div>
            </section>

            <section
                className="z-2 relative bg-[#ffffffed] shadow-[0_24px_70px_-30px_#001b6055] backdrop-blur-[10px] mx-auto mt-[26px] max-[800px]:mt-0 px-16 max-[480px]:px-[18px] max-[800px]:px-6 max-[480px]:py-[25px] max-[800px]:py-[30px] pt-[34px] pb-7 border border-[#e0e7f2] rounded-[18px] max-[480px]:rounded-[14px] w-full max-w-[920px]"
                aria-label="Consulta de credenciales verificables"
            >
                <DniAvailabilityForm onContinue={onContinue} />
            </section>

            <section
                className="z-1 relative grid grid-cols-2 max-[800px]:grid-cols-1 mx-auto mt-[25px] w-full max-w-[560px]"
                aria-label="Características del servicio"
            >
                <Benefit
                    icon={<ShieldIcon />}
                    title="Protegido"
                    text="Tratamos tu información solo para este proceso."
                />
                <Benefit
                    icon={<BoltIcon />}
                    title="Inmediato"
                    text="La consulta inicial toma solo unos momentos."
                />
            </section>
        </div>
    );
}

function Benefit({
    icon,
    title,
    text,
}: {
    icon: React.ReactNode;
    title: string;
    text: string;
}) {
    return (
        <article className="flex items-center gap-[15px] px-7 max-[800px]:px-[22px] py-[18px] max-[800px]:py-4 border-[#dfe6f1] last:border-0 border-r max-[800px]:border-r-0 max-[800px]:border-b max-[800px]:last:border-b-0">
            <div className="place-items-center grid bg-[#f5f8fe] border border-[#dae4f5] rounded-full size-[52px] [&_svg]:size-7 text-[#073c9d] shrink-0">
                {icon}
            </div>
            <div>
                <h2 className="m-0 text-sm">{title}</h2>
                <p className="mt-[5px] mb-0 text-[#5f7096] text-xs leading-[1.45]">
                    {text}
                </p>
            </div>
        </article>
    );
}

function ShieldIcon() {
    return (
        <svg className={iconStroke} viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 3 5.5 6v5.2c0 4.2 2.5 7.7 6.5 9.8 4-2.1 6.5-5.6 6.5-9.8V6L12 3Z" />
            <path d="m9 12 2 2 4-4" />
        </svg>
    );
}
function BoltIcon() {
    return (
        <svg className={iconStroke} viewBox="0 0 24 24" aria-hidden="true">
            <path d="m13.5 2-8 12h6l-1 8 8-12h-6l1-8Z" />
        </svg>
    );
}
