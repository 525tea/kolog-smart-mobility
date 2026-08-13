import { useEffect, useRef, useState } from 'react';
import { login as loginRequest } from './api/auth';
import { getCandidates, getMyConsolidations, joinConsolidation } from './api/consolidation';
import { getMyPayments, getMyReservations, getTracking } from './api/reservation';
import { getUpcomingTrains } from './api/train';
import type { FailurePreference, TrackingResponse } from './types';

/** 최신 화면에서 사용할 API 진입점. 현재 Spring 백엔드에 실제로 존재하는 계약만 노출한다. */
export const KologApi = {
  login: (email: string, password: string) => loginRequest(email, password),
  getUpcomingTrains,
  getCandidates,
  getMyConsolidations,
  getMyPayments,
  getMyReservations,
  getTracking,
  joinConsolidation: (
    consolidatedCargoId: number,
    cargoOrderId: number,
    failurePreference: FailurePreference,
    firstMile = false,
    lastMile = false,
  ) => joinConsolidation(consolidatedCargoId, cargoOrderId, failurePreference, { firstMile, lastMile }),
};

/** 모집 마감시각까지 남은 시간을 mm:ss 또는 hh:mm:ss로 표시한다. */
export function useHoldTimer(expiresAt: string | null, onExpire: () => void) {
  const [label, setLabel] = useState('--:--');
  const fired = useRef(false);
  const onExpireRef = useRef(onExpire);

  useEffect(() => {
    onExpireRef.current = onExpire;
  }, [onExpire]);

  useEffect(() => {
    fired.current = false;
    if (!expiresAt) {
      setLabel('--:--');
      return;
    }
    const tick = () => {
      const remainingMs = new Date(expiresAt).getTime() - Date.now();
      if (remainingMs <= 0) {
        setLabel('00:00');
        if (!fired.current) {
          fired.current = true;
          onExpireRef.current();
        }
        return;
      }
      const totalSeconds = Math.floor(remainingMs / 1000);
      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;
      setLabel(hours > 0
        ? `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
        : `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`);
    };
    tick();
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [expiresAt]);

  return label;
}

/** 백엔드의 시간표 기반 위치 시뮬레이션을 권장 주기마다 갱신한다. */
export function useTracking(reservationId: number | null) {
  const [tracking, setTracking] = useState<TrackingResponse | null>(null);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (reservationId == null) {
      setTracking(null);
      return;
    }
    let cancelled = false;
    let timer: number | undefined;
    const refresh = async () => {
      try {
        const next = await getTracking(reservationId);
        if (cancelled) return;
        setTracking(next);
        setError(null);
        timer = window.setTimeout(refresh, Math.max(1, next.refreshAfterSeconds) * 1000);
      } catch (cause) {
        if (!cancelled) setError(cause instanceof Error ? cause : new Error('위치 정보를 불러오지 못했습니다.'));
      }
    };
    void refresh();
    return () => {
      cancelled = true;
      if (timer !== undefined) window.clearTimeout(timer);
    };
  }, [reservationId]);

  return { tracking, error };
}
