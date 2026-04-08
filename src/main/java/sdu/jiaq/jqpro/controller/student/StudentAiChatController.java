package sdu.jiaq.jqpro.controller.student;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.aichat.AiChatMessageResponse;
import sdu.jiaq.jqpro.dto.aichat.AiChatSessionResponse;
import sdu.jiaq.jqpro.dto.aichat.CreateAiChatSessionRequest;
import sdu.jiaq.jqpro.dto.aichat.SendAiChatMessageRequest;
import sdu.jiaq.jqpro.dto.aichat.SendAiChatMessageResponse;
import sdu.jiaq.jqpro.service.AiChatService;

import java.util.List;

/**
 * 学生 AI 会话接口。
 */
@RestController
@RequestMapping("/api/student/ai-sessions")
@SaCheckRole(RoleConstants.STUDENT)
public class StudentAiChatController {

    private final AiChatService aiChatService;

    public StudentAiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping
    public Result<AiChatSessionResponse> createSession(@Valid @RequestBody(required = false) CreateAiChatSessionRequest request) {
        CreateAiChatSessionRequest actualRequest = request == null ? new CreateAiChatSessionRequest() : request;
        return Result.success("AI会话创建成功", aiChatService.createSession(actualRequest));
    }

    @GetMapping
    public Result<List<AiChatSessionResponse>> listSessions() {
        return Result.success(aiChatService.listCurrentStudentSessions());
    }

    @GetMapping("/{sessionId}/messages")
    public Result<List<AiChatMessageResponse>> listMessages(@PathVariable Long sessionId) {
        return Result.success(aiChatService.listCurrentStudentMessages(sessionId));
    }

    @PostMapping("/{sessionId}/messages")
    public Result<SendAiChatMessageResponse> sendMessage(@PathVariable Long sessionId,
                                                         @Valid @RequestBody SendAiChatMessageRequest request) {
        return Result.success("消息发送成功", aiChatService.sendMessage(sessionId, request));
    }
}
