# 前端核心界面重写交接文档

## 1. 文档用途

这份文档是给接手前端重写的 AI 或前端工程师看的。

目标不是解释业务逻辑本身，而是回答下面几个问题：

- 这个前端项目的结构是什么
- 哪些页面是大的、核心的界面
- 第一批应该优先重写哪些页面
- 每个页面对应哪个路由、哪个 Vue 文件
- 改样式时哪些可以改，哪些不要乱动

## 2. 项目技术栈

- 框架：Vue 3
- 构建：Vite
- 路由：Vue Router 4
- 状态：Pinia
- 请求：Axios
- UI：当前项目没有形成统一设计系统，很多页面是各自独立写样式
- 接口前缀：默认走 `/api`
- 开发代理：
  - `/api -> http://127.0.0.1:8080`
  - `/ws -> ws://127.0.0.1:8080`

关键文件：

- 路由入口：`frontend/src/router/index.ts`
- 公共路由：`frontend/src/router/modules/public.ts`
- 学生端路由：`frontend/src/router/modules/student.ts`
- 咨询师端路由：`frontend/src/router/modules/counselor.ts`
- 管理员端路由：`frontend/src/router/modules/admin.ts`
- 全局布局：`frontend/src/layouts/RoleWorkspaceLayout.vue`
- HTTP 封装：`frontend/src/api/http.ts`
- 类型出口：`frontend/src/api/types/index.ts`

## 3. 当前前端的真实结构

前端按角色分成三套工作台：

1. 学生端
2. 咨询师端
3. 管理员端

另外还有一套公共页面：

- 登录页
- 根路由跳转页
- 403 页面
- 404 页面

绝大多数业务页面都挂在同一个角色布局下面：

- `frontend/src/layouts/RoleWorkspaceLayout.vue`

但要注意：

- 这个布局现在几乎只有一个壳，顶部导航代码被注释掉了
- 也就是说，当前大部分页面各自为战，视觉没有统一母版
- 如果要做“所有核心界面样式重写”，第一步应当先把共享布局重建出来

## 4. 必须优先识别的核心界面

下面是按“业务价值 + 使用频率 + 演示价值”排序后的核心界面。

### 4.1 一级核心页面

这些页面应该视为第一批必须重写的页面。

#### A. 登录页

- 路由：`/login`
- 文件：`frontend/src/views/public/LoginRoute.vue`
- 作用：所有角色的统一入口
- 价值：第一印象页面，必须重做

#### B. 全局角色工作台布局

- 文件：`frontend/src/layouts/RoleWorkspaceLayout.vue`
- 作用：学生端、咨询师端、管理员端的共享外层容器
- 价值：决定导航、页边距、头部区、统一背景、统一按钮语言
- 备注：当前顶部导航注释掉了，这里适合成为全局重构支点

#### C. 学生首页

- 路由：`/student`
- 文件：`frontend/src/views/student/StudentHomeRoute.vue`
- 作用：学生侧总入口
- 业务地位：核心中的核心
- 页面内容：测评入口、预约入口、AI 倾诉入口、通知、资源推荐、最近报告

#### D. 测评目录页

- 路由：`/student/scales`
- 文件：`frontend/src/views/student/StudentScaleListRoute.vue`
- 作用：学生选择量表的入口页
- 业务地位：测评闭环起点

#### E. 测评详情页

- 路由：`/student/scales/:scaleId`
- 文件：`frontend/src/views/student/StudentScaleIntroRoute.vue`
- 作用：展示量表介绍并开始测评

#### F. 测评作答页

- 路由：`/student/assessment-sessions/:sessionId`
- 文件：`frontend/src/views/student/StudentAssessmentSessionRoute.vue`
- 作用：核心业务操作页
- 业务地位：学生测评闭环中最关键的交互页

#### G. 测评报告详情页

- 路由：`/student/reports/:reportId`
- 文件：`frontend/src/views/student/StudentReportDetailRoute.vue`
- 作用：展示总分、风险等级、AI 解读、建议、推荐资源
- 业务地位：整个项目最适合展示“AI + 测评价值”的页面之一

#### H. 学生 AI 会话列表页

- 路由：`/student/ai-sessions`
- 文件：`frontend/src/views/student/StudentAiSessionListRoute.vue`
- 作用：创建 AI 会话、查看会话归档

#### I. 学生 AI 会话详情页

- 路由：`/student/ai-sessions/:sessionId`
- 文件：`frontend/src/views/student/StudentAiSessionRoute.vue`
- 作用：学生和 AI 直接对话
- 业务地位：项目第二个最重要的展示页

#### J. 学生预约时段页

- 路由：`/student/appointments/slots`
- 文件：`frontend/src/views/student/StudentAppointmentSlotRoute.vue`
- 作用：选择咨询时段并发起匿名预约
- 业务地位：人工咨询闭环起点

#### K. 咨询师首页

- 路由：`/counselor`
- 文件：`frontend/src/views/counselor/CounselorHomeRoute.vue`
- 作用：咨询师工作台总览
- 页面内容：绑定学生、预约处理、通知、快捷跳转

#### L. 咨询师学生列表页

