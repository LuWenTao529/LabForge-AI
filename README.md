# LabForge-AI 后端

LabForge-AI 是一个基于 Spring Boot 3 的 AI 代码生成与应用管理后端，整合 LangChain4j（DeepSeek 模型）、MyBatis-Flex、Reactor SSE，支持将用户输入转换为可落地的前端代码（HTML/CSS/JS），并提供生成、预览、部署、用户与应用管理等能力。

## 技术栈
- JDK 21、Spring Boot 3.5、Spring WebFlux（SSE）、Spring AOP
- MyBatis-Flex、HikariCP、MySQL 8
- LangChain4j（深度集成 DeepSeek Chat，支持流式输出）
- Hutool 工具集、Knife4j + springdoc-openapi

## 目录速览
- `src/main/java/com/winter/labforgeai`
  - `controller`：用户、应用、健康检查、静态资源预览等 HTTP 接口
  - `service`/`service.impl`：用户与应用领域服务，包含权限校验和业务编排
  - `core`：代码解析（`parser`）与落盘（`saver`）执行器，统一处理多文件/单文件生成
  - `ai`：AI 调用接口与工厂，绑定 LangChain4j ChatModel/StreamingChatModel
  - `model`：`dto`/`vo`/`entity`/`enums` 持久化与传输模型
  - `config`：CORS、JSON Long 精度处理等配置
- `src/main/resources`
  - `application.yml`、`application-local.yml`：环境配置与 DeepSeek Key 占位
  - `prompt/`：代码生成系统提示词（HTML/多文件/Vue 等）
  - `mapper/`：MyBatis-Flex XML
- `sql/create_table.sql`：数据库初始化脚本（user、app、chat_history）
- 运行时输出：`tmp/code_output/{codeType}_{appId}`（生成），`tmp/code_deploy/{deployKey}`（部署）

## 快速开始
1. **准备环境**：JDK 21、Maven 3.8+、MySQL 8。
2. **配置数据库与模型**：在 `src/main/resources/application.yml` 或 `application-local.yml` 中填写 `spring.datasource.*`；将 `langchain4j.open-ai.*.api-key` 替换为自己的 DeepSeek 密钥（请勿提交真实密钥）。
3. **初始化数据库**：执行 `sql/create_table.sql` 创建 `labforge_ai` 库与基础表。
4. **启动服务**
   ```bash
   mvn clean package -DskipTests
   java -jar target/LabForge-AI-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
   ```
   默认监听 `8123`，上下文路径 `/api`。
5. **接口文档**：启用 Knife4j 后可访问 `http://localhost:8123/api/doc.html`（按需调整路径）。

## 核心能力与流程
- **AI 代码生成（流式）**：`AiCodeGeneratorFacade` 统一路由 HTML / 多文件生成，基于 `AiCodeGeneratorService`（LangChain4j）获取流式内容，`CodeParserExecutor` 按代码块提取，`CodeFileSaverExecutor` 将生成结果写入 `tmp/code_output/{codeType}_{appId}`。
- **应用部署与预览**：
  - `POST /api/app/deploy` 将生成目录复制到 `tmp/code_deploy/{deployKey}` 并返回可访问的部署 URL。
  - `GET /api/static/{deployKey}/**` 从生成目录按 `deployKey` 提供静态文件访问（默认返回 `index.html`）。
- **权限体系**：`@AuthCheck` + `AuthInterceptor` 基于会话用户与角色（`admin`/`user`）做接口级校验。
- **通用返回**：`BaseResponse` + 全局异常处理，长整型统一转字符串避免前端精度丢失。

## 接口速览
- **健康检查**：`GET /api/health/` -> `"ok"`
- **用户模块**
  - `POST /api/user/register` `{userAccount,userPassword,checkPassword}`
  - `POST /api/user/login` `{userAccount,userPassword}`，基于会话维持登录
  - `POST /api/user/logout`
  - 管理端：`/api/user/add`、`/update`、`/delete`、`/list/page/vo`（需 `admin`）
- **应用模块**
  - `POST /api/app/add`：创建应用，默认 `codeGenType=MULTI_FILE`，名称取 `initPrompt` 前 12 字符
  - `GET /api/app/chat/gen/code`（SSE）：`appId`,`message`；每个数据块为 `data: {"d":"<chunk>"}`，末尾 `event: done`
  - `POST /api/app/deploy`：返回部署 URL
  - `POST /api/app/update`、`/delete`：仅限创建者；`/my/list/page/vo` 用户分页；`/good/list/page/vo` 精选分页（`priority=99`）；`/get/vo` 单条详情
  - 管理端：`/api/app/admin/update`、`/admin/delete`、`/admin/list/page/vo`、`/admin/get/vo`
- **静态资源**：`GET /api/static/{deployKey}/` 及其子路径访问已生成的 `index.html / style.css / script.js`

示例（流式生成）：
```bash
curl -N "http://localhost:8123/api/app/chat/gen/code?appId=1&message=写一个带动画的登陆页"
```

## 数据库说明
- `user`：账号、加密密码、角色、头像等，逻辑删除字段 `isDelete`
- `app`：应用名称、封面、初始化 Prompt、生成类型、`deployKey`、优先级、创建人等
- `chat_history`：预留的对话记录表，可按 `appId`、`createTime` 检索

## 运行时目录与部署
- 生成目录：`tmp/code_output/{codeType}_{appId}`（HTML 单文件或 HTML/CSS/JS 多文件）
- 部署目录：`tmp/code_deploy/{deployKey}`，`AppService#deployApp` 完成复制并记录 `deployKey`
- 访问域名：由 `AppConstant.CODE_DEPLOY_HOST` 决定（默认 `http://localhost`），可按需调整或由网关映射到 `/api/static/{deployKey}/`

## 提示词与模型
- 系统提示词位于 `src/main/resources/prompt/`，支持 HTML、Multi-file、Vue、代码质量检查等场景，可根据业务微调。
- LangChain4j 配置通过 `application*.yml` 注入，支持开启请求/响应日志与流式输出。

## 开发建议
- 使用 `mvn test` 前请准备测试数据库或开启隔离配置。
- 登录态依赖服务端 Session，调用需要携带返回的 Cookie。
- 生产环境请将 API Key 通过环境变量或密钥管理注入，并清理示例密钥。
