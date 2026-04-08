# 系统基础接口

## GET /api/system/ping
- 角色：公开接口
- 说明：返回固定 pong，用于健康检查

示例响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "pong",
  "timestamp": "2026-04-07T20:00:00"
}
```

## GET /api/system/business-error
- 角色：公开接口
- 说明：抛出业务异常，验证统一异常拦截器

示例错误响应：
```json
{
  "code": 600,
  "message": "这是一个用于验证全局异常处理的业务异常",
  "timestamp": "2026-04-07T20:00:00"
}
```
