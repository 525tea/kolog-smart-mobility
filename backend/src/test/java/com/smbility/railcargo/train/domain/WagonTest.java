package com.smbility.railcargo.train.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smbility.railcargo.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class WagonTest {

    private Train sampleTrain() {
        return Train.of("KTX-1", "천안", "서울",
                LocalDateTime.now().plusHours(8), LocalDateTime.now().plusHours(9), LocalDateTime.now().plusHours(6));
    }

    @Test
    void 잔여용량_범위_내에서는_적재가_가능하다() {
        Wagon wagon = Wagon.of(sampleTrain(), "W-1", WagonType.CONTAINER, BigDecimal.valueOf(1000), false);

        assertTrue(wagon.canAccommodate(BigDecimal.valueOf(500), false));
    }

    @Test
    void 잔여용량을_초과하면_적재할_수_없다() {
        Wagon wagon = Wagon.of(sampleTrain(), "W-1", WagonType.CONTAINER, BigDecimal.valueOf(1000), false);

        assertFalse(wagon.canAccommodate(BigDecimal.valueOf(1200), false));
    }

    @Test
    void 위험물_비허용_화차는_위험물을_적재할_수_없다() {
        Wagon wagon = Wagon.of(sampleTrain(), "W-1", WagonType.CONTAINER, BigDecimal.valueOf(1000), false);

        assertFalse(wagon.canAccommodate(BigDecimal.valueOf(100), true));
    }

    @Test
    void allocate는_잔여용량을_초과하면_예외를_던진다() {
        Wagon wagon = Wagon.of(sampleTrain(), "W-1", WagonType.CONTAINER, BigDecimal.valueOf(1000), false);

        assertThrows(BusinessException.class, () -> wagon.allocate(BigDecimal.valueOf(1500)));
    }

    @Test
    void allocate_후_적재율이_올바르게_계산된다() {
        Wagon wagon = Wagon.of(sampleTrain(), "W-1", WagonType.CONTAINER, BigDecimal.valueOf(1000), false);

        wagon.allocate(BigDecimal.valueOf(800));

        assertEquals(BigDecimal.valueOf(200).setScale(2), wagon.getRemainingWeightKg().setScale(2));
        assertEquals(0, BigDecimal.valueOf(80.0).compareTo(wagon.getLoadFactorPercent()));
    }

    @Test
    void release는_최대중량을_넘지_않도록_보정한다() {
        Wagon wagon = Wagon.of(sampleTrain(), "W-1", WagonType.CONTAINER, BigDecimal.valueOf(1000), false);
        wagon.allocate(BigDecimal.valueOf(300));

        wagon.release(BigDecimal.valueOf(1000));

        assertEquals(0, BigDecimal.valueOf(1000).compareTo(wagon.getRemainingWeightKg()));
    }
}
