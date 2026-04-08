package sdu.jiaq.jqpro.controller.assessment;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.assessment.AnswerSaveRequest;
import sdu.jiaq.jqpro.dto.assessment.ScaleDetailResponse;
import sdu.jiaq.jqpro.dto.assessment.ScaleQuestionPageResponse;
import sdu.jiaq.jqpro.dto.assessment.ScaleSessionResponse;
import sdu.jiaq.jqpro.dto.assessment.ScaleSummaryResponse;
import sdu.jiaq.jqpro.dto.assessment.SubmitScaleResponse;
import sdu.jiaq.jqpro.service.AssessmentService;

import java.util.List;

/**
 * 量表测评接口。
 */
@RestController
@RequestMapping("/api/scales")
@SaCheckRole(RoleConstants.STUDENT)
public class ScaleController {

    private final AssessmentService assessmentService;

    public ScaleController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public Result<List<ScaleSummaryResponse>> listScales() {
        return Result.success(assessmentService.listActiveScales());
    }

    @GetMapping("/{scaleId}")
    public Result<ScaleDetailResponse> getScaleDetail(@PathVariable Long scaleId) {
        return Result.success(assessmentService.getScaleDetail(scaleId));
    }

    @PostMapping("/{scaleId}/sessions/draft")
    public Result<ScaleSessionResponse> createOrGetDraftSession(@PathVariable Long scaleId) {
        return Result.success("草稿会话已就绪", assessmentService.createOrGetDraftSession(scaleId));
    }

    @GetMapping("/sessions/{sessionId}/questions")
    public Result<ScaleQuestionPageResponse> getQuestionPage(@PathVariable Long sessionId,
                                                             @RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize) {
        return Result.success(assessmentService.getQuestionPage(sessionId, pageNum, pageSize));
    }

    @PutMapping("/sessions/{sessionId}/answers")
    public Result<ScaleSessionResponse> saveAnswers(@PathVariable Long sessionId,
                                                    @Valid @RequestBody AnswerSaveRequest request) {
        return Result.success("答案保存成功", assessmentService.saveAnswers(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/submit")
    public Result<SubmitScaleResponse> submit(@PathVariable Long sessionId) {
        return Result.success("量表提交成功", assessmentService.submit(sessionId));
    }
}
