import { NavLink } from "react-router-dom";

const ITEMS = [
  { to: "/home", label: "홈", icon: HomeIcon },
  { to: "/exchange", label: "거래소", icon: SearchIcon },
  { to: "/cargo/new/form?mode=CO_LOAD", label: "화물등록", icon: CargoIcon },
  { to: "/shipments", label: "운송현황", icon: TrainIcon },
  { to: "/me", label: "마이", icon: UserIcon },
];

export function BottomNav() {
  return (
    <nav className="sticky bottom-0 z-[1100] flex h-[62px] shrink-0 border-t border-[#e5eaf2] bg-white">
      {ITEMS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          className={({ isActive }) =>
            `relative flex flex-1 flex-col items-center justify-center gap-1 text-[10px] font-extrabold ${
              isActive ? "text-brand" : "text-ink-faint"
            }`
          }
        >
          {({ isActive }) => (
            <>
              {isActive && <span className="absolute top-0 h-[3px] w-7 rounded-b-full bg-brand" />}
              <item.icon active={isActive} />
              {item.label}
            </>
          )}
        </NavLink>
      ))}
    </nav>
  );
}

function HomeIcon({ active }: { active: boolean }) {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill={active ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2">
      <path d="M3 11.5 12 4l9 7.5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M5 10v9a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1v-9" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
function SearchIcon({ active }: { active: boolean }) {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={active ? 2.5 : 2}>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" strokeLinecap="round" />
    </svg>
  );
}
function CargoIcon({ active }: { active: boolean }) {
  return (
    <svg width="21" height="21" viewBox="0 0 24 24" fill={active ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2">
      <path d="M4 7.5 12 3l8 4.5v9L12 21l-8-4.5z" strokeLinejoin="round" />
      <path d="m4 7.5 8 4.5 8-4.5M12 12v9" />
    </svg>
  );
}
function TrainIcon({ active }: { active: boolean }) {
  return <svg width="21" height="21" viewBox="0 0 24 24" fill={active ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2"><rect x="5" y="3" width="14" height="15" rx="3"/><path d="M8 7h8M8 11h8M8 21l2-3m6 0 2 3M8 15h.01M16 15h.01" strokeLinecap="round"/></svg>;
}
function UserIcon({ active }: { active: boolean }) {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill={active ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 20c1.5-4 5-6 8-6s6.5 2 8 6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
