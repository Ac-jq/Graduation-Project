package sdu.jiaq.jqpro.dto.appointment;

import lombok.Builder;
import lombok.Data;

/**
 * 学生预约页的咨询师选项。
 */
@Data
@Builder
public class AppointmentCounselorOptionResponse {

    private Long counselorUserId;

    private String counselorName;

    private String counselorNo;
}
