import { useEffect } from "react";
import { useLocation } from "react-router-dom";

export function ScrollToTop() {
  const { pathname, search } = useLocation();
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "instant" });
    document.querySelectorAll<HTMLElement>(".app-shell").forEach((element) => element.scrollTo({ top: 0, left: 0 }));
  }, [pathname, search]);
  return null;
}
