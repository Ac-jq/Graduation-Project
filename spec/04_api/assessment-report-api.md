# 测评与报告接口

## GET /api/scales
- 角色：`STUDENT`
- 请求头：`Authorization`
- 说明：查询当前已启用量表列表
- 返回字段：`id`、`code`、`name`、`description`、`totalQuestions`、`pageSize`

## GET /api/scales/{scaleId}
- 角色：`STUDENT`
- 请求头：`Authorization`
- 返回字段：`id`、`code`、`name`、`description`、`introduction`、`totalQuestions`、`pageSize`

## POST /api/scales/{scaleId}/sessions/draft
- 角色：`STUDENT`
- 请求头：`Authorization`
- 说明：创建或续用草稿作答会话
- 返回：`sessionId`、`scaleId`、`answeredCount`、`totalQuestions`、`status`

## GET /api/scales/sessions/{sessionId}/questions
- 角色：`STUDENT`
- 请求头：`Authorization`
- 查询参数：`pageNum`、`pageSize`
- 返回字段：
  - `sessionId`
  - `pageNum`
  - `pageSize`
  - `total`
  - `answeredCount`
  - `totalQuestions`
  - `records[]`
- `records[]` 内字段：`questionId`、`questionNo`、`content`、`selectedOptionId`、`options[]`

## PUT /api/scales/sessions/{sessionId}/answers
- 角色：`STUDENT`
- 请求头：`Authorization`、`Content-Type: application/json`
- Body：
```json
{
  "answers": [
    {
      "questionId": 1,
      "optionId": 2
    }
  ]
}
```

## POST /api/scales/sessions/{sessionId}/submit
- 角色：`STUDENT`
- 请求头：`Authorization`
- 返回字段：`sessionId`、`reportId`、`totalScore`、`levelCode`、`summaryText`

## GET /api/student/reports
- 角色：`STUDENT`
- 请求头：`Authorization`
- 返回：历史报告列表
- 字段：`reportId`、`scaleId`、`scaleName`、`totalScore`、`levelCode`、`summaryText`、`createdAt`

## GET /api/student/reports/{reportId}
- 角色：`STUDENT`
- 请求头：`Authorization`
- 返回字段：
  - `reportId`
  - `sessionId`
  - `scaleId`
  - `scaleName`
  - `studentUserId`
  - `studentName`
  - `studentNo`
  - `totalScore`
  - `levelCode`
  - `summaryText`
  - `aiInterpretation`
  - `recommendationNote`
  - `recommendAppointment`
  - `recommendedResources[]`
  - `createdAt`
- 备注：`recommendedResources` 来自已发布资源库；中高风险结果会返回 `recommendAppointment=true`

报告详情响应示例：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "reportId": 9,
    "scaleId": 1,
    "scaleName": "PHQ-9 抑郁情绪自评量表",
    "studentUserId": 1,
    "totalScore": 27,
    "levelCode": "HIGH",
    "summaryText": "当前结果提示情绪负担较高",
    "aiInterpretation": "你最近似乎承受了较高压力……",
    "recommendationNote": "Current result suggests that you should combine self-help resources with counselor support as soon as possible.",
    "recommendAppointment": true,
    "recommendedResources": [
      {
        "resourceId": 9,
        "title": "三分钟呼吸放松"
      }
    ]
  }
}
```

## GET /api/counselor/students/{studentUserId}/reports
## GET /api/counselor/students/{studentUserId}/reports/{reportId}
- 角色：`COUNSELOR`
- 请求头：`Authorization`
- 说明：仅允许访问已绑定学生的报告
- 常见异常：
  - `400 / code=600`：无权查看该学生报告、报告与学生不匹配

## 管理员量表接口

### GET /api/admin/scales
### GET /api/admin/scales/{scaleId}
- 角色：`ADMIN`
- 请求头：`Authorization`
- 说明：查询量表列表与详情

### POST /api/admin/scales
### PUT /api/admin/scales/{scaleId}
- 角色：`ADMIN`
- 请求头：`Authorization`、`Content-Type: application/json`
- 说明：创建或编辑量表与题目结构
- Body 顶层字段：
  - `code`
  - `name`
  - `description`
  - `introduction`
  - `pageSize`
  - `lowThreshold`
  - `mediumThreshold`
  - `highThreshold`
  - `questions[]`
- `questions[]` 字段：`questionNo`、`content`、`requiredFlag`、`options[]`
- `options[]` 字段：`optionCode`、`content`、`score`、`sortNo`

### POST /api/admin/scales/{scaleId}/activate
### POST /api/admin/scales/{scaleId}/deactivate
- 角色：`ADMIN`
- 请求头：`Authorization`
- 说明：控制量表是否对学生端可见

## 常见异常
- `600`：量表不存在或已停用、存在非法选项、题目与选项不匹配、当前作答会话已提交、量表尚未全部作答
