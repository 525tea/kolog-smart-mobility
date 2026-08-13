package com.smbility.railcargo.reservation.repository;

import com.smbility.railcargo.reservation.domain.Reservation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByConsolidatedCargoId(Long consolidatedCargoId);
}
