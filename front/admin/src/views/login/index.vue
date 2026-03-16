<template>
  <div class="login-page">
    <div class="login-panel page-card">
      <div class="panel-title">WolfChat 管理端</div>
      <div class="panel-sub">请输入管理员账号信息</div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="账号" prop="account">
          <el-input v-model.trim="form.account" placeholder="请输入管理员账号" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model.trim="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            clearable
            @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginByPassword, fetchAdminProfile } from '@/api/auth'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  account: '',
  password: ''
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    const loginRes = await loginByPassword({
      account: form.account,
      password: form.password
    })
    authStore.setToken(loginRes?.token || '')

    const profile = await fetchAdminProfile()
    authStore.setAdminInfo(profile || null)

    ElMessage.success('登录成功')
    router.replace('/dashboard')
  } catch (error) {
    authStore.logout()
    ElMessage.error(error?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at top, #d9e8de 0%, #f3f6f9 55%, #eef2f6 100%);
}

.login-panel {
  width: 420px;
  padding: 24px;
  border-radius: 6px;
}

.panel-title {
  font-size: 24px;
  font-weight: 700;
  color: #173327;
}

.panel-sub {
  margin: 6px 0 16px;
  font-size: 13px;
  color: #5a6d7f;
}

.submit-btn {
  width: 100%;
}
</style>
