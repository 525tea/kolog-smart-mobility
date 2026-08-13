import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";

interface AppHeaderProps {
  title: string;
  subtitle?: string;
  onBack?: () => void;
  right?: ReactNode;
}

export function AppHeader({ title, subtitle, onBack, right }: AppHeaderProps) {
  const navigate = useNavigate();
  return (
    <header className="border-b border-[#e8edf5] bg-white px-5 pt-5 text-[#111c2e]">
      <div className="flex h-12 items-center justify-between">
        <div className="flex items-center gap-2">
          {onBack && (
            <button
              onClick={() => (onBack ? onBack() : navigate(-1))}
              aria-label="뒤로가기"
              className="-ml-1 rounded-full p-1 active:bg-gray-100"
            >
              <BackIcon />
            </button>
          )}
          <h1 className="text-[18px] font-black leading-tight">{title}</h1>
        </div>
        {right}
      </div>
      {subtitle && <span className="sr-only">{subtitle}</span>}
    </header>
  );
}

function BackIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M15 18l-6-6 6-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
