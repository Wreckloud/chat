# 提示与日志归类规范

## 1. 目标
- 前端提示简洁，不暴露内部实现细节。
- 后端日志可定位，便于排障与审计。
- 对“可预期失败”和“系统异常”做分级处理。

## 2. 前端提示分级
- A 类：无效操作（输入为空、按钮无效、状态已可见）
  - 不弹提示，直接通过禁用按钮或页面状态反馈。
- B 类：可预期业务失败（如登录凭证错误）
  - 弹简短业务提示，不返回底层原因。
  - 登录统一展示“账号或密码错误”。
- C 类：非预期失败（网络异常、系统异常）
  - 弹通用提示：`网络异常，请稍后重试` 或 `系统繁忙，请稍后重试`。

## 3. 后端日志分级
- INFO：安全相关常规事件（登录成功、登录失败业务码）。
- WARN：业务异常（参数不合法、状态冲突、权限不足）。
- ERROR：系统异常（未预期错误，保留堆栈）。

## 4. 已落地约束
- 登录接口对外错误统一为 `LOGIN_FAILED`，消息统一：`账号或密码错误`。
- 请求封装保留业务 `code` 与错误 `kind`（business/http/network），供 UI 层统一归类。
- 发送消息与回帖失败不再静默吞错，统一走错误提示策略。

## 5. 修改入口
- 后端异常出口：
  - `backend/src/main/java/com/wreckloud/wolfchat/common/web/GlobalExceptionHandler.java`
- 登录日志与脱敏：
  - `backend/src/main/java/com/wreckloud/wolfchat/account/application/service/AuthService.java`
  - `backend/src/main/java/com/wreckloud/wolfchat/account/application/service/LoginRecordService.java`
  - `backend/src/main/java/com/wreckloud/wolfchat/account/application/support/AccountMaskingSupport.java`
- 前端提示与请求归类：
  - `front/miniprogram/utils/request.js`
  - `front/miniprogram/utils/ui.js`
  - `front/miniprogram/pages/login/login.js`

