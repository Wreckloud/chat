# WolfChat 管理后台 🐺🌲

> 深绿森林主题，狼的领地

## 📦 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - Vue 3 UI组件库
- **Vue Router** - 官方路由管理器
- **Pinia** - 新一代状态管理
- **Axios** - HTTP请求库
- **Sass** - CSS预处理器

## 🎨 主题色

- **主色调**: `#0A3E1E` - 深森林绿
- **辅助色**: 
  - 成功: `#4CAF50`
  - 警告: `#FF9800`
  - 危险: `#F44336`
  - 信息: `#2196F3`

## 🚀 快速开始

### 1. 安装依赖

```bash
cd chat/front/admin
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问: http://localhost:5173

### 3. 构建生产版本

```bash
npm run build
```

## 📁 项目结构

```
chat/front/admin/
├── public/              # 静态资源
├── src/
│   ├── api/            # API接口
│   │   ├── account.js  # 账户相关
│   │   ├── user.js     # 用户管理
│   │   └── group.js    # 群组管理
│   ├── assets/         # 资源文件
│   ├── components/     # 公共组件
│   ├── layout/         # 布局组件
│   │   └── index.vue   # 主布局
│   ├── router/         # 路由配置
│   │   └── index.js
│   ├── store/          # 状态管理
│   │   └── auth.js     # 认证状态
│   ├── styles/         # 样式文件
│   │   ├── variables.scss  # 变量
│   │   └── index.scss      # 全局样式
│   ├── utils/          # 工具类
│   │   ├── logger.js   # 日志工具
│   │   └── request.js  # 请求封装
│   ├── views/          # 页面组件
│   │   ├── login/      # 登录页
│   │   ├── dashboard/  # 控制台
│   │   ├── users/      # 用户管理
│   │   └── groups/     # 群组管理
│   ├── App.vue         # 根组件
│   └── main.js         # 入口文件
├── index.html
├── package.json
├── vite.config.js
└── README.md
```

## 🔐 登录说明

### 管理员登录

1. 打开登录页面
2. 输入手机号（需要是已注册的管理员账号）
3. 点击"获取验证码"
4. 输入短信验证码
5. 点击"登录"

**测试账号**（开发环境）:
- 手机号: 13800138000
- 验证码: 666666（后端mock）

## ✨ 功能模块

### 1. 控制台 Dashboard
- 系统数据统计
  - 用户总数
  - 群组总数
  - 消息总数
  - 活跃用户数
- 快捷操作入口
- 系统信息展示

### 2. 用户管理 Users
- 用户列表查询
- 搜索功能（用户名、WF号）
- 用户状态管理
  - 禁用用户
  - 启用用户
- 分页浏览

### 3. 群组管理 Groups
- 群组列表查询
- 搜索功能（群名称、状态）
- 群组详情查看
  - 基本信息
  - 成员列表
  - 群公告列表
- 群组操作
  - 解散群组
  - 移除成员
  - 删除公告

## 📊 日志系统

项目内置了完善的日志系统，参考后端风格：

### 日志级别

- **DEBUG** - 调试信息（灰色）
- **INFO** - 一般信息（深绿色）
- **WARN** - 警告信息（橙色）
- **ERROR** - 错误信息（红色）
- **HTTP** - 网络请求（蓝色/橙色）
- **ROUTE** - 路由跳转（绿色）
- **ACTN** - 用户操作（浅绿色）

### 使用方法

```javascript
import logger from '@/utils/logger'

// INFO 日志
logger.info('ComponentName', '操作成功', { data: 'value' })

// ERROR 日志
logger.error('ComponentName', '操作失败', error)

// 用户操作日志
logger.action('ComponentName', 'buttonClick', { id: 1 })

// HTTP 请求日志（自动记录）
// [HTTP ] 2024-12-18 15:30:00.123 [Request] - GET /api/users
// [HTTP ] 2024-12-18 15:30:00.456 [Response] - GET /api/users - Success
```

## 🔧 配置说明

### API代理配置

在 `vite.config.js` 中配置后端API代理：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端地址
      changeOrigin: true
    }
  }
}
```

### 主题定制

在 `src/styles/variables.scss` 中修改主题变量：

```scss
// 主题色
$primary-color: #0A3E1E;       // 深森林绿
$primary-light: #0F5A2B;       // 浅一点
$primary-lighter: #14733A;     // 更浅
$primary-dark: #062815;        // 深一点
```

## 🐛 开发调试

### 查看日志

打开浏览器控制台，可以看到详细的日志输出：

```
[INFO ] 2024-12-18 15:30:00.123 [Application] - WolfChat 管理端启动成功 🐺
[HTTP ] 2024-12-18 15:30:05.456 [Request] - POST /api/account/login/mobile
[HTTP ] 2024-12-18 15:30:05.789 [Response] - POST /api/account/login/mobile - Success
[ROUTE] 2024-12-18 15:30:06.000 [Navigation] - /login → /dashboard
```

### API实现状态

**✅ 已实现并可用**:
- 登录系统（手机号+验证码）
- 群组管理（查看、创建、解散）
- 群成员管理（邀请、移除、设置管理员）
- 群公告管理（发布、删除）

**❌ 未实现（会报404）**:
- 用户管理（需要后端实现 `/api/admin/users`）
- 统计数据（需要后端实现 `/api/admin/statistics`）

详细信息请查看 `API实现状态.md`

## 📝 代码规范

### 文件命名
- 组件文件：大驼峰 `MyComponent.vue`
- 工具文件：小驼峰 `myUtil.js`
- 样式文件：小写+连字符 `my-style.scss`

### 变量命名
- 常量：大写下划线 `MAX_COUNT`
- 变量/函数：小驼峰 `userName`, `getUserInfo()`
- 组件：大驼峰 `MyComponent`

### 注释规范
```javascript
/**
 * 函数说明
 * @param {String} name 参数说明
 * @returns {Object} 返回值说明
 */
```

## 🎯 后续开发计划

- [ ] 数据统计图表（ECharts）
- [ ] 消息管理模块
- [ ] 系统日志查看
- [ ] 用户详情页
- [ ] 批量操作功能
- [ ] 导出功能（Excel）
- [ ] 权限管理（RBAC）
- [ ] 暗黑模式

## 🤝 联系方式

- 作者: Wreckloud
- 项目: WolfChat 即时通讯系统
- 主题: 深绿森林 🐺🌲

---

**WolfChat © 2024 - 狼的领地，深绿森林**

