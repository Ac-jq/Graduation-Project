package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.assessment.ReportDetailResponse;
import sdu.jiaq.jqpro.dto.assessment.ReportSummaryResponse;

import java.util.List;

/**
 * 报告服务。
 */
public interface ReportService {

    List<ReportSummaryResponse> listCurrentStudentReports();

    ReportDetailResponse getCurrentStudentReportDetail(Long reportId);

    List<ReportSummaryResponse> listCounselorStudentReports(Long studentUserId);

    ReportDetailResponse getCounselorStudentReportDetail(Long studentUserId, Long reportId);
}
