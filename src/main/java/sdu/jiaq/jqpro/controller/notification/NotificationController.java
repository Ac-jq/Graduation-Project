package sdu.jiaq.jqpro.controller.notification;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.notification.NotificationResponse;
import sdu.jiaq.jqpro.service.NotificationService;

import java.util.List;

/**
 * 通知接口。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<List<NotificationResponse>> listNotifications() {
        return Result.success(notificationService.listCurrentUserNotifications());
    }

    @PostMapping("/{notificationId}/read")
    public Result<Void> markRead(@PathVariable Long notificationId) {
        notificationService.markRead(notificationId);
        return Result.success("通知已读成功", null);
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead();
        return Result.success("通知已全部标记为已读", null);
    }
}
