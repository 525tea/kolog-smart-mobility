import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { getToken, setRefreshToken, setToken as persistToken } from "../api/client";
import { getMyShipperProfile } from "../api/shipper";
import type { ShipperResponse } from "../types";
import type { MemberResponse } from "../types";
import { getCurrentMember } from "../api/auth";

interface AuthContextValue {
  isAuthenticated: boolean;
  member: MemberResponse | null;
  shipper: ShipperResponse | null;
  needsShipperRegistration: boolean;
  loading: boolean;
  login: (token: string) => Promise<MemberResponse>;
  logout: () => void;
  refreshShipper: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getToken()));
  const [shipper, setShipper] = useState<ShipperResponse | null>(null);
  const [member, setMember] = useState<MemberResponse | null>(null);
  const [needsShipperRegistration, setNeedsShipperRegistration] = useState(false);
  const [loading, setLoading] = useState(true);

  const loadIdentity = useCallback(async () => {
    const currentMember = await getCurrentMember();
    setMember(currentMember);
    if (currentMember.role === "OPERATOR") {
      setShipper(null);
      setNeedsShipperRegistration(false);
      return currentMember;
    }
    try {
      const profile = await getMyShipperProfile();
      setShipper(profile);
      setNeedsShipperRegistration(false);
    } catch {
      // 사업자 등록을 아직 안 한 화주 계정일 수 있음 — 에러가 아니라 온보딩 필요 상태로 취급한다
      setShipper(null);
      setNeedsShipperRegistration(true);
    }
    return currentMember;
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }
    loadIdentity().catch(() => {
      persistToken(null);
      setIsAuthenticated(false);
      setMember(null);
    }).finally(() => setLoading(false));
  }, [isAuthenticated, loadIdentity]);

  const login = useCallback(
    async (token: string) => {
      persistToken(token);
      setIsAuthenticated(true);
      setLoading(true);
      try {
        return await loadIdentity();
      } catch (error) {
        persistToken(null);
        setIsAuthenticated(false);
        setMember(null);
        throw error;
      } finally {
        setLoading(false);
      }
    },
    [loadIdentity],
  );

  const logout = useCallback(() => {
    persistToken(null);
    setRefreshToken(null);
    setIsAuthenticated(false);
    setShipper(null);
    setMember(null);
  }, []);

  const value = useMemo(
    () => ({ isAuthenticated, member, shipper, needsShipperRegistration, loading, login, logout, refreshShipper: async () => { await loadIdentity(); } }),
    [isAuthenticated, member, shipper, needsShipperRegistration, loading, login, logout, loadIdentity],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.");
  return ctx;
}
