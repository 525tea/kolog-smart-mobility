import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../api/auth";
import { ApiError } from "../api/client";
import { setRefreshToken } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { Button } from "../components/ui/Button";
import { Field, Input } from "../components/ui/Input";

export function LoginPage() {
  const navigate = useNavigate();
  const auth = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [remember, setRemember] = useState(true);

  async function performLogin(loginEmail: string, loginPassword: string) {
    setError(null);
    setSubmitting(true);
    try {
      const token = await login(loginEmail, loginPassword);
      setRefreshToken(token.refreshToken);
      const member = await auth.login(token.accessToken);
      navigate(member.role === "OPERATOR" ? "/operator" : "/home");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "로그인에 실패했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    await performLogin(email, password);
  }

  return (
    <div className="app-shell bg-white">
      <div className="bg-brand-700 px-[22px] pb-[46px] pt-6 text-white">
        <div className="mt-12 flex flex-col gap-3.5">
          <div className="flex size-[46px] items-center justify-center rounded-[14px] bg-white/20"><span className="size-[18px] rounded-md bg-white" /></div>
          <h1 className="text-[27px] font-extrabold leading-[1.3] tracking-[-.8px]">화물을 등록하면<br />철도가 함께 실어드립니다</h1>
          <p className="text-[13px] font-medium text-white/85">소량화물 공동운송 플랫폼 · 화주용</p>
        </div>
      </div>

      <div className="-mt-6 flex flex-1 flex-col rounded-t-3xl bg-white px-[22px] pb-7 pt-[26px]">
        <form className="flex flex-col gap-3" onSubmit={handleSubmit}>
          <Field label="기업 아이디 (이메일)">
            <Input
              type="email"
              required
              autoComplete="email"
              placeholder="demo@kolog.kr"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </Field>
          <Field label="비밀번호">
            <Input
              type="password"
              required
              autoComplete="current-password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </Field>

          {error && <p className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-600">{error}</p>}

          <label className="my-0.5 flex items-center gap-2 text-xs font-semibold text-[#4a5567]">
            <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} className="size-[18px] accent-[#1b5cf0]" />
            자동 로그인 · 담당자 정보 유지
          </label>

          <Button type="submit" fullWidth disabled={submitting}>
            {submitting ? "로그인 중…" : "로그인"}
          </Button>
        </form>

        <div className="mt-3 flex items-center justify-center gap-3 text-xs font-bold text-[#78859b]">
          <button type="button" onClick={() => navigate("/signup")}>회원가입</button><i className="h-3 w-px bg-[#d6ddea]" />
          <button type="button" onClick={() => navigate("/signup")}>기업정보 등록</button>
        </div>

        <button type="button" disabled={submitting} onClick={() => void performLogin("demo@kolog.kr", "demo1234")} className="mt-4 rounded-xl border border-brand-100 bg-brand-50 px-4 py-3 text-sm font-extrabold text-brand-800">심사위원용 데모 계정으로 둘러보기</button>

        <div className="mt-auto rounded-[14px] bg-brand-50 p-4">
          <p className="text-xs font-extrabold text-brand-800">사업자 인증이 필요합니다</p>
          <p className="mt-1 text-[11.5px] font-medium leading-5 text-[#4a5567]">기업명 · 담당자 · 연락처를 등록하면 운송 예약과 세금계산서 발행이 가능해집니다.</p>
        </div>
      </div>
    </div>
  );
}
