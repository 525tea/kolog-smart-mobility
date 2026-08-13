import { Card, IconChip, LoadBar, Pill, PulseDot, pct } from '../components/ui';

export interface MarketplaceViewItem {
  listingId: string;
  emoji: string;
  title: string;
  subtitle: string;
  departureLabel: string;
  priceLabel: string;
  remainingLabel: string;
  expectedSavedRate: number;
  capacity: number;
  segments: { owner: string; cbm: number }[];
  badge?: { text: string; tone: 'warning' | 'neutral' | 'danger'; pulse?: boolean };
  live?: boolean;
}

interface Props {
  route: string;
  filters: { label: string; active: boolean }[];
  listings: MarketplaceViewItem[];
  unread: number;
  loading?: boolean;
  error?: string | null;
  onFilter: (label: string) => void;
  onOpen: (listingId: string) => void;
  onNotifications: () => void;
}

/** A-19 거래소. 화면 모양은 최신 프로토타입을 따르고 값과 이동은 실제 백엔드 데이터로 받는다. */
export default function MarketplaceScreen({
  route, filters, listings, unread, loading = false, error, onFilter, onOpen, onNotifications,
}: Props) {
  return (
    <div className="flex min-h-full flex-col bg-base">
      <header className="px-[22px] pt-[22px] pb-4 flex flex-col gap-[15px]">
        <div className="flex items-center gap-3">
          <div className="flex flex-col gap-[3px]">
            <span className="text-[11px] font-extrabold tracking-[.6px] text-ink-muted">공동화물 거래소</span>
            <h1 className="text-[22px] font-extrabold tracking-[-.7px] text-brand">{route}</h1>
          </div>
          <button
            type="button"
            onClick={onNotifications}
            aria-label={`알림 ${unread}개`}
            className="ml-auto relative w-[38px] h-[38px] rounded-full bg-surface shadow-chip flex items-center justify-center text-[16px]"
          >
            🔔
            {unread > 0 && <span className="absolute top-2 right-[9px] w-[7px] h-[7px] rounded-full bg-action" />}
          </button>
        </div>

        <div className="flex gap-2 overflow-x-auto [scrollbar-width:none]">
          {filters.map((filter) => (
            <button
              key={filter.label}
              onClick={() => onFilter(filter.label)}
              className={`px-[14px] py-2 rounded-full text-[11.5px] font-extrabold whitespace-nowrap transition ${
                filter.active ? 'bg-brand text-white' : 'bg-surface text-ink-muted shadow-chip'
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </header>

      <div className="flex-1 px-[22px] pb-5 flex flex-col gap-[13px]">
        {loading && <p className="py-10 text-center text-sm text-ink-faint">열차 정보를 불러오는 중…</p>}
        {!loading && error && <p className="rounded-chip bg-danger-soft px-4 py-3 text-center text-sm font-semibold text-danger">{error}</p>}
        {!loading && !error && listings.length === 0 && (
          <p className="py-10 text-center text-sm text-ink-faint">운행 예정 열차가 없어요.</p>
        )}
        {listings.map((listing) => (
          <Card key={listing.listingId} className="gap-[13px] cursor-pointer">
            <button type="button" onClick={() => onOpen(listing.listingId)} className="flex items-start gap-[11px] text-left">
              <IconChip emoji={listing.emoji} tone={listing.badge?.tone === 'warning' ? 'warning' : 'info'} size={42} />
              <span className="flex-1 flex flex-col gap-1 min-w-0">
                <span className="text-[15.5px] font-extrabold text-ink">{listing.title}</span>
                <span className="text-[11.5px] font-semibold text-ink-muted truncate">{listing.subtitle}</span>
              </span>
              {listing.live ? (
                <span className="flex items-center gap-[6px] px-[11px] py-[5px] rounded-full bg-gold-soft">
                  <PulseDot size={7} />
                  <span className="text-[10.5px] font-extrabold text-gold-text">모집중</span>
                </span>
              ) : listing.badge ? (
                <Pill tone={listing.badge.tone} solid={listing.badge.tone === 'warning'} pulse={listing.badge.pulse}>
                  {listing.badge.text}
                </Pill>
              ) : null}
            </button>

            <div className="h-px bg-divider" />
            <div className="flex items-center gap-2">
              <Pill tone="info">🚆 {listing.departureLabel}</Pill>
              <Pill tone="success">절감 {pct(listing.expectedSavedRate)}</Pill>
              <span className="ml-auto text-[11px] font-bold text-ink-muted">{listing.remainingLabel}</span>
            </div>
            <LoadBar segments={listing.segments} capacityCbm={listing.capacity} />
            <button type="button" onClick={() => onOpen(listing.listingId)} className="text-right text-[12px] font-extrabold text-brand">
              {listing.priceLabel} · 이 노선으로 등록 ›
            </button>
          </Card>
        ))}
      </div>
    </div>
  );
}