- 路由：`/counselor/students`
- 文件：`frontend/src/views/counselor/CounselorStudentListRoute.vue`
- 作用：查看绑定学生，进入报告和 AI 会话

#### M. 咨询师预约处理页

- 路由：`/counselor/appointments`
- 文件：`frontend/src/views/counselor/CounselorAppointmentRoute.vue`
- 作用：接单、拒绝、填写处理意见、进入聊天室
- 业务地位：咨询师端最关键页面

#### N. 管理员首页

- 路由：`/admin`
- 文件：`frontend/src/views/admin/AdminHomeRoute.vue`
- 作用：治理大盘入口
- 业务地位：如果要做完整三端重设计，管理员首页必须重做

### 4.2 二级核心页面

这些页面是第二批应跟进重写的页面，通常与一级页面构成完整闭环。

#### 学生侧

- 学生档案页
  - 路由：`/student/profile`
  - 文件：`frontend/src/views/student/StudentProfileRoute.vue`

- 学生报告列表页
  - 路由：`/student/reports`
  - 文件：`frontend/src/views/student/StudentReportListRoute.vue`

- 学生预约列表页
  - 路由：`/student/appointments`
  - 文件：`frontend/src/views/student/StudentAppointmentListRoute.vue`

- 学生通知页
  - 路由：`/student/notifications`
  - 文件：`frontend/src/views/student/StudentNotificationRoute.vue`

- 学生私密聊天室页
  - 路由：`/student/chat/appointments/:appointmentId`
  - 文件：`frontend/src/views/student/StudentConsultChatRoute.vue`

#### 咨询师侧

- 学生报告列表页
  - 路由：`/counselor/students/:studentUserId/reports`
  - 文件：`frontend/src/views/counselor/CounselorStudentReportListRoute.vue`

- 学生报告详情页
  - 路由：`/counselor/students/:studentUserId/reports/:reportId`
  - 文件：`frontend/src/views/counselor/CounselorStudentReportDetailRoute.vue`

- 学生 AI 会话列表页
  - 路由：`/counselor/students/:studentUserId/ai-sessions`
  - 文件：`frontend/src/views/counselor/CounselorStudentAiSessionListRoute.vue`

- 学生 AI 会话详情页
  - 路由：`/counselor/students/:studentUserId/ai-sessions/:sessionId`
  - 文件：`frontend/src/views/counselor/CounselorStudentAiSessionRoute.vue`

- 咨询师通知页
  - 路由：`/counselor/notifications`
  - 文件：`frontend/src/views/counselor/CounselorNotificationRoute.vue`

- 咨询师聊天室页
  - 路由：`/counselor/chat/appointments/:appointmentId`
  - 文件：`frontend/src/views/counselor/CounselorConsultChatRoute.vue`

#### 管理员侧

- 用户管理页
  - 路由：`/admin/users`
  - 文件：`frontend/src/views/admin/AdminUserRoute.vue`

- 量表管理列表页
  - 路由：`/admin/scales`
  - 文件：`frontend/src/views/admin/AdminScaleListRoute.vue`

- 量表详情页
  - 路由：`/admin/scales/new`
  - 路由：`/admin/scales/:scaleId`
  - 文件：`frontend/src/views/admin/AdminScaleDetailRoute.vue`

- 资源管理列表页
  - 路由：`/admin/resources`
  - 文件：`frontend/src/views/admin/AdminResourceListRoute.vue`

- 资源详情页
  - 路由：`/admin/resources/new`
  - 路由：`/admin/resources/:resourceId`
  - 文件：`frontend/src/views/admin/AdminResourceDetailRoute.vue`

- 资源元数据页
  - 路由：`/admin/resources/meta`
  - 文件：`frontend/src/views/admin/AdminResourceMetaRoute.vue`

- 统计页
  - 路由：`/admin/statistics`
  - 文件：`frontend/src/views/admin/AdminStatisticsRoute.vue`

- 管理员 AI 运维页
  - 路由：`/admin/ai-tasks`
  - 文件：`frontend/src/views/admin/AdminAiTaskRoute.vue`

- 审计日志页
  - 路由：`/admin/audit-logs`
  - 文件：`frontend/src/views/admin/AdminAuditLogRoute.vue`

## 5. 建议给 AI 的重写范围

如果你想让 AI “先做大改，但聚焦核心”，建议明确告诉它分三批做：

### 第一批

- `LoginRoute.vue`
- `RoleWorkspaceLayout.vue`
- `StudentHomeRoute.vue`
- `StudentScaleListRoute.vue`
- `StudentScaleIntroRoute.vue`
- `StudentAssessmentSessionRoute.vue`
- `StudentReportDetailRoute.vue`
- `StudentAiSessionListRoute.vue`
- `StudentAiSessionRoute.vue`
- `StudentAppointmentSlotRoute.vue`
- `CounselorHomeRoute.vue`
- `CounselorStudentListRoute.vue`
- `CounselorAppointmentRoute.vue`
- `AdminHomeRoute.vue`

### 第二批

- 学生档案、报告列表、预约列表、通知、私密聊天室
- 咨询师报告列表/详情、AI 会话列表/详情、通知、聊天室
- 管理员用户、量表、资源、统计、AI 运维、审计日志

