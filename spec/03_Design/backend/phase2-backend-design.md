# 第 2 期后端设计说明

## 1. 目标与范围

第 2 期目标是完成“AI 倾诉 + 咨询介入 + 匿名预约 + 私密聊天室”的最小消息通信闭环。

当前代码已经落地以下能力：

- 学生 AI 会话创建、查看、发消息
- 咨询师查看绑定学生 AI 会话与消息
- 学生匿名预约
- 咨询师接单/拒单
- 站内通知
- 聊天室会话查询
- WebSocket 文本聊天

## 2. 模块划分

| 模块 | 责任 |
| --- | --- |
| `controller.student.StudentAiChatController` | 学生侧 AI 会话 |
| `controller.counselor.CounselorAiChatController` | 咨询师侧学生 AI 会话只读查看 |
| `controller.student.StudentAppointmentController` | 学生侧预约时段查询与预约创建 |
| `controller.counselor.CounselorAppointmentController` | 咨询师侧预约列表、接单、拒单 |
| `controller.notification.NotificationController` | 当前用户通知列表与已读操作 |
| `controller.chat.ConsultChatController` | 聊天室会话查询与消息历史 |
| `websocket.ConsultChatHandshakeInterceptor` | WebSocket 握手鉴权 |
| `websocket.ConsultChatWebSocketHandler` | WebSocket 文本消息收发 |

## 3. AI 会话模型

### 3.1 数据模型

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `ai_chat_session` | AI 会话主表 | `student_user_id`、`title`、`status`、`summary_text`、`risk_flag`、`risk_level`、`last_active_at` |
| `ai_chat_message` | AI 会话消息表 | `session_id`、`sender_type`、`content_text`、`risk_level`、`hit_keywords` |

### 3.2 状态与行为

当前会话状态常量：

- `ACTIVE`
- `ARCHIVED`

但当前实现实际只写入 `ACTIVE`，尚未提供归档入口。

### 3.3 风险识别

当前风险识别策略是轻量规则模型：

- 内置高风险关键词列表
- 命中即标记：
  - `riskFlag = true`
  - `riskLevel = HIGH`
- 未命中时：
  - `riskFlag = false`
  - `riskLevel = LOW`

命中的关键词会写入：

- `ai_chat_message.hit_keywords`
- `ai_chat_message.risk_level`
- `ai_chat_session.risk_flag`
- `ai_chat_session.risk_level`

### 3.4 回复生成策略

当前 AI 回复采用两级兜底：

1. 若 `ChatModel` 可用，则走 Spring AI 模型调用
2. 若模型未配置或调用失败，则回退到内置回复模板

对高风险内容优先走固定安全话术，而不是继续普通闲聊。

## 4. 预约与通知模型

### 4.1 数据模型

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `consult_appointment_slot` | 咨询师可预约时段 | `counselor_user_id`、`start_time`、`end_time`、`status` |
| `consult_appointment` | 预约单 | `slot_id`、`student_user_id`、`counselor_user_id`、`anonymous_name`、`issue_summary`、`status`、`result_message` |
| `site_notification` | 站内通知 | `receiver_user_id`、`title`、`content_text`、`read_flag`、`read_at` |

### 4.2 时段状态

`consult_appointment_slot.status`：

- `OPEN`
- `RESERVED`
- `CLOSED`

当前实现逻辑：

- 学生只可预约 `OPEN` 且未过期时段
- 创建预约后时段改为 `RESERVED`
- 咨询师拒单后时段恢复 `OPEN`
- `CLOSED` 已预留，当前未主动写入

### 4.3 预约状态

`consult_appointment.status`：

- `PENDING`
- `ACCEPTED`
- `REJECTED`
- `CANCELED`
- `COMPLETED`

当前实现状态流转：

1. 学生创建预约：`PENDING`
2. 咨询师接单：`ACCEPTED`
3. 咨询师拒单：`REJECTED`
4. 聊天室过期封存时，如预约仍是 `ACCEPTED`，自动更新为 `COMPLETED`

### 4.4 匿名策略

预约单中不向咨询师暴露学生实名，而是存储系统生成匿名代号 `anonymous_name`。

当前匿名名生成方式：

- 根据学生用户 ID 取模选择前缀
- 组合成“某某同学”

