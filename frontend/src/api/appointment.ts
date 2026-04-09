import { get, post } from './http'
import type { Appointment, AppointmentActionRequest, AppointmentSlot, CreateAppointmentRequest } from './types'

export function fetchStudentAppointmentSlotsApi(): Promise<AppointmentSlot[]> {
  return get<AppointmentSlot[]>('/student/appointments/slots')
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