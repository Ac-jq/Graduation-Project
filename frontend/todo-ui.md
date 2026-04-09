# todo-ui

## 第一阶段：前端基础设施建设

- [x] 初始化前端工程：使用 Vue 3 + Vite + TypeScript 创建项目根结构，补齐 `src/`、`public/`、`env`、`tsconfig`、`vite.config` 基础文件。
  索引：依赖后端接口 [无直接接口依赖]，对应 UI-SDD [1.文档边界, 5.P0 - 脚手架与 Axios / Router 基建]

- [x] 初始化依赖与入口：接入 `vue-router`、`pinia`、`axios`，创建 `src/main.ts`、应用挂载入口与基础插件注册流程。
  索引：依赖后端接口 [POST /api/auth/login, GET /api/auth/current-user]，对应 UI-SDD [3.1 Store 划分原则, 5.P0 - 脚手架与 Axios / Router 基建]

- [x] 建立路由目录结构：创建 `src/router/index.ts`、`src/router/guards.ts`、`src/router/route-meta.ts`、`src/router/modules/public.ts|student.ts|counselor.ts|admin.ts`。
  索引：依赖后端接口 [GET /api/auth/current-user]，对应 UI-SDD [2.1 总体路由树, 2.2 路由守卫规则, 2.4 推荐路由分层]

- [x] 配置全局前置守卫：实现 `requiresAuth`、`roles`、`guestOnly` 校验，未登录跳 `/login`，无权限跳 `/forbidden`。
  索引：依赖后端接口 [GET /api/auth/current-user]，对应 UI-SDD [2.2 路由守卫规则, 2.3 路由权限校验逻辑]

- [x] 建立按角色的默认首页重定向逻辑：根据 `roleCode` 将已登录用户重定向到学生、咨询师、管理员主工作区。
  索引：依赖后端接口 [POST /api/auth/login, GET /api/auth/current-user]，对应 UI-SDD [2.3 路由权限校验逻辑, 3.2 authStore]

- [x] 封装 Axios 实例：创建 `src/api/http.ts`，统一 `baseURL`、`Authorization` 注入、超时控制、响应解包与错误归一化。
  索引：依赖后端接口 [全部 `/api/**` 接口]，对应 UI-SDD [4.7 请求层标准化, 5.P0 - 脚手架与 Axios / Router 基建]

- [x] 配置 Axios 响应拦截器：统一处理 `401/403/600/500`，登录失效时清空会话并跳转。
  索引：依赖后端接口 [全部 `/api/**` 接口]，对应 UI-SDD [4.7 请求层标准化, 6.3 前端错误恢复原则]

- [x] 初始化 Pinia：创建 `src/stores/index.ts` 与各业务 store 占位文件，形成统一状态入口。
  索引：依赖后端接口 [POST /api/auth/login, GET /api/auth/current-user]，对应 UI-SDD [3.1 Store 划分原则, 5.P1 - 状态管理与接口定义]

- [x] 建立本地会话恢复机制：持久化 `token/roleCode/userId`，应用启动时执行 `restoreSession()` 与 `fetchCurrentUser()`。
  索引：依赖后端接口 [GET /api/auth/current-user, POST /api/auth/logout]，对应 UI-SDD [3.2 authStore, 6.2 本地缓存策略]

- [x] 创建基础类型目录：建立 `src/types/common.ts`、分页类型、统一 `ApiResult<T>`、角色与路由 Meta 类型。
  索引：依赖后端接口 [全部 `/api/**` 接口]，对应 UI-SDD [4.7 请求层标准化, 5.P1 - 状态管理与接口定义]

## 第二阶段：API 请求层级开发

- [x] 创建 `src/api/auth.ts`，封装登录、登出、改密、当前用户接口。
  索引：依赖后端接口 [POST /api/auth/login, POST /api/auth/logout, POST /api/auth/change-password, GET /api/auth/current-user]，对应 UI-SDD [4.1 认证与用户模块, 3.2 authStore]

- [x] 创建 `src/api/user.ts`，封装学生档案、咨询师绑定学生列表、管理员用户管理接口。
  索引：依赖后端接口 [GET /api/student/profile/me, PUT /api/student/profile/me, GET /api/counselor/students, GET /api/admin/users, POST /api/admin/users/counselors, POST /api/admin/users/{userId}/enable, POST /api/admin/users/{userId}/disable, POST /api/admin/users/{userId}/reset-password]，对应 UI-SDD [4.1 认证与用户模块, 3.3 userStore, 3.9 adminStore]

