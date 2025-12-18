# WolfChat 图片资源说明 🐺

## 📦 需要准备的图片

### 1. wolf-logo.png (160x160px)
**用途**：登录页面的Logo
**建议**：
- 紫色或棕色的狼头像
- 简洁的矢量风格
- 透明背景

**在线资源**：
- 搜索关键词："wolf logo png"
- 推荐网站：iconfont.cn, flaticon.com

**临时替代**：
使用文字 "🐺" 或 "WolfChat"

---

### 2. default-avatar.png (120x120px)
**用途**：默认用户头像
**建议**：
- 单只狼的剪影
- 圆形或方形
- 灰色背景

**临时替代方案**：
```javascript
// 使用微信默认头像或文字
<image src="data:image/svg+xml..." />
```

---

### 3. default-group.png (120x120px)
**用途**：默认群组头像
**建议**：
- 多只狼的剪影（狼群）
- 体现团队感
- 与主题色搭配

**临时替代**：
使用文字 "群" 或 "🐺🐺🐺"

---

### 4. tab-home.png (64x64px)
**用途**：底部导航栏图标（未选中）
**建议**：
- 简单的首页图标
- 灰色（#999999）
- 线性风格

---

### 5. tab-home-active.png (64x64px)
**用途**：底部导航栏图标（选中）
**建议**：
- 与未选中图标相同
- 紫色（#667eea）
- 填充风格

---

## 🎨 快速创建图片

### 方法1：使用在线工具

**Icon 生成**：
1. 访问 https://www.iconfont.cn/
2. 搜索 "wolf" 或 "狼"
3. 下载PNG格式
4. 调整尺寸

**AI 生成**：
1. 访问 https://www.midjourney.com/ 或其他AI工具
2. 提示词："minimalist wolf logo, purple gradient, transparent background"

### 方法2：使用 Photoshop/Figma

1. 创建指定尺寸的画布
2. 使用矢量工具绘制
3. 导出为PNG格式

### 方法3：使用文字占位（临时）

在代码中使用emoji或文字作为临时替代：

```wxml
<!-- 临时方案：使用emoji -->
<view class="wolf-logo">🐺</view>

<!-- 临时方案：使用文字 -->
<view class="wolf-logo">WolfChat</view>
```

---

## 📁 文件放置位置

将图片放在以下目录：

```
chat/front/miniprogram/images/
├── wolf-logo.png          (160x160px)
├── default-avatar.png     (120x120px)
├── default-group.png      (120x120px)
├── tab-home.png           (64x64px)
└── tab-home-active.png    (64x64px)
```

---

## 🔧 图片处理建议

### 压缩
使用 TinyPNG (https://tinypng.com/) 压缩图片，减小包体积。

### 格式
- PNG：支持透明背景
- WEBP：更小的体积（微信小程序支持）

### 尺寸
使用2倍图（@2x），提高清晰度：
- wolf-logo.png → 320x320px
- default-avatar.png → 240x240px

---

## ⚠️ 当前状态

目前图片资源**缺失**，会导致以下错误：
```
no such file or direactory: D:\Portfolio\毕业设计\chat\front\miniprogram\images\wolf-logo.png
```

**临时解决方案**：
代码已添加容错处理，使用文字占位符。

**永久解决方案**：
准备上述5张图片，放入 `images/` 目录。

---

**WolfChat © 2024 🐺**

