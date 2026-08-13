import { pct } from '../components/ui';

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
    <div className="flex min-h-full flex-col bg-[#f4f7fb]">
      <header className="flex flex-col gap-4 bg-white px-5 pb-4 pt-7">
        <div className="flex items-center gap-3">
          <div>
            <h1 className="text-[20px] font-black tracking-[-.5px] text-[#111c2e]">{route}</h1>
          </div>
          <button
            type="button"
            onClick={onNotifications}
            aria-label={`알림 ${unread}개`}
            className="relative ml-auto grid size-[38px] place-items-center rounded-xl bg-[#f3f6fb] text-[#40506a]"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></svg>
            {unread > 0 && <span className="absolute top-2 right-[9px] w-[7px] h-[7px] rounded-full bg-action" />}
          </button>
        </div>

        <div className="flex h-[42px] items-center gap-2 rounded-[14px] bg-[#f2f5fa] px-3 text-[#9aa5b7]"><svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg><span className="text-[13px] font-semibold">노선 · 역 검색</span></div>

        <div className="flex gap-2 overflow-x-auto [scrollbar-width:none]">
          {filters.map((filter) => (
            <button
              key={filter.label}
              onClick={() => onFilter(filter.label)}
              className={`whitespace-nowrap rounded-full px-[14px] py-2 text-[12px] font-extrabold transition ${
                filter.active ? 'bg-[#2d49bd] text-white' : 'border border-[#dfe5f0] bg-white text-[#6d7a90]'
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </header>

      <div className="flex flex-1 flex-col gap-3 px-5 py-5">
        {loading && <p className="py-10 text-center text-sm text-ink-faint">열차 정보를 불러오는 중…</p>}
        {!loading && error && <p className="rounded-chip bg-danger-soft px-4 py-3 text-center text-sm font-semibold text-danger">{error}</p>}
        {!loading && !error && listings.length === 0 && (
          <p className="py-10 text-center text-sm text-ink-faint">운행 예정 열차가 없어요.</p>
        )}
        {listings.map((listing, index) => (
          <button type="button" key={listing.listingId} onClick={() => onOpen(listing.listingId)} className="rounded-[22px] border border-[#dce3ee] bg-white p-4 text-left shadow-[0_5px_16px_rgba(43,61,91,.04)]">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0"><div className="flex items-center gap-2"><span className="grid size-8 place-items-center rounded-[10px] bg-brand-50 text-brand-700"><TrainIcon /></span><h2 className="truncate text-[15px] font-black text-[#182237]">{listing.title}</h2></div><p className="mt-2 truncate text-[12px] font-semibold text-[#7a879c]">{listing.subtitle}</p></div>
              <span className={`shrink-0 rounded-full px-2.5 py-1 text-[11px] font-black ${listing.live ? "bg-[#fff3dc] text-[#b87400]" : "bg-brand-50 text-brand-700"}`}>{listing.live ? "모집중" : "참여 가능"}</span>
            </div>
            <div className="mt-4 flex items-end justify-between"><div><p className="text-[12px] font-semibold text-[#7a879c]">{listing.departureLabel}</p><p className="mt-1 text-[14px] font-black text-brand-700">{listing.priceLabel.replace("예상 ", "")}</p></div><div className="text-right"><p className="text-[12px] font-black text-[#27855a]">최대 {pct(listing.expectedSavedRate)} 절감</p><p className="mt-1 text-[12px] font-semibold text-[#7a879c]">{listing.remainingLabel}</p></div></div>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-[#edf1f7]"><span className="block h-full rounded-full bg-brand-600" style={{ width: `${Math.max(12, Math.min(90, (listing.segments.reduce((sum, segment) => sum + segment.cbm, 0) / Math.max(1, listing.capacity)) * 100))}%` }} /></div>
            {index === 0 && <p className="mt-2 text-right text-[12px] font-black text-brand-700">이 노선으로 등록 ›</p>}
          </button>
        ))}
        {!loading && listings.length > 0 && <div className="mt-1 rounded-[22px] bg-[#2d49bd] px-4 py-4 text-white"><p className="text-[13px] font-black">원하는 노선이 없나요?</p><p className="mt-1 text-[12px] leading-5 text-white/75">화물을 먼저 등록하면 조건에 맞는 열차와<br />공동화물을 자동으로 찾아 알려드려요.</p></div>}
      </div>
    </div>
  );
}

function TrainIcon() {
  return <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.9"><rect x="5" y="3" width="14" height="15" rx="3"/><path d="M8 7h8M8 11h8M8 21l2-3m6 0 2 3M8 15h.01M16 15h.01" strokeLinecap="round"/></svg>;
}
