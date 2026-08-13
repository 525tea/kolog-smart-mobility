package com.smbility.railcargo.notification.service;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.repository.MemberRepository;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import com.smbility.railcargo.notification.domain.Notification;
import com.smbility.railcargo.notification.domain.NotificationType;
import com.smbility.railcargo.notification.dto.NotificationResponse;
import com.smbility.railcargo.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서버에서 실제로 발생한 상태 변화를 알림으로 남긴다 (화물 등록/AI 분석 완료, 매칭 대기 전환,
 * 코레일 승인/반려, 공동화 실패 처리 등). 각 도메인 서비스가 상태를 바꾸는 시점에 직접 호출한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void notify(Long memberId, NotificationType type, String title, String message) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원을 찾을 수 없습니다."));
        notificationRepository.save(Notification.of(member, type, title, message));
    }

    public List<NotificationResponse> getMyNotifications(Long memberId) {
        return notificationRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long getUnreadCount(Long memberId) {
        return notificationRepository.countByMemberIdAndReadFalse(memberId);
    }

    @Transactional
    public void markAllRead(Long memberId) {
        notificationRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId)
                .forEach(Notification::markRead);
    }
}
