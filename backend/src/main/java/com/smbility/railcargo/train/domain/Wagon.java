package com.smbility.railcargo.train.domain;

import com.smbility.railcargo.common.BaseTimeEntity;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 화차(Wagon). 특정 Train에 연결되며 잔여 적재용량을 관리한다. */
@Getter
@Entity
@Table(name = "wagon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wagon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "wagon_number", nullable = false, length = 20)
    private String wagonNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "wagon_type", nullable = false, length = 20)
    private WagonType wagonType;

    @Column(name = "max_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "remaining_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal remainingWeightKg;

    @Column(name = "hazardous_allowed", nullable = false)
    private boolean hazardousAllowed;

    private Wagon(Train train, String wagonNumber, WagonType wagonType, BigDecimal maxWeightKg,
                  boolean hazardousAllowed) {
        this.train = train;
        this.wagonNumber = wagonNumber;
        this.wagonType = wagonType;
        this.maxWeightKg = maxWeightKg;
        this.remainingWeightKg = maxWeightKg;
        this.hazardousAllowed = hazardousAllowed;
    }

    public static Wagon of(Train train, String wagonNumber, WagonType wagonType, BigDecimal maxWeightKg,
                            boolean hazardousAllowed) {
        return new Wagon(train, wagonNumber, wagonType, maxWeightKg, hazardousAllowed);
    }

    public BigDecimal getLoadedWeightKg() {
        return maxWeightKg.subtract(remainingWeightKg);
    }

    /** 현재 적재율(0~100). */
    public BigDecimal getLoadFactorPercent() {
        if (maxWeightKg.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return getLoadedWeightKg()
                .multiply(BigDecimal.valueOf(100))
                .divide(maxWeightKg, 1, java.math.RoundingMode.HALF_UP);
    }

    public boolean canAccommodate(BigDecimal weightKg, boolean hazardous) {
        if (hazardous && !hazardousAllowed) {
            return false;
        }
        return remainingWeightKg.compareTo(weightKg) >= 0;
    }

    public void allocate(BigDecimal weightKg) {
        if (remainingWeightKg.compareTo(weightKg) < 0) {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED);
        }
        this.remainingWeightKg = this.remainingWeightKg.subtract(weightKg);
    }

    public void release(BigDecimal weightKg) {
        BigDecimal released = this.remainingWeightKg.add(weightKg);
        this.remainingWeightKg = released.min(maxWeightKg);
    }
}
