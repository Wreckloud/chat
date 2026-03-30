<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { login, register } from '@/api/modules'

const NICKNAME_MAX_LEN = 12

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const mode = ref(String(route.query.mode || 'login') === 'register' ? 'register' : 'login')

const loginForm = ref({
  account: String(route.query.account || '').trim(),
  loginKey: ''
})
const registerForm = ref({
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const showRegisterResult = ref(false)
const registeredWolfNo = ref('')
const loading = ref(false)
const errorMessage = ref('')

function normalizeText(value) {
  return String(value || '').trim()
}

function normalizeEmail(value) {
  return String(value || '').trim().toLowerCase()
}

function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || '').trim())
}

function evaluatePasswordStrength(password) {
  const source = String(password || '')
  if (!source) {
    return ''
  }
  let score = 0
  if (/[0-9]/.test(source)) score += 1
  if (/[a-z]/.test(source)) score += 1
  if (/[A-Z]/.test(source)) score += 1
  if (/[^a-zA-Z0-9]/.test(source)) score += 1
  if (source.length < 6 || score <= 1) return 'weak'
  if (score <= 2) return 'medium'
  return 'strong'
}

const passwordStrengthText = computed(() => {
  const level = evaluatePasswordStrength(registerForm.value.password)
  if (level === 'weak') return '密码强度：弱'
  if (level === 'medium') return '密码强度：中'
  if (level === 'strong') return '密码强度：强'
  return ''
})

const canLoginSubmit = computed(() => {
  if (loading.value) return false
  return Boolean(normalizeText(loginForm.value.account) && normalizeText(loginForm.value.loginKey))
})

const canRegisterSubmit = computed(() => {
  if (loading.value) return false
  return Boolean(
    normalizeText(registerForm.value.nickname) &&
    normalizeText(registerForm.value.password) &&
    normalizeText(registerForm.value.confirmPassword)
  )
})

watch(
  () => route.query,
  (query) => {
    if (query.mode === 'login' || query.mode === 'register') {
      mode.value = query.mode
    }
    if (typeof query.account === 'string') {
      loginForm.value.account = query.account
    }
  }
)

function switchMode(nextMode) {
  if (nextMode !== 'login' && nextMode !== 'register') {
    return
  }
  mode.value = nextMode
  errorMessage.value = ''
  if (nextMode === 'register') {
    showRegisterResult.value = false
    registeredWolfNo.value = ''
  }
}

async function handleLogin() {
  if (!canLoginSubmit.value) {
    errorMessage.value = '请输入账号和密码'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await login({
      account: normalizeText(loginForm.value.account),
      loginKey: normalizeText(loginForm.value.loginKey)
    })
    authStore.setAuth(data.token, data.userInfo)
    router.replace('/chat')
  } catch (error) {
    errorMessage.value = error.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!canRegisterSubmit.value) {
    return
  }
  const nickname = normalizeText(registerForm.value.nickname)
  const email = normalizeEmail(registerForm.value.email)
  const password = normalizeText(registerForm.value.password)
  const confirmPassword = normalizeText(registerForm.value.confirmPassword)

  if (!nickname) {
    errorMessage.value = '称谓不能为空'
    return
  }
  if (nickname.length > NICKNAME_MAX_LEN) {
    errorMessage.value = `称谓最多 ${NICKNAME_MAX_LEN} 个字符`
    return
  }
  if (password.length < 6) {
    errorMessage.value = '密码至少 6 位'
    return
  }
  if (password !== confirmPassword) {
    errorMessage.value = '两次输入密码不一致'
    return
  }
  if (email && !isValidEmail(email)) {
    errorMessage.value = '邮箱格式不正确'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const payload = { nickname, password }
    if (email) {
      payload.email = email
    }
    const data = await register(payload)
    authStore.setAuth(data.token, data.userInfo)
    showRegisterResult.value = true
    registeredWolfNo.value = String(data?.userInfo?.wolfNo || '')
  } catch (error) {
    errorMessage.value = error.message || '注册失败'
  } finally {
    loading.value = false
  }
}

