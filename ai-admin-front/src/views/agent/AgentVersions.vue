<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="router.push('/agent')" :icon="ArrowLeft" text>返回</el-button>
        <h2>AgentOps — {{ agent?.name ?? agentId }}</h2>
        <el-tag v-if="agent?.keySlug" size="small" type="info">{{ agent.keySlug }}</el-tag>
      </div>
      <el-button type="primary" @click="refreshAll" :loading="loading || eventLoading">刷新</el-button>
    </div>

    <section class="ops-actions">
      <div class="action-main">
        <span class="summary-label">生产操作</span>
        <strong>{{ activeVersions.length ? '生产版本已接管' : '等待首次发布' }}</strong>
      </div>
      <div class="action-buttons">
        <el-button type="primary" @click="router.push(`/agent/${agentId}/studio`)">进入 Studio 发布</el-button>
        <el-button @click="openProductionSnapshot" :disabled="!currentProductionVersion">当前生产快照</el-button>
        <el-button @click="openRuntimeDetail" :disabled="!agent">Runtime 配置</el-button>
        <el-button @click="loadEvents" :loading="eventLoading">刷新事件</el-button>
      </div>
    </section>

    <section class="ops-overview">
      <div class="ops-summary">
        <div class="ops-summary-main">
          <span class="summary-label">当前生产态</span>
          <strong>{{ productionStateLabel }}</strong>
        </div>
        <div class="ops-summary-tags">
          <el-tag :type="agent?.enabled ? 'success' : 'info'" size="small">
            {{ agent?.enabled ? '已启用' : '未启用' }}
          </el-tag>
          <el-tag size="small">{{ agent?.runtimePlacement || 'CENTRAL' }}</el-tag>
          <el-tag v-if="agent?.runtimeType" size="small" type="info">{{ agent.runtimeType }}</el-tag>
        </div>
      </div>
      <div class="ops-metrics">
        <div v-for="item in overviewItems" :key="item.label" class="metric-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </div>
    </section>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>版本与灰度</span>
          <el-tag v-if="activeVersions.length > 1" type="warning" size="small">多版本灰度</el-tag>
        </div>
      </template>
      <el-table :data="versions" v-loading="loading" stripe>
        <el-table-column prop="version" label="版本" width="140">
          <template #default="{ row }">
            <span class="version-cell">{{ row.version }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rolloutPercent" label="灰度比例" width="130">
          <template #default="{ row }">
            <el-progress
              :percentage="row.rolloutPercent"
              :stroke-width="10"
              :status="row.status === 'ACTIVE' ? 'success' : undefined"
            />
          </template>
        </el-table-column>
        <el-table-column prop="publishedBy" label="发布者" width="140" />
        <el-table-column prop="publishedAt" label="发布时间" width="180" />
        <el-table-column prop="note" label="发布说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click.stop="showSnapshot(row)"
            >查看快照</el-button>
            <el-button
              link
              type="primary"
              size="small"
              @click.stop="showDiff(row)"
            >Diff</el-button>
            <el-popconfirm
              :title="`确认回滚到 ${row.version}？其它 ACTIVE 版本会被置为 RETIRED`"
              @confirm="handleRollback(row)"
              :disabled="row.status === 'ACTIVE' && row.rolloutPercent === 100"
            >
              <template #reference>
                <el-button
                  link
                  type="warning"
                  size="small"
                  :disabled="row.status === 'ACTIVE' && row.rolloutPercent === 100"
                  @click.stop
                >回滚</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="release-events-card">
      <template #header>
        <div class="card-header">
          <span>发布治理时间线</span>
          <el-button size="small" text type="primary" @click="loadEvents">刷新</el-button>
        </div>
      </template>
      <div class="event-toolbar">
        <el-segmented v-model="eventActionFilter" :options="eventActionOptions" />
        <el-segmented v-model="eventDecisionFilter" :options="eventDecisionOptions" />
      </div>
      <div class="event-stats">
        <div v-for="item in eventStats" :key="item.label" class="event-stat">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
      <el-timeline v-loading="eventLoading" class="release-timeline">
        <el-timeline-item
          v-for="event in filteredEvents"
          :key="event.id"
          :timestamp="event.createdAt"
          :type="timelineType(event)"
          placement="top"
        >
          <div class="event-item">
            <div class="event-title">
              <el-tag size="small" :type="actionTagType(event.action)">{{ actionLabel(event.action) }}</el-tag>
              <el-tag size="small" :type="decisionTagType(event.decision)">{{ event.decision }}</el-tag>
              <span v-if="event.version" class="event-version">{{ event.version }}</span>
            </div>
            <div class="event-summary">{{ event.summary || '-' }}</div>
            <div class="event-meta">
              <span>操作者：{{ event.operator || '-' }}</span>
              <span>灰度：{{ event.rolloutPercent != null ? `${event.rolloutPercent}%` : '-' }}</span>
              <el-button
                v-if="validationIssueCount(event)"
                link
                type="primary"
                size="small"
                @click="showEventValidation(event)"
              >查看 {{ validationIssueCount(event) }} 项检查</el-button>
              <el-button
                link
                type="primary"
                size="small"
                @click="showEventDetail(event)"
              >详情</el-button>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="!eventLoading && !filteredEvents.length" description="暂无匹配的发布治理事件" />
    </el-card>

    <el-dialog v-model="snapshotOpen" :title="snapshotTitle" width="720px">
      <pre class="snapshot-pre">{{ prettySnapshot }}</pre>
    </el-dialog>

    <el-dialog v-model="diffOpen" :title="diffTitle" width="820px">
      <el-table :data="snapshotDiffRows" border size="small">
        <el-table-column prop="field" label="字段" width="160" />
        <el-table-column prop="current" label="当前版本" min-width="300">
          <template #default="{ row }">
            <pre class="diff-pre">{{ row.current }}</pre>
          </template>
        </el-table-column>
        <el-table-column prop="previous" label="对比版本" min-width="300">
          <template #default="{ row }">
            <pre class="diff-pre">{{ row.previous }}</pre>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!snapshotDiffRows.length" description="核心字段无差异或没有可对比版本" />
    </el-dialog>

    <el-dialog v-model="eventValidationOpen" title="发布检查详情" width="760px">
      <el-table :data="eventValidationRows" border size="small">
        <el-table-column prop="level" label="级别" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.level === 'ERROR' ? 'danger' : 'warning'">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="220" />
        <el-table-column prop="nodeId" label="节点" width="140" />
        <el-table-column prop="message" label="说明" min-width="260" />
      </el-table>
      <el-empty v-if="!eventValidationRows.length" description="没有校验项" />
    </el-dialog>

    <el-drawer v-model="eventDetailOpen" title="治理事件详情" size="560px">
      <div v-if="selectedEvent" class="event-detail">
        <section class="detail-section detail-hero">
          <div>
            <span class="detail-eyebrow">{{ actionLabel(selectedEvent.action) }}</span>
            <h3>{{ selectedEvent.summary || '发布治理事件' }}</h3>
          </div>
          <el-tag :type="decisionTagType(selectedEvent.decision)">{{ selectedEvent.decision }}</el-tag>
        </section>

        <section class="detail-section">
          <div class="detail-grid">
            <div v-for="item in eventDetailFields" :key="item.label" class="detail-field">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <div class="detail-section-head">
            <span>门禁结果</span>
            <el-tag v-if="eventDetailIssues.length" type="warning" size="small">
              {{ eventDetailIssues.length }} 项
            </el-tag>
          </div>
          <div v-if="eventDetailIssues.length" class="issue-list">
            <div v-for="issue in eventDetailIssues" :key="`${issue.level}-${issue.code}-${issue.nodeId}`" class="issue-item">
              <el-tag size="small" :type="issue.level === 'ERROR' ? 'danger' : 'warning'">{{ issue.level }}</el-tag>
              <div>
                <strong>{{ issue.code }}</strong>
                <p>{{ issue.message }}</p>
                <div class="issue-meta">
                  <small v-if="issue.nodeId">节点：{{ issue.nodeId }}</small>
                  <el-button link type="primary" size="small" @click="locateIssue(issue)">定位修复</el-button>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else description="没有门禁阻断或警告" :image-size="64" />
        </section>

        <section class="detail-section">
          <div class="detail-section-head">
            <span>Runtime 摘要</span>
          </div>
          <div class="detail-grid">
            <div v-for="item in runtimeDetailFields" :key="item.label" class="detail-field">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
          <pre v-if="prettyRuntimeConfig" class="detail-pre">{{ prettyRuntimeConfig }}</pre>
        </section>

        <section class="detail-section">
          <div class="detail-section-head">
            <span>事件快照</span>
            <el-tag v-if="selectedEventVersion" size="small" type="info">{{ selectedEventVersion.version }}</el-tag>
          </div>
          <el-tabs v-model="eventDetailTab">
            <el-tab-pane label="快照摘要" name="snapshot">
              <div v-if="selectedEventVersion" class="detail-grid">
                <div v-for="item in eventSnapshotFields" :key="item.label" class="detail-field">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
              <el-empty v-else description="该事件未关联版本快照" :image-size="64" />
            </el-tab-pane>
            <el-tab-pane label="与当前配置 Diff" name="diff">
              <el-table v-if="eventSnapshotDiffRows.length" :data="eventSnapshotDiffRows" border size="small">
                <el-table-column prop="field" label="字段" width="150" />
                <el-table-column prop="snapshot" label="事件快照" min-width="220">
                  <template #default="{ row }">
                    <pre class="diff-pre">{{ row.snapshot }}</pre>
                  </template>
                </el-table-column>
                <el-table-column prop="current" label="当前配置" min-width="220">
                  <template #default="{ row }">
                    <pre class="diff-pre">{{ row.current }}</pre>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="没有发现核心字段差异" :image-size="64" />
            </el-tab-pane>
            <el-tab-pane label="原始 JSON" name="json">
              <pre v-if="selectedEventSnapshotJson" class="detail-pre">{{ selectedEventSnapshotJson }}</pre>
              <el-empty v-else description="暂无快照 JSON" :image-size="64" />
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import type { AgentDefinition, AgentReleaseEvent, AgentReleaseValidationItem, AgentVersion } from '@/types/agent'
import { getAgent, listAgentReleaseEvents, listAgentVersions, rollbackAgentVersion } from '@/api/agent'

const route = useRoute()
const router = useRouter()
const agentId = route.params.id as string

const loading = ref(false)
const eventLoading = ref(false)
const versions = ref<AgentVersion[]>([])
const events = ref<AgentReleaseEvent[]>([])
const agent = ref<AgentDefinition | null>(null)

const snapshotOpen = ref(false)
const snapshotRow = ref<AgentVersion | null>(null)
const diffOpen = ref(false)
const diffRow = ref<AgentVersion | null>(null)
const eventValidationOpen = ref(false)
const eventValidationRows = ref<AgentReleaseValidationItem[]>([])
const eventDetailOpen = ref(false)
const selectedEvent = ref<AgentReleaseEvent | null>(null)
const eventActionFilter = ref('ALL')
const eventDecisionFilter = ref('ALL')
const eventDetailTab = ref('snapshot')
const eventActionOptions = [
  { label: '全部动作', value: 'ALL' },
  { label: '校验', value: 'VALIDATE' },
  { label: '发布', value: 'PUBLISH' },
  { label: '回滚', value: 'ROLLBACK' },
]
const eventDecisionOptions = [
  { label: '全部结果', value: 'ALL' },
  { label: '通过', value: 'PASSED' },
  { label: '完成', value: 'COMPLETED' },
  { label: '阻断', value: 'BLOCKED' },
]
const activeVersions = computed(() => versions.value.filter((item) => item.status === 'ACTIVE'))
const currentProductionVersion = computed(() =>
  activeVersions.value.find((item) => item.rolloutPercent === 100) ?? activeVersions.value[0] ?? null,
)
const latestPublishEvent = computed(() => events.value.find((item) => item.action === 'PUBLISH' && item.decision === 'COMPLETED') ?? null)
const latestRollbackEvent = computed(() => events.value.find((item) => item.action === 'ROLLBACK' && item.decision === 'COMPLETED') ?? null)
const blockedEventCount = computed(() => events.value.filter((item) => item.decision === 'BLOCKED').length)
const filteredEvents = computed(() => events.value.filter((event) => {
  const actionMatched = eventActionFilter.value === 'ALL' || event.action === eventActionFilter.value
  const decisionMatched = eventDecisionFilter.value === 'ALL' || event.decision === eventDecisionFilter.value
  return actionMatched && decisionMatched
}))
const eventStats = computed(() => [
  { label: '全部事件', value: events.value.length },
  { label: '发布完成', value: events.value.filter((item) => item.action === 'PUBLISH' && item.decision === 'COMPLETED').length },
  { label: '发布阻断', value: blockedEventCount.value },
  { label: '回滚完成', value: events.value.filter((item) => item.action === 'ROLLBACK' && item.decision === 'COMPLETED').length },
])
const activeRolloutTotal = computed(() =>
  activeVersions.value.reduce((sum, item) => sum + (item.rolloutPercent || 0), 0),
)
const productionStateLabel = computed(() => {
  if (!activeVersions.value.length) return '尚未发布'
  if (activeVersions.value.length === 1) return `${activeVersions.value[0].version} · ${activeVersions.value[0].rolloutPercent}%`
  return `${activeVersions.value.length} 个 ACTIVE 版本 · 合计 ${activeRolloutTotal.value}%`
})
const overviewItems = computed(() => [
  {
    label: 'ACTIVE 版本',
    value: activeVersions.value.length ? activeVersions.value.map((v) => v.version).join(' / ') : '-',
    hint: activeVersions.value.length > 1 ? '灰度分流中' : '当前承载生产流量',
  },
  {
    label: '灰度覆盖',
    value: activeVersions.value.length ? `${activeRolloutTotal.value}%` : '-',
    hint: activeRolloutTotal.value === 100 ? '覆盖完整' : '存在未覆盖流量或草稿回落',
  },
  {
    label: '最近发布',
    value: latestPublishEvent.value?.version || '-',
    hint: latestPublishEvent.value?.createdAt || '暂无发布记录',
  },
  {
    label: '最近回滚',
    value: latestRollbackEvent.value?.version || '-',
    hint: latestRollbackEvent.value?.createdAt || '暂无回滚记录',
  },
  {
    label: '阻断事件',
    value: String(blockedEventCount.value),
    hint: blockedEventCount.value ? '存在发布门禁阻断' : '暂无阻断记录',
  },
])
const snapshotTitle = computed(() => snapshotRow.value
  ? `快照 — ${snapshotRow.value.version}`
  : '快照')
const prettySnapshot = computed(() => {
  if (!snapshotRow.value) return ''
  try {
    return JSON.stringify(JSON.parse(snapshotRow.value.snapshotJson), null, 2)
  } catch {
    return snapshotRow.value.snapshotJson
  }
})
const diffTitle = computed(() => diffRow.value ? `版本 Diff — ${diffRow.value.version}` : '版本 Diff')
const snapshotDiffRows = computed(() => {
  if (!diffRow.value) return []
  const previous = findPreviousVersion(diffRow.value)
  if (!previous) return []
  const currentJson = parseSnapshot(diffRow.value.snapshotJson)
  const previousJson = parseSnapshot(previous.snapshotJson)
  const fields = ['name', 'intentType', 'systemPrompt', 'tools', 'skills', 'maxSteps', 'allowIrreversible', 'canvasJson']
  return fields
    .map((field) => ({
      field,
      current: stringifyValue(currentJson?.[field]),
      previous: stringifyValue(previousJson?.[field]),
    }))
    .filter((row) => row.current !== row.previous)
})
const selectedEventMetadata = computed(() => parseMetadata(selectedEvent.value?.metadataJson))
const eventDetailIssues = computed(() => selectedEvent.value ? parseValidationItems(selectedEvent.value.validationJson) : [])
const eventDetailFields = computed(() => {
  const event = selectedEvent.value
  const metadata = selectedEventMetadata.value
  if (!event) return []
  return [
    { label: '版本', value: event.version || textValue(metadata.version) || '-' },
    { label: '事件类型', value: actionLabel(event.action) },
    { label: '决策', value: event.decision },
    { label: '灰度比例', value: event.rolloutPercent != null ? `${event.rolloutPercent}%` : '-' },
    { label: '操作者', value: event.operator || '-' },
    { label: '发生时间', value: event.createdAt || '-' },
    { label: 'Agent', value: textValue(metadata.agentName) || agent.value?.name || '-' },
    { label: '项目', value: textValue(metadata.projectCode) || agent.value?.projectCode || '-' },
  ]
})
const runtimeDetailFields = computed(() => {
  const metadata = selectedEventMetadata.value
  return [
    { label: 'Runtime', value: textValue(metadata.runtimeType) || agent.value?.runtimeType || '-' },
    { label: '部署位置', value: textValue(metadata.runtimePlacement) || agent.value?.runtimePlacement || '-' },
    { label: '模型实例', value: textValue(metadata.modelInstanceId) || agent.value?.modelInstanceId || '-' },
    { label: '图节点', value: textValue(metadata.graphNodeCount) || '0' },
    { label: '图边', value: textValue(metadata.graphEdgeCount) || '0' },
    { label: '工具 / Skill', value: `${textValue(metadata.toolCount) || '0'} / ${textValue(metadata.skillCount) || '0'}` },
    { label: '最大步数', value: textValue(metadata.maxSteps) || '-' },
    { label: '副作用权限', value: metadata.allowIrreversible === true ? '允许' : '未允许' },
  ]
})
const prettyRuntimeConfig = computed(() => {
  const config = selectedEventMetadata.value.runtimeConfig
  if (!config || typeof config !== 'object') return ''
  return JSON.stringify(config, null, 2)
})
const selectedEventVersion = computed(() => {
  const event = selectedEvent.value
  if (!event) return null
  if (event.versionId != null) {
    const matched = versions.value.find((item) => item.id === event.versionId)
    if (matched) return matched
  }
  if (event.version) {
    return versions.value.find((item) => item.version === event.version) ?? null
  }
  return null
})
const selectedEventSnapshot = computed(() => {
  const version = selectedEventVersion.value
  if (!version) return null
  return parseSnapshot(version.snapshotJson)
})
const selectedEventSnapshotJson = computed(() => {
  const snapshot = selectedEventSnapshot.value
  return snapshot ? JSON.stringify(snapshot, null, 2) : ''
})
const currentAgentComparable = computed(() => {
  const current = agent.value
  if (!current) return null
  return current as unknown as Record<string, unknown>
})
const eventSnapshotFields = computed(() => {
  const snapshot = selectedEventSnapshot.value
  if (!snapshot) return []
  return [
    { label: 'Agent 名称', value: textValue(snapshot.name) || '-' },
    { label: '意图类型', value: textValue(snapshot.intentType) || '-' },
    { label: 'Runtime', value: textValue(snapshot.runtimeType) || '-' },
    { label: '部署位置', value: textValue(snapshot.runtimePlacement) || '-' },
    { label: '模型实例', value: textValue(snapshot.modelInstanceId) || '-' },
    { label: '最大步数', value: textValue(snapshot.maxSteps) || '-' },
    { label: '工具数', value: Array.isArray(snapshot.tools) ? String(snapshot.tools.length) : '0' },
    { label: 'Skill 数', value: Array.isArray(snapshot.skills) ? String(snapshot.skills.length) : '0' },
  ]
})
const eventSnapshotDiffRows = computed(() => {
  const snapshot = selectedEventSnapshot.value
  const current = currentAgentComparable.value
  if (!snapshot || !current) return []
  const fields = [
    'name',
    'intentType',
    'systemPrompt',
    'runtimeType',
    'runtimePlacement',
    'runtimeConfig',
    'modelInstanceId',
    'tools',
    'skills',
    'maxSteps',
    'allowIrreversible',
    'graphSpec',
    'canvasJson',
  ]
  return fields
    .map((field) => ({
      field,
      snapshot: stringifyValue(snapshot[field]),
      current: stringifyValue(current[field]),
    }))
    .filter((row) => row.snapshot !== row.current)
})

function statusTagType(status: string): '' | 'success' | 'info' | 'warning' | 'danger' {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'RETIRED': return 'info'
    case 'DRAFT': return 'warning'
    default: return ''
  }
}

