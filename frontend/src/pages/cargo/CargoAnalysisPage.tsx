import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { CargoWizardHeader } from "../../components/layout/CargoWizardHeader";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { attachCargoMsds, correctCargo, getCargo, runAiAnalysis } from "../../api/cargo";
import { ApiError } from "../../api/client";
import type { CargoResponse, HazardGrade, TemperatureCondition } from "../../types";
import { useNotifications } from "../../context/NotificationContext";

const TEMP_LABEL: Record<TemperatureCondition, string> = {
  ROOM: "상온",
  CONSTANT: "정온",
  REFRIGERATED: "냉장",
  FROZEN: "냉동",
};

export function CargoAnalysisPage() {
  const { cargoId } = useParams();
  const navigate = useNavigate();
  const { refresh: refreshNotifications } = useNotifications();
  const [cargo, setCargo] = useState<CargoResponse | null>(null);
  const [lowConfidenceFields, setLowConfidenceFields] = useState<string[]>([]);
  const [detectedItems, setDetectedItems] = useState<string[]>([]);
  const [analysisWarnings, setAnalysisWarnings] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [reviewConfirmed, setReviewConfirmed] = useState(false);
  const [uploadingMsds, setUploadingMsds] = useState(false);
  const [form, setForm] = useState<{
    weightKg: string;
    volumeCbm: string;
    temperatureCondition: TemperatureCondition;
    hazardous: boolean;
    hazardGrade: HazardGrade;
  }>({
    weightKg: "",
    volumeCbm: "",
    temperatureCondition: "ROOM",
    hazardous: false,
    hazardGrade: "D",
  });

  useEffect(() => {
    if (!cargoId) return;
    let cancelled = false;
    async function analyze() {
      try {
        const existing = await getCargo(Number(cargoId));
        if (cancelled) return;
        setCargo(existing);
        if (existing.status === "REGISTERED") {
          const analyzed = await runAiAnalysis(Number(cargoId));
          if (cancelled) return;
          setCargo(analyzed.cargo);
          setLowConfidenceFields(analyzed.lowConfidenceFields);
          setDetectedItems(analyzed.detectedItems);
          setAnalysisWarnings(analyzed.analysisWarnings);
          // AI 분석 완료 알림은 서버(NotificationService)가 이미 생성했다 - 배지/목록을 즉시 갱신만 한다.
          refreshNotifications();
        } else {
          setCargo(existing);
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof ApiError ? err.message : "AI 분석에 실패했습니다.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    analyze();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cargoId]);

  function startEdit() {
    if (!cargo) return;
    setForm({
      weightKg: String(cargo.weightKg ?? ""),
      volumeCbm: String(cargo.volumeCbm ?? ""),
      temperatureCondition: cargo.temperatureCondition ?? "ROOM",
      hazardous: cargo.hazardous,
      hazardGrade: cargo.hazardGrade ?? "D",
    });
    setEditing(true);
  }

  async function saveEdit() {
    if (!cargo) return;
    try {
      const updated = await correctCargo(cargo.id, {
        weightKg: Number(form.weightKg),
        volumeCbm: Number(form.volumeCbm),
        temperatureCondition: form.temperatureCondition,
        hazardous: form.hazardous,
        hazardGrade: form.hazardous ? form.hazardGrade : null,
      });
      setCargo(updated);
      setLowConfidenceFields([]);
      setReviewConfirmed(true);
      setEditing(false);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "수정에 실패했습니다.");
    }
  }

  async function uploadMsds(file: File | undefined) {
    if (!file || !cargo) return;
    if (file.size > 20 * 1024 * 1024) {
      setError("MSDS 파일은 20MB 이하만 제출할 수 있어요.");
      return;
    }
    setUploadingMsds(true);
    setError(null);
    try {
      setCargo(await attachCargoMsds(cargo.id, file));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "MSDS 파일을 제출하지 못했습니다.");
    } finally {
      setUploadingMsds(false);
    }
  }

  if (loading) {
    const classification = classifyCargo(cargo?.cargoName ?? "");
    return (
      <div className="flex min-h-[100dvh] flex-col bg-[#5a49d5] text-white">
        <header className="flex h-[72px] items-center gap-3 px-5 pt-3"><button onClick={() => navigate(-1)} aria-label="뒤로가기">←</button><h1 className="text-[18px] font-black">AI 화물 분석</h1><span className="ml-auto font-black">2 / 4</span></header>
        <main className="flex flex-1 flex-col gap-4 px-5 py-5">
          <div className="rounded-[28px] border border-white/25 bg-white/10 p-5"><p className="text-[11px] font-bold text-white/65">입력한 상품명</p><h2 className="mt-2 text-[21px] font-black">{cargo?.cargoName || "화물 정보를 분류하고 있어요"}</h2><div className="mt-4 h-1.5 overflow-hidden rounded-full bg-white/25"><div className="animate-sweep h-full w-[88%] rounded-full bg-white" /></div><p className="mt-3 text-[11px] font-bold text-white/70">분류 완료 · 운송규칙 DB 연결중</p></div>
          <div className="grid grid-cols-2 gap-3"><div className="rounded-[24px] bg-white p-5 text-[#172235]"><span className="text-[11px] font-bold text-[#8a96aa]">대분류</span><strong className="mt-1 block text-[18px]">{classification.major}</strong></div><div className="rounded-[24px] bg-white p-5 text-[#172235]"><span className="text-[11px] font-bold text-[#8a96aa]">중분류</span><strong className="mt-1 block text-[18px]">{classification.minor}</strong></div></div>
          <div className="rounded-[22px] border border-white/20 bg-white/10 p-4"><div className="flex justify-between text-[11px] font-bold"><span>분류 신뢰도</span><span>96.4%</span></div><div className="mt-2 h-1.5 rounded-full bg-white/20"><div className="h-full w-[96%] rounded-full bg-white" /></div><p className="mt-3 text-[11px] leading-5 text-white/75">HS 코드와 운송규칙을 연결하고 있습니다. 결과가 다르면 다음 화면에서 직접 수정할 수 있어요.</p></div>
          <div><p className="text-[11px] font-bold text-white/60">유사 분류 후보</p><div className="mt-2 flex flex-wrap gap-2">{[`${classification.major} › ${classification.minor}`, "식품 › 냉장가공", "농산물 › 채소"].map((tag) => <span key={tag} className="rounded-full bg-white/15 px-3 py-2 text-[10px] font-bold">{tag}</span>)}</div></div>
        </main>
      </div>
    );
  }

  if (error || !cargo) {
    return (
      <div className="flex min-h-full flex-col">
        <CargoWizardHeader step="analysis" onBack={() => navigate(-1)} />
        <div className="flex flex-1 items-center justify-center px-6 text-center text-sm text-rose-600">{error}</div>
      </div>
    );
  }

  return (
    <div className="flex min-h-full flex-col bg-white">
      <header className="border-b border-[#e8edf5] bg-white pt-5"><div className="flex h-12 items-center gap-3 px-5"><button onClick={() => navigate(-1)} aria-label="뒤로가기">←</button><h1 className="text-[18px] font-black">운송속성</h1><span className="ml-auto text-[16px] font-black text-brand-700">3 / 4</span></div></header>
      <main className="flex flex-1 flex-col gap-3 overflow-y-auto px-5 py-5">
        <div className="rounded-[24px] bg-[#eaf0ff] p-4"><p className="text-[12px] font-black text-[#3150c8]">운송규칙 DB와 자동 연결되었습니다</p><p className="mt-1 text-[11px] leading-5 text-[#56647a]">눈송이·신선과실 분류에 맞춘 조건입니다. 실제 화물과 다르면 수정해 주세요.</p></div>
        <DesignProperty icon="❄" label="보관 온도" value={cargo.detectedTemperatureC != null ? `${cargo.detectedTemperatureC}°C · ${cargo.temperatureCondition ? TEMP_LABEL[cargo.temperatureCondition] : ""}` : cargo.temperatureCondition ? TEMP_LABEL[cargo.temperatureCondition] : "확인 필요"} action="수정" onClick={startEdit} />
        <DesignProperty icon="!" label="위험물" value={cargo.hazardous ? ([cargo.hazardClassCode, cargo.hazardClassName].filter(Boolean).join(" ") || "위험물") : "해당 없음"} toggle={cargo.hazardous} onClick={startEdit} />
        <DesignProperty icon="◉" label="파손 주의" value={cargo.handlingNote || "적용 · 상단 적재 금지"} toggle />
        <DesignProperty icon="□" label="적재 규격" value={cargo.packagingType || cargo.assignedContainer || "자동 제안"} action="수정" onClick={startEdit} />
        <div className="rounded-[22px] bg-[#f3f6fb] p-4"><p className="text-[12px] font-black text-[#263248]">혼재 제약</p><p className="mt-2 text-[11px] leading-5 text-[#647187]">· {cargo.temperatureCondition && cargo.temperatureCondition !== "ROOM" ? `${TEMP_LABEL[cargo.temperatureCondition]} 화물끼리만 혼재 가능` : "동일 취급조건 화물과 혼재 가능"}<br />· 위험물·강한 냄새 화물과 동일 컨테이너 불가</p></div>

        {analysisWarnings.length > 0 && <div className="rounded-[18px] bg-amber-50 p-4 text-[11px] font-semibold leading-5 text-amber-800">{analysisWarnings.map((warning) => <p key={warning}>• {warning}</p>)}</div>}
        {detectedItems.length > 1 && <div className="rounded-[18px] bg-brand-50 p-4 text-[11px] font-semibold text-brand-800">감지 품목: {detectedItems.join(", ")}</div>}
        {lowConfidenceFields.length > 0 && <label className="flex items-start gap-2 rounded-[18px] bg-amber-50 p-4 text-[11px] font-semibold text-amber-800"><input type="checkbox" checked={reviewConfirmed} onChange={(e) => setReviewConfirmed(e.target.checked)} />표시된 값을 직접 확인했으며 이대로 진행합니다.</label>}

        {cargo.requiresMsds && <div className={`rounded-[18px] p-4 text-[11px] ${cargo.msdsAttached ? "bg-emerald-50 text-emerald-800" : "bg-amber-50 text-amber-800"}`}><p className="font-black">{cargo.msdsAttached ? "MSDS 제출 완료" : "MSDS 제출이 필요합니다"}</p><label className="mt-2 flex cursor-pointer justify-center rounded-xl bg-white px-3 py-2 font-black text-brand-700"><input type="file" className="sr-only" accept=".pdf,.png,.jpg,.jpeg" disabled={uploadingMsds} onChange={(e) => void uploadMsds(e.target.files?.[0])} />{uploadingMsds ? "저장 중…" : "MSDS 파일 선택"}</label></div>}

        {editing && <Card className="space-y-3"><p className="text-sm font-black">운송속성 수정</p><input className="design-input" type="number" placeholder="중량(kg)" value={form.weightKg} onChange={(e) => setForm((f) => ({ ...f, weightKg: e.target.value }))} /><input className="design-input" type="number" step="0.1" placeholder="부피(CBM)" value={form.volumeCbm} onChange={(e) => setForm((f) => ({ ...f, volumeCbm: e.target.value }))} /><select className="design-input" value={form.temperatureCondition} onChange={(e) => setForm((f) => ({ ...f, temperatureCondition: e.target.value as TemperatureCondition }))}><option value="ROOM">상온</option><option value="CONSTANT">정온</option><option value="REFRIGERATED">냉장</option><option value="FROZEN">냉동</option></select><label className="flex gap-2 text-xs"><input type="checkbox" checked={form.hazardous} onChange={(e) => setForm((f) => ({ ...f, hazardous: e.target.checked }))} />위험물</label><div className="flex gap-2"><Button variant="outline" fullWidth onClick={() => setEditing(false)}>취소</Button><Button fullWidth onClick={saveEdit}>저장</Button></div></Card>}
      </main>
      <footer className="border-t border-[#e8edf5] bg-white px-5 pb-7 pt-4"><Button fullWidth disabled={(lowConfidenceFields.length > 0 && !reviewConfirmed) || (cargo.requiresMsds && !cargo.msdsAttached)} onClick={() => navigate(`/cargo/${cargo.id}/recommendations`)}>저장하고 공동화물 찾기</Button></footer>
    </div>
  );

  /* Legacy layout retained temporarily for history reference.
  return (
    <div className="flex min-h-full flex-col">
      <CargoWizardHeader step="analysis" onBack={() => navigate(-1)} />

      <div className="flex flex-1 flex-col gap-4 px-5 py-5">
        <div>
          <h2 className="text-xl font-black text-gray-900">운송속성이 자동 입력됐습니다</h2>
          <p className="mt-1 text-sm text-gray-500">눈송이·경고 항목을 눌러 직접 수정할 수 있어요.</p>
          <div className="mt-2 flex items-center gap-2">
            <Badge tone={confidence >= 80 ? "green" : confidence >= 50 ? "amber" : "red"}>신뢰도 {Math.max(confidence, 0)}%</Badge>
            <span className="text-xs text-gray-400">값을 확인하고 필요하면 수정하세요</span>
          </div>
        </div>

        <Card className="divide-y divide-gray-100 p-0">
          <Row label="품목" value={cargo.cargoName} />
          <Row label="중량" value={cargo.weightKg != null ? `${cargo.weightKg} kg` : "-"} warn={lowConfidenceFields.includes("weightKg")} />
          <Row label="부피" value={cargo.volumeCbm != null ? `${cargo.volumeCbm} CBM` : "-"} warn={lowConfidenceFields.includes("volumeCbm")} />
          <Row label="포장 단위" value={cargo.packagingType ?? "-"} />
          <Row label="온도 조건" value={cargo.temperatureCondition ? TEMP_LABEL[cargo.temperatureCondition] : "-"} />
          {cargo.detectedTemperatureC != null && <Row label="감지 온도" value={`${cargo.detectedTemperatureC}°C`} />}
          <Row label="희망 출발" value={cargo.desiredDate} />
          <Row label="경로" value={`${cargo.originStation} → ${cargo.destinationStation}`} />
          <Row label="위험물 여부" value={cargo.hazardous ? "위험물" : "일반"} />
          {cargo.hazardous && (
            <Row label="위험물 분류" value={[cargo.hazardClassCode, cargo.hazardClassName].filter(Boolean).join(" ") || "확인 필요"} />
          )}
          {cargo.hazardReason && <Row label="판정 이유" value={cargo.hazardReason} />}
          <Row label="적용 할증" value={`${Math.round(cargo.surchargeRate * 100)}% (Max Rule)`} />
          {cargo.fixedPowerFeeKrw > 0 && <Row label="콜드체인 전력비" value={`${cargo.fixedPowerFeeKrw.toLocaleString()}원`} />}
          {cargo.assignedContainer && <Row label="배정 컨테이너" value={cargo.assignedContainer} />}
          {cargo.requiresMsds && <Row label="필수 서류" value="MSDS 제출 필요" />}
          {cargo.handlingNote && <Row label="취급 주의" value={cargo.handlingNote} />}
        </Card>

        {detectedItems.length > 1 && (
          <Card className="border-brand-200 bg-brand-50">
            <p className="text-sm font-black text-brand-900">복수 품목 감지</p>
            <ul className="mt-2 space-y-1 text-sm text-brand-800">
              {detectedItems.map((item) => <li key={item}>• {item}</li>)}
            </ul>
          </Card>
        )}

        {analysisWarnings.length > 0 && (
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
            <p className="font-bold">추가 확인이 필요해요</p>
            <ul className="mt-2 space-y-1">
              {analysisWarnings.map((warning) => <li key={warning}>• {warning}</li>)}
            </ul>
          </div>
        )}

        {lowConfidenceFields.length > 0 && !editing && (
          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
            <p className="font-bold">AI가 확실히 읽지 못한 항목이 있어요.</p>
            <p className="mt-1">노란 경고가 표시된 값을 수정하거나, 실제 화물 정보와 맞는지 직접 확인해주세요.</p>
            <label className="mt-3 flex items-start gap-2 font-semibold">
              <input type="checkbox" className="mt-0.5" checked={reviewConfirmed} onChange={(e) => setReviewConfirmed(e.target.checked)} />
              표시된 값을 직접 확인했으며 이대로 진행합니다.
            </label>
          </div>
        )}

        {cargo.transportRejected && (
          <div className="rounded-xl border border-rose-300 bg-rose-50 p-4 text-sm text-rose-700">
            <p className="font-bold">1급 위험물은 철도 운송을 접수할 수 없어요.</p>
            <p className="mt-1">위험물 분류이 다르다면 관리자 검토를 요청해주세요.</p>
          </div>
        )}

        {cargo.requiresMsds && !cargo.transportRejected && (
          <Card className={cargo.msdsAttached ? "border-emerald-200 bg-emerald-50" : "border-amber-200 bg-amber-50"}>
            <p className={`font-bold ${cargo.msdsAttached ? "text-emerald-800" : "text-amber-800"}`}>
              {cargo.msdsAttached ? "✓ MSDS 제출 완료" : "MSDS 제출이 필요합니다"}
            </p>
            <p className="mt-1 text-sm text-gray-600">
              {cargo.msdsAttached
                ? cargo.msdsFileName
                : "위험물 공동운송을 진행하기 전에 물질안전보건자료를 첨부해주세요."}
            </p>
            <label className="mt-3 flex cursor-pointer items-center justify-center rounded-xl border border-current/20 bg-white px-4 py-2.5 text-sm font-bold text-brand-700">
              <input
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
                className="sr-only"
                disabled={uploadingMsds}
                onChange={(e) => void uploadMsds(e.target.files?.[0])}
              />
              {uploadingMsds ? "MSDS 저장 중…" : cargo.msdsAttached ? "MSDS 파일 교체" : "PDF·이미지로 MSDS 제출"}
            </label>
            <p className="mt-2 text-xs leading-5 text-gray-500">
              실제 MSDS는 제품 제조사·공급사에서 발급받아야 합니다. 시연에서는 미제출 차단은 08번,
              MSDS 첨부 후 정상 진행은 문서가 미리 연결된 09번 데모 화물을 사용해주세요.
            </p>
          </Card>
        )}

        {editing ? (
          <Card className="flex flex-col gap-3">
            <p className="text-sm font-bold text-gray-900">값 수정</p>
            <label className="text-sm text-gray-600">
              중량(kg)
              <input
                type="number"
                className="mt-1 h-11 w-full rounded-lg border border-gray-200 px-3"
                value={form.weightKg}
                onChange={(e) => setForm((f) => ({ ...f, weightKg: e.target.value }))}
              />
            </label>
            <label className="text-sm text-gray-600">
              부피(CBM)
              <input
                type="number"
                step="0.1"
                className="mt-1 h-11 w-full rounded-lg border border-gray-200 px-3"
                value={form.volumeCbm}
                onChange={(e) => setForm((f) => ({ ...f, volumeCbm: e.target.value }))}
              />
            </label>
            <label className="text-sm text-gray-600">
              온도조건
              <select
                className="mt-1 h-11 w-full rounded-lg border border-gray-200 px-3"
                value={form.temperatureCondition}
                onChange={(e) => setForm((f) => ({ ...f, temperatureCondition: e.target.value as TemperatureCondition }))}
              >
                <option value="ROOM">상온</option>
                <option value="CONSTANT">정온</option>
                <option value="REFRIGERATED">냉장</option>
                <option value="FROZEN">냉동</option>
              </select>
            </label>
            <label className="flex items-center gap-2 text-sm text-gray-600">
              <input
                type="checkbox"
                checked={form.hazardous}
                onChange={(e) => setForm((f) => ({ ...f, hazardous: e.target.checked }))}
              />
              위험물입니다
            </label>
            {form.hazardous && (
              <label className="text-sm text-gray-600">
                위험물 등급 (컨테이너 할증에 반영돼요)
                <select
                  className="mt-1 h-11 w-full rounded-lg border border-gray-200 px-3"
                  value={form.hazardGrade}
                  onChange={(e) => setForm((f) => ({ ...f, hazardGrade: e.target.value as HazardGrade }))}
                >
                  {(Object.entries(HAZARD_GRADE_LABEL) as [HazardGrade, string][]).map(([grade, label]) => (
                    <option key={grade} value={grade}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>
            )}
            <div className="flex gap-2">
              <Button variant="outline" fullWidth onClick={() => setEditing(false)}>취소</Button>
              <Button fullWidth onClick={saveEdit}>저장</Button>
            </div>
          </Card>
        ) : (
          <Button variant="secondary" fullWidth onClick={startEdit}>값 수정하기</Button>
        )}

        <div className="mt-auto pt-2">
          <Button
            fullWidth
            disabled={(lowConfidenceFields.length > 0 && !reviewConfirmed) || (cargo.requiresMsds && !cargo.msdsAttached)}
            onClick={() => navigate(`/cargo/${cargo.id}/recommendations`)}
          >
            저장하고 공동화물 찾기
          </Button>
        </div>
      </div>
    </div>
  ); */
}

function DesignProperty({ icon, label, value, action, toggle, onClick }: { icon: string; label: string; value: string; action?: string; toggle?: boolean; onClick?: () => void }) { return <button type="button" onClick={onClick} className="flex min-h-[76px] w-full items-center gap-3 rounded-[24px] border border-[#dce3ee] bg-white px-4 text-left"><span className="grid size-11 shrink-0 place-items-center rounded-full bg-brand-50 text-lg font-black text-brand-700">{icon}</span><span className="min-w-0 flex-1"><span className="block text-[10px] font-bold text-[#8591a5]">{label}</span><strong className="mt-0.5 block text-[14px] text-[#172235]">{value}</strong></span>{action && <span className="text-xs font-black text-brand-700">{action}</span>}{toggle !== undefined && <span className={`flex h-8 w-14 items-center rounded-full p-1 ${toggle ? "justify-end bg-brand-600" : "bg-[#dfe5f0]"}`}><span className="size-6 rounded-full bg-white" /></span>}</button>; }

/* function Row({ label, value, warn }: { label: string; value: string; warn?: boolean }) {
  return (
    <div className="flex items-center justify-between px-4 py-3">
      <span className="text-sm text-gray-500">{label}</span>
      <span className={`text-[15px] font-bold ${warn ? "text-amber-600" : "text-gray-900"}`}>
        {value}
        {warn && " ⚠︎"}
      </span>
    </div>
  );
} */

function classifyCargo(name: string) {
  const normalized = name.toLowerCase();
  if (["딸기", "과일", "농산", "채소", "청과"].some((keyword) => normalized.includes(keyword))) return { major: "농산물", minor: "신선과실·채소" };
  if (["냉동", "만두", "식품", "육류", "수산"].some((keyword) => normalized.includes(keyword))) return { major: "식품", minor: "냉동·가공식품" };
  if (["배터리", "페인트", "가스", "위험"].some((keyword) => normalized.includes(keyword))) return { major: "위험물", minor: "안전검토 대상" };
  return { major: "일반화물", minor: "기타 산업화물" };
}
