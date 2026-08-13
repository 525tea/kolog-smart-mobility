import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getAppBootstrap, saveRuntimeConfig } from "../api/app";
import { refreshAccessToken } from "../api/auth";
import { getRefreshToken, setRefreshToken } from "../api/client";
import SplashScreen from "../screens/SplashScreen";

export function SplashPage() {
  const navigate = useNavigate();
  const { isAuthenticated, login: authenticate } = useAuth();
  const initiallyAuthenticated = useRef(isAuthenticated);
  const [blockingMessage, setBlockingMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const startedAt = Date.now();
    async function bootstrap() {
      try {
        const config = await getAppBootstrap();
        if (cancelled) return;
        saveRuntimeConfig(config.config);
        if (config.maintenance.active) {
          setBlockingMessage(config.maintenance.message ?? "서비스 점검 중입니다. 잠시 후 다시 시도해주세요.");
          return;
        }
        if (config.forceUpdate) {
          setBlockingMessage("새 버전으로 업데이트한 뒤 다시 이용해주세요.");
          return;
        }
        const refreshToken = getRefreshToken();
        if (refreshToken) {
          try {
            const renewed = await refreshAccessToken(refreshToken);
            setRefreshToken(renewed.refreshToken);
            await authenticate(renewed.accessToken);
          } catch {
            setRefreshToken(null);
          }
        }
      } catch {
        // 부트스트랩 장애가 로그인 화면 진입까지 막지는 않도록 한다.
      }
      const delay = Math.max(0, 1900 - (Date.now() - startedAt));
      window.setTimeout(() => {
        if (!cancelled) navigate(getRefreshToken() || initiallyAuthenticated.current ? "/home" : "/login", { replace: true });
      }, delay);
    }
    void bootstrap();
    return () => { cancelled = true; };
  }, [authenticate, navigate]);

  function handleStart() {
    navigate(isAuthenticated || Boolean(getRefreshToken()) ? "/home" : "/login");
  }

  return <SplashScreen onReady={handleStart} message={blockingMessage ?? undefined} blocked={Boolean(blockingMessage)} />;
}
