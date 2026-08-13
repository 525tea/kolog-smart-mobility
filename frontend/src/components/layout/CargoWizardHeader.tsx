import type { WizardStepKey } from "./WizardTabs";

const TITLES: Record<WizardStepKey, { title: string; subtitle: string }> = {
  register: { title: "화물 등록", subtitle: "Cargo Registration" },
  analysis: { title: "운송속성", subtitle: "Extracted Conditions" },
  mode: { title: "AI 운송수단 추천", subtitle: "Optimal Mode" },
  recommend: { title: "공동화물 추천", subtitle: "Co-load Recommendations" },
  checkout: { title: "예약 및 결제", subtitle: "Checkout" },
  status: { title: "예약 현황", subtitle: "Shipment Status" },
};

export function CargoWizardHeader({ step, onBack }: { step: WizardStepKey; onBack?: () => void }) {
  const { title } = TITLES[step];
  return (
    <header className="border-b border-[#e8edf5] bg-white pt-5 text-[#111c2e]">
      <div className="flex h-12 items-center justify-between px-5">
        <div className="flex items-center gap-2">
          {onBack && (
            <button onClick={onBack} aria-label="뒤로가기" className="-ml-1 rounded-full p-1 active:bg-gray-100">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M15 18l-6-6 6-6" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          )}
          <h1 className="text-[18px] font-black leading-tight">{title}</h1>
        </div>
        <span className="text-[16px] font-black text-brand-700">{step === "register" ? "1 / 4" : step === "analysis" ? "2 / 4" : step === "recommend" ? "3 / 4" : step === "checkout" ? "4 / 4" : ""}</span>
      </div>
      {["register", "analysis", "recommend", "checkout"].includes(step) && <div className="h-1 bg-[#edf1f7]"><div className={`h-full bg-brand-700 ${step === "register" ? "w-1/4" : step === "analysis" ? "w-1/2" : step === "recommend" ? "w-3/4" : "w-full"}`} /></div>}
    </header>
  );
}
