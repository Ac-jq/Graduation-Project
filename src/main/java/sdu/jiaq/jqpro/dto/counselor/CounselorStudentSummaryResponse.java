package sdu.jiaq.jqpro.dto.counselor;

import lombok.Builder;
import lombok.Data;

/**
 * Counselor-side student summary.
 */
@Data
@Builder
public class CounselorStudentSummaryResponse {

    private Long studentUserId;

    private String studentName;

    private String studentNo;

    private String college;

    private String grade;

    private String gender;
}