function actionTagType(action: string): '' | 'success' | 'info' | 'warning' | 'danger' {
  switch (action) {
    case 'PUBLISH': return 'success'
    case 'ROLLBACK': return 'warning'
    case 'VALIDATE': return 'info'
    default: return ''
  }
}

function actionLabel(action: string) {
  switch (action) {
    case 'PUBLISH': return '发布'
    case 'ROLLBACK': return '回滚'
    case 'VALIDATE': return '校验'
    default: return action
  }
}

function decisionTagType(decision: string): '' | 'success' | 'info' | 'warning' | 'danger' {
  switch (decision) {
    case 'PASSED': return 'success'
    case 'COMPLETED': return 'success'
    case 'BLOCKED': return 'danger'
    default: return ''
  }
}

function timelineType(event: AgentReleaseEvent): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (event.decision === 'BLOCKED') return 'danger'
  if (event.action === 'ROLLBACK') return 'warning'
  if (event.action === 'PUBLISH') return 'success'
  return 'info'
}

async function loadAgent() {
  try {
    const { data } = await getAgent(agentId)
    agent.value = data
  } catch {
    ElMessage.error('加载 Agent 失败')
  }
}

async function loadVersions() {
  loading.value = true
  try {
    const { data } = await listAgentVersions(agentId)
    versions.value = Array.isArray(data) ? data : []
  } catch {
    versions.value = []
    ElMessage.error('加载版本列表失败')
  } finally {
    loading.value = false
  }
}

