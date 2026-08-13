package com.smbility.railcargo.notification.controller;

import com.smbility.railcargo.auth.jwt.LoginMember;
import com.smbility.railcargo.notification.dto.NotificationResponse;
import com.smbility.railcargo.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "서버 알림 조회/읽음 처리")
@RestController
@RequestMapping("/api/v1/members/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getMyNotifications(@AuthenticationPrincipal LoginMember loginMember) {
        return notificationService.getMyNotifications(loginMember.memberId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal LoginMember loginMember) {
        return Map.of("unreadCount", notificationService.getUnreadCount(loginMember.memberId()));
    }

    @PostMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal LoginMember loginMember) {
        notificationService.markAllRead(loginMember.memberId());
    }
}
