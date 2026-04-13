# 第五阶段：AI 解读与报告闭环

## 完成范围

- 修正测评报告 AI 解读的 DeepSeek 配置入口
- 将 AI 配置改为环境变量驱动，不在源码中硬编码 key
- 重写系统提示词与用户提示词模板
- 为高、中、低三个等级补齐本地安全兜底文案
- 增加敏感词净化与回退机制
- 修正报告摘要、建议文案与非诊断性声明
- 增加第五阶段验收脚本

## 配置方式

后端默认关闭 AI 解读远程调用，启用时通过环境变量注入：

- `JQPRO_AI_INTERPRETATION_ENABLED=true`
- `JQPRO_AI_INTERPRETATION_API_KEY=你的 DeepSeek Key`
- `JQPRO_AI_INTERPRETATION_BASE_URL=https://api.deepseek.com`
- `JQPRO_AI_INTERPRETATION_MODEL=deepseek-chat`

未注入 key 或未启用时，系统自动回退到本地安全模板，不会阻断报告生成。

## 报告约束

- 报告始终携带固定非诊断性声明
- AI 解读中禁止出现诊断化敏感词
- 如模型返回包含敏感词，系统自动回退到安全模板
- 高风险报告默认建议预约咨询

## 验收方式

1. 启动后端：`powershell -ExecutionPolicy Bypass -File E:\Store\SDJZU\毕设\JQPro\scripts\run_backend_phase3.ps1`
2. 执行阶段五验收：`powershell -ExecutionPolicy Bypass -File E:\Store\SDJZU\毕设\JQPro\scripts\verify_assessment_phase5.ps1`

## 验收点

- `PHQ9` 低、中、高三组报告生成成功
- 提交响应与报告详情都带固定声明
- 报告摘要、AI 解读、建议文案均非空
- AI 解读与建议文案中不包含敏感诊断词
- 高风险报告建议预约咨询
- 咨询师查看学生报告详情时，报告内容与学生端结构一致
