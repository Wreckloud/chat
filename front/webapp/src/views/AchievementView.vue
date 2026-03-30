<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  equipTitle,
  getCurrentUser,
  listMyAchievements,
  unequipTitle
} from '@/api/modules'
import { formatDateTime } from '@/utils/time'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const equipping = ref(false)
const errorMessage = ref('')
const achievements = ref([])

const equippedTitleName = computed(() => String(authStore.userInfo?.equippedTitleName || '').trim())
const equippedTitleColor = computed(() => String(authStore.userInfo?.equippedTitleColor || '').trim())

function normalizeAchievement(item) {
  return {
    achievementCode: String(item?.achievementCode || '').trim(),
    name: String(item?.name || '未命名成就').trim(),
    description: String(item?.description || '').trim(),
    titleName: String(item?.titleName || '').trim(),
    titleColor: String(item?.titleColor || '').trim(),
    unlocked: item?.unlocked === true,
    equipped: item?.equipped === true,
    unlockTime: item?.unlockTime || ''
  }
}

function resolveUnlockTimeText(item) {
  if (!item?.unlocked) {
    return '--'
  }
  return formatDateTime(item.unlockTime)
}

async function loadData() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [userInfo, achievementList] = await Promise.all([
      getCurrentUser(),
      listMyAchievements()
    ])
    authStore.setAuth(authStore.token, userInfo)
    achievements.value = (Array.isArray(achievementList) ? achievementList : []).map((item) => normalizeAchievement(item))
  } catch (error) {
    errorMessage.value = error.message || '加载成就失败'
  } finally {
    loading.value = false
  }
}

async function handleEquip(achievementCode) {
  if (equipping.value || !achievementCode) {
    return
  }
  equipping.value = true
  errorMessage.value = ''
  try {
    await equipTitle(achievementCode)
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '佩戴失败'
  } finally {
    equipping.value = false
  }
}

async function handleUnequip() {
  if (equipping.value) {
    return
  }
  equipping.value = true
  errorMessage.value = ''
  try {
    await unequipTitle()
    await loadData()
  } catch (error) {
    errorMessage.value = error.message || '取消佩戴失败'
  } finally {
    equipping.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <section class="achievement-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="router.back()">返回</button>
      <h1 class="page-title">成就头衔</h1>
      <button class="button page-action-btn" :disabled="loading" @click="loadData">刷新</button>
    </header>

    <div class="page-body">
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <article class="card equipped-card">
        <div class="section-head">
          <strong>当前佩戴</strong>
          <button
            v-if="equippedTitleName"
            class="button page-action-btn"
            :disabled="equipping"
            @click="handleUnequip"
          >
            取消佩戴
          </button>
        </div>
        <div v-if="equippedTitleName" class="equipped-main">
          <strong>{{ authStore.userInfo?.nickname || authStore.userInfo?.wolfNo || '行者' }}</strong>
          <span class="title-badge" :style="equippedTitleColor ? `color:${equippedTitleColor}` : ''">
            [{{ equippedTitleName }}]
          </span>
        </div>
        <div v-else class="text-muted">未佩戴头衔</div>
      </article>

      <article class="card list-card">
        <div class="section-head">
          <strong>我的成就</strong>
          <span class="text-muted">解锁后可佩戴</span>
        </div>
        <p v-if="loading && achievements.length === 0" class="text-muted">加载中...</p>
        <div v-else-if="achievements.length === 0" class="empty">暂无成就</div>
        <div v-else class="achievement-list">
          <div v-for="item in achievements" :key="item.achievementCode" class="achievement-item">
            <div class="achievement-main">
              <div class="name-line">
                <strong>{{ item.name }}</strong>
                <span
                  v-if="item.titleName"
                  class="title-badge"
                  :style="item.titleColor ? `color:${item.titleColor}` : ''"
                >
                  [{{ item.titleName }}]
                </span>
              </div>
              <div class="text-muted description">{{ item.description || '--' }}</div>
              <div class="text-muted meta">
                <span>{{ item.unlocked ? '已解锁' : '未解锁' }}</span>
                <span v-if="item.unlocked">解锁于 {{ resolveUnlockTimeText(item) }}</span>
              </div>
            </div>
            <div class="achievement-action">
              <button
                v-if="item.unlocked && !item.equipped"
                class="button page-action-btn"
                :disabled="equipping"
                @click="handleEquip(item.achievementCode)"
              >
                佩戴
              </button>
              <span v-else-if="item.equipped" class="title-badge">[已佩戴]</span>
              <span v-else class="text-muted">[未解锁]</span>
            </div>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.achievement-page {
  min-height: 100%;
}

.equipped-card,
.list-card {
  padding: 10px;
  margin-bottom: 10px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.equipped-main,
.name-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.title-badge {
  color: var(--accent);
  font-size: 12px;
}

.achievement-list {
  display: grid;
  gap: 8px;
}

.achievement-item {
  border: 1px solid var(--line);
  background: var(--retro-theme-item-bg);
  padding: 8px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.achievement-main {
  min-width: 0;
  flex: 1;
}

.description {
  margin-top: 4px;
}

.meta {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.achievement-action {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
}

</style>
