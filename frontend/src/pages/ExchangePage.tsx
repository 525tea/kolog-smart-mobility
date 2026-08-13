import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getUpcomingTrains } from "../api/train";
import { browseConsolidations } from "../api/consolidation";
import type { ConsolidationDetailResponse, TrainResponse } from "../types";
import MarketplaceScreen, { type MarketplaceViewItem } from "../screens/MarketplaceScreen";
import { useNotifications } from "../context/NotificationContext";

const BASE_RATE_PER_KG = 500;
type SortKey = "price" | "capacity" | "time";
const SORT_FILTERS: Record<string, SortKey> = { "가격 낮은 순": "price", "8월 3주": "time" };
const DETAIL_FILTERS = ["냉장", "10 CBM 이상"] as const;

export function ExchangePage() {
  const navigate = useNavigate();
  const { unreadCount } = useNotifications();
  const [trains, setTrains] = useState<TrainResponse[]>([]);
  const [groups, setGroups] = useState<ConsolidationDetailResponse[]>([]);
  const [sort, setSort] = useState<SortKey>("price");
  const [detailFilters, setDetailFilters] = useState<Set<string>>(() => new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([getUpcomingTrains(), browseConsolidations()])
      .then(([t, g]) => { setTrains(t); setGroups(g); })
      .catch(() => setError("열차 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요."))
      .finally(() => setLoading(false));
  }, []);

  const rows = trains.flatMap((train) =>
    train.wagons.map((wagon) => {
      // 실제 동적 가격은 특정 화물 기준으로만 계산되므로, 여기서는 기준운임 대비 잔여율로 대략적인 우선순위를 매긴다.
      const estimatedRate = Math.round(BASE_RATE_PER_KG * (1 - Math.min(wagon.loadFactorPercent, 90) / 300));
      const groupCount = groups.filter(
        (g) => g.originStation === train.originStation && g.destinationStation === train.destinationStation,
      ).length;
      return { train, wagon, estimatedRate, groupCount };
    }),
  );

  const filtered = rows.filter(({ wagon }) => {
    if (detailFilters.has("냉장") && wagon.wagonType !== "REFRIGERATED") return false;
    if (detailFilters.has("10 CBM 이상") && wagon.remainingWeightKg < 10_000) return false;
    return true;
  });
  const sorted = [...filtered].sort((a, b) => {
    if (sort === "price") return a.estimatedRate - b.estimatedRate;
    if (sort === "capacity") return b.wagon.remainingWeightKg - a.wagon.remainingWeightKg;
    return new Date(a.train.departureAt).getTime() - new Date(b.train.departureAt).getTime();
  });
  const listings: MarketplaceViewItem[] = sorted.slice(0, 3).map(({ train, wagon, estimatedRate, groupCount }) => ({
    listingId: `${train.id}:${wagon.id}`,
    emoji: wagon.wagonType === "REFRIGERATED" ? "❄️" : "🚆",
    title: `${train.originStation} → ${train.destinationStation}`,
    subtitle: `${train.trainNumber} · ${wagon.wagonNumber} · 공동화물 ${groupCount}건`,
    departureLabel: new Date(train.departureAt).toLocaleString("ko-KR", { month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit" }),
    priceLabel: `${Math.round(estimatedRate * 42).toLocaleString()}원/CBM`,
    remainingLabel: `잔여 ${Math.max(1, Math.round(wagon.remainingWeightKg / Math.max(1, wagon.maxWeightKg) * 67))} TEU`,
    expectedSavedRate: Math.max(0, (BASE_RATE_PER_KG - estimatedRate) / BASE_RATE_PER_KG),
    capacity: wagon.maxWeightKg,
    segments: [{ owner: "OTHER", cbm: Math.max(0, wagon.maxWeightKg - wagon.remainingWeightKg) }],
    live: groupCount > 0,
    badge: groupCount === 0 ? { text: "참여 가능", tone: "neutral" } : undefined,
  }));

  function handleFilter(label: string) {
    const nextSort = SORT_FILTERS[label];
    if (nextSort) {
      setSort(nextSort);
      return;
    }
    setDetailFilters((current) => {
      const next = new Set(current);
      if (next.has(label)) next.delete(label);
      else next.add(label);
      return next;
    });
  }

  return (
    <MarketplaceScreen
      route="잔여용량 거래소"
      filters={[
        ...Object.entries(SORT_FILTERS).map(([label, key]) => ({ label, active: sort === key })),
        ...DETAIL_FILTERS.map((label) => ({ label, active: detailFilters.has(label) })),
      ]}
      listings={listings}
      unread={unreadCount}
      loading={loading}
      error={error}
      onFilter={handleFilter}
      onOpen={() => navigate("/cargo/new/form?mode=CO_LOAD")}
      onNotifications={() => navigate("/notifications")}
    />
  );
}
