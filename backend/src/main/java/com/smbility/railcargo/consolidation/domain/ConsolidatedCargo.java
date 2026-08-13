package com.smbility.railcargo.consolidation.domain;

import com.smbility.railcargo.cargo.domain.TemperatureCondition;
import com.smbility.railcargo.common.BaseTimeEntity;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import com.smbility.railcargo.train.domain.Wagon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 혼적 조건이 맞는 소형 주문들을 모으는 공동화물 모집 단위. */
@Getter
@Entity
@Table(name = "consolidated_cargo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsolidatedCargo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origin_station", nullable = false, length = 50)
    private String originStation;

    @Column(name = "destination_station", nullable = false, length = 50)
    private String destinationStation;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature_condition", nullable = false, length = 20)
    private TemperatureCondition temperatureCondition;

    @Column(nullable = false)
    private boolean hazardous;

    @Column(name = "target_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal targetWeightKg;

    @Column(name = "recruited_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal recruitedWeightKg;

    @Column(name = "recruitment_deadline", nullable = false)
    private LocalDateTime recruitmentDeadline;

    @Column(name = "desired_date", nullable = false)
    private LocalDate desiredDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_wagon_id")
    private Wagon matchedWagon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsolidationStatus status;

    private ConsolidatedCargo(String originStation, String destinationStation,
                               TemperatureCondition temperatureCondition, boolean hazardous,
                               BigDecimal targetWeightKg, LocalDate desiredDate, LocalDateTime recruitmentDeadline) {
        this.originStation = originStation;
        this.destinationStation = destinationStation;
        this.temperatureCondition = temperatureCondition;
        this.hazardous = hazardous;
        this.targetWeightKg = targetWeightKg;
        this.recruitedWeightKg = BigDecimal.ZERO;
        this.desiredDate = desiredDate;
        this.recruitmentDeadline = recruitmentDeadline;
        this.status = ConsolidationStatus.RECRUITING;
    }

    public static ConsolidatedCargo open(String originStation, String destinationStation,
                                          TemperatureCondition temperatureCondition, boolean hazardous,
                                          BigDecimal targetWeightKg, LocalDate desiredDate,
                                          LocalDateTime recruitmentDeadline) {
        return new ConsolidatedCargo(originStation, destinationStation, temperatureCondition, hazardous,
                targetWeightKg, desiredDate, recruitmentDeadline);
    }

    /** 기존 단위 테스트·내부 호출 호환용. 신규 서비스 코드는 희망일을 명시한다. */
    public static ConsolidatedCargo open(String originStation, String destinationStation,
                                          TemperatureCondition temperatureCondition, boolean hazardous,
                                          BigDecimal targetWeightKg, LocalDateTime recruitmentDeadline) {
        return open(originStation, destinationStation, temperatureCondition, hazardous,
                targetWeightKg, recruitmentDeadline.toLocalDate(), recruitmentDeadline);
    }

    public boolean isCompatibleWith(String originStation, String destinationStation,
                                     TemperatureCondition temperatureCondition, boolean hazardous,
                                     LocalDate desiredDate) {
        return this.originStation.equals(originStation)
                && this.destinationStation.equals(destinationStation)
                && this.temperatureCondition == temperatureCondition
                && this.hazardous == hazardous
                && this.desiredDate.equals(desiredDate);
    }

    public boolean isCompatibleWith(String originStation, String destinationStation,
                                     TemperatureCondition temperatureCondition, boolean hazardous) {
        return isCompatibleWith(originStation, destinationStation, temperatureCondition, hazardous, desiredDate);
    }

    public BigDecimal getRecruitmentRatePercent() {
        if (targetWeightKg.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return recruitedWeightKg.multiply(BigDecimal.valueOf(100))
                .divide(targetWeightKg, 1, RoundingMode.HALF_UP);
    }

    public boolean isTargetReached() {
        return recruitedWeightKg.compareTo(targetWeightKg) >= 0;
    }

    public void addParticipation(BigDecimal weightKg) {
        if (status != ConsolidationStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "모집 중인 공동화물에만 참여할 수 있습니다.");
        }
        this.recruitedWeightKg = this.recruitedWeightKg.add(weightKg);
    }

    /** 목표중량 도달 직후: 최적 적재 조합 계산(화차 배정) 대기 상태로 전환한다. */
    public void markReadyForMatching() {
        if (status != ConsolidationStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "모집 중인 공동화물만 매칭 대기로 전환할 수 있습니다.");
        }
        this.status = ConsolidationStatus.READY_FOR_MATCHING;
    }

    public void markMatched(Wagon wagon) {
        this.matchedWagon = wagon;
        this.status = ConsolidationStatus.MATCHED;
    }

    public void markPendingApproval() {
        this.status = ConsolidationStatus.PENDING_APPROVAL;
    }

    public void approve() {
        requireStatus(ConsolidationStatus.PENDING_APPROVAL);
        this.status = ConsolidationStatus.APPROVED;
    }

    public void reject() {
        requireStatus(ConsolidationStatus.PENDING_APPROVAL);
        this.status = ConsolidationStatus.REJECTED;
    }

    /** 마감까지 목표중량을 채우지 못해 모집 자체가 실패한 경우 (기획안 보충 "공동화 실패시 대응"). */
    public void cancel() {
        if (status != ConsolidationStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "모집 중인 공동화물만 취소할 수 있습니다.");
        }
        this.status = ConsolidationStatus.CANCELLED;
    }

    public void confirm() {
        requireStatus(ConsolidationStatus.APPROVED);
        this.status = ConsolidationStatus.CONFIRMED;
    }

    private void requireStatus(ConsolidationStatus expected) {
        if (this.status != expected) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "현재 상태(" + this.status + ")에서는 수행할 수 없습니다.");
        }
    }
}
