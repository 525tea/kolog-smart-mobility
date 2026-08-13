import { useEffect } from 'react';

/* A-00 스플래시 — 로고 플레이 (컨테이너 3장 슬라이드인 → 레일 스윕 → 워드마크 → 슬로건) */

interface Props {
  onReady: () => void;
  message?: string;
  blocked?: boolean;
}

export default function SplashScreen({ onReady, message, blocked = false }: Props) {
  useEffect(() => {
    const done = blocked ? undefined : setTimeout(onReady, 1900);
    return () => clearTimeout(done);
  }, [blocked, onReady]);

  return (
    <div
      className="app-shell relative h-full w-full cursor-pointer overflow-hidden bg-white flex flex-col"
      onClick={() => !blocked && onReady()}
      role="button"
      tabIndex={0}
      aria-label="KO-LOG 시작하기"
      onKeyDown={(event) => event.key === 'Enter' && !blocked && onReady()}
    >
      <div className="flex flex-1 flex-col items-center justify-center gap-7 pb-6">
        <div className="relative w-[210px] animate-riseIn">
          <div className="flex items-end justify-center gap-2">
            <CargoBox className="h-[52px] w-[62px] bg-[#6653d7]" lines={2} />
            <CargoBox className="h-[68px] w-[74px] bg-[#2460ee]" lines={3} />
            <CargoBox className="h-[43px] w-[62px] bg-[#3047b7]" lines={2} />
          </div>
          <div className="mx-auto mt-7 h-[6px] w-[206px] rounded-full bg-[#111c2e]" />
          <div className="mx-auto mt-3 h-[4px] w-[220px] rounded-full bg-[#e4e9f3]" />
          <div className="absolute bottom-[12px] left-[43px] flex gap-[14px]">
            {[0, 1, 2, 3].map((i) => <span key={i} className="size-3 rounded-full bg-[#111c2e]" />)}
          </div>
        </div>
        <div className="text-center">
          <div className="text-[40px] font-black tracking-[-2px] text-[#111c2e]">KO<span className="text-[#6653d7]">-</span>LOG</div>
          <p className="mt-2 text-[13px] font-bold text-[#666]">각자의 화물을 하나의 운송으로, KOLOG</p>
        </div>
      </div>

      <div className="pb-[62px] flex flex-col items-center gap-4">
        <div className="h-[4px] w-[290px] overflow-hidden rounded-full bg-[#eef1f5]">
          <div className="h-full w-[12%] animate-sweep rounded-full bg-[#2460ee]" />
        </div>
        <span className="text-[11px] font-bold text-[#a2aec2]">{message ?? '운송 데이터를 불러오는 중…'}</span>
      </div>
    </div>
  );
}

function CargoBox({ className, lines }: { className: string; lines: number }) {
  return <div className={`rounded-[14px] px-2.5 py-3 shadow-sm ${className}`}>{Array.from({ length: lines }, (_, i) => <span key={i} className="mb-1.5 block h-[5px] rounded-full bg-white/50" />)}</div>;
}
