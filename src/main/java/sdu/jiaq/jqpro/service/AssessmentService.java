package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.assessment.AnswerSaveRequest;
import sdu.jiaq.jqpro.dto.assessment.ScaleDetailResponse;
import sdu.jiaq.jqpro.dto.assessment.ScaleQuestionPageResponse;
import sdu.jiaq.jqpro.dto.assessment.ScaleSessionResponse;
import sdu.jiaq.jqpro.dto.assessment.ScaleSummaryResponse;
import sdu.jiaq.jqpro.dto.assessment.SubmitScaleResponse;

import java.util.List;

/**
 * 量表测评服务。
 */
public interface AssessmentService {

    List<ScaleSummaryResponse> listActiveScales();

    ScaleDetailResponse getScaleDetail(Long scaleId);

    ScaleSessionResponse createOrGetDraftSession(Long scaleId);

    ScaleQuestionPageResponse getQuestionPage(Long sessionId, Integer pageNum, Integer pageSize);

    ScaleSessionResponse saveAnswers(Long sessionId, AnswerSaveRequest request);

    SubmitScaleResponse submit(Long sessionId);
}
