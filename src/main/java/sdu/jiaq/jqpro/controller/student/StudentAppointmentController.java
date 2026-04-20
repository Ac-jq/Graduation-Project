package sdu.jiaq.jqpro.controller.student;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.appointment.AppointmentCounselorOptionResponse;
import sdu.jiaq.jqpro.dto.appointment.AppointmentResponse;
import sdu.jiaq.jqpro.dto.appointment.AppointmentSlotResponse;
import sdu.jiaq.jqpro.dto.appointment.CreateAppointmentRequest;
import sdu.jiaq.jqpro.service.AppointmentService;

import java.time.LocalDate;
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

    @GetMapping("/counselors")
    public Result<List<AppointmentCounselorOptionResponse>> listCounselors() {
        return Result.success(appointmentService.listAvailableCounselors());
    }

    @GetMapping("/slots")
    public Result<List<AppointmentSlotResponse>> listSlots(@RequestParam("counselorId") Long counselorId,
                                                           @RequestParam("date")
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(appointmentService.listDailySlots(counselorId, date));
    }

    @GetMapping
    public Result<List<AppointmentResponse>> listAppointments() {
        return Result.success(appointmentService.listStudentAppointments());
    }

    @PostMapping
    public Result<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        return Result.success("预约创建成功", appointmentService.createAppointment(request));
    }
}
