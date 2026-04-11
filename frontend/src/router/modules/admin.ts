import type { RouteRecordRaw } from 'vue-router'
import RoleWorkspaceLayout from '@/layouts/RoleWorkspaceLayout.vue'
import AdminHomeRoute from '@/views/admin/AdminHomeRoute.vue'
import AdminAiTaskRoute from '@/views/admin/AdminAiTaskRoute.vue'
import AdminAuditLogRoute from '@/views/admin/AdminAuditLogRoute.vue'
import AdminResourceDetailRoute from '@/views/admin/AdminResourceDetailRoute.vue'
import AdminResourceListRoute from '@/views/admin/AdminResourceListRoute.vue'
import AdminResourceMetaRoute from '@/views/admin/AdminResourceMetaRoute.vue'
import AdminScaleDetailRoute from '@/views/admin/AdminScaleDetailRoute.vue'
import AdminScaleListRoute from '@/views/admin/AdminScaleListRoute.vue'
import AdminStatisticsRoute from '@/views/admin/AdminStatisticsRoute.vue'
import AdminUserRoute from '@/views/admin/AdminUserRoute.vue'
import AccountSecurityRoute from '@/views/shared/AccountSecurityRoute.vue'

const adminMeta = {
  requiresAuth: true,
  roles: ['ADMIN'] as const
}

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: RoleWorkspaceLayout,
    meta: adminMeta,
    children: [
      {
        path: '',
        name: 'admin-home',
        component: AdminHomeRoute,
        meta: {
          ...adminMeta,
          description: 'Admin workspace'
        }
      },
      { path: 'account', name: 'admin-account', component: AccountSecurityRoute, meta: adminMeta },
      { path: 'users', name: 'admin-users', component: AdminUserRoute, meta: adminMeta },
      { path: 'scales', name: 'admin-scales', component: AdminScaleListRoute, meta: adminMeta },
      { path: 'scales/new', name: 'admin-scale-new', component: AdminScaleDetailRoute, meta: adminMeta },
      { path: 'scales/:scaleId', name: 'admin-scale-detail', component: AdminScaleDetailRoute, meta: adminMeta },
      { path: 'resources', name: 'admin-resources', component: AdminResourceListRoute, meta: adminMeta },
      { path: 'resources/meta', name: 'admin-resource-meta', component: AdminResourceMetaRoute, meta: adminMeta },
      { path: 'resources/new', name: 'admin-resource-new', component: AdminResourceDetailRoute, meta: adminMeta },
      { path: 'resources/:resourceId', name: 'admin-resource-detail', component: AdminResourceDetailRoute, meta: adminMeta },
      { path: 'statistics', name: 'admin-statistics', component: AdminStatisticsRoute, meta: adminMeta },
      { path: 'ai-tasks', name: 'admin-ai-tasks', component: AdminAiTaskRoute, meta: adminMeta },
      { path: 'audit-logs', name: 'admin-audit-logs', component: AdminAuditLogRoute, meta: adminMeta }
    ]
  }
]
