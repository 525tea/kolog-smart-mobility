import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { getUpcomingTrains } from "../api/train";
import { getCargo } from "../api/cargo";
import { getConsolidationDetail } from "../api/consolidation";
import type { TrainResponse } from "../types";

export function CapacitySearchPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const cargoId = params.get("cargoId");
  const groupId = params.get("groupId");
  const [trains, setTrains] = useState<TrainResponse[]>([]);
  const [selectedDate, setSelectedDate] = useState("");
  const [selectedTrain, setSelectedTrain] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    Promise.all([
      getUpcomingTrains(),
      cargoId ? getCargo(Number(cargoId)) : Promise.resolve(null),
      groupId ? getConsolidationDetail(Number(groupId)) : Promise.resolve(null),
    ])
      .then(([trainRows, cargo, group]) => {
        const sorted = [...trainRows].sort(
          (a, b) => +new Date(a.departureAt) - +new Date(b.departureAt),
        );
        const origin = cargo?.originStation ?? group?.originStation;
        const destination = cargo?.destinationStation ?? group?.destinationStation;
        const routeTrains = origin && destination
          ? sorted.filter((row) => row.originStation === origin && row.destinationStation === destination)
          : sorted;
        const requestedDate = cargo?.desiredDate ?? group?.desiredDate;
        const initialDate = requestedDate && routeTrains.some((row) => localDate(row.departureAt) === requestedDate)
          ? requestedDate
          : routeTrains[0] ? localDate(routeTrains[0].departureAt) : "";
        setTrains(routeTrains);
        setSelectedDate(initialDate);
        setSelectedTrain(routeTrains.find((row) => localDate(row.departureAt) === initialDate)?.id ?? null);
      })
      .catch(() => setError("잔여용량 정보를 불러오지 못했어요."))
      .finally(() => setLoading(false));
  }, [cargoId, groupId]);
  const dates = useMemo(
    () =>
      Array.from(
        new Set(trains.map((row) => localDate(row.departureAt))),
      ).slice(0, 4),
    [trains],
  );
  const dayTrains = trains.filter((row) => localDate(row.departureAt) === selectedDate);
  const rows = dayTrains
    .map((train) => {
      const wagon =
        train.wagons.find((row) => row.wagonType === "CONTAINER") ??
        train.wagons[0];
      const max = wagon?.maxWeightKg ?? 1;
      const remain = wagon?.remainingWeightKg ?? 0;
      const load = Math.round(((max - remain) / max) * 100);
      const teu = Math.max(0, Math.round(67 * (remain / max)));
      return {
        train,
        wagon,
        load,
        teu,
        price: Math.round(300000 + load * 1200),
      };
    });
  const chosen = rows.find((row) => row.train.id === selectedTrain) ?? rows[0];
  return (
    <div className="flex min-h-full flex-col bg-[#f4f7fb]">
      <header className="border-b border-[#e7ecf4] bg-white pt-5">
        <div className="flex h-12 items-center gap-3 px-5">
          <button onClick={() => navigate(-1)} aria-label="뒤로가기">
            ←
          </button>
          <h1 className="text-[18px] font-black">철도 잔여용량</h1>
        </div>
        <div className="flex gap-2 overflow-x-auto px-5 pb-4 pt-2">
          {dates.map((date) => {
            const dateRows = trains.filter((row) => localDate(row.departureAt) === date);
            const availableTeu = dateRows.reduce((sum, train) => {
              const wagon = train.wagons.find((row) => row.wagonType === "CONTAINER") ?? train.wagons[0];
              if (!wagon) return sum;
              return sum + Math.max(0, Math.round(67 * (wagon.remainingWeightKg / wagon.maxWeightKg)));
            }, 0);
            return (
            <button
              key={date}
              onClick={() => {
                setSelectedDate(date);
                setSelectedTrain(null);
              }}
              className={`min-w-[86px] rounded-[20px] px-3 py-3 text-center ${date === selectedDate ? "bg-brand-600 text-white" : "border border-[#dce3ee] bg-[#f3f6fb] text-[#526078]"}`}
            >
              <span className="block text-[10px] font-bold">
                {new Date(`${date}T00:00:00`).toLocaleDateString("ko-KR", {
                  month: "numeric",
                  day: "numeric",
                  weekday: "short",
                })}
              </span>
              <strong className="mt-1 block text-[17px]">
                {dateRows.length === 0 ? "운휴" : `${availableTeu} TEU`}
              </strong>
            </button>
            );
          })}
        </div>
      </header>
      <main className="flex flex-1 flex-col gap-3 overflow-y-auto px-5 py-4">
        {loading && (
          <p className="py-10 text-center text-sm text-[#8a96aa]">
            불러오는 중…
          </p>
        )}
        {error && (
          <p className="rounded-xl bg-rose-50 p-3 text-sm text-rose-600">
            {error}
          </p>
        )}
        {rows.length > 0 && (
          <>
            <div className="flex justify-between text-[11px] font-bold text-[#6c7990]">
              <span>
                {rows[0].train.originStation} →{" "}
                {rows[0].train.destinationStation} · {rows.length}편성
              </span>
              <span>잔여용량 기준 정렬</span>
            </div>
            {rows.map(({ train, load, teu, price }, index) => (
              <button
                key={train.id}
                onClick={() => setSelectedTrain(train.id)}
                className={`rounded-[24px] bg-white p-4 text-left ${chosen?.train.id === train.id ? "border-2 border-brand-600" : "border border-[#dce3ee]"}`}
              >
                <div className="flex justify-between">
                  <div>
                    <strong className="text-[17px]">
                      {train.trainNumber} 화물열차
                    </strong>
                    <p className="mt-1 text-[10px] font-semibold text-[#78859b]">
                      {train.originStation} {time(train.departureAt)} →{" "}
                      {train.destinationStation} {time(train.arrivalAt)}
                    </p>
                  </div>
                  <span className="h-fit rounded-full bg-brand-50 px-2 py-1 text-[9px] font-black text-brand-700">
                    {index === 0 ? "AI 추천" : index === 1 ? "야간" : "환적"}
                  </span>
                </div>
                <div className="mt-3 h-2 rounded-full bg-[#edf1f6]">
                  <div
                    className="h-full rounded-full bg-brand-600"
                    style={{ width: `${load}%` }}
                  />
                </div>
                <div className="mt-2 flex justify-between text-[11px] font-semibold text-[#536077]">
                  <span>
                    적재율 {load}% · 잔여 {teu} TEU
                  </span>
                  <strong>{price.toLocaleString()}원</strong>
                </div>
              </button>
            ))}
          </>
        )}
        {chosen && (
          <section className="rounded-[24px] border border-[#dce3ee] bg-white p-4">
            <h2 className="mb-3 text-[12px] font-black">선택 열차 요약</h2>
            <Summary
              label="열차"
              value={`${chosen.train.trainNumber} 화물열차`}
            />
            <Summary
              label="공동화물 적재"
              value={`${chosen.teu} TEU · ${chosen.load}%`}
            />
            <Summary
              label="내 운임"
              value={`${chosen.price.toLocaleString()}원`}
              blue
            />
          </section>
        )}
      </main>
      {cargoId && chosen && (
        <footer className="border-t border-[#e7ecf4] bg-white px-5 pb-7 pt-4">
          <Button
            fullWidth
            onClick={() =>
              navigate(
                `/cargo/${cargoId}/mode-comparison?trainId=${chosen.train.id}${groupId ? `&groupId=${groupId}` : ""}`,
              )
            }
          >
            이 열차로 운송수단 비교
          </Button>
        </footer>
      )}
    </div>
  );
}
function localDate(value: string) {
  const date = new Date(value);
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
}
function time(value: string) {
  return new Date(value).toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  });
}
function Summary({
  label,
  value,
  blue = false,
}: {
  label: string;
  value: string;
  blue?: boolean;
}) {
  return (
    <div className="flex justify-between py-1 text-[11px]">
      <span className="font-semibold text-[#647187]">{label}</span>
      <strong className={blue ? "text-brand-700" : "text-[#182237]"}>
        {value}
      </strong>
    </div>
  );
}
