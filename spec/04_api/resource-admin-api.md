# 资源库与管理员运维接口

## 学生资源库

### GET /api/resources/categories
### GET /api/resources/tags
### GET /api/resources
### GET /api/resources/{resourceId}
- 角色：任意已登录用户
- 请求头：`Authorization`
- 说明：查询资源分类、标签、已发布资源和资源详情
- `GET /api/resources` 查询参数：`categoryId`、`tagId`、`keyword`
- `GET /api/resources/{resourceId}` 备注：学生访问会记录浏览日志；资源详情只允许查看已发布资源

资源摘要字段：
- `resourceId`
- `title`
- `summaryText`
- `resourceType`
- `contentUrl`
- `coverUrl`
- `status`
- `publishedAt`
- `categoryId`
- `categoryName`
- `tags[]`
- `favorite`
- `favoriteCount`
- `viewCount`

### GET /api/student/favorites
### POST /api/student/favorites/{resourceId}
### DELETE /api/student/favorites/{resourceId}
- 角色：`STUDENT`
- 请求头：`Authorization`
- 说明：查询收藏、添加收藏、取消收藏
- 业务规则：仅允许收藏已发布资源

## 管理员资源治理

### GET /api/admin/resources
- 角色：`ADMIN`
- 请求头：`Authorization`
- 查询参数：`status`、`keyword`
- 说明：查询资源列表，支持草稿、发布、下线状态

### POST /api/admin/resources
### PUT /api/admin/resources/{resourceId}
- 角色：`ADMIN`
- 请求头：`Authorization`、`Content-Type: application/json`
- Body 字段：
  - `title`
  - `summaryText`
  - `resourceType`
  - `contentUrl`
  - `coverUrl`
  - `categoryId`
  - `tagIds[]`

请求示例：
```json
{
  "title": "三分钟呼吸放松",
  "summaryText": "帮助学生快速平稳情绪。",
  "resourceType": "AUDIO",
  "contentUrl": "https://example.com/resources/breathing",
  "coverUrl": "https://example.com/resources/breathing-cover.png",
  "categoryId": 1,
  "tagIds": [1, 3]
}
```

### POST /api/admin/resources/{resourceId}/publish
### POST /api/admin/resources/{resourceId}/offline
- 角色：`ADMIN`
- 请求头：`Authorization`
- 说明：发布/下线资源

### GET /api/admin/resource-categories
### POST /api/admin/resource-categories
### PUT /api/admin/resource-categories/{categoryId}
- 角色：`ADMIN`
- 请求头：`Authorization`
- 分类字段：`name`、`description`、`sortNo`、`status`

### GET /api/admin/resource-tags
### POST /api/admin/resource-tags
- 角色：`ADMIN`
- 请求头：`Authorization`
- 标签字段：`name`、`description`

## 管理员 AI 运维

### POST /api/admin/ai-tasks/parse
- 角色：`ADMIN`
- 请求头：`Authorization`、`Content-Type: application/json`
- Body：`instruction`
- 已支持指令：
  - 启用/禁用账号
  - 创建咨询师账号
  - 资源发布/下线
- 解析后返回待执行任务清单与字段新旧值对比，不会直接落库

请求示例：
```json
{
  "instruction": "offline resource \"三分钟呼吸放松\""
}
```

### GET /api/admin/ai-tasks
### GET /api/admin/ai-tasks/{taskId}
### POST /api/admin/ai-tasks/{taskId}/confirm
### POST /api/admin/ai-tasks/{taskId}/cancel
- 角色：`ADMIN`
- 请求头：`Authorization`
- 说明：查看任务、确认执行、取消任务

## 常见异常
- `600`：资源不存在、资源未发布、资源分类不存在、资源分类已停用、资源标签无效、AI 指令无法解析、目标账号不存在
