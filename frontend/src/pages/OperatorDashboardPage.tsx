import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { approveConsolidation, getOperatorDashboard, rejectConsolidation, runLoadOptimization } from "../api/operator";
import { ApiError } from "../api/client";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { Card } from "../components/ui/Card";
import { ProgressBar } from "../components/ui/ProgressBar";
import { useAuth } from "../context/AuthContext";
import type { OperatorDashboardResponse } from "../types";

const STATUS_LABEL: Record<string, string> = {
  READY_FOR_MATCHING: "화차 배정 대기",
  MATCHED: "배정 완료",
  PENDING_APPROVAL: "승인 대기",
};

export function OperatorDashboardPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [data, setData] = useState<OperatorDashboardResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [workingId, setWorkingId] = useState<number | null>(null);
  const [memoByGroup, setMemoByGroup] = useState<Record<number, string>>({});

  const load = useCallback(async () => {
    try {
      setData(await getOperatorDashboard());
      setError(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "운영 현황을 불러오지 못했어요.");
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function decide(groupId: number, decision: "approve" | "reject") {
    const memo = memoByGroup[groupId]?.trim() || undefined;
    if (decision === "reject" && !memo) {
      setError("반려하려면 검토 메모를 입력해주세요.");
      return;
    }
    setWorkingId(groupId);
    try {
      if (decision === "approve") await approveConsolidation(groupId, memo);
      else await rejectConsolidation(groupId, memo);
      await load();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "처리하지 못했어요.");
    } finally {
      setWorkingId(null);
    }
  }

  async function optimize() {
    setWorkingId(-1);
    try {
      const results = await runLoadOptimization();
      await load();
      setError(results.length === 0 ? "새로 배정된 공동화물이 없습니다." : `${results.length}건의 화차 배정을 완료했습니다.`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "적재 최적화를 실행하지 못했어요.");
    } finally {
      setWorkingId(null);
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 px-5 py-6">
      <div className="mx-auto max-w-5xl">
        <header className="flex items-center justify-between rounded-2xl bg-brand-800 px-5 py-5 text-white">
          <div>
            <p className="text-xs font-semibold text-white/70">KOLOG OPERATOR</p>
            <h1 className="mt-1 text-2xl font-black">철도 공동화물 운영</h1>
          </div>
          <button
            className="rounded-lg border border-white/30 px-3 py-2 text-sm"
            onClick={() => {
              logout();
              navigate("/login");
            }}
          >
            로그아웃
          </button>
        </header>

        <div className="mt-5 grid gap-4 sm:grid-cols-3">
          <Summary label="운행 예정 열차" value={`${data?.upcomingTrains.length ?? 0}편`} />
          <Summary label="검토 필요" value={`${data?.reviewQueue.length ?? 0}건`} />
          <Card className="flex items-center">
            <Button fullWidth disabled={workingId !== null} onClick={optimize}>
              {workingId === -1 ? "계산 중…" : "적재 최적화 재실행"}
            </Button>
          </Card>
        </div>

        {error && <p className="mt-4 rounded-xl bg-amber-50 px-4 py-3 text-sm text-amber-700">{error}</p>}

        <section className="mt-7">
          <h2 className="mb-3 text-lg font-black text-gray-900">승인·배정 큐</h2>
          <div className="grid gap-3 lg:grid-cols-2">
            {data?.reviewQueue.map((group) => (
              <Card key={group.id}>
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-bold text-gray-900">{group.originStation} → {group.destinationStation}</p>
                    <p className="mt-1 text-xs text-gray-400">#{group.id} · 운행일 {group.desiredDate} · {group.participantCount}개사</p>
                  </div>
                  <Badge tone={group.status === "PENDING_APPROVAL" ? "amber" : "blue"}>
                    {STATUS_LABEL[group.status] ?? group.status}
                  </Badge>
                </div>
                <div className="mt-4">
                  <ProgressBar percent={group.recruitmentRatePercent} />
                  <div className="mt-1 flex justify-between text-xs text-gray-400">
                    <span>{group.recruitedWeightKg}kg / {group.targetWeightKg}kg</span>
                    <span>화차 {group.matchedWagonId ?? "미배정"}</span>
                  </div>
                </div>
                {group.status === "PENDING_APPROVAL" && (
                  <div className="mt-4">
                    <label className="text-xs font-semibold text-gray-500" htmlFor={`review-memo-${group.id}`}>검토 메모</label>
                    <input
                      id={`review-memo-${group.id}`}
                      value={memoByGroup[group.id] ?? ""}
                      onChange={(event) => setMemoByGroup((current) => ({ ...current, [group.id]: event.target.value }))}
                      placeholder="승인 메모 또는 반려 사유"
                      className="mt-1 h-10 w-full rounded-xl border border-gray-200 px-3 text-sm outline-none focus:border-brand-600"
                    />
                    <div className="mt-2 flex gap-2">
                      <Button variant="outline" fullWidth disabled={workingId !== null} onClick={() => decide(group.id, "reject")}>반려</Button>
                      <Button fullWidth disabled={workingId !== null} onClick={() => decide(group.id, "approve")}>승인</Button>
                    </div>
                  </div>
                )}
              </Card>
            ))}
            {data && data.reviewQueue.length === 0 && <p className="py-10 text-center text-sm text-gray-400">검토할 공동화물이 없어요.</p>}
          </div>
        </section>

        <section className="mt-7">
          <h2 className="mb-3 text-lg font-black text-gray-900">운행 예정 열차</h2>
          <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white">
            {data?.upcomingTrains.map((train) => (
              <div key={train.id} className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-100 px-4 py-3 last:border-0">
                <div>
                  <p className="font-bold text-gray-900">{train.trainNumber} · {train.originStation} → {train.destinationStation}</p>
                  <p className="text-xs text-gray-400">{new Date(train.departureAt).toLocaleString("ko-KR")}</p>
                </div>
                <span className="text-sm font-semibold text-brand-700">화차 {train.wagons.length}량</span>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}

function Summary({ label, value }: { label: string; value: string }) {
  return <Card><p className="text-sm text-gray-500">{label}</p><p className="mt-1 text-2xl font-black text-brand-800">{value}</p></Card>;
}
