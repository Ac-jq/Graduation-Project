# 验收脚本说明

## 当前脚本清单

- `common.ps1`
  - 公共请求、断言、鉴权头、数据库查询函数
- `import-acceptance-data.ps1`
  - 自动读取 `application.yml` 中的 MySQL 配置并导入验收 SQL
- `phase1-auth-profile.ps1`
  - 验证登录、当前用户、学生档案、改密、登出
- `phase1-assessment-report.ps1`
  - 验证量表、答题草稿、提交、报告、咨询师查看学生报告
- `phase2-aichat-appointment.ps1`
  - 验证学生 AI 会话、匿名预约、通知中心、聊天室 HTTP 查询
- `phase3-resource-governance.ps1`
  - 验证资源库、收藏、管理员资源治理、统计分析、管理员 AI 助手、审计日志
- `phase4-6-closure.ps1`
  - 验证学生档案补充字段、测评报告联动、咨询师学生列表、管理员用户管理、管理员量表管理
- `phase7-security-regression.ps1`
  - 验证 AI 消息密文落库、权限隔离、聊天室自动封存相关能力
- `full-acceptance.ps1`
  - 顺序执行全部纯后端验收脚本

## 推荐执行顺序

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\import-acceptance-data.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\full-acceptance.ps1
```

后端与前端请优先使用 IDEA 运行配置 `JQPro Backend` 和 `JQPro Frontend` 启动。

## WebSocket 验收说明

聊天室 WebSocket 建议使用 Postman、Apifox、Insomnia 或 `websocat` 手工验证。

连接地址格式：
```text
ws://127.0.0.1:8080/ws/consult-chat?appointmentId={appointmentId}&token={token}
```

发送消息体：
```json
{
  "content": "test message"
}
```
