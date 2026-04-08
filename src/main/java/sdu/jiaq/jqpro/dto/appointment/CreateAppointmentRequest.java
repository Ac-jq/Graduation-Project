package sdu.jiaq.jqpro.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发起预约请求。
 */
@Data
public class CreateAppointmentRequest {

    @NotNull(message = "预约时段不能为空")
    private Long slotId;

    @NotBlank(message = "问题简介不能为空")
    @Size(max = 500, message = "问题简介长度不能超过500")
    private String issueSummary;
}
