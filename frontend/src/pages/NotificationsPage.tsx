import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useNotifications } from "../context/NotificationContext";

export function NotificationsPage() {
  const navigate = useNavigate();
  const { notifications, markAllRead } = useNotifications();

  useEffect(() => {
    markAllRead();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="flex min-h-full flex-col bg-[#f4f7fb]">
      <header className="flex h-[72px] items-center gap-3 bg-white px-5">
        <button onClick={() => navigate(-1)} aria-label="뒤로 가기" className="text-[28px] font-light text-[#1c2940]">‹</button>
        <h1 className="text-[20px] font-black text-[#111c2e]">알림</h1>
        {notifications.length > 0 && <button onClick={markAllRead} className="ml-auto text-xs font-black text-brand-700">모두 읽음</button>}
      </header>

      {notifications.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-2 px-8 text-[#9aa5b7]">
          <BellIcon />
          <p className="mt-2 text-sm font-black text-[#4d5a70]">아직 알림이 없어요</p>
          <p className="text-center text-xs">화물 등록부터 결제·운송까지<br />새 소식이 생기면 알려드릴게요.</p>
        </div>
      ) : (
        <main className="flex flex-col gap-5 px-5 py-5">
          {groupByDay(notifications).slice(0, 2).map(([day, rows]) => (
            <section key={day}>
              <p className="mb-2 text-[11px] font-black text-[#8b96a9]">{day}</p>
              <div className="overflow-hidden rounded-[20px] border border-[#dfe5f0] bg-white">
                {rows.slice(0, day === "오늘" ? 3 : 2).map((n) => (
                  <div key={n.id} className={`relative flex gap-3 border-b border-[#e8edf5] px-4 py-4 last:border-0 ${!n.read ? "bg-[#f7f9ff]" : ""}`}>
                    {!n.read && <span className="absolute inset-y-3 left-0 w-[3px] rounded-r-full bg-brand-600" />}
                    <div className="grid size-9 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700"><NotificationIcon type={n.type} /></div>
                    <div className="min-w-0 flex-1">
                      <div className="flex items-start justify-between gap-2"><p className="text-[13px] font-black text-[#182237]">{n.title}</p><span className="shrink-0 text-[10px] font-semibold text-[#a0aabd]">{formatTime(n.createdAt)}</span></div>
                      <p className="mt-1 text-[12px] leading-[1.55] text-[#66738a]">{n.message}</p>
                    </div>
                  </div>
                ))}
              </div>
            </section>
          ))}
        </main>
      )}
    </div>
  );
}

function groupByDay<T extends { createdAt: string }>(rows: T[]): [string, T[]][] {
  const today = new Date().toDateString();
  const groups = new Map<string, T[]>();
  rows.forEach((row) => {
    const date = new Date(row.createdAt);
    const key = date.toDateString() === today ? "오늘" : date.toLocaleDateString("ko-KR", { month: "long", day: "numeric" });
    groups.set(key, [...(groups.get(key) ?? []), row]);
  });
  return [...groups.entries()];
}

function formatTime(value: string) {
  return new Date(value).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}

function NotificationIcon({ type }: { type: string }) {
  if (type === "PAYMENT") return <span className="text-sm font-black">₩</span>;
  if (type === "APPROVAL" || type === "MATCH") return <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.3"><path d="m5 12 4 4L19 6" /></svg>;
  return <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 7.5 12 3l8 4.5v9L12 21l-8-4.5zM4 7.5l8 4.5 8-4.5M12 12v9" /></svg>;
}

function BellIcon() {
  return <div className="grid size-16 place-items-center rounded-full bg-white text-brand-700 shadow-sm"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></svg></div>;
}
