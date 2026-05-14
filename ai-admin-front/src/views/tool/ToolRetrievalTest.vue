<template>
  <div class="page-container">
    <div class="page-header">
      <h2>Tool 检索测试</h2>
      <div class="header-actions">
        <el-button type="warning" @click="openRebuildDialog">重建向量索引</el-button>
      </div>
    </div>

    <el-card shadow="never" class="section-card">
      <el-form :inline="true" class="search-form" @submit.prevent="handleSearch">
        <el-form-item label="用户问题">
          <el-input
            v-model="form.query"
            placeholder="例如：帮我查询最近一周的工单数量"
            style="width: 420px"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="TopK">
          <el-input-number v-model="form.topK" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="仅启用">
          <el-switch v-model="form.enabledOnly" />
        </el-form-item>
        <el-form-item label="仅 Agent 可见">
          <el-switch v-model="form.agentVisibleOnly" />
        </el-form-item>
        <el-form-item label="相似度下限">
          <el-input-number
            v-model="form.minScore"
            :min="0"
            :max="1"
            :step="0.05"
            :precision="2"
            placeholder="默认用服务端配置"
            controls-position="right"
            style="width: 160px"
          />
          <span class="form-hint">0=不过滤；留空用服务端 min-score</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="searching" @click="handleSearch">检索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>召回结果（{{ candidates.length }}）</template>
      <el-empty v-if="!searching && candidates.length === 0" description="无召回结果" />
      <el-table v-else :data="candidates" stripe>
        <el-table-column label="#" type="index" width="60" />
        <el-table-column prop="toolName" label="Tool 名" min-width="220" />
        <el-table-column label="分数" width="100">
          <template #default="{ row }">
            <el-tag :type="scoreTag(row.score)">{{ row.score.toFixed(4) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="projectId" label="项目 ID" width="100" />
        <el-table-column prop="moduleId" label="模块 ID" width="100" />
        <el-table-column label="入库文本" min-width="320">
          <template #default="{ row }">
            <span class="text-ellipsis" :title="row.text">{{ row.text }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="task" shadow="never" class="section-card">
      <template #header>
        <span>重建任务</span>
        <el-tag :type="stageTag(task.stage)" style="margin-left: 10px">{{ task.stage }}</el-tag>
      </template>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="总数">{{ task.totalSteps }}</el-descriptions-item>
        <el-descriptions-item label="已完成">{{ task.completedSteps }}</el-descriptions-item>
        <el-descriptions-item label="成功">{{ task.successCount }}</el-descriptions-item>
        <el-descriptions-item label="跳过">{{ task.skippedCount }}</el-descriptions-item>
        <el-descriptions-item label="失败">{{ task.failedCount }}</el-descriptions-item>
        <el-descriptions-item label="向量模型实例">{{ task.embeddingModelInstanceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前">{{ task.currentStep || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始">{{ task.startedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束">{{ task.finishedAt || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-progress
        v-if="task.stage === 'QUEUED' || task.stage === 'RUNNING'"
        :percentage="taskPercent"
        :text-inside="true"
        :stroke-width="18"
        style="margin-top: 12px"
      />
      <el-alert
        v-if="task.stage === 'FAILED'"
        style="margin-top: 12px"
        type="error"
        :title="`重建失败：${task.errorMessage || '未知错误'}`"
        :closable="false"
        show-icon
      />
    </el-card>

    <el-dialog
      v-model="rebuildDialogVisible"
      title="选择向量索引模型"
      width="480px"
      destroy-on-close
      @open="loadEmbeddingInstances"
    >
      <p class="rebuild-dialog-hint">
        重建会为每条 Tool 定义调用 Embedding 服务写入 Milvus，请选择与集合维度一致且状态为可用的向量模型实例。
        所选模型实例 ID 会写入库表 tool_retrieval_setting，供智能体对话时的 Tool 语义召回与用户问题向量化共用；未设置环境变量时不再使用占位默认值。
      </p>
      <el-form label-width="120px">
        <el-form-item label="模型厂商" required>
          <el-select
            v-model="rebuildModelProvider"
            placeholder="请选择厂商"
            filterable
            style="width: 100%"
            @change="handleRebuildProviderChange"
          >
            <el-option
              v-for="provider in embeddingProviderOptions"
              :key="provider"
              :label="provider"
              :value="provider"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Embedding 实例" required>
          <el-select
            v-model="rebuildModelInstanceId"
            placeholder="请选择向量模型实例"
            filterable
            style="width: 100%"
            :disabled="!rebuildModelProvider"
          >
            <el-option
              v-for="item in filteredEmbeddingInstances"
              :key="item.id"
              :label="`${item.name} / ${item.modelName}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rebuildDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="rebuildStarting" @click="confirmRebuild">开始重建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getToolRetrievalRebuildStatus,
  searchToolRetrieval,
  startToolRetrievalRebuild,
} from '@/api/toolRetrieval'
import { getModelInstances } from '@/api/model'
import type { ModelInstance } from '@/types/model'
import type { ToolCandidate, ToolRebuildTask, ToolRetrievalSearchRequest } from '@/types/toolRetrieval'

const form = reactive({
  query: '',
  topK: 10,
  enabledOnly: false,
  agentVisibleOnly: false,
  /** undefined：不传，走后端默认 min-score */
  minScore: undefined as number | undefined,
})

const searching = ref(false)
const candidates = ref<ToolCandidate[]>([])

const rebuildStarting = ref(false)
const rebuildDialogVisible = ref(false)
const rebuildModelProvider = ref('')
const rebuildModelInstanceId = ref('')
const embeddingInstances = ref<ModelInstance[]>([])
const task = ref<ToolRebuildTask | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const taskPercent = computed(() => {
  if (!task.value || !task.value.totalSteps) return 0
  return Math.round((task.value.completedSteps / task.value.totalSteps) * 100)
})
const embeddingProviderOptions = computed(() =>
  Array.from(new Set(embeddingInstances.value.map((item) => item.provider).filter(Boolean))).sort(),
)
const filteredEmbeddingInstances = computed(() =>
  embeddingInstances.value.filter((item) => item.provider === rebuildModelProvider.value),
)

async function handleSearch() {
  if (!form.query.trim()) {
    ElMessage.warning('请输入用户问题')
    return
  }
  searching.value = true
  try {
    const payload: ToolRetrievalSearchRequest = {
      query: form.query.trim(),
      topK: form.topK,
      enabledOnly: form.enabledOnly,
      agentVisibleOnly: form.agentVisibleOnly,
    }
    if (form.minScore !== undefined && form.minScore !== null) {
      payload.minScore = form.minScore
    }
    const { data } = await searchToolRetrieval(payload)
    candidates.value = data?.candidates || []
    if (!candidates.value.length && data?.message) {
      ElMessage.info(data.message)
    }
  } catch (err) {
    ElMessage.error((err as Error).message || '检索失败')
  } finally {
    searching.value = false
  }
}

function openRebuildDialog() {
  rebuildModelProvider.value = ''
  rebuildModelInstanceId.value = ''
  rebuildDialogVisible.value = true
}

function handleRebuildProviderChange() {
  rebuildModelInstanceId.value = ''
}

async function loadEmbeddingInstances() {
  try {
    const { data } = await getModelInstances({ modelType: 'EMBEDDING' })
    const list = data?.data ?? (Array.isArray(data) ? data : [])
    embeddingInstances.value = list.filter((item) => item.status === 'ACTIVE')
    if (!embeddingInstances.value.length) {
      ElMessage.warning('未找到已开启的 Embedding 类型模型实例，请先在「模型实例」中配置')
    }
  } catch {
    embeddingInstances.value = []
  }
}

async function confirmRebuild() {
  if (!rebuildModelInstanceId.value) {
    ElMessage.warning('请选择向量模型实例')
    return
  }
  rebuildStarting.value = true
  try {
    const { data } = await startToolRetrievalRebuild({
      embeddingModelInstanceId: rebuildModelInstanceId.value,
    })
    ElMessage.success('已提交重建任务')
    rebuildDialogVisible.value = false
    startPolling(data.taskId)
  } catch {
    /* 错误提示由 request 拦截器处理 */
  } finally {
    rebuildStarting.value = false
  }
}

function startPolling(taskId: string) {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const { data } = await getToolRetrievalRebuildStatus(taskId)
      if (!data) return
      task.value = data
      if (data.stage === 'DONE' || data.stage === 'FAILED') {
        stopPolling()
      }
    } catch {
      /* silent */
    }
  }, 1500)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function loadLatest() {
  try {
    const { data } = await getToolRetrievalRebuildStatus()
    if (data) {
      task.value = data
      if (data.stage === 'RUNNING' || data.stage === 'QUEUED') {
        startPolling(data.taskId)
      }
    }
  } catch {
    /* silent */
  }
}

function stageTag(stage: string): 'success' | 'warning' | 'info' | 'danger' | '' {
  switch (stage) {
    case 'DONE':
      return 'success'
    case 'FAILED':
      return 'danger'
    case 'RUNNING':
      return 'warning'
    default:
      return 'info'
  }
}

function scoreTag(score: number): 'success' | 'warning' | 'info' | 'danger' | '' {
  if (score >= 0.7) return 'success'
  if (score >= 0.5) return 'warning'
  return 'info'
}

onMounted(loadLatest)
onUnmounted(stopPolling)
</script>

<style scoped>
.section-card {
  margin-top: 12px;
}
.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}
.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.text-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.rebuild-dialog-hint {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}
</style>
