import { Card, CardTitle, Divider, IconChip, LoadBar, Pill, PrimaryButton, SelectRow, won } from '../components/ui';
import type { PaymentMethodId } from '../types';

export interface CheckoutFareLine {
  label: string;
  value: number;
  discount?: boolean;
}

interface Props {
  route: string;
  trainNumber: string;
  schedule: string;
  temperatureLabel: string;
  recruitedWeightKg: number;
  targetWeightKg: number;
  fareLines: CheckoutFareLine[];
  total: number;
  savedAmount: number;
  holdRemaining: string;
  autoReschedule: boolean;
  onAutoRescheduleChange: (checked: boolean) => void;
  onBack: () => void;
  onPay: (methodId: PaymentMethodId) => void;
  paying?: boolean;
  expired?: boolean;
  error?: string | null;
}

/** A-16 결제 확정. 최신 UI를 사용하되 표시 값과 결제 처리는 현재 백엔드 계약에서 받는다. */
export default function CheckoutScreen({
  route, trainNumber, schedule, temperatureLabel, recruitedWeightKg, targetWeightKg,
  fareLines, total, savedAmount, holdRemaining, autoReschedule, onAutoRescheduleChange,
  onBack, onPay, paying = false, expired = false, error,
}: Props) {
  const method: PaymentMethodId = 'card';
  return (
    <div className="flex min-h-full flex-col bg-base">
      <header className="px-[22px] py-[14px] flex items-center gap-[13px]">
        <button onClick={onBack} className="text-[18px] font-extrabold text-brand" aria-label="뒤로가기">←</button>
        <h1 className="text-[18px] font-extrabold tracking-[-.5px] text-brand">결제</h1>
        <span className="ml-auto"><Pill tone={expired ? 'danger' : 'warning'} solid pulse={!expired}>{holdRemaining}</Pill></span>
      </header>

      <div className="flex-1 px-[22px] pb-5 flex flex-col gap-[13px]">
        <Card className="gap-[14px]">
          <div className="flex items-center gap-[11px]">
            <IconChip emoji="🚆" />
            <div className="flex-1 flex flex-col gap-[3px] min-w-0">
              <span className="text-[15px] font-extrabold text-ink">{trainNumber}</span>
              <span className="text-[11.5px] font-semibold text-ink-muted truncate">{schedule}</span>
              <span className="text-[11.5px] font-semibold text-ink-muted truncate">{route}</span>
            </div>
            <Pill tone="info">{temperatureLabel}</Pill>
          </div>
          <div>
            <div className="mb-2 flex justify-between text-[11px] font-bold text-ink-muted">
              <span>현재 모집 {recruitedWeightKg.toLocaleString()}kg</span>
              <span>목표 {targetWeightKg.toLocaleString()}kg</span>
            </div>
            <LoadBar
              capacityCbm={targetWeightKg}
              segments={[{ owner: 'CO_CARGO', cbm: Math.min(recruitedWeightKg, targetWeightKg) }]}
              height={8}
            />
          </div>
        </Card>

        <Card className="gap-[13px]">
          <CardTitle>운임 명세</CardTitle>
          <div className="flex flex-col gap-[10px]">
            {fareLines.filter((line) => line.value !== 0).map((line) => (
              <FareRow key={line.label} label={line.label} value={line.value} discount={line.discount} />
            ))}
          </div>
          <Divider />
          <div className="flex justify-between items-baseline">
            <span className="text-[13.5px] font-extrabold text-ink">총 결제금액</span>
            <span className="text-[23px] font-extrabold tracking-[-.8px] text-brand">{won(total)}</span>
          </div>
          {savedAmount > 0 && (
            <div className="flex items-center gap-2 px-[13px] py-[11px] rounded-chip bg-teal-soft">
              <span className="text-[15px]">🌱</span>
              <span className="text-[11.5px] font-bold text-teal-deep">단독 운송 대비 {won(savedAmount)} 절감</span>
            </div>
          )}
        </Card>

        <Card className="gap-2">
          <CardTitle>결제 수단</CardTitle>
          <SelectRow emoji="💳" title="가상 결제" detail="실제 승인 없이 데모 결제로 처리됩니다" selected onSelect={() => undefined} />
          <label className="mt-1 flex items-center gap-2 px-2 text-[12px] font-semibold text-ink-muted">
            <input type="checkbox" checked={autoReschedule} onChange={(event) => onAutoRescheduleChange(event.target.checked)} />
            미성립 시 다음 열차로 자동 이월
          </label>
        </Card>

        {error && <p className="rounded-chip bg-danger-soft px-3 py-2 text-sm font-semibold text-danger">{error}</p>}
      </div>

      <footer className="px-[22px] pt-[14px] pb-[30px] bg-surface shadow-dock flex flex-col gap-[10px]">
        <span className="text-[10.5px] font-semibold text-ink-faint text-center">결제 시 KO-LOG 운송약관 및 공동화물 배상규정에 동의합니다</span>
        <PrimaryButton variant="action" onClick={() => onPay(method)} disabled={paying || expired}>
          {paying ? '결제 처리중…' : expired ? '모집이 마감되었습니다' : `${won(total)} 결제하기`}
        </PrimaryButton>
      </footer>
    </div>
  );
}

function FareRow({ label, value, discount = false }: CheckoutFareLine) {
  return (
    <div className="flex justify-between text-[13px]">
      <span className="font-semibold text-ink-muted">{label}</span>
      <span className={`font-extrabold ${discount ? 'text-teal-deep' : 'text-ink'}`}>
        {discount ? `-${won(Math.abs(value))}` : `+${won(value)}`}
      </span>
    </div>
  );
}
