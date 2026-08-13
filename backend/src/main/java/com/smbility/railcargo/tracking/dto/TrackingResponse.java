package com.smbility.railcargo.tracking.dto;

import com.smbility.railcargo.tracking.domain.TransportPhase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 건의 실시간(시뮬레이션) 위치 정보.
 *
 * <p>실제 화차에 부착된 GPS 단말 연동이 없으므로, 열차 시간표(출발/도착 시각)를 기준으로
 * 경과 비율만큼 출발역-도착역 사이를 선형보간한 좌표를 계산해서 보여준다({@code isSimulated=true}).
 */
public record TrackingResponse(
        Long reservationId,
        TransportPhase phase,
        BigDecimal progressPercent,
        double currentLatitude,
        double currentLongitude,
        double originLatitude,
        double originLongitude,
        double destinationLatitude,
        double destinationLongitude,
        String originStation,
        String destinationStation,
        String currentSegment,
        LocalDateTime departureAt,
        LocalDateTime arrivalAt,
        LocalDateTime lastUpdatedAt,
        int refreshAfterSeconds,
        List<TrackingWaypoint> route,
        boolean isSimulated
) {
}
