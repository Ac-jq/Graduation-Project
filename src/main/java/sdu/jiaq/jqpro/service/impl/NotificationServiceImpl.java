package sdu.jiaq.jqpro.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sdu.jiaq.jqpro.common.exception.BusinessException;
import sdu.jiaq.jqpro.common.util.SecurityUtil;
import sdu.jiaq.jqpro.dto.notification.NotificationResponse;
import sdu.jiaq.jqpro.entity.SiteNotification;
import sdu.jiaq.jqpro.mapper.SiteNotificationMapper;
import sdu.jiaq.jqpro.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知服务实现。
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final SiteNotificationMapper siteNotificationMapper;

    public NotificationServiceImpl(SiteNotificationMapper siteNotificationMapper) {
        this.siteNotificationMapper = siteNotificationMapper;
    }

    @Override
    public List<NotificationResponse> listCurrentUserNotifications() {
        Long userId = SecurityUtil.getCurrentUserId();
        return siteNotificationMapper.selectList(new LambdaQueryWrapper<SiteNotification>()
                        .eq(SiteNotification::getReceiverUserId, userId)
                        .orderByDesc(SiteNotification::getCreatedAt))
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long notificationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        SiteNotification notification = getRequiredNotification(notificationId);
        if (!userId.equals(notification.getReceiverUserId())) {
            throw new BusinessException("无权操作该通知");
        }
        if (notification.getReadFlag() == 0) {
            notification.setReadFlag(1);
            notification.setReadAt(LocalDateTime.now());
            siteNotificationMapper.updateById(notification);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<SiteNotification> notifications = siteNotificationMapper.selectList(new LambdaQueryWrapper<SiteNotification>()
                .eq(SiteNotification::getReceiverUserId, userId)
                .eq(SiteNotification::getReadFlag, 0));
        LocalDateTime now = LocalDateTime.now();
        for (SiteNotification notification : notifications) {
            notification.setReadFlag(1);
            notification.setReadAt(now);
            siteNotificationMapper.updateById(notification);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pushNotification(Long receiverUserId, String title, String contentText) {
        SiteNotification notification = new SiteNotification();
        notification.setReceiverUserId(receiverUserId);
        notification.setTitle(title);
        notification.setContentText(contentText);
        notification.setReadFlag(0);
        siteNotificationMapper.insert(notification);
    }

    private SiteNotification getRequiredNotification(Long notificationId) {
        SiteNotification notification = siteNotificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        return notification;
    }

    private NotificationResponse buildResponse(SiteNotification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getId())
                .title(notification.getTitle())
                .contentText(notification.getContentText())
                .read(notification.getReadFlag() != null && notification.getReadFlag() == 1)
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
