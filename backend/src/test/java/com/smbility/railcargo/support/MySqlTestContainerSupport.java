package com.smbility.railcargo.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/**
 * DB가 필요한 통합 테스트에서 상속해서 사용하는 공통 Testcontainers(MySQL) 설정.
 * 로컬/CI 모두 Docker만 있으면 별도 DB 준비 없이 실제 MySQL로 Flyway 마이그레이션까지 검증한다.
 * 하위 클래스에 {@code @SpringBootTest}를 붙여 사용한다.
 *
 * <p>여러 테스트 클래스({@code RailcargoApiApplicationTests}, {@code CargoOrderRepositoryIntegrationTest})가
 * 이 컨테이너 하나를 공유하는 "싱글턴 컨테이너" 패턴이다. 예전에는 {@code @Testcontainers}+{@code @Container}를
 * 썼는데, 그 조합은 컨테이너 시작/종료를 "테스트 클래스 단위"로 관리한다 — 정적(static) 필드로 컨테이너를
 * 공유해도 첫 번째로 실행된 테스트 클래스가 끝나면서 컨테이너를 꺼버려서, 두 번째 클래스는 이미 죽은
 * 컨테이너에 연결하려다 "Connection refused"로 실패했다(GitHub Actions에서 매번 재현됨).
 * 정적 초기화 블록에서 한 번만 직접 start()하고 절대 stop()하지 않는 방식(Testcontainers 공식 문서의
 * "Singleton Container" 패턴)으로 바꿔 여러 클래스가 안전하게 공유하도록 고쳤다. 컨테이너는 JVM 종료 시
 * Testcontainers의 Ryuk 정리 컨테이너가 자동으로 치운다.
 */
public abstract class MySqlTestContainerSupport {

    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("railcargo")
            .withUsername("railcargo")
            .withPassword("railcargo");

    static {
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
    }
}
