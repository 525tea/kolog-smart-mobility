import type { ReactNode } from 'react';

function classes(...values: Array<string | false | null | undefined>) {
  return values.filter(Boolean).join(' ');
}

/* ─────────── Card ─────────── */
export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={classes('bg-surface rounded-card shadow-card p-[18px] flex flex-col', className)}>
      {children}
    </div>
  );
}

export function CardTitle({ children }: { children: ReactNode }) {
  return <span className="text-[13px] font-extrabold text-brand">{children}</span>;
}

export function Divider() {
  return <div className="h-px bg-divider" />;
}

/* ─────────── Pill (뱃지) ───────────
   tone 별 색 매핑. 서버가 badgeTone 을 내려주면 그대로 전달. */
export type Tone = 'info' | 'success' | 'warning' | 'danger' | 'neutral';

const TONE: Record<Tone, string> = {
  info: 'bg-brand-soft text-brand',
  success: 'bg-teal-soft text-teal-deep',
  warning: 'bg-gold-soft text-gold-text',
  danger: 'bg-danger-soft text-danger',
  neutral: 'bg-divider text-ink-muted',
};

export function Pill({
  children, tone = 'neutral', solid = false, pulse = false,
}: { children: ReactNode; tone?: Tone; solid?: boolean; pulse?: boolean }) {
  const solidBg = tone === 'warning' ? 'bg-gold text-white'
    : tone === 'danger' ? 'bg-danger text-white'
    : tone === 'success' ? 'bg-teal text-white'
    : 'bg-brand text-white';
  return (
    <span
      className={classes(
        'px-[11px] py-[5px] rounded-full text-[10.5px] font-extrabold whitespace-nowrap',
        solid ? solidBg : TONE[tone],
        pulse && 'animate-goldPulse',
      )}
    >
      {children}
    </span>
  );
}

/* ─────────── IconChip (아이콘 사각칩) ─────────── */
export function IconChip({
  emoji, tone = 'info', size = 40,
}: { emoji: string; tone?: Tone; size?: number }) {
  const bg = { info: 'bg-brand-soft', success: 'bg-teal-soft', warning: 'bg-gold-soft', danger: 'bg-danger-soft', neutral: 'bg-base' }[tone];
  return (
    <div
      className={classes('rounded-chip flex items-center justify-center shrink-0', bg)}
      style={{ width: size, height: size, fontSize: size * 0.47 }}
    >
      {emoji}
    </div>
  );
}

/* ─────────── PulseDot (실시간 위치 · 현재 노드) ─────────── */
export function PulseDot({ size = 11, color = '#F5B041' }: { size?: number; color?: string }) {
  return (
    <div className="relative flex items-center justify-center shrink-0" style={{ width: size, height: size }}>
      <div className="absolute rounded-full animate-ring" style={{ width: size, height: size, background: color }} />
      <div className="relative rounded-full" style={{ width: size, height: size, background: color }} />
    </div>
  );
}

/* ─────────── ProgressNodes (구간 진행 바) ───────────
   nodes: DONE 이전 구간은 brand, 이후는 divider, CURRENT 는 골드 펄스, DELAYED 는 danger */
export type NodeState = 'DONE' | 'CURRENT' | 'PENDING' | 'DELAYED' | 'FAILED';

export function ProgressNodes({ nodes }: { nodes: { label: string; state: NodeState }[] }) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-[9px]">
        {nodes.map((n, i) => {
          const delayed = n.state === 'DELAYED' || n.state === 'FAILED';
          const filled = n.state === 'DONE';
          return (
            <div key={n.label} className="contents">
              {n.state === 'CURRENT' || delayed ? (
                <PulseDot color={delayed ? '#B4531F' : '#F5B041'} />
              ) : (
                <div
                  className={classes('w-[9px] h-[9px] rounded-full shrink-0', filled ? 'bg-brand' : 'bg-hairline')}
                />
              )}
              {i < nodes.length - 1 && (
                <div className={classes('flex-1 h-[3px]', filled ? 'bg-brand' : 'bg-divider')} />
              )}
            </div>
          );
        })}
      </div>
      <div className="flex justify-between text-[10.5px] font-bold text-ink-muted">
        {nodes.map((n) => <span key={n.label}>{n.label}</span>)}
      </div>
    </div>
  );
}

