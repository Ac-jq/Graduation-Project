import { createRouter, createWebHistory } from 'vue-router'
import { applyRouteGuards } from './guards'
import { publicRoutes } from './modules/public'
import { studentRoutes } from './modules/student'
import { counselorRoutes } from './modules/counselor'
import { adminRoutes } from './modules/admin'

const router = createRouter({
  history: createWebHistory(),
  routes: [...publicRoutes, ...studentRoutes, ...counselorRoutes, ...adminRoutes]
})

router.beforeEach((to, from, next) => {
  void applyRouteGuards(to, from, next)
})

export default router
