import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useNotifications } from "../context/NotificationContext";
import { browseConsolidations, getMyConsolidations } from "../api/consolidation";
import { getUpcomingTrains } from "../api/train";
import { getMyPayments } from "../api/reservation";
import type { ConsolidationDetailResponse, ParticipationPaymentResponse, TrainResponse } from "../types";
import { ProgressBar } from "../components/ui/ProgressBar";

const STATUS_LABEL: Record<string, string> = {
  RECRUITING: "모집 중", READY_FOR_MATCHING: "배정 대기", MATCHED: "매칭 완료",
  PENDING_APPROVAL: "승인 대기", APPROVED: "승인 완료", CONFIRMED: "철도 운송중",
  REJECTED: "반려", CANCELLED: "취소",
};

export function HomePage() {
  const navigate = useNavigate();
  const { shipper } = useAuth();
  const { unreadCount } = useNotifications();
  const [myGroups, setMyGroups] = useState<ConsolidationDetailResponse[]>([]);
  const [groups, setGroups] = useState<ConsolidationDetailResponse[]>([]);
  const [payments, setPayments] = useState<ParticipationPaymentResponse[]>([]);
  const [trains, setTrains] = useState<TrainResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([getMyConsolidations(), getUpcomingTrains(), browseConsolidations(), getMyPayments()])
      .then(([mine, trainRows, publicGroups, paymentRows]) => {
        if (cancelled) return;
        setMyGroups(mine); setTrains(trainRows); setGroups(publicGroups); setPayments(paymentRows);
      })
      .catch(() => !cancelled && setLoadError(true))
      .finally(() => !cancelled && setLoading(false));
    return () => { cancelled = true; };
  }, []);

  const activeGroup = myGroups.find((g) => g.status === "CONFIRMED")
    ?? myGroups.find((g) => !["REJECTED", "CANCELLED"].includes(g.status)) ?? null;
  const activePayment = activeGroup ? payments.find((p) => p.consolidatedCargoId === activeGroup.id) ?? null : null;
  const recommendation = groups[0] ?? activeGroup;
  const train = trains[0];
  const wagon = train?.wagons.find((w) => w.wagonType === "CONTAINER") ?? train?.wagons[0];
  const calculatedCbm = recommendation?.participants.reduce((sum, p) => sum + (p.volumeCbm ?? 0), 0) ?? 0;
  const totalCbmEstimate = calculatedCbm > 0 ? calculatedCbm : null;
  const today = useMemo(() => new Date(Date.now() + 8.64e7).toISOString().slice(0, 10), []);

  function startRegister() {
    const params = new URLSearchParams({ date: today });
    if (train?.originStation) params.set("origin", train.originStation);
    if (train?.destinationStation) params.set("destination", train.destinationStation);
    params.set("mode", "CO_LOAD");
    navigate(`/cargo/new/form?${params.toString()}`);
  }

  return (
    <div className="flex min-h-full flex-col">
      <section className="bg-brand-700 px-5 pb-[26px] pt-6 text-white">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-[11.5px] font-semibold text-white/80">{shipper?.companyName ?? "(주)한성물류"}</p>
            <h1 className="mt-1 text-xl font-extrabold tracking-[-.5px]">오늘 실을 화물이 있으신가요?</h1>
          </div>
          <button aria-label="알림" onClick={() => navigate("/notifications")} className="relative grid size-[34px] place-items-center rounded-[11px] bg-white/20">
            <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></svg>
            {unreadCount > 0 && <span className="absolute right-1.5 top-1.5 size-2 rounded-full border-2 border-brand-700 bg-[#ff5a5a]" />}
          </button>
        </div>

        <div className="mt-[18px] rounded-[18px] bg-white p-4 text-[#111c2e] shadow-[0_10px_26px_rgba(10,44,116,.16)]">
          <div className="flex items-center gap-2.5">
            <RoutePoint label="출발지" value={train?.originStation ?? "선택"} />
            <span className="grid size-8 place-items-center rounded-[11px] bg-brand-50 text-sm font-extrabold text-brand-700">→</span>
            <RoutePoint label="도착지" value={train?.destinationStation ?? "선택"} align="right" />
          </div>
          <div className="mt-3 flex gap-2">
            <SummaryBox label="희망일" value={new Date(today).toLocaleDateString("ko-KR", { month: "long", day: "numeric" })} />
            <SummaryBox label="CBM" value={totalCbmEstimate == null ? "미정" : totalCbmEstimate.toFixed(1)} />
          </div>
          <button onClick={startRegister} className="mt-3 h-[50px] w-full rounded-[13px] bg-brand-700 text-[14.5px] font-extrabold text-white">화물 등록하기</button>
        </div>
      </section>

      <main className="flex flex-1 flex-col gap-[14px] overflow-y-auto px-5 py-[18px]">
        {loadError && <p className="rounded-chip bg-danger-soft px-4 py-3 text-sm font-semibold text-danger">홈 정보를 불러오지 못했습니다. 네트워크 연결을 확인해주세요.</p>}
        <SectionTitle title="추천 공동화물" action="전체보기" onClick={() => navigate("/exchange")} />
        <div className="grid grid-cols-2 gap-2.5">
          <button onClick={() => recommendation ? navigate(`/consolidated-cargos/${recommendation.id}`) : navigate("/exchange")} className="rounded-2xl border border-[#e4e9f3] bg-white p-3.5 text-left">
            <span className="rounded-md bg-brand-50 px-2 py-1 text-[10px] font-extrabold text-brand-700">CBM 적합</span>
            <p className="mt-2 text-[13.5px] font-extrabold leading-5">{recommendation ? `${recommendation.participantCount}개사 공동운송` : "모집 중인 공동화물 없음"}<br />{recommendation ? `${recommendation.originStation} → ${recommendation.destinationStation}` : "화물을 등록해 추천을 받아보세요"}</p>
            <p className="mt-2 text-[11px] font-semibold text-[#78859b]">{recommendation ? `${recommendation.recruitedWeightKg.toLocaleString()}kg / ${recommendation.targetWeightKg.toLocaleString()}kg 모집` : "실제 운송조건 기준으로 조회됩니다"}</p>
          </button>
          <button onClick={() => navigate("/capacity")} className="rounded-2xl bg-brand-800 p-3.5 text-left text-white">
            <span className="rounded-md bg-white/20 px-2 py-1 text-[10px] font-extrabold">잔여공간</span>
            <p className="mt-2 text-[13.5px] font-extrabold leading-5">{train ? `${new Date(train.departureAt).toLocaleDateString("ko-KR", { month: "numeric", day: "numeric" })} ${train.trainNumber}열차` : "예정 열차 없음"}<br />{wagon ? `${wagon.remainingWeightKg.toLocaleString()}kg 잔여` : "잔여공간을 확인해주세요"}</p>
            <p className="mt-2 text-[11px] font-semibold text-white/75">{train ? `도착 ${new Date(train.arrivalAt).toLocaleTimeString("ko-KR", {hour:"2-digit",minute:"2-digit"})}` : "운행정보가 등록되면 표시됩니다"}</p>
          </button>
        </div>

        <div className="grid grid-cols-4 divide-x divide-[#edf1f7] rounded-2xl border border-[#e4e9f3] bg-white">
          <QuickAction kind="cargo" label="화물등록" onClick={startRegister} />
          <QuickAction kind="match" label="거래소" onClick={() => navigate("/exchange")} />
          <QuickAction kind="truck" label="집하요청" onClick={() => navigate("/shipments")} />
          <QuickAction kind="payment" label="결제" onClick={() => navigate("/me")} />
        </div>

        <SectionTitle title="예약 현황" action="운송 추적" onClick={() => navigate("/shipments")} />
        <button onClick={() => activeGroup && navigate(activePayment ? `/cargo/${activePayment.cargoOrderId}/status?groupId=${activeGroup.id}` : `/consolidated-cargos/${activeGroup.id}`)} className="rounded-2xl border border-[#e4e9f3] bg-white p-[15px] text-left disabled:opacity-60" disabled={!activeGroup}>
          {loading ? <p className="text-xs text-[#78859b]">불러오는 중…</p> : activeGroup ? <>
            <div className="flex items-center justify-between"><strong className="text-[13.5px]">{activeGroup.originStation} → {activeGroup.destinationStation} · {activeGroup.recruitedWeightKg.toLocaleString()}kg</strong><span className="rounded-md bg-brand-50 px-2 py-1 text-[10px] font-extrabold text-brand-700">{STATUS_LABEL[activeGroup.status]}</span></div>
            <div className="mt-2"><ProgressBar percent={activeGroup.recruitmentRatePercent} /></div>
            <div className="mt-2 flex justify-between text-[10.5px] font-semibold text-[#78859b]"><span>{activeGroup.status === "CONFIRMED" ? "철도 운송" : "공동화물 모집"}</span><span>도착 예정 확인 ›</span></div>
          </> : <p className="text-xs font-semibold text-[#78859b]">진행 중인 예약이 없습니다. 화물을 등록해보세요.</p>}
        </button>

        <div className="rounded-[14px] bg-brand-50 p-4 text-[11.5px] font-semibold leading-5 text-brand-800">
          <strong className="block text-xs">사업자 인증이 완료된 데모 계정입니다</strong>
          운송 예약·알림·결제·세금계산서 흐름을 모두 확인할 수 있어요.
        </div>
      </main>
    </div>
  );
}

