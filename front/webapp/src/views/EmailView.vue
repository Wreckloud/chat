<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUser, sendBindEmailLink } from '@/api/modules'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const sending = ref(false)
const errorMessage = ref('')
const userInfo = ref(null)
const emailInput = ref('')

function normalizeEmail(value) {
  return String(value || '').trim().toLowerCase()
}

function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || '').trim())
}

const statusMeta = computed(() => {
  const user = userInfo.value
  if (!user?.email) {
    return {
      type: 'unbound',
      text: '未绑定',
      email: '未设置',
      hint: '绑定邮箱后可用于找回密码与登录。'
    }
  }
  if (user.emailVerified === true) {
    return {
      type: 'verified',
      text: '已认证',
      email: user.email,
      hint: '当前邮箱已认证，可用于账号登录。'
    }
  }
  return {
    type: 'pending',
    text: '待认证',
    email: user.email,
    hint: '请点击发送链接并到邮箱完成认证。'
  }
})

const emailInputDisabled = computed(() => Boolean(userInfo.value?.email))
const canSendLink = computed(() => userInfo.value?.emailVerified !== true)

const submitText = computed(() => {
  const user = userInfo.value
  if (!user?.email) {
    return '发送认证链接'
  }
  if (user.emailVerified === true) {
    return '邮箱已认证'
  }
  const sameEmail = normalizeEmail(emailInput.value) === normalizeEmail(user.email)
  return sameEmail ? '重新发送认证链接' : '发送认证链接'
})

async function loadUserInfo() {
  loading.value = true
  errorMessage.value = ''
  try {
    const user = await getCurrentUser()
    userInfo.value = user || null
    emailInput.value = String(user?.email || '').trim()
    authStore.setAuth(authStore.token, user)
  } catch (error) {
    errorMessage.value = error.message || '加载邮箱信息失败'
  } finally {
    loading.value = false
  }
}

async function handleSendLink() {
  if (sending.value || !canSendLink.value) {
    return
  }
  const normalizedEmail = normalizeEmail(emailInput.value)
  if (!normalizedEmail) {
    errorMessage.value = '请输入邮箱地址'
    return
  }
  if (!isValidEmail(normalizedEmail)) {
    errorMessage.value = '邮箱格式不正确'
    return
  }
  sending.value = true
  errorMessage.value = ''
  try {
    await sendBindEmailLink({ email: normalizedEmail })
    await loadUserInfo()
  } catch (error) {
    errorMessage.value = error.message || '发送失败'
  } finally {
    sending.value = false
  }
}

onMounted(loadUserInfo)
</script>

<template>
  <section class="email-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="router.back()">返回</button>
      <h1 class="page-title">邮箱管理</h1>
      <button class="button page-action-btn" :disabled="loading" @click="loadUserInfo">刷新</button>
    </header>

    <div class="page-body">
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <article class="card status-card">
        <div class="status-row">
          <span class="text-muted">认证状态</span>
          <span :class="['status-tag', `status-${statusMeta.type}`]">{{ statusMeta.text }}</span>
        </div>
        <div class="status-email">{{ statusMeta.email }}</div>
        <div class="text-muted">{{ statusMeta.hint }}</div>
      </article>

      <article class="card form-card">
        <label class="field-label">邮箱</label>
        <input
          v-model="emailInput"
          class="input field-input"
          maxlength="128"
          placeholder="输入要绑定的邮箱"
          :disabled="emailInputDisabled"
        />

        <button
          v-if="canSendLink"
          class="button button-primary submit-btn"
          :disabled="sending"
          @click="handleSendLink"
        >
          {{ sending ? '发送中...' : submitText }}
        </button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.email-page {
  min-height: 100%;
}

.status-card,
.form-card {
  padding: 10px;
  margin-bottom: 10px;
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.status-tag {
  font-size: 12px;
  color: var(--text-main);
}

.status-unbound {
  color: var(--text-sub);
}

.status-pending {
  color: var(--retro-warning-text);
}

.status-verified {
  color: var(--retro-success-text);
}

.status-email {
  margin-bottom: 6px;
}

.field-label {
  display: block;
  color: var(--text-sub);
  font-size: 12px;
  margin-bottom: 6px;
}

.field-input {
  width: 100%;
  margin-bottom: 10px;
}

.submit-btn {
  width: 100%;
}

</style>
