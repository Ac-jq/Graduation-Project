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

# 前端 UI 设计约定
1. 本项目所有前端页面、组件与视觉改造任务，只遵循项目本地 skill `jqpro-editorial-ui`。
2. skill 路径: `E:\Store\SDJZU\毕设\JQPro\.codex\skills\jqpro-editorial-ui\SKILL.md`。
3. 本地 skill 是本项目前端视觉第一约束，目标是“极简主义、杂志画报风、情绪安抚感”，严禁回退到传统 Admin Dashboard 的死板布局。
4. 新页面或改版页面必须先选页面范式，再写代码:
   - 表单、列表、单体交互页: 参考 `frontend/src/views/student/StudentScaleIntroRoute.vue`、`frontend/src/views/student/StudentAssessmentSessionRoute.vue` 与 `frontend/src/views/student/StudentAppointmentSlotRoute.vue`
   - 图文网格、资源库页: 参考 `frontend/src/views/student/StudentFavoriteRoute.vue` 与 `frontend/src/views/student/StudentResourceListRoute.vue`
   - 长文章、详情页: 参考 `frontend/src/views/student/StudentResourceDetailRoute.vue`
   - 工作台、首页、跨角色外壳: 参考 `frontend/src/layouts/RoleWorkspaceLayout.vue` 与 `frontend/src/views/student/StudentHomeRoute.vue`
5. 以下页面为冻结参考页，后续默认不可修改，除非用户明确要求直接改它们:
   - `frontend/src/views/student/StudentHomeRoute.vue`
   - `frontend/src/views/student/StudentFavoriteRoute.vue`
   - `frontend/src/views/student/StudentResourceListRoute.vue`
   - `frontend/src/views/student/StudentResourceDetailRoute.vue`
   - `frontend/src/views/student/StudentScaleIntroRoute.vue`
   - `frontend/src/views/student/StudentAssessmentSessionRoute.vue`
6. CSS 必须遵守以下铁律:
   - 严禁内容贴边，主内容必须配合 `max-width` 与居中容器使用
   - 严禁纯黑字色，统一使用深灰或墨色，如 `#1e2821`、`#2a362e`
   - 严禁硬边框，若需要分隔，仅允许低透明度线条
   - 严禁使用原生 `<table>` 承载页面主体数据，一律使用 Grid 或 Flex 卡片化表达
   - 标题默认使用 `'Noto Serif SC', serif`，正文与数据使用 `'Manrope', sans-serif`
   - 大标题默认使用 `clamp()`，卡片优先使用半透明渐变、模糊与柔和阴影
   - 所有可点击元素必须有 `transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);`
   - Hover 不得只变色，必须配合轻微上浮或位移与阴影变化
7. 每次生成前端代码前，先执行这句自检: “去除多余的线条，加大留白，加大标题，一切为了沉浸式的阅读体验。”

# 语言规则
1. 本项目默认语言为中文。
2. 后续所有界面文案、默认资源、默认量表、数据库种子数据与演示数据，默认优先使用中文。
3. 仅允许在次要标签、装饰性英文小字或品牌名中保留少量英文，不能影响主体中文阅读。
