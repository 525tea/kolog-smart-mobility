import { api } from "./client";
import type { NotificationResponse } from "../types";

export function getMyNotifications() {
  return api.get<NotificationResponse[]>("/api/v1/members/me/notifications");
}

export function getUnreadNotificationCount() {
  return api.get<{ unreadCount: number }>("/api/v1/members/me/notifications/unread-count");
}

export function markAllNotificationsRead() {
  return api.post<void>("/api/v1/members/me/notifications/read-all");
}
