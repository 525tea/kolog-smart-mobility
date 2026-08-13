package com.smbility.railcargo.notification.domain;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서버가 실제로 발생한 상태 변화(매칭 대기 전환, 코레일 승인/반려, 공동화 실패 처리 등)를 기록해 두는 알림.
 * 프론트엔드의 기존 "클라이언트 로컬 활동 로그"(새로고침해도 남지만 다른 기기와 공유되지 않던 방식)를
 * 대체하는 서버 저장 알림이다.
 */
@Getter
@Entity
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    private Notification(Member member, NotificationType type, String title, String message) {
        this.member = member;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = false;
    }

    public static Notification of(Member member, NotificationType type, String title, String message) {
        return new Notification(member, type, title, message);
    }

    public void markRead() {
        this.read = true;
    }
}
