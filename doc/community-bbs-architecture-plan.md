# 社区 BBS 化架构规划（基于当前代码现状）

## 1. 目标与边界

本规划用于把当前“帖子+评论流”升级为“版块+主题+楼层回复”的 BBS 结构，优先保证：

- 结构清晰：用户知道内容该发到哪个版块。
- 讨论可追踪：楼层、引用、最后回复信息明确。
- 运营可控：置顶、精华、锁帖、逻辑删除可执行。
- 实现干净：不保留兼容分支，不做迁移期双读双写。

非目标（本阶段不做）：

- 积分商城、复杂勋章体系、插件系统。
- 多级嵌套回复树（仅楼层+单层引用）。
- 复杂推荐算法。

## 2. 当前状态盘点（代码事实）

### 2.1 后端现状

当前社区只有一个 `community` 模块，核心能力是：

- 发帖：`POST /posts`
- 帖子列表：`GET /posts?page&size`
- 帖子详情：`GET /posts/{postId}`
- 发评论：`POST /posts/{postId}/comments`

当前数据模型：

- `wf_post`：`user_id + content + room_id + status + create_time/update_time`
- `wf_comment`：`post_id + user_id + content + status + create_time`

当前缺失：

- 无版块（Board）概念。
- 无主题标题（只有正文）。
- 无楼层编号、无引用关系。
- 无置顶、精华、锁帖、最后回复信息。
- 无浏览数、回复数冗余统计字段。

### 2.2 前端现状

当前页面是：

- `pages/community`：帖子列表（信息密度低）
- `pages/post-create`：仅正文输入
- `pages/post-detail`：帖子 + 扁平评论

当前缺失：

- 无版块入口与切换。
- 无主题列表运营标记（置顶/精华/锁帖）。
- 无楼层展示（#1/#2）、无引用回复。
- 无“最后回复”导向与分页阅读体验。

## 3. 架构原则（本项目约束）

遵循仓库 `AGENTS.md`：

- 不做旧接口兼容，不做过渡双模型。
- 不做兜底掩盖配置或建模问题。
- 一次性切换到新社区模型，旧实现清理掉。

工程原则：

- 写入模型和读取模型分离（同库不同 DTO 即可，不引入额外中间件）。
- 统计字段（回复数/最后回复）在服务层事务内维护。
- 社区状态统一用枚举状态字段（逻辑删除/锁定等）。

## 4. 目标领域模型（BBS MVP）

## 4.1 核心实体

1. `wf_forum_board`（版块）
- `id`
- `name`（版块名）
- `slug`（短标识，唯一）
- `description`
- `sort_no`
- `status`（NORMAL/CLOSED）
- `thread_count`
- `reply_count`
- `last_thread_id`
- `last_reply_time`
- `create_time`
- `update_time`

2. `wf_forum_thread`（主题）
- `id`
- `board_id`
- `author_id`
- `title`
- `content`（首帖正文）
- `thread_type`（NORMAL/STICKY/ANNOUNCEMENT）
- `status`（NORMAL/LOCKED/DELETED）
- `is_essence`（0/1）
- `view_count`
- `reply_count`
- `last_reply_id`
- `last_reply_user_id`
- `last_reply_time`
- `create_time`
- `update_time`

3. `wf_forum_reply`（楼层回复）
- `id`
- `thread_id`
- `floor_no`（从 2 开始，首帖视作 1 楼）
- `author_id`
- `content`
- `quote_reply_id`（可空，单层引用）
- `status`（NORMAL/DELETED）
- `create_time`
- `update_time`

4. `wf_forum_moderation_log`（版务日志，Phase B）
- `id`
- `operator_user_id`
- `target_type`（THREAD/REPLY）
- `target_id`
- `action`（LOCK/UNLOCK/STICKY/UNSTICKY/ESSENCE/UNESSENCE/DELETE）
- `reason`
- `create_time`

## 4.2 索引建议

- `wf_forum_board`：`uk_slug`，`idx_status_sort(status, sort_no)`
- `wf_forum_thread`：
  - `idx_board_type_status_reply_time(board_id, thread_type, status, last_reply_time desc)`
  - `idx_author_create(author_id, create_time desc)`
  - `idx_status_create(status, create_time desc)`
