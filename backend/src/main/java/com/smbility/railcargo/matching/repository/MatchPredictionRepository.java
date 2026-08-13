package com.smbility.railcargo.matching.repository;

import com.smbility.railcargo.matching.domain.MatchPrediction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchPredictionRepository extends JpaRepository<MatchPrediction, Long> {

    Optional<MatchPrediction> findTopByConsolidatedCargoIdOrderByIdDesc(Long consolidatedCargoId);
}
