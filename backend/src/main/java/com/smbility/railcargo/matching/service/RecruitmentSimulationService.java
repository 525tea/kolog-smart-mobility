package com.smbility.railcargo.matching.service;

import com.smbility.railcargo.consolidation.domain.ConsolidatedCargo;
import java.math.BigDecimal;

/**
 * 아직 목표중량을 채우지 못한 공동화물이 마감 전까지 성립할 확률을 추정한다.
 * 기획안 보충 "3) 수요 성립확률 계산 — 2단계: 확률 시뮬레이션(포아송 분포 + 몬테카를로)"에 대응한다.
 */
public interface RecruitmentSimulationService {

    /** @return 0~100 사이의 성립확률(%) */
    BigDecimal estimateSuccessProbability(ConsolidatedCargo consolidatedCargo);
}
