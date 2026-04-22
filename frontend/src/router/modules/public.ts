import type { RouteRecordRaw } from 'vue-router'
import LoginRoute from '@/views/public/LoginRoute.vue'
import RegisterRoute from '@/views/public/RegisterRoute.vue'
import ForbiddenPage from '@/views/public/ForbiddenPage.vue'
import NotFoundPage from '@/views/public/NotFoundPage.vue'
import RootEntryPage from '@/views/public/RootEntryPage.vue'

export const publicRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'root-entry',
    component: RootEntryPage
  },
  {
    path: '/login',
    name: 'login',
    component: LoginRoute,
    meta: {
      guestOnly: true,
      isAuthPage: true
    }
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterRoute,
    meta: {
      guestOnly: true,
      isAuthPage: true
    }
  },
  {
    path: '/forbidden',
    name: 'forbidden',
    component: ForbiddenPage
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundPage
  }
]
