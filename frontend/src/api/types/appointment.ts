export interface AppointmentCounselorOption {
  counselorUserId: number
  counselorName: string
  counselorNo: string | null
}

export interface AppointmentSlot {
  slotId: number | null
  counselorUserId: number
  counselorName: string | null
  startTime: string
  endTime: string
  status: string
  isBooked: boolean
  isSelectable: boolean
  timeLabel: string
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
  chatAvailable: boolean
  chatEnded: boolean
  chatStatus: string | null
  chatSealed: boolean
}

export interface CreateAppointmentRequest {
  slotId: number
  issueSummary: string
}

export interface AppointmentActionRequest {
  resultMessage?: string | null
}
