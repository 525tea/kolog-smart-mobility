package com.smbility.railcargo.tracking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.reservation.domain.Reservation;
import com.smbility.railcargo.tracking.domain.TransportPhase;
import com.smbility.railcargo.tracking.dto.TrackingResponse;
import com.smbility.railcargo.train.domain.Train;
import com.smbility.railcargo.train.domain.Wagon;
import com.smbility.railcargo.train.domain.WagonType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TrackingSimulationServiceTest {

    private final TrackingSimulationService service = new TrackingSimulationService();

    private Reservation reservationWithSchedule(LocalDateTime departureAt, LocalDateTime arrivalAt) {
        Train train = Train.of("KTX-C101", "천안", "서울", departureAt, arrivalAt, departureAt.minusHours(2));
        Wagon wagon = Wagon.of(train, "W-1", WagonType.CONTAINER, BigDecimal.valueOf(1000), false);
        ConsolidatedCargo group = ConsolidatedCargo.open("천안", "서울", TemperatureCondition.ROOM, false,
                BigDecimal.valueOf(500), departureAt.minusHours(1));
        Reservation reservation = Reservation.confirm(group, wagon, BigDecimal.valueOf(100_000));
        ReflectionTestUtils.setField(reservation, "id", 1L);
        return reservation;
    }

    @Test
    void 출발_전이면_출발역_좌표에서_대기중으로_표시된다() {
        LocalDateTime departureAt = LocalDateTime.now().plusHours(2);
        LocalDateTime arrivalAt = departureAt.plusHours(1);
        Reservation reservation = reservationWithSchedule(departureAt, arrivalAt);

        TrackingResponse tracking = service.simulate(reservation);

        assertThat(tracking.phase()).isEqualTo(TransportPhase.BEFORE_DEPARTURE);
        assertThat(tracking.progressPercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(tracking.isSimulated()).isTrue();
    }

    @Test
    void 운행중이면_경과비율만큼_두_역_사이로_보간된_위치를_반환한다() {
        // 1시간 전 출발, 1시간 후 도착 -> 지금은 정확히 절반 지점(50%)
        LocalDateTime departureAt = LocalDateTime.now().minusHours(1);
        LocalDateTime arrivalAt = LocalDateTime.now().plusHours(1);
        Reservation reservation = reservationWithSchedule(departureAt, arrivalAt);

        TrackingResponse tracking = service.simulate(reservation);

        assertThat(tracking.phase()).isEqualTo(TransportPhase.IN_TRANSIT);
        assertThat(tracking.progressPercent().doubleValue()).isCloseTo(50.0, within(2.0));
        // 천안(36.8065) -> 서울(37.5547) 사이 어딘가여야 한다
        assertThat(tracking.currentLatitude()).isBetween(36.8065, 37.5547);
    }

    @Test
    void 도착시간이_지났으면_도착완료로_표시된다() {
        LocalDateTime departureAt = LocalDateTime.now().minusHours(3);
        LocalDateTime arrivalAt = LocalDateTime.now().minusHours(1);
        Reservation reservation = reservationWithSchedule(departureAt, arrivalAt);

        TrackingResponse tracking = service.simulate(reservation);

        assertThat(tracking.phase()).isEqualTo(TransportPhase.ARRIVED);
        assertThat(tracking.progressPercent()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(tracking.currentLatitude()).isCloseTo(37.5547, within(0.001));
    }
}
