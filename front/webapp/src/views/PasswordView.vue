<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { changeCurrentUserPassword } from '@/api/modules'

const router = useRouter()
const authStore = useAuthStore()

const oldLoginKey = ref('')
const newLoginKey = ref('')
const confirmLoginKey = ref('')
const submitting = ref(false)
const errorMessage = ref('')

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

const passwordStrengthLevel = computed(() => evaluatePasswordStrength(newLoginKey.value))
const passwordStrengthText = computed(() => {
  if (passwordStrengthLevel.value === 'weak') return '弱'
  if (passwordStrengthLevel.value === 'medium') return '中'
  if (passwordStrengthLevel.value === 'strong') return '强'
  return ''
})

const canSubmit = computed(() => {
  if (submitting.value) {
    return false
  }
  return Boolean(
    String(oldLoginKey.value || '').trim() &&
    String(newLoginKey.value || '').trim() &&
    String(confirmLoginKey.value || '').trim()
  )
})

async function handleSubmit() {
  if (!canSubmit.value) {
    return
  }
  const oldPassword = String(oldLoginKey.value || '').trim()
  const newPassword = String(newLoginKey.value || '').trim()
  const confirmPassword = String(confirmLoginKey.value || '').trim()

  if (newPassword.length < 6) {
    errorMessage.value = '新密码至少 6 位'
    return
  }
  if (newPassword !== confirmPassword) {
    errorMessage.value = '两次输入的新密码不一致'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    await changeCurrentUserPassword({
      oldLoginKey: oldPassword,
      newLoginKey: newPassword,
      confirmLoginKey: confirmPassword
    })
    authStore.clearAuth()
    router.replace('/login')
  } catch (error) {
    errorMessage.value = error.message || '修改密码失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="password-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="router.back()">返回</button>
      <h1 class="page-title">修改密码</h1>
      <span class="page-action text-muted">账号安全</span>
    </header>

    <div class="page-body">
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <article class="card form-card">
        <label class="field-label">原密码</label>
        <input v-model="oldLoginKey" class="input field-input" type="password" placeholder="输入原密码" maxlength="64" />

        <label class="field-label">新密码</label>
        <input v-model="newLoginKey" class="input field-input" type="password" placeholder="输入新密码（6-64位）" maxlength="64" />
        <p v-if="passwordStrengthText" :class="['strength', `strength-${passwordStrengthLevel}`]">
          密码强度：{{ passwordStrengthText }}
        </p>

        <label class="field-label">确认新密码</label>
        <input v-model="confirmLoginKey" class="input field-input" type="password" placeholder="再次输入新密码" maxlength="64" />

        <button class="button button-primary submit-btn" :disabled="!canSubmit" @click="handleSubmit">
          {{ submitting ? '提交中...' : '确认修改' }}
        </button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.password-page {
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
  margin-bottom: 8px;
}

.strength {
  margin: -2px 0 8px;
  font-size: 12px;
}

.strength-weak {
  color: var(--retro-btn-danger-text);
}

.strength-medium {
  color: var(--retro-warning-text);
}

.strength-strong {
  color: var(--retro-success-text);
}

.submit-btn {
  width: 100%;
  margin-top: 2px;
}

</style>
