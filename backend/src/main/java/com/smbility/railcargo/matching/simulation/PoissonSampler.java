package com.smbility.railcargo.matching.simulation;

import java.util.Random;

/**
 * Knuth의 알고리즘을 이용한 포아송 분포 샘플러.
 * lambda(평균 발생 횟수)가 주어졌을 때, 그 분포를 따르는 정수 표본을 하나 뽑는다.
 * 순수 함수라 별도 의존성 없이 단위 테스트가 가능하다.
 */
public final class PoissonSampler {

    private PoissonSampler() {
    }

    public static int sample(double lambda, Random random) {
        if (lambda <= 0) {
            return 0;
        }
        double l = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            p *= random.nextDouble();
        } while (p > l);
        return k - 1;
    }
}
