import type { HTMLAttributes, ReactNode } from "react";

export function Card({ className = "", children, ...rest }: HTMLAttributes<HTMLDivElement> & { children: ReactNode }) {
  const backgroundClass = /(?:^|\s)bg-/.test(className) ? "" : "bg-surface";
  return (
    <div className={`rounded-card ${backgroundClass} p-[18px] shadow-card ${className}`} {...rest}>
      {children}
    </div>
  );
}
