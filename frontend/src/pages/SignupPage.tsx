import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login, signup } from "../api/auth";
import { registerShipper } from "../api/shipper";
import { ApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { Button } from "../components/ui/Button";
import { Field, Input } from "../components/ui/Input";

/**
 * 디자인에는 없던 화면이지만, 로그인만으로는 새 계정으로 앱을 써볼 수 없어 추가했다.
 * 계정 생성(SHIPPER) → 사업자 정보 등록까지 한 흐름으로 처리한다.
 */
export function SignupPage() {
  const navigate = useNavigate();
  const auth = useAuth();
  const [step, setStep] = useState<1 | 2>(1);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [businessNumber, setBusinessNumber] = useState("");
  const [companyName, setCompanyName] = useState("");
  const [managerName, setManagerName] = useState("");
  const [phone, setPhone] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleAccountSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await signup(email, password, "SHIPPER");
      const token = await login(email, password);
      await auth.login(token.accessToken);
      setStep(2);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "회원가입에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleBusinessSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await registerShipper({ businessNumber, companyName, managerName, phone });
      await auth.refreshShipper();
      navigate("/home");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "사업자 등록에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="app-shell bg-white">
      <div className="bg-gradient-to-br from-brand-800 to-brand-600 px-6 pb-16 pt-14 text-white">
        <h1 className="text-2xl font-black">KOLOG</h1>
        <p className="mt-1 text-sm text-white/70">AI 철도 공동화물 거래소</p>
      </div>

      <div className="-mt-8 flex-1 rounded-t-3xl bg-white px-6 pb-8 pt-7">
        <h2 className="text-xl font-bold text-gray-900">{step === 1 ? "회원가입" : "사업자 정보 등록"}</h2>
        <p className="mt-1 text-sm text-gray-500">
          {step === 1 ? "계정으로 사용할 정보를 입력해주세요" : "화물 등록을 위해 사업자 정보가 필요해요"}
        </p>

        {step === 1 ? (
          <form className="mt-6 flex flex-col gap-4" onSubmit={handleAccountSubmit}>
            <Field label="이메일">
              <Input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} placeholder="example@kolog.kr" />
            </Field>
            <Field label="비밀번호 (8자 이상)">
              <Input type="password" required minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} />
            </Field>
            {error && <p className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-600">{error}</p>}
            <Button type="submit" fullWidth disabled={submitting} className="mt-2">
              {submitting ? "처리 중…" : "다음"}
            </Button>
          </form>
        ) : (
          <form className="mt-6 flex flex-col gap-4" onSubmit={handleBusinessSubmit}>
            <Field label="사업자등록번호">
              <Input required value={businessNumber} onChange={(e) => setBusinessNumber(e.target.value)} placeholder="123-45-67890" />
            </Field>
            <Field label="상호명">
              <Input required value={companyName} onChange={(e) => setCompanyName(e.target.value)} placeholder="(주)한국물류" />
            </Field>
            <Field label="담당자명">
              <Input required value={managerName} onChange={(e) => setManagerName(e.target.value)} placeholder="김물류" />
            </Field>
            <Field label="연락처">
              <Input required value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="010-1234-5678" />
            </Field>
            {error && <p className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-600">{error}</p>}
            <Button type="submit" fullWidth disabled={submitting} className="mt-2">
              {submitting ? "등록 중…" : "등록 완료"}
            </Button>
          </form>
        )}

        {step === 1 && (
          <p className="mt-6 text-center text-sm text-gray-400">
            이미 계정이 있으신가요? <Link to="/login" className="font-semibold text-brand-700">로그인</Link>
          </p>
        )}
      </div>
    </div>
  );
}
