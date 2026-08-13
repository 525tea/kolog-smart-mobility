import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button } from "../../components/ui/Button";
import { getCargo } from "../../api/cargo";
import { getConsolidationDetail } from "../../api/consolidation";
import type {
  CargoResponse,
  ConsolidationDetailResponse,
} from "../../types";
export function DoorToDoorPage() {
  const { cargoId, groupId } = useParams();
  const navigate = useNavigate();
  const [cargo, setCargo] = useState<CargoResponse | null>(null);
  const [group, setGroup] = useState<ConsolidationDetailResponse | null>(null);
  const [first] = useState(true);
  const [last, setLast] = useState(true);
  const [slot, setSlot] = useState(0);
  useEffect(() => {
    if (!cargoId || !groupId) return;
    Promise.all([
      getCargo(+cargoId),
      getConsolidationDetail(+groupId),
    ]).then(([c, g]) => {
      setCargo(c);
      setGroup(g);
    });
  }, [cargoId, groupId]);
  if (!cargo || !group)
    return (
      <div className="grid min-h-full place-items-center text-sm text-gray-400">
        연계 구간을 구성하는 중…
      </div>
    );
  return (
    <div className="flex min-h-full flex-col bg-white">
      <header className="border-b border-[#e7ecf4] pt-5">
        <div className="flex h-12 items-center gap-3 px-5">
          <button onClick={() => navigate(-1)}>←</button>
          <h1 className="text-[18px] font-black">집하 · 라스트마일</h1>
        </div>
      </header>
      <main className="flex flex-1 flex-col gap-4 overflow-y-auto px-5 py-4">
        <div className="relative h-[180px] rounded-[28px] bg-[#e8efff]">
          <span className="absolute left-[22%] top-[32%] size-4 rounded-full bg-brand-600 ring-4 ring-white" />
          <span className="absolute bottom-[32%] right-[22%] size-4 rounded-full bg-[#0ba995] ring-4 ring-white" />
          <strong className="absolute inset-0 grid place-items-center text-[12px] text-[#5872b4]">
            MAP · 집하지 → {group.originStation} 경로
          </strong>
        </div>
        <section>
          <h2 className="mb-2 text-[13px] font-black">집하 (First mile)</h2>
          <div className="rounded-[24px] border border-[#dce3ee] p-4">
            <div className="flex justify-between">
              <strong className="text-[12px]">
                전남 광양시 항만로 21 · 1창고
              </strong>
              <button className="text-[10px] font-black text-brand-700">
                변경
              </button>
            </div>
            <div className="mt-3 grid grid-cols-3 gap-2">
              {["10:00-11:00", "11:00-12:00", "13:00-14:00"].map(
                (value, index) => (
                  <button
                    key={value}
                    onClick={() => setSlot(index)}
                    className={`h-11 rounded-[16px] text-[10px] font-black ${slot === index ? "bg-brand-600 text-white" : "bg-[#f2f5fb] text-[#657288]"}`}
                  >
                    {value}
                  </button>
                ),
              )}
            </div>
            <p className="mt-3 text-[10px] font-semibold text-[#8290a4]">
              5톤 윙바디 · 냉장 차량 배정 · {group.originStation} 12:40 도착
              예정
            </p>
          </div>
        </section>
        <section>
          <h2 className="mb-2 text-[13px] font-black">
            라스트마일 (Last mile)
          </h2>
          <Choice
            active={last}
            title={`${group.destinationStation} → 안양 물류센터`}
            detail="도착 당일 23:30 배송 · 68,000원"
            onClick={() => setLast(true)}
          />
          <Choice
            active={!last}
            title="역 픽업 (직접 수령)"
            detail={`${group.destinationStation} 도착 보관 12시간 무료`}
            onClick={() => setLast(false)}
          />
        </section>
        <section className="rounded-[22px] bg-[#f3f6fb] p-4">
          <p className="text-[11px] font-black">연계 구간 합계</p>
          <Line label="집하" value={first ? "92,000원" : "0원"} />
          <Line label="라스트마일" value={last ? "68,000원" : "0원"} />
        </section>
      </main>
      <footer className="border-t border-[#e7ecf4] px-5 pb-7 pt-4">
        <Button
          fullWidth
          onClick={() =>
            navigate(
              `/cargo/${cargo.id}/integrated-reservation/${group.id}?firstMile=${first ? 1 : 0}&lastMile=${last ? 1 : 0}`,
            )
          }
        >
          통합 예약 확인
        </Button>
      </footer>
    </div>
  );
}
function Choice({
  active,
  title,
  detail,
  onClick,
}: {
  active: boolean;
  title: string;
  detail: string;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={`mb-2 flex w-full items-center rounded-[22px] p-4 text-left ${active ? "border-2 border-brand-600" : "border border-[#dce3ee]"}`}
    >
      <span>
        <strong className="block text-[12px]">{title}</strong>
        <span className="mt-1 block text-[10px] text-[#8290a4]">{detail}</span>
      </span>
      <span
        className={`ml-auto size-6 rounded-full border-2 ${active ? "border-brand-600 bg-brand-600 shadow-[inset_0_0_0_6px_white]" : "border-[#d4dbe6]"}`}
      />
    </button>
  );
}
function Line({ label, value }: { label: string; value: string }) {
  return (
    <div className="mt-2 flex justify-between text-[11px]">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}
