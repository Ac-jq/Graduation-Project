# JQPro

高校心理健康平台项目。

## 启动前准备

启动前请先确认本机环境满足以下条件：

- JDK 17
- MySQL 8.0
- Redis
- Node.js 18+ 与 npm

## Redis 简单使用

本项目默认连接本机 Redis：

- 地址：`127.0.0.1:6379`

常用启动与查看方式如下。

Windows 如果你已经安装 Redis，可直接进入 Redis 安装目录执行：

```powershell
redis-server.exe
```

新开一个终端查看 Redis 是否正常：

```powershell
redis-cli.exe
ping
```

如果返回 `PONG`，说明 Redis 已正常启动。

查看当前库里有多少个 key：

```powershell
dbsize
```

查看常用信息：

```powershell
info server
info clients
info memory
```

查看某个 key 是否存在：

```powershell
exists 你的key
```

列出部分 key 进行简单检查：

```powershell
keys *
```

说明：

- `keys *` 只建议本地答辩演示时临时查看
- 正式环境不建议频繁使用 `keys *`

## 本地依赖配置

后端默认使用以下本地配置，已经写在 [application.yml](E:/Store/SDJZU/毕设/JQPro/src/main/resources/application.yml) 中，不依赖额外启动脚本或环境变量：

- MySQL：`127.0.0.1:3306/jqpro`
- 用户名：`root`
- 密码：`123456`
- Redis：`127.0.0.1:6379`
- 后端端口：`8080`

前端默认使用以下本地配置：

- 开发端口：`5173`
- 代理目标：`http://127.0.0.1:8080`

对应文件：

- [vite.config.ts](E:/Store/SDJZU/毕设/JQPro/frontend/vite.config.ts)
- [.env.development](E:/Store/SDJZU/毕设/JQPro/frontend/.env.development)

## 后端启动方式

后端按答辩要求，直接在 IDEA 中启动：

1. 用 IDEA 打开项目根目录 `E:\Store\SDJZU\毕设\JQPro`
2. 确认 `Project SDK` 和 `Run SDK` 都是 `JDK 17`
3. 找到 [JqProApplication.java](E:/Store/SDJZU/毕设/JQPro/src/main/java/sdu/jiaq/jqpro/JqProApplication.java)
4. 点击类左侧或右上角绿色三角直接运行

我已经处理了两个容易导致 IDEA 启动报环境错误的点：

- 去掉了运行配置里写死的 `JDK21` 路径
- 把 Lombok 注解处理改成了走项目 classpath，不再依赖固定本地 Maven 仓库路径

如果 IDEA 首次打开后还有 Lombok 报红，请确认：

1. 已安装 Lombok 插件
2. `Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors`
   中注解处理已启用

后端启动成功后访问：

- [http://127.0.0.1:8080](http://127.0.0.1:8080)

## 前端启动方式

前端不要用 IDEA 运行配置，直接在命令行启动即可。

进入目录：

```powershell
cd E:\Store\SDJZU\毕设\JQPro\frontend
```

首次安装依赖：

```powershell
npm install
```

启动开发服务：

```powershell
npm run dev
```

前端启动成功后访问：

- [http://127.0.0.1:5173](http://127.0.0.1:5173)

## 默认账号

- 管理员：`admin / Jqpro@123`
- 咨询师：`teacher01 / Jqpro@123`

## 启动自检

建议答辩前按下面顺序自检一次：

1. 先启动 MySQL 和 Redis
2. 在 IDEA 中启动后端 `JqProApplication`
3. 进入 `frontend` 目录执行 `npm run dev`
4. 打开前端页面登录测试

## 目录说明

- [src/main/java](E:/Store/SDJZU/毕设/JQPro/src/main/java)：后端源码
- [src/main/resources](E:/Store/SDJZU/毕设/JQPro/src/main/resources)：配置与 SQL
- [frontend](E:/Store/SDJZU/毕设/JQPro/frontend)：前端项目
- [scripts](E:/Store/SDJZU/毕设/JQPro/scripts)：历史脚本，当前手动启动不依赖这些脚本
