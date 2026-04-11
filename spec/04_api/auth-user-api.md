# 认证与用户接口

## POST /api/auth/login
- 角色：公开接口
- 请求头：`Content-Type: application/json`
- Body：`account`、`password`

请求示例：
```json
{
  "account": "20230001",
  "password": "Jqpro@123"
}
```

成功 `data` 字段：`token`、`userId`、`account`、`roleCode`、`displayName`、`roles`

## POST /api/auth/logout
- 角色：已登录用户
- 请求头：`Authorization`
- 说明：主动退出登录

## POST /api/auth/change-password
- 角色：已登录用户
- 请求头：`Authorization`
- Body：`oldPassword`、`newPassword`、`confirmPassword`

## GET /api/auth/current-user
- 角色：已登录用户
- 请求头：`Authorization`
- 返回：`userId`、`account`、`roleCode`、`realName`、`displayName`、`studentNo`、`counselorNo`、`roles`

## GET /api/student/profile/me
- 角色：`STUDENT`
- 请求头：`Authorization`
- 返回字段：
  - `userId`
  - `account`
  - `realName`
  - `displayName`
  - `studentNo`
  - `avatarUrl`
  - `college`
  - `grade`
  - `gender`
  - `phone`
  - `emergencyContact`
  - `emergencyPhone`
  - `counselorUserId`

响应示例：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "account": "20230001",
    "realName": "张同学",
    "displayName": "向日葵同学",
    "studentNo": "20230001",
    "avatarUrl": "https://example.com/avatar/student-20230001.png",
    "college": "软件学院",
    "grade": "2023级",
    "gender": "男",
    "phone": "13800000000",
    "emergencyContact": "张家长",
    "emergencyPhone": "13900000000",
    "counselorUserId": 2
  }
}
```

## PUT /api/student/profile/me
- 角色：`STUDENT`
- 请求头：`Authorization`、`Content-Type: application/json`
- Body：
  - `avatarUrl`：头像地址，最大 500 字符
  - `college`
  - `grade`
  - `gender`
  - `phone`
  - `emergencyContact`
  - `emergencyPhone`
- 备注：学号、账号、真实姓名、展示名不可通过该接口修改

请求示例：
```json
{
  "avatarUrl": "https://example.com/avatar/updated-student.png",
  "college": "Software College",
  "grade": "2023",
  "gender": "Male",
  "phone": "13800138111",
  "emergencyContact": "Parent Zhang",
  "emergencyPhone": "13900139111"
}
```

## GET /api/counselor/students
- 角色：`COUNSELOR`
- 请求头：`Authorization`
- 说明：返回当前咨询师已绑定学生列表
- 返回字段：`studentUserId`、`studentName`、`studentNo`、`college`、`grade`、`gender`

## GET /api/admin/users
- 角色：`ADMIN`
- 请求头：`Authorization`
- 查询参数：`roleCode`、`status`、`keyword`
- 返回字段：`userId`、`account`、`roleCode`、`realName`、`displayName`、`studentNo`、`counselorNo`、`status`、`college`、`grade`、`createdAt`

## POST /api/admin/users/counselors
- 角色：`ADMIN`
- 请求头：`Authorization`、`Content-Type: application/json`
- Body：`account`、`displayName`、`realName`、`counselorNo`
- 备注：默认密码固定为 `Jqpro@123`

## POST /api/admin/users/{userId}/enable
## POST /api/admin/users/{userId}/disable
## POST /api/admin/users/{userId}/reset-password
- 角色：`ADMIN`
- 请求头：`Authorization`
- 说明：启用、禁用、重置账号密码

## 常见异常
- `401`：未登录
- `403`：角色无权限
- `600`：账号或密码错误、当前账号已被禁用、用户不存在、咨询师工号已存在等业务错误