async function loadEvents() {
  eventLoading.value = true
  try {
    const { data } = await listAgentReleaseEvents(agentId, 100)
    events.value = Array.isArray(data) ? data : []
  } catch {
    events.value = []
    ElMessage.error('加载发布治理事件失败')
  } finally {
    eventLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadAgent(), loadVersions(), loadEvents()])
}

async function handleRollback(row: AgentVersion) {
  try {
    await rollbackAgentVersion(agentId, row.id, 'admin')
    ElMessage.success(`已回滚到 ${row.version}`)
    await loadVersions()
    await loadEvents()
  } catch (err) {
    ElMessage.error('回滚失败：' + (err as Error).message)
  }
}

function showSnapshot(row: AgentVersion) {
  snapshotRow.value = row
  snapshotOpen.value = true
}

function showDiff(row: AgentVersion) {
  diffRow.value = row
  diffOpen.value = true
}

function showEventValidation(row: AgentReleaseEvent) {
  eventValidationRows.value = parseValidationItems(row.validationJson)
  eventValidationOpen.value = true
}

function showEventDetail(row: AgentReleaseEvent) {
  selectedEvent.value = row
  eventDetailTab.value = 'snapshot'
  eventDetailOpen.value = true
}

function openProductionSnapshot() {
  if (!currentProductionVersion.value) return
  showSnapshot(currentProductionVersion.value)
}

