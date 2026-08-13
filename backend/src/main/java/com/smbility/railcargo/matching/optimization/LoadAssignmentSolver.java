package com.smbility.railcargo.matching.optimization;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;
import com.google.ortools.sat.Literal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 공동화물(item)을 화차(bin)에 배정하는 일반화 배정 문제(Generalized Assignment Problem)를
 * Google OR-Tools CP-SAT로 푼다. 기획안 보충 "2) 공동화 군집 생성 - 3단계: CP-SAT 최적화" 대응.
 *
 * <p>목적함수는 (1) 전체 배정 중량 최대화를 최우선으로 하고, 그 다음으로 (2) 화차별 여유공간이
 * 작아지도록(=적재율이 높아지도록) 배정하는 것을 2순위 목표로 둔다. JPA 엔티티에 의존하지 않는
 * 순수 값 객체만 사용해서 Spring 컨텍스트 없이도 단위 테스트할 수 있게 했다.
 */
public class LoadAssignmentSolver {

    static {
        Loader.loadNativeLibraries();
    }

    /** kg을 centikg(0.01kg) 정수 단위로 스케일링해 CP-SAT의 정수 도메인에 맞춘다. */
    private static final long SCALE = 100L;
    /** 1순위(총 배정중량)가 2순위(적재 타이트함)보다 항상 우선하도록 만드는 가중치. */
    private static final long PRIMARY_WEIGHT_MULTIPLIER = 10_000_000L;
    private static final double SOLVER_TIME_LIMIT_SECONDS = 5.0;

    public record LoadItem(String id, BigDecimal weightKg, boolean hazardous) {
    }

    public record BinCapacity(String id, BigDecimal remainingWeightKg, boolean hazardousAllowed) {
    }

    /** @return itemId -> binId 매핑 (배정에 실패한 item은 결과에 포함되지 않는다) */
    public Map<String, String> solve(List<LoadItem> items, List<BinCapacity> bins) {
        if (items.isEmpty() || bins.isEmpty()) {
            return Map.of();
        }

        CpModel model = new CpModel();
        int itemCount = items.size();
        int binCount = bins.size();
        BoolVar[][] assignmentVars = new BoolVar[itemCount][binCount];

        for (int i = 0; i < itemCount; i++) {
            for (int j = 0; j < binCount; j++) {
                if (isCompatible(items.get(i), bins.get(j))) {
                    assignmentVars[i][j] = model.newBoolVar("x_i%d_b%d".formatted(i, j));
                }
            }
        }

        addAtMostOneBinPerItem(model, assignmentVars, itemCount, binCount);
        addCapacityConstraints(model, assignmentVars, items, bins, itemCount, binCount);
        model.maximize(buildObjective(assignmentVars, items, bins, itemCount, binCount));

        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(SOLVER_TIME_LIMIT_SECONDS);
        CpSolverStatus status = solver.solve(model);

        return extractAssignment(solver, status, assignmentVars, items, bins, itemCount, binCount);
    }

    private boolean isCompatible(LoadItem item, BinCapacity bin) {
        if (item.hazardous() && !bin.hazardousAllowed()) {
            return false;
        }
        return bin.remainingWeightKg().compareTo(item.weightKg()) >= 0;
    }

    private void addAtMostOneBinPerItem(CpModel model, BoolVar[][] assignmentVars, int itemCount, int binCount) {
        for (int i = 0; i < itemCount; i++) {
            List<Literal> literals = new ArrayList<>();
            for (int j = 0; j < binCount; j++) {
                if (assignmentVars[i][j] != null) {
                    literals.add(assignmentVars[i][j]);
                }
            }
            if (!literals.isEmpty()) {
                model.addAtMostOne(literals);
            }
        }
    }

    private void addCapacityConstraints(CpModel model, BoolVar[][] assignmentVars, List<LoadItem> items,
                                         List<BinCapacity> bins, int itemCount, int binCount) {
        for (int j = 0; j < binCount; j++) {
            LinearExprBuilder capacityExpr = LinearExpr.newBuilder();
            for (int i = 0; i < itemCount; i++) {
                if (assignmentVars[i][j] != null) {
                    capacityExpr.addTerm(assignmentVars[i][j], scale(items.get(i).weightKg()));
                }
            }
            model.addLessOrEqual(capacityExpr, scale(bins.get(j).remainingWeightKg()));
        }
    }

    private LinearExprBuilder buildObjective(BoolVar[][] assignmentVars, List<LoadItem> items, List<BinCapacity> bins,
                                              int itemCount, int binCount) {
        LinearExprBuilder objective = LinearExpr.newBuilder();
        for (int i = 0; i < itemCount; i++) {
            long weightScaled = scale(items.get(i).weightKg());
            for (int j = 0; j < binCount; j++) {
                if (assignmentVars[i][j] != null) {
                    long leftover = scale(bins.get(j).remainingWeightKg()) - weightScaled;
                    long coefficient = weightScaled * PRIMARY_WEIGHT_MULTIPLIER - leftover;
                    objective.addTerm(assignmentVars[i][j], coefficient);
                }
            }
        }
        return objective;
    }

    private Map<String, String> extractAssignment(CpSolver solver, CpSolverStatus status, BoolVar[][] assignmentVars,
                                                    List<LoadItem> items, List<BinCapacity> bins,
                                                    int itemCount, int binCount) {
        Map<String, String> assignment = new LinkedHashMap<>();
        if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE) {
            return assignment;
        }
        for (int i = 0; i < itemCount; i++) {
            for (int j = 0; j < binCount; j++) {
                if (assignmentVars[i][j] != null && solver.booleanValue(assignmentVars[i][j])) {
                    assignment.put(items.get(i).id(), bins.get(j).id());
                }
            }
        }
        return assignment;
    }

    private long scale(BigDecimal value) {
        return value.multiply(BigDecimal.valueOf(SCALE)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
