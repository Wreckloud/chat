/**
 * 个人资料编辑页面
 */
const request = require('../../utils/request')
const auth = require('../../utils/auth')
const { toastError, toastSuccess } = require('../../utils/ui')
const { applyPageTheme } = require('../../utils/page-theme')
const { normalizeUser } = require('../../utils/user')
const pageLifecycleHelper = require('../../utils/page-lifecycle-helper')

const NICKNAME_MAX_LEN = 12

function normalizeText(value) {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value).trim()
}

function normalizeNullableText(value) {
  const normalized = normalizeText(value)
  return normalized || ''
}

function buildFormState(userInfo) {
  return {
    nickname: normalizeNullableText(userInfo && userInfo.nickname),
    avatar: normalizeNullableText(userInfo && userInfo.avatar),
    signature: normalizeNullableText(userInfo && userInfo.signature),
    bio: normalizeNullableText(userInfo && userInfo.bio)
  }
}

function isSameForm(left, right) {
  return normalizeNullableText(left.nickname) === normalizeNullableText(right.nickname) &&
    normalizeNullableText(left.signature) === normalizeNullableText(right.signature) &&
    normalizeNullableText(left.bio) === normalizeNullableText(right.bio)
}

Page({
  data: {
    loading: false,
    submitting: false,
    themeClass: 'theme-retro-blue',
    nickname: '',
    avatarPreview: '',
    signature: '',
    bio: '',
    canSubmit: false
  },

  onLoad() {
    pageLifecycleHelper.handleProtectedPageLoad(auth)
  },

  onShow() {
    pageLifecycleHelper.handleProtectedPageShow(auth, {
      afterShow: () => {
        this.applyTheme()
        this.loadUserInfo()
      }
    })
  },

  async loadUserInfo() {
    this.setData({ loading: true })
    try {
      const res = await request.get('/users/me')
      const userInfo = normalizeUser(res.data || {}) || {}
      auth.setUserInfo(userInfo)
      this.applyFormState(userInfo)
    } catch (error) {
      toastError(error, '加载资料失败')
    } finally {
      this.setData({ loading: false })
    }
  },

  applyFormState(userInfo) {
    const formState = buildFormState(userInfo)
    this.originalFormState = formState
    this.setData({
      nickname: formState.nickname,
      avatarPreview: formState.avatar,
      signature: formState.signature,
      bio: formState.bio,
      canSubmit: false
    })
  },

  onNicknameInput(e) {
    this.updateField('nickname', e.detail.value || '')
  },

  onSignatureInput(e) {
    this.updateField('signature', e.detail.value || '')
  },

  onBioInput(e) {
    this.updateField('bio', e.detail.value || '')
  },

  updateField(field, value) {
    this.setData({
      [field]: value
    }, () => {
      this.syncCanSubmit()
    })
  },

  syncCanSubmit() {
    const currentForm = {
      nickname: this.data.nickname,
      signature: this.data.signature,
      bio: this.data.bio
    }
    const normalizedNickname = normalizeText(currentForm.nickname)
    const hasNickname = !!normalizedNickname
    const nicknameWithinLimit = normalizedNickname.length <= NICKNAME_MAX_LEN
    const hasChanged = !isSameForm(this.originalFormState || {}, currentForm)
    this.setData({
      canSubmit: hasNickname && nicknameWithinLimit && hasChanged && !this.data.submitting
    })
  },

  async handleSubmit() {
    if (this.data.submitting || !this.data.canSubmit) {
      return
    }

    const payload = {
      nickname: normalizeText(this.data.nickname),
      signature: normalizeNullableText(this.data.signature),
      bio: normalizeNullableText(this.data.bio)
    }

    if (!payload.nickname) {
      toastError('称谓不能为空')
      return
    }

    if (payload.nickname.length > NICKNAME_MAX_LEN) {
      toastError(`称谓最多 ${NICKNAME_MAX_LEN} 个字符`)
      return
    }

    this.setData({
      submitting: true,
      canSubmit: false
    })
    try {
      const res = await request.put('/users/profile', payload)
      const userInfo = normalizeUser(res.data || {}) || {}
      auth.setUserInfo(userInfo)
      this.applyFormState(userInfo)
      toastSuccess('资料已保存')
    } catch (error) {
      toastError(error, '保存失败')
      this.setData({ submitting: false }, () => {
        this.syncCanSubmit()
      })
      return
    }

    this.setData({ submitting: false })
  },

  applyTheme() {
    applyPageTheme(this)
  }
})
