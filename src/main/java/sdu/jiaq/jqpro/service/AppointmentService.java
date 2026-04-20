package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.appointment.AppointmentActionRequest;
import sdu.jiaq.jqpro.dto.appointment.AppointmentCounselorOptionResponse;
import sdu.jiaq.jqpro.dto.appointment.AppointmentResponse;
import sdu.jiaq.jqpro.dto.appointment.AppointmentSlotResponse;
import sdu.jiaq.jqpro.dto.appointment.CreateAppointmentRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约服务。
 */
public interface AppointmentService {

    List<AppointmentCounselorOptionResponse> listAvailableCounselors();

    List<AppointmentSlotResponse> listDailySlots(Long counselorUserId, LocalDate date);

    List<AppointmentResponse> listStudentAppointments();

    AppointmentResponse createAppointment(CreateAppointmentRequest request);

    List<AppointmentResponse> listCounselorAppointments();

    AppointmentResponse acceptAppointment(Long appointmentId, AppointmentActionRequest request);

    AppointmentResponse rejectAppointment(Long appointmentId, AppointmentActionRequest request);
}
