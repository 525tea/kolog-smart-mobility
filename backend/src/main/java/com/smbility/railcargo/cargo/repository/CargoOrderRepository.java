package com.smbility.railcargo.cargo.repository;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.CargoOrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargoOrderRepository extends JpaRepository<CargoOrder, Long> {

    List<CargoOrder> findAllByShipperId(Long shipperId);

    List<CargoOrder> findAllByShipperIdAndStatus(Long shipperId, CargoOrderStatus status);
}
