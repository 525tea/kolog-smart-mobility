import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { BottomNav } from "./BottomNav";

/** 로그인 필요 + 하단 탭바가 있는 화면들(홈/거래소/알림/마이)을 감싼다. */
export function ProtectedLayout() {
  const { isAuthenticated, member, loading } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (loading) {
    return <div className="app-shell items-center justify-center text-gray-400">불러오는 중…</div>;
  }
  if (member?.role === "OPERATOR") {
    return <Navigate to="/operator" replace />;
  }

  return (
    <div className="app-shell">
      <div className="flex-1 overflow-y-auto pb-2">
        <Outlet />
      </div>
      <BottomNav />
    </div>
  );
}

/** 로그인은 필요하지만 하단 탭바 없이 전체 화면을 쓰는 흐름(화물등록 위저드, 상세 화면 등)용. */
export function ProtectedFlowLayout() {
  const { isAuthenticated, member, loading } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  if (loading) {
    return <div className="app-shell items-center justify-center text-gray-400">불러오는 중…</div>;
  }
  if (member?.role === "OPERATOR") {
    return <Navigate to="/operator" replace />;
  }

  return (
    <div className="app-shell">
      <Outlet />
    </div>
  );
}

export function OperatorProtectedLayout() {
  const { isAuthenticated, member, loading } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (loading) return <div className="app-shell items-center justify-center text-gray-400">불러오는 중…</div>;
  if (member?.role !== "OPERATOR") return <Navigate to="/home" replace />;
  return <Outlet />;
}
