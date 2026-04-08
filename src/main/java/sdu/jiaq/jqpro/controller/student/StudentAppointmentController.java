package sdu.jiaq.jqpro.controller.student;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.appointment.AppointmentResponse;
import sdu.jiaq.jqpro.dto.appointment.AppointmentSlotResponse;
import sdu.jiaq.jqpro.dto.appointment.CreateAppointmentRequest;
import sdu.jiaq.jqpro.service.AppointmentService;

import java.util.List;

/**
 * 学生预约接口。
 */
@RestController
@RequestMapping("/api/student/appointments")
@SaCheckRole(RoleConstants.STUDENT)
public class StudentAppointmentController {

    private final AppointmentService appointmentService;

    public StudentAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/slots")
    public Result<List<AppointmentSlotResponse>> listSlots() {
        return Result.success(appointmentService.listOpenSlots());
    }

    // Student-side appointment list page uses this endpoint to render status and chat entry.
    @GetMapping
    public Result<List<AppointmentResponse>> listAppointments() {
        return Result.success(appointmentService.listStudentAppointments());
    }

    @PostMapping
    public Result<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        return Result.success("预约创建成功", appointmentService.createAppointment(request));
    }
}
