# JQPro

高校心理健康自助与咨询协同平台后端项目。

当前仓库已经收口为纯后端交付状态，包含以下业务闭环：
- 认证与用户：登录、登出、改密、当前用户、学生档案、管理员用户管理
- 测评与报告：量表列表、答题草稿、提交、AI 解读、学生报告、咨询师查看学生报告
- AI 与咨询：学生 AI 会话、咨询师查看学生 AI 会话、匿名预约、通知中心、私密聊天室 HTTP + WebSocket
- 资源与治理：资源库、收藏、管理员资源治理、管理员量表管理、统计分析、管理员 AI 助手、审计日志
- 安全与合规：AI/聊天室消息密文存储、权限隔离、聊天室自动封存、关键动作审计
- 验收与交付：数据导入脚本、分阶段验收脚本、全量回归脚本、完整接口文档

## 技术栈

- Java 17+
- Spring Boot 3.2.12
- MyBatis-Plus
- MySQL 8.0
- Redis 7+
- WebSocket
- Spring AI
- Sa-Token

## 目录说明

- `src/main/java`：后端业务代码
- `src/main/resources`：配置、SQL 初始化脚本、模板与静态资源
- `scripts`：数据导入与自动化验收脚本
- `spec/01_Requirements`：任务书与 PRD
- `spec/02_Proposals`：阶段任务、测试数据、纯后端验收指南
- `spec/03_Design/backend`：后端设计与 PRD 核对清单
- `spec/04_api`：完整后端接口文档

## 默认账号

- 学生：`20230001 / Jqpro@123`
- 咨询师：`teacher01 / Jqpro@123`
- 管理员：`admin / Jqpro@123`
- 隔离学生：`20230002 / Jqpro@123`

## 环境要求

- JDK 17+，当前仓库默认使用 `E:\environment\JDK21\jdk-21.0.2\bin\java.exe`
- MySQL 8.0
- Redis 7+
- Node.js 与 npm
- 本机 `mysql.exe` 默认路径：`D:\DownLoad\mysql-8.0.33-winx64\bin\mysql.exe`

## 启动方式

### 1. IDEA 运行后端

在 IDEA 右上角运行配置中选择 `JQPro Backend`，点击运行。

该配置已固定使用：

```text
E:\environment\JDK21\jdk-21.0.2
```

后端基础地址：`http://127.0.0.1:8080`

### 2. IDEA 运行前端

在 IDEA 右上角运行配置中选择 `JQPro Frontend`，点击运行。

前端开发地址：`http://127.0.0.1:5173`

### 3. 可选：导入验收数据

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\import-acceptance-data.ps1
```

## 自动化验收

### 分阶段脚本

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\phase1-auth-profile.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\phase1-assessment-report.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\phase2-aichat-appointment.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\phase3-resource-governance.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\phase4-6-closure.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\phase7-security-regression.ps1
```

### 全量回归

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\full-acceptance.ps1
```

## 文档入口

- 后端 PRD 核对清单：`spec/03_Design/backend/prd-backend-checklist.md`
- 纯后端验收指南：`spec/02_Proposals/纯后端_测试验收指南.md`
- 接口文档总览：`spec/04_api/README.md`
- 脚本说明：`scripts/README.md`

## 当前状态

当前仓库已达到：
- 纯后端可运行
- 纯后端可回归
- 纯后端可验收
- 纯后端可答辩展示与交付
