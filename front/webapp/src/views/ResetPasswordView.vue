<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { sendResetPasswordLink } from '@/api/modules'

const SEND_INTERVAL_SECONDS = 60

const route = useRoute()
const router = useRouter()

const email = ref(String(route.query.email || '').trim())
const sending = ref(false)
const cooldown = ref(0)
const errorMessage = ref('')

let timer = null

function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(value || '').trim())
}

function clearTimer() {
  if (!timer) {
    return
  }
  clearInterval(timer)
  timer = null
}

function startCooldown(seconds) {
  clearTimer()
  cooldown.value = seconds
  timer = setInterval(() => {
    if (cooldown.value <= 1) {
      clearTimer()
      cooldown.value = 0
      return
    }
    cooldown.value -= 1
  }, 1000)
}

async function handleSendLink() {
  if (sending.value || cooldown.value > 0) {
    return
  }
  const normalizedEmail = String(email.value || '').trim().toLowerCase()
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
    await sendResetPasswordLink({ email: normalizedEmail })
    startCooldown(SEND_INTERVAL_SECONDS)
  } catch (error) {
    errorMessage.value = error.message || '发送失败'
  } finally {
    sending.value = false
  }
}

function goBackToLogin() {
  const normalizedEmail = String(email.value || '').trim()
  if (!normalizedEmail) {
    router.replace('/login?mode=login')
    return
  }
  router.replace(`/login?mode=login&account=${encodeURIComponent(normalizedEmail)}`)
}

onBeforeUnmount(() => {
  clearTimer()
})
</script>

<template>
  <section class="reset-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="goBackToLogin">返回登录</button>
      <h1 class="page-title">找回密码</h1>
      <span class="page-action text-muted">邮箱重置</span>
    </header>

    <div class="page-body">
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <article class="card form-card">
        <label class="field-label">邮箱</label>
        <input
          v-model="email"
          class="input field-input"
          maxlength="128"
          placeholder="输入已绑定邮箱"
          @keyup.enter="handleSendLink"
        />
        <button class="button button-primary submit-btn" :disabled="sending || cooldown > 0" @click="handleSendLink">
          <template v-if="sending">发送中...</template>
          <template v-else-if="cooldown > 0">{{ cooldown }}s</template>
          <template v-else>发送重置链接</template>
        </button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.reset-page {
  min-height: 100%;
}

.form-card {
  padding: 10px;
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
