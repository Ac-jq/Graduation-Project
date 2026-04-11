package sdu.jiaq.jqpro.dto.appointment;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 预约处理请求。
 */
@Data
public class AppointmentActionRequest {

    @Size(max = 255, message = "处理说明长度不能超过255")
    private String resultMessage;
}
