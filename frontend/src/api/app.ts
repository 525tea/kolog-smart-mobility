import { api } from "./client";
import type { AppBootstrapResponse } from "../types";

export function getAppBootstrap() {
  return api.get<AppBootstrapResponse>("/app/bootstrap?platform=web&appVersion=1.4.0", false);
}

export function saveRuntimeConfig(config: AppBootstrapResponse["config"]) {
  sessionStorage.setItem("app/bootstrap.config", JSON.stringify(config));
}

export function getRuntimeConfig(): AppBootstrapResponse["config"] | null {
  const raw = sessionStorage.getItem("app/bootstrap.config");
  if (!raw) return null;
  try { return JSON.parse(raw) as AppBootstrapResponse["config"]; }
  catch { return null; }
}
