<template>
  <div class="ai-page">
    <div class="page-card panel-card">
      <div class="panel-header">
        <div class="panel-title">AI 运行管控</div>
        <div class="panel-tip">本页面修改的是运行态参数，保存后即时生效</div>
      </div>
      <el-form label-position="top" class="ai-form">
        <el-row :gutter="12">
          <el-col :span="6">
            <el-form-item label="AI总开关">
              <el-switch v-model="form.enabled" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Provider">
              <el-input v-model.trim="form.provider" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Model">
              <el-input v-model.trim="form.model" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="API Key">
              <el-tag :type="form.apiKeyConfigured ? 'success' : 'danger'">
                {{ form.apiKeyConfigured ? '已配置' : '未配置' }}
              </el-tag>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="Temperature">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.01" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Max Output Tokens">
              <el-input-number v-model="form.maxOutputTokens" :min="64" :max="4096" :step="16" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Timeout (ms)">
              <el-input-number v-model="form.timeoutMs" :min="3000" :max="120000" :step="1000" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="page-card panel-card">
      <div class="sub-title">全局守卫</div>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="守卫开关">
            <el-switch v-model="form.guardEnabled" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="每小时上限">
            <el-input-number v-model="form.guardMaxCallsPerHour" :min="1" :max="10000" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="每日上限">
            <el-input-number v-model="form.guardMaxCallsPerDay" :min="1" :max="200000" />
          </el-form-item>
        </el-col>
      </el-row>
    </div>

    <div class="page-card panel-card">
      <div class="sub-title">大厅场景</div>
      <el-row :gutter="12">
        <el-col :span="6"><el-form-item label="开关"><el-switch v-model="form.lobbyEnabled" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="AI用户ID"><el-input-number v-model="form.lobbyBotUserId" :min="1" :max="9999999999" :controls="false" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="基础回复概率"><el-input-number v-model="form.lobbyReplyProbability" :min="0" :max="1" :step="0.01" :precision="2" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="@回复概率"><el-input-number v-model="form.lobbyMentionReplyProbability" :min="0" :max="1" :step="0.01" :precision="2" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="6"><el-form-item label="冷却(秒)"><el-input-number v-model="form.lobbyCooldownSeconds" :min="0" :max="3600" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="每小时上限"><el-input-number v-model="form.lobbyMaxRepliesPerHour" :min="1" :max="1000" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="系统提示词"><el-input v-model="form.lobbySystemPrompt" type="textarea" :rows="3" maxlength="2000" show-word-limit /></el-form-item></el-col>
      </el-row>
    </div>

    <div class="page-card panel-card">
      <div class="sub-title">私聊场景</div>
      <el-row :gutter="12">
        <el-col :span="6"><el-form-item label="开关"><el-switch v-model="form.privateChatEnabled" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="AI用户ID"><el-input-number v-model="form.privateChatBotUserId" :min="1" :max="9999999999" :controls="false" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="回复概率"><el-input-number v-model="form.privateChatReplyProbability" :min="0" :max="1" :step="0.01" :precision="2" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="冷却(秒)"><el-input-number v-model="form.privateChatCooldownSeconds" :min="0" :max="3600" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="6"><el-form-item label="每小时上限"><el-input-number v-model="form.privateChatMaxRepliesPerHour" :min="1" :max="1000" /></el-form-item></el-col>
        <el-col :span="18"><el-form-item label="系统提示词"><el-input v-model="form.privateChatSystemPrompt" type="textarea" :rows="3" maxlength="2000" show-word-limit /></el-form-item></el-col>
      </el-row>
    </div>

    <div class="page-card panel-card">
      <div class="sub-title">论坛场景</div>
      <el-row :gutter="12">
        <el-col :span="6"><el-form-item label="开关"><el-switch v-model="form.forumEnabled" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="AI用户ID"><el-input-number v-model="form.forumBotUserId" :min="1" :max="9999999999" :controls="false" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="基础回复概率"><el-input-number v-model="form.forumReplyProbability" :min="0" :max="1" :step="0.01" :precision="2" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="@回复概率"><el-input-number v-model="form.forumMentionReplyProbability" :min="0" :max="1" :step="0.01" :precision="2" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="6"><el-form-item label="楼中楼回复概率"><el-input-number v-model="form.forumReplyToReplyProbability" :min="0" :max="1" :step="0.01" :precision="2" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="冷却(秒)"><el-input-number v-model="form.forumCooldownSeconds" :min="0" :max="3600" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="每小时上限"><el-input-number v-model="form.forumMaxRepliesPerHour" :min="1" :max="5000" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="每日上限"><el-input-number v-model="form.forumMaxRepliesPerDay" :min="1" :max="50000" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="24"><el-form-item label="系统提示词"><el-input v-model="form.forumSystemPrompt" type="textarea" :rows="3" maxlength="2000" show-word-limit /></el-form-item></el-col>
      </el-row>
    </div>

    <div class="page-card panel-card">
      <div class="sub-title">关注回关</div>
      <el-row :gutter="12">
        <el-col :span="8"><el-form-item label="自动回关"><el-switch v-model="form.followAutoFollowBackEnabled" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="最小延迟(秒)"><el-input-number v-model="form.followMinDelaySeconds" :min="0" :max="3600" /></el-form-item></el-col>
        <el-col :span="8"><el-form-item label="最大延迟(秒)"><el-input-number v-model="form.followMaxDelaySeconds" :min="0" :max="3600" /></el-form-item></el-col>
      </el-row>
    </div>

    <div class="action-bar">
      <el-button @click="loadConfig">重载</el-button>
      <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAiConfig, updateAiConfig } from '@/api/ai'