- [x] 创建 `src/api/assessment.ts`，封装量表列表、量表详情、草稿创建、题目分页、答案保存、提交接口。
  索引：依赖后端接口 [GET /api/scales, GET /api/scales/{scaleId}, POST /api/scales/{scaleId}/sessions/draft, GET /api/scales/sessions/{sessionId}/questions, PUT /api/scales/sessions/{sessionId}/answers, POST /api/scales/sessions/{sessionId}/submit]，对应 UI-SDD [4.2 测评与报告模块, 3.4 assessmentStore]

- [x] 在 `src/api/assessment.ts` 中继续封装学生报告与咨询师报告查询接口。
  索引：依赖后端接口 [GET /api/student/reports, GET /api/student/reports/{reportId}, GET /api/counselor/students/{studentUserId}/reports, GET /api/counselor/students/{studentUserId}/reports/{reportId}]，对应 UI-SDD [4.2 测评与报告模块, 3.4 assessmentStore]

- [x] 创建 `src/api/ai-chat.ts`，封装学生 AI 会话列表、创建会话、消息查询、发送消息接口。
  索引：依赖后端接口 [POST /api/student/ai-sessions, GET /api/student/ai-sessions, GET /api/student/ai-sessions/{sessionId}/messages, POST /api/student/ai-sessions/{sessionId}/messages]，对应 UI-SDD [4.3 AI 会话模块, 3.5 aiChatStore]

- [x] 在 `src/api/ai-chat.ts` 中继续封装咨询师查看学生 AI 会话与消息接口。
  索引：依赖后端接口 [GET /api/counselor/students/{studentUserId}/ai-sessions, GET /api/counselor/students/{studentUserId}/ai-sessions/{sessionId}/messages]，对应 UI-SDD [4.3 AI 会话模块, 3.5 aiChatStore]

- [x] 创建 `src/api/appointment.ts`，封装学生预约时段、学生预约列表、发起预约接口。
  索引：依赖后端接口 [GET /api/student/appointments/slots, GET /api/student/appointments, POST /api/student/appointments]，对应 UI-SDD [4.4 预约、通知、聊天室模块, 3.6 appointmentStore]

- [x] 在 `src/api/appointment.ts` 中继续封装咨询师预约列表、接单、拒绝接口。
  索引：依赖后端接口 [GET /api/counselor/appointments, POST /api/counselor/appointments/{appointmentId}/accept, POST /api/counselor/appointments/{appointmentId}/reject]，对应 UI-SDD [4.4 预约、通知、聊天室模块, 3.6 appointmentStore]

- [x] 创建 `src/api/notification.ts`，封装通知列表、单条已读、全部已读接口。
  索引：依赖后端接口 [GET /api/notifications, POST /api/notifications/{notificationId}/read, POST /api/notifications/read-all]，对应 UI-SDD [4.4 预约、通知、聊天室模块, 3.7 notificationStore]

- [x] 创建 `src/api/chat.ts`，封装聊天室会话查询、历史消息查询，以及 WebSocket 连接参数构造函数。
  索引：依赖后端接口 [GET /api/chat/appointments/{appointmentId}/session, GET /api/chat/appointments/{appointmentId}/messages, WebSocket /ws/consult-chat]，对应 UI-SDD [4.4 预约、通知、聊天室模块, 3.6 appointmentStore, 6.1 页面级 composable]

- [x] 创建 `src/api/resource.ts`，封装资源分类、标签、资源列表、资源详情、收藏列表、添加收藏、取消收藏接口。
  索引：依赖后端接口 [GET /api/resources/categories, GET /api/resources/tags, GET /api/resources, GET /api/resources/{resourceId}, GET /api/student/favorites, POST /api/student/favorites/{resourceId}, DELETE /api/student/favorites/{resourceId}]，对应 UI-SDD [4.5 资源库模块, 3.8 resourceStore]

