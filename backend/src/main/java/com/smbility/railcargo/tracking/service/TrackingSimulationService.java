package com.smbility.railcargo.tracking.service;

import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.reservation.domain.Reservation;
import com.smbility.railcargo.tracking.domain.TransportPhase;
import com.smbility.railcargo.tracking.dto.TrackingResponse;
import com.smbility.railcargo.tracking.dto.TrackingWaypoint;
import com.smbility.railcargo.tracking.support.StationCoordinate;
import com.smbility.railcargo.tracking.support.StationCoordinates;
import com.smbility.railcargo.train.domain.Train;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 화차에 부착된 실제 GPS 단말 연동 없이, 열차 시간표(출발/도착 시각) 기준 경과 비율로
 * 출발역-도착역 사이의 위치를 선형보간해 "실시간 위치"를 시뮬레이션한다.
 *
 * <p>기획안 보충의 실시간 위치추적 요구사항을 반영하되, 실제 GPS 데이터가 없다는 한계를
 * {@link TrackingResponse#isSimulated()}로 명시적으로 드러낸다.
 */
@Service
public class TrackingSimulationService {

    public TrackingResponse simulate(Reservation reservation) {
        Train train = reservation.getWagon().getTrain();
        ConsolidatedCargo group = reservation.getConsolidatedCargo();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureAt = train.getDepartureAt();
        LocalDateTime arrivalAt = train.getArrivalAt();

        StationCoordinate origin = StationCoordinates.of(group.getOriginStation());
        StationCoordinate destination = StationCoordinates.of(group.getDestinationStation());

        TransportPhase phase = resolvePhase(now, departureAt, arrivalAt);
        BigDecimal progressPercent = resolveProgressPercent(phase, now, departureAt, arrivalAt);
        List<TrackingWaypoint> route = buildRoute(group.getOriginStation(), group.getDestinationStation());
        RoutePosition routePosition = interpolateRoute(route, progressPercent);

        return new TrackingResponse(
                reservation.getId(),
                phase,
                progressPercent,
                routePosition.coordinate().latitude(),
                routePosition.coordinate().longitude(),
                origin.latitude(),
                origin.longitude(),
                destination.latitude(),
                destination.longitude(),
                group.getOriginStation(),
                group.getDestinationStation(),
                routePosition.segmentLabel(),
                departureAt,
                arrivalAt,
                now,
                30,
                route,
                true);
    }

    private TransportPhase resolvePhase(LocalDateTime now, LocalDateTime departureAt, LocalDateTime arrivalAt) {
        if (now.isBefore(departureAt)) {
            return TransportPhase.BEFORE_DEPARTURE;
        }
        if (!now.isBefore(arrivalAt)) {
            return TransportPhase.ARRIVED;
        }
        return TransportPhase.IN_TRANSIT;
    }

    private BigDecimal resolveProgressPercent(TransportPhase phase, LocalDateTime now,
                                               LocalDateTime departureAt, LocalDateTime arrivalAt) {
        if (phase == TransportPhase.BEFORE_DEPARTURE) {
            return BigDecimal.ZERO;
        }
        if (phase == TransportPhase.ARRIVED) {
            return BigDecimal.valueOf(100);
        }

        long totalMillis = Duration.between(departureAt, arrivalAt).toMillis();
        if (totalMillis <= 0) {
            return BigDecimal.valueOf(100);
        }
        long elapsedMillis = Duration.between(departureAt, now).toMillis();
        BigDecimal fraction = BigDecimal.valueOf(elapsedMillis)
                .divide(BigDecimal.valueOf(totalMillis), 6, RoundingMode.HALF_UP);
        return fraction.multiply(BigDecimal.valueOf(100))
                .max(BigDecimal.ZERO).min(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private List<TrackingWaypoint> buildRoute(String originName, String destinationName) {
        List<String> names = new ArrayList<>();
        names.add(originName);
        if (destinationName.contains("부산") || destinationName.contains("대구")) {
            names.add("오송");
            names.add("대전");
            names.add("동대구");
        }
        names.add(destinationName);
        return names.stream().distinct().map(name -> {
            StationCoordinate coordinate = StationCoordinates.of(name);
            return new TrackingWaypoint(name, coordinate.latitude(), coordinate.longitude());
        }).toList();
    }

    private RoutePosition interpolateRoute(List<TrackingWaypoint> route, BigDecimal progressPercent) {
        if (route.size() < 2) {
            TrackingWaypoint point = route.get(0);
            return new RoutePosition(new StationCoordinate(point.latitude(), point.longitude()), point.name());
        }
        double scaled = progressPercent.doubleValue() / 100d * (route.size() - 1);
        int segment = Math.min((int) Math.floor(scaled), route.size() - 2);
        double fraction = Math.min(1d, Math.max(0d, scaled - segment));
        TrackingWaypoint from = route.get(segment);
        TrackingWaypoint to = route.get(segment + 1);
        double lat = from.latitude() + (to.latitude() - from.latitude()) * fraction;
        double lng = from.longitude() + (to.longitude() - from.longitude()) * fraction;
        return new RoutePosition(new StationCoordinate(lat, lng), from.name() + " → " + to.name());
    }

    private record RoutePosition(StationCoordinate coordinate, String segmentLabel) {
    }
}
