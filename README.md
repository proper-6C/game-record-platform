# 游戏对局记录分享平台

基于 Spring Boot 3 + MyBatis-Plus + MySQL 8 的后端 + Vue 3 + Element Plus 前端，提供**对局上传、评论、点赞**三大功能。

## 项目结构

```
First-db/
├── pom.xml                     # 后端 Maven 构建配置
├── sql/init.sql                # 数据库初始化脚本
├── src/main/java/...           # 后端源码（Spring Boot）
├── src/main/resources/application.yml   # 后端配置
├── frontend/                   # 前端（Vue 3 + Vite + Element Plus）
│   ├── package.json
│   ├── vite.config.js          # 开发代理 /api、/uploads -> 8080
│   └── src/
│       ├── api/index.js        # axios 封装 + 全部接口
│       ├── router/index.js
│       ├── App.vue             # 导航布局
│       └── views/              # 登录/列表/详情/上传 四个页面
├── scripts/api-smoke-test.py   # 后端接口冒烟测试（19 项断言）
└── uploads/                    # 上传文件目录
```

## 技术栈

| 端 | 组件 | 版本 |
| --- | --- | --- |
| 后端 | JDK / Spring Boot / MyBatis-Plus / MySQL | 17 / 3.3.5 / 3.5.7 / 8.0 |
| 前端 | Vue / Vite / Element Plus / Axios / Vue Router | 3.5 / 5.4 / 2.9 / 1.7 / 4.5 |

## 启动步骤

### 1. 初始化数据库

确保本机 MySQL 8 服务已启动（本机服务名 `MySQL80`），然后执行建库脚本：

```sql
-- 在 Navicat / IDEA Database / 命令行中执行 sql/init.sql
source sql/init.sql;
```

脚本会创建 `game_record` 库和 4 张表：`user`、`game_record`、`comment`、`like_record`。

### 2. 修改数据库密码

打开 `src/main/resources/application.yml`，把 `password` 改成你本机 MySQL 的 root 密码（本机当前已配置为 `123456`）：

```yaml
spring:
  datasource:
    username: root
    password: 你的密码
```

### 3. 启动后端

- **方式一（推荐）**：用 IDEA 打开项目根目录 `First-db` → 等待 Maven 下载依赖 → 运行 `GameRecordApplication.main()`（Project SDK 指向 `D:\soft\tools\jdk-17`）。
- **方式二（命令行）**：

  ```bash
  mvn spring-boot:run
  ```

- **方式三（生产方式，打可执行 jar）**：

  ```bash
  mvn clean package -DskipTests
  java -jar target/game-record-1.0.0.jar
  ```

  或双击 `scripts/start-backend.bat` 后台启动；`scripts/start-all.bat` 可一键打包并同时启动后端（8080）与前端静态预览（8081）。

### 4. 启动前端

```bash
cd frontend
npm install     # 首次需要
npm run dev     # 打开 http://localhost:5173
```

开发环境由 Vite 自动把 `/api` 和 `/uploads` 代理到后端 8080，无需额外跨域配置。

## Docker 部署（可选，需要安装 Docker）

1. 构建前端产物：`cd frontend && npm install && npm run build`（生成 `frontend/dist`）。
2. 进 `deploy/` 目录执行 `docker compose up -d`：
   - 后端镜像（`backend.Dockerfile`：Maven 多阶段构建 → JRE 17 运行）；
   - 前端镜像（nginx 托管 `dist`，`nginx.conf` 把 `/api`、`/uploads` 反代到 `backend:8080`）；
   - 默认复用宿主机 MySQL（后端通过 `DB_HOST=host.docker.internal` 环境变量连接，`application.yml` 已支持环境变量覆盖）；如需容器化 MySQL，取消 `docker-compose.yml` 中 mysql 服务的注释即可。
3. 访问 `http://localhost`。上传目录通过 volume `game-record-uploads` 持久化。

### 5. 验证

后端验证（演示账号 `player1 / 123456`）：

```bash
curl -X POST http://localhost:8080/api/user/login -H "Content-Type: application/json" -d "{\"username\":\"player1\",\"password\":\"123456\"}"
```

前端验证：浏览器打开 `http://localhost:5173` → 登录 → 上传对局 → 详情页评论/点赞。

### 6. 自动化冒烟测试（可选）

```bash
python scripts/api-smoke-test.py
```

