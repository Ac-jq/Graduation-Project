package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.appointment.AppointmentActionRequest;
import sdu.jiaq.jqpro.dto.appointment.AppointmentResponse;
import sdu.jiaq.jqpro.dto.appointment.AppointmentSlotResponse;
import sdu.jiaq.jqpro.dto.appointment.CreateAppointmentRequest;

import java.util.List;

/**
 * 预约服务。
 */
public interface AppointmentService {

    List<AppointmentSlotResponse> listOpenSlots();

    List<AppointmentResponse> listStudentAppointments();

    AppointmentResponse createAppointment(CreateAppointmentRequest request);

    List<AppointmentResponse> listCounselorAppointments();

    AppointmentResponse acceptAppointment(Long appointmentId, AppointmentActionRequest request);

    AppointmentResponse rejectAppointment(Long appointmentId, AppointmentActionRequest request);
}
