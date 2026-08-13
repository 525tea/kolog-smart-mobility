import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { getCargo } from "../../api/cargo";
import type { CargoResponse } from "../../types";
import { ApiError } from "../../api/client";

export function ModeRecommendationPage() {
  const { cargoId } = useParams();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const groupId = params.get("groupId");
  const [cargo, setCargo] = useState<CargoResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (cargoId)
      getCargo(Number(cargoId))
        .then(setCargo)
        .catch((cause) =>
          setError(
            cause instanceof ApiError
              ? cause.message
              : "화물 정보를 불러오지 못했습니다.",
          ),
        );
  }, [cargoId]);
  if (!cargo)
    return (
      <div className="grid min-h-full place-items-center text-sm text-gray-400">
        {error ?? "운송수단을 분석하는 중…"}
      </div>
    );
  const weight = cargo.weightKg ?? 200;
  const rail = Math.round(
    weight * 500 * 0.65 * (1 + cargo.surchargeRate) +
      cargo.fixedPowerFeeKrw +
      5700,
  );
  const road = Math.max(
    98000,
    Math.round(weight * 790 * (1 + cargo.surchargeRate)),
  );
  const saving = Math.max(0, road - rail);
  return (
    <div className="flex min-h-full flex-col bg-[#5a49d5] text-white">
      <header className="flex h-[72px] items-center gap-3 px-5 pt-3">
        <button onClick={() => navigate(-1)}>←</button>
        <h1 className="text-[18px] font-black">AI 운송수단 추천</h1>
      </header>
      <main className="flex flex-1 flex-col gap-5 overflow-y-auto px-5">
        <h2 className="text-[24px] font-black leading-[1.45]">
          비용 · 납기 · 운송조건을
          <br />
          모두 고려한 결과입니다
        </h2>
        <section className="rounded-[28px] bg-white p-5 text-[#172235]">
          <div className="flex justify-between">
            <div>
              <p className="text-[10px] font-black text-[#6b55dc]">
                추천 운송수단
              </p>
              <h3 className="mt-2 text-[20px] font-black">
                철도 공동운송 · 3061 화물열차
              </h3>
            </div>
            <span className="h-fit rounded-full bg-[#eeeafd] px-3 py-1 text-[9px] font-black text-[#624ed8]">
              적합도 92점
            </span>
          </div>
          <div className="mt-4 h-2 rounded-full bg-[#edf1f6]">
            <div className="h-full w-[92%] rounded-full bg-[#624ed8]" />
          </div>
          <div className="mt-4 grid grid-cols-3 gap-2">
            <Metric label="비용" value={`-${saving.toLocaleString()}원`} />
            <Metric label="납기" value="희망일 충족" />
            <Metric
              label="조건"
              value={
                cargo.temperatureCondition === "FROZEN"
                  ? "냉장 가능"
                  : "조건 충족"
              }
            />
          </div>
        </section>
        <section>
          <h3 className="mb-3 text-[12px] font-black text-white/75">
            판단 근거
          </h3>
          <div className="rounded-[24px] border border-white/25 bg-white/10 p-4 text-[11px] font-semibold leading-7 text-white/85">
            <p>● 희망일 기준 화차 잔여공간 확보</p>
            <p>● 공동운송 매칭으로 CBM 단가 절감</p>
            <p>● 요청 운송조건을 만족하는 냉장 컨테이너 편성</p>
          </div>
        </section>
        <button className="flex items-center rounded-[22px] border border-white/25 bg-white/10 p-4 text-left">
          <span>
            <strong className="text-[13px]">도로 단독운송 (대안)</strong>
            <span className="mt-1 block text-[10px] text-white/65">
              4시간 빠르지만 +{saving.toLocaleString()}원
            </span>
          </span>
          <span className="ml-auto text-[10px] font-black">선택</span>
        </button>
      </main>
      <footer className="px-5 pb-7 pt-4">
        <button
          onClick={() =>
            navigate(
              groupId
                ? `/cargo/${cargo.id}/reservation/${groupId}`
                : `/cargo/${cargo.id}/recommendations`,
            )
          }
          className="h-[54px] w-full rounded-[16px] bg-white text-sm font-black text-[#5a49d5]"
        >
          철도로 예약 진행
        </button>
      </footer>
    </div>
  );
}
function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[18px] bg-[#f3f4f9] p-3">
      <p className="text-[9px] font-bold text-[#8995a8]">{label}</p>
      <strong className="mt-1 block text-[12px]">{value}</strong>
    </div>
  );
}
