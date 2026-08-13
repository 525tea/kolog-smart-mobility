import { useState } from 'react';
import {
  Card, CardTitle, Divider, Pill, IconChip, ProgressNodes,
  PrimaryButton, SelectRow, PhoneFrame, won,
} from '../components/ui';
import type { FareBreakdown, PaymentMethodId, Reservation } from '../types';

/* A-16 결제 확정 — 앱 전체에서 유일하게 action(#FF8C00) 버튼을 쓰는 화면 */

interface Props {
  reservation: Reservation;
  methods: { methodId: PaymentMethodId; label: string; detail: string; emoji: string }[];
  savedAmount: number;
  co2SavedKg: number;
  holdRemaining: string;          // "14:52" — useHoldTimer 훅 결과
  onBack: () => void;
  onPay: (methodId: PaymentMethodId) => void;
  paying?: boolean;
}

export default function CheckoutScreen({
  reservation, methods, savedAmount, co2SavedKg, holdRemaining, onBack, onPay, paying,
}: Props) {
  const [method, setMethod] = useState<PaymentMethodId>('card');
  const f: FareBreakdown = reservation.fare;

  const ctaLabel =
    method === 'later' ? '월 정산으로 확정'
    : method === 'bank' ? '계좌 정보 확인'
    : `${won(f.total)} 결제하기`;

  return (
    <PhoneFrame>
      <header className="px-[22px] py-[14px] flex items-center gap-[13px]">
        <button onClick={onBack} className="text-[18px] font-extrabold text-brand">←</button>
        <h1 className="text-[18px] font-extrabold tracking-[-.5px] text-brand">결제</h1>
        <span className="ml-auto">
          <Pill tone="warning" solid pulse>{holdRemaining} 남음</Pill>
        </span>
      </header>

      <div className="flex-1 overflow-auto px-[22px] pb-5 flex flex-col gap-[13px] [scrollbar-width:none]">

        {/* 여정 요약 */}
        <Card className="gap-[14px]">
          <div className="flex items-center gap-[11px]">
            <IconChip emoji="🚆" />
            <div className="flex-1 flex flex-col gap-[3px] min-w-0">
              <span className="text-[15px] font-extrabold text-ink">3061 화물열차</span>
              <span className="text-[11.5px] font-semibold text-ink-muted truncate">
                8/20 21:10 부산진 → 8/21 05:40 오봉
              </span>
            </div>
            <Pill tone="info">냉장</Pill>
          </div>
          <ProgressNodes
            nodes={[
              { label: '집하', state: 'DONE' },
              { label: '철도 운송', state: 'CURRENT' },
              { label: '라스트마일', state: 'PENDING' },
            ]}
          />
        </Card>

        {/* 운임 명세 */}
        <Card className="gap-[13px]">
          <CardTitle>운임 명세</CardTitle>
          <div className="flex flex-col gap-[10px]">
            <FareRow label="철도 운임 (공동화물 적용)" value={f.railFare} />
            <FareRow label="집하 (진주 → 부산진)" value={f.pickupFare} />
            <FareRow label="라스트마일 (오봉 → 의왕)" value={f.lastMileFare} />
            <FareRow label="부가세" value={f.vat} />
          </div>
          <Divider />
          <div className="flex justify-between items-baseline">
            <span className="text-[13.5px] font-extrabold text-ink">총 결제금액</span>
            <span className="text-[23px] font-extrabold tracking-[-.8px] text-brand">{won(f.total)}</span>
          </div>
          <div className="flex items-center gap-2 px-[13px] py-[11px] rounded-chip bg-teal-soft">
            <span className="text-[15px]">🌱</span>
            <span className="text-[11.5px] font-bold text-teal-deep">
              단독 운송 대비 {won(savedAmount)} 절감 · CO₂ {co2SavedKg}kg 감축
            </span>
          </div>
        </Card>

        {/* 결제 수단 */}
        <Card className="gap-2">
          <CardTitle>결제 수단</CardTitle>
          {methods.map((m) => (
            <SelectRow
              key={m.methodId}
              emoji={m.emoji}
              title={m.label}
              detail={m.detail}
              selected={method === m.methodId}
              onSelect={() => setMethod(m.methodId)}
            />
          ))}
        </Card>
      </div>

      <footer className="px-[22px] pt-[14px] pb-[30px] bg-surface shadow-dock flex flex-col gap-[10px]">
        <span className="text-[10.5px] font-semibold text-ink-faint text-center">
          결제 시 KO-LOG 운송약관 및 공동화물 배상규정에 동의합니다
        </span>
        <PrimaryButton variant="action" onClick={() => onPay(method)} disabled={paying}>
          {paying ? '결제 처리중…' : ctaLabel}
        </PrimaryButton>
      </footer>
    </PhoneFrame>
  );
}

function FareRow({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex justify-between text-[13px]">
      <span className="font-semibold text-ink-muted">{label}</span>
      <span className="font-extrabold text-ink">{won(value)}</span>
    </div>
  );
}
