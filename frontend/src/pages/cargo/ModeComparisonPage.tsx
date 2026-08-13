import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { getCargo } from "../../api/cargo";
import type { CargoResponse } from "../../types";
import { ApiError } from "../../api/client";

export function ModeComparisonPage() {
  const { cargoId } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const groupId = searchParams.get("groupId");
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
      <div className="p-8 text-center text-sm text-gray-400">
        {error ?? "비교표를 만드는 중…"}
      </div>
    );

  const weight = cargo.weightKg ?? 200;
  const volume = cargo.volumeCbm ?? 18.6;
  const railCost = Math.round(
    weight * 500 * 0.65 * (1 + cargo.surchargeRate) +
      cargo.fixedPowerFeeKrw +
      5700,
  );
  const roadCost = Math.max(
    98_000,
    Math.round(weight * 790 * (1 + cargo.surchargeRate)),
  );
  const existingCbm = 41.5;
  const coLoadCbm = Math.min(40.8, Math.max(volume, 1));
  const remainingCbm = Math.max(0, 67 - existingCbm - coLoadCbm);
  const railSaving = Math.max(
    0,
    Math.round(((roadCost - railCost) / roadCost) * 100),
  );

  return (
    <div className="flex min-h-full flex-col bg-[#f4f7fb]">
      <header className="border-b border-[#e7ecf4] bg-white pt-5">
        <div className="flex h-12 items-center gap-3 px-5">
          <button onClick={() => navigate(-1)} aria-label="뒤로가기">
            ←
          </button>
          <h1 className="text-[18px] font-black">운송수단 비교</h1>
          <span className="ml-auto text-[10px] font-bold text-[#8190a6]">
            {cargo.originStation} → {cargo.destinationStation} ·{" "}
            {volume.toFixed(1)} CBM
          </span>
        </div>
      </header>
      <main className="flex flex-1 flex-col gap-4 overflow-y-auto px-5 py-5">
        <section className="rounded-[26px] border border-[#dce3ee] bg-white p-5">
          <h2 className="text-[12px] font-black">
            3061 화물열차 화차 잔여공간
          </h2>
          <div
            className="relative mx-auto mt-4 size-[190px] rounded-full"
            style={{
              background: `conic-gradient(#2460ee 0 62%, #12b3a6 62% 78%, #e8edf5 78% 100%)`,
            }}
          >
            <div className="absolute inset-[28px] grid place-items-center rounded-full bg-white text-center">
              <div>
                <p className="text-[10px] font-bold text-[#8190a6]">
                  잔여 화차공간
                </p>
                <strong className="block text-[24px] text-[#2c49bd]">
                  {remainingCbm.toFixed(1)} CBM
                </strong>
                <p className="text-[10px] font-black text-brand-700">
                  전체의 22%
                </p>
              </div>
            </div>
          </div>
          <div className="mt-4 space-y-2">
            <Legend
              color="#2460ee"
              label="기존 적재 화물"
              value={`${existingCbm.toFixed(1)} CBM · 62%`}
            />
            <Legend
              color="#12b3a6"
              label="내 공동화물"
              value={`${coLoadCbm.toFixed(1)} CBM · 16%`}
            />
            <Legend
              color="#e8edf5"
              label="잔여"
              value={`${remainingCbm.toFixed(1)} CBM · 22%`}
            />
          </div>
        </section>

        <section>
          <h2 className="mb-3 text-[12px] font-black">동일 구간 비교</h2>
          <div className="grid grid-cols-2 gap-3">
            <ModeCard
              selected
              title="철도 공동운송"
              badge="추천"
              cost={railCost}
              time="13시간 30분"
              distance="348km"
              carbon="42kg"
            />
            <ModeCard
              title="도로 단독운송"
              badge="대안"
              cost={roadCost}
              time="9시간 10분"
              distance="372km"
              carbon="168kg"
            />
          </div>
        </section>

        <section className="rounded-[24px] border border-[#dce3ee] bg-white p-4">
          <h2 className="mb-3 text-[12px] font-black">항목별 격차</h2>
          <GapRow
            label="운임"
            left={76}
            right={24}
            value={`철도 -${railSaving}%`}
          />
          <GapRow
            label="소요 시간"
            left={58}
            right={42}
            value="도로 -4h 20m"
            muted
          />
          <GapRow
            label="탄소 배출"
            left={20}
            right={80}
            value="철도 -75%"
            green
          />
        </section>
      </main>
      <footer className="border-t border-[#e7ecf4] bg-white px-5 pb-7 pt-4">
        <Button
          fullWidth
          onClick={() =>
            navigate(
              `/cargo/${cargo.id}/mode-recommendation${groupId ? `?groupId=${groupId}` : ""}`,
            )
          }
        >
          AI 운송수단 추천 받기
        </Button>
      </footer>
    </div>
  );
}

function Legend({
  color,
  label,
  value,
}: {
  color: string;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center gap-2 text-[11px]">
      <span className="size-2.5 rounded-full" style={{ background: color }} />
      <span className="font-semibold text-[#536077]">{label}</span>
      <strong className="ml-auto text-[#182237]">{value}</strong>
    </div>
  );
}
function ModeCard({
  selected = false,
  title,
  badge,
  cost,
  time,
  distance,
  carbon,
}: {
  selected?: boolean;
  title: string;
  badge: string;
  cost: number;
  time: string;
  distance: string;
  carbon: string;
}) {
  return (
    <div
      className={`rounded-[22px] p-4 ${selected ? "bg-[#3049bd] text-white" : "border border-[#dce3ee] bg-white text-[#182237]"}`}
    >
      <div className="flex items-center justify-between">
        <strong className="text-[13px]">{title}</strong>
        <span
          className={`rounded-full px-2 py-1 text-[9px] font-black ${selected ? "bg-white/20" : "bg-[#f1f4fa] text-[#8a96aa]"}`}
        >
          {badge}
        </span>
      </div>
      <p
        className={`mt-3 text-[9px] font-bold ${selected ? "text-white/65" : "text-[#8a96aa]"}`}
      >
        총 운임
      </p>
      <p className="text-[19px] font-black">{cost.toLocaleString()}원</p>
      <p
        className={`mt-2 text-[9px] ${selected ? "text-white/65" : "text-[#8a96aa]"}`}
      >
        소요 시간
      </p>
      <p className="text-[12px] font-black">{time}</p>
      <p
        className={`mt-2 text-[9px] ${selected ? "text-white/65" : "text-[#8a96aa]"}`}
      >
        거리 · CO₂
      </p>
      <p className="text-[12px] font-black">
        {distance} · {carbon}
      </p>
    </div>
  );
}
function GapRow({
  label,
  left,
  right,
  value,
  muted = false,
  green = false,
}: {
  label: string;
  left: number;
  right: number;
  value: string;
  muted?: boolean;
  green?: boolean;
}) {
  return (
    <div className="mb-3 last:mb-0">
      <div className="flex justify-between text-[10px]">
        <span className="font-bold text-[#4e5b72]">{label}</span>
        <span
          className={`font-black ${green ? "text-[#0fae9a]" : muted ? "text-[#8a96aa]" : "text-[#0fae9a]"}`}
        >
          {value}
        </span>
      </div>
      <div className="mt-1 flex gap-1">
        <span
          className={`h-1.5 rounded-full ${green ? "bg-[#12b3a6]" : "bg-brand-600"}`}
          style={{ width: `${left}%` }}
        />
        <span
          className="h-1.5 rounded-full bg-[#dfe5ef]"
          style={{ width: `${right}%` }}
        />
      </div>
    </div>
  );
}
