# 订单中台

本项目通过低代码平台发布，项目编码 `order-service`，平台生成分支为 `platform-generated`。README 根据项目标记与仓库中已发布的业务产物生成。

## 开发入口

- [设计规范](DESIGN.md)：主题、组件复用与交互规范。
- [开发约定](AGENTS.md)：交付项目本地开发必须遵守的约定。
- [项目目录](#目录)：后端模块与前端工程职责。
- [本地启动](#本地启动)：本地前后端启动与数据库初始化。
- [容器部署](#容器部署)：单主机 Docker Compose 部署。
- [环境变量示例](.env.example)：数据库、Redis、JWT 与前端代理配置。

## 已发布业务

| 业务模块 | 后端位置 | 前端页面 |
| --- | --- | --- |
| `demo` | `scaffold-system/scaffold-business/src/main/java/com/scaffold/business/demo/` | `Demo.vue` |
| `e2e_order_trace` | `scaffold-system/scaffold-business/src/main/java/com/scaffold/business/e2e_order_trace/` | — |
| `order_detail` | `scaffold-system/scaffold-business/src/main/java/com/scaffold/business/order_detail/` | — |
| `order_list` | `scaffold-system/scaffold-business/src/main/java/com/scaffold/business/order_list/` | — |
| `sales_order` | `scaffold-system/scaffold-business/src/main/java/com/scaffold/business/sales_order/` | `SalesOrder.vue` |
| `t11_demo` | `scaffold-system/scaffold-business/src/main/java/com/scaffold/business/t11_demo/` | `T11Demo.vue` |
| `t11_verify_20261118` | `scaffold-system/scaffold-business/src/main/java/com/scaffold/business/t11_verify_20261118/` | — |

### 数据脚本

- `scaffold-system/sql/pages/demo_table.sql`
- `scaffold-system/sql/pages/sales_order_table.sql`
- `scaffold-system/sql/pages/t11_demo_table.sql`

顶层 `V1__init_schema.sql`、`V2__seed_data.sql`、`V3__system_resource_menu.sql` 用于首次初始化；`sql/pages/` 下的业务脚本需审核后手动执行。

### Flow 快照

- `scaffold-system/scaffold-business/src/main/resources/flow/4/archive_order.v2.json`
- `scaffold-system/scaffold-business/src/main/resources/flow/4/export_orders.v1.json`
- `scaffold-system/scaffold-business/src/main/resources/flow/4/export_orders.v3.json`
- `scaffold-system/scaffold-business/src/main/resources/flow/archive_order.v2.json`
- `scaffold-system/scaffold-business/src/main/resources/flow/export_orders.v3.json`

## 目录

| 目录 | 职责 |
| --- | --- |
| `scaffold-system/scaffold-common` | 统一返回、分页、异常与导出工具 |
| `scaffold-system/scaffold-framework` | 认证、权限、数据权限、审计与租户管道 |
| `scaffold-system/scaffold-flow-runtime` | Flow 协议校验与受限执行内核 |
| `scaffold-system/scaffold-system` | 用户、角色、菜单、字典、参数、日志等系统能力 |
| `scaffold-system/scaffold-business` | 已发布业务模块与人工扩展点 |
| `scaffold-system/scaffold-admin-core` | 健康检查、公共配置与本地建库工具 |
| `scaffold-system/scaffold-admin` | Spring Boot 启动模块 |
| `scaffold-vue` | Vue 3 管理后台与已发布业务页面 |

## 本地启动

准备 JDK 17+、Maven、Node.js 22+、MySQL 5.7+ 与 Redis。后端从进程环境读取配置，不会自动加载 `.env`。

```bash
export JWT_SECRET="$(openssl rand -hex 32)"
export DB_HOST=localhost DB_PORT=3306 DB_NAME=scaffold_system
export DB_USERNAME=root DB_PASSWORD=root
export REDIS_HOST=localhost REDIS_PORT=6379
```

首次使用空库时，按文件名顺序导入 `scaffold-system/sql/` 顶层的三份 `V*__*.sql`，不要递归导入 `sql/pages/`。也可运行 `com.scaffold.admin.util.DbInit`；其 `fix` 参数会删除目标库，只有确认库名并备份后才能使用。

```bash
cd scaffold-system
mvn -pl scaffold-admin -am package -DskipTests
java -jar scaffold-admin/target/scaffold-admin-0.1.0-SNAPSHOT.jar
```

```bash
cd scaffold-vue
npm install
npm run dev
```

前端默认地址为 `http://localhost:5173`，`/api` 代理到 `http://localhost:8080`。种子管理员为 `admin`，开发密码为 `admin123`；接入实际环境前必须修改密码、JWT 密钥与数据库/Redis 凭据。

## 容器部署

```bash
cp .env.example .env
# 修改 .env 中的 JWT_SECRET、数据库账号与端口
docker compose --profile mysql up -d --build
```

复用外部 MySQL 时不要启用 `mysql` profile，并把 `DB_HOST` 改为容器可达地址。完整变量说明见 `.env.example`。

## 开发约定

- 本地开发规范见 `AGENTS.md`；前端继续使用 `@yuanda-lowcode-ai/scaffold-ui`，不把组件包源码复制进本仓库；视觉规范见 `DESIGN.md`。
- 平台生成的 B 档文件会在发布时覆盖；人工业务逻辑写入 `*Extension.java`，页面扩展放入 `src/views/<module>/overrides/`。
- 平台只写 `.scaffold-project.json` 中的 `generatedBranch`；主干分支由使用方控制。
- 构建前先执行 `npm run build`，后端执行 `mvn -pl scaffold-admin -am package -DskipTests`。

## 许可证

本项目代码采用 [MIT License](LICENSE)。第三方依赖遵循各自许可证。