- [x] 创建 `src/api/admin-resource.ts`，封装管理员资源列表、资源新增/更新、发布/下线、分类管理、标签管理接口。
  索引：依赖后端接口 [GET /api/admin/resources, POST /api/admin/resources, PUT /api/admin/resources/{resourceId}, POST /api/admin/resources/{resourceId}/publish, POST /api/admin/resources/{resourceId}/offline, GET /api/admin/resource-categories, POST /api/admin/resource-categories, PUT /api/admin/resource-categories/{categoryId}, GET /api/admin/resource-tags, POST /api/admin/resource-tags]，对应 UI-SDD [4.6 管理员治理模块, 3.8 resourceStore, 3.9 adminStore]

- [x] 创建 `src/api/admin-scale.ts`，封装管理员量表列表、详情、新增、编辑、启用、停用接口。
  索引：依赖后端接口 [GET /api/admin/scales, GET /api/admin/scales/{scaleId}, POST /api/admin/scales, PUT /api/admin/scales/{scaleId}, POST /api/admin/scales/{scaleId}/activate, POST /api/admin/scales/{scaleId}/deactivate]，对应 UI-SDD [4.6 管理员治理模块, 3.9 adminStore]

- [x] 创建 `src/api/admin-statistics.ts`，封装总览、测评统计、资源统计、预约统计、导出接口。
  索引：依赖后端接口 [GET /api/admin/statistics/overview, GET /api/admin/statistics/assessments, GET /api/admin/statistics/resources, GET /api/admin/statistics/appointments, GET /api/admin/statistics/export]，对应 UI-SDD [4.6 管理员治理模块, 3.9 adminStore]

- [x] 创建 `src/api/admin-ai-task.ts`，封装 AI 运维任务解析、列表、详情、确认、取消接口。
  索引：依赖后端接口 [POST /api/admin/ai-tasks/parse, GET /api/admin/ai-tasks, GET /api/admin/ai-tasks/{taskId}, POST /api/admin/ai-tasks/{taskId}/confirm, POST /api/admin/ai-tasks/{taskId}/cancel]，对应 UI-SDD [4.6 管理员治理模块, 3.9 adminStore]

- [x] 创建 `src/api/admin-audit.ts`，封装审计日志分页查询接口。
  索引：依赖后端接口 [GET /api/admin/audit-logs]，对应 UI-SDD [4.6 管理员治理模块, 3.9 adminStore]

- [x] 在 `src/api/types/` 下补齐所有模块 DTO、分页、查询参数、WebSocket 载荷类型定义。
  索引：依赖后端接口 [全部 `/api/**` 接口与 `/ws/consult-chat`]，对应 UI-SDD [4.7 请求层标准化, 5.P1 - 状态管理与接口定义]

## 第三阶段：页面逻辑层预建

- [x] 创建 `src/views/public/LoginRoute.vue`，仅编写登录表单状态、提交逻辑、登录后重定向逻辑。
  索引：依赖后端接口 [POST /api/auth/login, GET /api/auth/current-user]，对应 UI-SDD [2.1 总体路由树, 4.1 认证与用户模块, 5.P2 - 各页面骨架]

- [x] 创建 `src/views/student/StudentProfileRoute.vue`，仅编写档案加载、编辑态、保存逻辑。
  索引：依赖后端接口 [GET /api/student/profile/me, PUT /api/student/profile/me]，对应 UI-SDD [2.1 总体路由树, 4.1 认证与用户模块, 3.3 userStore]

- [x] 创建 `src/views/student/StudentScaleListRoute.vue` 与 `StudentScaleIntroRoute.vue`，仅编写量表列表加载、量表详情加载、草稿创建跳转逻辑。
  索引：依赖后端接口 [GET /api/scales, GET /api/scales/{scaleId}, POST /api/scales/{scaleId}/sessions/draft]，对应 UI-SDD [2.1 总体路由树, 4.2 测评与报告模块, 3.4 assessmentStore]

- [x] 创建 `src/views/student/StudentAssessmentSessionRoute.vue`，仅编写题目分页读取、答案暂存、提交逻辑。
  索引：依赖后端接口 [GET /api/scales/sessions/{sessionId}/questions, PUT /api/scales/sessions/{sessionId}/answers, POST /api/scales/sessions/{sessionId}/submit]，对应 UI-SDD [2.3 路由权限校验逻辑, 4.2 测评与报告模块, 6.1 页面级 composable]

- [x] 创建 `src/views/student/StudentReportListRoute.vue` 与 `StudentReportDetailRoute.vue`，仅编写报告列表/详情加载与推荐资源、预约跳转逻辑。
  索引：依赖后端接口 [GET /api/student/reports, GET /api/student/reports/{reportId}]，对应 UI-SDD [2.1 总体路由树, 4.2 测评与报告模块, 3.4 assessmentStore]

