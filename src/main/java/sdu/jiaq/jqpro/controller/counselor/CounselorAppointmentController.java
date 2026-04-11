package sdu.jiaq.jqpro.controller.counselor;

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
import sdu.jiaq.jqpro.dto.appointment.AppointmentActionRequest;
import sdu.jiaq.jqpro.dto.appointment.AppointmentResponse;
import sdu.jiaq.jqpro.service.AppointmentService;

import java.util.List;

/**
 * 咨询师预约处理接口。
 */
@RestController
@RequestMapping("/api/counselor/appointments")
@SaCheckRole(RoleConstants.COUNSELOR)
public class CounselorAppointmentController {

    private final AppointmentService appointmentService;

    public CounselorAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public Result<List<AppointmentResponse>> listAppointments() {
        return Result.success(appointmentService.listCounselorAppointments());
    }

    @PostMapping("/{appointmentId}/accept")
    public Result<AppointmentResponse> accept(@PathVariable Long appointmentId,
                                              @Valid @RequestBody(required = false) AppointmentActionRequest request) {
        AppointmentActionRequest actualRequest = request == null ? new AppointmentActionRequest() : request;
        return Result.success("预约接单成功", appointmentService.acceptAppointment(appointmentId, actualRequest));
    }

    @PostMapping("/{appointmentId}/reject")
    public Result<AppointmentResponse> reject(@PathVariable Long appointmentId,
                                              @Valid @RequestBody(required = false) AppointmentActionRequest request) {
        AppointmentActionRequest actualRequest = request == null ? new AppointmentActionRequest() : request;
        return Result.success("预约拒绝成功", appointmentService.rejectAppointment(appointmentId, actualRequest));
    }
}
