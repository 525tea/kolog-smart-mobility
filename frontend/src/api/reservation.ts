import { api } from "./client";
import type { ParticipationPaymentResponse, ReservationResponse, TrackingResponse } from "../types";

export function getMyReservations() {
  return api.get<ReservationResponse[]>("/api/v1/shippers/me/reservations");
}

export function getMyPayments() {
  return api.get<ParticipationPaymentResponse[]>("/api/v1/shippers/me/payments");
}

export function getReservation(reservationId: number) {
  return api.get<ReservationResponse>(`/api/v1/reservations/${reservationId}`);
}

/** 실시간(시뮬레이션) 위치 조회. 실제 GPS 단말 연동이 없어 열차 시간표 기준으로 시뮬레이션한 값이다. */
export function getTracking(reservationId: number) {
  return api.get<TrackingResponse>(`/api/v1/reservations/${reservationId}/tracking`);
}
