<template>
  <div class="workflow-list">
    <header class="page-header">
      <div>
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageDescription }}</p>
      </div>
      <div class="page-actions">
        <el-button
          v-if="projectScoped"
          @click="backToProject"
        >
          返回项目
        </el-button>
        <el-button
          v-if="projectScoped"
          @click="openGlobalWorkflows"
        >
          全部 Workflow
        </el-button>
        <el-button type="primary" @click="openCreateDialog">新建 Workflow</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadWorkflows">刷新</el-button>
      </div>
    </header>

    <el-alert
      v-if="projectScoped"
      class="context-alert"
      type="info"
      :closable="false"
      show-icon
    >
      <template #title>
        项目范围：{{ filters.projectCode }}
      </template>
    </el-alert>

    <el-form class="filters" :inline="true">
      <el-form-item label="项目">
        <el-input v-model="filters.projectCode" clearable placeholder="projectCode" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="filters.workflowType" clearable placeholder="全部" style="width: 150px">
          <el-option
            v-for="item in WORKFLOW_TYPE_SELECT_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部" style="width: 140px">
          <el-option
            v-for="item in WORKFLOW_STATUS_SELECT_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadWorkflows">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="workflows" v-loading="loading" stripe>
      <el-table-column prop="name" label="名称" min-width="220">
        <template #default="{ row }">
          <el-button text type="primary" @click="openStudio(row.id)">{{ row.name }}</el-button>
          <div class="muted">{{ row.keySlug }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="projectCode" label="项目" min-width="140" />
      <el-table-column prop="workflowType" label="类型" min-width="130">
        <template #default="{ row }">
          {{ formatWorkflowTypeLabel(row.workflowType) }}
        </template>
      </el-table-column>
      <el-table-column prop="runtimeType" label="运行时" min-width="140">
        <template #default="{ row }">
          {{ formatRuntimeTypeLabel(row.runtimeType) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="workflowStatusTagType(row.status)">
            {{ formatWorkflowStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="managedBy" label="管理来源" min-width="140">
        <template #default="{ row }">
          {{ formatWorkflowManagedByLabel(row.managedBy) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openStudio(row.id)">编排</el-button>
          <el-button size="small" @click="openVersions(row.id)">版本</el-button>
          <el-button
            v-if="canDeleteWorkflow(row)"
            size="small"
            type="danger"
            plain
            :loading="deletingId === row.id"
            @click="confirmDeleteWorkflow(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="createDialogVisible"
      title="新建 Workflow"
      width="560px"
      destroy-on-close
    >
      <el-form label-position="top" class="create-form">
        <el-form-item label="名称" required>
          <el-input
            v-model="createForm.name"
            maxlength="80"
            show-word-limit
            placeholder="订单页助手"
            @blur="fillKeySlugFromName"
          />
        </el-form-item>
        <el-form-item label="Key" required>
          <el-input
            v-model="createForm.keySlug"
            maxlength="128"
            placeholder="orders-page-assistant"
          />
        </el-form-item>
        <el-form-item label="项目">
          <el-input
            v-model="createForm.projectCode"
            clearable
            :disabled="projectScoped"
            placeholder="projectCode"
          />
        </el-form-item>
        <div class="create-form-grid">
          <el-form-item label="类型">
            <el-select v-model="createForm.workflowType">
              <el-option
                v-for="item in WORKFLOW_TYPE_SELECT_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="运行时">
            <el-select v-model="createForm.runtimeType">
              <el-option label="LangGraph4j 工作流" value="LANGGRAPH4J" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            maxlength="240"
            show-word-limit
            placeholder="可选"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreateWorkflow">
          创建并进入编排
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { createWorkflow, deleteWorkflow, listWorkflows } from '@/api/workflow'
import type { WorkflowDefinition } from '@/types/workflow'
import { formatRuntimeTypeLabel } from '@/utils/registryLabels'
import {
  WORKFLOW_STATUS_SELECT_OPTIONS,
  WORKFLOW_TYPE_SELECT_OPTIONS,
  formatWorkflowManagedByLabel,
  formatWorkflowStatusLabel,
  formatWorkflowTypeLabel,
  workflowStatusTagType,
} from '@/utils/workflowLabels'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const creating = ref(false)
const deletingId = ref('')
const createDialogVisible = ref(false)
const workflows = ref<WorkflowDefinition[]>([])
const filters = reactive({
  projectCode: String(route.query.projectCode || ''),
  workflowType: '',
  status: '',
})
const createForm = reactive({
  name: '',
  keySlug: '',
  projectCode: '',
  workflowType: 'CHAT',
  runtimeType: 'LANGGRAPH4J',
  description: '',
})

const routeProjectCode = computed(() => String(route.query.projectCode || '').trim())
const projectScoped = computed(() => !!routeProjectCode.value)
const pageTitle = computed(() => (projectScoped.value ? '项目 Workflow' : 'Workflow 编排'))
const pageDescription = computed(() =>
  projectScoped.value
    ? '当前项目下的可执行 Workflow 资产。'
    : '面向页面动作、SDK 图和对话流的可执行图资产。',
)

onMounted(loadWorkflows)

watch(
  () => route.query.projectCode,
  (value) => {
    filters.projectCode = String(value || '')
    void loadWorkflows()
  },
)

async function loadWorkflows() {
  loading.value = true
  try {
    const { data } = await listWorkflows({
      projectCode: filters.projectCode || undefined,
      workflowType: filters.workflowType || undefined,
      status: filters.status || undefined,
    })
    workflows.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}

function openStudio(id: string) {
  router.push(`/workflows/${id}/studio`)
}

function openVersions(id: string) {
  router.push(`/workflows/${id}/versions`)
}

function canDeleteWorkflow(row: WorkflowDefinition) {
  return row.deletable === true
}

async function confirmDeleteWorkflow(row: WorkflowDefinition) {
  try {
    await ElMessageBox.confirm(
      `确认删除草稿 Workflow「${row.name}」吗？删除后不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  deletingId.value = row.id
  try {
    await deleteWorkflow(row.id)
    ElMessage.success('Workflow 已删除')
    await loadWorkflows()
  } catch {
    // 错误提示由 request 拦截器处理
  } finally {
    deletingId.value = ''
  }
}

function backToProject() {
  if (!routeProjectCode.value) return
  router.push({ name: 'RegistryProjectDetail', params: { projectCode: routeProjectCode.value } })
}

function openGlobalWorkflows() {
  filters.projectCode = ''
  router.push({ name: 'WorkflowList' })
}

function openCreateDialog() {
  createForm.name = ''
  createForm.keySlug = ''
  createForm.projectCode = routeProjectCode.value || filters.projectCode || ''
  createForm.workflowType = 'CHAT'
  createForm.runtimeType = 'LANGGRAPH4J'
  createForm.description = ''
  createDialogVisible.value = true
}

function fillKeySlugFromName() {
  if (createForm.keySlug.trim() || !createForm.name.trim()) return
  createForm.keySlug = slugFromName(createForm.name)
}

function slugFromName(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_-]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 128)
}

function validateCreateForm() {
  if (!createForm.name.trim()) {
    ElMessage.warning('请输入 Workflow 名称')
    return false
  }
  fillKeySlugFromName()
  if (!/^[A-Za-z0-9][A-Za-z0-9_-]{1,127}$/.test(createForm.keySlug.trim())) {
    ElMessage.warning('Key 需为 2-128 位，仅支持字母、数字、下划线或连字符')
    return false
  }
  return true
}

async function submitCreateWorkflow() {
  if (!validateCreateForm()) return
  creating.value = true
  try {
    const { data } = await createWorkflow({
      name: createForm.name.trim(),
      keySlug: createForm.keySlug.trim(),
      projectCode: createForm.projectCode.trim() || null,
      workflowType: createForm.workflowType,
      runtimeType: createForm.runtimeType,
      description: createForm.description.trim() || null,
      status: 'DRAFT',
      managedBy: 'MANUAL',
      graphSpecJson: '{"nodes":[],"edges":[]}',
      canvasJson: '{"version":2,"nodes":[],"edges":[]}',
    })
    createDialogVisible.value = false
    ElMessage.success('Workflow 已创建')
    router.push(`/workflows/${data.id}/studio`)
  } catch (err) {
    ElMessage.error((err as Error).message || '创建 Workflow 失败')
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.workflow-list {
  min-height: calc(100vh - 56px);
  padding: 20px;
  background: var(--el-bg-color-page);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.create-form {
  display: grid;
  gap: 2px;
}

.create-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.page-header h1 {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0;
}

.page-header p,
.muted {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.filters {
  margin-bottom: 12px;
}

.context-alert {
  margin-bottom: 12px;
}
</style>
