export interface AppointmentSlot {
  slotId: number
  counselorUserId: number
  counselorName: string | null
  startTime: string
  endTime: string
  status: string
}

export interface Appointment {
  appointmentId: number
  slotId: number
  studentUserId: number
  anonymousName: string
  counselorUserId: number | null
  counselorName: string | null
  issueSummary: string
  status: string
  resultMessage: string | null
  startTime: string
  endTime: string
  createdAt: string
}

export interface CreateAppointmentRequest {
  slotId: number
  issueSummary: string
}

export interface AppointmentActionRequest {
  resultMessage?: string | null
}