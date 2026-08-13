package com.smbility.railcargo.matching.service;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.repository.CargoOrderRepository;
import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import com.smbility.railcargo.matching.simulation.PoissonSampler;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 노선별 최근 주문 도착 이력을 포아송 과정(주문이 시간당 λ건씩 무작위로 도착)으로 가정하고,
 * 마감시간까지 목표중량을 채울 확률을 몬테카를로 시뮬레이션으로 추정한다.
 *
 * <p>과거 데이터가 충분히 쌓이기 전까지(콜드스타트)는 {@link #DEFAULT_ORDERS_PER_DAY}, {@link #DEFAULT_AVG_WEIGHT_PER_ORDER_KG}
 * 같은 가정값을 사용한다. 이는 코레일 실시간 예약 데이터가 공개되어 있지 않아 해커톤 단계에서는
 * 가상 값으로 대체한다고 명시한 기획안 보충 4번의 전제와 동일하다. 데이터가 쌓이면
 * {@link #calculateDemandStats}만 실제 통계 기반으로 교체하면 된다.
 */
@Service
@RequiredArgsConstructor
public class MonteCarloRecruitmentSimulationService implements RecruitmentSimulationService {

    private static final int TRIALS = 2000;
    private static final int LOOKBACK_DAYS = 14;
    private static final int MIN_SAMPLE_SIZE = 3;

    /** 콜드스타트 가정값: 노선당 하루 평균 신규 주문 건수. */
    private static final double DEFAULT_ORDERS_PER_DAY = 3.0;
    /** 콜드스타트 가정값: 주문 1건당 평균 중량(kg). 기획안 보충의 "건당 100~500kg" 범위의 하한 근처. */
    private static final BigDecimal DEFAULT_AVG_WEIGHT_PER_ORDER_KG = BigDecimal.valueOf(150);

    private final CargoOrderRepository cargoOrderRepository;

    @Override
    public BigDecimal estimateSuccessProbability(ConsolidatedCargo consolidatedCargo) {
        if (consolidatedCargo.isTargetReached()) {
            return BigDecimal.valueOf(100);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!consolidatedCargo.getRecruitmentDeadline().isAfter(now)) {
            return BigDecimal.ZERO;
        }

        DemandStats stats = calculateDemandStats(consolidatedCargo.getOriginStation(), consolidatedCargo.getDestinationStation());
        double remainingHours = Duration.between(now, consolidatedCargo.getRecruitmentDeadline()).toMinutes() / 60.0;
        double lambdaOrders = stats.ordersPerHour() * remainingHours;

        BigDecimal shortfall = consolidatedCargo.getTargetWeightKg().subtract(consolidatedCargo.getRecruitedWeightKg());
        if (shortfall.signum() <= 0) {
            return BigDecimal.valueOf(100);
        }

        Random random = new Random();
        int successes = 0;
        for (int i = 0; i < TRIALS; i++) {
            int arrivals = PoissonSampler.sample(lambdaOrders, random);
            BigDecimal simulatedWeight = stats.avgWeightPerOrderKg().multiply(BigDecimal.valueOf(arrivals));
            if (simulatedWeight.compareTo(shortfall) >= 0) {
                successes++;
            }
        }

        return BigDecimal.valueOf(successes)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(TRIALS), 1, RoundingMode.HALF_UP);
    }

    private DemandStats calculateDemandStats(String originStation, String destinationStation) {
        LocalDateTime after = LocalDateTime.now().minusDays(LOOKBACK_DAYS);
        List<CargoOrder> recentOrders = cargoOrderRepository
                .findAllByOriginStationAndDestinationStationAndCreatedAtAfter(originStation, destinationStation, after);

        if (recentOrders.size() < MIN_SAMPLE_SIZE) {
            return new DemandStats(DEFAULT_ORDERS_PER_DAY / 24.0, DEFAULT_AVG_WEIGHT_PER_ORDER_KG);
        }

        double ordersPerHour = recentOrders.size() / (double) (LOOKBACK_DAYS * 24);
        BigDecimal totalWeight = recentOrders.stream()
                .map(order -> order.getWeightKg() == null ? DEFAULT_AVG_WEIGHT_PER_ORDER_KG : order.getWeightKg())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgWeight = totalWeight.divide(BigDecimal.valueOf(recentOrders.size()), 2, RoundingMode.HALF_UP);

        return new DemandStats(ordersPerHour, avgWeight);
    }

    private record DemandStats(double ordersPerHour, BigDecimal avgWeightPerOrderKg) {
    }
}
