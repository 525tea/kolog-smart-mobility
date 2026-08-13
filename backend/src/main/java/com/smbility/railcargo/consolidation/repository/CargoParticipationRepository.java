package com.smbility.railcargo.consolidation.repository;

import com.smbility.railcargo.consolidation.domain.CargoParticipation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargoParticipationRepository extends JpaRepository<CargoParticipation, Long> {

    List<CargoParticipation> findAllByConsolidatedCargoId(Long consolidatedCargoId);

    List<CargoParticipation> findAllByCargoOrderId(Long cargoOrderId);

    List<CargoParticipation> findAllByCargoOrderShipperMemberIdOrderByIdDesc(Long memberId);
}
