const PASSWORD_STRENGTH_COPY = {
  weakInline: '弱 · 建议混合字符',
  mediumInline: '中 · 可加符号',
  strongInline: '强 · 可用'
}

const LOGIN_PAGE_COPY = {
  register: {
    title: '注册',
    nicknameLabel: '称谓',
    nicknamePlaceholder: '被其他行者所记住的称呼',
    emailLabel: '邮箱',
    emailPlaceholder: '请输入邮箱',
    passwordLabel: '密码',
    passwordPlaceholder: '请输入密码（至少6位）',
    confirmPasswordLabel: '确认',
    confirmPasswordPlaceholder: '请再次输入密码',
    submitButton: '注册',
    switchToLogin: '已是行者？回到群落',
    resultTitle: '注册成功',
    resultDesc: '您的行者编号',
    resultNoLabel: '行者编号',
    resultConfirmButton: '进入群落'
  },
  login: {
    title: '登录',
    accountLabel: '账号',
    accountPlaceholder: '请输入狼藉号或邮箱',
    passwordLabel: '密码',
    passwordPlaceholder: '请输入密码',
    forgotPassword: '忘记密码？',
    submitButton: '登录',
    switchToRegister: '还未加入？去注册'
  },
  validation: {
    nicknameRequired: '请输入行者名',
    passwordMinLength: '密码至少6位',
    confirmPasswordRequired: '请确认密码',
    registerPasswordMismatch: '两次输入的密码不一致',
    invalidEmail: '请输入正确的邮箱',
    accountRequired: '请输入狼藉号或邮箱',
    loginPasswordRequired: '请输入密码'
  },
  toast: {
    registerSuccess: '注册成功',
    registerFail: '注册失败',
    loginSuccess: '登录成功',
    loginFail: '登录失败'
  }
}

const PASSWORD_PAGE_COPY = {
  headerTitle: '修改密码',
  headerSubtitle: '修改后需要重新登录',
  fields: {
    oldLabel: '原密码',
    oldPlaceholder: '请输入当前密码',
    newLabel: '新密码',
    newPlaceholder: '请输入新密码（至少6位）',
    confirmLabel: '确认',
    confirmPlaceholder: '请再次输入新密码'
  },
  submitButton: '确认修改',
  validation: {
    oldRequired: '请输入原密码',
    newRequired: '请输入新密码',
    newMinLength: '新密码至少6位',
    confirmRequired: '请确认新密码',
    newMismatch: '两次输入的新密码不一致'
  },
  toast: {
    success: '密码已修改，请重新登录',
    fail: '修改失败'
  }
}

const RESET_PASSWORD_PAGE_COPY = {
  headerTitle: '找回密码',
  headerSubtitle: '向已认证邮箱发送重置链接',
  fields: {
    emailLabel: '邮箱',
    emailPlaceholder: '请输入已认证邮箱'
  },
  actions: {
    sendLink: '发送重置链接',
    backToLogin: '返回登录'
  },
  validation: {
    emailRequired: '请输入邮箱',
    invalidEmail: '请输入正确的邮箱'
  },
  toast: {
    sendSuccess: '重置链接已发送',
    sendFail: '发送重置链接失败'
  }
}

const EMAIL_BIND_PAGE_COPY = {
  headerTitle: '邮箱管理',
  headerSubtitle: '绑定后可使用邮箱登录与重置密码',
  status: {
    label: '当前邮箱',
    unbound: '未绑定',
    pending: '待认证',
    verified: '已认证',
    unboundHint: '当前未绑定邮箱，建议尽快绑定以提升账号安全性。',
    pendingHint: '邮箱已设置，完成认证后才能用于邮箱登录。',
    verifiedHint: '邮箱已认证，可用于邮箱登录与密码找回，不支持换绑。'
  },
  fields: {
    emailLabel: '邮箱',
    emailPlaceholder: '请输入邮箱'
  },
  actions: {
    sendLink: '发送认证链接',
    resendLink: '重新发送认证链接',
    verifiedLocked: '邮箱已认证'
  },
  validation: {
    emailRequired: '请输入邮箱',
    invalidEmail: '请输入正确的邮箱'
  },
  toast: {
    sendSuccess: '认证链接已发送',
    sendFail: '发送失败',
    loadFail: '加载失败'
  }
}

module.exports = {
  PASSWORD_STRENGTH_COPY,
  LOGIN_PAGE_COPY,
  PASSWORD_PAGE_COPY,
  RESET_PASSWORD_PAGE_COPY,
  EMAIL_BIND_PAGE_COPY
}
