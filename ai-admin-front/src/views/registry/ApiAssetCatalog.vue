<template>
  <div class="api-asset-page">
    <section class="page-head">
      <div>
        <p class="eyebrow">注册中心</p>
        <h1>API 资产目录</h1>
        <span>统一管理 SDK 接入、OpenAPI 扫描和 Controller 扫描发现的业务接口</span>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadAssets">刷新</el-button>
        <el-button type="primary" :icon="FolderOpened" @click="goProjects">项目管理</el-button>
      </div>
    </section>

    <section class="summary-strip">
      <div v-for="item in summaryItems" :key="item.label" class="summary-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.caption }}</small>
      </div>
    </section>

    <section class="catalog-shell">
      <aside class="project-rail">
        <div class="rail-title">项目 / 模块</div>
        <button
          type="button"
          class="rail-project"
          :class="{ active: filters.projectId === null }"
          @click="selectProject(null)"
        >
          <strong>全部项目</strong>
          <span>{{ projectOptions.length }} 个项目</span>
        </button>
        <button
          v-for="project in projectOptions"
          :key="project.id"
          type="button"
          class="rail-project"
          :class="{ active: filters.projectId === project.id }"
          @click="selectProject(project.id)"
        >
          <strong>{{ project.name }}</strong>
          <span>{{ project.projectCode || `ID ${project.id}` }}</span>
        </button>
      </aside>

      <main class="catalog-main">
        <div class="toolbar">
          <el-input
            v-model="filters.keyword"
            clearable
            :prefix-icon="Search"
            placeholder="搜索接口名、路径、描述、参数语义"
            @keyup.enter="reloadFirstPage"
          />
          <el-select v-model="filters.sourceType" clearable placeholder="来源">
            <el-option label="SDK 接入" value="SDK" />
            <el-option label="OpenAPI" value="OPENAPI" />
            <el-option label="Controller 扫描" value="CONTROLLER" />
            <el-option label="离线扫描" value="SCAN" />
          </el-select>
          <el-select v-model="filters.toolLinkStatus" clearable placeholder="Tool 状态">
            <el-option label="已添加为 Tool" value="LINKED" />
            <el-option label="未添加" value="NOT_LINKED" />
            <el-option label="全局 Tool 缺失" value="GLOBAL_MISSING" />
            <el-option label="源接口已移除" value="REMOVED" />
          </el-select>
          <el-select v-model="filters.semanticStatus" clearable placeholder="语义状态">
            <el-option label="语义完整" value="COMPLETE" />
            <el-option label="基础说明" value="BASIC" />
            <el-option label="缺少语义" value="MISSING" />
          </el-select>
          <el-select v-model="quickFilter" clearable placeholder="治理视图" @change="applyQuickFilter">
            <el-option label="待治理" value="todo" />
            <el-option label="Agent 可见" value="agentVisible" />
            <el-option label="敏感风险" value="sensitive" />
            <el-option label="已下线" value="removed" />
          </el-select>
          <el-button @click="resetFilters">重置</el-button>
          <el-button :icon="Refresh" :loading="loading" @click="loadAssets" />
        </div>

        <el-table
          v-loading="loading"
          :data="assets"
          row-key="apiId"
          class="asset-table"
          @row-click="openDetail"
        >
          <el-table-column label="API" min-width="260">
            <template #default="{ row }">
              <div class="api-cell">
                <strong>{{ row.name }}</strong>
                <span>
                  <el-tag size="small" effect="plain">{{ row.httpMethod || '-' }}</el-tag>
                  {{ row.endpointPath || '-' }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="项目 / 模块" min-width="220">
            <template #default="{ row }">
              <div class="stacked">
                <strong>{{ row.projectName || row.projectCode || '-' }}</strong>
                <span>{{ row.moduleName || '未关联模块' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="120">
            <template #default="{ row }">
              <el-tag :type="sourceTagType(row.sourceType)" effect="light">
                {{ sourceLabel(row.sourceType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="语义" width="110">
            <template #default="{ row }">
              <el-tag :type="semanticTagType(row.semanticStatus)" effect="plain">
                {{ semanticLabel(row.semanticStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="参数" width="80" align="center" prop="parameterCount" />
          <el-table-column label="治理状态" min-width="190">
            <template #default="{ row }">
              <div class="governance-tags">
                <el-tag size="small" :type="row.enabled ? 'success' : 'info'" effect="plain">
                  {{ row.enabled ? '已启用' : '未启用' }}
                </el-tag>
                <el-tag size="small" :type="row.agentVisible ? 'success' : 'info'" effect="plain">
                  {{ row.agentVisible ? 'Agent 可见' : 'Agent 不可见' }}
                </el-tag>
                <el-tag v-if="row.sensitiveRisk === 'REVIEW'" size="small" type="danger" effect="plain">
                  敏感风险
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="Tool 关联" min-width="160">
            <template #default="{ row }">
              <el-tag :type="toolStatusTagType(row.toolLinkStatus)" effect="light">
                {{ toolStatusLabel(row.toolLinkStatus) }}
              </el-tag>
              <div v-if="row.globalToolName" class="subtle">{{ row.globalToolName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right" align="right">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
              <el-button link type="primary" :loading="rowActionLoading[row.apiId]" @click.stop="primaryGovernanceAction(row)">
                {{ primaryGovernanceLabel(row) }}
              </el-button>
              <el-button link type="primary" @click.stop="goStudioWithAsset(row)">用于 Agent</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <span>共 {{ total }} 条</span>
          <el-pagination
            v-model:current-page="filters.page"
            v-model:page-size="filters.pageSize"
            background
            layout="prev, pager, next, sizes"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            @current-change="loadAssets"
            @size-change="reloadFirstPage"
          />
        </div>
      </main>
    </section>

    <el-drawer v-model="detailVisible" size="520px" title="API 资产详情">
      <template v-if="selectedAsset">
        <div class="drawer-title">
          <h3>{{ selectedAsset.name }}</h3>
          <span>{{ selectedAsset.httpMethod || '-' }} {{ selectedAsset.endpointPath || '-' }}</span>
        </div>
        <el-tabs model-value="overview" class="asset-tabs">
          <el-tab-pane label="概览" name="overview">
            <dl class="detail-list">
              <dt>所属项目</dt>
              <dd>{{ selectedAsset.projectName || selectedAsset.projectCode || '-' }}</dd>
              <dt>所属模块</dt>
              <dd>{{ selectedAsset.moduleName || '未关联模块' }}</dd>
              <dt>来源</dt>
              <dd>{{ sourceLabel(selectedAsset.sourceType) }}</dd>
              <dt>Tool 关联</dt>
              <dd>{{ toolStatusLabel(selectedAsset.toolLinkStatus) }}</dd>
              <dt>最近同步</dt>
              <dd>{{ selectedAsset.lastSyncedAt || '-' }}</dd>
            </dl>
          </el-tab-pane>
          <el-tab-pane label="参数" name="parameters">
            <el-table
              v-if="parameterRows(selectedAsset.parameters).length"
              :data="parameterRows(selectedAsset.parameters)"
              row-key="_key"
              size="small"
              border
              :tree-props="{ children: 'children' }"
              default-expand-all
            >
              <el-table-column prop="name" label="参数名" min-width="150" />
              <el-table-column prop="type" label="类型" width="120" />
              <el-table-column prop="location" label="位置" width="100" />
              <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
              <el-table-column label="必填" width="70" align="center">
                <template #default="{ row }">{{ row.required ? '是' : '否' }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无参数结构" :image-size="72" />
          </el-tab-pane>
          <el-tab-pane label="语义" name="semantic">
            <div class="semantic-box">
              <strong>业务说明</strong>
              <p>{{ selectedAsset.description || '暂无接口说明' }}</p>
              <strong>AI 语义</strong>
              <p>{{ selectedAsset.aiDescription || '暂无 AI 语义，请在项目 API 治理页生成或补充。' }}</p>
            </div>
          </el-tab-pane>
          <el-tab-pane label="治理" name="governance">
            <div class="governance-panel">
              <el-alert
                :title="governanceHint(selectedAsset)"
                type="info"
                :closable="false"
                show-icon
              />
              <el-button type="primary" @click="goProjectApiDetail(selectedAsset)">进入 API 治理页</el-button>
              <el-button
                type="success"
                :loading="rowActionLoading[selectedAsset.apiId]"
                :disabled="selectedAsset.removedFromSource"
                @click="primaryGovernanceAction(selectedAsset)"
              >
                {{ primaryGovernanceLabel(selectedAsset) }}
              </el-button>
              <el-button
                :disabled="selectedAsset.removedFromSource"
                @click="openTest(selectedAsset)"
              >
                测试调用
              </el-button>
              <el-button
                v-if="selectedAsset.globalToolDefinitionId"
                type="danger"
                plain
                :loading="rowActionLoading[selectedAsset.apiId]"
                @click="unpromoteFromGlobal(selectedAsset)"
              >
                解除 Tool 关联
              </el-button>
              <el-button @click="goToolList(selectedAsset)">查看 Tool</el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <el-dialog v-model="testDialogVisible" :title="`测试 API - ${testingAsset?.name || ''}`" width="640px">
      <el-form v-if="testingAsset" label-width="120px">
        <el-form-item
          v-for="param in flatParameters(testingAsset.parameters)"
          :key="param._key"
          :label="param.name"
          :required="param.required"
        >
          <el-input v-model="testArgs[param.name]" :placeholder="param.description || param.type" />
          <div class="param-hint">{{ param.description || '-' }} ({{ param.type || '-' }} · {{ param.location || 'body' }})</div>
        </el-form-item>
        <el-empty v-if="flatParameters(testingAsset.parameters).length === 0" description="该 API 没有可填写参数" :image-size="72" />
      </el-form>

      <div v-if="testResult" class="test-result-area">
        <el-divider content-position="left">执行结果</el-divider>
        <el-alert
          :type="testResult.success ? 'success' : 'error'"
          :title="testResult.success ? '执行成功' : '执行失败'"
          :description="testResult.errorMessage || ''"
          :closable="false"
          show-icon
        />
        <pre v-if="testResult.result" class="result-content">{{ testResult.result }}</pre>
        <p class="result-duration">耗时：{{ testResult.durationMs }}ms</p>
      </div>

      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testRunning" @click="handleTest">执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { FolderOpened, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApiAssets } from '@/api/apiAsset'
import {
  getScanProjects,
  promoteScanProjectToolToGlobal,
  pushScanProjectToolToGlobalTool,
  testScanProjectTool,
  unpromoteScanProjectToolFromGlobal,
} from '@/api/scanProject'
import type { ApiAssetItem, ApiAssetQuery } from '@/types/apiAsset'
import type { ScanProject } from '@/types/scanProject'
import type { ToolParameter, ToolTestResult } from '@/types/tool'
import { useProjectStore } from '@/store/project'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()

const loading = ref(false)
const assets = ref<ApiAssetItem[]>([])
const total = ref(0)
const projectOptions = ref<ScanProject[]>([])
const selectedAsset = ref<ApiAssetItem | null>(null)
const detailVisible = ref(false)
const testingAsset = ref<ApiAssetItem | null>(null)
const testDialogVisible = ref(false)
const testRunning = ref(false)
const testResult = ref<ToolTestResult | null>(null)
const quickFilter = ref('')
const rowActionLoading = reactive<Record<number, boolean>>({})
const testArgs = reactive<Record<string, string>>({})

const filters = reactive<ApiAssetQuery>({
  projectId: null,
  keyword: '',
  sourceType: '',
  toolLinkStatus: '',
  semanticStatus: '',
  sensitiveRisk: '',
  removedFromSource: null,
  page: 1,
  pageSize: 20,
})

const summaryItems = computed(() => [
  { label: 'API 总数', value: total.value, caption: '当前筛选结果' },
  { label: '已添加 Tool', value: assets.value.filter((item) => item.toolLinkStatus === 'LINKED').length, caption: '本页数量' },
  { label: '待补语义', value: assets.value.filter((item) => item.semanticStatus === 'MISSING').length, caption: '本页数量' },
  { label: '敏感风险', value: assets.value.filter((item) => item.sensitiveRisk === 'REVIEW').length, caption: '本页数量' },
])

watch(
  () => route.query.projectId,
  (value) => {
    const id = Number(value)
    filters.projectId = Number.isFinite(id) && id > 0 ? id : null
    filters.page = 1
    loadAssets()
  },
  { immediate: true },
)

onMounted(async () => {
  await loadProjects()
})

async function loadProjects() {
  const { data } = await getScanProjects()
  projectOptions.value = Array.isArray(data) ? data : []
}

async function loadAssets() {
  loading.value = true
  try {
    const query = cleanQuery(filters)
    const { data } = await listApiAssets(query)
    assets.value = data.items || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function cleanQuery(query: ApiAssetQuery): ApiAssetQuery {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== '' && value !== null && value !== undefined),
  ) as ApiAssetQuery
}

function reloadFirstPage() {
  filters.page = 1
  loadAssets()
}

function selectProject(projectId: number | null) {
  filters.projectId = projectId
  projectStore.setCurrentProject(projectId)
  router.replace({ name: 'ApiAssetCatalog', query: projectId ? { projectId } : {} })
}

function resetFilters() {
  filters.keyword = ''
  filters.sourceType = ''
  filters.toolLinkStatus = ''
  filters.semanticStatus = ''
  filters.sensitiveRisk = ''
  filters.removedFromSource = null
  filters.agentVisible = null
  filters.enabled = null
  quickFilter.value = ''
  reloadFirstPage()
}

function applyQuickFilter() {
  filters.toolLinkStatus = ''
  filters.semanticStatus = ''
  filters.sensitiveRisk = ''
  filters.removedFromSource = null
  filters.agentVisible = null
  filters.enabled = null
  if (quickFilter.value === 'todo') {
    filters.semanticStatus = 'MISSING'
  } else if (quickFilter.value === 'agentVisible') {
    filters.agentVisible = true
  } else if (quickFilter.value === 'sensitive') {
    filters.sensitiveRisk = 'REVIEW'
  } else if (quickFilter.value === 'removed') {
    filters.removedFromSource = true
  }
  reloadFirstPage()
}

function openDetail(row: ApiAssetItem) {
  selectedAsset.value = row
  detailVisible.value = true
}

function goProjects() {
  router.push('/registry/projects')
}

function goProjectApiDetail(row: ApiAssetItem) {
  router.push({ name: 'ScanProjectDetail', params: { id: String(row.projectId) } })
}

function primaryGovernanceLabel(row: ApiAssetItem) {
  if (row.toolLinkStatus === 'LINKED') return '更新 Tool'
  if (row.toolLinkStatus === 'GLOBAL_MISSING') return '重新添加'
  if (row.toolLinkStatus === 'REMOVED') return '查看风险'
  return '添加 Tool'
}

async function primaryGovernanceAction(row: ApiAssetItem) {
  if (row.removedFromSource) {
    goProjectApiDetail(row)
    return
  }
  if (row.globalToolDefinitionId && row.toolLinkStatus === 'LINKED') {
    await pushToGlobal(row)
    return
  }
  await promoteToGlobal(row)
}

async function promoteToGlobal(row: ApiAssetItem) {
  rowActionLoading[row.apiId] = true
  try {
    const { data } = await promoteScanProjectToolToGlobal(row.projectId, row.apiId)
    ElMessage.success(`已添加到 Tool 管理：${data.globalToolName}`)
    await loadAssets()
    patchSelected(row.apiId)
  } catch (error) {
    ElMessage.error((error as Error).message || '添加失败')
  } finally {
    rowActionLoading[row.apiId] = false
  }
}

async function pushToGlobal(row: ApiAssetItem) {
  rowActionLoading[row.apiId] = true
  try {
    await pushScanProjectToolToGlobalTool(row.projectId, row.apiId)
    ElMessage.success('已更新到 Tool 管理中的对应工具')
    await loadAssets()
    patchSelected(row.apiId)
  } catch (error) {
    ElMessage.error((error as Error).message || '更新失败')
  } finally {
    rowActionLoading[row.apiId] = false
  }
}

async function unpromoteFromGlobal(row: ApiAssetItem) {
  if (!row.globalToolDefinitionId) return
  try {
    await ElMessageBox.confirm(
      '将删除 Tool 管理中的该工具，并解除与本 API 资产的关联。若需对外暴露，可再次添加为 Tool。',
      '解除 Tool 关联',
      { type: 'warning', confirmButtonText: '确定解除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  rowActionLoading[row.apiId] = true
  try {
    await unpromoteScanProjectToolFromGlobal(row.projectId, row.apiId)
    ElMessage.success('已解除 Tool 关联')
    await loadAssets()
    patchSelected(row.apiId)
  } catch (error) {
    ElMessage.error((error as Error).message || '解除失败')
  } finally {
    rowActionLoading[row.apiId] = false
  }
}

function patchSelected(apiId: number) {
  if (!selectedAsset.value || selectedAsset.value.apiId !== apiId) return
  selectedAsset.value = assets.value.find((item) => item.apiId === apiId) || selectedAsset.value
}

function goStudioWithAsset(row: ApiAssetItem) {
  if (!row.globalToolDefinitionId) {
    ElMessage.warning('该 API 还没有添加为 Tool，请先进入治理页完成关联')
    return
  }
  router.push({
    path: '/agent',
    query: {
      projectId: row.projectId,
      intent: 'api-query-template',
      apiAssetId: row.apiId,
      apiAssetTool: row.globalToolName || row.name,
      apiAssetName: row.name,
    },
  })
}

function goToolList(row: ApiAssetItem) {
  router.push({ path: '/tool', query: { projectId: row.projectId, keyword: row.globalToolName || row.name } })
}

function openTest(row: ApiAssetItem) {
  testingAsset.value = row
  testResult.value = null
  Object.keys(testArgs).forEach((key) => delete testArgs[key])
  for (const parameter of flatParameters(row.parameters)) {
    testArgs[parameter.name] = ''
  }
  testDialogVisible.value = true
}

async function handleTest() {
  if (!testingAsset.value) return
  testRunning.value = true
  testResult.value = null
  try {
    const args: Record<string, unknown> = {}
    for (const [key, value] of Object.entries(testArgs)) {
      if (value !== '') args[key] = value
    }
    const { data } = await testScanProjectTool(testingAsset.value.projectId, testingAsset.value.apiId, args)
    testResult.value = data as unknown as ToolTestResult
  } catch (error) {
    testResult.value = {
      success: false,
      result: '',
      errorMessage: (error as Error).message || '执行失败',
      durationMs: 0,
    }
  } finally {
    testRunning.value = false
  }
}

interface ParameterRow extends ToolParameter {
  _key: string
  children?: ParameterRow[]
}

function parameterRows(parameters: ToolParameter[] | null | undefined, prefix = ''): ParameterRow[] {
  if (!parameters || parameters.length === 0) return []
  return parameters.map((parameter, index) => {
    const keyBase = `${parameter.location || 'ROOT'}:${parameter.name || `#${index}`}`
    const key = prefix ? `${prefix}>${keyBase}` : keyBase
    const nested = parameter.children?.length ? parameterRows(parameter.children, key) : undefined
    const { children, ...rest } = parameter
    const row: ParameterRow = { ...rest, _key: key }
    if (nested) row.children = nested
    return row
  })
}

function flatParameters(parameters: ToolParameter[] | null | undefined): ParameterRow[] {
  const rows: ParameterRow[] = []
  const walk = (items: ParameterRow[]) => {
    for (const item of items) {
      rows.push(item)
      if (item.children?.length) walk(item.children)
    }
  }
  walk(parameterRows(parameters))
  return rows
}

function sourceLabel(value?: string | null) {
  const normalized = (value || '').toUpperCase()
  if (normalized === 'SDK' || normalized === 'REGISTERED') return 'SDK 接入'
  if (normalized === 'OPENAPI') return 'OpenAPI'
  if (normalized === 'CONTROLLER') return 'Controller'
  if (normalized === 'SCAN') return '扫描'
  return value || '-'
}

function sourceTagType(value?: string | null) {
  const normalized = (value || '').toUpperCase()
  if (normalized === 'SDK' || normalized === 'REGISTERED') return 'success'
  if (normalized === 'OPENAPI') return 'primary'
  if (normalized === 'CONTROLLER') return 'warning'
  return 'info'
}

function semanticLabel(value?: string | null) {
  if (value === 'COMPLETE') return '完整'
  if (value === 'BASIC') return '基础'
  return '缺失'
}

function semanticTagType(value?: string | null) {
  if (value === 'COMPLETE') return 'success'
  if (value === 'BASIC') return 'warning'
  return 'danger'
}

function toolStatusLabel(value?: string | null) {
  if (value === 'LINKED') return '已添加'
  if (value === 'GLOBAL_MISSING') return 'Tool 缺失'
  if (value === 'REMOVED') return '源已移除'
  return '未添加'
}

function toolStatusTagType(value?: string | null) {
  if (value === 'LINKED') return 'success'
  if (value === 'GLOBAL_MISSING' || value === 'REMOVED') return 'danger'
  return 'info'
}

function governanceHint(row: ApiAssetItem) {
  if (row.removedFromSource) return '源接口已经移除，请检查是否仍被 Agent 或 Tool 引用。'
  if (!row.globalToolDefinitionId) return '该 API 还是项目内资产，添加为 Tool 后才能稳定进入 Agent 工作流。'
  if (row.semanticStatus === 'MISSING') return '该 API 缺少 AI 语义，建议补齐后再用于智能体。'
  return '该 API 已具备基础治理信息，可以在 Agent 或工作流中使用。'
}
</script>

<style scoped lang="scss">
.api-asset-page {
  min-height: calc(100vh - 56px);
  padding: 24px 28px 32px;
  background: #f6f8fb;
  color: #111827;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;

  h1 {
    margin: 3px 0 6px;
    font-size: 28px;
    line-height: 1.2;
  }

  span {
    color: #667085;
  }
}

.eyebrow {
  margin: 0;
  color: #4f46e5;
  font-weight: 700;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item,
.catalog-main,
.project-rail {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 8px;
}

.summary-item {
  padding: 14px 16px;

  span,
  small {
    display: block;
    color: #667085;
  }

  strong {
    display: block;
    margin: 5px 0 2px;
    font-size: 24px;
  }
}

.catalog-shell {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  gap: 16px;
}

.project-rail {
  padding: 12px;
  align-self: start;
}

.rail-title {
  margin: 4px 6px 10px;
  color: #475467;
  font-weight: 700;
}

.rail-project {
  display: block;
  width: 100%;
  padding: 10px 12px;
  margin-bottom: 6px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  text-align: left;
  cursor: pointer;

  strong,
  span {
    display: block;
  }

  span {
    margin-top: 3px;
    color: #667085;
    font-size: 12px;
  }

  &.active,
  &:hover {
    border-color: #c7d2fe;
    background: #eef2ff;
  }
}

.catalog-main {
  padding: 14px;
  min-width: 0;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 140px 150px 140px 130px auto auto;
  gap: 10px;
  margin-bottom: 12px;
}

.api-cell,
.stacked {
  display: flex;
  flex-direction: column;
  gap: 5px;

  span {
    color: #667085;
    font-size: 12px;
  }
}

.governance-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.subtle {
  margin-top: 4px;
  color: #667085;
  font-size: 12px;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  color: #667085;
}

.drawer-title {
  margin-bottom: 16px;

  h3 {
    margin: 0 0 6px;
  }

  span {
    color: #667085;
  }
}

.detail-list {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 10px 14px;

  dt {
    color: #667085;
  }

  dd {
    margin: 0;
    word-break: break-word;
  }
}

.semantic-box {
  display: flex;
  flex-direction: column;
  gap: 8px;

  p {
    margin: 0 0 10px;
    color: #475467;
    line-height: 1.7;
  }
}

.governance-panel {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.param-hint,
.result-duration {
  margin-top: 4px;
  color: #667085;
  font-size: 12px;
}

.test-result-area {
  margin-top: 12px;
}

.result-content {
  max-height: 260px;
  padding: 12px;
  overflow: auto;
  border-radius: 6px;
  background: #111827;
  color: #e5e7eb;
  white-space: pre-wrap;
}

@media (max-width: 1180px) {
  .summary-strip,
  .catalog-shell,
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
