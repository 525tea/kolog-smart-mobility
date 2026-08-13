const STEPS = [
  { key: "register", label: "등록" },
  { key: "analysis", label: "AI 분석" },
  { key: "mode", label: "운송수단" },
  { key: "recommend", label: "추천" },
  { key: "checkout", label: "예약" },
  { key: "status", label: "현황" },
] as const;

export type WizardStepKey = (typeof STEPS)[number]["key"];

export function WizardTabs({ current }: { current: WizardStepKey }) {
  const currentIndex = STEPS.findIndex((s) => s.key === current);
  return (
    <div className="mt-3 flex gap-1.5 px-5 pb-3">
      {STEPS.map((step, index) => (
        <div key={step.key} className="flex-1">
          <div className={`h-[3px] rounded-full ${index <= currentIndex ? "bg-brand-700" : "bg-[#e8edf5]"}`} />
        </div>
      ))}
    </div>
  );
}

export { STEPS };