- `wf_forum_reply`：
  - `uk_thread_floor(thread_id, floor_no)`
  - `idx_thread_status_floor(thread_id, status, floor_no)`

## 5. API 设计（按功能分层）

命名改为 `/forum/*`，不再保留 `/posts/*`：

1. 版块
- `GET /forum/boards`

2. 主题
- `GET /forum/boards/{boardId}/threads?page&size&tab`
- `POST /forum/boards/{boardId}/threads`
- `GET /forum/threads/{threadId}`
- `PUT /forum/threads/{threadId}/view`（可选，计数上报）

3. 回复
- `GET /forum/threads/{threadId}/replies?page&size`
- `POST /forum/threads/{threadId}/replies`

4. 版务（Phase B）
- `PUT /forum/threads/{threadId}/lock`
- `PUT /forum/threads/{threadId}/sticky`
- `PUT /forum/threads/{threadId}/essence`
- `DELETE /forum/threads/{threadId}`（逻辑删除）
- `DELETE /forum/replies/{replyId}`（逻辑删除）

## 6. 前端信息架构（小程序）

## 6.1 页面结构

1. `community`：版块入口 + 当前版块主题列表  
2. `thread-create`（替换 post-create）：标题 + 正文 + 版块  
3. `thread-detail`（替换 post-detail）：首帖 + 楼层列表 + 快速回复 + 引用回复  

## 6.2 经典 BBS 交互（保留）

- 主题列表显示：`[置顶] [精华] [锁]` 标签。
- 每条主题展示：标题、作者、回复数、最后回复时间。
- 楼层显示：`#1 #2 #3...`
- 单层引用：回复时附一段“引用自 #n”摘要。
- 长帖分页：每页固定条数，避免性能抖动。

## 7. 实施阶段（建议）

## Phase A：BBS MVP（先做）

目标：完成可用的“版块-主题-楼层”闭环。

任务：

1. 数据库重构  
- 在 `database/init.sql` 新增 `wf_forum_*` 三张主表（可含版务日志预留表）。  
- 移除旧社区表在业务层的依赖（`wf_post/wf_comment` 不再被社区模块使用）。  

2. 后端模块重构  
- 新建 `community.board` / `community.thread` / `community.reply` 子域包结构。  
- 新接口统一到 `/forum/*`。  
- 服务层事务内维护：回复数、最后回复信息、版块统计。  

3. 前端页面重构  
- 社区列表页切换为主题列表视图。  
- 发帖页升级为主题创建（标题必填）。  
- 详情页升级楼层阅读与引用回复。  

验收标准：

- 用户可在指定版块发主题、看主题、回帖、按楼层阅读。
- 主题列表能准确显示置顶/精华/锁帖状态与回复信息。
- 旧 `/posts/*` 接口不再被前端调用。

## Phase B：治理与运营

任务：

- 版主操作（锁帖/精华/置顶/逻辑删除）。
- 举报与审核队列。
- 操作日志（可追责）。

## Phase C：留存增强

任务：

- 收藏主题、我的主题、我的回复。
- 未读跟踪（我参与过的主题有新回复）。
- 轻量导读（按最后回复时间、按精华、按版块热帖）。

## 8. 关键决策

1. 是否保留旧 `/posts`  
- 决策：不保留，直接切换到 `/forum`。  
- 原因：项目规则明确禁止兼容分支，且当前数据价值低。  

2. 是否做多级嵌套回复  
- 决策：不做，仅楼层 + 单层引用。  
- 原因：BBS 可读性与实现复杂度平衡最佳。  

3. 是否先做版主体系  
- 决策：Phase A 只留字段与接口骨架，完整治理放 Phase B。  
- 原因：先把用户主流程跑通，降低首轮风险。

## 9. 下一步执行入口

按顺序直接开工：

1. 先改 `database/init.sql`，落 `wf_forum_board/thread/reply`。  
2. 重建后端社区 API 到 `/forum/*`。  
3. 重构前端三个社区页面的路由与数据绑定。  
4. 清理旧社区调用链（不留并行旧入口）。

