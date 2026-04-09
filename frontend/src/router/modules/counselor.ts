import type { RouteRecordRaw } from 'vue-router'
import RoleWorkspaceLayout from '@/layouts/RoleWorkspaceLayout.vue'
import CounselorAppointmentRoute from '@/views/counselor/CounselorAppointmentRoute.vue'
import CounselorConsultChatRoute from '@/views/counselor/CounselorConsultChatRoute.vue'
import CounselorHomeRoute from '@/views/counselor/CounselorHomeRoute.vue'
import CounselorNotificationRoute from '@/views/counselor/CounselorNotificationRoute.vue'
import CounselorStudentAiSessionListRoute from '@/views/counselor/CounselorStudentAiSessionListRoute.vue'
import CounselorStudentAiSessionRoute from '@/views/counselor/CounselorStudentAiSessionRoute.vue'
import CounselorStudentListRoute from '@/views/counselor/CounselorStudentListRoute.vue'
import CounselorStudentReportDetailRoute from '@/views/counselor/CounselorStudentReportDetailRoute.vue'
import CounselorStudentReportListRoute from '@/views/counselor/CounselorStudentReportListRoute.vue'
import AccountSecurityRoute from '@/views/shared/AccountSecurityRoute.vue'

const counselorMeta = {
  requiresAuth: true,
  roles: ['COUNSELOR'] as const
}

export const counselorRoutes: RouteRecordRaw[] = [
  {
    path: '/counselor',
    component: RoleWorkspaceLayout,
    meta: counselorMeta,
    children: [
      {
        path: '',
        name: 'counselor-home',
        component: CounselorHomeRoute,
        meta: {
          ...counselorMeta,
          description: 'Counselor workspace'
        }
      },
      { path: 'account', name: 'counselor-account', component: AccountSecurityRoute, meta: counselorMeta },
      { path: 'students', name: 'counselor-students', component: CounselorStudentListRoute, meta: counselorMeta },
      { path: 'students/:studentUserId/reports', name: 'counselor-student-reports', component: CounselorStudentReportListRoute, meta: counselorMeta },
      { path: 'students/:studentUserId/reports/:reportId', name: 'counselor-student-report-detail', component: CounselorStudentReportDetailRoute, meta: counselorMeta },
      { path: 'students/:studentUserId/ai-sessions', name: 'counselor-student-ai-sessions', component: CounselorStudentAiSessionListRoute, meta: counselorMeta },
      { path: 'students/:studentUserId/ai-sessions/:sessionId', name: 'counselor-student-ai-session-detail', component: CounselorStudentAiSessionRoute, meta: counselorMeta },
      { path: 'appointments', name: 'counselor-appointments', component: CounselorAppointmentRoute, meta: counselorMeta },
      { path: 'notifications', name: 'counselor-notifications', component: CounselorNotificationRoute, meta: counselorMeta },
      { path: 'chat/appointments/:appointmentId', name: 'counselor-chat', component: CounselorConsultChatRoute, meta: counselorMeta }
    ]
  }
]
