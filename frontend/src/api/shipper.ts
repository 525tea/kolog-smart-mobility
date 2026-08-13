import { api } from "./client";
import type { ShipperResponse } from "../types";

export function registerShipper(input: {
  businessNumber: string;
  companyName: string;
  managerName: string;
  phone: string;
}) {
  return api.post<ShipperResponse>("/api/v1/shippers", input);
}

export function getMyShipperProfile() {
  return api.get<ShipperResponse>("/api/v1/shippers/me");
}
