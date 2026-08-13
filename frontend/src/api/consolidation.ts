import { api } from "./client";
import type { ConsolidationCandidateResponse, ConsolidationDetailResponse, FailurePreference } from "../types";

export function getCandidates(cargoOrderId: number) {
  return api.get<ConsolidationCandidateResponse[]>(`/api/v1/cargo-orders/${cargoOrderId}/consolidation-candidates`);
}

export function joinConsolidation(
  consolidatedCargoId: number,
  cargoOrderId: number,
  failurePreference: FailurePreference,
  options?: { firstMile?: boolean; lastMile?: boolean },
) {
  return api.post<ConsolidationDetailResponse>(`/api/v1/consolidated-cargos/${consolidatedCargoId}/participations`, {
    cargoOrderId,
    failurePreference,
    firstMile: options?.firstMile ?? false,
    lastMile: options?.lastMile ?? false,
  });
}

export function getConsolidationDetail(consolidatedCargoId: number) {
  return api.get<ConsolidationDetailResponse>(`/api/v1/consolidated-cargos/${consolidatedCargoId}`);
}

export function browseConsolidations(originStation?: string, destinationStation?: string) {
  const params = new URLSearchParams();
  if (originStation) params.set("originStation", originStation);
  if (destinationStation) params.set("destinationStation", destinationStation);
  const query = params.toString();
  return api.get<ConsolidationDetailResponse[]>(`/api/v1/consolidated-cargos${query ? `?${query}` : ""}`);
}

export function getMyConsolidations() {
  return api.get<ConsolidationDetailResponse[]>("/api/v1/consolidated-cargos/me");
}
