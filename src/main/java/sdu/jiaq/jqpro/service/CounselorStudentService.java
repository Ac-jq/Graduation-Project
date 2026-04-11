package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.counselor.CounselorStudentSummaryResponse;

import java.util.List;

/**
 * Counselor student service.
 */
public interface CounselorStudentService {

    List<CounselorStudentSummaryResponse> listCurrentCounselorStudents();
}
