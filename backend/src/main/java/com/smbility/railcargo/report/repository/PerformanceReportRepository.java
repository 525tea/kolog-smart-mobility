package com.smbility.railcargo.report.repository;

import com.smbility.railcargo.report.domain.PerformanceReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceReportRepository extends JpaRepository<PerformanceReport, Long> {

    List<PerformanceReport> findAllByTrainIdOrderByCreatedAtDesc(Long trainId);
}
