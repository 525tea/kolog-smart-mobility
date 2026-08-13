import { useNavigate, useSearchParams } from "react-router-dom";
import { AppHeader } from "../../components/layout/AppHeader";
import { Card } from "../../components/ui/Card";

const OPTIONS = [
  {
    mode: "CO_LOAD",
    icon: "🤝",
    title: "공동화물로 보내기",
    description: "같은 노선의 화물과 함께 적재해 운임을 낮춰요.",
    badges: ["최대 35% 절감", "AI 매칭", "철도 공동운송"],
    accent: true,
  },
  {
    mode: "INDIVIDUAL",
    icon: "📦",
    title: "개별운송으로 보내기",
    description: "내 화물의 납기와 조건을 우선해 단독 운송안을 비교해요.",
    badges: ["빠른 배차", "철도·도로 비교", "Door-to-Door"],
    accent: false,
  },
] as const;

export function CargoGatewayPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  function registrationTarget(mode: string) {
    const params = new URLSearchParams(searchParams);
    params.set("mode", mode);
    return `/cargo/new/form?${params.toString()}`;
  }

  return (
    <div className="flex min-h-full flex-col">
      <AppHeader title="화물 등록" subtitle="Choose Shipping Type" onBack={() => navigate("/home")} />
      <main className="flex flex-1 flex-col px-5 py-6">
        <div>
          <p className="text-sm font-semibold text-brand-700">어떻게 보내실 건가요?</p>
          <h1 className="mt-1 text-2xl font-black leading-tight text-gray-950">
            운송 방식을 먼저 선택하면
            <br />맞는 견적 흐름으로 안내해요
          </h1>
          <p className="mt-2 text-sm leading-6 text-gray-500">분석 결과에서 철도·도로를 다시 비교하고 변경할 수도 있어요.</p>
        </div>

        <div className="mt-7 flex flex-col gap-4">
          {OPTIONS.map((option) => (
            <button
              key={option.mode}
              type="button"
              className="text-left"
              onClick={() => navigate(registrationTarget(option.mode))}
            >
              <Card
                className={`relative overflow-hidden border-2 p-5 transition-transform active:scale-[0.99] ${
                  option.accent ? "border-brand-700 bg-brand-50" : "border-gray-200 bg-white"
                }`}
              >
                {option.accent && (
                  <span className="absolute right-4 top-4 rounded-full bg-brand-700 px-2.5 py-1 text-xs font-bold text-white">추천</span>
                )}
                <div className={`flex size-12 items-center justify-center rounded-2xl text-2xl ${option.accent ? "bg-white" : "bg-gray-50"}`}>
                  {option.icon}
                </div>
                <h2 className="mt-4 text-lg font-black text-gray-950">{option.title}</h2>
                <p className="mt-1 text-sm leading-6 text-gray-500">{option.description}</p>
                <div className="mt-4 flex flex-wrap gap-1.5">
                  {option.badges.map((badge) => (
                    <span key={badge} className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-brand-700 shadow-sm">
                      {badge}
                    </span>
                  ))}
                </div>
                <p className="mt-5 text-right text-sm font-bold text-brand-700">선택하고 등록하기 →</p>
              </Card>
            </button>
          ))}
        </div>

        <div className="mt-auto rounded-2xl bg-gray-50 p-4 text-xs leading-5 text-gray-500">
          <p className="font-bold text-gray-700">잘 모르겠다면?</p>
          <p className="mt-1">공동화물을 선택해도 AI가 납기·비용·온도 조건을 분석해 개별운송이 더 적합하면 알려드려요.</p>
        </div>
      </main>
    </div>
  );
}
