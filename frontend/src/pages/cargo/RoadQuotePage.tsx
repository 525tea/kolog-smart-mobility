import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { AppHeader } from "../../components/layout/AppHeader";
import { Card } from "../../components/ui/Card";
import { Button } from "../../components/ui/Button";
import { getCargo } from "../../api/cargo";
import type { CargoResponse } from "../../types";
import { ApiError } from "../../api/client";

export function RoadQuotePage() {
  const { cargoId } = useParams();
  const navigate = useNavigate();
  const [cargo, setCargo] = useState<CargoResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { if (cargoId) getCargo(Number(cargoId)).then(setCargo).catch((cause) => setError(cause instanceof ApiError ? cause.message : "화물 정보를 불러오지 못했습니다.")); }, [cargoId]);
  if (!cargo) return <div className="p-8 text-center text-sm text-gray-400">{error ?? "트럭 직배송 견적을 계산하는 중…"}</div>;

  const weight = cargo.weightKg ?? 50;
  const base = Math.max(98_000, Math.round(weight * 790));
  const surcharge = Math.round(base * cargo.surchargeRate);
  const total = base + surcharge + Math.round(cargo.fixedPowerFeeKrw);

  return (
    <div className="flex min-h-full flex-col">
      <AppHeader title="트럭 직배송 견적" subtitle="Road Delivery Plan B" onBack={() => navigate(-1)} />
      <main className="flex flex-1 flex-col gap-4 px-5 py-5">
        <Card className="border-amber-200 bg-amber-50">
          <p className="text-xs font-bold text-amber-700">철도 권역 밖 Plan B</p>
          <h2 className="mt-1 text-xl font-black text-gray-950">100% 트럭 직배송</h2>
          <p className="mt-2 text-sm text-gray-600">입력한 출발지와 도착지를 바꾸지 않고 단독 차량으로 운송하는 예상 견적입니다.</p>
        </Card>
        <Card>
          <p className="font-black text-gray-900">{cargo.originStation} → {cargo.destinationStation}</p>
          <div className="mt-4 flex flex-col gap-2 text-sm">
            <Row label={`기본 도로운임 (${weight}kg)`} value={`₩${base.toLocaleString()}`} />
            {surcharge > 0 && <Row label={`화물 취급 할증 ${Math.round(cargo.surchargeRate * 100)}%`} value={`+₩${surcharge.toLocaleString()}`} />}
            {cargo.fixedPowerFeeKrw > 0 && <Row label="콜드체인 전력 유지비" value={`+₩${cargo.fixedPowerFeeKrw.toLocaleString()}`} />}
          </div>
          <div className="mt-4 flex items-end justify-between border-t border-gray-100 pt-4">
            <span className="font-bold text-gray-700">예상 견적</span>
            <strong className="text-2xl font-black text-brand-800">₩{total.toLocaleString()}</strong>
          </div>
        </Card>
        <Card>
          <p className="text-sm font-bold text-gray-900">예상 운송 조건</p>
          <div className="mt-3 grid grid-cols-2 gap-2 text-center text-sm">
            <div className="rounded-xl bg-gray-50 p-3"><p className="text-xs text-gray-400">운송 방식</p><strong>단독 배차</strong></div>
            <div className="rounded-xl bg-gray-50 p-3"><p className="text-xs text-gray-400">예상 시간</p><strong>5시간 30분</strong></div>
          </div>
          <p className="mt-3 text-xs leading-5 text-gray-400">실제 운임과 배차 가능 여부는 도로 운송사 연동 시 확정됩니다.</p>
        </Card>
        <div className="mt-auto"><Button fullWidth onClick={() => navigate("/home")}>견적 확인 완료</Button></div>
      </main>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return <div className="flex justify-between"><span className="text-gray-500">{label}</span><span className="font-semibold text-gray-900">{value}</span></div>;
}
