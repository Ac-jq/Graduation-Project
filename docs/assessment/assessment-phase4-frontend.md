# 第四阶段：前端测评闭环

## 完成范围

- 学生端量表列表页
- 学生端量表说明页
- 学生端分页作答页
- 学生端提交结果页
- 学生端历史报告列表页
- 学生端报告详情页
- 咨询师端学生报告列表页
- 咨询师端学生报告详情页
- 工作台侧边栏中文文案与导航整理
- 前端构建与预览脚本

## 页面闭环

### 学生端

- `/student/scales`
  - 读取量表列表
  - 只展示 `PHQ9` 与 `GAD7`
  - 展示量表定位、题量、分页、免责声明
- `/student/scales/:scaleId`
  - 展示量表导语、评分说明、固定声明
  - 创建或复用草稿会话后进入作答页
- `/student/assessment-sessions/:sessionId`
  - 分页拉取题目
  - 支持选项选择、暂存、翻页
  - 提交后跳转结果页
- `/student/assessment-results/:reportId`
  - 展示本次总分、等级、摘要
  - 跳转完整报告页
- `/student/reports`
  - 展示历史报告列表与最近一次报告卡片
- `/student/reports/:reportId`
  - 展示总分、等级、AI 辅助解释、推荐资源、固定声明

### 咨询师端

- `/counselor/students/:studentUserId/reports`
  - 展示绑定学生的报告列表
- `/counselor/students/:studentUserId/reports/:reportId`
  - 展示单份报告详情、AI 辅助解释、推荐资源、固定声明

## 验证方式

1. 运行后端：`powershell -ExecutionPolicy Bypass -File E:\Store\SDJZU\毕设\JQPro\scripts\run_backend_phase3.ps1`
2. 运行前端：`powershell -ExecutionPolicy Bypass -File E:\Store\SDJZU\毕设\JQPro\scripts\run_frontend_phase4.ps1`
3. 执行验收：`powershell -ExecutionPolicy Bypass -File E:\Store\SDJZU\毕设\JQPro\scripts\verify_frontend_phase4.ps1`

## 验收点

- 前端 `npm run build` 成功
- 预览服务可返回 `index.html`
- 后端量表接口可返回 `PHQ9` 与 `GAD7`
- 学生端与咨询师端相关路由可正常进入
- 所有第四阶段页面中文文案已纠正
- 提交流程改为先进入结果页，再进入完整报告页
