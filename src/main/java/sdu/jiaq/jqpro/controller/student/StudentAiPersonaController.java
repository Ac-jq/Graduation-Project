package sdu.jiaq.jqpro.controller.student;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.aichat.AiPersonaSettingResponse;
import sdu.jiaq.jqpro.dto.aichat.UpdateAiPersonaSettingRequest;
import sdu.jiaq.jqpro.service.AiPersonaSettingService;

/**
 * Student AI mentor persona endpoints.
 */
@RestController
@RequestMapping("/api/student/ai-persona")
@SaCheckRole(RoleConstants.STUDENT)
public class StudentAiPersonaController {

    private final AiPersonaSettingService aiPersonaSettingService;

    public StudentAiPersonaController(AiPersonaSettingService aiPersonaSettingService) {
        this.aiPersonaSettingService = aiPersonaSettingService;
    }

    @GetMapping
    public Result<AiPersonaSettingResponse> getPersona() {
        return Result.success(aiPersonaSettingService.getCurrentStudentPersona());
    }

    @PutMapping
    public Result<AiPersonaSettingResponse> updatePersona(@Valid @RequestBody UpdateAiPersonaSettingRequest request) {
        return Result.success("AI导师设定已更新", aiPersonaSettingService.updateCurrentStudentPersona(request));
    }
}
