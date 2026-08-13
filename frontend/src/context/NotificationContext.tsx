import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { getToken } from "../api/client";
import { getMyNotifications, getUnreadNotificationCount, markAllNotificationsRead } from "../api/notification";
import type { NotificationResponse } from "../types";

/**
 * 서버가 실제로 발생한 상태 변화(매칭 대기 전환, 코레일 승인/반려, 공동화 실패 처리 등)를
 * 기록한 알림을 보여준다. 로그인 중일 때만 주기적으로 서버에서 새 알림을 가져온다.
 */
const POLL_INTERVAL_MS = 8000;

interface NotificationContextValue {
  notifications: NotificationResponse[];
  unreadCount: number;
  refresh: () => Promise<void>;
  markAllRead: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextValue | undefined>(undefined);

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const refresh = useCallback(async () => {
    if (!getToken()) {
      setNotifications([]);
      setUnreadCount(0);
      return;
    }
    try {
      const [list, unread] = await Promise.all([getMyNotifications(), getUnreadNotificationCount()]);
      setNotifications(list);
      setUnreadCount(unread.unreadCount);
    } catch {
      // 토큰 만료 등 일시적 오류 - 다음 폴링에서 다시 시도한다
    }
  }, []);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [refresh]);

  const markAllRead = useCallback(async () => {
    if (!getToken()) return;
    try {
      await markAllNotificationsRead();
    } finally {
      await refresh();
    }
  }, [refresh]);

  const value = useMemo(
    () => ({ notifications, unreadCount, refresh, markAllRead }),
    [notifications, unreadCount, refresh, markAllRead],
  );

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications() {
  const ctx = useContext(NotificationContext);
  if (!ctx) throw new Error("useNotifications는 NotificationProvider 내부에서만 사용할 수 있습니다.");
  return ctx;
}
