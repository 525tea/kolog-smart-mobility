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
import jakarta.persistence.Lob;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "service_mode", nullable = false, length = 20)
    private ServiceMode serviceMode;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "volume_cbm", precision = 10, scale = 3)
    private BigDecimal volumeCbm;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature_condition", length = 20)
    private TemperatureCondition temperatureCondition;

    @Column(nullable = false)
    private boolean hazardous;

    /** hazardous=true일 때만 의미 있음. A(최고위험)~D(최저위험). */
    @Enumerated(EnumType.STRING)
    @Column(name = "hazard_grade", length = 10)
    private HazardGrade hazardGrade;

    @Column(name = "hazard_class_code", length = 10)
    private String hazardClassCode;

    @Column(name = "hazard_class_name", length = 100)
    private String hazardClassName;

    @Column(name = "transport_rejected", nullable = false)
    private boolean transportRejected;

    @Column(name = "requires_msds", nullable = false)
    private boolean requiresMsds;

    @Column(name = "msds_file_name", length = 255)
    private String msdsFileName;

    @Column(name = "msds_content_type", length = 100)
    private String msdsContentType;

    @Lob
    @Column(name = "msds_data", columnDefinition = "LONGBLOB")
    private byte[] msdsData;

    @Column(name = "surcharge_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal surchargeRate = BigDecimal.ZERO;

    @Column(name = "fixed_power_fee_krw", nullable = false, precision = 12, scale = 2)
    private BigDecimal fixedPowerFeeKrw = BigDecimal.ZERO;

    @Column(name = "detected_temperature_c", precision = 6, scale = 2)
    private BigDecimal detectedTemperatureC;

    @Column(name = "packaging_type", length = 30)
    private String packagingType;

    @Column(name = "handling_note", length = 200)
    private String handlingNote;

    /**
     * 화주가 등록 시 신고한 화물가액(원). 적재보험료 산정 기준이 된다({@code PricingPolicy.insurancePremium}).
     * 선택 입력이며, 신고하지 않으면(null) 보험료는 0으로 계산한다.
     */
    @Column(name = "declared_value_krw", precision = 12, scale = 2)
    private BigDecimal declaredValueKrw;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CargoOrderStatus status;

    private CargoOrder(Shipper shipper, String cargoName, String rawInput, String originStation,
                        String destinationStation, LocalDate desiredDate, BigDecimal declaredValueKrw,
                        ServiceMode serviceMode) {
        this.shipper = shipper;
        this.cargoName = cargoName;
        this.rawInput = rawInput;
        this.originStation = originStation;
        this.destinationStation = destinationStation;
        this.desiredDate = desiredDate;
        this.declaredValueKrw = declaredValueKrw;
        this.serviceMode = serviceMode == null ? ServiceMode.CO_LOAD : serviceMode;
        this.status = CargoOrderStatus.REGISTERED;
    }

    public static CargoOrder register(Shipper shipper, String cargoName, String rawInput, String originStation,
                                       String destinationStation, LocalDate desiredDate) {
        return register(shipper, cargoName, rawInput, originStation, destinationStation, desiredDate, null);
    }

    public static CargoOrder register(Shipper shipper, String cargoName, String rawInput, String originStation,
                                       String destinationStation, LocalDate desiredDate, BigDecimal declaredValueKrw) {
        return register(shipper, cargoName, rawInput, originStation, destinationStation, desiredDate,
                declaredValueKrw, ServiceMode.CO_LOAD);
    }

    public static CargoOrder register(Shipper shipper, String cargoName, String rawInput, String originStation,
                                       String destinationStation, LocalDate desiredDate, BigDecimal declaredValueKrw,
                                       ServiceMode serviceMode) {
        return new CargoOrder(shipper, cargoName, rawInput, originStation, destinationStation, desiredDate,
                declaredValueKrw, serviceMode);
    }

    public void applyAiAnalysis(BigDecimal weightKg, BigDecimal volumeCbm, TemperatureCondition temperatureCondition,
                                 boolean hazardous, HazardGrade hazardGrade, String packagingType, String handlingNote) {
        applyAiAnalysis(weightKg, volumeCbm, temperatureCondition, hazardous, hazardGrade,
                null, null, false, false, BigDecimal.ZERO, BigDecimal.ZERO, null,
                packagingType, handlingNote);
    }

    public void applyAiAnalysis(BigDecimal weightKg, BigDecimal volumeCbm, TemperatureCondition temperatureCondition,
                                 boolean hazardous, HazardGrade hazardGrade, String hazardClassCode,
                                 String hazardClassName, boolean transportRejected, boolean requiresMsds,
                                 BigDecimal surchargeRate, BigDecimal fixedPowerFeeKrw,
                                 BigDecimal detectedTemperatureC, String packagingType, String handlingNote) {
        this.weightKg = weightKg;
        this.volumeCbm = volumeCbm;
        this.temperatureCondition = temperatureCondition;
        this.hazardous = hazardous;
        this.hazardGrade = hazardous ? hazardGrade : null;
        this.hazardClassCode = hazardous ? hazardClassCode : null;
        this.hazardClassName = hazardous ? hazardClassName : null;
        this.transportRejected = transportRejected;
        this.requiresMsds = hazardous && requiresMsds;
        this.surchargeRate = surchargeRate == null ? BigDecimal.ZERO : surchargeRate;
        this.fixedPowerFeeKrw = fixedPowerFeeKrw == null ? BigDecimal.ZERO : fixedPowerFeeKrw;
        this.detectedTemperatureC = detectedTemperatureC;
        this.packagingType = packagingType;
        this.handlingNote = handlingNote;
        this.status = CargoOrderStatus.ANALYZED;
    }

    public void applyShipperCorrection(BigDecimal weightKg, BigDecimal volumeCbm,
                                        TemperatureCondition temperatureCondition, Boolean hazardous,
                                        HazardGrade hazardGrade, String packagingType, String handlingNote) {
        if (weightKg != null) this.weightKg = weightKg;
        if (volumeCbm != null) this.volumeCbm = volumeCbm;
        if (temperatureCondition != null) this.temperatureCondition = temperatureCondition;
        if (hazardous != null) {
            this.hazardous = hazardous;
            if (hazardous) this.requiresMsds = true;
        }
        if (hazardGrade != null) this.hazardGrade = hazardGrade;
        if (!this.hazardous) this.hazardGrade = null;
        if (!this.hazardous) {
            this.hazardClassCode = null;
            this.hazardClassName = null;
            this.transportRejected = false;
            this.requiresMsds = false;
        }
        if (packagingType != null) this.packagingType = packagingType;
        if (handlingNote != null) this.handlingNote = handlingNote;
    }

    public void markParticipating() {
        requireAnalyzed();
        this.status = CargoOrderStatus.PARTICIPATING;
    }

    public void attachMsds(String fileName, String contentType, byte[] data) {
        if (!this.requiresMsds) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "MSDS 제출 대상 화물이 아닙니다.");
        }
        this.msdsFileName = fileName;
        this.msdsContentType = contentType;
        this.msdsData = data;
    }

    public boolean isMsdsAttached() {
        return msdsData != null && msdsData.length > 0;
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
