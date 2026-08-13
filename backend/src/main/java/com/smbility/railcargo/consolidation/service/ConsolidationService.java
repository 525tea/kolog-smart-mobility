package com.smbility.railcargo.consolidation.service;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.CargoOrderStatus;
import com.smbility.railcargo.cargo.service.CargoService;
import com.smbility.railcargo.common.PricingPolicy;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import com.smbility.railcargo.consolidation.domain.CargoParticipation;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.consolidation.domain.ConsolidationStatus;
import com.smbility.railcargo.consolidation.domain.FailurePreference;
import com.smbility.railcargo.consolidation.dto.ConsolidationCandidateResponse;
import com.smbility.railcargo.consolidation.dto.ConsolidationDetailResponse;
import com.smbility.railcargo.consolidation.repository.CargoParticipationRepository;
import com.smbility.railcargo.consolidation.repository.ConsolidatedCargoRepository;
import com.smbility.railcargo.matching.dto.MatchPredictionResponse;
import com.smbility.railcargo.matching.optimization.LoadOptimizationService;
import com.smbility.railcargo.matching.service.RecruitmentSimulationService;
import com.smbility.railcargo.notification.domain.NotificationType;
import com.smbility.railcargo.notification.service.NotificationService;
import com.smbility.railcargo.pricing.DynamicPricingService;
import com.smbility.railcargo.pricing.PriceQuote;
import com.smbility.railcargo.train.domain.Wagon;
import com.smbility.railcargo.train.service.TrainService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsolidationService {

    /**
     * 모집 그룹이 새로 만들어질 때의 기본 목표중량(kg). 데모 시나리오(약 820kg)를 참고한 값이며,
     * 기획안 보충 "공동화 후 최소 0.8~2t 이상 성립" 범위의 하한과도 일치한다.
     */
    private static final BigDecimal DEFAULT_TARGET_WEIGHT_KG = BigDecimal.valueOf(800);

    /**
     * 공동화 참여가 의미 있으려면 최소 이 정도 중량은 되어야 한다는 하한선(kg).
     * 기획안 보충 "건당 약 100~500kg 이상" 최저 조건의 하한을 그대로 사용한다.
     * 이보다 작은 주문은 공동운송으로 묶기엔 배분비용/처리비용 대비 실익이 작다고 보고 참여를 막는다.
     */
    private static final BigDecimal MIN_PARTICIPATION_WEIGHT_KG = BigDecimal.valueOf(100);

    private final ConsolidatedCargoRepository consolidatedCargoRepository;
    private final CargoParticipationRepository cargoParticipationRepository;
    private final CargoService cargoService;
    private final LoadOptimizationService loadOptimizationService;
    private final RecruitmentSimulationService recruitmentSimulationService;
    private final DynamicPricingService dynamicPricingService;
    private final NotificationService notificationService;
    private final TrainService trainService;

    /** 화물 등록 화면에서 "공동화물 추천"을 조회할 때 사용. 없으면 새 모집 그룹을 생성해서라도 후보를 제공한다. */
    @Transactional
    public List<ConsolidationCandidateResponse> getCandidates(Long memberId, Long cargoOrderId) {
        CargoOrder order = cargoService.getOwnedEntity(memberId, cargoOrderId);
        requireAnalyzed(order);

        List<ConsolidatedCargo> compatibleGroups = consolidatedCargoRepository
                .findAllByOriginStationAndDestinationStationAndTemperatureConditionAndHazardousAndDesiredDateAndStatus(
                        order.getOriginStation(), order.getDestinationStation(), order.getTemperatureCondition(),
                        order.isHazardous(), order.getDesiredDate(), ConsolidationStatus.RECRUITING).stream()
                .filter(group -> !findEligibleWagons(group).isEmpty())
                .toList();

        if (compatibleGroups.isEmpty()) {
            List<Wagon> eligibleWagons = findEligibleWagons(order, DEFAULT_TARGET_WEIGHT_KG);
            if (eligibleWagons.isEmpty()) {
                return List.of();
            }
            compatibleGroups = List.of(openNewGroup(order, eligibleWagons.get(0)));
        }

        return compatibleGroups.stream()
                .map(group -> toCandidateResponse(group, order, findEligibleWagons(group).get(0)))
                .toList();
    }

    @Transactional
    public ConsolidationDetailResponse join(Long memberId, Long consolidatedCargoId, Long cargoOrderId,
                                             FailurePreference failurePreference) {
        return join(memberId, consolidatedCargoId, cargoOrderId, failurePreference, false, false);
    }

    @Transactional
    public ConsolidationDetailResponse join(Long memberId, Long consolidatedCargoId, Long cargoOrderId,
                                             FailurePreference failurePreference, boolean firstMile, boolean lastMile) {
        ConsolidatedCargo group = getEntity(consolidatedCargoId);
        CargoOrder order = cargoService.getOwnedEntity(memberId, cargoOrderId);

        requireAnalyzed(order);
        if (!group.isCompatibleWith(order.getOriginStation(), order.getDestinationStation(),
                order.getTemperatureCondition(), order.isHazardous(), order.getDesiredDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "화물의 운송조건이 이 공동화물과 맞지 않습니다.");
        }
        if (findEligibleWagons(group).isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "예약 가능한 열차 또는 화차가 없어 이 공동화물에 참여할 수 없습니다.");
        }
        if (order.getWeightKg().compareTo(MIN_PARTICIPATION_WEIGHT_KG) < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "공동화 최소 중량(" + MIN_PARTICIPATION_WEIGHT_KG + "kg) 미만인 화물은 참여할 수 없습니다. AI 분석 결과를 확인해주세요.");
        }

        FailurePreference preference = failurePreference != null ? failurePreference : FailurePreference.AUTO_REFUND;
        PriceQuote priceQuote = dynamicPricingService.quote(group);
        if (!priceQuote.feasible()) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, priceQuote.reason());
        }
        BigDecimal freight = applyHandlingCharges(order.getWeightKg().multiply(priceQuote.ratePerKg()), order);
        BigDecimal allocatedCost = totalPayable(freight, order);
        if (firstMile) {
            allocatedCost = allocatedCost.add(BigDecimal.valueOf(24_000));
        }
        if (lastMile) {
            allocatedCost = allocatedCost.add(BigDecimal.valueOf(19_000));
        }
        recordParticipation(group, order, allocatedCost, preference);
        order.markParticipating();

        notificationService.notify(order.getShipper().getMember().getId(), NotificationType.PAYMENT,
                "공동화물 참여 접수", group.getOriginStation() + " → " + group.getDestinationStation()
                        + " 공동화물 참여가 접수됐어요. 결제금액 " + allocatedCost.toBigInteger() + "원.");

        return toDetailResponse(group);
    }

    public ConsolidationDetailResponse getDetail(Long consolidatedCargoId) {
        return toDetailResponse(getEntity(consolidatedCargoId));
    }

    /** 화면 "잔여용량 조회/거래소"에서 특정 노선의 모집 중인 공동화물을 둘러볼 때 사용 (로그인 없이도 조회 가능). */
    public List<ConsolidationDetailResponse> browseRecruitingGroups(String originStation, String destinationStation) {
        List<ConsolidatedCargo> groups = (originStation != null && destinationStation != null)
                ? consolidatedCargoRepository.findAllByOriginStationAndDestinationStationAndStatus(
                        originStation, destinationStation, ConsolidationStatus.RECRUITING)
                : consolidatedCargoRepository.findAllByStatus(ConsolidationStatus.RECRUITING);

        return groups.stream().map(this::toDetailResponse).toList();
    }

    /** 화주가 참여한 공동화물을 최신 참여 순으로 조회한다. */
    public List<ConsolidationDetailResponse> getMyGroups(Long memberId) {
        return cargoParticipationRepository.findAllByCargoOrderShipperMemberIdOrderByIdDesc(memberId).stream()
                .filter(participation -> !participation.isSuperseded())
                .map(CargoParticipation::getConsolidatedCargo)
                .distinct()
                .map(this::toDetailResponse)
                .toList();
    }

    /**
     * 운영 대시보드의 "검토 필요" 목록: 목표중량은 채웠지만 화차 배정 대기 중(READY_FOR_MATCHING,
     * 이용 가능한 화차가 없어 최적화가 보류된 경우), 매칭 완료(MATCHED), 승인 대기(PENDING_APPROVAL) 상태를 모두 보여준다.
     */
    public List<ConsolidationDetailResponse> getReviewQueue() {
        List<ConsolidationStatus> reviewStatuses = List.of(
                ConsolidationStatus.READY_FOR_MATCHING, ConsolidationStatus.MATCHED, ConsolidationStatus.PENDING_APPROVAL);

        return reviewStatuses.stream()
                .flatMap(status -> consolidatedCargoRepository.findAllByStatus(status).stream())
                .map(this::toDetailResponse)
                .toList();
    }

    private ConsolidationDetailResponse toDetailResponse(ConsolidatedCargo group) {
        List<CargoParticipation> participations = cargoParticipationRepository.findAllByConsolidatedCargoId(group.getId());
        return ConsolidationDetailResponse.from(group, participations);
    }

    /** 이용 가능한 화차가 없어 대기 중이던 그룹을 위해, 운영자가 수동으로 최적화를 재실행할 때 사용한다. */
    @Transactional
    public List<MatchPredictionResponse> runLoadOptimization() {
        return loadOptimizationService.optimizeReadyGroups();
    }

    /**
     * 모집 마감시간이 지났는데도 목표중량을 채우지 못한 공동화물을 찾아 실패 처리한다
     * (기획안 보충 "공동화 실패시 대응/보상"). 스케줄러({@code ConsolidationFailureScheduler})가 주기적으로 호출한다.
     *
     * @return 실패 처리된 그룹 수
     */
    @Transactional
    public int handleExpiredRecruitingGroups() {
        List<ConsolidatedCargo> expiredGroups = consolidatedCargoRepository
                .findAllByStatusAndRecruitmentDeadlineBefore(ConsolidationStatus.RECRUITING, LocalDateTime.now());

        for (ConsolidatedCargo group : expiredGroups) {
            processFailedGroup(group);
            group.cancel();
        }
        return expiredGroups.size();
    }

    /**
     * 공동화가 성립하지 못한 그룹의 참여자들을 각자 선택해둔 {@link FailurePreference}에 따라 처리한다.
     * 마감 경과({@link #handleExpiredRecruitingGroups})와 코레일 반려({@code ApprovalService.reject}) 두 경로에서 호출된다.
     * 그룹 자체의 상태 전이(CANCELLED/REJECTED)는 호출자가 처리한다.
     */
    @Transactional
    public void processFailedGroup(ConsolidatedCargo failedGroup) {
        List<CargoParticipation> participations =
                cargoParticipationRepository.findAllByConsolidatedCargoId(failedGroup.getId());

        for (CargoParticipation participation : participations) {
            if (participation.isSuperseded()) {
                continue;
            }
            CargoOrder order = participation.getCargoOrder();
            switch (participation.getFailurePreference()) {
                case NEXT_TRAIN -> rescheduleToNextGroup(participation, failedGroup);
                case AUTO_REFUND -> {
                    order.cancel();
                    notificationService.notify(order.getShipper().getMember().getId(), NotificationType.REJECT,
                            "공동화 실패 - 자동환불 처리",
                            failedGroup.getOriginStation() + " → " + failedGroup.getDestinationStation()
                                    + " 공동화물이 성립되지 않아 자동환불 처리됐어요.");
                }
            }
        }
    }

    /** 같은 노선의 다른 모집 그룹으로 참여를 이월한다. 마땅한 그룹이 없으면 새로 연다. */
    private void rescheduleToNextGroup(CargoParticipation participation, ConsolidatedCargo failedGroup) {
        CargoOrder order = participation.getCargoOrder();
        Optional<ConsolidatedCargo> nextGroupResult = findOrOpenCompatibleGroup(order, failedGroup.getId());
        if (nextGroupResult.isEmpty()) {
            order.cancel();
            participation.markSuperseded();
            notificationService.notify(order.getShipper().getMember().getId(), NotificationType.REJECT,
                    "다음 열차 이월 불가 - 자동환불",
                    "요청한 운행일에 예약 가능한 다음 열차가 없어 자동환불 처리됐어요.");
            return;
        }
        ConsolidatedCargo nextGroup = nextGroupResult.get();

        PriceQuote priceQuote = dynamicPricingService.quote(nextGroup);
        BigDecimal cost = priceQuote.feasible()
                ? totalPayable(applyHandlingCharges(order.getWeightKg().multiply(priceQuote.ratePerKg()), order), order)
                : participation.getAllocatedCost();

        recordParticipation(nextGroup, order, cost, participation.getFailurePreference());
        participation.markSuperseded();
        // order는 이미 PARTICIPATING 상태이므로 다시 markParticipating()을 호출하지 않는다.

        notificationService.notify(order.getShipper().getMember().getId(), NotificationType.MATCH,
                "다음 열차로 자동 이월",
                failedGroup.getOriginStation() + " → " + failedGroup.getDestinationStation()
                        + " 공동화물이 성립되지 않아 다음 모집 그룹으로 이월됐어요.");
    }

    private Optional<ConsolidatedCargo> findOrOpenCompatibleGroup(CargoOrder order, Long excludeGroupId) {
        Optional<ConsolidatedCargo> existing = consolidatedCargoRepository
                .findAllByOriginStationAndDestinationStationAndTemperatureConditionAndHazardousAndDesiredDateAndStatus(
                        order.getOriginStation(), order.getDestinationStation(), order.getTemperatureCondition(),
                        order.isHazardous(), order.getDesiredDate(), ConsolidationStatus.RECRUITING)
                .stream()
                .filter(group -> !group.getId().equals(excludeGroupId))
                .filter(group -> !findEligibleWagons(group).isEmpty())
                .findFirst();
        if (existing.isPresent()) {
            return existing;
        }
        List<Wagon> eligibleWagons = findEligibleWagons(order, DEFAULT_TARGET_WEIGHT_KG);
        return eligibleWagons.isEmpty()
                ? Optional.empty()
                : Optional.of(openNewGroup(order, eligibleWagons.get(0)));
    }

    /** 참여 등록 + 모집 그룹 중량 반영 + 목표중량 도달 시 최적화 트리거까지 한 번에 처리한다. */
    private void recordParticipation(ConsolidatedCargo group, CargoOrder order, BigDecimal allocatedCost,
                                      FailurePreference preference) {
        cargoParticipationRepository.save(CargoParticipation.of(order, group, allocatedCost, preference));
        group.addParticipation(order.getWeightKg());

        if (group.isTargetReached()) {
            group.markReadyForMatching();
            loadOptimizationService.optimizeReadyGroups();
            notifyAllParticipants(group, NotificationType.MATCH, "공동화물 매칭 대기로 전환",
                    group.getOriginStation() + " → " + group.getDestinationStation()
                            + " 공동화물이 목표중량을 채워 화차 배정을 진행합니다.");
        }
    }

    /** 그룹에 참여 중인 모든 화주에게 같은 알림을 보낸다 (매칭 대기 전환 등 그룹 단위 이벤트용). */
    private void notifyAllParticipants(ConsolidatedCargo group, NotificationType type, String title, String message) {
        cargoParticipationRepository.findAllByConsolidatedCargoId(group.getId()).stream()
                .filter(p -> !p.isSuperseded())
                .map(p -> p.getCargoOrder().getShipper().getMember().getId())
                .distinct()
                .forEach(memberId -> notificationService.notify(memberId, type, title, message));
    }

    public ConsolidatedCargo getEntity(Long consolidatedCargoId) {
        return consolidatedCargoRepository.findById(consolidatedCargoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "공동화물을 찾을 수 없습니다."));
    }

    private ConsolidatedCargo openNewGroup(CargoOrder order, Wagon selectedWagon) {
        LocalDateTime deadline = selectedWagon.getTrain().getReservationDeadline();
        ConsolidatedCargo group = ConsolidatedCargo.open(
                order.getOriginStation(), order.getDestinationStation(), order.getTemperatureCondition(),
                order.isHazardous(), DEFAULT_TARGET_WEIGHT_KG, order.getDesiredDate(), deadline);
        return consolidatedCargoRepository.save(group);
    }

    private void requireAnalyzed(CargoOrder order) {
        if (order.getStatus() != CargoOrderStatus.ANALYZED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "AI 운송조건 분석이 완료된 화물만 공동화물을 조회/참여할 수 있습니다.");
        }
        if (order.isTransportRejected()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "1급 위험물은 철도 운송 접수가 불가능합니다.");
        }
        if (order.isRequiresMsds() && !order.isMsdsAttached()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "위험물 공동운송을 진행하려면 MSDS 파일을 먼저 제출해주세요.");
        }
    }

    private ConsolidationCandidateResponse toCandidateResponse(ConsolidatedCargo group, CargoOrder order, Wagon wagon) {
        PriceQuote priceQuote = dynamicPricingService.quote(group);
        BigDecimal baseFreight = order.getWeightKg().multiply(priceQuote.ratePerKg());
        BigDecimal freight = applyHandlingCharges(baseFreight, order);
        BigDecimal soloTruckCost = freight.multiply(PricingPolicy.SOLO_TRUCK_COST_MULTIPLIER);
        BigDecimal savings = soloTruckCost.subtract(freight).setScale(2, RoundingMode.HALF_UP);

        BigDecimal estimatedSuccessProbability = recruitmentSimulationService.estimateSuccessProbability(group);
        BigDecimal hazardSurchargeRate = order.getSurchargeRate();
        BigDecimal insuranceFee = PricingPolicy.insurancePremium(order.getDeclaredValueKrw());
        BigDecimal platformFee = PricingPolicy.platformFee(freight);
        BigDecimal totalPayable = freight.add(insuranceFee).add(platformFee);

        return new ConsolidationCandidateResponse(
                group.getId(),
                group.getOriginStation(),
                group.getDestinationStation(),
                group.getTargetWeightKg(),
                group.getRecruitedWeightKg(),
                group.getRecruitmentRatePercent(),
                group.getRecruitmentDeadline(),
                wagon.getTrain().getId(),
                wagon.getTrain().getTrainNumber(),
                wagon.getTrain().getDepartureAt(),
                wagon.getTrain().getArrivalAt(),
                wagon.getRemainingWeightKg(),
                BigDecimal.valueOf(100),
                estimatedSuccessProbability,
                freight,
                savings,
                priceQuote.ratePerKg(),
                priceQuote.discountRate(),
                priceQuote.reason(),
                hazardSurchargeRate,
                order.getFixedPowerFeeKrw(),
                insuranceFee,
                platformFee,
                totalPayable);
    }

    private List<Wagon> findEligibleWagons(ConsolidatedCargo group) {
        return trainService.findEligibleWagons(group.getOriginStation(), group.getDestinationStation(),
                group.getDesiredDate(), group.getTemperatureCondition(), group.isHazardous(), group.getTargetWeightKg());
    }

    private List<Wagon> findEligibleWagons(CargoOrder order, BigDecimal requiredWeightKg) {
        return trainService.findEligibleWagons(order.getOriginStation(), order.getDestinationStation(),
                order.getDesiredDate(), order.getTemperatureCondition(), order.isHazardous(), requiredWeightKg);
    }

    /** 위험물·콜드체인·특수화물 중 최고 요율 하나(Max Rule)와 콜드체인 정액 전력비를 적용한다. */
    private BigDecimal applyHandlingCharges(BigDecimal baseCost, CargoOrder order) {
        BigDecimal variableCharge = baseCost.multiply(BigDecimal.ONE.add(order.getSurchargeRate()));
        return variableCharge.add(order.getFixedPowerFeeKrw()).setScale(2, RoundingMode.HALF_UP);
    }

    /** 실제 참여(join) 시 화주가 부담하는 최종 금액 = 운임(위험물 할증 포함) + 적재보험료 + 플랫폼 수수료. */
    private BigDecimal totalPayable(BigDecimal freight, CargoOrder order) {
        BigDecimal insuranceFee = PricingPolicy.insurancePremium(order.getDeclaredValueKrw());
        BigDecimal platformFee = PricingPolicy.platformFee(freight);
        return freight.add(insuranceFee).add(platformFee);
    }
}
