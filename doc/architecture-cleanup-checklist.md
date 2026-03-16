# 架构优化清单（当前迭代）

## 已明确并完成

1. 仅保留单聊 + 公共 Lobby，不保留群聊入口。
2. 会话页「更多」菜单移除未实现项（删除好友、拉聊天室），仅保留「查看个人资料」。
3. 社区分页已补全：主题列表与回复列表支持下拉分页加载。
4. 聊天消息分段规则已落地：同发送者超过 5 分钟自动断段。
5. 私聊与 Lobby 的消息规则统一：`MessageRuleSupport` 统一类型归一化、内容归一化、文本媒体字段校验、分页参数校验。
6. 聊天前端共享工具已抽取：`im-helper.js` 统一消息簇构建、链接识别、文件名截断、媒体尺寸与键盘高度解析。
7. 在线状态查询结构已优化：`UserPresenceService` 使用 Redis `ZSET` 管理在线与活跃时间，不再使用 `keys(prefix*)` 扫描。
8. 聊天发送链路已继续收敛：`im-helper.js` 承载 ACK/超时、clientMsgId、滚动锚点与计时器清理逻辑。
9. 媒体面板通用交互已收敛：`im-helper.js` 承载 `chooseMedia`、`chooseMessageFile`、确认弹窗、可编辑弹窗、文件下载与文档打开。
10. 输入区交互已收敛：`im-helper.js` 承载键盘高度处理、聚焦/失焦、输入框回焦、底部高度测量与重置逻辑。
11. 消息规则校验入口已统一：私聊与 Lobby 均复用 `ChatMediaService.validateMessagePayload`，不再分别维护类型分支。
12. 聊天连接态与发送态提示已统一：`ws.js` 提供状态订阅，`im-page-helper.js` 统一连接提示与发送失败点击重试。
13. 媒体发送状态提示已统一：图片/视频/文件/链接发送复用 `im-page-helper.js` 的发送状态展示，不再仅文本消息可见。
14. 聊天页面生命周期清理已收敛：`im-page-helper.js` 统一 `onReady/onShow/onUnload` 公共流程，页面仅保留场景差异逻辑。
15. 聊天 WS 公共处理已收敛：`im-ws-helper.js` 统一 `ERROR/ACK` 分支分发，页面只处理业务消息类型差异。
16. 当前用户资料加载已收敛：`im-page-helper.js` 统一 `loadCurrentUserProfile + messageBlocks` 刷新流程。
17. 历史消息加载骨架已收敛：`im-page-helper.js` 统一 `loadMessages` 默认行为（分页加载、构建消息块、首屏滚底、资料预热）。
18. 发送者资料预热已收敛：`im-user-helper.js` 统一 `ensureSenderProfiles`（消息内资料预热 + 按需补全），并清理页面未使用的 socket 拆除方法。
19. 「更多」面板动作已收敛：`im-page-helper.js` 统一默认媒体动作分发与发送依赖构建，页面仅保留上传能力配置。
20. 文本发送流程已收敛：`im-page-helper.js` 统一 `sendComposerTextMessage`，页面不再重复维护同一发送实现。
21. 页面初始化与上拉加载流程已收敛：`im-page-helper.js` 统一 `onLoad` 启动编排与 `onMessageListUpper` 加载条件，移除页面重复判定。
22. 会话列表页清理流程已收敛：`chat.js` 统一 `onHide/onUnload` 退出清理入口，并收敛 `loadConversations` 的 loading 复位逻辑。
23. 会话列表页生命周期已对齐：`chat.js` 的 `onLoad/onShow` 统一接入 `im-page-helper.js` 公共编排，登录校验与展示刷新行为一致。
24. 高频预期日志已降噪：认证与参数类业务失败下调为 `debug`，WS 建连/断连与逐条送达日志下调为 `debug`，保留关键异常 `error`。
25. 社区展示映射已收敛：新增 `forum-view-helper.js` 统一主题/回复映射、分页合并与 `hasMore` 计算，减少 `community.js` 与 `post-detail.js` 重复代码。
26. 媒体校验规则已收敛：`ChatMediaService` 抽取图片/视频上传策略与消息校验公共模板，减少重复分支并统一错误边界。
27. 版块统计查询已优化：`ForumService.refreshBoardStats` 改为数据库聚合统计与最新主题直查，避免全量加载主题再内存累加。
28. IM Helper 导出面已收口：`im-page-helper.js` 移除仅内部使用的对外导出，降低误用与耦合风险。
29. 受保护页面生命周期已统一：新增 `page-lifecycle-helper.js`，社区与个人相关页面统一登录校验与 `onLoad/onShow` 编排，减少重复模板代码。
30. IM 生命周期已复用统一入口：`im-page-helper.js` 的 `handlePageLoad/handlePageShow` 改为复用 `page-lifecycle-helper.js`，去除重复登录校验模板。
31. 聊天详情用户资料回填已收敛：`chat-detail.js` 抽取 `applyLoadedUserProfile` 统一处理 targetUser/当前用户/消息块刷新，去除重复回填逻辑。
32. 聊天页生命周期参数已统一：`chat.js`/`chat-detail.js`/`lobby.js` 全部改用 `beforeInit/afterInit/beforeShow/afterShow`，并删除 `im-page-helper.js` 旧参数分支。
33. 在线状态批量查询已优化：`UserPresenceService` 对会话在线态与最近活跃态改为 Redis pipeline 批量 `score` 查询，减少循环请求往返。
34. 社区分页视图组装已收敛：`ForumQueryService` 抽取主题/回复分页 VO 组装方法，减少重复模板代码与后续漏改风险。
35. 聊天页样式已去重：抽取 `styles/im-chat-common.wxss` 统一消息区/输入区/媒体样式，`chat-detail` 与 `lobby` 仅保留头部差异样式。
36. 聊天页结构模板已去重：新增 `templates/im-chat-body.wxml` 统一消息流与输入区结构，`chat-detail` 与 `lobby` 仅保留场景头部与文案差异。
37. 聊天页事件绑定已收敛：新增 `utils/im-page-methods.js` 统一输入区、媒体动作与消息列表交互方法，`chat-detail` 与 `lobby` 仅保留业务差异逻辑。
38. 社区视图装配已拆分：新增 `ForumViewAssembler` 承接 `ForumQueryService` 内的 VO 映射逻辑，查询服务仅保留查询编排职责。
39. 发送交互已精简：聊天移除发送状态提示与重试提示链路，改为输入为空禁用发送；帖子详情改为底部固定回帖栏，回帖按钮按内容/锁帖/提交态统一禁用并移除回帖成功失败提示。

## 仍需收口（按建议顺序）

1. 当前轮次无阻塞收口项，可进入下一阶段功能迭代。
2. 若进入新一轮治理，优先补最小集成测试（聊天发送/已读/在线状态）再继续扩展功能。

## 执行原则

1. 禁止引入兼容分支、兜底逻辑和临时开关。
2. 每次改动只解决一个明确目标，并保证可编译、可运行。
3. 公共行为改动优先通过提取共享模块完成，避免复制粘贴扩散。