### 第三批

- 403 页、404 页、账户安全页
- 细枝末节的二级列表页和状态页

## 6. 设计重写时的工程约束

这一段非常重要，交给 AI 时必须一起带上。

### 6.1 可以改的内容

- 页面布局
- 视觉风格
- 字体、颜色、阴影、背景、卡片、边框
- 信息层级
- 按钮、表单、列表、状态展示方式
- 页面骨架和组件抽取
- 全局共享样式方案
- `RoleWorkspaceLayout.vue` 的头部、侧栏、导航重建

### 6.2 尽量不要改的内容

- 路由路径
- 接口调用语义
- API 文件对外方法名
- Pinia store 的核心数据结构
- 权限守卫逻辑
- WebSocket 连接参数格式

### 6.3 最好保持不变的目录边界

- `frontend/src/api/*`
- `frontend/src/api/types/*`
- `frontend/src/router/*`
- `frontend/src/core/*`
- `frontend/src/stores/*`

AI 做视觉改造时，最好主要动这些地方：

- `frontend/src/layouts/*`
- `frontend/src/views/*`
- 如有必要新增：
  - `frontend/src/components/*`
  - `frontend/src/styles/*`
  - `frontend/src/theme/*`

## 7. 当前前端的真实问题

这是交给 AI 时非常值得点明的部分。

### 7.1 最大问题

- 虽然很多页面单独看已经有样式，但整体没有统一母版
- 三个角色端的视觉语言不稳定
- 共享布局没有真正承担“工作台外壳”的职责
- 组件复用不足，页面之间像独立作品，不像同一产品

### 7.2 结果

- 登录页、学生页、咨询师页、管理员页像不同项目
- 用户切换角色时缺乏产品一致性
- 后续继续扩展页面会越来越难统一

## 8. 推荐的重构方向

如果让 AI 做整套核心界面改造，建议要求它遵循下面原则：

1. 先定义统一设计语言，再改页面，不要一页一页各写各的。
2. 先重做 `RoleWorkspaceLayout.vue`，再推进三端首页和核心流程页。
3. 学生端、咨询师端、管理员端可以共享基础设计语言，但应有清晰角色差异。
4. 学生端要偏“安静、疗愈、陪伴感”。
5. 咨询师端要偏“专业、秩序、信息密度适中”。
6. 管理员端要偏“治理中枢、强结构、强控制感”。
7. 不要只做颜色替换，要重做层级、导航、区块关系和视觉节奏。

## 9. 建议直接发给 AI 的任务描述

可以把下面这段直接复制给另一个 AI：

```text
你现在接手 JQPro 前端核心界面重写任务。

项目位置：E:\Store\SDJZU\毕设\JQPro\frontend

请先阅读：
1. frontend/src/router/modules/public.ts
2. frontend/src/router/modules/student.ts
3. frontend/src/router/modules/counselor.ts
4. frontend/src/router/modules/admin.ts
5. frontend/src/layouts/RoleWorkspaceLayout.vue
6. spec/03_Design/frontend/frontend-redesign-handoff.md

任务目标：
- 对核心页面做统一视觉重构
- 先重建共享布局，再重写三端核心页面
- 不改接口语义，不改路由路径，不破坏现有数据流
- 优先修改 frontend/src/views/* 与 frontend/src/layouts/*

第一批优先页面：
- LoginRoute.vue
- RoleWorkspaceLayout.vue
- StudentHomeRoute.vue
- StudentScaleListRoute.vue
- StudentScaleIntroRoute.vue
- StudentAssessmentSessionRoute.vue
- StudentReportDetailRoute.vue
- StudentAiSessionListRoute.vue
- StudentAiSessionRoute.vue
- StudentAppointmentSlotRoute.vue
- CounselorHomeRoute.vue
- CounselorStudentListRoute.vue
- CounselorAppointmentRoute.vue
- AdminHomeRoute.vue

设计要求：
- 不要做模板化、AI 感很重的普通后台
- 学生端、咨询师端、管理员端要共用同一产品语言，但角色气质不同
- 先抽主题变量、共享容器、共享按钮/表单风格
- 保证桌面和移动端都能正常展示
```

## 10. 最后结论

如果你现在只想先抓“大而核心”的界面，那么最值得优先重写的是这 14 个：

1. `LoginRoute.vue`
2. `RoleWorkspaceLayout.vue`
3. `StudentHomeRoute.vue`
4. `StudentScaleListRoute.vue`
5. `StudentScaleIntroRoute.vue`
6. `StudentAssessmentSessionRoute.vue`
7. `StudentReportDetailRoute.vue`
8. `StudentAiSessionListRoute.vue`
9. `StudentAiSessionRoute.vue`
10. `StudentAppointmentSlotRoute.vue`
11. `CounselorHomeRoute.vue`
12. `CounselorStudentListRoute.vue`
13. `CounselorAppointmentRoute.vue`
14. `AdminHomeRoute.vue`

先把这 14 个页面和共享布局做完，整个项目的“丑”会先被解决 70% 以上。
