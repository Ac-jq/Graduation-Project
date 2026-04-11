# PRD 后端实现核对

## 结论

当前仓库后端已覆盖 PRD 中需要由后端承担的核心业务能力，并已完成可调用、可返回、可验收的纯后端交付收口。

## 模块核对

| PRD 模块 | 后端状态 | 代码落点 | 说明 |
| --- | --- | --- | --- |
| 用户管理与鉴权 | 已实现 | `AuthController` `StudentProfileController` `AdminUserController` | 包含登录、登出、改密、当前用户、学生档案、管理员账号管理 |
| 个人信息维护 | 已实现 | `StudentProfileServiceImpl` `StudentProfileResponse` | 本轮补齐头像字段 `avatarUrl` 与档案更新审计 |
| 安全审计日志 | 已实现 | `AuditLogServiceImpl` `AdminAuditController` | 登录、改密、测评、预约、资源、量表、管理员 AI 等关键操作均有记录 |
| 心理测评与 AI 解读 | 已实现 | `ScaleController` `AssessmentServiceImpl` `ReportServiceImpl` | 包含量表列表、草稿、分页答题、提交、报告、AI 解读 |
| AI 导师 | 已实现 | `StudentAiChatController` `AiChatServiceImpl` | 支持创建会话、发消息、风险分析；消息密文存储 |
| 咨询师查看学生 AI 会话 | 已实现 | `CounselorAiChatController` | 仅允许查看已绑定学生 |
| 匿名预约 | 已实现 | `StudentAppointmentController` `AppointmentServiceImpl` | 学生匿名发起预约，生成匿名代号 |
| 排班与通知 | 已实现 | `CounselorAppointmentController` `NotificationController` | 包含时段查询、接单、拒绝、通知中心 |
| 私密聊天室 | 已实现 | `ConsultChatController` `ConsultChatServiceImpl` `ConsultChatArchiveScheduler` | HTTP + WebSocket，消息加密，本轮补齐定时自动封存 |
| 自助资源库 | 已实现 | `ResourceController` `StudentFavoriteController` `ResourceServiceImpl` | 资源分类、标签、检索、详情、收藏 |
| 资源后台管理 | 已实现 | `AdminResourceController` | 创建、编辑、发布、下线、分类、标签 |
| 数据统计与导出 | 已实现 | `AdminStatisticsController` `StatisticsServiceImpl` | 总览、测评、资源、预约、按维度导出；本轮补齐非法维度业务异常 |
| 管理员 AI 运维 | 已实现 | `AdminAiTaskController` `AdminAiTaskServiceImpl` | 解析、展示待执行清单、确认执行、取消 |
| 隐私与合规 | 已实现 | `AiChatServiceImpl` `ConsultChatServiceImpl` `ResourceController` | AI/聊天密文存储、资源接口登录保护、学生侧匿名化由业务接口支撑 |

## 本轮补齐项

1. 学生头像字段未实现：已补数据库列、实体、DTO、档案更新逻辑。
2. 学生档案更新未审计：已补 `PROFILE_UPDATE` 审计。
3. 预约创建/接单/拒绝未审计：已补 `APPOINTMENT_CREATE`、`APPOINTMENT_ACCEPT`、`APPOINTMENT_REJECT`。
4. 资源接口原先允许匿名访问：已改为登录后访问。
5. 统计导出非法维度原先抛 500：已改为统一业务异常返回。
6. 私密聊天室结束后主要靠访问时归档：已补定时自动归档调度器。

## 不属于后端阻塞的问题

- 学生头像当前存储为 URL 字符串，不依赖对象存储即可运行。
- AI 依赖 Spring AI，可在无外部模型 key 时走已有降级回复逻辑，不阻塞后端交付。

## 外部资源结论

当前无阻塞后端交付的外部资源缺口。
