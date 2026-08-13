package com.smbility.railcargo.consolidation.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ConsolidatedCargoTest {

    private ConsolidatedCargo openGroup() {
        return ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(800), LocalDateTime.now().plusDays(1));
    }

    @Test
    void 참여할수록_모집률이_올라간다() {
        ConsolidatedCargo group = openGroup();

        group.addParticipation(BigDecimal.valueOf(200));

        assertEquals(0, BigDecimal.valueOf(25.0).compareTo(group.getRecruitmentRatePercent()));
        assertFalse(group.isTargetReached());
    }

    @Test
    void 목표중량을_채우면_isTargetReached가_true다() {
        ConsolidatedCargo group = openGroup();

        group.addParticipation(BigDecimal.valueOf(620));
        group.addParticipation(BigDecimal.valueOf(200));

        assertTrue(group.isTargetReached());
    }

    @Test
    void 모집중이_아니면_참여할_수_없다() {
        ConsolidatedCargo group = openGroup();
        group.addParticipation(BigDecimal.valueOf(800));
        group.markMatched(null);
        group.markPendingApproval();

        assertThrows(BusinessException.class, () -> group.addParticipation(BigDecimal.valueOf(100)));
    }

    @Test
    void 승인은_PENDING_APPROVAL_상태에서만_가능하다() {
        ConsolidatedCargo group = openGroup();

        assertThrows(BusinessException.class, group::approve);
    }

    @Test
    void 정상적인_승인_흐름() {
        ConsolidatedCargo group = openGroup();
        group.addParticipation(BigDecimal.valueOf(800));
        group.markMatched(null);
        group.markPendingApproval();

        group.approve();
        group.confirm();

        assertEquals(ConsolidationStatus.CONFIRMED, group.getStatus());
    }

    @Test
    void 다른_구간의_화물은_호환되지_않는다() {
        ConsolidatedCargo group = openGroup();

        assertFalse(group.isCompatibleWith("부산", "서울", TemperatureCondition.ROOM, false));
    }
}
