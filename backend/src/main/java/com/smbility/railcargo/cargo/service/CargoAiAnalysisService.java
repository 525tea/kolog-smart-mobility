package com.smbility.railcargo.cargo.service;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;

/**
 * 화물의 운송조건(중량/부피/온도/위험물/포장/취급주의)을 추출하는 AI 분석 서비스.
 * 기획안 5번 "운송조건 자동 추출" 기능에 대응한다.
 * 현재는 {@link RuleBasedCargoAiAnalysisService}가 규칙 기반으로 구현하며,
 * 추후 실제 AI/ML 모델(외부 API 또는 자체 모델 서빙)로 구현체만 교체하면 된다.
 */
public interface CargoAiAnalysisService {

    CargoAiAnalysisResult analyze(CargoOrder cargoOrder);
}
