// 백엔드 DTO(backend/src/main/java/.../dto)와 1:1로 맞춘 타입들.
// 백엔드에 없는 필드는 만들어내지 않고, 화면에서 필요하면 별도로 표시한다.

export type MemberRole = "SHIPPER" | "OPERATOR";

export interface MemberResponse {
  id: number;
  email: string;
  role: MemberRole;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface AppBootstrapResponse {
  minSupportedVersion: string;
  latestVersion: string;
  forceUpdate: boolean;
  storeUrl: string | null;
  maintenance: { active: boolean; message: string | null; until: string | null };
  config: {
    containerCapacityCbm: number;
    holdMinutes: number;
    classifyConfidenceThreshold: number;
    ruleDbVersion: string;
    platform: string;
    requestedAppVersion: string;
  };
}

export interface ShipperResponse {
  id: number;
  memberId: number;
  businessNumber: string;
  companyName: string;
  managerName: string;
  phone: string;
}

export type TemperatureCondition = "ROOM" | "CONSTANT" | "REFRIGERATED" | "FROZEN";
export type ServiceMode = "INDIVIDUAL" | "CO_LOAD";
export type CargoOrderStatus =
  | "REGISTERED"
  | "ANALYZED"
  | "PARTICIPATING"
  | "RESERVED"
  | "CANCELLED";

export type HazardGrade = "A" | "B" | "C" | "D";

export interface CargoDocumentExtractionResponse {
  fileName: string;
  mimeType: string;
  provider: "GOOGLE_DOCUMENT_AI_FORM_PARSER" | "APACHE_POI" | "DIRECT_TEXT" | "DEMO_FIXTURE";
  extractedText: string;
  pageOrSheetCount: number;
  formFieldCount: number;
  tableCount: number;
  warnings: string[];
}

export interface CargoResponse {
  id: number;
  shipperId: number;
  cargoName: string;
  originStation: string;
  destinationStation: string;
  desiredDate: string;
  serviceMode: ServiceMode;
  weightKg: number | null;
  volumeCbm: number | null;
  temperatureCondition: TemperatureCondition | null;
  hazardous: boolean;
  hazardGrade: HazardGrade | null;
  hazardClassCode: string | null;
  hazardClassName: string | null;
  transportRejected: boolean;
  requiresMsds: boolean;
  msdsAttached: boolean;
  msdsFileName: string | null;
  hazardReason: string | null;
  surchargeRate: number;
  fixedPowerFeeKrw: number;
  detectedTemperatureC: number | null;
  assignedContainer: string | null;
  packagingType: string | null;
  handlingNote: string | null;
  /** 화주가 신고한 화물가액(원). 신고하지 않았으면 null → 적재보험료 0원. */
  declaredValueKrw: number | null;
  status: CargoOrderStatus;
}

export type ConsolidationStatus =
  | "RECRUITING"
  | "READY_FOR_MATCHING"
  | "MATCHED"
  | "PENDING_APPROVAL"
  | "APPROVED"
  | "REJECTED"
  | "CONFIRMED"
  | "CANCELLED";

export type FailurePreference = "NEXT_TRAIN" | "AUTO_REFUND";

export interface ConsolidationCandidateResponse {
  consolidatedCargoId: number;
  originStation: string;
  destinationStation: string;
  targetWeightKg: number;
  recruitedWeightKg: number;
  recruitmentRatePercent: number;
  recruitmentDeadline: string;
  trainId: number;
  trainNumber: string;
  departureAt: string;
  arrivalAt: string;
  availableWeightKg: number;
  suitabilityScore: number;
  estimatedSuccessProbability: number;
  estimatedFreightForOrder: number;
  estimatedSavingsForOrder: number;
  appliedRatePerKg: number;
  appliedDiscountRate: number;
  pricingReason: string;
  /** 위험물·콜드체인·특수화물 중 최고 할증률(Max Rule). */
  hazardSurchargeRate: number;
  fixedPowerFeeKrw: number;
  /** 적재보험료(원). 화물가액을 신고하지 않았으면 0. */
  insuranceFeeKrw: number;
  /** 플랫폼 이용 수수료(원). */
  platformFeeKrw: number;
  /** 화주가 실제로 결제하는 최종 금액(원) = estimatedFreightForOrder + insuranceFeeKrw + platformFeeKrw. */
  totalPayableKrw: number;
}

export interface ConsolidationParticipant {
  companyName: string;
  cargoName: string;
  weightKg: number;
  volumeCbm: number | null;
  temperatureCondition: TemperatureCondition;
  hazardous: boolean;
}

export interface ConsolidationDetailResponse {
  id: number;
  originStation: string;
  destinationStation: string;
  targetWeightKg: number;
  recruitedWeightKg: number;
  recruitmentRatePercent: number;
  desiredDate: string;
  recruitmentDeadline: string;
  status: ConsolidationStatus;
  participantCount: number;
  matchedWagonId: number | null;
  participants: ConsolidationParticipant[];
}

export type WagonType = "CONTAINER" | "BULK" | "REFRIGERATED";
export type TrainStatus = "SCHEDULED" | "DEPARTED" | "ARRIVED" | "CANCELLED";

export interface WagonResponse {
  id: number;
  trainId: number;
  wagonNumber: string;
  wagonType: WagonType;
  maxWeightKg: number;
  remainingWeightKg: number;
  loadFactorPercent: number;
  hazardousAllowed: boolean;
}

export interface TrainResponse {
  id: number;
  trainNumber: string;
  originStation: string;
  destinationStation: string;
  departureAt: string;
  arrivalAt: string;
  reservationDeadline: string;
  status: TrainStatus;
  wagons: WagonResponse[];
}

export interface OperatorDashboardResponse {
  upcomingTrains: TrainResponse[];
  reviewQueue: ConsolidationDetailResponse[];
}

export type PaymentStatus = "PENDING" | "VIRTUAL_PAID";

export interface ReservationResponse {
  id: number;
  consolidatedCargoId: number;
  wagonId: number;
  totalCost: number;
  paymentStatus: PaymentStatus;
}

export interface ParticipationPaymentResponse {
  id: number;
  cargoOrderId: number;
  consolidatedCargoId: number;
  totalCost: number;
  paymentStatus: PaymentStatus;
}

export type TransportPhase = "BEFORE_DEPARTURE" | "IN_TRANSIT" | "ARRIVED";

/**
 * 실시간(시뮬레이션) 위치 정보. 실제 GPS 단말 연동이 없어, 열차 시간표(출발/도착 시각) 기준으로
 * 경과 비율만큼 출발역-도착역 사이를 선형보간한 좌표를 보여준다 (isSimulated=true).
 */
export interface TrackingResponse {
  reservationId: number;
  phase: TransportPhase;
  progressPercent: number;
  currentLatitude: number;
  currentLongitude: number;
  originLatitude: number;
  originLongitude: number;
  destinationLatitude: number;
  destinationLongitude: number;
  originStation: string;
  destinationStation: string;
  departureAt: string;
  arrivalAt: string;
  currentSegment: string;
  lastUpdatedAt: string;
  refreshAfterSeconds: number;
  route: Array<{ name: string; latitude: number; longitude: number }>;
  isSimulated: boolean;
}

export type NotificationType = "ANALYSIS" | "MATCH" | "PAYMENT" | "APPROVAL" | "REJECT" | "INFO";

export interface NotificationResponse {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface CargoAnalysisResponse {
  cargo: CargoResponse;
  lowConfidenceFields: string[];
  detectedItems: string[];
  analysisWarnings: string[];
}

export interface ApiErrorBody {
  code: string;
  message: string;
  fieldErrors?: { field: string; reason: string }[];
  timestamp?: string;
}
