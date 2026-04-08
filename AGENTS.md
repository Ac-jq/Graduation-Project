# 项目基础规范
- 后端引擎: Java 17 + Spring Boot 3.2.12
- 持久层: MyBatis-Plus 3.5.5 + MySQL 8.0（用户名root，密码123456）
- 鉴权与安全: Sa-Token + Bouncy Castle 密码学组件
- 中间件规范: Redis + WebSocket
- AI 引擎: Spring AI
- 顶级包名: sdu.jiaq.jqpro

# 编码约定
1. 所有 Controller 接口必须返回统一的全局 Result 类。
2. 任何业务异常必须抛出统一的自定义异常类（BusinessException），并由全局异常处理器统一拦截处理。
3. 必须包含完善的注释。
4. 每完成一个闭环，必须启动项目，使用 curl 测试接口，如有 bug 自我修复。