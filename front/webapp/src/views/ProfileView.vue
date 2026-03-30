<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUser, updateCurrentUserProfile } from '@/api/modules'

const NICKNAME_MAX_LEN = 12

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

const nickname = ref('')
const signature = ref('')
const bio = ref('')
const avatar = ref('')

const originalForm = ref({
  nickname: '',
  signature: '',
  bio: ''
})

function normalizeText(value) {
  return String(value || '').trim()
}

const normalizedNickname = computed(() => normalizeText(nickname.value))
const canSubmit = computed(() => {
  if (submitting.value || loading.value) {
    return false
  }
  if (!normalizedNickname.value || normalizedNickname.value.length > NICKNAME_MAX_LEN) {
    return false
  }
  const changed = (
    normalizedNickname.value !== originalForm.value.nickname ||
    normalizeText(signature.value) !== originalForm.value.signature ||
    normalizeText(bio.value) !== originalForm.value.bio
  )
  return changed
})

async function loadProfile() {
  loading.value = true
  errorMessage.value = ''
  try {
    const user = await getCurrentUser()
    authStore.setAuth(authStore.token, user)
    nickname.value = String(user?.nickname || '').trim()
    signature.value = String(user?.signature || '').trim()
    bio.value = String(user?.bio || '').trim()
    avatar.value = String(user?.avatar || '').trim()
    originalForm.value = {
      nickname: normalizeText(nickname.value),
      signature: normalizeText(signature.value),
      bio: normalizeText(bio.value)
    }
  } catch (error) {
    errorMessage.value = error.message || '加载资料失败'
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!canSubmit.value) {
    return
  }
  const payload = {
    nickname: normalizedNickname.value,
    signature: normalizeText(signature.value),
    bio: normalizeText(bio.value)
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    const user = await updateCurrentUserProfile(payload)
    authStore.setAuth(authStore.token, user)
    nickname.value = String(user?.nickname || '').trim()
    signature.value = String(user?.signature || '').trim()
    bio.value = String(user?.bio || '').trim()
    avatar.value = String(user?.avatar || '').trim()
    originalForm.value = {
      nickname: normalizeText(nickname.value),
      signature: normalizeText(signature.value),
      bio: normalizeText(bio.value)
    }
  } catch (error) {
    errorMessage.value = error.message || '保存失败'
  } finally {
    submitting.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <section class="profile-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="router.back()">返回</button>
      <h1 class="page-title">编辑资料</h1>
      <button class="button page-action-btn" :disabled="loading" @click="loadProfile">刷新</button>
    </header>

    <div class="page-body">
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <article class="card preview-card">
        <div class="avatar-box">
          <img v-if="avatar" :src="avatar" alt="" class="avatar" />
          <div v-else class="avatar placeholder">{{ nickname ? nickname.slice(0, 1) : '行' }}</div>
        </div>
        <div class="preview-meta">
          <strong>{{ nickname || '未命名' }}</strong>
          <p class="text-muted">{{ signature || '未设置个性签名' }}</p>
        </div>
      </article>

      <article class="card form-card">
        <label class="field-label">称谓</label>
        <input v-model="nickname" class="input field-input" maxlength="12" placeholder="输入你的公开称谓" />
        <p class="text-muted field-hint">{{ normalizedNickname.length }}/{{ NICKNAME_MAX_LEN }}</p>

        <label class="field-label">个性签名</label>
        <input v-model="signature" class="input field-input" maxlength="255" placeholder="一句简短的话" />

        <label class="field-label">个人简介</label>
        <textarea v-model="bio" class="input field-textarea" maxlength="1000" placeholder="介绍一下你自己" />

        <button class="button button-primary submit-btn" :disabled="!canSubmit" @click="handleSubmit">
          {{ submitting ? '保存中...' : '保存资料' }}
        </button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.profile-page {
  min-height: 100%;
}

.preview-card,
.form-card {
  padding: 10px;
  margin-bottom: 10px;
}

.preview-card {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-box {
  width: 56px;
  height: 56px;
  border: 1px solid var(--line);
  background: var(--retro-theme-item-bg);
  flex: 0 0 auto;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar.placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-sub);
}

.preview-meta {
  min-width: 0;
}

.preview-meta p {
  margin: 4px 0 0;
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

.field-hint {
  margin: -4px 0 8px;
  text-align: right;
  font-size: 12px;
}

.field-textarea {
  width: 100%;
  height: 120px;
  resize: vertical;
  padding: 8px;
  margin-bottom: 10px;
}

.submit-btn {
  width: 100%;
}

</style>
