<template>
  <div class="login-container">
    <div class="login-card wolf-card">
      <div class="login-header">
        <div class="logo">🐺</div>
        <h2 class="title">WolfChat 管理后台</h2>
        <p class="subtitle">深绿森林，狼的领地</p>
      </div>
      
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="mobile">
          <el-input
            v-model="loginForm.mobile"
            placeholder="请输入手机号"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><Phone /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="smsCode">
          <el-input
            v-model="loginForm.smsCode"
            placeholder="请输入验证码"
            size="large"
            clearable
          >
            <template #prefix>
              <el-icon><Key /></el-icon>
            </template>
            <template #append>
              <el-button
                :disabled="countdown > 0"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}秒后重试` : '获取验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <el-text type="info" size="small">
          管理员账号登录 · 仅限授权人员
        </el-text>
      </div>
    </div>
    
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="wolf-pattern">🐺</div>
      <div class="wolf-pattern">🐺</div>
      <div class="wolf-pattern">🐺</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginByMobile, sendSmsCode } from '@/api/account'
import { useAuthStore } from '@/store/auth'
import logger from '@/utils/logger'

const router = useRouter()
const authStore = useAuthStore()

// 表单引用
const loginFormRef = ref(null)

// 登录表单
const loginForm = reactive({
  mobile: '',
  smsCode: '',
  smsCodeKey: ''
})

// 表单验证规则
const loginRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

// 加载状态
const loading = ref(false)
// 倒计时
const countdown = ref(0)

/**
 * 发送验证码
 */
const handleSendCode = async () => {
  // 验证手机号
  if (!loginForm.mobile) {
    ElMessage.warning('请输入手机号')
    return
  }
  
  if (!/^1[3-9]\d{9}$/.test(loginForm.mobile)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  
  logger.action('LoginPage', 'sendCode', { mobile: loginForm.mobile })
  
  try {
    const res = await sendSmsCode(loginForm.mobile)
    loginForm.smsCodeKey = res.smsCodeKey
    
    ElMessage.success('验证码已发送')
    logger.info('LoginPage', '验证码发送成功', { smsCodeKey: res.smsCodeKey })
    
    // 开始倒计时
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    logger.error('LoginPage', '发送验证码失败', error)
  }
}

/**
 * 登录
 */
const handleLogin = async () => {
  // 验证表单
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (!loginForm.smsCodeKey) {
      ElMessage.warning('请先获取验证码')
      return
    }
    
    logger.action('LoginPage', 'login', { mobile: loginForm.mobile })
    
    loading.value = true
    
    try {
      const res = await loginByMobile({
        mobile: loginForm.mobile,
        smsCodeKey: loginForm.smsCodeKey,
        smsCode: loginForm.smsCode
      })
      
      logger.info('LoginPage', '登录成功', { userId: res.userId })
      
      // 保存登录信息
      authStore.login(res)
      
      ElMessage.success('登录成功')
      
      // 跳转到首页
      setTimeout(() => {
        router.push('/')
      }, 500)
    } catch (error) {
      logger.error('LoginPage', '登录失败', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $primary-dark 0%, $primary-color 50%, $primary-light 100%);
  position: relative;
  overflow: hidden;
}

.login-card {
  width: 420px;
  padding: 40px;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95) !important;
  z-index: 1;
  
  .login-header {
    text-align: center;
    margin-bottom: 40px;
    
    .logo {
      font-size: 64px;
      margin-bottom: 16px;
      animation: float 3s ease-in-out infinite;
    }
    
    .title {
      font-size: 24px;
      font-weight: 600;
      color: $primary-color;
      margin-bottom: 8px;
    }
    
    .subtitle {
      font-size: 14px;
      color: $text-secondary;
    }
  }
  
  .login-form {
    .login-btn {
      width: 100%;
      background: $primary-color;
      border-color: $primary-color;
      
      &:hover {
        background: $primary-light;
        border-color: $primary-light;
      }
    }
  }
  
  .login-footer {
    text-align: center;
    margin-top: 20px;
  }
}

.bg-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  overflow: hidden;
  opacity: 0.1;
  
  .wolf-pattern {
    position: absolute;
    font-size: 200px;
    color: $background-white;
    animation: float 6s ease-in-out infinite;
    
    &:nth-child(1) {
      top: 10%;
      left: 10%;
      animation-delay: 0s;
    }
    
    &:nth-child(2) {
      top: 60%;
      right: 15%;
      animation-delay: 2s;
    }
    
    &:nth-child(3) {
      bottom: 15%;
      left: 50%;
      animation-delay: 4s;
    }
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}
</style>

