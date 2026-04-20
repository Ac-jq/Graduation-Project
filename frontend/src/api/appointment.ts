import { get, post } from './http'
import type {
  Appointment,
  AppointmentActionRequest,
  AppointmentCounselorOption,
  AppointmentSlot,
  CreateAppointmentRequest
} from './types'

export function fetchStudentAppointmentCounselorsApi(): Promise<AppointmentCounselorOption[]> {
  return get<AppointmentCounselorOption[]>('/student/appointments/counselors')
}

export function fetchStudentAppointmentSlotsApi(counselorId: number, date: string): Promise<AppointmentSlot[]> {
  return get<AppointmentSlot[]>('/student/appointments/slots', {
    params: {
      counselorId,
      date
    }
  })
}

export function fetchStudentAppointmentsApi(): Promise<Appointment[]> {
  return get<Appointment[]>('/student/appointments')
}

export function createStudentAppointmentApi(payload: CreateAppointmentRequest): Promise<Appointment> {
  return post<Appointment>('/student/appointments', payload)
}

export function fetchCounselorAppointmentsApi(): Promise<Appointment[]> {
  return get<Appointment[]>('/counselor/appointments')
}

export function acceptAppointmentApi(appointmentId: number, payload: AppointmentActionRequest): Promise<Appointment> {
  return post<Appointment>(`/counselor/appointments/${appointmentId}/accept`, payload)
}

export function rejectAppointmentApi(appointmentId: number, payload: AppointmentActionRequest): Promise<Appointment> {
  return post<Appointment>(`/counselor/appointments/${appointmentId}/reject`, payload)
}
