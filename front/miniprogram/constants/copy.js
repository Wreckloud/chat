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

module.exports = {
  PASSWORD_STRENGTH_COPY,
  LOGIN_PAGE_COPY,
  PASSWORD_PAGE_COPY
}