/* ─────────── LoadBar (컨테이너 적재 시각화) ─────────── */
const SEG_COLOR: Record<string, string> = {
  MINE: '#1B5CF0', CO_CARGO: '#3D48A8', OTHER: '#F5B041',
};

export function LoadBar({
  segments, capacityCbm, height = 7,
}: { segments: { owner: string; cbm: number; colorKey?: string }[]; capacityCbm: number; height?: number }) {
  return (
    <div className="rounded-[5px] bg-divider overflow-hidden flex" style={{ height }}>
      {segments.filter((s) => s.owner !== 'FREE').map((s, i) => (
        <div key={i} style={{ width: `${(s.cbm / capacityCbm) * 100}%`, background: SEG_COLOR[s.owner] ?? '#3D48A8' }} />
      ))}
    </div>
  );
}

/* ─────────── PrimaryButton ───────────
   variant="action" 은 앱 전체에서 '단 하나의 최종 CTA' 에만 사용 (결제/최종 확정). */
export function PrimaryButton({
  children, onClick, variant = 'brand', disabled,
}: { children: ReactNode; onClick?: () => void; variant?: 'brand' | 'action'; disabled?: boolean }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={classes(
        'h-[56px] w-full rounded-cta text-white text-[16px] font-extrabold tracking-[-.3px]',
        'flex items-center justify-center transition active:scale-[.985] disabled:opacity-40',
        variant === 'action' ? 'bg-action shadow-cta' : 'bg-brand',
      )}
    >
      {children}
    </button>
  );
}

/* ─────────── Radio row (결제수단 등) ─────────── */
export function SelectRow({
  emoji, title, detail, selected, onSelect,
}: { emoji: string; title: string; detail: string; selected: boolean; onSelect: () => void }) {
  return (
    <div
      onClick={onSelect}
      className={classes(
        'flex items-center gap-[11px] px-[13px] py-3 rounded-[14px] cursor-pointer transition',
        selected ? 'bg-base' : 'bg-transparent hover:bg-base/60',
      )}
    >
      <div className="w-[34px] h-[34px] rounded-[11px] bg-surface shadow-chip flex items-center justify-center text-[16px] shrink-0">
        {emoji}
      </div>
      <div className="flex-1 flex flex-col gap-[2px] min-w-0">
        <span className="text-[13.5px] font-extrabold text-ink">{title}</span>
        <span className="text-[11px] font-semibold text-ink-muted truncate">{detail}</span>
      </div>
      <div
        className={classes(
          'w-5 h-5 rounded-full flex items-center justify-center text-[11px] font-extrabold shrink-0',
          selected ? 'bg-brand text-white' : 'bg-divider text-transparent',
        )}
      >
        ✓
      </div>
    </div>
  );
}

/* ─────────── PhoneFrame (프로토타입 전용 · 실제 앱에서는 제거) ─────────── */
export function PhoneFrame({ children }: { children: ReactNode }) {
  return (
    <div className="w-[390px] h-[844px] rounded-phone bg-base overflow-hidden shadow-[0_20px_50px_rgba(10,44,116,.15)] flex flex-col">
      <div className="px-[22px] pt-4 flex justify-between text-[12px] font-extrabold text-ink-muted">
        <span>10:38</span><span>5G · 82%</span>
      </div>
      {children}
    </div>
  );
}

/* ─────────── 포맷터 ─────────── */
export const won = (n: number) => `${n.toLocaleString('ko-KR')}원`;
export const pct = (r: number) => `${r > 0 ? '+' : ''}${Math.round(r * 100)}%`;
export const cbm = (n: number) => `${n.toFixed(1)} CBM`;