function openRuntimeDetail() {
  selectedEvent.value = null
  snapshotRow.value = {
    id: 0,
    agentId,
    version: 'CURRENT_RUNTIME',
    snapshotJson: JSON.stringify({
      runtimeType: agent.value?.runtimeType,
      runtimePlacement: agent.value?.runtimePlacement,
      runtimeConfig: agent.value?.runtimeConfig,
      modelInstanceId: agent.value?.modelInstanceId,
      graphSpec: agent.value?.graphSpec,
    }, null, 2),
    rolloutPercent: 0,
    status: 'DRAFT',
    createTime: '',
  }
  snapshotOpen.value = true
}

function locateIssue(issue: AgentReleaseValidationItem) {
  const query = issue.nodeId ? { focusNode: issue.nodeId } : undefined
  router.push({ path: `/agent/${agentId}/studio`, query })
}

function findPreviousVersion(row: AgentVersion) {
  const idx = versions.value.findIndex((v) => v.id === row.id)
  if (idx < 0) return null
  return versions.value[idx + 1] ?? null
}

function parseSnapshot(raw: string): Record<string, unknown> | null {
  try {
    return JSON.parse(raw) as Record<string, unknown>
  } catch {
    return null
  }
}

function parseValidationItems(raw?: string | null): AgentReleaseValidationItem[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw) as { errors?: AgentReleaseValidationItem[]; warnings?: AgentReleaseValidationItem[] }
    return [...(parsed.errors ?? []), ...(parsed.warnings ?? [])]
  } catch {
    return []
  }
}

