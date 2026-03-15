# WolfChat 图片资源说明

## 1. 当前必需图片

### 1. default-avatar.png（120x120）
用途：默认用户头像（单聊、Lobby、社区通用）。

建议：
- 方形头像内容，避免圆形素材放入方形容器后观感不一致。
- 风格与当前复古主题一致，边缘留出 2~4px 安全区域。

## 2. 可选图片

### 1. wolf-logo.png（160x160）
用途：登录页或启动页品牌展示。

### 2. tab-home.png / tab-home-active.png（64x64）
用途：若后续需要图标化 tab，可使用该资源。
当前 tab 以文本为主，可暂不提供。

## 3. 目录结构

```text
chat/front/miniprogram/images/
├── default-avatar.png
└── README.md
```

## 4. 规范建议

- 优先 PNG/WebP，透明背景。
- 建议先出 2x 源图，再导出业务尺寸。
- 上传前做压缩，减少小程序包体积。
