<template>
  <div class="workflow-list-page" :class="{ 'is-dark': theme === 'dark' }">
    <div class="page-hero">
      <div>
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageDescription }}</p>
      </div>
      <div class="hero-actions">
        <el-button v-if="projectScoped" @click="backToProject">返回项目</el-button>
        <el-button v-if="projectScoped" @click="openGlobalWorkflows">全部 Workflow</el-button>
        <el-button type="primary" size="large" class="primary-action" :icon="Plus" @click="openCreateDialog">
          新建 Workflow
        </el-button>
      </div>
    </div>

    <div class="metric-grid">
      <div v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-icon" :class="metric.tone" aria-hidden="true">
          <el-icon>
            <component :is="metric.icon" />
          </el-icon>
        </div>
        <div>
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
          <small>
            {{ metric.caption }}
            <em v-if="metric.delta">{{ metric.delta }}</em>
          </small>
        </div>
      </div>
    </div>

    <el-card class="workflow-card" shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          class="search-input"
          clearable
          :prefix-icon="Search"
          placeholder="搜索 Workflow 名称、Key、描述、项目"
        />
        <el-input
          v-model="filters.projectCode"
          clearable
          :disabled="projectScoped"
          placeholder="项目编码"
          @change="searchWorkflows"
          @clear="searchWorkflows"
          @keyup.enter="searchWorkflows"
        />
        <el-select v-model="filters.workflowType" clearable placeholder="Workflow 类型" @change="searchWorkflows">
          <el-option
            v-for="item in WORKFLOW_TYPE_SELECT_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="发布状态" @change="searchWorkflows">
          <el-option
            v-for="item in WORKFLOW_STATUS_SELECT_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button @click="resetFilters">重置</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadWorkflows" />
      </div>

      <div v-if="projectScoped" class="context-filter">
        当前仅展示项目 {{ filters.projectCode }} 下的 Workflow。
        <el-button link type="primary" @click="openGlobalWorkflows">查看全部 Workflow</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="pagedWorkflows"
        row-key="id"
        class="workflow-table"
        :max-height="workflowTableMaxHeight"
      >
        <el-table-column label="Workflow 名称" min-width="280">
          <template #default="{ row }">
            <button type="button" class="workflow-name-cell workflow-name-cell-btn" @click="openStudio(row.id)">
              <div class="workflow-avatar" :class="workflowAvatarClass(row.workflowType)">
                {{ workflowInitial(row.name) }}
              </div>
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.keySlug }}</span>
              </div>
            </button>
          </template>
        </el-table-column>
        <el-table-column label="描述" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="muted">{{ row.description || '可执行图资产' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="项目" min-width="120">
          <template #default="{ row }">
            <span class="muted">{{ row.projectCode || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="130">
          <template #default="{ row }">
            <el-tag :type="workflowTypeTagType(row.workflowType)" effect="light">
              {{ formatWorkflowTypeLabel(row.workflowType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="运行时" width="150">
          <template #default="{ row }">
            <span class="muted">{{ formatRuntimeTypeLabel(row.runtimeType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span class="status-pill" :class="workflowStatusClass(row.status)">
              <i />
              {{ formatWorkflowStatusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="管理来源" width="140">
          <template #default="{ row }">
            <span class="muted">{{ formatWorkflowManagedByLabel(row.managedBy) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
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

      <div v-if="!loading && filteredWorkflows.length === 0" class="empty-state">
        <div class="empty-main">
          <div class="empty-illustration" aria-hidden="true">
            <img class="empty-illustration-img" src="/智能化.svg" alt="" />
          </div>
          <div class="empty-body">
            <div class="empty-copy">
              <h3>还没有匹配的 Workflow</h3>
              <p>可以调整项目、类型或状态筛选，也可以新建一个可执行图资产。</p>
            </div>
            <div class="empty-actions">
              <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建 Workflow</el-button>
              <el-button @click="resetFilters">重置筛选</el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="filteredWorkflows.length > 0" class="table-footer">
        <span>共 {{ filteredWorkflows.length }} 条</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          background
          layout="prev, pager, next, sizes"
          :page-sizes="[5, 10, 20, 50]"
          :total="filteredWorkflows.length"
        />
      </div>
    </el-card>

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
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  Connection,
  Document,
  Operation,
  Plus,
  Refresh,
  Search,
} from '@element-plus/icons-vue'
import { createWorkflow, deleteWorkflow, listWorkflows } from '@/api/workflow'
import type { WorkflowDefinition } from '@/types/workflow'
import { useTheme } from '@/composables/useTheme'
import { formatRuntimeTypeLabel } from '@/utils/registryLabels'
import {
  WORKFLOW_STATUS_SELECT_OPTIONS,
  WORKFLOW_TYPE_SELECT_OPTIONS,
  formatWorkflowManagedByLabel,
  formatWorkflowStatusLabel,
  formatWorkflowTypeLabel,
} from '@/utils/workflowLabels'

const route = useRoute()
const router = useRouter()
const { theme } = useTheme()

const loading = ref(false)
const creating = ref(false)
const deletingId = ref('')
const createDialogVisible = ref(false)
const workflows = ref<WorkflowDefinition[]>([])
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const workflowTableMaxHeight = ref(480)
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

let mainContentScrollEl: HTMLElement | null = null
let tableHeightRaf = 0

const routeProjectCode = computed(() => String(route.query.projectCode || '').trim())
const projectScoped = computed(() => !!routeProjectCode.value)
const pageTitle = computed(() => (projectScoped.value ? '项目 Workflow' : 'Workflow 编排'))
const pageDescription = computed(() =>
  projectScoped.value
    ? '当前项目下的可执行 Workflow 资产。'
    : '面向页面动作、SDK 图和对话流的可执行图资产。',
)

const filteredWorkflows = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return workflows.value
  return workflows.value.filter((workflow) => {
    return [
      workflow.name,
      workflow.keySlug,
      workflow.description,
      workflow.projectCode,
      formatWorkflowTypeLabel(workflow.workflowType),
      formatWorkflowStatusLabel(workflow.status),
      formatWorkflowManagedByLabel(workflow.managedBy),
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(text))
  })
})

const pagedWorkflows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredWorkflows.value.slice(start, start + pageSize.value)
})

const metrics = computed(() => {
  const total = workflows.value.length
  const activeCount = workflows.value.filter((workflow) => workflowStatusKey(workflow.status) === 'ACTIVE').length
  const draftCount = workflows.value.filter((workflow) => workflowStatusKey(workflow.status) === 'DRAFT').length
  const pageActionCount = workflows.value.filter((workflow) => workflowTypeKey(workflow.workflowType) === 'PAGE_ACTION').length
  const sdkGraphCount = workflows.value.filter((workflow) => workflowTypeKey(workflow.workflowType) === 'SDK_GRAPH').length
  const projectCount = new Set(workflows.value.map((workflow) => workflow.projectCode).filter(Boolean)).size
  const manualCount = workflows.value.filter((workflow) => workflowManagedByKey(workflow.managedBy) === 'MANUAL').length
  return [
    {
      label: 'Workflow 总数',
      value: total,
      caption: '可执行图资产',
      delta: projectCount ? `${projectCount} 个项目` : '',
      icon: Operation,
      tone: 'purple',
    },
    {
      label: '已发布数量',
      value: activeCount,
      caption: '可进入运行时',
      delta: draftCount ? `${draftCount} 个草稿` : '',
      icon: CircleCheck,
      tone: 'green',
    },
    {
      label: '页面动作 / SDK 图',
      value: `${pageActionCount} / ${sdkGraphCount}`,
      caption: '业务页面与能力图',
      delta: pageActionCount || sdkGraphCount ? 'GraphSpec 管理' : '',
      icon: Connection,
      tone: 'blue',
    },
    {
      label: '手动编排',
      value: manualCount,
      caption: '人工创建与维护',
      delta: total ? `${Math.max(total - manualCount, 0)} 个托管` : '',
      icon: Document,
      tone: 'orange',
    },
  ]
})

onMounted(() => {
  loadWorkflows().finally(() => {
    nextTick(scheduleUpdateWorkflowTableMaxHeight)
  })
  mainContentScrollEl = document.querySelector('.main-layout .main-content') as HTMLElement | null
  mainContentScrollEl?.addEventListener('scroll', scheduleUpdateWorkflowTableMaxHeight, { passive: true })
  window.addEventListener('resize', scheduleUpdateWorkflowTableMaxHeight)
})

onUnmounted(() => {
  mainContentScrollEl?.removeEventListener('scroll', scheduleUpdateWorkflowTableMaxHeight)
  window.removeEventListener('resize', scheduleUpdateWorkflowTableMaxHeight)
})

watch(
  () => route.query.projectCode,
  (value) => {
    filters.projectCode = String(value || '')
    currentPage.value = 1
    void loadWorkflows()
  },
)

watch([keyword, pageSize], () => {
  currentPage.value = 1
})

watch(
  () => filteredWorkflows.value.length,
  () => {
    const maxPage = Math.max(1, Math.ceil(filteredWorkflows.value.length / pageSize.value))
    if (currentPage.value > maxPage) currentPage.value = maxPage
    nextTick(scheduleUpdateWorkflowTableMaxHeight)
  },
)

function scheduleUpdateWorkflowTableMaxHeight() {
  if (typeof window === 'undefined') return
  if (tableHeightRaf) return
  tableHeightRaf = requestAnimationFrame(() => {
    tableHeightRaf = 0
    updateWorkflowTableMaxHeight()
  })
}

function updateWorkflowTableMaxHeight() {
  if (typeof window === 'undefined') return
  const root = document.querySelector('.workflow-list-page')
  const cardEl = root?.querySelector('.workflow-card') as HTMLElement | undefined
  const tableEl = root?.querySelector('.workflow-table') as HTMLElement | undefined
  const footerEl = root?.querySelector('.table-footer') as HTMLElement | undefined
  if (!tableEl) {
    workflowTableMaxHeight.value = Math.max(280, window.innerHeight - 420)
    return
  }
  const tableTop = tableEl.getBoundingClientRect().top
  const viewportPad = 16
  let bottomLimit = window.innerHeight - viewportPad
  if (cardEl) {
    const cardBottom = cardEl.getBoundingClientRect().bottom
    if (cardBottom > tableTop && cardBottom <= window.innerHeight) {
      bottomLimit = Math.min(bottomLimit, cardBottom - 12)
    }
  }
  if (footerEl && footerEl.offsetParent !== null) {
    const footerTop = footerEl.getBoundingClientRect().top
    if (footerTop > tableTop && footerTop < bottomLimit) {
      bottomLimit = footerTop - 12
    }
  }
  const h = bottomLimit - tableTop
  workflowTableMaxHeight.value = Math.max(260, Math.floor(h))
}

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
    nextTick(scheduleUpdateWorkflowTableMaxHeight)
  }
}

function searchWorkflows() {
  currentPage.value = 1
  void loadWorkflows()
}

function resetFilters() {
  keyword.value = ''
  filters.projectCode = routeProjectCode.value || ''
  filters.workflowType = ''
  filters.status = ''
  currentPage.value = 1
  void loadWorkflows()
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

function workflowInitial(value?: string | null) {
  const trimmed = (value || 'W').trim()
  return trimmed.slice(0, 1).toUpperCase()
}

function workflowTypeKey(value?: string | null) {
  return String(value || 'CHAT').toUpperCase()
}

function workflowStatusKey(value?: string | null) {
  return String(value || 'DRAFT').toUpperCase()
}

function workflowManagedByKey(value?: string | null) {
  return String(value || 'MANUAL').toUpperCase()
}

function workflowAvatarClass(type?: string | null) {
  const key = workflowTypeKey(type)
  if (key === 'PAGE_ACTION') return 'page-action'
  if (key === 'SDK_GRAPH') return 'sdk-graph'
  return 'chat'
}

function workflowTypeTagType(type?: string | null) {
  const key = workflowTypeKey(type)
  if (key === 'PAGE_ACTION') return 'success'
  if (key === 'SDK_GRAPH') return 'warning'
  return 'info'
}

function workflowStatusClass(status?: string | null) {
  const key = workflowStatusKey(status)
  if (key === 'ACTIVE') return 'status-active'
  if (key === 'ARCHIVED') return 'status-archived'
  return 'status-draft'
}
</script>

<style scoped lang="scss">
.workflow-list-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  height: calc(100vh - 56px);
  padding: 24px 28px 24px;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  background: var(--brand-page-bg);
  background-size: 28px 28px, 28px 28px, auto, auto, auto, auto;
}

.page-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  width: 100%;
  min-width: 0;
  margin: 0;
  flex-shrink: 0;

  h1 {
    margin: 0 0 8px;
    color: #111827;
    font-size: 26px;
    font-weight: 800;
    line-height: 1.2;
  }

  p {
    margin: 0;
    color: #667085;
    font-size: 14px;
  }
}

.hero-actions,
.empty-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.primary-action {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: var(--brand-hover);
  --el-button-hover-border-color: var(--brand-hover);
  min-width: 142px;
  border-radius: 8px;
  box-shadow: 0 10px 20px rgb(var(--brand-primary-rgb) / 0.22);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  width: 100%;
  min-width: 0;
  margin: 0;
  flex-shrink: 0;
}

.metric-card {
  display: flex;
  gap: 14px;
  align-items: center;
  min-height: 92px;
  padding: 18px 20px;
  border: 1px solid #eaecf5;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(17, 24, 39, 0.035);

  span,
  small {
    display: block;
    color: #667085;
  }

  span {
    font-size: 12px;
  }

  strong {
    display: block;
    margin: 5px 0 4px;
    color: #101828;
    font-size: 25px;
    font-weight: 800;
    line-height: 1;
  }

  small {
    font-size: 12px;

    em {
      margin-left: 6px;
      color: #039855;
      font-style: normal;
      font-weight: 700;
    }
  }
}

.metric-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border: 1px solid transparent;
  border-radius: 14px;
  font-size: 22px;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    0 10px 22px rgba(16, 24, 40, 0.06);

  .el-icon {
    filter: drop-shadow(0 1px 0 rgba(255, 255, 255, 0.7));
  }

  &.purple {
    color: var(--brand-active);
    border-color: rgb(var(--brand-hover-rgb) / 0.22);
    background: linear-gradient(135deg, rgb(var(--brand-selected-rgb) / 0.7), rgba(255, 255, 255, 0.86));
  }

  &.blue {
    color: var(--brand-primary);
    border-color: rgb(var(--brand-primary-rgb) / 0.2);
    background: linear-gradient(135deg, rgb(var(--brand-selected-rgb) / 0.72), rgba(255, 255, 255, 0.88));
  }

  &.green {
    color: #16a34a;
    border-color: #cdefd8;
    background: linear-gradient(135deg, #ecfdf3, #dcfce7);
  }

  &.orange {
    color: #ea580c;
    border-color: #fedfc2;
    background: linear-gradient(135deg, #fff7ed, #ffedd5);
  }
}

.workflow-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  min-height: 0;
  margin: 0;
  border: 1px solid #eaecf5;
  border-radius: 12px;
  box-shadow: 0 14px 34px rgba(17, 24, 39, 0.045);

  :deep(.el-card__body) {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-height: 0;
    padding: 0;
  }
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 2fr) minmax(150px, 1fr) repeat(2, minmax(150px, 1fr)) auto auto;
  align-items: center;
  gap: 10px;
  padding: 14px 22px 16px;
  border-bottom: 1px solid #eef1f7;
  background: #fff;
  flex-shrink: 0;

  .search-input {
    width: 100%;
    min-width: 0;
  }

  .el-select,
  .el-input {
    width: 100%;
    min-width: 0;
  }

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    min-height: 34px;
    border-radius: 7px;
    box-shadow: 0 0 0 1px #d9deea inset;
  }

  :deep(.el-button) {
    min-height: 34px;
    border-radius: 7px;
  }
}

.context-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 22px;
  color: var(--brand-primary);
  background: rgb(var(--brand-selected-rgb) / 0.52);
  border-bottom: 1px solid rgb(var(--brand-selected-rgb) / 0.72);
  font-size: 13px;
  flex-shrink: 0;
}

.workflow-table {
  width: 100%;
  flex: 1;
  min-height: 0;

  :deep(th.el-table__cell) {
    height: 44px;
    color: var(--brand-active);
    background: rgb(var(--brand-selected-rgb) / 0.34);
    font-size: 12px;
    font-weight: 700;
  }

  :deep(td.el-table__cell) {
    height: 58px;
    color: #344054;
    border-bottom-color: #f0f2f7;
  }

  :deep(.el-table__row:hover > td.el-table__cell) {
    background: rgb(var(--brand-selected-rgb) / 0.22);
  }

  :deep(.el-table-fixed-column--right),
  :deep(.el-table-fixed-column--left) {
    background-color: #fff !important;
  }

  :deep(th.el-table-fixed-column--right),
  :deep(th.el-table-fixed-column--left) {
    background-color: rgb(var(--brand-selected-rgb) / 0.34) !important;
  }

  :deep(td.el-table-fixed-column--right),
  :deep(td.el-table-fixed-column--left) {
    background-color: #fff !important;
  }

  :deep(.el-table__row:hover > td.el-table-fixed-column--right),
  :deep(.el-table__row:hover > td.el-table-fixed-column--left),
  :deep(.el-table__body tr.hover-row > td.el-table-fixed-column--right),
  :deep(.el-table__body tr.hover-row > td.el-table-fixed-column--left),
  :deep(.el-table__body tr.current-row > td.el-table-fixed-column--right),
  :deep(.el-table__body tr.current-row > td.el-table-fixed-column--left) {
    background-color: rgb(var(--brand-selected-rgb) / 0.22) !important;
  }

  :deep(.el-table__fixed-right-patch) {
    background-color: #fff !important;
  }
}

.workflow-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;

  strong,
  span {
    display: block;
  }

  strong {
    color: #101828;
    font-weight: 700;
  }

  span {
    margin-top: 4px;
    color: #667085;
    font-size: 12px;
  }
}

.workflow-name-cell-btn {
  border: none;
  background: transparent;
  padding: 0;
  margin: 0;
  font: inherit;
  text-align: left;
  cursor: pointer;
  width: 100%;
  min-width: 0;

  &:hover strong {
    color: var(--brand-primary);
  }

  &:focus-visible {
    outline: 2px solid rgb(var(--brand-primary-rgb) / 0.35);
    outline-offset: 2px;
    border-radius: 10px;
  }
}

.workflow-avatar {
  display: grid;
  place-items: center;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  color: #fff;
  font-weight: 800;

  &.chat {
    background: linear-gradient(135deg, var(--brand-hover), var(--brand-active));
  }

  &.sdk-graph {
    background: linear-gradient(135deg, #22c55e, #14b8a6);
  }

  &.page-action {
    background: linear-gradient(135deg, #f97316, #f59e0b);
  }
}

.muted {
  color: #667085;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;

  i {
    width: 7px;
    height: 7px;
    border-radius: 999px;
  }
}

.status-active {
  color: #16a34a;

  i {
    background: #22c55e;
  }
}

.status-draft {
  color: #d97706;

  i {
    background: #f59e0b;
  }
}

.status-archived {
  color: #64748b;

  i {
    background: #94a3b8;
  }
}

.empty-state {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: stretch;
  gap: 0;
  margin: 20px;
  padding: 28px 32px;
  border: 1px dashed rgb(var(--brand-primary-rgb) / 0.28);
  border-radius: 18px;
  background: linear-gradient(135deg, rgb(var(--brand-primary-rgb) / 0.06), rgb(var(--brand-hover-rgb) / 0.03));
  min-height: 0;

  h3 {
    margin: 0 0 8px;
    color: #101828;
    font-size: 18px;
    font-weight: 700;
  }

  p {
    margin: 0;
    color: #667085;
    line-height: 1.55;
  }
}

.empty-main {
  display: flex;
  align-items: stretch;
  gap: 28px;
  min-width: 0;
}

.empty-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  min-width: 0;
}

.empty-copy {
  flex: 0 0 auto;
  min-width: 0;
}

.empty-illustration {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 240px;
  width: 240px;
  min-height: 140px;
  align-self: stretch;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(135deg, var(--brand-selected-bg), rgba(255, 255, 255, 0.88));
}

.empty-illustration-img {
  display: block;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 22px 16px;
  border-top: 1px solid #eef1f7;
  color: #667085;
  font-size: 13px;
  flex-shrink: 0;
  margin-top: auto;

  :deep(.el-pagination.is-background .el-pager li.is-active) {
    background-color: var(--brand-primary);
  }
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

.workflow-list-page.is-dark {
  background:
    radial-gradient(circle at 14% 8%, rgb(var(--brand-primary-rgb) / 0.18), transparent 28%),
    linear-gradient(180deg, #0a0a0f 0%, #10101a 48%, #0a0a0f 100%);

  .page-hero {
    h1 {
      color: #e2e8f0;
    }

    p {
      color: #94a3b8;
    }
  }

  .primary-action {
    box-shadow: 0 10px 22px rgb(var(--brand-primary-rgb) / 0.28);
  }

  .metric-card,
  .workflow-card {
    border-color: rgba(255, 255, 255, 0.07);
    background: rgba(255, 255, 255, 0.035);
    box-shadow: 0 14px 34px rgba(0, 0, 0, 0.22);
    backdrop-filter: blur(12px);
  }

  .metric-card {
    span,
    small {
      color: #94a3b8;
    }

    strong {
      color: #e2e8f0;
    }

    small em {
      color: #22d3ee;
    }
  }

  .metric-icon {
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.08),
      0 12px 26px rgba(0, 0, 0, 0.18);

    .el-icon {
      filter: drop-shadow(0 0 10px currentColor);
    }

    &.purple {
      color: var(--brand-selected-bg);
      border-color: rgb(var(--brand-hover-rgb) / 0.22);
      background: rgb(var(--brand-hover-rgb) / 0.18);
    }

    &.blue {
      color: #93c5fd;
      border-color: rgba(147, 197, 253, 0.18);
      background: rgba(37, 99, 235, 0.18);
    }

    &.green {
      color: #86efac;
      border-color: rgba(134, 239, 172, 0.16);
      background: rgba(22, 163, 74, 0.16);
    }

    &.orange {
      color: #fdba74;
      border-color: rgba(253, 186, 116, 0.18);
      background: rgba(234, 88, 12, 0.16);
    }
  }

  .workflow-card {
    --el-card-bg-color: transparent;

    :deep(.el-card__body) {
      background: transparent;
    }
  }

  .toolbar {
    background: rgba(255, 255, 255, 0.02);
    border-bottom-color: rgba(255, 255, 255, 0.06);

    :deep(.el-input__wrapper),
    :deep(.el-select__wrapper) {
      background: rgba(255, 255, 255, 0.035);
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08) inset;
    }

    :deep(.el-input__inner),
    :deep(.el-select__placeholder),
    :deep(.el-select__selected-item) {
      color: #cbd5e1;
    }

    :deep(.el-input__inner::placeholder) {
      color: #64748b;
    }

    :deep(.el-input__prefix),
    :deep(.el-input__suffix),
    :deep(.el-select__suffix),
    :deep(.el-select__caret) {
      color: #94a3b8;
    }

    :deep(.el-button--default) {
      color: #cbd5e1;
      background: rgba(255, 255, 255, 0.04);
      border-color: rgba(255, 255, 255, 0.1);

      &:hover {
        color: #e2e8f0;
        background: rgb(var(--brand-primary-rgb) / 0.12);
        border-color: rgb(var(--brand-primary-rgb) / 0.35);
      }
    }
  }

  .context-filter {
    color: var(--brand-selected-bg);
    background: rgb(var(--brand-primary-rgb) / 0.12);
    border-bottom-color: rgb(var(--brand-primary-rgb) / 0.22);
  }

  .workflow-table {
    --el-table-bg-color: transparent;
    --el-table-tr-bg-color: transparent;
    --el-table-header-bg-color: rgba(255, 255, 255, 0.045);
    --el-table-row-hover-bg-color: rgb(var(--brand-primary-rgb) / 0.08);
    --el-table-border-color: rgba(255, 255, 255, 0.06);
    --el-table-text-color: #cbd5e1;
    --el-table-header-text-color: #94a3b8;
    background: transparent;

    :deep(.el-table__inner-wrapper::before),
    :deep(.el-table__border-left-patch),
    :deep(.el-table__border-bottom-patch) {
      background-color: rgba(255, 255, 255, 0.06);
    }

    :deep(th.el-table__cell) {
      color: #94a3b8;
      background: rgba(255, 255, 255, 0.045) !important;
    }

    :deep(td.el-table__cell) {
      color: #cbd5e1;
      background: rgba(255, 255, 255, 0.015) !important;
      border-bottom-color: rgba(255, 255, 255, 0.06);
    }

    :deep(.el-table__row:hover > td.el-table__cell),
    :deep(.el-table__body tr.hover-row > td.el-table__cell) {
      background: rgb(var(--brand-primary-rgb) / 0.08) !important;
    }

    :deep(.el-table__inner-wrapper),
    :deep(.el-table__header-wrapper),
    :deep(.el-table__body-wrapper) {
      background: transparent;
    }

    :deep(.el-table-fixed-column--right),
    :deep(.el-table-fixed-column--left) {
      background-color: var(--bg-secondary) !important;
    }

    :deep(th.el-table-fixed-column--right),
    :deep(th.el-table-fixed-column--left) {
      background-color: #1c1c2a !important;
    }

    :deep(td.el-table-fixed-column--right),
    :deep(td.el-table-fixed-column--left) {
      background-color: var(--bg-secondary) !important;
    }

    :deep(.el-table__row:hover > td.el-table-fixed-column--right),
    :deep(.el-table__row:hover > td.el-table-fixed-column--left),
    :deep(.el-table__body tr.hover-row > td.el-table-fixed-column--right),
    :deep(.el-table__body tr.hover-row > td.el-table-fixed-column--left),
    :deep(.el-table__body tr.current-row > td.el-table-fixed-column--right),
    :deep(.el-table__body tr.current-row > td.el-table-fixed-column--left) {
      background-color: #252538 !important;
    }

    :deep(.el-table__fixed-right-patch) {
      background-color: var(--bg-secondary) !important;
    }

    :deep(.el-tag.el-tag--light) {
      border-width: 1px;
      border-style: solid;
    }
  }

  .workflow-name-cell {
    strong {
      color: #e2e8f0;
    }

    span {
      color: #64748b;
    }
  }

  .workflow-name-cell-btn:hover strong {
    color: var(--brand-disabled);
  }

  .muted {
    color: #94a3b8;
  }

  .empty-illustration {
    background: linear-gradient(135deg, rgb(var(--brand-primary-rgb) / 0.22), rgb(var(--brand-hover-rgb) / 0.08));
  }

  .empty-state {
    border-color: rgb(var(--brand-primary-rgb) / 0.22);
    background: linear-gradient(135deg, rgb(var(--brand-primary-rgb) / 0.1), rgb(var(--brand-hover-rgb) / 0.04));

    h3 {
      color: #e2e8f0;
    }

    p {
      color: #94a3b8;
    }
  }

  .table-footer {
    color: #94a3b8;
    border-top-color: rgba(255, 255, 255, 0.06);

    :deep(.el-pagination.is-background .btn-prev),
    :deep(.el-pagination.is-background .btn-next),
    :deep(.el-pagination.is-background .el-pager li) {
      color: #94a3b8;
      background: rgba(255, 255, 255, 0.035);
    }

    :deep(.el-pagination.is-background .btn-prev:hover),
    :deep(.el-pagination.is-background .btn-next:hover),
    :deep(.el-pagination.is-background .el-pager li:hover) {
      color: var(--brand-selected-bg);
      background: rgb(var(--brand-primary-rgb) / 0.12);
    }

    :deep(.el-pagination.is-background .el-pager li.is-active) {
      color: #fff;
      background: linear-gradient(135deg, var(--brand-primary), var(--brand-hover));
    }

    :deep(.el-pagination.is-background .btn-prev.is-disabled),
    :deep(.el-pagination.is-background .btn-next.is-disabled),
    :deep(.el-pagination.is-background .el-pager li.is-disabled) {
      color: rgba(148, 163, 184, 0.35);
      background: rgba(255, 255, 255, 0.02);
    }
  }
}

:global(.main-layout.registry-shell .main-content:has(.workflow-list-page)) {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .page-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar {
    grid-template-columns: minmax(240px, 1fr) repeat(2, minmax(150px, 1fr));
  }

  .empty-main {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .empty-body {
    align-items: center;
    width: 100%;
  }

  .empty-actions {
    justify-content: center;
  }

  .empty-illustration {
    align-self: center;
    flex: 0 0 auto;
    width: min(100%, 240px);
    height: 200px;
    min-height: 0;
  }
}

@media (max-width: 760px) {
  .workflow-list-page {
    padding: 18px 14px 24px;
  }

  .metric-grid,
  .toolbar,
  .create-form-grid {
    grid-template-columns: 1fr;
  }

  .empty-illustration {
    width: min(100%, 240px);
    height: 176px;
  }

  .table-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
