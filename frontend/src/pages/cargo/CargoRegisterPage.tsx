import { useState, type FormEvent } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { CargoWizardHeader } from "../../components/layout/CargoWizardHeader";
import { Button } from "../../components/ui/Button";
import { TextArea } from "../../components/ui/Input";
import { extractCargoDocument, previewStationMapping, registerCargo } from "../../api/cargo";
import { ApiError } from "../../api/client";
import { useNotifications } from "../../context/NotificationContext";
import type { CargoDocumentExtractionResponse } from "../../types";

type CargoType = "GENERAL" | "FROZEN_FRESH" | "HAZARDOUS";
type TempOption = "DRY" | "FROZEN" | "CHILLED" | "CONSTANT";
type InputMode = "NATURAL" | "STRUCTURED";
type ServiceMode = "INDIVIDUAL" | "CO_LOAD";

const CARGO_TYPES: { key: CargoType; label: string }[] = [
  { key: "GENERAL", label: "일반화물" },
  { key: "FROZEN_FRESH", label: "냉동·신선" },
  { key: "HAZARDOUS", label: "위험물" },
];

const TEMP_OPTIONS: { key: TempOption; label: string; hint: string }[] = [
  { key: "DRY", label: "드라이", hint: "상온" },
  { key: "FROZEN", label: "-18~-20°C", hint: "냉동 -18~-20도" },
  { key: "CHILLED", label: "0~8°C", hint: "냉장 0~8도" },
  { key: "CONSTANT", label: "정온", hint: "정온" },
];

function formatKoreanAmount(value: number) {
  if (value >= 100_000_000) {
    const eok = Math.floor(value / 100_000_000);
    const man = Math.floor((value % 100_000_000) / 10_000);
    return `${eok}억${man > 0 ? ` ${man.toLocaleString("ko-KR")}만원` : "원"}`;
  }
  if (value >= 10_000) return `${Math.floor(value / 10_000).toLocaleString("ko-KR")}만원`;
  return `${value.toLocaleString("ko-KR")}원`;
}

