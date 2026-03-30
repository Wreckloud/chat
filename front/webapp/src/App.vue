<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const themeStore = useThemeStore()
const showTabBar = computed(() => Boolean(route.meta.tabbar))
</script>

<template>
  <div :class="['app-root', themeStore.rootClass]">
    <router-view class="page" />
    <nav v-if="showTabBar" class="app-tabbar">
      <RouterLink to="/chat" class="tab-item">聊天</RouterLink>
      <RouterLink to="/community" class="tab-item">社区</RouterLink>
      <RouterLink to="/me" class="tab-item">我的</RouterLink>
    </nav>
  </div>
</template>
