# WolfChat 管理端（Admin）

## 当前定位

WolfChat Admin 是论文与运维演示用的轻量管理后台，当前聚焦：

- 管理员登录
- 控制台概览
- 用户管理
- 内容治理（社区）
- 审计日志

不包含群聊管理等旧设计模块。

## 技术栈

- Vue 3
- Vite 5
- Element Plus
- Vue Router 4
- Pinia
- Axios

## 目录结构（当前实际）

```text
front/admin/
├── src/
│   ├── api/              # auth/dashboard/user/content/audit
│   ├── layout/           # 管理端壳布局
│   ├── router/           # 路由与登录守卫
│   ├── store/            # Pinia（认证状态）
│   ├── utils/            # 请求封装、token 存储
│   └── views/
│       ├── login/
│       ├── dashboard/
│       ├── users/
│       ├── content/
│       ├── audit/
│       └── not-found/
├── vite.config.js
└── package.json
```

## 运行方式

### 1) 本地开发（推荐）

```bash
cd chat/front/admin
npm install
npm run dev
```

- 默认地址：`http://localhost:5173`
- 开发代理：`/api -> http://localhost:8080`（见 `vite.config.js`）

### 2) 嵌入后端静态资源

```bash
cd chat/front/admin
npm run build:embed
```

构建产物输出到：

- `chat/backend/src/main/resources/static/admin-console/`

后端启动后访问：

- `http://localhost:8080/api/admin-console/`

## 接口与鉴权

- Axios 基础路径：`/api`（`src/utils/request.js`）
- 请求头：存在 token 时自动注入 `Authorization: Bearer <token>`
- 响应处理：
  - `code === 0` 视为成功
  - `code === 2001/2002` 或 HTTP 401 视为登录失效，自动清理并跳转登录页

## 当前路由

- `/login`
- `/dashboard`
- `/users`
- `/content`
- `/audit`

## 对单人维护的建议

1. 日常使用 `npm run dev`，答辩前再执行一次 `build:embed` 联调验证。
2. 接口或权限变更后，先同步 `src/api/*` 和本 README，再改页面。
3. 管理员权限由后端 `allowed-user-ids` 控制，演示账号需提前准备。

