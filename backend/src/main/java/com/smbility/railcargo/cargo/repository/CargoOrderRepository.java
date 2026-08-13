package com.smbility.railcargo.cargo.repository;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.domain.CargoOrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargoOrderRepository extends JpaRepository<CargoOrder, Long> {

    List<CargoOrder> findAllByShipperId(Long shipperId);

    List<CargoOrder> findAllByShipperIdAndStatus(Long shipperId, CargoOrderStatus status);

    /** 성립확률 시뮬레이션에서 노선별 최근 주문 도착 통계(도착률·평균중량)를 계산할 때 사용한다. */
    List<CargoOrder> findAllByOriginStationAndDestinationStationAndCreatedAtAfter(
            String originStation, String destinationStation, LocalDateTime after);
}