function RoutePoint({ label, value, align }: { label: string; value: string; align?: "right" }) {
  return <div className={`flex flex-1 flex-col gap-0.5 ${align === "right" ? "items-end" : ""}`}><span className="text-[11px] font-bold text-[#78859b]">{label}</span><strong className="text-[17px]">{value}</strong></div>;
}
function SummaryBox({ label, value }: { label: string; value: string }) {
  return <div className="flex h-11 flex-1 items-center justify-between rounded-[11px] bg-[#f4f6fb] px-[13px]"><span className="text-[11px] font-bold text-[#666]">{label}</span><strong className="text-[13px]">{value}</strong></div>;
}
function SectionTitle({ title, action, onClick }: { title: string; action: string; onClick: () => void }) {
  return <div className="flex items-center justify-between"><h2 className="text-sm font-extrabold">{title}</h2><button onClick={onClick} className="text-[11.5px] font-bold text-brand-700">{action}</button></div>;
}
function QuickAction({ kind, label, onClick }: { kind: "cargo" | "match" | "truck" | "payment"; label: string; onClick: () => void }) {
  return <button onClick={onClick} className="flex flex-col items-center gap-1.5 px-1 py-3.5"><span className="grid size-[34px] place-items-center rounded-[11px] bg-brand-50 text-brand-700"><QuickIcon kind={kind} /></span><span className="text-[10.5px] font-bold">{label}</span></button>;
}
function QuickIcon({ kind }: { kind: "cargo" | "match" | "truck" | "payment" }) {
  if (kind === "match") return <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9"><circle cx="11" cy="11" r="6"/><path d="m20 20-4.5-4.5"/></svg>;
  if (kind === "truck") return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M3 6h11v11H3zM14 10h4l3 3v4h-7z"/><circle cx="7" cy="18" r="2"/><circle cx="18" cy="18" r="2"/></svg>;
  if (kind === "payment") return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 9h18M7 15h4"/></svg>;
  return <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M4 7.5 12 3l8 4.5v9L12 21l-8-4.5zM4 7.5l8 4.5 8-4.5M12 12v9"/></svg>;
}
