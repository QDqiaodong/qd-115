# 家用影音设备运维台账

全栈 Web 应用，用于管理家庭影院音响、投影仪、播放器、功放等影音设备的基础档案、使用时长、故障检修、固件更新与清洁养护记录。

## 技术栈

| 层级 | 技术栈 |
|------|--------|
| 前端 | Vue 3 + Vite 5 + Element Plus 2 + Vue Router 4 + Axios |
| 后端 | Spring Boot 3.3 + JDK 17 + Spring Data JPA + Spring Data Redis |
| 数据库 | MySQL 8.0.36 + Redis 7.2 |
| 部署 | Docker Compose + Nginx |

## 端口分配（固定端口，不可自动切换）

| 服务 | 宿主机端口 | 容器端口 | 绑定地址 | 说明 |
|------|-----------|----------|----------|------|
| 前端 Nginx | 3008 | 80 | 127.0.0.1 | 静态资源 + API 代理 |
| 后端 SpringBoot | 8088 | 8088 | 127.0.0.1 | RESTful API 服务 |
| MySQL | 3309 | 3306 | 127.0.0.1 | 业务数据库 |
| Redis | 6380 | 6379 | 127.0.0.1 | 缓存服务 |

**所有端口统一在 `.env` 中管理，禁止写死。**

## 快速启动

### 前置要求

- Docker 20+
- Docker Compose v2+
- 本机 Docker 配置镜像加速器（可选，加速基础镜像拉取）

### 启动命令

```bash
# 一键启动（含端口冲突检测）
./start.sh

# 停止服务
./stop.sh

# 重启服务
./start.sh
```

`start.sh` 会自动：
1. 检测所有端口是否被占用，占用则报错退出并显示占用进程
2. 构建镜像（首次全量构建，后续复用缓存）
3. 启动所有容器
4. 等待服务健康检查通过
5. 输出访问地址

## 访问地址

| 服务 | 地址 |
|------|------|
| 前端首页 | http://localhost:3008 |
| 前端首页 (IPv4) | http://127.0.0.1:3008 |
| 后端 API | http://127.0.0.1:8088/api |
| MySQL | `mysql -h127.0.0.1 -P3309 -uavledger -pavledger2024 avledger` |
| Redis | `redis-cli -h127.0.0.1 -p6380` |

**注意**：`localhost` 和 `127.0.0.1` 必须指向同一服务，页面内容完全一致。

## 功能模块

### 1. 设备档案管理
- 设备基础信息建档：名称、型号、品牌、购入日期、位置、规格参数
- 设备分类：音响、投影仪、播放器、功放、其他
- 设备状态：正常使用、待检修、故障停用、已报废
- 批量编辑、搜索筛选、分页展示

### 2. 使用时长统计
- 每次使用记录：日期、时长、使用场景、备注
- 自动累计单台设备总使用时长
- 支持按设备、时间范围筛选

### 3. 故障检修登记
- 故障现象描述、报修日期、检修人员
- 维修方案、更换配件、维修费用
- 检修状态：待处理、维修中、已完成、无法修复

### 4. 日常养护记录
- 养护类型：固件更新、清洁除尘、校准调试、更换耗材
- 养护日期、操作人员、养护说明
- 下次养护日期提醒

## Docker 架构说明

### 分层缓存机制（核心优化）

**前端 Dockerfile**：
```dockerfile
# 1. 依赖层（缓存）
COPY package*.json ./
RUN npm ci

# 2. 源码层（变更才重建）
COPY . .
RUN npm run build
```

**后端 Dockerfile**：
```dockerfile
# 1. 依赖层（缓存）
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# 2. 源码层（变更才重建）
COPY src ./src
RUN mvn clean package -DskipTests -B
```

**效果**：仅修改 `src/` 目录时，前端 14 秒完成，后端 13 秒完成，无需重新下载依赖。

### 构建上下文控制

`.dockerignore` 已排除：
- 前端：`node_modules`、`dist`、`.idea`、`.vscode`、`*.log`
- 后端：`target`、`.mvn`、`.idea`、`*.iml`、`*.log`
- 通用：`.git`、`.DS_Store`、`npm-debug.log`

### 全局镜像源配置

所有基础镜像统一通过 `DOCKER_REGISTRY` 环境变量前缀引用：
```yaml
image: ${DOCKER_REGISTRY}mysql:8.0.36
image: ${DOCKER_REGISTRY}redis:7.2-alpine
image: ${DOCKER_REGISTRY}node:18-alpine
image: ${DOCKER_REGISTRY}maven:3.9.6-eclipse-temurin-17
image: ${DOCKER_REGISTRY}eclipse-temurin:17-jre
image: ${DOCKER_REGISTRY}nginx:alpine
```

**修改 `.env` 中 `DOCKER_REGISTRY=` 即可切换镜像仓库**，默认留空使用 Docker Hub。

### 依赖源镜像

