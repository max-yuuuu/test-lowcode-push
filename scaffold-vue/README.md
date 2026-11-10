# 通用后台管理前端

Vue 3 + TypeScript + Vite + Vue Router + GSAP 的后台开发底座，使用珍珠玻璃主题和自有组件库；已接入真实 Spring Boot 后端。

## 开发

```bash
npm install
npm run dev
npm run test:unit
npm run build
```

默认地址为 `http://localhost:5173`，`/api` 代理到 `http://localhost:8080`，可通过 `VITE_API_PROXY_TARGET` 指定其他本地后端。后端与数据库准备见 [根目录 README](../README.md)。

依赖装的是 npm 版本号：本项目不是 workspace，仓库里没有锁文件，所以用 `npm install` 而不是 `npm ci`（`@yuanda-lowcode-ai/scaffold-ui` 由自带的 `.npmrc` 钉在官方源）。

种子管理员 `admin` 的开发密码为 `admin123`，验证码必须使用当前页面显示的字符。非管理员菜单来自角色授权；无菜单账号显示明确的权限分配提示。

## 目录

| 路径 | 中文职责 |
| --- | --- |
| `src/api` | 统一 HTTP、会话存储、认证与系统管理接口；物料包要用的宿主能力在 `src/api/scaffold-providers.ts` 注入 |
| `src/shell` | 应用壳装配：静态与动态路由、`views/<component>.vue` 解析与别名表、外壳插槽、宿主申报入口 |
| `src/composables/auth.ts` | 当前用户、权限、动态菜单与会话版本 |
| `src/components/layout` | 外壳、侧栏、顶栏、命令搜索、外观与密码弹窗 |
| `src/views` | 登录、工作台、系统管理与监控页、无权限与未匹配页面 |
| `scripts/session-regression.py` | 真实浏览器会话、路由竞态与低权限回归 |

控件与通用状态不在本项目里：按钮、输入、下拉、表格、弹窗等基础控件，`ListPage`、`FormDialog`、`PermissionButton` 等业务组件，列表 / 字典 / 日期 / 浮层 / 上传等 composable，以及珍珠玻璃主题，都住在 npm 包 `@yuanda-lowcode-ai/scaffold-ui`，按子路径引用（`@yuanda-lowcode-ai/scaffold-ui/components/ui`、`.../components/business`、`.../composables/list`、`@yuanda-lowcode-ai/scaffold-ui/theme.css`）。升级物料就是改 `package.json` 里的版本号再 `npm install`，不搬组件源码。

## 扩展约定

新列表先复用 `ListPage` 与列表 composable，新表单先复用 `FormDialog`，权限动作使用 `PermissionButton`；接口复用 `api/http.ts`。页面按 `views/<sys_menu.component>.vue` 同名放置即可被 `src/shell/views.ts` 的 glob 解析，文件名与菜单里的 `component` 值不同名时才需要在 `src/shell/index.ts` 的 `BASE_COMPONENT_ALIAS` 登记别名；之后在菜单管理配置路径、组件标识与权限码。

退出和 401 均通过统一会话清理撤销动态路由与缓存；不得在页面直接修改认证存储或路由就绪标记。旧请求只能更新发起时仍有效的会话。

主题、密度和减少动效规则见 [DESIGN.md](../DESIGN.md)，接口与字段的对应关系见 [前后端接口约定](../docs/frontend-backend-integration.md)。


## 浏览器回归

已有 Python Playwright 与 Chromium、前后端均启动时，可运行：

```bash
python3 scripts/session-regression.py
```

建议连接独立本地校验库；脚本创建并删除自身的角色 / 用户夹具，不清空任何表。管理员密码可用 `SCAFFOLD_TEST_PASSWORD` 覆盖；脚本需要 Vite 开发服务器来访问应用实际加载的 HTTP 模块。

历史原型和工作流页面已移除。


本项目代码采用 [MIT License](../LICENSE)；GSAP 依赖遵循其 [Standard "No Charge" License](https://gsap.com/community/standard-license/)。
