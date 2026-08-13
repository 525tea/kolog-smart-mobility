import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { CargoWizardHeader } from "../../components/layout/CargoWizardHeader";
import { Card } from "../../components/ui/Card";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { LiveTrackingMap } from "../../components/tracking/LiveTrackingMap";
import { BottomNav } from "../../components/layout/BottomNav";
import { getConsolidationDetail } from "../../api/consolidation";
import { getCargo } from "../../api/cargo";
import { getMyReservations, getTracking } from "../../api/reservation";
import type { ConsolidationDetailResponse, ReservationResponse, TrackingResponse } from "../../types";

const PHASE_LABEL: Record<TrackingResponse["phase"], string> = {
  BEFORE_DEPARTURE: "출발 대기",
  IN_TRANSIT: "운송 중",
  ARRIVED: "도착 완료",
};

const STAGES = [
  { key: "recruiting", label: "모집중", statuses: ["RECRUITING"] },
  { key: "matching", label: "배정대기", statuses: ["READY_FOR_MATCHING"] },
  { key: "pending", label: "승인대기", statuses: ["MATCHED", "PENDING_APPROVAL"] },
  { key: "confirmed", label: "운송확정", statuses: ["APPROVED", "CONFIRMED"] },
] as const;

function stageIndex(status: string) {
  const idx = STAGES.findIndex((s) => (s.statuses as readonly string[]).includes(status));
  return idx === -1 ? 0 : idx;
}

