import { useEffect, useState } from 'react';

/* A-00 스플래시 — 로고 플레이 (컨테이너 3장 슬라이드인 → 레일 스윕 → 워드마크 → 슬로건) */

export default function SplashScreen({ onReady }: { onReady: () => void }) {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const t = setTimeout(() => setProgress(1), 1900);
    const done = setTimeout(onReady, 3600);
    return () => { clearTimeout(t); clearTimeout(done); };
  }, [onReady]);

  return (
    <div className="relative w-full h-full bg-base overflow-hidden flex flex-col">
      <div className="absolute -top-[90px] -right-[70px] w-[280px] h-[280px] rounded-full
                      bg-[radial-gradient(circle,rgba(245,176,65,.16),transparent_68%)]" />
      <div className="absolute -bottom-[60px] -left-20 w-[260px] h-[260px] rounded-full
                      bg-[radial-gradient(circle,rgba(27,92,240,.09),transparent_70%)]" />

      <div className="flex-1 flex flex-col items-center justify-center gap-[34px] relative">
        <div className="flex flex-col items-center gap-[5px]">
          <div className="w-[132px] h-[26px] rounded-md bg-brand-deep animate-slabIn [animation-delay:.5s]" />
          <div className="w-[154px] h-[26px] rounded-md bg-brand animate-slabIn [animation-delay:.3s]" />
          <div className="w-[176px] h-[26px] rounded-md bg-gold animate-slabIn [animation-delay:.1s]" />
          <div className="w-[200px] h-1 rounded-[3px] bg-hairline mt-[9px] origin-left
                          animate-railSweep [animation-delay:.9s]" />
          <div className="flex gap-[26px] mt-[2px]">
            {[0, 1, 2].map((i) => <div key={i} className="w-[11px] h-[11px] rounded-full bg-[#C6CBD3]" />)}
          </div>
        </div>

        <div className="flex flex-col items-center gap-[14px]">
          <div className="text-[44px] font-extrabold tracking-[-2px] text-brand animate-riseIn [animation-delay:1.15s]">
            KO<span className="text-gold">-</span>LOG
          </div>
          <div className="text-[13.5px] font-bold text-ink-muted tracking-[-.2px] animate-fadeUp [animation-delay:1.6s]">
            각자의 화물을 하나의 운송으로
          </div>
        </div>
      </div>

      <div className="pb-[54px] flex flex-col items-center gap-4 relative">
        <div className="w-[132px] h-[3px] rounded-[3px] bg-hairline overflow-hidden">
          <div
            className="h-full rounded-[3px] bg-gold transition-[width] duration-[1600ms] ease-out"
            style={{ width: progress ? '100%' : '8%' }}
          />
        </div>
        <span className="text-[10.5px] font-bold tracking-[1.6px] text-ink-faint">
          한국철도공사 공동물류 플랫폼
        </span>
      </div>
    </div>
  );
}