function handleConfirmRegister() {
  router.replace('/chat')
}

function goResetPassword() {
  const account = normalizeText(loginForm.value.account)
  const maybeEmail = isValidEmail(account)
  if (maybeEmail) {
    router.push(`/reset-password?email=${encodeURIComponent(account)}`)
    return
  }
  router.push('/reset-password')
}
</script>

<template>
  <div class="login-page">
    <div class="card login-card" v-if="mode === 'login'">
      <h1 class="login-title">行者登录</h1>
      <p class="login-subtitle">狼藉号 / 邮箱 + 密码</p>

      <label class="label">账号</label>
      <input
        v-model="loginForm.account"
        class="input"
        placeholder="狼藉号或邮箱"
        @keyup.enter="handleLogin"
      />
      <label class="label">密码</label>
      <input
        v-model="loginForm.loginKey"
        class="input"
        type="password"
        placeholder="登录密码"
        @keyup.enter="handleLogin"
      />

      <p v-if="errorMessage" class="error login-error">{{ errorMessage }}</p>
      <button class="button button-primary login-btn" :disabled="!canLoginSubmit" @click="handleLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>
      <div class="link-row">
        <button class="link-btn" @click="goResetPassword">忘记密码</button>
        <button class="link-btn" @click="switchMode('register')">去注册</button>
      </div>
    </div>

    <div class="card login-card" v-else>
      <h1 class="login-title">注册新行者</h1>
      <p class="login-subtitle">填写基础信息后直接进入社区</p>

      <div v-if="showRegisterResult" class="register-result">
        <p class="result-title">注册成功</p>
        <p class="text-muted">系统已分配狼藉号：{{ registeredWolfNo || '--' }}</p>
        <button class="button button-primary login-btn" @click="handleConfirmRegister">进入社区</button>
        <button class="link-btn" @click="switchMode('login')">返回登录</button>
      </div>

      <template v-else>
        <label class="label">称谓</label>
        <input
          v-model="registerForm.nickname"
          class="input"
          maxlength="12"
          placeholder="输入公开称谓"
        />
        <p class="text-muted tiny">{{ normalizeText(registerForm.nickname).length }}/{{ NICKNAME_MAX_LEN }}</p>

        <label class="label">邮箱（选填）</label>
        <input
          v-model="registerForm.email"
          class="input"
          maxlength="128"
          placeholder="绑定后可用于找回密码"
        />

        <label class="label">密码</label>
        <input
          v-model="registerForm.password"
          class="input"
          type="password"
          maxlength="64"
          placeholder="至少 6 位"
        />
        <p v-if="passwordStrengthText" class="text-muted tiny">{{ passwordStrengthText }}</p>

        <label class="label">确认密码</label>
        <input
          v-model="registerForm.confirmPassword"
          class="input"
          type="password"
          maxlength="64"
          placeholder="再次输入密码"
          @keyup.enter="handleRegister"
        />

        <p v-if="errorMessage" class="error login-error">{{ errorMessage }}</p>
        <button class="button button-primary login-btn" :disabled="!canRegisterSubmit" @click="handleRegister">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <div class="link-row single">
          <button class="link-btn" @click="switchMode('login')">已有账号，去登录</button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.login-card {
  width: min(420px, 100%);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login-title {
  margin: 0;
  font-size: 24px;
  color: var(--text-main);
}

.login-subtitle {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--text-sub);
}

.label {
  font-size: 12px;
  color: var(--text-sub);
}

.login-btn {
  margin-top: 6px;
}

.login-error {
  margin: 0;
}

.tiny {
  margin: -2px 0 2px;
  font-size: 12px;
  text-align: right;
}

.link-row {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.link-row.single {
  justify-content: center;
}

.link-btn {
  border: none;
  background: transparent;
  color: var(--accent);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.register-result {
  display: grid;
  gap: 8px;
}

.result-title {
  margin: 0;
  color: var(--text-main);
  font-size: 16px;
}
</style>
