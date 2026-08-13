import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getMyCargoOrders } from "../api/cargo";
import { getMyPayments } from "../api/reservation";
import type { CargoResponse, ParticipationPaymentResponse } from "../types";

const STATUS = {
  REGISTERED: { label: "등록 완료", tone: "gray" },
  ANALYZED: { label: "AI 분석 완료", tone: "blue" },
  PARTICIPATING: { label: "공동화물 모집", tone: "amber" },
  RESERVED: { label: "운송 예약", tone: "green" },
  CANCELLED: { label: "취소", tone: "red" },
} as const;

export function ShipmentsPage() {
  const navigate = useNavigate();
  const [cargos, setCargos] = useState<CargoResponse[]>([]);
  const [payments, setPayments] = useState<ParticipationPaymentResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getMyCargoOrders(), getMyPayments()])
      .then(([cargoRows, paymentRows]) => {
        setCargos(cargoRows);
        setPayments(paymentRows);
      })
      .finally(() => setLoading(false));
  }, []);

  const paymentByCargo = useMemo(
    () => new Map(payments.map((payment) => [payment.cargoOrderId, payment])),
    [payments],
  );
  const activeCargos = useMemo(
    () => cargos
      .filter((cargo) => cargo.status === "RESERVED" || cargo.status === "PARTICIPATING")
      .sort((a, b) => (a.status === "RESERVED" ? -1 : b.status === "RESERVED" ? 1 : b.id - a.id)),
    [cargos],
  );

  return (
    <div className="flex min-h-full flex-col bg-[#f4f7fb]">
      <header className="flex items-center bg-white px-5 pb-4 pt-7">
        <h1 className="text-[20px] font-black text-[#111c2e]">운송 현황</h1>
        <span className="ml-auto rounded-full bg-brand-50 px-3 py-1 text-[11px] font-black text-brand-700">진행 {activeCargos.length}건</span>
      </header>
      <main className="flex flex-1 flex-col gap-4 px-5 py-5">
        {loading && <p className="py-14 text-center text-sm text-gray-400">화물 정보를 불러오는 중…</p>}
        {!loading && activeCargos.length === 0 && (
          <button onClick={() => navigate("/cargo/new/form?mode=CO_LOAD")} className="mt-10 rounded-3xl border border-dashed border-brand-200 bg-white px-6 py-12 text-center">
            <CargoOutline />
            <p className="mt-3 font-black text-gray-900">진행 중인 운송이 없어요</p>
            <p className="mt-1 text-sm text-brand-700">첫 화물 등록하기 →</p>
          </button>
        )}
        {activeCargos.map((cargo, index) => {
          const payment = paymentByCargo.get(cargo.id);
          const meta = STATUS[cargo.status];
          const target = payment
            ? `/cargo/${cargo.id}/status?groupId=${payment.consolidatedCargoId}`
            : cargo.status === "REGISTERED" || cargo.status === "ANALYZED"
              ? `/cargo/${cargo.id}/analysis`
              : `/cargo/${cargo.id}/recommendations`;
          if (index === 0) return (
            <section key={cargo.id}>
              <button onClick={() => navigate(target)} className="w-full rounded-[24px] bg-[#2d49bd] p-5 text-left text-white shadow-[0_10px_28px_rgba(45,73,189,.22)]">
                <div className="flex items-start justify-between gap-3">
                  <div><p className="text-[10px] font-bold text-white/60">KLG-{String(cargo.id).padStart(6, "0")}</p><h2 className="mt-1 text-[17px] font-black">{cargo.cargoName}</h2></div>
                  <span className="rounded-full bg-white/15 px-2.5 py-1 text-[10px] font-black">{meta.label}</span>
                </div>
                <p className="mt-3 text-[13px] font-black">{cargo.originStation} → {cargo.destinationStation}</p>
                <p className="mt-1 text-[11px] font-semibold text-white/65">{cargo.desiredDate} · {cargo.weightKg?.toLocaleString() ?? "-"}kg</p>
                <Progress status={cargo.status} />
              </button>
              <div className="mt-4 rounded-[22px] border border-[#dfe5f0] bg-white p-4">
                <div className="flex items-center justify-between"><h3 className="text-[13px] font-black text-[#182237]">실시간 이벤트</h3><button type="button" onClick={() => navigate("/notifications")} className="rounded-lg px-2 py-1 text-[11px] font-black text-brand-700">전체보기 ›</button></div>
                <Event title={payment ? "공동화물 결제가 완료됐어요" : "공동화물 모집이 진행 중이에요"} detail={payment ? "운송 배정 정보를 확인해 주세요." : "목표 중량 달성 시 열차가 확정됩니다."} active />
                <Event title="화물 정보가 정상 등록됐어요" detail={`${cargo.originStation} 출발 · ${cargo.destinationStation} 도착`} />
              </div>
            </section>
          );
          return (
            <button key={cargo.id} onClick={() => navigate(target)} className="rounded-[22px] border border-[#dfe5f0] bg-white p-4 text-left">
              <div className="flex items-start justify-between"><div><p className="text-[10px] font-bold text-[#9aa5b7]">KLG-{String(cargo.id).padStart(6, "0")}</p><h2 className="mt-1 text-sm font-black text-[#182237]">{cargo.cargoName}</h2></div><span className="rounded-full bg-brand-50 px-2 py-1 text-[10px] font-black text-brand-700">{meta.label}</span></div>
              <p className="mt-3 text-[12px] font-bold text-[#4d5a70]">{cargo.originStation} → {cargo.destinationStation}</p>
              <p className="mt-1 text-[10px] text-[#9aa5b7]">{cargo.desiredDate} · {cargo.weightKg?.toLocaleString() ?? "-"}kg</p>
            </button>
          );
        })}
        {!loading && activeCargos.length > 0 && <button onClick={() => navigate("/me")} className="py-3 text-xs font-bold text-gray-400">완료·분석 중 화물은 마이페이지에서 보기 ›</button>}
      </main>
    </div>
  );
}

function Progress({ status }: { status: CargoResponse["status"] }) {
  const current = status === "RESERVED" ? 3 : 2;
  return <div className="mt-5 flex items-start">{["집하", "광양역", "운송중", "라스트마일"].map((label, index) => <div key={label} className="relative flex flex-1 flex-col items-center"><span className={`relative z-10 size-3 rounded-full border-2 ${index <= current ? "border-white bg-white" : "border-white/40 bg-[#2d49bd]"}`} />{index < 3 && <span className={`absolute left-1/2 top-[5px] h-[2px] w-full ${index < current ? "bg-white" : "bg-white/25"}`} />}<span className={`mt-2 text-[9px] font-bold ${index <= current ? "text-white" : "text-white/45"}`}>{label}</span></div>)}</div>;
}

function Event({ title, detail, active = false }: { title: string; detail: string; active?: boolean }) {
  return <div className="relative ml-1 flex gap-3 border-b border-[#edf1f6] py-3 last:border-0 last:pb-0"><span className={`mt-1 size-2.5 shrink-0 rounded-full ${active ? "bg-brand-600 ring-4 ring-brand-50" : "bg-[#cbd3e0]"}`} /><div><p className="text-[11px] font-black text-[#29364b]">{title}</p><p className="mt-1 text-[10px] text-[#8a96aa]">{detail}</p></div></div>;
}

function CargoOutline() {
  return <svg className="mx-auto text-brand-600" width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"><path d="M4 7.5 12 3l8 4.5v9L12 21l-8-4.5zM4 7.5l8 4.5 8-4.5M12 12v9" /></svg>;
}
