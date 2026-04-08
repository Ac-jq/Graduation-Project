# 纯后端接口文档总览

JQPro 当前仓库已切换为纯后端交付状态。

## 1. 基础信息

- 基础地址：`http://127.0.0.1:8080`
- 鉴权方式：Sa-Token
- 认证请求头：`Authorization: <token>`
- 统一返回：`Result<T>`

统一响应结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-04-07T20:00:00"
}
```

## 2. 角色

- `STUDENT`：学生
- `COUNSELOR`：心理咨询师
- `ADMIN`：管理员

## 3. 通用错误码

| code | HTTP | 含义 |
| --- | --- | --- |
| 200 | 200 | 成功 |
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未登录或登录失效 |
| 403 | 403 | 无权访问 |
| 404 | 404 | 资源不存在 |
| 405 | 405 | 方法不允许 |
| 422 | 422 | 参数校验失败 |
| 600 | 400 | 业务处理失败 |
| 500 | 500 | 系统异常 |

## 4. 文档索引

- [auth-user-api.md](E:/Store/SDJZU/毕设/JQPro/spec/04_api/auth-user-api.md)
- [assessment-report-api.md](E:/Store/SDJZU/毕设/JQPro/spec/04_api/assessment-report-api.md)
- [ai-appointment-chat-api.md](E:/Store/SDJZU/毕设/JQPro/spec/04_api/ai-appointment-chat-api.md)
- [resource-admin-api.md](E:/Store/SDJZU/毕设/JQPro/spec/04_api/resource-admin-api.md)
- [statistics-audit-api.md](E:/Store/SDJZU/毕设/JQPro/spec/04_api/statistics-audit-api.md)
- [system-api.md](E:/Store/SDJZU/毕设/JQPro/spec/04_api/system-api.md)

## 5. 特殊说明

- 学生 AI 对话消息在数据库中以加密密文存储，接口出参为解密后的业务文本。
- 私密聊天室消息在数据库中以加密密文存储，聊天室超时后会自动封存。
- 学生预约、聊天室等场景采用匿名代号，不向学生端暴露咨询师真实身份。
- 管理员无权直接查看学生私密聊天室明文记录；管理员仅可查看宏观统计、审计日志和系统管理数据。
