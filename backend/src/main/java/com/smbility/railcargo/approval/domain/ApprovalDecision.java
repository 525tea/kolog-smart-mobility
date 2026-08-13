package com.smbility.railcargo.approval.domain;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.common.BaseTimeEntity;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 코레일 담당자가 공동화물에 대해 내린 승인/반려 기록. */
@Getter
@Entity
@Table(name = "approval_decision")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalDecision extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consolidated_cargo_id", nullable = false)
    private ConsolidatedCargo consolidatedCargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", nullable = false)
    private Member operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status;

    @Column(length = 500)
    private String memo;

    private ApprovalDecision(ConsolidatedCargo consolidatedCargo, Member operator, ApprovalStatus status, String memo) {
        this.consolidatedCargo = consolidatedCargo;
        this.operator = operator;
        this.status = status;
        this.memo = memo;
    }

    public static ApprovalDecision of(ConsolidatedCargo consolidatedCargo, Member operator, ApprovalStatus status,
                                       String memo) {
        return new ApprovalDecision(consolidatedCargo, operator, status, memo);
    }
}