- [x] 创建 `src/views/student/StudentAiSessionListRoute.vue` 与 `StudentAiSessionRoute.vue`，仅编写 AI 会话列表、消息加载、发送消息逻辑。
  索引：依赖后端接口 [POST /api/student/ai-sessions, GET /api/student/ai-sessions, GET /api/student/ai-sessions/{sessionId}/messages, POST /api/student/ai-sessions/{sessionId}/messages]，对应 UI-SDD [2.1 总体路由树, 4.3 AI 会话模块, 3.5 aiChatStore]

- [x] 创建 `src/views/student/StudentAppointmentSlotRoute.vue` 与 `StudentAppointmentListRoute.vue`，仅编写时段查询、发起预约、学生预约列表逻辑。
  索引：依赖后端接口 [GET /api/student/appointments/slots, GET /api/student/appointments, POST /api/student/appointments]，对应 UI-SDD [2.1 总体路由树, 4.4 预约、通知、聊天室模块, 3.6 appointmentStore]

- [x] 创建 `src/views/student/StudentResourceListRoute.vue`、`StudentResourceDetailRoute.vue`、`StudentFavoriteRoute.vue`，仅编写资源筛选、详情读取、收藏管理逻辑。
  索引：依赖后端接口 [GET /api/resources/categories, GET /api/resources/tags, GET /api/resources, GET /api/resources/{resourceId}, GET /api/student/favorites, POST /api/student/favorites/{resourceId}, DELETE /api/student/favorites/{resourceId}]，对应 UI-SDD [2.1 总体路由树, 4.5 资源库模块, 3.8 resourceStore]

- [x] 创建 `src/views/student/StudentNotificationRoute.vue`，仅编写通知列表读取、单条已读、全部已读逻辑。
  索引：依赖后端接口 [GET /api/notifications, POST /api/notifications/{notificationId}/read, POST /api/notifications/read-all]，对应 UI-SDD [2.1 总体路由树, 4.4 预约、通知、聊天室模块, 3.7 notificationStore]

- [x] 创建 `src/views/student/StudentConsultChatRoute.vue`，仅编写聊天室会话加载、历史消息读取、WebSocket 建连/断连/收发逻辑。
  索引：依赖后端接口 [GET /api/chat/appointments/{appointmentId}/session, GET /api/chat/appointments/{appointmentId}/messages, WebSocket /ws/consult-chat]，对应 UI-SDD [2.3 路由权限校验逻辑, 4.4 预约、通知、聊天室模块, 3.6 appointmentStore, 6.1 页面级 composable]

- [x] 创建 `src/views/counselor/CounselorStudentListRoute.vue`，仅编写绑定学生列表加载与子路由跳转逻辑。
  索引：依赖后端接口 [GET /api/counselor/students]，对应 UI-SDD [2.1 总体路由树, 4.1 认证与用户模块, 3.3 userStore]

- [x] 创建 `src/views/counselor/CounselorStudentReportListRoute.vue` 与 `CounselorStudentReportDetailRoute.vue`，仅编写学生报告读取逻辑。
  索引：依赖后端接口 [GET /api/counselor/students/{studentUserId}/reports, GET /api/counselor/students/{studentUserId}/reports/{reportId}]，对应 UI-SDD [2.3 路由权限校验逻辑, 4.2 测评与报告模块, 3.4 assessmentStore]

- [x] 创建 `src/views/counselor/CounselorStudentAiSessionListRoute.vue` 与 `CounselorStudentAiSessionRoute.vue`，仅编写咨询师查看学生 AI 会话逻辑。
  索引：依赖后端接口 [GET /api/counselor/students/{studentUserId}/ai-sessions, GET /api/counselor/students/{studentUserId}/ai-sessions/{sessionId}/messages]，对应 UI-SDD [2.3 路由权限校验逻辑, 4.3 AI 会话模块, 3.5 aiChatStore]

- [x] 创建 `src/views/counselor/CounselorAppointmentRoute.vue`，仅编写预约列表、接单、拒绝逻辑。
  索引：依赖后端接口 [GET /api/counselor/appointments, POST /api/counselor/appointments/{appointmentId}/accept, POST /api/counselor/appointments/{appointmentId}/reject]，对应 UI-SDD [2.1 总体路由树, 4.4 预约、通知、聊天室模块, 3.6 appointmentStore]

