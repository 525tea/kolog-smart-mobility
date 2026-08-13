package com.smbility.railcargo.train.service;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import com.smbility.railcargo.train.domain.Train;
import com.smbility.railcargo.train.domain.TrainStatus;
import com.smbility.railcargo.train.domain.Wagon;
import com.smbility.railcargo.train.domain.WagonType;
import com.smbility.railcargo.train.dto.TrainRegisterRequest;
import com.smbility.railcargo.train.dto.TrainResponse;
import com.smbility.railcargo.train.dto.WagonRegisterRequest;
import com.smbility.railcargo.train.dto.WagonResponse;
import com.smbility.railcargo.train.repository.TrainRepository;
import com.smbility.railcargo.train.repository.WagonRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainService {

    private final TrainRepository trainRepository;
    private final WagonRepository wagonRepository;

    @Transactional
    public TrainResponse registerTrain(TrainRegisterRequest request) {
        Train train = Train.of(request.trainNumber(), request.originStation(), request.destinationStation(),
                request.departureAt(), request.arrivalAt(), request.reservationDeadline());
        return toResponse(trainRepository.save(train));
    }

    @Transactional
    public WagonResponse registerWagon(Long trainId, WagonRegisterRequest request) {
        Train train = getTrainEntity(trainId);
        Wagon wagon = Wagon.of(train, request.wagonNumber(), request.wagonType(), request.maxWeightKg(),
                request.hazardousAllowed());
        return WagonResponse.from(wagonRepository.save(wagon));
    }

    public List<TrainResponse> getUpcomingTrains() {
        return trainRepository.findAllByStatusAndDepartureAtAfterOrderByDepartureAtAsc(
                        TrainStatus.SCHEDULED, LocalDateTime.now()).stream()
                .map(this::toResponse)
                .toList();
    }

    public TrainResponse getTrain(Long trainId) {
        return toResponse(getTrainEntity(trainId));
    }

    public Train getTrainEntity(Long trainId) {
        return trainRepository.findById(trainId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "열차를 찾을 수 없습니다."));
    }

    public Wagon getWagonEntity(Long wagonId) {
        return wagonRepository.findById(wagonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "화차를 찾을 수 없습니다."));
    }

    public List<Wagon> findAvailableWagons(String originStation, String destinationStation,
                                            java.math.BigDecimal minRemainingWeightKg) {
        return wagonRepository
                .findAllByTrain_OriginStationAndTrain_DestinationStationAndRemainingWeightKgGreaterThanEqual(
                        originStation, destinationStation, minRemainingWeightKg);
    }

    public List<Wagon> findEligibleWagons(String originStation, String destinationStation,
                                           LocalDate desiredDate, TemperatureCondition temperatureCondition,
                                           boolean hazardous, BigDecimal minRemainingWeightKg) {
        LocalDateTime now = LocalDateTime.now();
        return wagonRepository.findEligibleForRouteAndDate(
                        originStation, destinationStation, desiredDate.atStartOfDay(),
                        desiredDate.plusDays(1).atStartOfDay(), now, minRemainingWeightKg).stream()
                .filter(wagon -> temperatureCondition == TemperatureCondition.ROOM
                        || wagon.getWagonType() == WagonType.REFRIGERATED)
                .filter(wagon -> !hazardous || wagon.isHazardousAllowed())
                .toList();
    }

    private TrainResponse toResponse(Train train) {
        List<WagonResponse> wagons = wagonRepository.findAllByTrainId(train.getId()).stream()
                .map(WagonResponse::from)
                .toList();
        return TrainResponse.from(train, wagons);
    }
}
