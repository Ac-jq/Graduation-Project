# 第 1 期后端设计说明

## 1. 目标与范围

第 1 期后端目标是打通第一个可运行业务闭环：

- 登录鉴权
- 学生档案维护
- 量表作答
- 自动评分
- AI 解读
- 报告归档

本期以 `P0` 核心能力为中心，不扩展聊天、预约和运营治理能力。

## 2. 模块划分

当前后端按职责拆分为以下模块：

| 模块 | 责任 |
| --- | --- |
| `controller.auth` | 登录、登出、当前用户、改密 |
| `controller.student` | 学生档案、学生报告 |
| `controller.assessment` | 量表列表、详情、会话、题目、答案、提交 |
| `controller.counselor` | 咨询师查看绑定学生报告 |
| `service.auth` | 认证、密码校验、登录态处理、审计日志写入 |
| `service.studentProfile` | 学生档案查询与更新 |
| `service.assessment` | 量表作答流程、评分、报告生成 |
| `service.report` | 学生/咨询师视角报告读取 |
| `service.aiInterpretation` | 量表 AI 解读，支持模型不可用时回退内置文案 |
| `service.auditLog` | 关键行为审计日志落库 |

基础支撑模块：

- `common.result`
  - 统一 `Result<T>` 返回体
- `common.exception`
  - 通用 `BusinessException`
- `common.handler`
  - 全局异常处理器
- `config`
  - `Sa-Token`、`MyBatis-Plus`、初始化数据、Web 配置

## 3. 数据模型

本期核心表如下：

### 3.1 用户与权限域

| 表名 | 用途 | 关键字段 |
| --- | --- | --- |
| `sys_role` | 角色字典 | `code`、`name` |
| `sys_user` | 登录账号与身份信息 | `account`、`password_salt`、`password_hash`、`role_code`、`student_no`、`counselor_no`、`status` |
| `student_profile` | 学生扩展档案 | `user_id`、`college`、`grade`、`gender`、`phone`、`emergency_contact`、`counselor_user_id` |
| `counselor_student` | 咨询师与学生绑定关系 | `counselor_user_id`、`student_user_id` |

### 3.2 测评域

| 表名 | 用途 | 关键字段 |
| --- | --- | --- |
| `mental_scale` | 量表主表 | `code`、`name`、`page_size`、`medium_threshold`、`high_threshold`、`status` |
| `mental_scale_question` | 量表题目 | `scale_id`、`question_no`、`content`、`required_flag` |
| `mental_scale_option` | 题目选项 | `question_id`、`option_code`、`content`、`score`、`sort_no` |
| `mental_scale_session` | 作答会话 | `scale_id`、`user_id`、`status`、`answered_count`、`total_score`、`submitted_at` |
| `mental_scale_answer` | 作答答案 | `session_id`、`question_id`、`option_id`、`score` |
| `mental_scale_report` | 报告归档 | `session_id`、`scale_id`、`user_id`、`level_code`、`total_score`、`summary_text`、`ai_interpretation` |

### 3.3 审计域

| 表名 | 用途 | 关键字段 |
| --- | --- | --- |
| `sys_audit_log` | 关键行为审计 | `user_id`、`action_code`、`action_name`、`detail_text`、`ip_address` |

## 4. 权限边界

### 4.1 角色边界

当前已落地角色：

- `STUDENT`
- `COUNSELOR`
- `ADMIN`

其中第 1 期实际开放的接口边界如下：

| 接口域 | 学生 | 咨询师 | 管理员 |
| --- | --- | --- | --- |
| 登录/当前用户/改密 | 可访问 | 可访问 | 可访问 |
| 学生档案 `/api/student/profile/**` | 仅本人 | 不可访问 | 不可访问 |
| 量表 `/api/scales/**` | 仅本人作答 | 不可访问 | 不可访问 |
| 学生报告 `/api/student/reports/**` | 仅本人 | 不可访问 | 不可访问 |
| 咨询师查看学生报告 `/api/counselor/students/{studentUserId}/reports/**` | 不可访问 | 仅绑定学生 | 不可访问 |

### 4.2 资源边界

除角色外，还做了资源归属校验：

- 学生只能操作自己的 `mental_scale_session`
- 学生只能查看自己的 `mental_scale_report`
- 咨询师只能查看 `counselor_student` 中已绑定学生的报告
- 学生档案更新只开放扩展资料字段，不开放账号、学号、真实姓名等核心字段

## 5. 异常处理

### 5.1 统一返回

所有 Controller 使用统一结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-04-07T10:00:00"
}
```

### 5.2 统一异常策略

业务异常统一抛出 `BusinessException`，由 `GlobalExceptionHandler` 转换为标准响应。

当前异常分层：

- 参数校验异常
  - HTTP `422`
  - 业务码 `422`
- 未登录
  - HTTP `401`
  - 业务码 `401`
- 角色无权限
  - HTTP `403`
  - 业务码 `403`
- 业务资源校验失败
  - HTTP `400`
  - 业务码 `600`
- 未兜住的系统异常
  - HTTP `500`
  - 业务码 `500`

### 5.3 第 1 期常见业务异常

- 账号或密码错误
- 用户被禁用
- 学生档案不存在
- 量表不存在或未启用
- 作答会话不属于当前学生
- 会话已提交不能再修改
- 题目与选项不匹配
- 量表未全部作答不能提交
- 咨询师越权查看未绑定学生报告

## 6. 审计日志

当前已明确落审计日志的行为：

- 登录成功
- 主动登出
- 修改密码
- 提交量表并生成报告

现状说明：

- 审计日志只记录关键动作，不记录敏感明文正文
- IP 从 `HttpServletRequest` 中提取
- 日志结构已可支撑后续后台查询，但第 1 期尚未提供管理员查询接口

## 7. 验收方式

第 1 期建议按以下顺序验收：

1. 认证闭环
   - 登录
   - 获取当前用户
   - 修改密码
   - 恢复默认密码
   - 登出
2. 学生档案闭环
   - 查询本人档案
   - 更新允许维护的字段
3. 量表闭环
   - 查询量表
   - 获取量表详情
   - 创建草稿会话
   - 查询题目
   - 保存答案
   - 提交量表
4. 报告闭环
   - 学生查看自己的报告列表和详情
   - 咨询师查看绑定学生的报告列表和详情

验收工具：

- 优先使用 `scripts/phase1-auth-profile.ps1`
- 优先使用 `scripts/phase1-assessment-report.ps1`

## 8. 当前已知限制

- 角色模型当前是一人一主角色，未实现多角色叠加
- 量表评分规则目前按简单累计分值处理，后续如扩展更多量表需继续配置化
- 管理员侧暂无第 1 期可见的业务接口，仅保留账号与角色数据
- 审计日志已有落库能力，但暂无可视化查询页面或 API
