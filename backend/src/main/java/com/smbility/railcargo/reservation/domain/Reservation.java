package com.smbility.railcargo.reservation.domain;

import com.smbility.railcargo.common.BaseTimeEntity;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 코레일 승인이 확정된 공동화물의 예약 단위. */
@Getter
@Entity
@Table(name = "reservation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consolidated_cargo_id", nullable = false, unique = true)
    private ConsolidatedCargo consolidatedCargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wagon_id", nullable = false)
    private Wagon wagon;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    private Reservation(ConsolidatedCargo consolidatedCargo, Wagon wagon, BigDecimal totalCost) {
        this.consolidatedCargo = consolidatedCargo;
        this.wagon = wagon;
        this.totalCost = totalCost;
        // 이번 단계는 실제 PG 연동 없이 가상 결제로 즉시 완료 처리한다.
        this.paymentStatus = PaymentStatus.VIRTUAL_PAID;
    }

    public static Reservation confirm(ConsolidatedCargo consolidatedCargo, Wagon wagon, BigDecimal totalCost) {
        return new Reservation(consolidatedCargo, wagon, totalCost);
    }
}
