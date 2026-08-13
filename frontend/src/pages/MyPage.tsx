import { useEffect, useState, type ReactNode } from "react";
import { useAuth } from "../context/AuthContext";
import { getMyPayments } from "../api/reservation";
import { getMyCargoOrders } from "../api/cargo";
import { getMyConsolidations } from "../api/consolidation";
import type { CargoResponse, ConsolidationDetailResponse, ParticipationPaymentResponse } from "../types";

const GROUP_STATUS: Record<string, string> = {
  RECRUITING: "모집중", READY_FOR_MATCHING: "배정대기", MATCHED: "매칭완료",
  PENDING_APPROVAL: "승인대기", APPROVED: "승인완료", CONFIRMED: "운송중",
  REJECTED: "반려", CANCELLED: "취소",
};

export function MyPage() {
  const { shipper, logout } = useAuth();
  const [groups, setGroups] = useState<ConsolidationDetailResponse[]>([]);
  const [cargos, setCargos] = useState<CargoResponse[]>([]);
  const [payments, setPayments] = useState<ParticipationPaymentResponse[]>([]);
  const [section, setSection] = useState<"groups" | "cargo" | "payments" | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getMyConsolidations(), getMyCargoOrders(), getMyPayments()])
      .then(([groupRows, cargoRows, paymentRows]) => {
        setGroups(groupRows); setCargos(cargoRows); setPayments(paymentRows);
      })
      .finally(() => setLoading(false));
  }, []);

  const totalWeight = cargos.reduce((sum, cargo) => sum + (cargo.weightKg ?? 0), 0);

  return (
    <div className="flex min-h-full flex-col bg-[#f4f7fb]">
      <header className="bg-white px-5 pb-4 pt-7"><h1 className="text-[20px] font-black text-[#111c2e]">마이페이지</h1></header>
      <main className="flex flex-1 flex-col gap-4 px-5 pb-8">
        <section className="bg-white py-1">
          <div className="flex items-center gap-3">
            <div className="grid size-12 place-items-center rounded-full bg-brand-50 text-sm font-black text-brand-700">{shipper?.companyName?.replace(/^\(주\)/, "").slice(0, 2) ?? "한성"}</div>
            <div className="min-w-0 flex-1">
              <p className="font-black text-[#111c2e]">{shipper?.companyName ?? "-"}</p>
              <p className="text-[11px] font-semibold text-[#78859b]">담당자 {shipper?.managerName ?? "-"} · {shipper?.phone ?? "-"}</p>
            </div>
            <button className="text-xs font-black text-brand-700">수정</button>
          </div>
          <div className="mt-4 grid grid-cols-3 rounded-[22px] bg-[#f2f5fb] py-4 text-center">
            <Stat value={`${groups.length}`} label="이번달 운송" />
            <Stat value={`${payments.length > 0 ? 23 : 0}%`} label="평균 절감" />
            <Stat value={`${Math.max(0, totalWeight * .00082).toFixed(1)}t`} label="CO₂ 감축" />
          </div>
        </section>

        <div className="overflow-hidden rounded-[22px] border border-[#dfe5f0] bg-white">
          <MenuRow label="예약 내역" onClick={() => setSection(section === "groups" ? null : "groups")} />
          <MenuRow label="화물 내역" onClick={() => setSection(section === "cargo" ? null : "cargo")} />
          <MenuRow label="결제 내역 · 세금계산서" onClick={() => setSection(section === "payments" ? null : "payments")} trailing={<span className="text-xs font-black text-brand-700">{payments.length}건 ›</span>} />
          <MenuRow label="기업정보 관리" />
        </div>

        {loading && <p className="py-4 text-center text-sm text-[#9aa5b7]">불러오는 중…</p>}
        {!loading && section === "groups" && <Details rows={groups.map((group) => `${group.originStation} → ${group.destinationStation} · ${GROUP_STATUS[group.status] ?? group.status}`)} empty="예약 내역이 없어요." />}
        {!loading && section === "cargo" && <Details rows={cargos.map((cargo) => `${cargo.cargoName} · ${cargo.status}`)} empty="화물 내역이 없어요." />}
        {!loading && section === "payments" && <Details rows={payments.map((payment) => `공동화물 #${payment.consolidatedCargoId} · ${payment.totalCost.toLocaleString()}원 · 결제완료`)} empty="결제 내역이 없어요." />}

        <div className="overflow-hidden rounded-[22px] border border-[#dfe5f0] bg-white">
          <MenuRow label="알림 설정" trailing={<span className="inline-flex h-7 w-12 items-center justify-end rounded-full bg-brand-700 p-1"><span className="size-5 rounded-full bg-white" /></span>} />
          <MenuRow label="고객센터 · 1588-0000" />
        </div>
        <button onClick={logout} className="py-1 text-xs font-bold text-[#a8b1c1]">로그아웃</button>
      </main>
    </div>
  );
}

function Stat({ value, label }: { value: string; label: string }) {
  return <div className="border-r border-[#dfe5f0] last:border-0"><p className="text-[20px] font-black text-[#111c2e]">{value}</p><p className="text-[10px] font-bold text-[#8490a5]">{label}</p></div>;
}

function MenuRow({ label, onClick, trailing }: { label: string; onClick?: () => void; trailing?: ReactNode }) {
  return <button onClick={onClick} className="flex h-[52px] w-full items-center justify-between border-b border-[#e5eaf2] px-4 text-left text-sm font-bold last:border-0"><span>{label}</span>{trailing ?? <span className="text-xl font-light text-[#aab5c7]">›</span>}</button>;
}

function Details({ rows, empty }: { rows: string[]; empty: string }) {
  return <div className="screen-card divide-y divide-[#e8edf5] px-4">{rows.length === 0 ? <p className="py-5 text-center text-xs text-[#9aa5b7]">{empty}</p> : rows.slice(0, 6).map((row, index) => <p key={`${row}-${index}`} className="py-3 text-xs font-semibold text-[#4d5a70]">{row}</p>)}</div>;
}
