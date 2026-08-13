package com.smbility.railcargo.consolidation.domain;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.common.BaseTimeEntity;
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

/** CargoOrder(소형 주문) - ConsolidatedCargo(공동화물) 참여 관계. */
@Getter
@Entity
@Table(name = "cargo_participation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CargoParticipation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cargo_order_id", nullable = false)
    private CargoOrder cargoOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consolidated_cargo_id", nullable = false)
    private ConsolidatedCargo consolidatedCargo;

    /** 참여 시점에 배분된 예상 비용 (기획안 "주문별 비용 배분"). */
    @Column(name = "allocated_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal allocatedCost;

    /** 공동화가 성립하지 못했을 때 화주가 미리 선택해둔 처리 방식 (기획안 보충 "공동화 실패시 대응"). */
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_preference", nullable = false, length = 20)
    private FailurePreference failurePreference;

    /** 이 참여가 실패 처리로 인해 대체된 경우, 새로 이월된 참여를 가리킨다 (이력 추적용). */
    @Column(name = "superseded", nullable = false)
    private boolean superseded;

    private CargoParticipation(CargoOrder cargoOrder, ConsolidatedCargo consolidatedCargo, BigDecimal allocatedCost,
                                FailurePreference failurePreference) {
        this.cargoOrder = cargoOrder;
        this.consolidatedCargo = consolidatedCargo;
        this.allocatedCost = allocatedCost;
        this.failurePreference = failurePreference;
        this.superseded = false;
    }

    public static CargoParticipation of(CargoOrder cargoOrder, ConsolidatedCargo consolidatedCargo,
                                         BigDecimal allocatedCost, FailurePreference failurePreference) {
        return new CargoParticipation(cargoOrder, consolidatedCargo, allocatedCost, failurePreference);
    }

    public void markSuperseded() {
        this.superseded = true;
    }
}
