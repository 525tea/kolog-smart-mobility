package com.smbility.railcargo.cargo.domain;

import com.smbility.railcargo.common.BaseTimeEntity;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import com.smbility.railcargo.shipper.domain.Shipper;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 화주가 등록한 소형 주문(화물) 1건. */
@Getter
@Entity
@Table(name = "cargo_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CargoOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    private Shipper shipper;

    @Column(name = "cargo_name", nullable = false, length = 100)
    private String cargoName;

    /** 화주가 입력한 원본 텍스트: 자연어 설명, 상품 URL, 발주서/송장 내용 등 */
    @Column(name = "raw_input", columnDefinition = "TEXT")
    private String rawInput;

    @Column(name = "origin_station", nullable = false, length = 50)
    private String originStation;

    @Column(name = "destination_station", nullable = false, length = 50)
    private String destinationStation;

    @Column(name = "desired_date", nullable = false)
    private LocalDate desiredDate;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "volume_cbm", precision = 10, scale = 3)
    private BigDecimal volumeCbm;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature_condition", length = 20)
    private TemperatureCondition temperatureCondition;

    @Column(nullable = false)
    private boolean hazardous;

    @Column(name = "packaging_type", length = 30)
    private String packagingType;

    @Column(name = "handling_note", length = 200)
    private String handlingNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CargoOrderStatus status;

    private CargoOrder(Shipper shipper, String cargoName, String rawInput, String originStation,
                        String destinationStation, LocalDate desiredDate) {
        this.shipper = shipper;
        this.cargoName = cargoName;
        this.rawInput = rawInput;
        this.originStation = originStation;
        this.destinationStation = destinationStation;
        this.desiredDate = desiredDate;
        this.status = CargoOrderStatus.REGISTERED;
    }

    public static CargoOrder register(Shipper shipper, String cargoName, String rawInput, String originStation,
                                       String destinationStation, LocalDate desiredDate) {
        return new CargoOrder(shipper, cargoName, rawInput, originStation, destinationStation, desiredDate);
    }

    public void applyAiAnalysis(BigDecimal weightKg, BigDecimal volumeCbm, TemperatureCondition temperatureCondition,
                                 boolean hazardous, String packagingType, String handlingNote) {
        this.weightKg = weightKg;
        this.volumeCbm = volumeCbm;
        this.temperatureCondition = temperatureCondition;
        this.hazardous = hazardous;
        this.packagingType = packagingType;
        this.handlingNote = handlingNote;
        this.status = CargoOrderStatus.ANALYZED;
    }

    public void applyShipperCorrection(BigDecimal weightKg, BigDecimal volumeCbm,
                                        TemperatureCondition temperatureCondition, Boolean hazardous,
                                        String packagingType, String handlingNote) {
        if (weightKg != null) this.weightKg = weightKg;
        if (volumeCbm != null) this.volumeCbm = volumeCbm;
        if (temperatureCondition != null) this.temperatureCondition = temperatureCondition;
        if (hazardous != null) this.hazardous = hazardous;
        if (packagingType != null) this.packagingType = packagingType;
        if (handlingNote != null) this.handlingNote = handlingNote;
    }

    public void markParticipating() {
        requireAnalyzed();
        this.status = CargoOrderStatus.PARTICIPATING;
    }

    public void markReserved() {
        this.status = CargoOrderStatus.RESERVED;
    }

    public void cancel() {
        this.status = CargoOrderStatus.CANCELLED;
    }

    private void requireAnalyzed() {
        if (this.status != CargoOrderStatus.ANALYZED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "AI 운송조건 분석이 완료된 화물만 공동화에 참여할 수 있습니다.");
        }
    }
}
