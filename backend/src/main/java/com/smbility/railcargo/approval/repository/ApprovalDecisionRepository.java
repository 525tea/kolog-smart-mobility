package com.smbility.railcargo.approval.repository;

import com.smbility.railcargo.approval.domain.ApprovalDecision;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, Long> {
}
