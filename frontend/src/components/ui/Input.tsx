import type { InputHTMLAttributes, LabelHTMLAttributes, TextareaHTMLAttributes } from "react";

export function Field({ label, children, ...rest }: { label: string; children: React.ReactNode } & LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    <label className="block" {...rest}>
      <span className="mb-1.5 block text-sm font-extrabold text-brand-deep">{label}</span>
      {children}
    </label>
  );
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className={`h-12 w-full rounded-chip border border-hairline bg-surface px-3.5 text-[15px] text-ink outline-none focus:border-brand focus:ring-2 focus:ring-brand-soft ${props.className ?? ""}`}
    />
  );
}

export function TextArea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      {...props}
      className={`w-full rounded-chip border border-hairline bg-surface px-3.5 py-3 text-[15px] text-ink outline-none focus:border-brand focus:ring-2 focus:ring-brand-soft ${props.className ?? ""}`}
    />
  );
}
