import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { CargoWizardHeader } from "../../components/layout/CargoWizardHeader";
import { getCandidates, getConsolidationDetail, joinConsolidation } from "../../api/consolidation";
import { getCargo } from "../../api/cargo";
import { getMyPayments } from "../../api/reservation";
import { ApiError } from "../../api/client";
import { useNotifications } from "../../context/NotificationContext";
import type { CargoResponse, ConsolidationCandidateResponse, ConsolidationDetailResponse, ParticipationPaymentResponse } from "../../types";
import CheckoutScreen, { type CheckoutFareLine } from "../../screens/CheckoutScreen";

// 백엔드 PricingPolicy.RATE_PER_KG와 동일한 값 (kg당 기준운임, 할인 전). 화면에 "기본 철도운임" 항목을 보여주기 위해 사용.
const BASE_RATE_PER_KG = 500;

export function CargoCheckoutPage() {
  const { cargoId, groupId } = useParams();
  const [searchParams] = useSearchParams();
  const firstMile = searchParams.get("firstMile") === "1";
  const lastMile = searchParams.get("lastMile") === "1";
  const navigate = useNavigate();
  const { refresh: refreshNotifications } = useNotifications();
  const [cargo, setCargo] = useState<CargoResponse | null>(null);
  const [detail, setDetail] = useState<ConsolidationDetailResponse | null>(null);
  const [pricing, setPricing] = useState<ConsolidationCandidateResponse | null>(null);
  const [completedPayment, setCompletedPayment] = useState<ParticipationPaymentResponse | null>(null);
  const [autoReschedule, setAutoReschedule] = useState(true);
  const [now, setNow] = useState(() => Date.now());
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!cargoId || !groupId) return;
    let cancelled = false;
    Promise.all([getCargo(Number(cargoId)), getConsolidationDetail(Number(groupId)), getMyPayments()])
      .then(async ([cargoRes, detailRes, payments]) => {
        if (cancelled) return;
        setCargo(cargoRes);
        setDetail(detailRes);
        setCompletedPayment(payments.find((payment) => payment.cargoOrderId === Number(cargoId) && payment.consolidatedCargoId === Number(groupId)) ?? null);
        try {
          const candidates = await getCandidates(Number(cargoId));
          if (!cancelled) setPricing(candidates.find((c) => c.consolidatedCargoId === Number(groupId)) ?? null);
        } catch (candidateError) {
          if (cargoRes.status !== "PARTICIPATING" && cargoRes.status !== "RESERVED") throw candidateError;
        }
      })
      .catch((err) => !cancelled && setError(err instanceof ApiError ? err.message : "정보를 불러오지 못했습니다."));
    return () => {
      cancelled = true;
    };
  }, [cargoId, groupId]);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

  async function handleConfirm() {
    if (!cargoId || !groupId) return;
    setSubmitting(true);
    setError(null);
    try {
      const result = await joinConsolidation(
        Number(groupId),
        Number(cargoId),
        autoReschedule ? "NEXT_TRAIN" : "AUTO_REFUND",
        { firstMile, lastMile },
      );
      // 참여 접수 알림은 서버(NotificationService)가 이미 생성했다 - 배지/목록을 즉시 갱신만 한다.
      refreshNotifications();
      navigate(`/cargo/${cargoId}/status?groupId=${result.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "예약에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  if (cargo && detail && !pricing && (cargo.status === "PARTICIPATING" || cargo.status === "RESERVED")) {
    return (
      <div className="flex min-h-full flex-col bg-[#f4f7fb]">
        <CargoWizardHeader step="checkout" onBack={() => navigate(-1)} />
        <main className="flex flex-1 flex-col px-5 py-6">
          <section className="rounded-[26px] bg-[#e7edff] p-5">
            <div className="flex items-center justify-between">
              <p className="text-[13px] font-black text-brand-700">결제 완료</p>
              <span className="rounded-full bg-white px-3 py-1 text-[12px] font-black text-[#27855a]">완료</span>
            </div>
            <strong className="mt-2 block text-[30px] text-[#3049bd]">{completedPayment ? `${completedPayment.totalCost.toLocaleString()}원` : "결제 처리 완료"}</strong>
            <p className="mt-2 text-[13px] font-bold text-[#68758b]">{detail.originStation} → {detail.destinationStation}</p>
          </section>
          <section className="mt-4 rounded-[22px] border border-[#dfe5f0] bg-white p-5">
            <h2 className="text-[15px] font-black text-[#182237]">예약 및 결제가 완료됐어요</h2>
            <p className="mt-2 text-[13px] leading-5 font-semibold text-[#68758b]">공동화물 진행 상태와 열차 배정 결과는 운송 현황에서 계속 확인할 수 있어요.</p>
          </section>
          <button onClick={() => navigate(`/cargo/${cargo.id}/status?groupId=${detail.id}`)} className="mt-auto h-14 rounded-2xl bg-brand-700 text-[16px] font-black text-white">운송 현황 보기</button>
        </main>
      </div>
    );
  }

  if (!cargo || !detail || !pricing) {
    return (
      <div className="flex min-h-full flex-col">
        <CargoWizardHeader step="checkout" onBack={() => navigate(-1)} />
        <div className="flex flex-1 items-center justify-center text-sm text-gray-400">
          {error ?? "불러오는 중…"}
        </div>
      </div>
    );
  }

  const deadlineMs = new Date(detail.recruitmentDeadline).getTime();
  const remainingMs = Math.max(0, deadlineMs - now);
  const remainingLabel = formatRemainingTime(remainingMs);

  const weight = cargo.weightKg ?? 0;
  const baseCost = Math.round(weight * BASE_RATE_PER_KG);
  // 동적 할인 적용 후, 위험물 할증 적용 전 금액 (백엔드: applyHazardSurcharge(weight * priceQuote.ratePerKg())의 안쪽 값)
  const freightBeforeSurcharge = Math.round(weight * pricing.appliedRatePerKg);
  const discount = Math.round(baseCost - freightBeforeSurcharge);
  const hazardSurcharge = Math.round(pricing.estimatedFreightForOrder - freightBeforeSurcharge);
  const fixedPowerFee = Math.round(pricing.fixedPowerFeeKrw);
  const variableSurcharge = Math.max(0, hazardSurcharge - fixedPowerFee);
  const insuranceFee = Math.round(pricing.insuranceFeeKrw);
  const platformFee = Math.round(pricing.platformFeeKrw);
  const firstMileFee = firstMile ? 24_000 : 0;
  const lastMileFee = lastMile ? 19_000 : 0;
  const finalCost = Math.round(pricing.totalPayableKrw + firstMileFee + lastMileFee);

  const fareLines: CheckoutFareLine[] = [
    { label: `기본 철도운임 (${weight}kg)`, value: baseCost },
    { label: `공동화물 할인 (-${Math.round(pricing.appliedDiscountRate * 100)}%)`, value: discount, discount: true },
    { label: `화물 취급 할증 (+${Math.round(pricing.hazardSurchargeRate * 100)}%)`, value: variableSurcharge },
    { label: "콜드체인 전력 유지비", value: fixedPowerFee },
    { label: "적재보험료", value: insuranceFee },
    { label: "플랫폼 이용 수수료", value: platformFee },
    { label: "집하 연계운송", value: firstMileFee },
    { label: "라스트마일 연계운송", value: lastMileFee },
  ];

  return (
    <CheckoutScreen
      route={`${detail.originStation} → ${detail.destinationStation}`}
      trainNumber={pricing.trainNumber}
      schedule={`${new Date(pricing.departureAt).toLocaleString("ko-KR")} 출발`}
      temperatureLabel={temperatureLabel(cargo.temperatureCondition)}
      recruitedWeightKg={detail.recruitedWeightKg}
      targetWeightKg={detail.targetWeightKg}
      fareLines={fareLines}
      total={finalCost}
      savedAmount={Math.round(pricing.estimatedSavingsForOrder)}
      holdRemaining={remainingMs === 0 ? "마감" : `${remainingLabel} 남음`}
      autoReschedule={autoReschedule}
      onAutoRescheduleChange={setAutoReschedule}
      onBack={() => navigate(-1)}
      onPay={() => void handleConfirm()}
      paying={submitting}
      expired={remainingMs === 0}
      error={error}
    />
  );
}

function temperatureLabel(value: CargoResponse["temperatureCondition"]) {
  return { ROOM: "상온", CONSTANT: "정온", REFRIGERATED: "냉장", FROZEN: "냉동" }[value ?? "ROOM"];
}

function formatRemainingTime(remainingMs: number) {
  const totalSeconds = Math.floor(remainingMs / 1000);
  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3_600);
  const minutes = Math.floor((totalSeconds % 3_600) / 60);
  const seconds = totalSeconds % 60;
  if (days > 0) return [days > 0 && `${days}일`, hours > 0 && `${hours}시간`, `${minutes}분`].filter(Boolean).join(" ");
  if (hours > 0) return `${hours}시간 ${minutes}분`;
  return `${minutes}분 ${String(seconds).padStart(2, "0")}초`;
}
