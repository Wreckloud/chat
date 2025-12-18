<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import logger from '@/utils/logger'

onMounted(() => {
  logger.debug('App', 'App 组件已挂载')
})
</script>

<style lang="scss">
#app {
  width: 100%;
  height: 100%;
}
</style>

