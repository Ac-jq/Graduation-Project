package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.notification.NotificationResponse;

import java.util.List;

/**
 * 通知服务。
 */
public interface NotificationService {

    List<NotificationResponse> listCurrentUserNotifications();

    void markRead(Long notificationId);

    void markAllRead();

    void pushNotification(Long receiverUserId, String title, String contentText);
}
