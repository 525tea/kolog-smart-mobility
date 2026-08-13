package com.smbility.railcargo.matching.optimization;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbility.railcargo.matching.optimization.LoadAssignmentSolver.BinCapacity;
import com.smbility.railcargo.matching.optimization.LoadAssignmentSolver.LoadItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LoadAssignmentSolverTest {

    private final LoadAssignmentSolver solver = new LoadAssignmentSolver();

    @Test
    void 화차가_1대뿐이면_가장_잘_맞는_아이템을_배정한다() {
        LoadItem item = new LoadItem("group-1", BigDecimal.valueOf(500), false);
        BinCapacity bin = new BinCapacity("wagon-1", BigDecimal.valueOf(600), false);

        Map<String, String> assignment = solver.solve(List.of(item), List.of(bin));

        assertThat(assignment).containsEntry("group-1", "wagon-1");
    }

    @Test
    void 용량을_초과하는_아이템은_배정하지_않는다() {
        LoadItem item = new LoadItem("group-1", BigDecimal.valueOf(1000), false);
        BinCapacity bin = new BinCapacity("wagon-1", BigDecimal.valueOf(500), false);

        Map<String, String> assignment = solver.solve(List.of(item), List.of(bin));

        assertThat(assignment).isEmpty();
    }

    @Test
    void 위험물은_위험물_비허용_화차에_배정하지_않는다() {
        LoadItem hazardousItem = new LoadItem("group-1", BigDecimal.valueOf(300), true);
        BinCapacity nonHazardousBin = new BinCapacity("wagon-1", BigDecimal.valueOf(1000), false);

        Map<String, String> assignment = solver.solve(List.of(hazardousItem), List.of(nonHazardousBin));

        assertThat(assignment).isEmpty();
    }

    @Test
    void 여러_화차_중_가장_타이트하게_맞는_화차를_우선한다() {
        LoadItem item = new LoadItem("group-1", BigDecimal.valueOf(500), false);
        BinCapacity looseFit = new BinCapacity("wagon-loose", BigDecimal.valueOf(2000), false);
        BinCapacity tightFit = new BinCapacity("wagon-tight", BigDecimal.valueOf(550), false);

        Map<String, String> assignment = solver.solve(List.of(item), List.of(looseFit, tightFit));

        assertThat(assignment).containsEntry("group-1", "wagon-tight");
    }

    @Test
    void 여러_그룹과_화차를_동시에_배정해_전체_적재중량을_최대화한다() {
        // 화차 1대(용량 1000kg)에 그룹A(700kg)+그룹B(300kg)를 같이 넣으면 1000kg 전부 채울 수 있지만,
        // 그룹C(900kg) 하나만 넣으면 900kg밖에 못 채운다. 총 배정중량 최대화 관점에서 A+B 조합이 우수하다.
        LoadItem groupA = new LoadItem("A", BigDecimal.valueOf(700), false);
        LoadItem groupB = new LoadItem("B", BigDecimal.valueOf(300), false);
        LoadItem groupC = new LoadItem("C", BigDecimal.valueOf(900), false);
        BinCapacity wagon = new BinCapacity("wagon-1", BigDecimal.valueOf(1000), false);

        Map<String, String> assignment = solver.solve(List.of(groupA, groupB, groupC), List.of(wagon));

        assertThat(assignment).containsKeys("A", "B");
        assertThat(assignment).doesNotContainKey("C");
    }

    @Test
    void 아이템이나_화차가_없으면_빈_배정을_반환한다() {
        assertThat(solver.solve(List.of(), List.of(new BinCapacity("w", BigDecimal.TEN, false)))).isEmpty();
        assertThat(solver.solve(List.of(new LoadItem("i", BigDecimal.TEN, false)), List.of())).isEmpty();
    }
}
