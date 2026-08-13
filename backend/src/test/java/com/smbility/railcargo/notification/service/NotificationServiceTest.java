package com.smbility.railcargo.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;
import com.smbility.railcargo.auth.repository.MemberRepository;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.notification.domain.Notification;
import com.smbility.railcargo.notification.domain.NotificationType;
import com.smbility.railcargo.notification.dto.NotificationResponse;
import com.smbility.railcargo.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Member member() {
        Member member = Member.of("shipper@example.com", "encoded", MemberRole.SHIPPER);
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    @Test
    void 알림을_생성하면_해당_회원_앞으로_저장된다() {
        Member member = member();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        notificationService.notify(1L, NotificationType.MATCH, "제목", "내용");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("제목");
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.MATCH);
        assertThat(captor.getValue().isRead()).isFalse();
    }

    @Test
    void 존재하지_않는_회원에게_알림을_보내면_예외가_발생한다() {
        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.notify(999L, NotificationType.INFO, "제목", "내용"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 읽지_않은_알림_개수를_센다() {
        when(notificationRepository.countByMemberIdAndReadFalse(1L)).thenReturn(3L);

        assertThat(notificationService.getUnreadCount(1L)).isEqualTo(3L);
    }

    @Test
    void 전체_읽음_처리하면_모든_알림이_읽음_상태가_된다() {
        Member member = member();
        Notification n1 = Notification.of(member, NotificationType.INFO, "제목1", "내용1");
        Notification n2 = Notification.of(member, NotificationType.MATCH, "제목2", "내용2");
        when(notificationRepository.findAllByMemberIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1, n2));

        notificationService.markAllRead(1L);

        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
    }

    @Test
    void 내_알림_목록을_최신순으로_조회한다() {
        Member member = member();
        Notification n1 = Notification.of(member, NotificationType.INFO, "제목1", "내용1");
        when(notificationRepository.findAllByMemberIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1));

        List<NotificationResponse> responses = notificationService.getMyNotifications(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("제목1");
    }
}
