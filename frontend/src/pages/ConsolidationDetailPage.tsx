import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { getConsolidationDetail } from "../api/consolidation";
import { getCargo } from "../api/cargo";
import type { CargoResponse, ConsolidationDetailResponse } from "../types";

export function ConsolidationDetailPage() {
  const { groupId } = useParams();
  const [searchParams] = useSearchParams();
  const cargoId = searchParams.get("cargoId");
  const navigate = useNavigate();
  const [detail, setDetail] = useState<ConsolidationDetailResponse | null>(
    null,
  );
  const [cargo, setCargo] = useState<CargoResponse | null>(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    if (!groupId) return;
    Promise.all([
      getConsolidationDetail(Number(groupId)),
      cargoId ? getCargo(Number(cargoId)) : Promise.resolve(null),
    ])
      .then(([group, cargoRow]) => {
        setDetail(group);
        setCargo(cargoRow);
      })
      .finally(() => setLoading(false));
  }, [groupId, cargoId]);
  if (loading || !detail)
    return (
      <div className="grid min-h-full place-items-center text-sm text-[#8a96aa]">
        공동화물을 불러오는 중…
      </div>
    );
  const cargoVolume = cargo?.volumeCbm ?? 0;
  const hasKnownTotalVolume = cargo?.volumeCbm != null && detail.participants.every((row) => row.volumeCbm != null);
  const participantVolume = detail.participants.reduce(
    (sum, row) => sum + (row.volumeCbm ?? 0),
    0,
  );
  const totalVolume = cargoVolume + participantVolume;
  const estimatedFare = Math.round((cargo?.weightKg ?? 0) * 500 * (1 - 0.23));

  return (
    <div className="flex min-h-full flex-col bg-white">
      <header className="border-b border-[#e7ecf4] bg-white pt-5">
        <div className="flex h-12 items-center gap-3 px-5">
          <button onClick={() => navigate(-1)} aria-label="뒤로가기">
            ←
          </button>
          <h1 className="text-[18px] font-black">공동화물 상세</h1>
        </div>
      </header>
      <main className="flex flex-1 flex-col gap-4 overflow-y-auto px-5 py-4">
        <section className="rounded-[28px] bg-[#eaf0ff] p-5">
          <p className="text-[11px] font-black text-[#3150c8]">
            40ft {cargo?.temperatureCondition === "FROZEN" ? "냉장 " : ""}
            컨테이너 · 적재 계획
          </p>
          <div className="mt-4 flex h-[76px] overflow-hidden rounded-[20px] border border-[#cfdaf5]">
            <span
              className="grid bg-brand-600 px-4 text-center text-[11px] font-black text-white"
              style={{ width: "35%", placeItems: "center" }}
            >
              내 화물
            </span>
            <span
              className="grid bg-[#ef656b] px-3 text-center text-[10px] font-black text-white"
              style={{ width: "20%", placeItems: "center" }}
            >
              공동화물
            </span>
            <span
              className="grid bg-[#f5a623] px-3 text-center text-[10px] font-black text-white"
              style={{ width: "20%", placeItems: "center" }}
            >
              추가화물
            </span>
            <span className="grid flex-1 place-items-center text-[10px] font-bold text-[#8390a4]">
              여유
            </span>
          </div>
          <div className="mt-4 flex justify-between">
            <div>
              <p className="text-[10px] font-bold text-[#6f7c91]">총 CBM</p>
              <strong className="text-[22px] text-[#3049bd]">
                {hasKnownTotalVolume ? totalVolume.toFixed(1) : "확인 필요"}
              </strong>
            </div>
            <div className="text-right">
              <p className="text-[10px] font-bold text-[#6f7c91]">
                예상 운임 (내 분담)
              </p>
              <strong className="text-[22px] text-[#3049bd]">
                {estimatedFare.toLocaleString()}원
              </strong>
            </div>
          </div>
        </section>
        <section>
          <h2 className="mb-3 text-[13px] font-black">함께 운송되는 화물</h2>
          <div className="space-y-3">
            {cargo && (
              <CargoLine
                name={cargo.cargoName}
                company="내 화물"
                volume={cargo.volumeCbm}
                weight={cargo.weightKg}
                mine
              />
            )}
            {detail.participants.map((row, index) => (
              <CargoLine
                key={`${row.companyName}-${index}`}
                name={row.cargoName}
                company={row.companyName}
                volume={row.volumeCbm}
                weight={row.weightKg}
              />
            ))}
            {detail.participants.length === 0 && (
              <CargoLine
                name="추천 공동화물"
                company="모집 후보"
                volume={null}
                weight={detail.recruitedWeightKg}
              />
            )}
          </div>
        </section>
        <section className="rounded-[24px] bg-[#f3f6fb] p-4">
          <h2 className="mb-3 text-[12px] font-black">운임 분담 방식</h2>
          <PriceRow
            label="CBM 비중 기준"
            value={
              hasKnownTotalVolume && totalVolume > 0
                ? `${Math.round((cargoVolume / totalVolume) * 100)}%`
                : "확인 중"
            }
          />
          <PriceRow
            label="단독 운송 시"
            value={`${Math.round(estimatedFare / 0.77).toLocaleString()}원`}
            muted
          />
          <PriceRow
            label="공동운송 적용"
            value={`${estimatedFare.toLocaleString()}원 (-23%)`}
            green
          />
        </section>
      </main>
      {cargoId && (
        <footer className="border-t border-[#e7ecf4] bg-white px-5 pb-7 pt-4">
          <Button
            fullWidth
            onClick={() =>
              navigate(`/capacity?cargoId=${cargoId}&groupId=${detail.id}`)
            }
          >
            이 공동화물 선택하고 열차 확인
          </Button>
        </footer>
      )}
    </div>
  );
}
function CargoLine({
  name,
  company,
  volume,
  weight,
  mine = false,
}: {
  name: string;
  company: string;
  volume: number | null;
  weight: number | null;
  mine?: boolean;
}) {
  return (
    <div
      className={`rounded-[22px] bg-white p-4 ${mine ? "border-2 border-brand-600" : "border border-[#dce3ee]"}`}
    >
      <div className="flex items-center justify-between">
        <strong className="text-[13px]">{name}</strong>
        <span
          className={`rounded-full px-2 py-1 text-[9px] font-black ${mine ? "bg-brand-50 text-brand-700" : "text-[#78859b]"}`}
        >
          {company}
        </span>
      </div>
      <p className="mt-2 text-[11px] font-semibold text-[#5d6a80]">
        {volume != null ? `${volume.toFixed(1)} CBM · ` : ""}
        {weight != null ? `${weight.toLocaleString()} kg` : "중량 확인 중"}
      </p>
    </div>
  );
}
function PriceRow({
  label,
  value,
  muted = false,
  green = false,
}: {
  label: string;
  value: string;
  muted?: boolean;
  green?: boolean;
}) {
  return (
    <div className="flex justify-between py-1 text-[11px]">
      <span className="font-semibold text-[#5e6b81]">{label}</span>
      <strong
        className={
          green
            ? "text-[#0ba995]"
            : muted
              ? "text-[#9aa5b7] line-through"
              : "text-[#182237]"
        }
      >
        {value}
      </strong>
    </div>
  );
}
