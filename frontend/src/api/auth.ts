import { api } from "./client";
import type { MemberResponse, MemberRole, TokenResponse } from "../types";

export function login(email: string, password: string) {
  return api.post<TokenResponse>("/api/v1/auth/login", { email, password }, false);
}

export function refreshAccessToken(refreshToken: string) {
  return api.post<TokenResponse>("/auth/refresh", { refreshToken }, false);
}

export function signup(email: string, password: string, role: MemberRole) {
  return api.post<MemberResponse>("/api/v1/auth/signup", { email, password, role }, false);
}

export function getCurrentMember() {
  return api.get<MemberResponse>("/api/v1/auth/me");
}
