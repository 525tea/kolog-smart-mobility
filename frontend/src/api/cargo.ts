import { api } from "./client";
import type { CargoAnalysisResponse, CargoDocumentExtractionResponse, CargoResponse } from "../types";

export function registerCargo(input: {
  cargoName: string;
  rawInput: string;
  originStation: string;
  destinationStation: string;
  desiredDate: string;
  serviceMode: "INDIVIDUAL" | "CO_LOAD";
  /** 화물가액(원). 선택 입력 — 입력하면 적재보험료 산정에 쓰인다. */
  declaredValueKrw?: number;
}) {
  return api.post<CargoResponse>("/api/v1/cargo-orders", input);
}

export function runAiAnalysis(cargoOrderId: number) {
  return api.post<CargoAnalysisResponse>(`/api/v1/cargo-orders/${cargoOrderId}/ai-analysis`);
}

export function correctCargo(
  cargoOrderId: number,
  patch: Partial<
    Pick<
      CargoResponse,
      "weightKg" | "volumeCbm" | "temperatureCondition" | "hazardous" | "hazardGrade" | "packagingType" | "handlingNote"
    >
  >,
) {
  return api.patch<CargoResponse>(`/api/v1/cargo-orders/${cargoOrderId}`, patch);
}

export function getCargo(cargoOrderId: number) {
  return api.get<CargoResponse>(`/api/v1/cargo-orders/${cargoOrderId}`);
}

export function getMyCargoOrders() {
  return api.get<CargoResponse[]>("/api/v1/cargo-orders/me");
}

export function previewStationMapping(location: string) {
  return api.get<{
    inputLocation: string;
    railStation: string | null;
    mapped: boolean;
    mappedStationId: string | null;
    outOfCoverage: boolean;
    userMessage: string | null;
  }>(
    `/api/v1/cargo-orders/station-mapping?location=${encodeURIComponent(location)}`,
  );
}

export function extractCargoDocument(file: File) {
  const form = new FormData();
  form.append("file", file);
  return api.postForm<CargoDocumentExtractionResponse>("/api/v1/cargo-orders/documents/extract", form);
}

export function attachCargoMsds(cargoOrderId: number, file: File) {
  const form = new FormData();
  form.append("file", file);
  return api.postForm<CargoResponse>(`/api/v1/cargo-orders/${cargoOrderId}/msds`, form);
}