const saving = ref(false)
const form = reactive({
  enabled: false,
  provider: '',
  model: '',
  apiKeyConfigured: false,
  temperature: 0.68,
  maxOutputTokens: 400,
  timeoutMs: 20000,
  guardEnabled: true,
  guardMaxCallsPerHour: 80,
  guardMaxCallsPerDay: 600,
  lobbyEnabled: false,
  lobbyBotUserId: null,
  lobbyReplyProbability: 0.3,
  lobbyMentionReplyProbability: 0.9,
  lobbyCooldownSeconds: 30,
  lobbyMaxRepliesPerHour: 40,
  lobbySystemPrompt: '',
  privateChatEnabled: false,
  privateChatBotUserId: null,
  privateChatReplyProbability: 0.6,
  privateChatCooldownSeconds: 20,
  privateChatMaxRepliesPerHour: 60,
  privateChatSystemPrompt: '',
  forumEnabled: false,
  forumBotUserId: null,
  forumReplyProbability: 0.3,
  forumMentionReplyProbability: 0.9,
  forumReplyToReplyProbability: 0.2,
  forumCooldownSeconds: 20,
  forumMaxRepliesPerHour: 40,
  forumMaxRepliesPerDay: 150,
  forumSystemPrompt: '',
  followAutoFollowBackEnabled: false,
  followMinDelaySeconds: 10,
  followMaxDelaySeconds: 20
})

function fillForm(data) {
  if (!data) {
    return
  }
  Object.assign(form, {
    ...form,
    ...data
  })
}

async function loadConfig() {
  try {
    const data = await fetchAiConfig()
    fillForm(data)
  } catch (error) {
    ElMessage.error(error?.message || '加载AI配置失败')
  }
}

async function saveConfig() {
  saving.value = true
  try {
    const payload = { ...form }
    delete payload.apiKeyConfigured
    const data = await updateAiConfig(payload)
    fillForm(data)
    ElMessage.success('AI配置已保存并生效')
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped lang="scss">
.ai-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-card {
  padding: 14px;
}

.panel-header {
  margin-bottom: 8px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
}

.panel-tip {
  margin-top: 2px;
  font-size: 12px;
  color: #69788a;
}

.sub-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>

