package com.smbility.railcargo;

import com.smbility.railcargo.support.MySqlTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 스프링 컨텍스트가 정상적으로 뜨는지(빈 구성, JPA 매핑, Flyway 마이그레이션 포함) 확인하는 스모크 테스트.
 */
@SpringBootTest
class RailcargoApiApplicationTests extends MySqlTestContainerSupport {

	@Test
	void contextLoads() {
	}

}