覆盖注册、登录、上传、评论、点赞、删除共 19 项断言；需后端已启动且本机装有 Python 3。

## 前端页面

| 页面 | 路由 | 功能 |
| --- | --- | --- |
| 登录/注册 | /login | 登录、注册（Element Plus Tabs） |
| 主页 | / | 电竞霓虹动态主页：霓虹星空、精选对局、平台数据（真实统计）、热门游戏榜 / 玩家获赞榜 |
| 对局列表 | /records | 卡片网格、按游戏名搜索、最新/热门排序、分页 |
| 对局详情 | /record/:id | 视频/图片播放、点赞、评论与楼中楼回复、删除自己的内容 |
| 上传对局 | /upload | 拖拽上传（类型/大小校验）、对局信息表单 |
| 个人中心 | /profile | 我的对局 / 我的点赞（Tabs 分页、可删除）+ 数据看板统计卡片 |
| 站内通知 | 头部铃铛 | 被点赞/评论/回复时通知，未读红点、点击跳转对局、全部已读 |
| 实时通知（SSE） | 头部铃铛 | 点赞/评论发生时经 SSE 长连接实时推送，铃铛即时亮起闪烁，无需刷新 |
| 玩家主页 | /user/:id | 玩家公开主页：头像昵称、注册时间、作品数/获赞数、作品墙（分页），卡片作者可点击直达 |
| 氛围音乐 | 主页右下角 | Web Audio 合成霓虹电子乐（和弦 pad + 琶音 + 延迟），点击开关、记忆偏好、离开主页自动停止 |
| 举报与后台 | /admin | 详情页举报（原因+说明）；管理员后台待处理/已处理，驳回或下架对局、恢复上架 |
| 敏感词管理 | /admin 标签页 | 敏感词存数据库，后台增删、评论实时过滤 |
| 标签系统 | 上传页 | 对局可加最多 5 个标签（翻盘/五杀等），列表与详情展示 |
| 对局编辑 | 详情页 | 上传者可编辑对局信息（文件/封面不变），非本人 403 |
| 筛选增强 | 列表页 | 支持按游戏名 / 段位 / 标签组合筛选，可与最新/热门排序叠加 |
| 暗色模式 | 全局 | 顶栏太阳/月亮切换，Element Plus + 页面样式随 html.dark 变暗，localStorage 记住选择 |
| 部署打包 | 全栈 | 后端可执行 jar + Windows 启动脚本 + Docker 镜像/nginx/docker-compose 编排 |

## 已验证情况

- 后端：Maven 编译 38 个源文件全部通过（**JDK 17** / Spring Boot 3.3.5）
- 后端接口冒烟测试：19/19 全部通过（含权限拦截、防重复点赞、敏感词过滤、楼中楼回复、级联删除）
- 个人中心接口：`/api/record/liked`（我的点赞）与 `/api/record/list?userId=`（我的对局）实测通过，未登录访问返回 401
- 数据看板：`/api/user/stats` 实测通过（胜率、常用游戏 Top3 聚合正确；造数据验证后已清理）
- 站内通知：双账号实测通过——点赞/评论/回复均正确触发通知，未读数、单条已读、全部已读正常，未登录 401，删除对局级联清理通知
- 前端：`npm run build` 构建通过
- 前端按需引入：Element Plus 组件/样式按需加载 + 图标局部导入后，主包由 **1,206 kB → 303 kB**（gzip 387 → 111 kB），构建时间 26s → 6.3s，超大 chunk 警告消除
- 前后端联调：经 Vite 代理全链路验证通过（登录→上传→评论→点赞→列表→删除）
- 已处理：MySQL 保留字 `rank`（反引号转义）；根路径访问返回友好提示而非 500

## 接口清单

所有接口返回统一格式：`{ "code": 200, "message": "成功", "data": ... }`
登录后的请求需携带请求头：`Authorization: Bearer <登录返回的token>`

> 认证方案：**JWT 无状态令牌**（HS256，默认 7 天有效，配置见 `application.yml` 的 `jwt.secret` / `jwt.expire-hours`）。服务重启后已登录用户无需重新登录。

> 视频封面：上传 mp4/mov 时后端自动用 **ffmpeg** 抽取第一帧作为封面（宽度 640px），ffmpeg 路径见 `application.yml` 的 `ffmpeg.path`（未配置时取系统 PATH）。抽帧失败不阻断上传，封面为空时前端显示占位图。

