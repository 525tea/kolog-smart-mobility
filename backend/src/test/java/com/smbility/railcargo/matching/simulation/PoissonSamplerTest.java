package com.smbility.railcargo.matching.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class PoissonSamplerTest {

    @Test
    void lambda가_0이면_항상_0을_반환한다() {
        Random random = new Random(42);
        for (int i = 0; i < 100; i++) {
            assertThat(PoissonSampler.sample(0, random)).isZero();
        }
    }

    @Test
    void 표본평균은_lambda에_수렴한다() {
        Random random = new Random(1234);
        double lambda = 5.0;
        int trials = 20_000;
        long sum = 0;
        for (int i = 0; i < trials; i++) {
            sum += PoissonSampler.sample(lambda, random);
        }
        double average = sum / (double) trials;

        assertThat(average).isCloseTo(lambda, org.assertj.core.data.Offset.offset(0.2));
    }
}
