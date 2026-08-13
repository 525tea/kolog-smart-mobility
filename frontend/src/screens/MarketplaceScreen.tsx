import { Card, Pill, IconChip, PulseDot, LoadBar, PhoneFrame, cbm, pct } from '../components/ui';
import type { CoCargoCandidate, MarketplaceListing } from '../types';

/* A-19 거래소 — 흰 카드 + 알약 뱃지 리스트 뷰 */

interface Props {
  route: string;
  filters: { label: string; active: boolean }[];
  listings: (MarketplaceListing & {
    emoji: string;
    badge?: { text: string; tone: 'warning' | 'neutral' | 'danger'; pulse?: boolean };
    live?: boolean;
    segments: { owner: string; cbm: number }[];
    capacityCbm: number;
  })[];
  unread: number;
  onFilter: (label: string) => void;
  onOpen: (listingId: string) => void;
}

export default function MarketplaceScreen({ route, filters, listings, unread, onFilter, onOpen }: Props) {
  return (
    <PhoneFrame>
      <header className="px-[22px] pt-[14px] pb-4 flex flex-col gap-[15px]">
        <div className="flex items-center gap-3">
          <div className="flex flex-col gap-[3px]">
            <span className="text-[11px] font-extrabold tracking-[.6px] text-ink-muted">공동화물 거래소</span>
            <h1 className="text-[22px] font-extrabold tracking-[-.7px] text-brand">{route}</h1>
          </div>
          <div className="ml-auto relative w-[38px] h-[38px] rounded-full bg-surface shadow-chip flex items-center justify-center text-[16px]">
            🔔
            {unread > 0 && <span className="absolute top-2 right-[9px] w-[7px] h-[7px] rounded-full bg-action" />}
          </div>
        </div>

        <div className="flex gap-2 overflow-x-auto [scrollbar-width:none]">
          {filters.map((f) => (
            <button
              key={f.label}
              onClick={() => onFilter(f.label)}
              className={`px-[14px] py-2 rounded-full text-[11.5px] font-extrabold whitespace-nowrap transition ${
                f.active ? 'bg-brand text-white' : 'bg-surface text-ink-muted shadow-chip'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </header>

      <div className="flex-1 overflow-auto px-[22px] pb-5 flex flex-col gap-[13px] [scrollbar-width:none]">
        {listings.map((l) => (
          <Card key={l.listingId} className="gap-[13px] cursor-pointer" >
            <div onClick={() => onOpen(l.listingId)} className="flex items-start gap-[11px]">
              <IconChip emoji={l.emoji} tone={l.badge?.tone === 'warning' ? 'warning' : 'info'} size={42} />
              <div className="flex-1 flex flex-col gap-1 min-w-0">
                <span className="text-[15.5px] font-extrabold text-ink">{l.cargoName}</span>
                <span className="text-[11.5px] font-semibold text-ink-muted truncate">
                  {l.shipperMasked} · {cbm(l.cbm)}
                </span>
              </div>
              {l.live ? (
                <span className="flex items-center gap-[6px] px-[11px] py-[5px] rounded-full bg-gold-soft">
                  <PulseDot size={7} />
                  <span className="text-[10.5px] font-extrabold text-gold-text">집하중</span>
                </span>
              ) : l.badge ? (
                <Pill tone={l.badge.tone} solid={l.badge.tone === 'warning'} pulse={l.badge.pulse}>
                  {l.badge.text}
                </Pill>
              ) : null}
            </div>

            <div className="h-px bg-divider" />

            <div className="flex items-center gap-2">
              <Pill tone="info">❄️ 냉장 -1~5℃</Pill>
              <Pill tone="success">절감 {pct(l.expectedSavedRate)}</Pill>
              <span className="ml-auto text-[11px] font-bold text-ink-muted">잔여 {cbm(l.remainingCbm)}</span>
            </div>

            <LoadBar segments={l.segments} capacityCbm={l.capacityCbm} />
          </Card>
        ))}
      </div>

      <TabBar active="market" />
    </PhoneFrame>
  );
}

function TabBar({ active }: { active: string }) {
  const tabs = [
    { id: 'home', emoji: '🏠', label: '홈' },
    { id: 'cargo', emoji: '📦', label: '화물' },
    { id: 'market', emoji: '🤝', label: '거래소' },
    { id: 'my', emoji: '👤', label: '마이' },
  ];
  return (
    <nav className="px-[22px] pt-3 pb-[30px] bg-surface shadow-dock flex justify-between">
      {tabs.map((t) => (
        <button key={t.id} className="flex-1 flex flex-col items-center gap-[5px]">
          <span className={`text-[17px] ${active === t.id ? '' : 'opacity-40'}`}>{t.emoji}</span>
          <span className={`text-[10px] font-extrabold ${active === t.id ? 'text-brand' : 'text-ink-faint'}`}>
            {t.label}
          </span>
        </button>
      ))}
    </nav>
  );
}
