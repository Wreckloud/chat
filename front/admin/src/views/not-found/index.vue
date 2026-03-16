<template>
  <div class="not-found page-card">
    <div class="code">404</div>
    <div class="text">页面不存在</div>
    <el-button type="primary" @click="goHome">返回控制台</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.replace('/dashboard')
}
</script>

<style scoped lang="scss">
.not-found {
  margin: 40px auto;
  max-width: 480px;
  padding: 40px 20px;
  text-align: center;
}

.code {
  font-size: 56px;
  font-weight: 700;
  color: $primary;
}

.text {
  margin: 10px 0 20px;
  font-size: 15px;
  color: #5c6f83;
}
</style>