export function CargoRegisterPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { refresh: refreshNotifications } = useNotifications();
  const [serviceMode, setServiceMode] = useState<ServiceMode>(() => searchParams.get("mode") === "INDIVIDUAL" ? "INDIVIDUAL" : "CO_LOAD");
  const [originStation, setOriginStation] = useState(() => searchParams.get("origin") ?? "");
  const [destinationStation, setDestinationStation] = useState(() => searchParams.get("destination") ?? "");
  const [desiredDate, setDesiredDate] = useState(() => {
    const queryDate = searchParams.get("date");
    if (queryDate) return queryDate;
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return [tomorrow.getFullYear(), String(tomorrow.getMonth() + 1).padStart(2, "0"), String(tomorrow.getDate()).padStart(2, "0")].join("-");
  });
  const [cargoType] = useState<CargoType>("GENERAL");
  const [tempOption] = useState<TempOption | null>(null);
  const [rawInput, setRawInput] = useState("");
  const [inputMode, setInputMode] = useState<InputMode>("STRUCTURED");
  const [structured, setStructured] = useState({
    cargoName: "",
    weightKg: "",
    volumeCbm: "",
    packageCount: "",
    packageUnit: "파렛트",
    handlingNote: "",
  });
  const [showVolumeCalculator, setShowVolumeCalculator] = useState(false);
  const [dimensions, setDimensions] = useState({ widthCm: "", depthCm: "", heightCm: "", count: "" });
  const [declaredValueKrw, setDeclaredValueKrw] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [mappingHint, setMappingHint] = useState<{ origin?: string; destination?: string }>({});
  const [outOfCoverage, setOutOfCoverage] = useState<{ origin: boolean; destination: boolean }>({ origin: false, destination: false });
  const [attachmentName, setAttachmentName] = useState<string | null>(null);
  const [documentResult, setDocumentResult] = useState<CargoDocumentExtractionResponse | null>(null);
  const [extractingDocument, setExtractingDocument] = useState(false);

  async function checkStation(kind: "origin" | "destination", location: string) {
    if (!location.trim()) return;
    try {
      const result = await previewStationMapping(location);
      setOutOfCoverage((current) => ({ ...current, [kind]: result.outOfCoverage }));
      if (result.mapped && result.railStation) {
        if (kind === "origin") setOriginStation(result.railStation);
        else setDestinationStation(result.railStation);
      }
      setMappingHint((current) => ({
        ...current,
        [kind]: result.outOfCoverage
          ? result.userMessage ?? "철도 운송 지원 권역이 아닙니다."
          : result.mapped
            ? result.userMessage ?? `철도 거점 ${result.railStation}(으)로 연결`
            : undefined,
      }));
    } catch (err) {
      setOutOfCoverage((current) => ({ ...current, [kind]: false }));
      setMappingHint((current) => ({
        ...current,
        [kind]: err instanceof ApiError ? err.message : "지원 거점을 확인하지 못했어요.",
      }));
    }
  }

  async function handleDocumentAttachment(file: File | undefined) {
    if (!file) return;
    if (file.size > 20 * 1024 * 1024) {
      setError("파일은 20MB 이하만 첨부할 수 있어요.");
      return;
    }
    if (!/\.(pdf|png|jpe?g|gif|tiff?|bmp|webp|xlsx?|txt|csv|json|xml)$/i.test(file.name)) {
      setError("PDF·이미지·엑셀 또는 텍스트 문서를 선택해주세요.");
      return;
    }
    setExtractingDocument(true);
    setError(null);
    try {
      const result = await extractCargoDocument(file);
      if (!result.extractedText.trim()) {
        setError("문서에서 읽을 수 있는 내용을 찾지 못했어요. 스캔 품질을 확인해주세요.");
        return;
      }
      setRawInput((current) => [current, `첨부 문서(${file.name}) 추출 결과:\n${result.extractedText}`].filter(Boolean).join("\n\n"));
      setInputMode("NATURAL");
      setAttachmentName(file.name);
      setDocumentResult(result);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "문서를 분석하지 못했습니다.");
      setDocumentResult(null);
      setAttachmentName(null);
    } finally {
      setExtractingDocument(false);
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (serviceMode === "CO_LOAD" && (outOfCoverage.origin || outOfCoverage.destination)) {
      setError("철도 화물역 지원 권역 밖입니다. 아래에서 트럭 직배송 Plan B로 전환해주세요.");
      return;
    }
    if (inputMode === "NATURAL" && rawInput.trim().length < 10) {
      setError("품목·중량 등 화물 정보를 조금 더 자세히 적거나 ‘항목별 입력’으로 전환해주세요.");
      return;
    }
    if (inputMode === "STRUCTURED" && (!structured.cargoName.trim() || !structured.weightKg)) {
      setError("항목별 입력에서는 품목명과 중량을 반드시 입력해주세요.");
      return;
    }
    if (!declaredValueKrw || Number(declaredValueKrw) <= 0) {
      setError("화물가액은 보험료와 배상한도 산정을 위해 반드시 입력해주세요.");
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      // 화면에서 고른 화물유형/온도구분을 AI가 인식할 수 있도록 힌트 문구로 합쳐 rawInput에 담는다.
      // 냉동·신선은 상위 UI 그룹명이다. 그대로 AI 입력에 넣으면 냉장(0~8℃)을 골라도
      // "냉동" 키워드가 우선되어 오분류되므로, 실제 온도 선택값만 냉동/냉장을 결정하게 한다.
      const typeLabel = cargoType === "GENERAL"
        ? ""
        : cargoType === "FROZEN_FRESH"
        ? "신선식품"
        : CARGO_TYPES.find((t) => t.key === cargoType)?.label ?? "";
      const tempHint = TEMP_OPTIONS.find((t) => t.key === tempOption)?.hint ?? "";
      const structuredInput = inputMode === "STRUCTURED"
        ? [
            `품목: ${structured.cargoName}`,
            `중량: ${structured.weightKg}kg`,
            structured.volumeCbm && `부피: ${structured.volumeCbm}CBM`,
            structured.packageCount && `포장: ${structured.packageUnit} ${structured.packageCount}개`,
            structured.handlingNote && `취급 요구사항: ${structured.handlingNote}`,
          ].filter(Boolean).join(". ")
        : rawInput;
      const combinedInput = [structuredInput, `화물유형: ${typeLabel}`, tempHint && `온도조건: ${tempHint}`]
        .filter(Boolean)
        .join(". ");
      const cargoName = inputMode === "STRUCTURED"
        ? structured.cargoName.trim().slice(0, 50)
        : rawInput.split(/[\d,.\n]/)[0]?.trim().slice(0, 50) || "화물";

      const cargo = await registerCargo({
        cargoName,
        rawInput: combinedInput,
        originStation,
        destinationStation,
        desiredDate,
        serviceMode,
        declaredValueKrw: Number(declaredValueKrw),
      });
      // 등록 완료 알림은 서버(NotificationService)가 이미 생성했다 - 배지/목록을 즉시 갱신만 한다.
      refreshNotifications();
      navigate(`/cargo/${cargo.id}/analysis`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "화물 등록에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  const nextDate = (() => { const date = new Date(`${desiredDate}T12:00:00`); date.setDate(date.getDate() + 1); return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, "0"), String(date.getDate()).padStart(2, "0")].join("-"); })();

  function calculateVolume() {
    const width = Number(dimensions.widthCm);
    const depth = Number(dimensions.depthCm);
    const height = Number(dimensions.heightCm);
    const count = Number(dimensions.count);
    if (![width, depth, height, count].every((value) => Number.isFinite(value) && value > 0)) {
      setError("가로·세로·높이와 수량을 모두 0보다 크게 입력해주세요.");
      return;
    }
    const cbm = (width * depth * height * count) / 1_000_000;
    const value = cbm.toFixed(3).replace(/\.?0+$/, "");
    setStructured((current) => ({ ...current, volumeCbm: value, packageCount: dimensions.count, packageUnit: "박스" }));
    setError(null);
    setShowVolumeCalculator(false);
  }

  return (
    <div className="flex min-h-full flex-col bg-white">
      <CargoWizardHeader step="register" onBack={() => navigate("/cargo/new")} />
      <form className="flex flex-1 flex-col" onSubmit={handleSubmit}>
        <main className="flex flex-1 flex-col gap-5 overflow-y-auto px-5 py-5">
          <h2 className="text-[22px] font-black leading-[1.35] text-[#152033]">어떤 화물을 보내시나요?<br />상품명만 넣으면 나머지는 AI가 채웁니다</h2>

          <label><span className="mb-2 block text-[12px] font-black text-[#7b879b]">상품명</span><input aria-label="상품명" value={structured.cargoName} onChange={(e) => setStructured((s) => ({ ...s, cargoName: e.target.value }))} placeholder="예) 냉장 딸기 생과" className="h-[58px] w-full rounded-[20px] border-2 border-brand-600 bg-white px-5 text-[15px] font-black outline-none ring-[8px] ring-brand-50" /></label>
          <p className="-mt-3 text-[10px] font-semibold text-[#a0aabd]">입력한 상품명은 LLM 분류에 사용됩니다</p>

          <div className="grid grid-cols-2 gap-3">
            <DesignField label="출발지"><input aria-label="출발지" value={originStation} onChange={(e) => { setOriginStation(e.target.value); setOutOfCoverage((v) => ({ ...v, origin: false })); }} onBlur={() => checkStation("origin", originStation)} required className="design-input" placeholder="광양" /></DesignField>
            <DesignField label="도착지"><input aria-label="도착지" value={destinationStation} onChange={(e) => { setDestinationStation(e.target.value); setOutOfCoverage((v) => ({ ...v, destination: false })); }} onBlur={() => checkStation("destination", destinationStation)} required className="design-input" placeholder="의왕ICD" /></DesignField>
            <DesignField label="중량 (kg)"><input aria-label="중량 (kg)" type="number" min={0.1} step="0.1" value={structured.weightKg} onChange={(e) => setStructured((s) => ({ ...s, weightKg: e.target.value }))} required className="design-input" placeholder="4,200" /></DesignField>
            <DesignField label="CBM"><input aria-label="CBM" type="number" min={0} step="0.1" value={structured.volumeCbm} onChange={(e) => setStructured((s) => ({ ...s, volumeCbm: e.target.value }))} className="design-input" placeholder="18.6" /></DesignField>
          </div>
          <button type="button" onClick={() => setShowVolumeCalculator((open) => !open)} className="-mt-3 self-end text-[12px] font-black text-brand-700">{showVolumeCalculator ? "부피 계산기 닫기" : "박스 규격으로 CBM 계산 ›"}</button>
          {showVolumeCalculator && (
            <section className="-mt-2 rounded-[18px] border border-brand-100 bg-brand-50 p-4">
              <div className="mb-3 flex items-center justify-between"><h3 className="text-[14px] font-black text-[#182237]">부피 계산기</h3><span className="text-[12px] font-bold text-[#68758b]">cm 기준</span></div>
              <div className="grid grid-cols-4 gap-2">
                {([['widthCm', '가로'], ['depthCm', '세로'], ['heightCm', '높이'], ['count', '수량']] as const).map(([key, label]) => (
                  <label key={key}><span className="mb-1 block text-center text-[11px] font-black text-[#68758b]">{label}</span><input aria-label={label} type="number" min="0.1" step={key === "count" ? "1" : "0.1"} value={dimensions[key]} onChange={(event) => setDimensions((current) => ({ ...current, [key]: event.target.value }))} className="h-11 w-full rounded-xl border border-[#d8e1f2] bg-white px-2 text-center text-[13px] font-black outline-none focus:border-brand-600" /></label>
                ))}
              </div>
              <p className="mt-3 text-[12px] font-semibold text-[#68758b]">가로 × 세로 × 높이 × 수량 ÷ 1,000,000</p>
              <button type="button" onClick={calculateVolume} className="mt-3 h-11 w-full rounded-xl bg-brand-700 text-[14px] font-black text-white">CBM 계산해서 입력</button>
            </section>
          )}
          <DesignField label="화물가액 (원) *"><input aria-label="화물가액" type="text" inputMode="numeric" required placeholder="예) 30,000,000" value={declaredValueKrw ? Number(declaredValueKrw).toLocaleString("ko-KR") : ""} onChange={(event) => setDeclaredValueKrw(event.target.value.replace(/\D/g, ""))} className="design-input" /></DesignField>
          <p className="-mt-3 text-[12px] font-semibold text-[#68758b]">{declaredValueKrw ? `약 ${formatKoreanAmount(Number(declaredValueKrw))}` : "적재보험료와 배상한도 산정을 위한 필수 항목입니다"}</p>
          {(mappingHint.origin || mappingHint.destination) && <p className="text-[10px] font-semibold text-brand-700">{mappingHint.origin ?? mappingHint.destination}</p>}

          <section><p className="mb-2 text-[12px] font-black text-[#7b879b]">희망 운송일</p><div className="grid grid-cols-3 gap-2">
            <DesignDate value={desiredDate} active onClick={() => undefined} />
            <DesignDate value={nextDate} onClick={() => setDesiredDate(nextDate)} />
            <label className="grid h-[54px] cursor-pointer place-items-center rounded-[18px] bg-[#f2f5fb] text-[12px] font-black text-[#68758b]">직접 선택<input type="date" value={desiredDate} onChange={(e) => setDesiredDate(e.target.value)} className="sr-only" /></label>
          </div></section>

          <div className="rounded-[18px] bg-[#f3f6fc] px-4 py-3 text-[11px] font-semibold leading-5 text-[#59667b]"><span className="mr-2 inline-block size-2 rounded-full bg-brand-600" />파렛트 규격과 적재 방식은 다음 단계에서 AI 분류 결과와 함께 자동 제안됩니다.</div>

          {(outOfCoverage.origin || outOfCoverage.destination) && <div className="rounded-[18px] bg-amber-50 p-4 text-xs font-bold text-amber-800">철도 화물역 지원 권역 밖입니다. <button type="button" className="underline" onClick={() => setServiceMode("INDIVIDUAL")}>트럭 직배송으로 전환</button></div>}

          <details className="rounded-[18px] border border-[#e0e6f0]"><summary className="cursor-pointer px-4 py-3 text-[12px] font-black text-brand-700">문서·자연어·화물가액 추가 입력</summary><div className="space-y-3 border-t border-[#edf1f6] p-4">
            <TextArea rows={3} value={rawInput} onChange={(e) => { setRawInput(e.target.value); setInputMode(e.target.value ? "NATURAL" : "STRUCTURED"); }} placeholder="송장 내용이나 취급 요구사항을 자연어로 입력하세요." />
            <label className="flex cursor-pointer items-center justify-center rounded-xl border border-dashed border-gray-300 px-4 py-3 text-sm font-semibold text-gray-600"><input type="file" accept=".pdf,.png,.jpg,.jpeg,.gif,.tif,.tiff,.bmp,.webp,.xls,.xlsx,.txt,.csv,.json,.xml,application/pdf,image/*" className="sr-only" disabled={extractingDocument} onChange={(e) => void handleDocumentAttachment(e.target.files?.[0])} />{extractingDocument ? "문서 분석 중…" : attachmentName ? `${attachmentName} 분석 완료` : "송장·발주서·엑셀·이미지 불러오기"}</label>
            {documentResult && <p className="rounded-xl bg-emerald-50 p-3 text-xs font-bold text-emerald-800">문서 분석 완료 · 필드 {documentResult.formFieldCount}개 · 표 {documentResult.tableCount}개</p>}
          </div></details>
          {error && <p className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-600">{error}</p>}
        </main>
        <footer className="border-t border-[#e8edf5] bg-white px-5 pb-7 pt-4"><Button type="submit" fullWidth disabled={submitting || extractingDocument}>{submitting ? "등록 중…" : "AI 화물 분석 시작"}</Button></footer>
      </form>
    </div>
  );

  /* Legacy layout retained temporarily for history reference.
  return (
    <div className="flex min-h-full flex-col">
      <CargoWizardHeader step="register" onBack={() => navigate("/cargo/new")} />

      <form className="flex flex-1 flex-col gap-5 px-5 py-5" onSubmit={handleSubmit}>
        <section>
          <h2 className="text-xl font-black leading-7 text-gray-950">어떤 화물을 보내시나요?</h2>
          <p className="mt-1 text-sm text-gray-500">상품명만 넣으면 나머지는 AI가 채웁니다.</p>
        </section>
        <div className="flex items-center justify-between rounded-xl bg-brand-50 px-3 py-2.5 text-sm">
          <span className="font-bold text-brand-800">{serviceMode === "CO_LOAD" ? "🤝 공동화물" : "📦 개별운송"}</span>
          <button type="button" className="text-xs font-semibold text-brand-700" onClick={() => navigate("/cargo/new")}>운송 방식 변경</button>
        </div>
        <section>
          <h2 className="mb-3 text-sm font-bold text-gray-900">경로 및 일정</h2>
          <div className="flex flex-col gap-3">
            <Field label="출발지">
              <Input value={originStation} onChange={(e) => { setOriginStation(e.target.value); setOutOfCoverage((v) => ({ ...v, origin: false })); }} onBlur={() => checkStation("origin", originStation)} required />
            </Field>
            {mappingHint.origin && <p className="-mt-2 text-xs text-brand-700">{mappingHint.origin}</p>}
            <Field label="도착지">
              <Input value={destinationStation} onChange={(e) => { setDestinationStation(e.target.value); setOutOfCoverage((v) => ({ ...v, destination: false })); }} onBlur={() => checkStation("destination", destinationStation)} required />
            </Field>
            {mappingHint.destination && <p className="-mt-2 text-xs text-brand-700">{mappingHint.destination}</p>}
            <Field label="출발 희망일">
              <Input type="date" value={desiredDate} onChange={(e) => setDesiredDate(e.target.value)} required />
            </Field>
            <Field label="화물가액 (선택, 원)">
              <Input
                type="text"
                inputMode="numeric"
                placeholder="예) 30,000,000"
                value={declaredValueKrw ? Number(declaredValueKrw).toLocaleString("ko-KR") : ""}
                onChange={(e) => setDeclaredValueKrw(e.target.value.replace(/\D/g, ""))}
              />
            </Field>
            {declaredValueKrw && (
              <p className="-mt-2 text-xs font-bold text-brand-700">
                약 {formatKoreanAmount(Number(declaredValueKrw))}
              </p>
            )}
            <p className="-mt-2 text-xs text-gray-400">신고하면 적재보험료 산정에 반영돼요. 입력하지 않으면 보험료는 0원이에요.</p>
          </div>
        </section>

        {(outOfCoverage.origin || outOfCoverage.destination) && (
          <section className="rounded-2xl border border-amber-300 bg-amber-50 p-4">
            <p className="font-black text-amber-900">철도 화물역 지원 권역 밖이에요</p>
            <p className="mt-1 text-sm leading-6 text-amber-800">
              해당 구간은 100% 트럭 직배송이 더 적합합니다. 입력한 주소를 유지한 채 개별 도로운송 견적으로 전환할 수 있어요.
            </p>
            <button
              type="button"
              className="mt-3 w-full rounded-xl bg-amber-600 py-2.5 text-sm font-bold text-white"
              onClick={() => { setServiceMode("INDIVIDUAL"); setError(null); }}
            >
              {serviceMode === "INDIVIDUAL" ? "✓ 트럭 직배송 Plan B 선택됨" : "트럭 직배송 견적으로 전환"}
            </button>
          </section>
        )}

        <section>
          <h2 className="mb-2 text-sm font-bold text-gray-900">화물 유형</h2>
          <div className="grid grid-cols-3 gap-2">
            {CARGO_TYPES.map((type) => (
              <ChipButton key={type.key} active={cargoType === type.key} onClick={() => setCargoType(type.key)}>
                {type.label}
              </ChipButton>
            ))}
          </div>
        </section>

        {cargoType === "FROZEN_FRESH" && (
          <section>
            <h2 className="mb-2 text-sm font-bold text-gray-900">온도 관리 구분</h2>
            <div className="grid grid-cols-4 gap-2">
              {TEMP_OPTIONS.map((option) => (
                <ChipButton key={option.key} active={tempOption === option.key} onClick={() => setTempOption(option.key)}>
                  {option.label}
                </ChipButton>
              ))}
            </div>
          </section>
        )}

        {showFreezeWarning && (
          <div className="rounded-xl border border-rose-200 bg-rose-50 p-3.5 text-sm text-rose-700">
            <p className="font-bold">포장 주의 — 냉동 -18~-20°C</p>
            <p className="mt-1">이중 단열 포장과 아이스팩 배치가 필수입니다. 문 개방이 길어질 수 있어 꼼꼼하게 포장해주세요.</p>
          </div>
        )}

        <section className="flex-1">
          <h2 className="mb-2 text-sm font-bold text-gray-900">화물 정보 입력</h2>
          <div className="mb-3 grid grid-cols-2 rounded-xl bg-gray-100 p-1">
            <button
              type="button"
              onClick={() => setInputMode("NATURAL")}
              className={`rounded-lg py-2 text-sm font-bold ${inputMode === "NATURAL" ? "bg-white text-brand-700 shadow-sm" : "text-gray-500"}`}
            >
              ✨ 자연어로 입력
            </button>
            <button
              type="button"
              onClick={() => setInputMode("STRUCTURED")}
              className={`rounded-lg py-2 text-sm font-bold ${inputMode === "STRUCTURED" ? "bg-white text-brand-700 shadow-sm" : "text-gray-500"}`}
            >
              📝 항목별 입력
            </button>
          </div>

          {inputMode === "NATURAL" ? (
            <>
              <TextArea
                rows={5}
                value={rawInput}
                onChange={(e) => setRawInput(e.target.value)}
                placeholder="예) 냉동 만두 파렛트 8개, 200kg 정도예요. 목요일 밤 출발 희망하고 -18도 유지 필요합니다."
              />
              <div className="mt-2 rounded-xl bg-amber-50 px-3 py-2 text-xs leading-5 text-amber-800">
                AI가 품목·중량·부피·포장을 추출해요. 빠진 값은 다음 화면에서 바로 보정하며, 불안하면 항목별 입력을 선택하세요.
              </div>
            </>
          ) : (
            <div className="grid grid-cols-2 gap-3">
              <div className="col-span-2">
                <Field label="품목명 *">
                  <Input value={structured.cargoName} onChange={(e) => setStructured((s) => ({ ...s, cargoName: e.target.value }))} placeholder="예) 냉동 만두" />
                </Field>
              </div>
              <Field label="중량(kg) *">
                <Input type="number" min={0.1} step="0.1" value={structured.weightKg} onChange={(e) => setStructured((s) => ({ ...s, weightKg: e.target.value }))} />
              </Field>
              <Field label="부피(CBM)">
                <Input type="number" min={0} step="0.1" value={structured.volumeCbm} onChange={(e) => setStructured((s) => ({ ...s, volumeCbm: e.target.value }))} />
              </Field>
              <Field label="포장 단위">
                <select className="h-12 w-full rounded-xl border border-gray-200 bg-white px-3 text-sm" value={structured.packageUnit} onChange={(e) => setStructured((s) => ({ ...s, packageUnit: e.target.value }))}>
                  <option>파렛트</option><option>박스</option><option>드럼</option><option>톤백</option><option>개별포장</option>
                </select>
              </Field>
              <Field label="포장 수량">
                <Input type="number" min={0} value={structured.packageCount} onChange={(e) => setStructured((s) => ({ ...s, packageCount: e.target.value }))} />
              </Field>
              <div className="col-span-2">
                <Field label="취급·납기 요구사항">
                  <TextArea rows={3} value={structured.handlingNote} onChange={(e) => setStructured((s) => ({ ...s, handlingNote: e.target.value }))} placeholder="예) 충격 주의, 야간 출발 희망" />
                </Field>
              </div>
            </div>
          )}

          <label className="mt-3 flex cursor-pointer items-center justify-center rounded-xl border border-dashed border-gray-300 bg-white px-4 py-3 text-sm font-semibold text-gray-600">
            <input
              type="file"
              accept=".pdf,.png,.jpg,.jpeg,.gif,.tif,.tiff,.bmp,.webp,.xls,.xlsx,.txt,.csv,.json,.xml,application/pdf,image/*,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
              className="sr-only"
              disabled={extractingDocument}
              onChange={(e) => void handleDocumentAttachment(e.target.files?.[0])}
            />
            {extractingDocument
              ? "🔍 Google 문서 분석 중…"
              : attachmentName
                ? `📎 ${attachmentName} 분석 완료`
                : "📎 송장·발주서·엑셀·이미지 불러오기"}
          </label>
          <p className="mt-1 text-xs text-gray-400">PDF·PNG·JPG·TIFF·XLS·XLSX 등, 최대 20MB</p>
          {documentResult && (
            <div className="mt-2 rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs text-emerald-800">
              <p className="font-bold">
                {documentResult.provider === "GOOGLE_DOCUMENT_AI_FORM_PARSER" ? "Google Document AI OCR" : "엑셀/텍스트 구조 분석"} 완료
              </p>
              <p className="mt-0.5">
                {documentResult.pageOrSheetCount}페이지/시트 · 필드 {documentResult.formFieldCount}개 · 표 {documentResult.tableCount}개
              </p>
              {documentResult.warnings.map((warning) => (
                <p key={warning} className="mt-1 text-amber-700">확인 필요: {warning}</p>
              ))}
              <p className="mt-1 text-emerald-700">추출 결과가 위 요구사항 입력란에 추가됐어요. 분석 전에 내용을 확인해주세요.</p>
            </div>
          )}
        </section>

        {error && <p className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-600">{error}</p>}

        <Button type="submit" fullWidth disabled={submitting || extractingDocument}>
          {submitting ? "등록 중…" : "AI 분석 시작하기"}
        </Button>
      </form>
    </div>
  ); */
}

/* function ChipButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-xl border px-2 py-2.5 text-sm font-semibold transition-colors ${
        active ? "border-brand-700 bg-brand-50 text-brand-700" : "border-gray-200 bg-white text-gray-600"
      }`}
    >
      {children}
    </button>
  );
} */

function DesignField({ label, children }: { label: string; children: React.ReactNode }) { return <label><span className="mb-2 block text-[12px] font-black text-[#7b879b]">{label}</span>{children}</label>; }
function DesignDate({ value, active = false, onClick }: { value: string; active?: boolean; onClick: () => void }) { const date = new Date(`${value}T00:00:00`); return <button type="button" onClick={onClick} className={`h-[54px] rounded-[18px] text-[12px] font-black ${active ? "border-2 border-brand-600 bg-brand-50 text-brand-700" : "bg-[#f2f5fb] text-[#68758b]"}`}>{date.toLocaleDateString("ko-KR", { month: "long", day: "numeric", weekday: "short" })}</button>; }
