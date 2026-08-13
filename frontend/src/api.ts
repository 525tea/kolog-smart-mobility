import { useEffect, useRef, useState } from 'react';
import type {
  ApiResponse, CoCargoSimulation, Reservation, ShipmentSummary,
  TimelineNode, Tracking, PaymentMethodId,
} from './types';

const BASE = import.meta.env.VITE_API_BASE ?? 'https://api.kolog.kr/v1';

/* ─────────── fetch 래퍼 ─────────── */
async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('kolog.accessToken') ?? ''}`,
      ...init?.headers,
    },
  });
  const body: ApiResponse<T> = await res.json();
  if (body.error) throw new ApiError(body.error.code, body.error.message);
  return body.data as T;
}

export class ApiError extends Error {
  constructor(public code: string, message: string) { super(message); }
}

export const KolgApi = {
  login: (loginId: string, password: string) =>
    api<{ accessToken: string; refreshToken: string }>('/auth/login', {
      method: 'POST', body: JSON.stringify({ loginId, password }),
    }),

  simulateCoCargo: (groupId: string, cargoId: string, selectedCandidateIds: number[]) =>
    api<CoCargoSimulation>(`/co-cargo-groups/${groupId}/simulate`, {
      method: 'POST', body: JSON.stringify({ cargoId, selectedCandidateIds }),
    }),

  createReservation: (payload: { cargoId: string; trainId: string; coCargoGroupId: string; selectedCandidateIds: number[] }) =>
    api<Reservation>('/reservations', {
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: JSON.stringify(payload),
    }),

  pay: (reservationId: string, methodId: PaymentMethodId, amount: number) =>
    api<{ paymentId: string; status: string; receiptUrl: string | null }>('/payments', {
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: JSON.stringify({ reservationId, methodId, amount }),
    }),

  shipments: (status = 'active') => api<{ shipments: ShipmentSummary[] }>(`/shipments?status=${status}`),
  timeline: (id: string) => api<{ nodes: TimelineNode[] }>(`/shipments/${id}/timeline`),
  tracking: (id: string) => api<Tracking>(`/shipments/${id}/tracking`),
};

/* ─────────── 공동화물 실시간 시뮬레이션 (A-08) ───────────
   체크박스 토글마다 300ms 디바운스 후 서버 재계산. */
export function useCoCargoSimulation(groupId: string, cargoId: string, selected: number[]) {
  const [sim, setSim] = useState<CoCargoSimulation | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    const t = setTimeout(() => {
      KolgApi.simulateCoCargo(groupId, cargoId, selected)
        .then(setSim)
        .finally(() => setLoading(false));
    }, 300);
    return () => clearTimeout(t);
  }, [groupId, cargoId, selected.join(',')]);

  return { sim, loading };
}

/* ─────────── 예약 홀드 타이머 (A-16) ───────────
   서버가 내려준 holdExpiresAt 까지 남은 시간을 mm:ss 로. 0 이 되면 onExpire. */
export function useHoldTimer(expiresAt: string | null, onExpire: () => void) {
  const [label, setLabel] = useState('--:--');
  const fired = useRef(false);

  useEffect(() => {
    if (!expiresAt) return;
    const tick = () => {
      const ms = new Date(expiresAt).getTime() - Date.now();
      if (ms <= 0) {
        setLabel('00:00');
        if (!fired.current) { fired.current = true; onExpire(); }
        return;
      }
      const s = Math.floor(ms / 1000);
      setLabel(`${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`);
    };
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, [expiresAt, onExpire]);

  return label;
}

/* ─────────── 실시간 트래킹 WebSocket (A-17) ─────────── */
export function useTracking(shipmentId: string | null) {
  const [pos, setPos] = useState<Tracking | null>(null);

  useEffect(() => {
    if (!shipmentId) return;
    KolgApi.tracking(shipmentId).then(setPos);

    const ws = new WebSocket(`${BASE.replace(/^http/, 'ws')}/ws/shipments/${shipmentId}`);
    ws.onmessage = (e) => {
      const msg = JSON.parse(e.data);
      if (msg.type === 'tracking.update') setPos((p) => (p ? { ...p, current: msg.current, etaAt: msg.etaAt } : p));
    };
    return () => ws.close();
  }, [shipmentId]);

  return pos;
}