该策略满足演示和 MVP 闭环，但不适合生产场景下的强匿名要求。

### 4.5 通知策略

通知触发点：

- 学生发起预约后，给咨询师发通知
- 咨询师接单后，给学生发通知
- 咨询师拒单后，给学生发通知

通知当前仅支持：

- 列表查询
- 单条已读
- 全部已读

## 5. 聊天室模型

### 5.1 数据模型

| 表名 | 说明 | 关键字段 |
| --- | --- | --- |
| `consult_chat_session` | 私密聊天室会话 | `appointment_id`、`student_user_id`、`counselor_user_id`、`open_time`、`close_time`、`status`、`sealed_flag` |
| `consult_chat_message` | 聊天消息 | `chat_session_id`、`sender_user_id`、`sender_type`、`content_cipher_text` |

### 5.2 聊天室生成时机

聊天室在咨询师接单时创建或刷新：

- `openTime` = 预约开始时间
- `closeTime` = 预约结束时间
- `status` = `PENDING`
- `sealedFlag` = `0`

### 5.3 聊天室状态

当前定义的状态常量：

- `PENDING`
- `ACTIVE`
- `CLOSED`
- `ARCHIVED`

当前实际流转：

1. 接单后创建：`PENDING`
2. WebSocket 首次连接成功并且通过时间校验：`ACTIVE`
3. 超过 `closeTime` 自动归档：`ARCHIVED`

`CLOSED` 常量已预留但未使用。

### 5.4 消息存储策略

聊天消息写库前先经过 `ChatCryptoUtil.encrypt(...)` 加密。

读取时：

- HTTP 查询接口中会解密后返回
- WebSocket 广播时会返回解密后的业务对象

这保证了数据库落的是密文，而业务接口对前端暴露的是明文。

## 6. WebSocket 鉴权方式

### 6.1 握手阶段

当前 WebSocket 握手参数：

- `token`
- `appointmentId`

校验逻辑：

1. 参数必须存在
2. 使用 `StpUtil.getLoginIdByToken(token)` 解析登录态
3. 成功后把 `userId`、`appointmentId` 写入 `session.attributes`

### 6.2 连接建立后

`afterConnectionEstablished` 会进一步调用 `validateAndActivateChat(...)`，继续校验：

- 当前用户是预约参与者
- 聊天室存在
- 当前时间位于有效预约窗口内
- 聊天室未封存

校验通过后会返回：

```json
{
  "type": "CONNECTED",
  "tip": "聊天室连接成功"
}
```

### 6.3 消息处理

客户端发送：

```json
{
  "content": "文本消息"
}
```

服务端行为：

1. 校验内容非空
2. 写入数据库
3. 广播给同预约下所有在线连接

广播载荷：

```json
{
  "type": "MESSAGE",
  "message": {
    "messageId": 1,
    "chatSessionId": 1,
    "senderUserId": 1,
    "senderType": "STUDENT",
    "content": "文本消息",
    "createdAt": "2026-04-07T10:00:00"
  }
}
```

## 7. 数据隐私策略

第 2 期当前已落地的隐私策略：

- 预约场景用匿名代号替代学生实名
- 咨询师读取 AI 会话和报告时，必须通过 `counselor_student` 绑定关系
- 聊天内容数据库密文存储
- 管理员当前无直接读取聊天与 AI 会话的 API
- 通知按接收人隔离，不能跨用户标记已读

仍需注意：

- AI 会话消息当前以明文 `content_text` 存储，尚未做加密
- 匿名代号生成策略较简单，生产场景建议升级
- WebSocket `token` 走 query 参数，存在被中间日志采集的风险，后续可以升级为更安全的握手方案

## 8. 当前已知限制

- AI 会话未实现显式归档、关闭和摘要回写优化
- 风险识别只用了关键词规则，尚未引入更细粒度风控
- 聊天室只支持文本，不支持图片、文件、音视频
- WebSocket 握手失败未统一输出 `Result<T>`，前端需额外处理握手失败状态
- 聊天室 `PENDING -> ACTIVE` 依赖有效时段内建立 WebSocket 连接，若仅走 HTTP 查询接口不会自动激活
- `CLOSED` 状态常量预留但当前无对应业务流转
