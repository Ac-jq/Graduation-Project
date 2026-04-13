import type { RouteRecordRaw } from 'vue-router'
import RoleWorkspaceLayout from '@/layouts/RoleWorkspaceLayout.vue'
import StudentAiSessionListRoute from '@/views/student/StudentAiSessionListRoute.vue'
import StudentAiSessionRoute from '@/views/student/StudentAiSessionRoute.vue'
import StudentAppointmentListRoute from '@/views/student/StudentAppointmentListRoute.vue'
import StudentAppointmentSlotRoute from '@/views/student/StudentAppointmentSlotRoute.vue'
import StudentAssessmentSessionRoute from '@/views/student/StudentAssessmentSessionRoute.vue'
import StudentAssessmentResultRoute from '@/views/student/StudentAssessmentResultRoute.vue'
import StudentConsultChatRoute from '@/views/student/StudentConsultChatRoute.vue'
import StudentFavoriteRoute from '@/views/student/StudentFavoriteRoute.vue'
import StudentHomeRoute from '@/views/student/StudentHomeRoute.vue'
import StudentNotificationRoute from '@/views/student/StudentNotificationRoute.vue'
import StudentProfileRoute from '@/views/student/StudentProfileRoute.vue'
import StudentReportDetailRoute from '@/views/student/StudentReportDetailRoute.vue'
import StudentReportListRoute from '@/views/student/StudentReportListRoute.vue'
import StudentResourceDetailRoute from '@/views/student/StudentResourceDetailRoute.vue'
import StudentResourceListRoute from '@/views/student/StudentResourceListRoute.vue'
import StudentScaleIntroRoute from '@/views/student/StudentScaleIntroRoute.vue'
import StudentScaleListRoute from '@/views/student/StudentScaleListRoute.vue'
import AccountSecurityRoute from '@/views/shared/AccountSecurityRoute.vue'

const studentMeta = {
  requiresAuth: true,
  roles: ['STUDENT'] as const
}

export const studentRoutes: RouteRecordRaw[] = [
  {
    path: '/student',
    component: RoleWorkspaceLayout,
    meta: studentMeta,
    children: [
      {
        path: '',
        name: 'student-home',
        component: StudentHomeRoute,
        meta: {
          ...studentMeta,
          description: 'Student workspace'
        }
      },
      { path: 'account', name: 'student-account', component: AccountSecurityRoute, meta: studentMeta },
      { path: 'profile', name: 'student-profile', component: StudentProfileRoute, meta: studentMeta },
      { path: 'scales', name: 'student-scales', component: StudentScaleListRoute, meta: studentMeta },
      { path: 'scales/:scaleId', name: 'student-scale-detail', component: StudentScaleIntroRoute, meta: studentMeta },
      { path: 'assessment-sessions/:sessionId', name: 'student-assessment-session', component: StudentAssessmentSessionRoute, meta: studentMeta },
      { path: 'assessment-results/:reportId', name: 'student-assessment-result', component: StudentAssessmentResultRoute, meta: studentMeta },
      { path: 'reports', name: 'student-reports', component: StudentReportListRoute, meta: studentMeta },
      { path: 'reports/:reportId', name: 'student-report-detail', component: StudentReportDetailRoute, meta: studentMeta },
      { path: 'ai-sessions', name: 'student-ai-sessions', component: StudentAiSessionListRoute, meta: studentMeta },
      { path: 'ai-sessions/:sessionId', name: 'student-ai-session-detail', component: StudentAiSessionRoute, meta: studentMeta },
      { path: 'appointments/slots', name: 'student-appointment-slots', component: StudentAppointmentSlotRoute, meta: studentMeta },
      { path: 'appointments', name: 'student-appointments', component: StudentAppointmentListRoute, meta: studentMeta },
      { path: 'resources', name: 'student-resources', component: StudentResourceListRoute, meta: studentMeta },
      { path: 'resources/:resourceId', name: 'student-resource-detail', component: StudentResourceDetailRoute, meta: studentMeta },
      { path: 'favorites', name: 'student-favorites', component: StudentFavoriteRoute, meta: studentMeta },
      { path: 'notifications', name: 'student-notifications', component: StudentNotificationRoute, meta: studentMeta },
      { path: 'chat/appointments/:appointmentId', name: 'student-chat', component: StudentConsultChatRoute, meta: studentMeta }
    ]
  }
]
