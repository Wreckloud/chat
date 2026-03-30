<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  createConversation,
  listFollowers,
  listFollowing,
  listMutualFollows
} from '@/api/modules'

const router = useRouter()
const authStore = useAuthStore()

const tabs = [
  { key: 'following', label: '关注' },
  { key: 'followers', label: '粉丝' },
  { key: 'mutual', label: '互关' }
]

const activeTab = ref('following')
const loading = ref(false)
const errorMessage = ref('')
const followingList = ref([])
const followerList = ref([])
const mutualList = ref([])
const chatLoadingMap = ref({})

const currentList = computed(() => {
  if (activeTab.value === 'followers') {
    return followerList.value
  }
  if (activeTab.value === 'mutual') {
    return mutualList.value
  }
  return followingList.value
})

function resolveDisplayName(item) {
  return String(item?.nickname || item?.wolfNo || `行者${item?.userId || ''}`).trim() || '行者'
}

async function loadList() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [following, followers, mutual] = await Promise.all([
      listFollowing(),
      listFollowers(),
      listMutualFollows()
    ])
    followingList.value = Array.isArray(following) ? following : []
    followerList.value = Array.isArray(followers) ? followers : []
    mutualList.value = Array.isArray(mutual) ? mutual : []
  } catch (error) {
    errorMessage.value = error.message || '加载关注列表失败'
  } finally {
    loading.value = false
  }
}

function openUser(item) {
  const targetUserId = Number(item?.userId || 0)
  if (!targetUserId) {
    return
  }
  router.push(`/user/${targetUserId}`)
}

async function startConversation(item) {
  const targetUserId = Number(item?.userId || 0)
  if (!targetUserId || targetUserId === Number(authStore.userId || 0)) {
    return
  }
  if (chatLoadingMap.value[targetUserId]) {
    return
  }
  chatLoadingMap.value = {
    ...chatLoadingMap.value,
    [targetUserId]: true
  }
  try {
    const conversationId = await createConversation(targetUserId)
    router.push(`/chat/${conversationId}`)
  } catch (error) {
    errorMessage.value = error.message || '无法发起聊天'
  } finally {
    chatLoadingMap.value = {
      ...chatLoadingMap.value,
      [targetUserId]: false
    }
  }
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <section class="follow-page">
    <header class="page-header">
      <button class="button page-action-btn" @click="router.back()">返回</button>
      <h1 class="page-title">关注列表</h1>
      <button class="button page-action-btn" :disabled="loading" @click="loadList">刷新</button>
    </header>

    <div class="page-body">
      <div class="retro-tab-row retro-tab-row-fill">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="['button retro-tab-btn', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="loading" class="text-muted">加载中...</p>
      <div v-if="!loading && currentList.length === 0" class="empty">暂无数据</div>

      <article
        v-for="item in currentList"
        :key="`${activeTab}_${item.userId}`"
        class="card retro-list-card follow-item"
      >
        <div class="follow-main" @click="openUser(item)">
          <div class="avatar-box">
            <img v-if="item.avatar" :src="item.avatar" alt="" class="avatar" />
            <div v-else class="avatar placeholder">狼</div>
          </div>
          <div class="follow-meta">
            <div class="name-line">
              <strong>{{ resolveDisplayName(item) }}</strong>
              <span v-if="item.equippedTitleName" class="title-badge">[{{ item.equippedTitleName }}]</span>
            </div>
            <p class="text-muted">狼藉号 {{ item.wolfNo || '--' }}</p>
            <p class="text-muted">{{ item.mutual ? '互相关注' : '单向关注' }}</p>
          </div>
        </div>
        <button class="button action-btn" :disabled="chatLoadingMap[item.userId]" @click="startConversation(item)">
          发消息
        </button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.follow-page {
  min-height: 100%;
}

.follow-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.follow-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
  cursor: pointer;
}

.avatar-box {
  width: 42px;
  height: 42px;
  border: 1px solid var(--line);
  flex: 0 0 auto;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--retro-muted-text);
  background: var(--retro-theme-item-bg);
}

.follow-meta {
  min-width: 0;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}

.title-badge {
  font-size: 12px;
  color: var(--retro-accent-text);
}

.follow-meta p {
  margin: 0 0 3px;
}

.action-btn {
  height: 30px;
  font-size: 12px;
}

</style>
