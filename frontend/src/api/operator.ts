import { api } from "./client";
import type { OperatorDashboardResponse, ReservationResponse } from "../types";

export function getOperatorDashboard() {
  return api.get<OperatorDashboardResponse>("/api/v1/operator/dashboard");
}

export function runLoadOptimization() {
  return api.post<unknown[]>("/api/v1/operator/load-optimization/run");
}

export function approveConsolidation(groupId: number, memo?: string) {
  return api.post<ReservationResponse>(`/api/v1/operator/consolidated-cargos/${groupId}/approve`, { memo });
}

export function rejectConsolidation(groupId: number, memo?: string) {
  return api.post<void>(`/api/v1/operator/consolidated-cargos/${groupId}/reject`, { memo });
}
