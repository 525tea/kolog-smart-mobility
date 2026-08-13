import { api } from "./client";
import type { TrainResponse } from "../types";

export function getUpcomingTrains() {
  return api.get<TrainResponse[]>("/api/v1/trains");
}

export function getTrain(trainId: number) {
  return api.get<TrainResponse>(`/api/v1/trains/${trainId}`);
}
