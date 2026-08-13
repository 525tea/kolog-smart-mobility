import type { ReactNode } from "react";

type Tone = "blue" | "green" | "gray" | "red" | "amber";

const TONE_CLASSES: Record<Tone, string> = {
  blue: "bg-brand-soft text-brand",
  green: "bg-teal-soft text-teal-deep",
  gray: "bg-divider text-ink-muted",
  red: "bg-danger-soft text-danger",
  amber: "bg-gold-soft text-gold-text",
};

export function Badge({ tone = "gray", children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span className={`inline-flex items-center rounded-full px-[11px] py-[5px] text-[10.5px] font-extrabold ${TONE_CLASSES[tone]}`}>
      {children}
    </span>
  );
}
