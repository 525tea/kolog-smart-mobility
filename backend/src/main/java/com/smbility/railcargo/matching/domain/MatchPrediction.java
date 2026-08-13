package com.smbility.railcargo.matching.domain;

import com.smbility.railcargo.common.BaseTimeEntity;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
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

/**
 * 공동화물 - 화차 매칭 시 AI(현재는 규칙 기반)가 산정한 성립확률/수익성 예측 결과.
 * 기획안 5번 "성립·수익성 예측" 기능에 대응한다.
 */
@Getter
@Entity
@Table(name = "match_prediction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchPrediction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consolidated_cargo_id", nullable = false)
    private ConsolidatedCargo consolidatedCargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wagon_id", nullable = false)
    private Wagon wagon;

    /** 공동화 성립확률 (0~100). */
    @Column(name = "success_probability", nullable = false, precision = 5, scale = 2)
    private BigDecimal successProbability;

    @Column(name = "expected_revenue", nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedRevenue;

    /** 매칭 확정 시 예상 적재율 (0~100). */
    @Column(name = "expected_load_factor", nullable = false, precision = 5, scale = 2)
    private BigDecimal expectedLoadFactor;

    @Column(name = "contribution_margin", nullable = false, precision = 12, scale = 2)
    private BigDecimal contributionMargin;

    private MatchPrediction(ConsolidatedCargo consolidatedCargo, Wagon wagon, BigDecimal successProbability,
                             BigDecimal expectedRevenue, BigDecimal expectedLoadFactor, BigDecimal contributionMargin) {
        this.consolidatedCargo = consolidatedCargo;
        this.wagon = wagon;
        this.successProbability = successProbability;
        this.expectedRevenue = expectedRevenue;
        this.expectedLoadFactor = expectedLoadFactor;
        this.contributionMargin = contributionMargin;
    }

    public static MatchPrediction of(ConsolidatedCargo consolidatedCargo, Wagon wagon, BigDecimal successProbability,
                                      BigDecimal expectedRevenue, BigDecimal expectedLoadFactor,
                                      BigDecimal contributionMargin) {
        return new MatchPrediction(consolidatedCargo, wagon, successProbability, expectedRevenue,
                expectedLoadFactor, contributionMargin);
    }
}
