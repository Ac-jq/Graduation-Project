# 统计、审计与系统接口

## 管理员统计

### GET /api/admin/statistics/overview
- 角色：`ADMIN`
- 请求头：`Authorization`
- 说明：返回学生数、咨询师数、报告数、AI 会话数、预约数、资源数、发布资源数、浏览数、收藏数、日活等总览指标

### GET /api/admin/statistics/assessments
- 角色：`ADMIN`
- 请求头：`Authorization`
- 说明：返回测评报告总量、参与人数、平均分、等级分布、量表维度统计、前后测对比摘要

### GET /api/admin/statistics/resources
- 角色：`ADMIN`
- 请求头：`Authorization`
- 说明：返回资源总量、发布量、总浏览、总收藏、分类统计、热门资源排行

### GET /api/admin/statistics/appointments
- 角色：`ADMIN`
- 请求头：`Authorization`
- 说明：返回预约总量、学院分布、咨询师负载等聚合数据

### GET /api/admin/statistics/export
- 角色：`ADMIN`
- 请求头：`Authorization`
- 查询参数：`dimension`
- 支持值：`college`、`grade`、`gender`
- 返回字段：
  - `dimension`
  - `dimensionValue`
  - `studentCount`
  - `reportCount`
  - `averageScore`
  - `aiSessionCount`
  - `appointmentCount`
  - `resourceViewCount`
  - `favoriteCount`
- 备注：导出数据为聚合维度数据，不返回个人明细

## 管理员审计日志

### GET /api/admin/audit-logs
- 角色：`ADMIN`
- 请求头：`Authorization`
- 查询参数：`actionCode`、`keyword`
- 返回字段：`logId`、`userId`、`userDisplayName`、`actionCode`、`actionName`、`detailText`、`ipAddress`、`createdAt`

已覆盖的关键审计类型包括：
- `LOGIN`
- `LOGOUT`
- `CHANGE_PASSWORD`
- `PROFILE_UPDATE`
- `ASSESSMENT_SUBMIT`
- `APPOINTMENT_CREATE`
- `APPOINTMENT_ACCEPT`
- `APPOINTMENT_REJECT`
- `RESOURCE_FAVORITE_ADD`
- `RESOURCE_FAVORITE_REMOVE`
- `ADMIN_RESOURCE_*`
- `ADMIN_SCALE_*`
- `ADMIN_USER_*`
- `ADMIN_AI_*`

## 系统接口

### GET /api/system/ping
- 角色：公开接口
- 说明：服务存活检查

### GET /api/system/business-error
- 角色：公开接口
- 说明：用于验证全局异常处理行为

## 统一异常说明

### 业务异常
- 触发类：`BusinessException`
- 返回：`code=600` 或对应统一错误码，HTTP 状态通常为 `400`

### 登录异常
- 返回：`401`

### 权限异常
- 返回：`403`

### 校验异常
- 返回：`422`
- 典型场景：请求体字段长度超限、必填参数缺失
