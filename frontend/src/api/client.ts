import type { ApiErrorBody } from "../types";

// 로컬 개발 기본값은 docker-compose로 띄운 백엔드(8080). 배포 시 .env에서 덮어쓴다.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

const TOKEN_STORAGE_KEY = "kolog.accessToken";
const REFRESH_TOKEN_STORAGE_KEY = "kolog.refreshToken";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  }
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
}

export function setRefreshToken(token: string | null) {
  if (token) localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, token);
  else localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
}

export class ApiError extends Error {
  status: number;
  body: ApiErrorBody | null;

  constructor(status: number, body: ApiErrorBody | null) {
    super(body?.message ?? `요청이 실패했습니다 (HTTP ${status})`);
    this.status = status;
    this.body = body;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PATCH" | "PUT" | "DELETE";
  body?: unknown | FormData;
  auth?: boolean; // 기본 true — 대부분의 API가 로그인 필요
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, auth = true } = options;

  const headers: Record<string, string> = {};
  const isFormData = body instanceof FormData;
  if (body !== undefined && !isFormData) {
    headers["Content-Type"] = "application/json";
  }
  if (auth) {
    const token = getToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    if (response.status === 401 && auth) {
      // 토큰 만료/무효 - 로그인 화면으로 돌려보낸다
      setToken(null);
    }
    throw new ApiError(response.status, data as ApiErrorBody | null);
  }

  return data as T;
}

async function requestBlob(path: string): Promise<Blob> {
  const token = getToken();
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) {
    const text = await response.text();
    let body: ApiErrorBody | null = null;
    try { body = text ? JSON.parse(text) as ApiErrorBody : null; } catch { body = null; }
    throw new ApiError(response.status, body);
  }
  return response.blob();
}

export const api = {
  get: <T>(path: string, auth = true) => request<T>(path, { method: "GET", auth }),
  getBlob: (path: string) => requestBlob(path),
  post: <T>(path: string, body?: unknown, auth = true) => request<T>(path, { method: "POST", body, auth }),
  postForm: <T>(path: string, body: FormData, auth = true) => request<T>(path, { method: "POST", body, auth }),
  patch: <T>(path: string, body?: unknown, auth = true) => request<T>(path, { method: "PATCH", body, auth }),
};
