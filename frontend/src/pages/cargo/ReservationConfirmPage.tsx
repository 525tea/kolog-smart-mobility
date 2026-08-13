import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getCargo } from "../../api/cargo";
import { getConsolidationDetail, getCandidates } from "../../api/consolidation";
import type {
  CargoResponse,
  ConsolidationCandidateResponse,
  ConsolidationDetailResponse,
} from "../../types";
import { Button } from "../../components/ui/Button";
export function ReservationConfirmPage() {
  const { cargoId, groupId } = useParams();
  const navigate = useNavigate();
  const [cargo, setCargo] = useState<CargoResponse | null>(null);
  const [group, setGroup] = useState<ConsolidationDetailResponse | null>(null);
  const [quote, setQuote] = useState<ConsolidationCandidateResponse | null>(
    null,
  );
  useEffect(() => {
    if (!cargoId || !groupId) return;
    Promise.all([
      getCargo(+cargoId),
      getConsolidationDetail(+groupId),
      getCandidates(+cargoId),
    ]).then(([c, g, q]) => {
      setCargo(c);
      setGroup(g);
      setQuote(q.find((row) => row.consolidatedCargoId === +groupId) ?? null);
    });
  }, [cargoId, groupId]);
  if (!cargo || !group)
    return (
      <div className="grid min-h-full place-items-center text-sm text-gray-400">
        예약 내용을 불러오는 중…
      </div>
    );
  return (
    <div className="flex min-h-full flex-col bg-white">
      <header className="border-b border-[#e7ecf4] pt-5">
        <div className="flex h-12 items-center gap-3 px-5">
          <button onClick={() => navigate(-1)}>←</button>
          <h1 className="text-[18px] font-black">예약 확인</h1>
        </div>
      </header>
      <main className="flex flex-1 flex-col gap-5 overflow-y-auto px-5 py-5">
        <h2 className="text-[22px] font-black leading-[1.4]">
          아래 내용으로
          <br />
          철도 운송을 예약합니다
        </h2>
        <section className="overflow-hidden rounded-[26px] border border-[#dce3ee]">
          <div className="flex justify-between bg-[#3049bd] p-4 text-white">
            <div>
              <p className="text-[10px] text-white/65">운송일</p>
              <strong>
                {new Date(cargo.desiredDate).toLocaleDateString("ko-KR", {
                  year: "numeric",
                  month: "2-digit",
                  day: "2-digit",
                  weekday: "short",
                })}
              </strong>
            </div>
            <div className="text-right">
              <p className="text-[10px] text-white/65">열차</p>
              <strong>{quote?.trainNumber ?? "3061 화물열차"}</strong>
            </div>
          </div>
          <div className="p-4">
            <Route
              name={cargo.originStation}
              time={
                quote
                  ? new Date(quote.departureAt).toLocaleTimeString("ko-KR", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })
                  : "14:20"
              }
              active
            />
            <Route
              name={cargo.destinationStation}
              time={
                quote
                  ? new Date(quote.arrivalAt).toLocaleTimeString("ko-KR", {
                      hour: "2-digit",
                      minute: "2-digit",
                    })
                  : "22:05"
              }
            />
          </div>
        </section>
        <section className="rounded-[24px] bg-[#f3f6fb] p-4">
          <Line
            label="화물"
            value={`${cargo.cargoName} · ${(cargo.weightKg ?? 0).toLocaleString()}kg`}
          />
          <Line
            label="공동운송"
            value={`${group.participantCount + 1}건 · ${cargo.volumeCbm != null ? `${cargo.volumeCbm.toFixed(1)} CBM` : "부피 확인 필요"}`}
          />
          <Line
            label="운송조건"
            value={
              cargo.temperatureCondition === "FROZEN"
                ? "냉동 -18℃ 이하"
                : cargo.temperatureCondition === "REFRIGERATED"
                  ? "냉장 0~8℃"
                  : "일반 운송"
            }
          />
          <Line
            label="철도 운임"
            value={`${Math.round(quote?.totalPayableKrw ?? 0).toLocaleString()}원`}
            blue
          />
        </section>
        <label className="flex gap-3 rounded-[22px] border border-[#dce3ee] p-4 text-[11px] font-semibold leading-5 text-[#59667b]">
          <input
            type="checkbox"
            defaultChecked
            className="size-5 accent-brand-600"
          />
          공동운송 약관 및 화물 파손 책임 범위에 동의합니다. 출발 24시간 전까지
          무료 취소 가능.
        </label>
      </main>
      <footer className="border-t border-[#e7ecf4] px-5 pb-7 pt-4">
        <Button
          fullWidth
          onClick={() =>
            navigate(`/cargo/${cargo.id}/door-to-door/${group.id}`)
          }
        >
          예약하고 집하 설정
        </Button>
      </footer>
    </div>
  );
}
function Route({
  name,
  time,
  active = false,
}: {
  name: string;
  time: string;
  active?: boolean;
}) {
  return (
    <div className="flex items-center gap-3 py-2">
      <span
        className={`size-3 rounded-full ${active ? "bg-brand-600" : "border-2 border-brand-600 bg-white"}`}
      />
      <strong className="text-[13px]">{name}</strong>
      <span className="ml-auto text-[13px] font-black text-[#5a667b]">
        {time}
      </span>
    </div>
  );
}
function Line({
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
      <span className="font-semibold text-[#657288]">{label}</span>
      <strong className={blue ? "text-brand-700" : "text-[#182237]"}>
        {value}
      </strong>
    </div>
  );
}
