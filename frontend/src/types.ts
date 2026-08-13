// KO-LOG 화주앱 — 프론트/백엔드 공용 타입 (화주앱.dc.html 추출본)

export type ISODate = string;      // "2026-08-20"
export type ISODateTime = string;  // "2026-08-20T21:10:00+09:00"
export type Won = number;          // 원 단위 정수
export type Rate = number;         // -0.23 = -23%

export type BadgeTone = 'info' | 'success' | 'warning' | 'danger' | 'neutral';
export type TempMode = 'AMBIENT' | 'REFRIGERATED' | 'FROZEN';
export type ColorKey = 'blue' | 'coral' | 'amber' | 'violet' | 'teal';

export type ShipmentStatus =
  | 'DRAFT' | 'PENDING_PAYMENT' | 'CONFIRMED'
  | 'PICKUP_SCHEDULED' | 'PICKED_UP' | 'AT_ORIGIN_STATION'
  | 'IN_TRANSIT_RAIL' | 'AT_DEST_STATION' | 'LAST_MILE' | 'DELIVERED'
  | 'PICKUP_DELAYED' | 'RAIL_DELAYED' | 'DAMAGE_REPORTED'
  | 'CANCELLED' | 'EXPIRED';

export type NodeState = 'DONE' | 'CURRENT' | 'PENDING' | 'DELAYED' | 'FAILED';

export type PaymentMethodId = 'card' | 'later' | 'bank';
export type PaymentStatus = 'PAID' | 'AWAITING_DEPOSIT' | 'SETTLEMENT_SCHEDULED' | 'FAILED';

export type NotificationType =
  | 'MATCH_FOUND' | 'PAYMENT_DUE' | 'PICKUP_DELAY' | 'RAIL_DELAY'
  | 'DELIVERED' | 'SETTLEMENT' | 'DOC_REQUIRED';

/* ── 사용자 ── */
export interface User {
  userId: string;
  name: string;
  corpName: string;
  corpRegNo: string;
  role: 'SHIPPER' | 'CARRIER' | 'ADMIN';
}

/* ── 화물 ── */
export interface CargoDraft {
  name: string;
  originAddress: string;
  originStationCode: string;
  destAddress: string;
  destStationCode: string;
  desiredDate: ISODate;
  weightKg: number;
  cbm: number;
  packType: string;   // 'PALLET_T11' 등
  packCount: number;
}

export interface Classification {
  status: 'PROCESSING' | 'DONE' | 'FAILED';
  majorCategory: string | null;
  minorCategory: string | null;
  confidence: number | null;   // 0~1
  hsCode: string | null;
  appliedRules: string[];
  ruleDbVersion: string;
}

export interface CargoAttributes {
  temperature: { mode: TempMode; minC: number | null; maxC: number | null };
  hazmat: { applicable: boolean; unClass: string | null };
  fragile: { applicable: boolean; note: string | null };
  loadSpec: { packType: string; count: number; stackable: boolean };
  documents: CargoDocument[];
}

export interface CargoDocument {
  docId: string;
  type: 'INVOICE' | 'PACKING_LIST' | 'MSDS' | 'OTHER';
  fileName: string;
  uploadedAt: ISODateTime;
  downloadUrl?: string;
}

/* ── 공동화물 ── */
export interface CoCargoCandidate {
  candidateId: number;
  cargoName: string;
  shipperMasked: string;
  cbm: number;
  weightKg: number;
  temperature: TempMode;
  compatible: boolean;
  colorKey: ColorKey;
}

export interface CoCargoSimulation {
  totalCbm: number;
  capacityCbm: number;
  fillRate: number;      // 0~1
  soloFare: Won;
  sharedFare: Won;
  savedAmount: Won;
  savedRate: Rate;
  co2SavedKg: number;
}

/* ── 열차 ── */
export interface Train {
  trainId: string;
  trainNo: string;
  name: string;
  departAt: ISODateTime;
  arriveAt: ISODateTime;
  fare: Won;
  remainingTeu: number;
  totalTeu: number;
  refrigeratedAvailable: boolean;
  recommended: boolean;
}

export interface WagonCapacity {
  wagonNo: string;
  usedCbm: number;
  capacityCbm: number;
  segments: { owner: 'MINE' | 'CO_CARGO' | 'OTHER' | 'FREE'; cbm: number; colorKey?: ColorKey }[];
}

/* ── 비교 · 추천 ── */
export interface ModeQuote {
  fare: Won;
  leadTimeHours: number;
  distanceKm: number;
  co2Kg: number;
}

export interface QuoteComparison {
  rail: ModeQuote;
  road: ModeQuote;
  delta: { farePct: Rate; leadTimePct: Rate; co2Pct: Rate };
  wagonSpace: { existingPct: number; myCoCargoPct: number; freePct: number };
}

export interface Recommendation {
  score: number;   // 0~100
  verdict: 'RAIL_RECOMMENDED' | 'ROAD_RECOMMENDED' | 'NEUTRAL';
  breakdown: { label: string; score: number; weight: number }[];
  reasoning: string;
  alternative: { mode: 'RAIL' | 'ROAD'; fare: Won; leadTimeHours: number } | null;
}

/* ── 예약 · 결제 ── */
export interface FareBreakdown {
  railFare: Won;
  pickupFare: Won;
  lastMileFare: Won;
  vat: Won;
  total: Won;
}

export interface Reservation {
  reservationId: string;
  status: ShipmentStatus;
  holdExpiresAt: ISODateTime | null;
  fare: FareBreakdown;
}

export interface PickupSlot {
  slotId: string;
  window: string;   // "09:00-12:00"
  available: boolean;
  surcharge: Won;
}

export interface PaymentMethod {
  methodId: PaymentMethodId;
  label: string;
  detail: string;
  default?: boolean;
  creditRemaining?: Won;
}

export interface Payment {
  paymentId: string;
  status: PaymentStatus;
  paidAt: ISODateTime | null;
  receiptUrl: string | null;
  virtualAccount: { bank: string; accountNo: string; holder: string; expiresAt: ISODateTime } | null;
  settlementMonth?: string;   // "2026-09"
}

/* ── 운송현황 ── */
export interface ShipmentSummary {
  shipmentId: string;
  cargoName: string;
  route: string;
  date: ISODate;
  status: ShipmentStatus;
  statusLabel: string;
  badgeTone: BadgeTone;
  progressPct: number;         // 0~1
  delayMinutes?: number;
  delayReason?: string;
}

export interface TimelineNode {
  seq: number;
  code: ShipmentStatus;
  label: string;
  at: ISODateTime | null;
  state: NodeState;
}

export interface Tracking {
  current: { lat: number; lng: number; at: ISODateTime; speedKmh: number };
  origin: { lat: number; lng: number; label: string };
  dest: { lat: number; lng: number; label: string };
  polyline: string;
  etaAt: ISODateTime;
}

/* ── 알림 · 거래소 ── */
export interface Notification {
  notificationId: string;
  type: NotificationType;
  title: string;
  body: string;
  shipmentId: string | null;
  read: boolean;
  tone: BadgeTone;
  createdAt: ISODateTime;
}

export interface MarketplaceListing {
  listingId: string;
  shipperMasked: string;
  cargoName: string;
  cbm: number;
  route: string;
  date: ISODate;
  temperature: TempMode;
  remainingCbm: number;
  expectedSavedRate: Rate;
  matchScore: number;   // 0~100
}

/* ── 공통 응답 ── */
export interface ApiResponse<T> {
  data: T | null;
  error: { code: string; message: string } | null;
}

export interface Paged<T> {
  items: T[];
  nextCursor: string | null;
}