- **前端 npm**：`https://registry.npmmirror.com`（淘宝镜像）
- **后端 Maven**：华为云镜像 `https://repo.huaweicloud.com/repository/maven/`

无需 VPN 即可正常拉取依赖。

### 容器命名规范

所有容器统一以 `av-ledger-` 为前缀，与项目名称一致：
- `av-ledger-mysql`
- `av-ledger-redis`
- `av-ledger-backend`
- `av-ledger-frontend`

## 交付前自检

必须执行以下检查，全部通过才算完成：

```bash
# 1. 端口绑定检查（必须是 127.0.0.1）
lsof -nP -iTCP:3008 -sTCP:LISTEN
lsof -nP -iTCP:8088 -sTCP:LISTEN
lsof -nP -iTCP:3309 -sTCP:LISTEN
lsof -nP -iTCP:6380 -sTCP:LISTEN

# 2. 页面内容一致性检查（必须完全一致）
curl -sS http://127.0.0.1:3008 | head
curl -sS http://localhost:3008 | head

# 3. API 连通性检查
curl -s http://127.0.0.1:8088/api/devices | python3 -m json.tool

# 4. 分层缓存验证（修改源码后重新构建，依赖层应为 CACHED）
docker compose build frontend 2>&1 | grep 'npm ci'
# 预期输出：#12 CACHED
```

## 约束规范

### 端口安全
- 所有服务只绑定 `127.0.0.1`，禁止 `0.0.0.0` / `localhost` / `::`
- Docker Compose 端口映射必须写成 `127.0.0.1:${PORT}:容器端口`
- Vite 配置 `server.host='127.0.0.1'` + `strictPort=true`
- 端口冲突时**必须明确报错**，禁止自动切换端口

### 编码规范
- 数据库：`utf8mb4` 字符集，`init.sql` 开头 `SET NAMES utf8mb4`
- JVM：`-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8`
- Spring：`spring.servlet.encoding.force=true`
- HTTP 响应：`Content-Type: application/json;charset=UTF-8`

### 缓存策略
- Redis `maxmemory-policy allkeys-lru`
- 设备/养护类型：TTL 60 分钟
- 设备列表：TTL 30 分钟
- 序列化：`GenericJackson2JsonRedisSerializer` + `JavaTimeModule`

## 目录结构

```
.
├── .env                      # 全局环境变量（端口+镜像源）
├── .env.example              # 环境变量示例
├── docker-compose.yml        # Docker Compose 编排
├── start.sh                  # 启动脚本（端口检测+构建+提示）
├── stop.sh                   # 停止脚本
├── README.md                 # 本文档
├── sql/
│   └── init.sql              # 数据库初始化（含测试数据）
├── frontend/
│   ├── Dockerfile            # 前端镜像（分层缓存）
│   ├── .dockerignore         # 构建上下文排除
│   ├── nginx.conf            # Nginx 配置（API 代理+GZIP+缓存）
│   ├── vite.config.js        # Vite 配置（127.0.0.1+strictPort）
│   ├── package.json          # npm 依赖
│   ├── package-lock.json     # 版本锁定
│   └── src/                  # Vue3 源码
└── backend/
    ├── Dockerfile            # 后端镜像（分层缓存+编码参数）
    ├── .dockerignore         # 构建上下文排除
    ├── settings.xml          # Maven 镜像源配置
    ├── pom.xml               # Maven 依赖
    └── src/                  # SpringBoot 源码
        └── main/
            ├── java/com/avledger/
            │   ├── entity/       # JPA 实体
            │   ├── repository/   # 数据访问
            │   ├── service/      # 业务逻辑
            │   ├── controller/   # REST API
            │   ├── config/       # Redis/序列化配置
            │   └── dto/          # 数据传输对象
            └── resources/
                └── application.yml  # 应用配置
```

## 常见问题

### Q: 启动时提示端口被占用怎么办？
A: `start.sh` 会显示占用进程的 PID 和命令。停止对应进程或修改 `.env` 中的端口号，然后重新执行 `./start.sh`。

### Q: 构建慢怎么办？
A: 首次构建需要下载基础镜像和依赖，后续构建会复用缓存。如果仍慢：
1. 配置本机 Docker 镜像加速器
2. 在 `.env` 中设置 `DOCKER_REGISTRY=你的私有镜像仓库地址`

### Q: 中文乱码怎么办？
A: 本项目已全链路配置 UTF-8。如仍出现乱码：
1. 执行 `docker compose down -v` 删除数据卷重建
2. 检查终端编码是否为 UTF-8

### Q: 修改代码后如何更新？
A: 直接执行 `./start.sh`，Docker 会自动检测变更，只重新编译修改的部分。

## 数据库迁移

首次启动自动执行 `sql/init.sql` 初始化表结构和测试数据。

后续结构变更建议使用 Flyway 或 Liquibase 进行版本化迁移，避免数据卷 schema 不一致导致启动失败。