- [x] 创建 `src/views/counselor/CounselorNotificationRoute.vue` 与 `CounselorConsultChatRoute.vue`，仅编写通知与聊天室逻辑。
  索引：依赖后端接口 [GET /api/notifications, POST /api/notifications/{notificationId}/read, POST /api/notifications/read-all, GET /api/chat/appointments/{appointmentId}/session, GET /api/chat/appointments/{appointmentId}/messages, WebSocket /ws/consult-chat]，对应 UI-SDD [2.1 总体路由树, 4.4 预约、通知、聊天室模块, 3.6 appointmentStore, 3.7 notificationStore]

- [x] 创建 `src/views/admin/AdminUserRoute.vue`，仅编写用户列表、创建咨询师、启用/禁用/重置密码逻辑。
  索引：依赖后端接口 [GET /api/admin/users, POST /api/admin/users/counselors, POST /api/admin/users/{userId}/enable, POST /api/admin/users/{userId}/disable, POST /api/admin/users/{userId}/reset-password]，对应 UI-SDD [2.1 总体路由树, 4.1 认证与用户模块, 3.9 adminStore]

- [x] 创建 `src/views/admin/AdminScaleListRoute.vue` 与 `AdminScaleDetailRoute.vue`，仅编写量表列表、详情、保存、启停逻辑。
  索引：依赖后端接口 [GET /api/admin/scales, GET /api/admin/scales/{scaleId}, POST /api/admin/scales, PUT /api/admin/scales/{scaleId}, POST /api/admin/scales/{scaleId}/activate, POST /api/admin/scales/{scaleId}/deactivate]，对应 UI-SDD [2.1 总体路由树, 4.6 管理员治理模块, 3.9 adminStore]

- [x] 创建 `src/views/admin/AdminResourceListRoute.vue`、`AdminResourceDetailRoute.vue`、`AdminResourceMetaRoute.vue`，仅编写资源治理、分类标签治理逻辑。
  索引：依赖后端接口 [GET /api/admin/resources, POST /api/admin/resources, PUT /api/admin/resources/{resourceId}, POST /api/admin/resources/{resourceId}/publish, POST /api/admin/resources/{resourceId}/offline, GET /api/admin/resource-categories, POST /api/admin/resource-categories, PUT /api/admin/resource-categories/{categoryId}, GET /api/admin/resource-tags, POST /api/admin/resource-tags]，对应 UI-SDD [2.1 总体路由树, 4.6 管理员治理模块, 3.8 resourceStore, 3.9 adminStore]

- [x] 创建 `src/views/admin/AdminStatisticsRoute.vue`，仅编写统计数据拉取与导出逻辑。
  索引：依赖后端接口 [GET /api/admin/statistics/overview, GET /api/admin/statistics/assessments, GET /api/admin/statistics/resources, GET /api/admin/statistics/appointments, GET /api/admin/statistics/export]，对应 UI-SDD [2.1 总体路由树, 4.6 管理员治理模块, 3.9 adminStore]

- [x] 创建 `src/views/admin/AdminAiTaskRoute.vue`，仅编写 AI 运维任务解析、待确认任务读取、确认/取消逻辑。
  索引：依赖后端接口 [POST /api/admin/ai-tasks/parse, GET /api/admin/ai-tasks, GET /api/admin/ai-tasks/{taskId}, POST /api/admin/ai-tasks/{taskId}/confirm, POST /api/admin/ai-tasks/{taskId}/cancel]，对应 UI-SDD [2.1 总体路由树, 4.6 管理员治理模块, 3.9 adminStore]

- [x] 创建 `src/views/admin/AdminAuditLogRoute.vue`，仅编写审计日志分页查询、筛选参数同步逻辑。
  索引：依赖后端接口 [GET /api/admin/audit-logs]，对应 UI-SDD [2.1 总体路由树, 4.6 管理员治理模块, 3.9 adminStore]

- [x] 为所有页面骨架统一补 `onMounted/watch/route param` 数据预取逻辑，并保持 `<template>` 为空骨架或最小占位，不写具体 UI 结构。
  索引：依赖后端接口 [全部对应路由接口]，对应 UI-SDD [5.P2 - 各页面骨架, 6.3 前端错误恢复原则]

