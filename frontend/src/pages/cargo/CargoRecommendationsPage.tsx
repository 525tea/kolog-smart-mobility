import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { getCandidates } from "../../api/consolidation";
import { getCargo } from "../../api/cargo";
import { ApiError } from "../../api/client";
import type {
  CargoResponse,
  ConsolidationCandidateResponse,
} from "../../types";
import { getRuntimeConfig } from "../../api/app";

export function CargoRecommendationsPage() {
  const { cargoId } = useParams();
  const navigate = useNavigate();
  const [cargo, setCargo] = useState<CargoResponse | null>(null);
  const [candidates, setCandidates] = useState<
    ConsolidationCandidateResponse[]
  >([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (!cargoId) return;
    Promise.all([getCargo(Number(cargoId)), getCandidates(Number(cargoId))])
      .then(([cargoRow, rows]) => {
        const sorted = [...rows].sort(
          (a, b) => b.suitabilityScore - a.suitabilityScore,
        );
        setCargo(cargoRow);
        setCandidates(sorted);
        setSelectedId(sorted.find((row) => row.suitabilityScore >= 50)?.consolidatedCargoId ?? null);
      })
      .catch((cause) =>
        setError(
          cause instanceof ApiError
            ? cause.message
            : "추천 조회에 실패했습니다.",
        ),
      )
      .finally(() => setLoading(false));
  }, [cargoId]);

  const selected = candidates.filter((row) => row.consolidatedCargoId === selectedId);
  const totalCbm =
    (cargo?.volumeCbm ?? 0) +
    selected.reduce((sum, row) => sum + row.recruitedWeightKg / 250, 0);
  const hasKnownTotalCbm = cargo?.volumeCbm != null;
  const capacity = getRuntimeConfig()?.containerCapacityCbm ?? 67;
  const best = selected[0] ?? candidates[0];
  const savingsPercent = best ? Math.round(best.appliedDiscountRate * 100) : 0;
  const toggle = (id: number) => setSelectedId((current) => current === id ? null : id);

  return (
    <div className="flex min-h-full flex-col bg-[#f4f7fb]">
      <header className="border-b border-[#e7ecf4] bg-white pt-5">
        <div className="flex h-12 items-center gap-3 px-5">
          <button onClick={() => navigate(-1)} aria-label="뒤로가기">
            ←
          </button>
          <h1 className="text-[18px] font-black">공동화물 추천</h1>
          <span className="ml-auto text-[16px] font-black text-brand-700">
            4 / 4
          </span>
        </div>
      </header>
      <main className="flex flex-1 flex-col gap-4 overflow-y-auto px-5 py-4">
        {cargo && (
          <div className="flex gap-2">
            <Tag>
              {cargo.originStation} → {cargo.destinationStation}
            </Tag>
            <Tag>
              {new Date(cargo.desiredDate).toLocaleDateString("ko-KR", {
                month: "numeric",
                day: "numeric",
              })}{" "}
              출발
            </Tag>
            <Tag>
              {cargo.temperatureCondition === "FROZEN"
                ? "냉동"
                : cargo.temperatureCondition === "REFRIGERATED"
                  ? "냉장"
                  : "상온"}
            </Tag>
          </div>
        )}
        <section className="rounded-[24px] bg-[#3049bd] p-4 text-white">
          <div className="flex items-end justify-between">
            <span className="text-[11px] font-bold text-white/65">
              선택 화물 합계
            </span>
            <strong className="text-[25px]">
              {hasKnownTotalCbm ? `${totalCbm.toFixed(1)} CBM` : "부피 확인 필요"}
            </strong>
          </div>
          <div className="mt-3 h-2 rounded-full bg-white/20">
            <div
              className="h-full rounded-full bg-white"
              style={{
                width: `${hasKnownTotalCbm ? Math.min(100, (totalCbm / capacity) * 100) : 0}%`,
              }}
            />
          </div>
          <div className="mt-2 flex justify-between text-[10px] text-white/70">
            <span>40ft 컨테이너 기준 {capacity} CBM</span>
            <span>{hasKnownTotalCbm ? `${Math.round((totalCbm / capacity) * 100)}% 적재` : "부피 입력 후 계산"}</span>
          </div>
        </section>
        {loading && (
          <p className="py-10 text-center text-sm text-[#8a96aa]">
            추천 후보를 찾고 있어요…
          </p>
        )}
        {error && (
          <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-600">
            {error}
          </p>
        )}
        {!loading && candidates.length === 0 && (
          <p className="py-10 text-center text-sm text-[#8a96aa]">
            조건에 맞는 공동화물 후보가 없어요.
          </p>
        )}
        {candidates.length > 0 && (
          <section>
            <h2 className="mb-3 text-[13px] font-black">
              추천 후보 {candidates.length}건
            </h2>
            <div className="space-y-3">
              {candidates.map((row, index) => {
                const checked = selectedId === row.consolidatedCargoId;
                const blocked = row.suitabilityScore < 50;
                return (
                  <button
                    type="button"
                    key={row.consolidatedCargoId}
                    disabled={blocked}
                    onClick={() => toggle(row.consolidatedCargoId)}
                    className={`flex w-full items-center gap-3 rounded-[24px] bg-white p-4 text-left ${blocked ? "border border-dashed border-[#cfd7e4] opacity-60" : checked ? "border-2 border-brand-600" : "border border-[#dce3ee]"}`}
                  >
                    <span className="min-w-0 flex-1">
                      <strong className="block text-[14px] text-[#172235]">
                        {index === 0
                          ? "최적 공동화물"
                          : `${row.trainNumber} 공동화물`}
                      </strong>
                      <span className="mt-1 block text-[10px] font-semibold text-[#8490a4]">
                        공동화물 #{row.consolidatedCargoId} ·{" "}
                        {row.originStation}{" "}
                        {new Date(row.departureAt).toLocaleDateString("ko-KR", {
                          month: "numeric",
                          day: "numeric",
                        })}
                      </span>
                      <span className="mt-2 flex gap-5 text-[12px] font-bold text-[#556278]">
                        <span>
                          {(row.recruitedWeightKg / 250).toFixed(1)} CBM
                        </span>
                        <span>{row.recruitedWeightKg.toLocaleString()} kg</span>
                        <span
                          className={
                            blocked ? "text-orange-500" : "text-brand-700"
                          }
                        >
                          {blocked
                            ? "혼재 불가"
                            : row.suitabilityScore >= 80
                              ? "조건 적합"
                              : "집하 추가"}
                        </span>
                      </span>
                    </span>
                    <span
                      className={`size-6 shrink-0 rounded-full border-2 ${checked ? "border-brand-600 bg-brand-600 shadow-[inset_0_0_0_6px_white]" : "border-[#d2d9e5]"}`}
                    />
                  </button>
                );
              })}
            </div>
          </section>
        )}
      </main>
      <footer className="flex items-center gap-4 border-t border-[#e7ecf4] bg-white px-5 pb-7 pt-4">
        {savingsPercent > 0 && <div className="shrink-0">
          <p className="text-[10px] font-bold text-[#8a96aa]">예상 절감</p>
          <strong className="text-[18px] text-[#0ba995]">-{savingsPercent}%</strong>
        </div>}
        <div className="min-w-0 flex-1">
          <Button
            fullWidth
            disabled={!selected[0]}
            onClick={() =>
              selected[0] &&
              navigate(
                `/consolidated-cargos/${selected[0].consolidatedCargoId}?cargoId=${cargoId}`,
              )
            }
          >
            선택한 공동화물 확인
          </Button>
          <p className="mt-2 text-center text-[11px] font-semibold text-[#7a879c]">열차·운송수단 확인 후 예약 및 결제로 이동해요</p>
        </div>
      </footer>
    </div>
  );
}

function Tag({ children }: { children: React.ReactNode }) {
  return (
    <span className="rounded-full bg-[#eaf0ff] px-3 py-2 text-[10px] font-black text-brand-700">
      {children}
    </span>
  );
}