| 方法 | 路径 | 说明 | 是否需登录 |
| --- | --- | --- | --- |
| POST | /api/user/register | 注册 | 否 |
| POST | /api/user/login | 登录，返回 {token, user} | 否 |
| GET | /api/user/info | 当前登录用户信息 | 是 |
| POST | /api/record/upload | 上传对局（multipart 表单） | 是 |
| GET | /api/record/list | 对局列表（分页/搜索/排序/**userId 过滤**） | 否 |
| GET | /api/record/{id} | 对局详情 | 否 |
| GET | /api/record/liked | 我点赞过的对局（个人中心） | 是 |
| POST | /api/record/{id}/share-poster | 生成分享海报（java.awt 绘制 PNG） | 是 |
| GET | /api/user/stats | 个人数据看板（上传数/获赞/胜率/常用游戏） | 是 |
| GET | /api/user/{id}/profile | 玩家公开主页（昵称/注册时间/作品数/获赞数） | 否 |
| DELETE | /api/record/{id} | 删除自己的对局 | 是（仅本人） |
| POST | /api/comment/add | 发表评论/回复（可带配图） | 是 |
| GET | /api/comment/list | 评论分页列表（含回复） | 否 |
| DELETE | /api/comment/{id} | 删除自己的评论 | 是（仅本人） |
| POST | /api/file/upload | 通用图片上传（评论配图等，≤5MB） | 是 |
| POST | /api/like/toggle | 点赞/取消点赞（触发站内通知） | 是 |
| GET | /api/like/status | 查询点赞状态 | 否 |
| GET | /api/notification/list | 我的通知分页 | 是 |
| GET | /api/notification/unread-count | 未读通知数 | 是 |
| POST | /api/notification/{id}/read | 标记单条已读 | 是 |
| POST | /api/notification/read-all | 全部标记已读 | 是 |
| GET | /api/notification/stream?token= | 实时通知流（SSE，query 携带 token） | 是（token） |
| POST | /api/report/add | 提交举报（原因+说明，防重复） | 是 |
| GET | /api/report/list | 举报分页列表（按状态过滤） | 是（仅管理员） |
| POST | /api/report/{id}/handle | 处理举报（驳回 / 下架对局） | 是（仅管理员） |
| POST | /api/report/restore-record | 恢复已下架对局 | 是（仅管理员） |
| GET | /api/sensitive-word/list | 敏感词列表 | 是（仅管理员） |
| POST | /api/sensitive-word/add | 添加敏感词（实时生效） | 是（仅管理员） |
| DELETE | /api/sensitive-word/{id} | 删除敏感词 | 是（仅管理员） |

### 上传对局示例

请求体为 `multipart/form-data`：

| 表单字段 | 说明 |
| --- | --- |
| file | 对局文件：图片(jpg/png/gif/webp) 或视频(mp4/mov)，≤500MB |
| gameName | 游戏名称（必填） |
| gameMode | 游戏模式，如"排位" |
| matchDate | 对局日期 yyyy-MM-dd |
| result | 结果 win/lose/draw |
| rank | 段位 |
| description | 对局描述 |

## 前端对接要点

- 登录后把 `token` 存本地，请求头加 `Authorization: Bearer <token>`。
- 上传用 `FormData`：`formData.append('file', file)`，其余字段直接 append。
- 视频封面：当前版本视频上传后 `coverUrl` 为空，前端可用 `<video>` 首帧展示或显示占位图。

## 常见问题

- **8080 端口占用**：后端启动报 "Port 8080 was already in use" 时，先停掉占用进程（`netstat -ano | findstr 8080` → `taskkill /PID <PID> /F`）再启动。
- **5173 端口占用**：前端 dev server 端口被占时同理处理，或改 `vite.config.js` 的 `server.port`。
- **JDK 版本**：项目要求 **JDK 17**（Spring Boot 3.3 最低要求），IDEA 中设置 File → Project Structure → Project SDK 为 JDK 17，且 `Settings → Build → Compiler → Java Compiler` 的 Target bytecode 为 17。
- **数据库连接失败**：确认 MySQL80 服务已启动、密码已改、已执行 init.sql。
- **Lombok 报错**：IDEA 需启用注解处理（Settings → Build → Compiler → Annotation Processors → Enable）。