export function CargoStatusPage() {
  const { cargoId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const groupIdParam = searchParams.get("groupId");
  const [detail, setDetail] = useState<ConsolidationDetailResponse | null>(null);
  const [reservation, setReservation] = useState<ReservationResponse | null>(null);
  const [tracking, setTracking] = useState<TrackingResponse | null>(null);
  const [participationPaid, setParticipationPaid] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!groupIdParam) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    async function load() {
      const [groupDetail, cargo] = await Promise.all([
        getConsolidationDetail(Number(groupIdParam)),
        cargoId ? getCargo(Number(cargoId)) : Promise.resolve(null),
      ]);
      if (cancelled) return;
      setDetail(groupDetail);
      setParticipationPaid(cargo?.status === "PARTICIPATING" || cargo?.status === "RESERVED");
      if (groupDetail.status === "APPROVED" || groupDetail.status === "CONFIRMED") {
        const reservations = await getMyReservations();
        if (cancelled) return;
        const myReservation = reservations.find((r) => r.consolidatedCargoId === groupDetail.id) ?? null;
        setReservation(myReservation);
        if (myReservation) {
          const trackingRes = await getTracking(myReservation.id);
          if (cancelled) return;
          setTracking(trackingRes);
        }
      }
      setLoading(false);
    }
    load();
    const interval = setInterval(load, 8000); // 승인 대기 중이면 주기적으로 상태/위치 갱신
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [cargoId, groupIdParam]);

  if (!groupIdParam) {
    return (
      <div className="flex min-h-full flex-col">
        <CargoWizardHeader step="status" onBack={() => navigate("/home")} />
        <div className="flex flex-1 items-center justify-center px-6 text-center text-sm text-gray-400">
          조회할 예약이 없습니다. 홈에서 진행 중인 운송을 확인해주세요.
        </div>
      </div>
    );
  }

  if (loading || !detail) {
    return (
      <div className="flex min-h-full flex-col">
        <CargoWizardHeader step="status" onBack={() => navigate("/home")} />
        <div className="flex flex-1 items-center justify-center text-sm text-gray-400">불러오는 중…</div>
      </div>
    );
  }

  const isFailed = detail.status === "REJECTED" || detail.status === "CANCELLED";
  const currentStage = stageIndex(detail.status);

  return (
    <div className="flex min-h-full flex-col">
      <CargoWizardHeader step="status" onBack={() => navigate("/home")} />

      <div className="flex flex-1 flex-col gap-4 px-5 py-5 pb-8">
        <div className="flex items-center justify-between">
          <span className="text-sm text-gray-400">공동화물 번호 #{detail.id}</span>
          <Badge tone={isFailed ? "red" : detail.status === "CONFIRMED" ? "green" : "blue"}>
            {isFailed ? "성립 실패" : STAGES[currentStage].label}
          </Badge>
        </div>

        {!isFailed ? (
          <Card>
            <div className="flex items-center justify-between px-2">
              {STAGES.map((stage, index) => (
                <div key={stage.key} className="flex flex-1 flex-col items-center gap-1.5">
                  <div
                    className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold ${
                      index < currentStage
                        ? "bg-emerald-500 text-white"
                        : index === currentStage
                          ? "bg-brand-700 text-white"
                          : "bg-gray-100 text-gray-400"
                    }`}
                  >
                    {index < currentStage ? "✓" : index + 1}
                  </div>
                  <span className={`text-xs ${index <= currentStage ? "font-semibold text-gray-900" : "text-gray-400"}`}>
                    {stage.label}
                  </span>
                </div>
              ))}
            </div>
            <p className="mt-4 rounded-xl bg-brand-50 p-3 text-sm text-brand-700">
              {detail.status === "CONFIRMED" && "운송이 확정되었습니다. 인수증은 마이페이지에서 확인할 수 있어요."}
              {detail.status === "APPROVED" && "코레일 승인이 완료됐어요. 곧 운송이 시작됩니다."}
              {detail.status === "PENDING_APPROVAL" && "코레일 담당자가 검토 중이에요. 잠시만 기다려주세요."}
              {detail.status === "MATCHED" && "실제 화차 배정이 완료되어 승인 요청을 준비하고 있어요."}
              {detail.status === "READY_FOR_MATCHING" && "목표중량을 채워 예약 가능한 화차에 실제 배정하고 있어요."}
              {detail.status === "RECRUITING" && "공동화물을 모집하고 있어요. 목표중량을 채우면 화차 배정으로 진행됩니다."}
            </p>
          </Card>
        ) : (
          <Card className="border border-rose-200 bg-rose-50">
            <p className="font-bold text-rose-700">이 공동화물은 성립되지 않았습니다.</p>
            <p className="mt-1 text-sm text-rose-600">
              참여 시 선택한 처리방식에 따라 다음 그룹으로 자동 이월되었거나 취소 처리되었어요. 마이페이지에서 최신 상태를 확인해주세요.
            </p>
          </Card>
        )}

        <Card>
          <p className="mb-2 text-sm font-bold text-gray-900">
            {detail.originStation} → {detail.destinationStation}
          </p>
          <div className="grid grid-cols-3 gap-2 text-center text-sm">
            <Stat label="적재율" value={`${detail.recruitmentRatePercent}%`} />
            <Stat label="참여 화주" value={`${detail.participantCount}개사`} />
            <Stat
              label="결제상태"
              value={reservation
                ? (reservation.paymentStatus === "VIRTUAL_PAID" ? "결제완료" : "결제대기")
                : participationPaid ? "가상결제완료" : "결제대기"}
            />
          </div>
          {reservation && (
            <p className="mt-3 text-center text-lg font-black text-brand-800">₩{reservation.totalCost.toLocaleString()}</p>
          )}
        </Card>

        {tracking && (
          <Card>
            <div className="flex items-center justify-between">
              <p className="text-sm font-bold text-gray-900">실시간 위치추적</p>
              <Badge tone={tracking.phase === "ARRIVED" ? "green" : "blue"}>{PHASE_LABEL[tracking.phase]}</Badge>
            </div>
            <div className="mt-4"><LiveTrackingMap tracking={tracking} /></div>
            <div className="mt-3 flex items-center justify-between rounded-xl bg-brand-50 px-3 py-2 text-xs">
              <span className="font-bold text-brand-800">🚂 {tracking.currentSegment}</span>
              <span className="text-brand-600">진행률 {tracking.progressPercent}%</span>
            </div>
            <p className="mt-2 text-center text-xs text-gray-500">
              최근 갱신 {new Date(tracking.lastUpdatedAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit", second: "2-digit" })}
              {` · ${tracking.refreshAfterSeconds}초마다 자동 갱신`}
            </p>
            <p className="mt-1 text-center text-[11px] text-gray-300">
              * 현재는 실제 GPS 단말 연동 전으로, 열차 시간표(출발 {new Date(tracking.departureAt).toLocaleString("ko-KR", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" })} ~ 도착{" "}
              {new Date(tracking.arrivalAt).toLocaleString("ko-KR", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" })}) 기준으로 시뮬레이션한 위치예요.
            </p>
          </Card>
        )}

        <Button variant="outline" fullWidth onClick={() => navigate(`/consolidated-cargos/${detail.id}`)}>
          함께 가는 화물 자세히 보기
        </Button>
      </div>
      <BottomNav />
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl bg-gray-50 py-2">
      <p className="text-xs text-gray-400">{label}</p>
      <p className="font-bold text-gray-900">{value}</p>
    </div>
  );
}
