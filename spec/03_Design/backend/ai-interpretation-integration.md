# 测评报告 AI 解读接入说明

## 目标

本次改造仅调整“学生测评报告 AI 解读”链路，保持现有提交量表、生成报告和返回结构不变。系统不再使用本地模板文案冒充 AI 结果，必须依赖外部模型接口。

## 配置项

配置前缀：`jqpro.ai.interpretation`

- `enabled`：是否启用测评报告 AI 解读。默认 `false`。
- `base-url`：模型服务基础地址，例如 `https://your-api-host`。
- `path`：OpenAI 兼容聊天补全接口路径，默认 `/v1/chat/completions`。
- `api-key`：模型服务鉴权密钥。
- `model`：模型名称，例如 `gpt-4o-mini` 或其他兼容模型名。
- `auth-header-name`：鉴权请求头名称，默认 `Authorization`。
- `auth-prefix`：鉴权前缀，默认 `Bearer `。如果你的接口不需要前缀，可设为空字符串。
- `temperature`：生成温度，默认 `0.4`。
- `max-tokens`：最大输出 token，默认 `600`。
- `timeout-seconds`：连接和读取超时时间，默认 `60` 秒。
- `system-prompt`：系统提示词，用于约束语气、风险边界和输出格式。
- `user-prompt-template`：业务提示词模板，支持占位符：
  - `{scaleName}`
  - `{scaleDescription}`
  - `{scaleIntroduction}`
  - `{totalScore}`
  - `{riskLevel}`
  - `{thresholdSummary}`

## 如何替换成你的 API

1. 将 `jqpro.ai.interpretation.enabled` 改为 `true`。
2. 填入你的 `base-url`、`api-key`、`model`。
3. 如果你的服务不是标准 `/v1/chat/completions`，同步修改 `path`。
4. 如果鉴权头或前缀不同，修改 `auth-header-name` 与 `auth-prefix`。
5. 重启应用后重新提交量表，报告生成时会实时调用外部模型。

## 失败表现

以下情况都会通过 `BusinessException` 返回统一业务错误，不再返回伪造 AI 文案：

- 未启用：返回 `AI解读服务未启用，请联系管理员配置`
- 缺少 `base-url` / `api-key` / `model` / `path`：返回 `AI解读服务未配置完整：缺少 xxx`
- 外部接口超时或调用失败：返回 `AI解读生成失败，请稍后重试`
- 接口返回空内容：返回 `AI解读服务返回空内容，请检查模型配置`
- 接口返回结构不兼容：返回 `AI解读服务返回格式无法解析，请检查模型接口兼容性`

## 接口影响

- 量表提交接口路径不变：`POST /api/scales/sessions/{sessionId}/submit`
- 报告查询接口路径不变
- 报告表结构不变
- 返回结构不变，只是 `aiInterpretation` 字段的生成来源变成强依赖外部 AI 服务
