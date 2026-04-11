# AI 会话、预约与聊天室接口

## 学生 AI 会话

### POST /api/student/ai-sessions
- 角色：`STUDENT`
- 请求头：`Authorization`、`Content-Type: application/json`
- Body：`title`
- 说明：创建 AI 倾诉会话

### GET /api/student/ai-sessions
- 角色：`STUDENT`
- 请求头：`Authorization`
- 返回字段：`sessionId`、`studentUserId`、`studentName`、`title`、`status`、`summaryText`、`riskFlag`、`riskLevel`、`lastActiveAt`、`createdAt`

### GET /api/student/ai-sessions/{sessionId}/messages
- 角色：`STUDENT`
- 请求头：`Authorization`
- 返回字段：`messageId`、`sessionId`、`senderType`、`content`、`riskLevel`、`hitKeywords`、`createdAt`
- 备注：数据库中存的是密文，接口自动解密后返回明文

### POST /api/student/ai-sessions/{sessionId}/messages
- 角色：`STUDENT`
- 请求头：`Authorization`、`Content-Type: application/json`
- Body：
```json
{
  "content": "我最近学习压力有点大。"
}
```
- 返回：
  - `studentMessage`
  - `aiMessage`
  - `riskFlag`
  - `riskLevel`

## 咨询师查看学生 AI 会话

### GET /api/counselor/students/{studentUserId}/ai-sessions
### GET /api/counselor/students/{studentUserId}/ai-sessions/{sessionId}/messages
- 角色：`COUNSELOR`
- 请求头：`Authorization`
- 说明：仅能查看已绑定学生的 AI 会话和消息

## 学生预约

### GET /api/student/appointments/slots
- 角色：`STUDENT`
- 请求头：`Authorization`
- 返回字段：`slotId`、`counselorUserId`、`counselorName`、`startTime`、`endTime`、`status`
- 备注：学生前端展示应做匿名化，后端保留 counselor 信息用于业务处理

### GET /api/student/appointments
- 角色：`STUDENT`
- 请求头：`Authorization`
- 返回字段：`appointmentId`、`slotId`、`studentUserId`、`anonymousName`、`counselorUserId`、`counselorName`、`issueSummary`、`status`、`resultMessage`、`startTime`、`endTime`、`createdAt`

### POST /api/student/appointments
- 角色：`STUDENT`
- 请求头：`Authorization`、`Content-Type: application/json`
- Body：
```json
{
  "slotId": 15,
  "issueSummary": "希望预约一次匿名咨询。"
}
```
- 备注：会生成匿名代号并锁定预约时段

## 咨询师预约处理

### GET /api/counselor/appointments
- 角色：`COUNSELOR`
- 请求头：`Authorization`

### POST /api/counselor/appointments/{appointmentId}/accept
### POST /api/counselor/appointments/{appointmentId}/reject
- 角色：`COUNSELOR`
- 请求头：`Authorization`、`Content-Type: application/json`
- Body：`resultMessage`
- 说明：接单后自动创建或更新私密聊天室会话；拒绝后释放时段

## 通知中心

### GET /api/notifications
### POST /api/notifications/{notificationId}/read
### POST /api/notifications/read-all
- 角色：已登录用户
- 请求头：`Authorization`
- 说明：消息中心查询、单条已读、全部已读

## 私密聊天室 HTTP

### GET /api/chat/appointments/{appointmentId}/session
### GET /api/chat/appointments/{appointmentId}/messages
- 角色：`STUDENT` / `COUNSELOR`
- 请求头：`Authorization`
- 说明：仅预约双方可访问
- 备注：聊天室到期后由定时任务自动封存，并把预约状态更新为 `COMPLETED`

## 私密聊天室 WebSocket

### 握手地址
```text
ws://127.0.0.1:8080/ws/consult-chat?appointmentId={appointmentId}&token={token}
```

### 发送消息格式
```json
{
  "content": "测试消息"
}
```

### 服务端推送
- `CONNECTED`
- `MESSAGE`
- `ERROR`

推送载荷示例：
```json
{
  "type": "MESSAGE",
  "message": {
    "messageId": 1,
    "chatSessionId": 2,
    "senderUserId": 1,
    "senderType": "STUDENT",
    "content": "测试消息",
    "createdAt": "2026-04-07T20:00:00"
  }
}
```

## 常见异常
- `600`：AI 会话不存在、无权查看该学生 AI 会话、预约时段不存在、当前预约状态不允许接单/拒绝、当前不在预约有效时间内、聊天室已封存
- `401`：未登录或 token 失效
- `403`：角色无权限
