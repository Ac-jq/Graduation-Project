import { get } from './http'
import type { AppointmentStatistics, AssessmentStatistics, OverviewStatistics, ResourceStatistics, StatisticsExportQuery, StatisticsExportRow } from './types'

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

export function exportStatisticsApi(query: StatisticsExportQuery): Promise<StatisticsExportRow[]> {
  return get<StatisticsExportRow[]>('/admin/statistics/export', { params: query })
}