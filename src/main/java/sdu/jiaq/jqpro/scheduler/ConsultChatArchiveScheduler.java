package sdu.jiaq.jqpro.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sdu.jiaq.jqpro.entity.ConsultChatSession;
import sdu.jiaq.jqpro.mapper.ConsultChatSessionMapper;
import sdu.jiaq.jqpro.service.ConsultChatService;

import java.time.LocalDateTime;

/**
 * Periodically archives expired private chat rooms so the appointment lifecycle
 * matches PRD expectations without waiting for a user-triggered read.
 */
@Component
public class ConsultChatArchiveScheduler {

    private final ConsultChatSessionMapper consultChatSessionMapper;
    private final ConsultChatService consultChatService;

    public ConsultChatArchiveScheduler(ConsultChatSessionMapper consultChatSessionMapper,
                                       ConsultChatService consultChatService) {
        this.consultChatSessionMapper = consultChatSessionMapper;
        this.consultChatService = consultChatService;
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 30000)
    public void archiveExpiredChatSessions() {
        consultChatSessionMapper.selectList(new LambdaQueryWrapper<ConsultChatSession>()
                        .eq(ConsultChatSession::getSealedFlag, 0)
                        .lt(ConsultChatSession::getCloseTime, LocalDateTime.now()))
                .stream()
                .map(ConsultChatSession::getAppointmentId)
                .distinct()
                .forEach(consultChatService::archiveIfExpired);
    }
}
