# 第 1 期与第 2 期阶段验收清单

## 1. 前置条件

- [ ] 已启动后端服务，默认地址为 `http://127.0.0.1:8080`
- [ ] 数据库已初始化 `schema.sql`
- [ ] 初始化演示账号可用：
  - 学生：`20230001 / Jqpro@123`
  - 咨询师：`teacher01 / Jqpro@123`
  - 管理员：`admin / Jqpro@123`
- [ ] 量表演示数据 `PHQ9` 已存在
- [ ] 至少存在一个可预约时段

## 2. 第 1 期验收

### 2.1 学生角色正常路径

- [ ] `POST /api/auth/login`
  - 使用学生账号登录成功，返回 `token`
- [ ] `GET /api/auth/current-user`
  - 使用学生 token 获取本人信息成功
- [ ] `GET /api/student/profile/me`
  - 能读取本人档案
- [ ] `PUT /api/student/profile/me`
  - 能更新允许维护的字段，且返回更新后的档案
- [ ] `GET /api/scales`
  - 能看到启用量表列表
- [ ] `GET /api/scales/{scaleId}`
  - 能看到量表详情
- [ ] `POST /api/scales/{scaleId}/sessions/draft`
  - 能创建或获取草稿会话
- [ ] `GET /api/scales/sessions/{sessionId}/questions`
  - 能分页读取题目
- [ ] `PUT /api/scales/sessions/{sessionId}/answers`
  - 能保存答案并更新 `answeredCount`
- [ ] `POST /api/scales/sessions/{sessionId}/submit`
  - 全部作答后提交成功，返回 `reportId`
- [ ] `GET /api/student/reports`
  - 能看到自己新增的报告
- [ ] `GET /api/student/reports/{reportId}`
  - 能看到自己报告详情
- [ ] `POST /api/auth/change-password`
  - 改密成功后可重新登录
- [ ] `POST /api/auth/logout`
  - 登出成功

### 2.2 咨询师角色正常路径

- [ ] `POST /api/auth/login`
  - 使用咨询师账号登录成功
- [ ] `GET /api/counselor/students/{studentUserId}/reports`
  - 能看到已绑定学生的报告列表
- [ ] `GET /api/counselor/students/{studentUserId}/reports/{reportId}`
  - 能看到已绑定学生的报告详情

### 2.3 第 1 期越权路径

- [ ] 学生 token 访问 `GET /api/counselor/students/{studentUserId}/reports`
  - 返回 `403/403`
- [ ] 咨询师 token 访问 `GET /api/student/profile/me`
  - 返回 `403/403`
- [ ] 学生 A 尝试访问学生 B 的报告详情
  - 返回 `400/600` 或资源不匹配错误
- [ ] 咨询师访问未绑定学生报告
  - 返回 `400/600`

### 2.4 第 1 期异常路径

- [ ] `POST /api/auth/login`
  - 账号错误时返回 `400/600`
- [ ] `POST /api/auth/login`
  - 缺少密码时返回 `422/422`
- [ ] `POST /api/auth/change-password`
  - 原密码错误时返回 `400/600`
- [ ] `POST /api/auth/change-password`
  - 新密码与确认密码不一致时返回 `400/600`
- [ ] `PUT /api/scales/sessions/{sessionId}/answers`
  - 空答案列表返回 `422/422`
- [ ] `POST /api/scales/sessions/{sessionId}/submit`
  - 未全部作答时返回 `400/600`
- [ ] `GET /api/scales/{scaleId}`
  - 不存在量表返回 `400/600`

## 3. 第 2 期验收

### 3.1 学生角色正常路径

- [ ] `POST /api/student/ai-sessions`
  - 能创建 AI 会话
- [ ] `GET /api/student/ai-sessions`
  - 能看到自己的会话列表
- [ ] `POST /api/student/ai-sessions/{sessionId}/messages`
  - 能发送消息并收到 AI 回复
- [ ] `GET /api/student/ai-sessions/{sessionId}/messages`
  - 能看到消息历史
- [ ] `GET /api/student/appointments/slots`
  - 能看到可预约时段
- [ ] `POST /api/student/appointments`
  - 能提交匿名预约
- [ ] `GET /api/notifications`
  - 能看到预约处理相关通知
- [ ] `POST /api/notifications/{notificationId}/read`
  - 能将单条通知标记已读
- [ ] `POST /api/notifications/read-all`
  - 能批量已读
- [ ] `GET /api/chat/appointments/{appointmentId}/session`
  - 咨询师接单后能看到聊天室会话
- [ ] `GET /api/chat/appointments/{appointmentId}/messages`
  - 能查看聊天室消息历史

### 3.2 咨询师角色正常路径

- [ ] `GET /api/counselor/students/{studentUserId}/ai-sessions`
  - 能查看绑定学生 AI 会话列表
- [ ] `GET /api/counselor/students/{studentUserId}/ai-sessions/{sessionId}/messages`
  - 能查看绑定学生消息历史
- [ ] `GET /api/counselor/appointments`
  - 能查看自己的预约单列表
- [ ] `POST /api/counselor/appointments/{appointmentId}/accept`
  - 能接单并给学生发通知
- [ ] `POST /api/counselor/appointments/{appointmentId}/reject`
  - 能拒单并释放时段

### 3.3 WebSocket 正常路径

- [ ] 使用 `ws://127.0.0.1:8080/ws/consult-chat?token=<token>&appointmentId=<appointmentId>` 建立连接
  - 连接成功后收到 `type=CONNECTED`
- [ ] 在有效预约时段内发送：
  - `{"content":"测试消息"}`
- [ ] 参与双方都能收到 `type=MESSAGE` 广播
- [ ] 超过 `closeTime` 后连接或发消息
  - 返回封存或时间窗口错误

### 3.4 第 2 期越权路径

- [ ] 学生 token 访问 `GET /api/counselor/appointments`
  - 返回 `403/403`
- [ ] 咨询师 token 访问 `POST /api/student/appointments`
  - 返回 `403/403`
- [ ] 咨询师访问未绑定学生 AI 会话
  - 返回 `400/600`
- [ ] 非预约参与者访问 `GET /api/chat/appointments/{appointmentId}/session`
  - 返回 `400/600`
- [ ] 非通知接收人调用 `POST /api/notifications/{notificationId}/read`
  - 返回 `400/600`

### 3.5 第 2 期异常路径

- [ ] `POST /api/student/ai-sessions/{sessionId}/messages`
  - 空消息返回 `422/422`
- [ ] `POST /api/student/appointments`
  - 时段不存在返回 `400/600`
- [ ] `POST /api/student/appointments`
  - 时段已过期返回 `400/600`
- [ ] `POST /api/counselor/appointments/{appointmentId}/accept`
  - 非 `PENDING` 状态返回 `400/600`
- [ ] `POST /api/counselor/appointments/{appointmentId}/reject`
  - 非 `PENDING` 状态返回 `400/600`
- [ ] WebSocket 连接缺少 `token`
  - 握手失败
- [ ] WebSocket 发送空消息
  - 收到 `type=ERROR`

## 4. 推荐验收脚本

- [ ] 执行 `scripts/phase1-auth-profile.ps1`
- [ ] 执行 `scripts/phase1-assessment-report.ps1`
- [ ] 执行 `scripts/phase2-aichat-appointment.ps1`

备注：

- WebSocket 验收当前不建议伪造 curl 脚本，推荐按 `spec/04_api/phase2-api.md` 中的连接参数与消息格式使用专业客户端测试。
