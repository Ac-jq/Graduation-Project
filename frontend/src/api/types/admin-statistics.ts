export interface NamedMetric {
  name: string
  count: number
}

export interface OverviewStatistics {
  studentCount: number
  counselorCount: number
  scaleReportCount: number
  aiSessionCount: number
  appointmentCount: number
  resourceCount: number
  publishedResourceCount: number
  resourceViewCount: number
  favoriteCount: number
  dailyActiveUsers: number
}

export interface AssessmentScaleSummary {
  scaleId: number
  scaleName: string
  participantCount: number
  reportCount: number
  averageScore: number
}

export interface AssessmentCompareSummary {
  sampleCount: number
  averageDelta: number
  improvedCount: number
  stableCount: number
  worsenedCount: number
  smallSampleWarning: boolean
}

export interface AssessmentStatistics {
  totalReports: number
  participantCount: number
  averageScore: number
  levelDistribution: NamedMetric[]
  scales: AssessmentScaleSummary[]
  compareSummary: AssessmentCompareSummary
}

export interface ResourceCategoryStatistics {
  categoryId: number
  categoryName: string
  resourceCount: number
  publishedCount: number
  viewCount: number
  favoriteCount: number
}

export interface TopResourceStatistics {
  resourceId: number
  title: string
  categoryName: string
  viewCount: number
  favoriteCount: number
}

export interface ResourceStatistics {
  resourceCount: number
  publishedCount: number
  totalViews: number
  totalFavorites: number
  categories: ResourceCategoryStatistics[]
  topResources: TopResourceStatistics[]
}

export interface CounselorAppointmentStatistics {
  counselorUserId: number
  counselorName: string
  totalCount: number
  acceptedCount: number
  rejectedCount: number
  pendingCount: number
}

export interface AppointmentStatistics {
  totalCount: number
  acceptedCount: number
  rejectedCount: number
  pendingCount: number
  collegeDistribution: NamedMetric[]
  counselorLoads: CounselorAppointmentStatistics[]
}

export interface StatisticsExportQuery {
  dimension: 'college' | 'grade' | 'gender'
}

export interface StatisticsExportRow {
  dimension: string
  dimensionValue: string
  studentCount: number
  reportCount: number
  averageScore: number
  aiSessionCount: number
  appointmentCount: number
  resourceViewCount: number
  favoriteCount: number
}