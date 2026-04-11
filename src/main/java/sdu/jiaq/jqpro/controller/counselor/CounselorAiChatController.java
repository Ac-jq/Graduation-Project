package sdu.jiaq.jqpro.controller.counselor;

import cn.dev33.satoken.annotation.SaCheckRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.aichat.AiChatMessageResponse;
import sdu.jiaq.jqpro.dto.aichat.AiChatSessionResponse;
import sdu.jiaq.jqpro.service.AiChatService;

import java.util.List;

/**
 * 咨询师查看学生 AI 会话接口。
 */
@RestController
@RequestMapping("/api/counselor/students/{studentUserId}/ai-sessions")
@SaCheckRole(RoleConstants.COUNSELOR)
public class CounselorAiChatController {

    private final AiChatService aiChatService;

    public CounselorAiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping
    public Result<List<AiChatSessionResponse>> listStudentSessions(@PathVariable Long studentUserId) {
        return Result.success(aiChatService.listCounselorStudentSessions(studentUserId));
    }

    @GetMapping("/{sessionId}/messages")
    public Result<List<AiChatMessageResponse>> listStudentMessages(@PathVariable Long studentUserId,
                                                                   @PathVariable Long sessionId) {
        return Result.success(aiChatService.listCounselorStudentMessages(studentUserId, sessionId));
    }
}
