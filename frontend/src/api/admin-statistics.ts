import { get, request } from './http'
import type { AppointmentStatistics, AssessmentStatistics, OverviewStatistics, ResourceStatistics, StatisticsExportQuery, StatisticsExportRow, UserEngagementStatistics } from './types'

export function fetchOverviewStatisticsApi(): Promise<OverviewStatistics> {
  return get<OverviewStatistics>('/admin/statistics/overview')
}

export function fetchAssessmentStatisticsApi(): Promise<AssessmentStatistics> {
  return get<AssessmentStatistics>('/admin/statistics/assessments')
}

export function fetchResourceStatisticsApi(): Promise<ResourceStatistics> {
  return get<ResourceStatistics>('/admin/statistics/resources')
}

export function fetchAppointmentStatisticsApi(): Promise<AppointmentStatistics> {
  return get<AppointmentStatistics>('/admin/statistics/appointments')
}

export function fetchUserEngagementStatisticsApi(): Promise<UserEngagementStatistics> {
  return get<UserEngagementStatistics>('/admin/statistics/engagements')
}

export function exportStatisticsApi(query: StatisticsExportQuery): Promise<StatisticsExportRow[]> {
  return get<StatisticsExportRow[]>('/admin/statistics/export', { params: query })
}

export function exportInterventionEffectReportApi(): Promise<Blob> {
  return request<Blob>({
    url: '/admin/statistics/export/intervention-effect',
    method: 'get',
    responseType: 'blob'
  })
}
