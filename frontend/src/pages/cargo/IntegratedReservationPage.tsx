import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { getCargo } from "../../api/cargo";
import { getCandidates, getConsolidationDetail } from "../../api/consolidation";
import type {
  CargoResponse,
  ConsolidationCandidateResponse,
  ConsolidationDetailResponse,
} from "../../types";
export function IntegratedReservationPage() {
  const { cargoId, groupId } = useParams();
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [cargo, setCargo] = useState<CargoResponse | null>(null);
  const [group, setGroup] = useState<ConsolidationDetailResponse | null>(null);
  const [quote, setQuote] = useState<ConsolidationCandidateResponse | null>(
    null,
  );
  const first = params.get("firstMile") === "1";
  const last = params.get("lastMile") === "1";
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
  if (!cargo || !group) return <div />;
  const rail = Math.round(quote?.totalPayableKrw ?? (cargo.weightKg ?? 0) * 500 * 0.77);
  const link = first ? 24000 : 0;
  const end = last ? 19000 : 0;
  const discount = Math.round(quote?.estimatedSavingsForOrder ?? 0);
  const total = rail + link + end;
  const displayDate = new Date(`${cargo.desiredDate}T00:00:00`).toLocaleDateString("ko-KR", { month: "2-digit", day: "2-digit" });
  const vehicle = cargo.temperatureCondition === "FROZEN" ? "냉동" : cargo.temperatureCondition === "REFRIGERATED" ? "냉장" : "일반";
  return (
    <div className="flex min-h-full flex-col bg-[#5a49d5] text-white">
      <header className="flex h-[72px] items-center gap-3 px-5 pt-3">
        <button onClick={() => navigate(-1)}>←</button>
        <h1 className="text-[18px] font-black">통합 예약</h1>
      </header>
      <main className="flex flex-1 flex-col gap-5 px-5">
        <h2 className="text-[24px] font-black leading-[1.45]">
          집하부터 라스트마일까지
          <br />한 번에 예약됩니다
        </h2>
        <section className="rounded-[28px] border border-white/25 bg-white/10 p-5">
          <Timeline
            time={`${displayDate} 10:00 · 집하`}
            title={`${cargo.originStation} 인근 집하지 → ${group.originStation}`}
            detail={`5톤 ${vehicle} 윙바디 · ${link.toLocaleString()}원`}
          />
          <Timeline
            time={`${displayDate} ${quote ? new Date(quote.departureAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }) : "시간 확인"} · 철도`}
            title={`${quote?.trainNumber ?? "3061"} 화물열차 · 공동운송 ${group.participantCount + 1}건`}
            detail={`${group.originStation} → ${group.destinationStation} · ${rail.toLocaleString()}원`}
          />
          <Timeline
            time={`${displayDate} 도착 후 · 라스트마일`}
            title={`${group.destinationStation} → 최종 배송지`}
            detail={`당일 배송 · ${end.toLocaleString()}원`}
            last
          />
        </section>
        <section className="rounded-[28px] bg-white p-5 text-[#182237]">
          <Line label="철도 운임" value={`${rail.toLocaleString()}원`} />
          <Line label="연계운송" value={`${(link + end).toLocaleString()}원`} />
          {discount > 0 && <Line label="공동운송 절감액" value={`-${discount.toLocaleString()}원`} blue />}
          <div className="mt-4 flex items-end justify-between border-t border-[#e7ecf4] pt-4">
            <strong>총 결제 예정</strong>
            <strong className="text-[26px] text-[#604ed8]">
              {total.toLocaleString()}원
            </strong>
          </div>
        </section>
      </main>
      <footer className="px-5 pb-7 pt-4">
        <button
          onClick={() =>
            navigate(
              `/cargo/${cargo.id}/checkout/${group.id}?firstMile=${first ? 1 : 0}&lastMile=${last ? 1 : 0}`,
            )
          }
          className="h-[54px] w-full rounded-[16px] bg-white text-sm font-black text-[#5a49d5]"
        >
          결제하기
        </button>
      </footer>
    </div>
  );
}
function Timeline({
  time,
  title,
  detail,
  last = false,
}: {
  time: string;
  title: string;
  detail: string;
  last?: boolean;
}) {
  return (
    <div className="relative flex gap-4 pb-5 last:pb-0">
      <div className="relative">
        <span className="block size-3 rounded-full bg-white" />
        {!last && (
          <span className="absolute left-[5px] top-3 h-12 w-[2px] bg-white/30" />
        )}
      </div>
      <div>
        <p className="text-[10px] text-white/65">{time}</p>
        <strong className="block text-[13px]">{title}</strong>
        <p className="text-[10px] text-white/65">{detail}</p>
      </div>
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
    <div className="flex justify-between py-1 text-[12px]">
      <span>{label}</span>
      <strong className={blue ? "text-brand-700" : ""}>{value}</strong>
    </div>
  );
}
