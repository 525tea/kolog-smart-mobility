import type { ButtonHTMLAttributes, ReactNode } from "react";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "secondary" | "outline" | "ghost";
  fullWidth?: boolean;
  children: ReactNode;
}

const VARIANT_CLASSES: Record<NonNullable<ButtonProps["variant"]>, string> = {
  primary: "bg-brand text-white active:bg-brand-deep disabled:bg-hairline disabled:shadow-none",
  secondary: "bg-brand-soft text-brand active:opacity-80",
  outline: "border border-hairline text-ink bg-surface active:bg-base",
  ghost: "text-brand bg-transparent active:bg-brand-soft",
};

export function Button({ variant = "primary", fullWidth, className = "", children, ...rest }: ButtonProps) {
  return (
    <button
      className={`h-[56px] rounded-cta px-5 py-3.5 text-[15px] font-extrabold transition active:scale-[.985] disabled:cursor-not-allowed disabled:opacity-40 ${
        VARIANT_CLASSES[variant]
      } ${fullWidth ? "w-full" : ""} ${className}`}
      {...rest}
    >
      {children}
    </button>
  );
}
