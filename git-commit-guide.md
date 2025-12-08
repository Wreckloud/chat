# Git 提交指南（IDEA无法识别Git时的临时方案）

## 问题
IDEA只打开了backend子目录，无法识别父目录的Git仓库。

## 解决方案

### 方案1：在IDEA中配置Git仓库路径（推荐）
1. `File` → `Settings`（或 `Ctrl+Alt+S`）
2. `Version Control` → `Directory Mappings`
3. 添加映射：
   - Directory: `D:\Portfolio\project\chat`
   - VCS: `Git`
4. 应用并确定

### 方案2：重新打开整个项目
1. `File` → `Open`
2. 选择 `D:\Portfolio\project\chat`（不是backend）
3. 选择 `Open as Project`

### 方案3：使用命令行（临时方案）

#### 查看状态
```bash
cd D:\Portfolio\project\chat
git status
```

#### 暂存所有更改
```bash
git add .
```

#### 提交
```bash
git commit -m "完成了手机号登录和微信登录"
```

#### 推送到远程
```bash
git push origin dev
```

## 当前分支
- 本地分支：`dev`
- 远程仓库：`git@github.com:Wreckloud/chat.git`

