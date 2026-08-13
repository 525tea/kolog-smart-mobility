package com.smbility.railcargo.consolidation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 모집 마감시간이 지난 공동화물을 주기적으로 찾아 실패 처리한다.
 * 기획안 보충 "공동화 실패시 대응/보상 - 마감 시점까지 성립하지 않으면 ... 처리한다"에 대응.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsolidationFailureScheduler {

    private static final long FIXED_DELAY_MILLIS = 5 * 60 * 1000L; // 5분

    private final ConsolidationService consolidationService;

    @Scheduled(fixedDelay = FIXED_DELAY_MILLIS)
    public void sweepExpiredGroups() {
        int processed = consolidationService.handleExpiredRecruitingGroups();
        if (processed > 0) {
            log.info("모집 마감 경과로 {}건의 공동화물을 실패 처리했습니다.", processed);
        }
    }
}
