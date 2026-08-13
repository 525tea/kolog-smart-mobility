package com.smbility.railcargo.report.domain;

import com.smbility.railcargo.common.BaseTimeEntity;
import com.smbility.railcargo.train.domain.Train;
import com.smbility.railcargo.train.domain.Wagon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** 공동화물 승인·운송 확정 후 생성되는 성과 리포트 (기획안 화면 O3). */
@Getter
@Entity
@Table(name = "performance_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PerformanceReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wagon_id", nullable = false)
    private Wagon wagon;

    @Column(name = "before_load_factor", nullable = false, precision = 5, scale = 2)
    private BigDecimal beforeLoadFactor;

    @Column(name = "after_load_factor", nullable = false, precision = 5, scale = 2)
    private BigDecimal afterLoadFactor;

    @Column(name = "new_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal newRevenue;

    @Column(name = "cost_savings", nullable = false, precision = 12, scale = 2)
    private BigDecimal costSavings;

    @Column(name = "carbon_reduction_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal carbonReductionKg;

    private PerformanceReport(Train train, Wagon wagon, BigDecimal beforeLoadFactor, BigDecimal afterLoadFactor,
                               BigDecimal newRevenue, BigDecimal costSavings, BigDecimal carbonReductionKg) {
        this.train = train;
        this.wagon = wagon;
        this.beforeLoadFactor = beforeLoadFactor;
        this.afterLoadFactor = afterLoadFactor;
        this.newRevenue = newRevenue;
        this.costSavings = costSavings;
        this.carbonReductionKg = carbonReductionKg;
    }

    public static PerformanceReport of(Train train, Wagon wagon, BigDecimal beforeLoadFactor,
                                        BigDecimal afterLoadFactor, BigDecimal newRevenue, BigDecimal costSavings,
                                        BigDecimal carbonReductionKg) {
        return new PerformanceReport(train, wagon, beforeLoadFactor, afterLoadFactor, newRevenue, costSavings,
                carbonReductionKg);
    }
}
