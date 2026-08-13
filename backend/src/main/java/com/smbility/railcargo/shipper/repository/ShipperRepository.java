package com.smbility.railcargo.shipper.repository;

import com.smbility.railcargo.shipper.domain.Shipper;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipperRepository extends JpaRepository<Shipper, Long> {

    Optional<Shipper> findByMemberId(Long memberId);
}