function parseMetadata(raw?: string | null): Record<string, unknown> {
  if (!raw) return {}
  try {
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? parsed as Record<string, unknown>
      : {}
  } catch {
    return {}
  }
}

function validationIssueCount(row: AgentReleaseEvent) {
  return parseValidationItems(row.validationJson).length
}

function textValue(value: unknown) {
  if (value === undefined || value === null || value === '') return ''
  return String(value)
}

function stringifyValue(value: unknown) {
  if (value === undefined || value === null) return ''
  if (typeof value === 'string') return value
  return JSON.stringify(value, null, 2)
}

onMounted(() => {
  loadAgent()
  loadVersions()
  loadEvents()
})
</script>

<style scoped lang="scss">
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;

  h2 {
    margin: 0;
    font-size: 18px;
  }
}

.version-cell {
  font-family: 'Cascadia Code', 'Fira Code', monospace;
  font-weight: 500;
}

.ops-actions {
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.action-main {
  display: flex;
  flex-direction: column;
  gap: 5px;

  strong {
    color: var(--el-text-color-primary);
    font-size: 16px;
  }
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.ops-overview {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 12px;
}

.ops-summary,
.metric-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.ops-summary {
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
}

.ops-summary-main {
  display: flex;
  flex-direction: column;
  gap: 6px;

  strong {
    font-size: 20px;
    color: var(--el-text-color-primary);
    line-height: 1.3;
  }
}

.summary-label,
.metric-card span,
.metric-card small,
.event-meta {
  color: var(--el-text-color-secondary);
}

.ops-summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.ops-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.metric-card {
  min-height: 92px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  strong {
    color: var(--el-text-color-primary);
    font-size: 18px;
    line-height: 1.25;
    word-break: break-word;
  }

  small {
    line-height: 1.4;
  }
}

.snapshot-pre {
  background: #f8fafc;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 4px;
  padding: 12px;
  max-height: 480px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.diff-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
}

.release-events-card {
  margin-top: 16px;
}

.event-toolbar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.event-stats {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 10px;
}

.event-stat {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  display: flex;
  flex-direction: column;
  gap: 6px;

  span {
    color: var(--el-text-color-secondary);
  }

  strong {
    color: var(--el-text-color-primary);
    font-size: 18px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.release-timeline {
  padding: 4px 4px 0;
}

.event-item {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
}

.event-title,
.event-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.event-summary {
  margin: 8px 0;
  color: var(--el-text-color-primary);
}

.event-version {
  font-family: 'Cascadia Code', 'Fira Code', monospace;
  color: var(--el-text-color-regular);
}

.event-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-section {
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.detail-hero,
.detail-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-hero h3 {
  margin: 4px 0 0;
  font-size: 17px;
  line-height: 1.4;
  color: var(--el-text-color-primary);
}

.detail-eyebrow,
.detail-field span,
.issue-item small {
  color: var(--el-text-color-secondary);
}

.detail-eyebrow {
  font-size: 12px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.detail-field {
  min-width: 0;
  padding: 10px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  display: flex;
  flex-direction: column;
  gap: 6px;

  strong {
    color: var(--el-text-color-primary);
    font-size: 13px;
    line-height: 1.35;
    word-break: break-word;
  }
}

.detail-section-head {
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.issue-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.issue-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);

  p {
    margin: 4px 0;
    color: var(--el-text-color-regular);
    line-height: 1.45;
  }
}

.issue-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-pre {
  margin: 12px 0 0;
  padding: 10px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
}

@media (max-width: 980px) {
  .ops-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .action-buttons {
    justify-content: flex-start;
  }

  .ops-overview {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

// ── 日间模式覆盖 ──
:global([data-theme="light"]) {
  .snapshot-pre {
    border: 1px solid #ebeef5;
  }
}
</style>
