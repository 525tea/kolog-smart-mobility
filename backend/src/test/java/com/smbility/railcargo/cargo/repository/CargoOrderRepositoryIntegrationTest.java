package com.smbility.railcargo.cargo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;
import com.smbility.railcargo.auth.repository.MemberRepository;
import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.shipper.domain.Shipper;
import com.smbility.railcargo.shipper.repository.ShipperRepository;
import com.smbility.railcargo.support.MySqlTestContainerSupport;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 MySQL(Testcontainers)에 Flyway 마이그레이션을 적용한 뒤,
 * Member -> Shipper -> CargoOrder 로 이어지는 연관관계/제약조건이 의도대로 동작하는지 검증한다.
 */
@SpringBootTest
@Transactional
class CargoOrderRepositoryIntegrationTest extends MySqlTestContainerSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ShipperRepository shipperRepository;

    @Autowired
    private CargoOrderRepository cargoOrderRepository;

    @Test
    void 화주가_등록한_화물을_shipperId로_조회할_수_있다() {
        Member member = memberRepository.save(Member.of("shipper1@example.com", "encoded-password", MemberRole.SHIPPER));
        Shipper shipper = shipperRepository.save(Shipper.of(member, "123-45-67890", "테스트상사", "홍길동", "010-0000-0000"));

        CargoOrder cargoOrder = CargoOrder.register(shipper, "신선식품 박스", "냉장 200kg 파손주의",
                "천안", "서울", LocalDate.now().plusDays(1));
        cargoOrderRepository.save(cargoOrder);

        List<CargoOrder> found = cargoOrderRepository.findAllByShipperId(shipper.getId());

        assertEquals(1, found.size());
        assertEquals("신선식품 박스", found.get(0).getCargoName());
        assertEquals("REGISTERED", found.get(0).getStatus().name());
    }
}
