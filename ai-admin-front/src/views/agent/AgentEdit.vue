<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="router.push('/agent')" :icon="ArrowLeft" text>返回</el-button>
        <h2>{{ isNew ? '新建 Agent' : `编辑 Agent — ${form.name}` }}</h2>
      </div>
      <div class="header-actions">
        <el-button v-if="!isNew" @click="router.push(`/agent/${agentId}/studio`)">画布编排</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="140px"
      v-loading="pageLoading"
    >
      <!-- 基本信息 -->
      <el-card shadow="never" class="section-card">
        <template #header>基本信息</template>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="Agent 名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="意图类型" prop="intentType">
              <el-select
                v-model="form.intentType"
                placeholder="选择或输入意图类型"
                style="width: 100%"
                filterable
                allow-create
              >
                <el-option
                  v-for="t in INTENT_TYPES"
                  :key="t.value"
                  :label="t.label"
                  :value="t.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="所属项目">
              <el-select
                v-model="form.projectId"
                clearable
                filterable
                placeholder="平台级 / 全局 Agent"
                style="width: 100%"
                @change="handleProjectChange"
              >
                <el-option
                  v-for="project in scanProjects"
                  :key="project.id"
                  :label="projectOptionLabel(project)"
                  :value="project.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="项目编码">
              <el-input v-model="form.projectCode" disabled placeholder="选择项目后自动填充" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="可见性">
              <el-select v-model="form.visibility" style="width: 100%">
                <el-option label="PRIVATE" value="PRIVATE" />
                <el-option label="PROJECT" value="PROJECT" />
                <el-option label="SHARED" value="SHARED" />
                <el-option label="PUBLIC" value="PUBLIC" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="keySlug">
              <el-input
                v-model="form.keySlug"
                placeholder="对应 /api/v1/agents/{keySlug}/chat，留空自动用 id"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="允许 IRREVERSIBLE">
              <el-switch v-model="form.allowIrreversible" />
              <el-tooltip content="允许调用 DELETE 等不可逆副作用工具（护栏白名单）" placement="top">
                <el-icon class="tip-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="Agent 描述（同时用于意图识别候选列表）" />
        </el-form-item>
      </el-card>

      <!-- 模型与执行配置 -->
      <el-card shadow="never" class="section-card">
        <template #header>模型与执行</template>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="模型厂商">
              <el-select v-model="selectedLlmProvider" filterable placeholder="请选择模型厂商" style="width: 100%" @change="handleLlmProviderChange">
                <el-option
                  v-for="provider in llmProviderOptions"
                  :key="provider"
                  :label="provider"
                  :value="provider"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模型实例" prop="modelInstanceId">
              <el-select
                v-model="form.modelInstanceId"
                filterable
                placeholder="请选择模型实例"
                style="width: 100%"
                :disabled="!selectedLlmProvider"
                @change="syncSelectedLlmProvider"
              >
                <el-option
                  v-for="item in filteredLlmModelInstances"
                  :key="item.id"
                  :label="`${item.name} (${item.modelName})`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="触发方式">
              <el-select v-model="form.triggerMode" style="width: 100%">
                <el-option
                  v-for="m in TRIGGER_MODES"
                  :key="m.value"
                  :label="m.label"
                  :value="m.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="运行时">
              <el-select v-model="form.runtimeType" style="width: 100%" @change="handleRuntimeTypeChange">
                <el-option
                  v-for="runtime in runtimeOptions"
                  :key="runtime.runtimeType"
                  :label="runtime.displayName || runtime.runtimeType"
                  :value="runtime.runtimeType"
                  :disabled="!runtime.available"
                >
                  <div class="runtime-option">
                    <span>{{ runtime.displayName || runtime.runtimeType }}</span>
                    <el-tag v-if="runtime.available" size="small" type="success" effect="plain">可用</el-tag>
                    <el-tooltip v-else :content="runtime.unavailableReason || '当前运行时不可用'" placement="top">
                      <el-tag size="small" :type="runtimeUnavailableTagType(runtime)" effect="plain">
                        {{ runtimeUnavailableLabel(runtime) }}
                      </el-tag>
                    </el-tooltip>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="运行位置">
              <el-segmented v-model="form.runtimePlacement" :options="runtimePlacementOptions" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启用">
              <el-switch v-model="form.enabled" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="form.runtimePlacement !== 'CENTRAL'" :gutter="24">
          <el-col :span="8">
            <el-form-item label="纳管实例">
              <el-select
                v-model="selectedRuntimeInstanceId"
                clearable
                filterable
                placeholder="选择 Embedded Runtime 实例"
                style="width: 100%"
              >
                <el-option
                  v-for="instance in availableRuntimeInstances"
                  :key="instance.instanceId"
                  :label="runtimeInstanceLabel(instance)"
                  :value="instance.instanceId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-alert
              title="本地 / 混合运行将按所选实例投递"
              description="EMBEDDED 会直接投递到业务实例；HYBRID 会优先投递，失败时回落中台运行。"
              type="info"
              show-icon
              :closable="false"
            />
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="输出 Schema">
              <el-input v-model="form.outputSchemaType" placeholder="如 ReviewResult，留空返回纯文本" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- AI 能力中台配置 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="card-header-with-badge">
            AI 能力中台配置
            <el-tag size="small" type="info">新</el-tag>
          </div>
        </template>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="知识库组 ID">
              <el-input v-model="form.knowledgeBaseGroupId" placeholder="关联的知识库组（多库协同检索）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Prompt 模板 ID">
              <el-input v-model="form.promptTemplateId" placeholder="关联的 Prompt 模板（可覆盖 System Prompt）" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <!-- System Prompt -->
      <el-card shadow="never" class="section-card">
        <template #header>System Prompt</template>
        <el-form-item label-width="0">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="10"
            placeholder="输入 System Prompt，定义 Agent 的行为和角色..."
            class="prompt-editor"
          />
        </el-form-item>
      </el-card>

      <!-- Runtime 专属配置 -->
      <el-card shadow="never" class="section-card">
        <template #header>
          <div class="runtime-config-header">
            <span>Runtime 配置</span>
            <el-tag size="small" type="info" effect="plain">{{ selectedRuntime?.displayName || form.runtimeType }}</el-tag>
          </div>
        </template>

        <div v-if="form.runtimeType === 'AGENTSCOPE'" class="runtime-panel">
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="最大步数">
                <el-input-number v-model="form.maxSteps" :min="1" :max="20" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="Agent 类型">
                <el-radio-group v-model="form.type">
                  <el-radio value="single">单 Agent</el-radio>
                  <el-radio value="pipeline">Pipeline</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="多 Agent 模型">
                <el-switch v-model="form.useMultiAgentModel" />
                <el-tooltip content="Pipeline 子 Agent 应开启此项，使用 MultiAgentFormatter 模型" placement="top">
                  <el-icon class="tip-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="可用工具" label-width="100px">
            <div class="tool-select-area">
              <el-select
                v-model="form.tools"
                multiple
                filterable
                collapse-tags
                collapse-tags-tooltip
                style="width: 100%"
                placeholder="选择已启用且对 Agent 可见的工具"
              >
                <el-option
                  v-for="tool in availableTools"
                  :key="tool.name"
                  :label="capabilityLabel(tool)"
                  :value="tool.name"
                >
                  <div class="tool-option">
                    <span>{{ tool.name }}</span>
                    <span class="tool-option-desc">{{ tool.description }}</span>
                  </div>
                </el-option>
              </el-select>
              <div class="tool-hint">仅展示已启用且设置为 Agent 可见的工具。</div>
            </div>
          </el-form-item>

          <el-form-item label="可用能力" label-width="100px">
            <div class="tool-select-area">
              <el-select
                v-model="form.skills"
                multiple
                filterable
                collapse-tags
                collapse-tags-tooltip
                style="width: 100%"
                placeholder="选择已启用且对 Agent 可见的粗粒度能力"
              >
                <el-option
                  v-for="sk in availableCapabilities"
                  :key="sk.name"
                  :label="capabilityLabel(sk)"
                  :value="sk.name"
                >
                  <div class="tool-option">
                    <span>{{ sk.name }}</span>
                    <span class="tool-option-desc">{{ sk.description }}</span>
                  </div>
                </el-option>
              </el-select>
              <div class="tool-hint">保存后与 Tool 配置一并注入模型可调用的列表。</div>
            </div>
          </el-form-item>

          <el-form-item v-if="form.type === 'pipeline'" label="子 Agent ID" label-width="120px">
            <div class="tag-input-area">
              <el-tag
                v-for="(aid, idx) in form.pipelineAgentIds"
                :key="idx"
                closable
                type="warning"
                @close="form.pipelineAgentIds.splice(idx, 1)"
                class="item-tag"
              >{{ aid }}</el-tag>
              <el-input
                v-if="showPipelineInput"
                v-model="newPipelineId"
                size="small"
                style="width: 200px"
                @keyup.enter="addPipelineId"
                @blur="addPipelineId"
                placeholder="Agent ID"
              />
              <el-button v-else size="small" @click="showPipelineInput = true">
                + 添加子 Agent
              </el-button>
            </div>
          </el-form-item>
        </div>

        <div v-else-if="form.runtimeType === 'LANGGRAPH4J'" class="runtime-panel">
          <el-alert
            title="LangGraph4j 使用 GraphSpec 执行 LLM / Tool / 能力节点"
            type="success"
            show-icon
            :closable="false"
            class="runtime-alert"
          />
          <div class="graph-preview">
            <div class="graph-node">START</div>
            <div class="graph-edge">→</div>
            <div class="graph-node graph-node-primary">{{ langGraph4jNodeId }}</div>
            <div class="graph-edge">→</div>
            <div class="graph-node">END</div>
          </div>
          <el-row :gutter="24">
            <el-col :span="8">
              <el-form-item label="LLM 节点 ID">
                <el-input v-model="langGraph4jNodeId" placeholder="llm" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="图模式">
                <el-input :model-value="langGraph4jGraphMode" disabled />
              </el-form-item>
            </el-col>
          </el-row>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="图模式">GraphSpec 工作流</el-descriptions-item>
            <el-descriptions-item label="Tool / 能力">Studio 图节点支持</el-descriptions-item>
            <el-descriptions-item label="Pipeline">暂不支持</el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-else class="runtime-panel">
          <el-empty
            :description="`${selectedRuntime?.displayName || form.runtimeType} 配置面板尚未接入`"
            :image-size="80"
          />
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft, QuestionFilled } from '@element-plus/icons-vue'
import { INTENT_TYPES, TRIGGER_MODES } from '@/types/agent'
import type { AgentForm, AgentGraphSpec, AgentRuntimeCapability } from '@/types/agent'
import { getAgent, createAgent, updateAgent, getAgentRuntimes, validateAgentRuntime } from '@/api/agent'
import { listRegistryProjectInstances } from '@/api/registry'
import { getTools } from '@/api/tool'
import { listCapabilities } from '@/api/capability'
import { getScanProjects } from '@/api/scanProject'
import { getModelInstances } from '@/api/model'
import type { ToolInfo } from '@/types/tool'
import type { CapabilityInfo } from '@/types/capability'
import type { ScanProject } from '@/types/scanProject'
import type { ProjectInstance } from '@/types/registry'
import type { ModelInstance } from '@/types/model'
import { useProjectStore } from '@/store/project'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const agentId = route.params.id as string
const isNew = agentId === 'new'

const formRef = ref<FormInstance>()
const pageLoading = ref(false)
const saving = ref(false)
const toolOptions = ref<ToolInfo[]>([])
const capabilityOptions = ref<CapabilityInfo[]>([])
const scanProjects = ref<ScanProject[]>([])
const runtimeInstances = ref<ProjectInstance[]>([])
const llmModelInstances = ref<ModelInstance[]>([])
const runtimeOptions = ref<AgentRuntimeCapability[]>([])
const selectedLlmProvider = ref('')
const previousProjectId = ref<number | null>(null)
const previousRuntimeType = ref<AgentForm['runtimeType']>('AGENTSCOPE')

const form = reactive<AgentForm>({
  keySlug: '',
  name: '',
  description: '',
  projectId: null,
  projectCode: null,
  visibility: 'PRIVATE',
  intentType: 'GENERAL_CHAT',
  systemPrompt: '',
  tools: [],
  toolRefs: [],
  skills: [],
  skillRefs: [],
  modelInstanceId: '',
  runtimeType: 'AGENTSCOPE',
  runtimePlacement: 'CENTRAL',
  runtimeConfig: {},
  graphSpec: null,
  maxSteps: 5,
  enabled: true,
  type: 'single',
  pipelineAgentIds: [],
  knowledgeBaseGroupId: '',
  promptTemplateId: '',
  outputSchemaType: '',
  triggerMode: 'all',
  useMultiAgentModel: false,
  extra: {},
  allowIrreversible: false,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入 Agent 名称', trigger: 'blur' }],
  modelInstanceId: [{ required: true, message: '请选择模型实例', trigger: 'change' }],
}

const availableTools = computed(() =>
  toolOptions.value.filter((tool) => tool.enabled && tool.agentVisible),
)

const availableCapabilities = computed(() =>
  capabilityOptions.value.filter(
    (sk) => sk.enabled && sk.agentVisible && !sk.draft,
  ),
)
const llmProviderOptions = computed(() =>
  Array.from(new Set(llmModelInstances.value.map((item) => item.provider).filter(Boolean))).sort(),
)
const filteredLlmModelInstances = computed(() =>
  llmModelInstances.value.filter((item) => item.provider === selectedLlmProvider.value),
)
const selectedRuntime = computed(() =>
  runtimeOptions.value.find((item) => item.runtimeType === form.runtimeType),
)
const runtimePlacementOptions = [
  { label: '中台运行', value: 'CENTRAL' },
  { label: '本地运行', value: 'EMBEDDED' },
  { label: '混合运行', value: 'HYBRID' },
]
const availableRuntimeInstances = computed(() =>
  runtimeInstances.value.filter((item) => item.status === 'ONLINE'),
)
const selectedRuntimeInstanceId = computed({
  get: () => embeddedRuntimeConfig().instanceId || '',
  set: (instanceId: string) => {
    const instance = availableRuntimeInstances.value.find((item) => item.instanceId === instanceId)
    setEmbeddedRuntimeTarget(instance)
  },
})
const langGraph4jGraphSpec = computed<AgentGraphSpec>(() => ensureLangGraph4jGraphSpec())
const langGraph4jGraphMode = computed(() => langGraph4jGraphSpec.value.mode || 'WORKFLOW')
const langGraph4jNodeId = computed({
  get: () => langGraph4jGraphSpec.value.nodes[0]?.id || 'llm',
  set: (value: string) => {
    const nodeId = normalizeNodeId(value)
    const graphSpec = ensureLangGraph4jGraphSpec()
    graphSpec.nodes = [{ id: nodeId, type: 'LLM', name: 'LLM', config: { modelInstanceId: form.modelInstanceId } }]
    graphSpec.edges = [
      { from: 'START', to: nodeId },
      { from: nodeId, to: 'END' },
    ]
    graphSpec.entry = nodeId
    graphSpec.finish = [nodeId]
  },
})

const showPipelineInput = ref(false)
const newPipelineId = ref('')

function addPipelineId() {
  const id = newPipelineId.value.trim()
  if (id && !form.pipelineAgentIds.includes(id)) {
    form.pipelineAgentIds.push(id)
  }
  newPipelineId.value = ''
  showPipelineInput.value = false
}

function projectOptionLabel(project?: ScanProject | null) {
  if (!project) return ''
  const code = project.projectCode ? ` / ${project.projectCode}` : ''
  const env = project.environment ? ` · ${project.environment}` : ''
  return `${project.name}${code}${env}`
}

function projectCodeById(projectId?: number | null) {
  if (projectId == null) return null
  return scanProjects.value.find((project) => project.id === projectId)?.projectCode || null
}

function capabilityLabel(item: ToolInfo | CapabilityInfo) {
  const project = item.projectCode ? ` · ${item.projectCode}` : ''
  const visibility = item.visibility ? ` · ${item.visibility}` : ''
  return `${item.name}${project}${visibility}`
}

function runtimeInstanceLabel(instance: ProjectInstance) {
  const status = instance.status === 'DISABLED' ? '已禁用' : instance.status
  const host = instance.host || instance.baseUrl || instance.instanceId
  return `${host} · ${status} · SDK ${instance.sdkVersion || '-'}`
}

function embeddedRuntimeConfig() {
  const config = (form.runtimeConfig?.embeddedRuntime || {}) as {
    projectCode?: string
    instanceId?: string
  }
  return config
}

function setEmbeddedRuntimeTarget(instance?: ProjectInstance) {
  if (!instance) {
    const { embeddedRuntime: _embeddedRuntime, ...rest } = form.runtimeConfig || {}
    form.runtimeConfig = rest
    return
  }
  form.runtimeConfig = {
    ...(form.runtimeConfig || {}),
    embeddedRuntime: {
      projectCode: instance.projectCode || form.projectCode,
      instanceId: instance.instanceId,
    },
  }
}

function runtimeUnavailableLabel(runtime: AgentRuntimeCapability) {
  const reason = runtime.unavailableReason || ''
  if (reason.includes('enabled-runtimes') || reason.includes('未启用')) return '未启用'
  if (reason.includes('白名单') || reason.includes('未开启')) return '需授权'
  if (reason.includes('未接入') || reason.includes('尚未接入')) return '未接入'
  return '不可用'
}

function runtimeUnavailableTagType(runtime: AgentRuntimeCapability) {
  return runtimeUnavailableLabel(runtime) === '未接入' ? 'info' : 'warning'
}

function defaultLangGraph4jGraphSpec(): AgentGraphSpec {
  return {
    code: form.keySlug || form.name || 'agent_graph',
    name: form.name || 'Agent Graph',
    mode: 'WORKFLOW',
    runtimeHint: 'LANGGRAPH4J',
    nodes: [{ id: 'llm', type: 'LLM', name: 'LLM', config: { modelInstanceId: form.modelInstanceId } }],
    edges: [
      { from: 'START', to: 'llm' },
      { from: 'llm', to: 'END' },
    ],
    entry: 'llm',
    finish: ['llm'],
  }
}

function normalizeNodeId(value: string) {
  const normalized = value.trim().replace(/\s+/g, '-')
  return normalized || 'llm'
}

function ensureLangGraph4jGraphSpec() {
  const existing = form.graphSpec
  const nodeId =
    existing?.nodes?.[0]?.id && typeof existing.nodes[0].id === 'string'
      ? normalizeNodeId(existing.nodes[0].id)
      : 'llm'
  const graphSpec: AgentGraphSpec = {
    ...(existing || defaultLangGraph4jGraphSpec()),
    code: existing?.code || form.keySlug || form.name || 'agent_graph',
    name: existing?.name || form.name || 'Agent Graph',
    mode: 'WORKFLOW',
    runtimeHint: 'LANGGRAPH4J',
    nodes: [{ id: nodeId, type: 'LLM', name: existing?.nodes?.[0]?.name || 'LLM', config: { modelInstanceId: form.modelInstanceId } }],
    edges: [
      { from: 'START', to: nodeId },
      { from: nodeId, to: 'END' },
    ],
    entry: nodeId,
    finish: [nodeId],
  }
  form.graphSpec = graphSpec
  return graphSpec
}

function syncRuntimeConfigForSave() {
  const selectedInstance = availableRuntimeInstances.value.find((item) => item.instanceId === selectedRuntimeInstanceId.value)
  if (form.runtimePlacement !== 'CENTRAL' && selectedInstance) {
    setEmbeddedRuntimeTarget(selectedInstance)
  }
  if (form.runtimeType === 'AGENTSCOPE') {
    form.graphSpec = null
    form.runtimeConfig = {
      ...(form.runtimeConfig || {}),
      agentScope: {
        maxSteps: form.maxSteps,
        type: form.type,
        useMultiAgentModel: form.useMultiAgentModel,
        tools: form.tools,
        toolRefs: form.toolRefs || [],
        skills: form.skills,
        skillRefs: form.skillRefs || [],
        pipelineAgentIds: form.pipelineAgentIds,
      },
    }
    return
  }
  if (form.runtimeType === 'LANGGRAPH4J') {
    ensureLangGraph4jGraphSpec()
    form.tools = []
    form.toolRefs = []
    form.skills = []
    form.skillRefs = []
    form.type = 'single'
    form.pipelineAgentIds = []
    form.useMultiAgentModel = false
  }
}

async function handleRuntimeTypeChange(runtimeType: AgentForm['runtimeType']) {
  const runtime = runtimeOptions.value.find((item) => item.runtimeType === runtimeType)
  if (!runtime) return

  const hasToolConfig =
    form.tools.length > 0 ||
    (form.toolRefs?.length || 0) > 0 ||
    form.skills.length > 0 ||
    (form.skillRefs?.length || 0) > 0
  const hasPipelineConfig = form.type === 'pipeline' || form.pipelineAgentIds.length > 0
  if (runtime.runtimeType === 'LANGGRAPH4J' && (hasToolConfig || hasPipelineConfig)) {
    try {
      await ElMessageBox.confirm(
        'LangGraph4j 通过 Studio GraphSpec 编排 Tool / 能力节点；当前表单里的 AgentScope Tool、能力和 Pipeline 配置会被清空，是否继续？',
        '切换运行时',
        { type: 'warning' },
      )
      form.tools = []
      form.toolRefs = []
      form.skills = []
      form.skillRefs = []
      form.type = 'single'
      form.pipelineAgentIds = []
      form.useMultiAgentModel = false
    } catch {
      form.runtimeType = previousRuntimeType.value
      return
    }
  }
  previousRuntimeType.value = runtimeType
  if (runtimeType === 'LANGGRAPH4J') {
    ensureLangGraph4jGraphSpec()
  }
}

function handleLlmProviderChange() {
  form.modelInstanceId = ''
}

function syncSelectedLlmProvider() {
  const selected = llmModelInstances.value.find((item) => item.id === form.modelInstanceId)
  selectedLlmProvider.value = selected?.provider || selectedLlmProvider.value
}

async function loadScanProjects() {
  try {
    const { data } = await getScanProjects()
    scanProjects.value = Array.isArray(data) ? data : []
    projectStore.projects = scanProjects.value
  } catch {
    scanProjects.value = []
  }
}

function applyNewProjectDefault() {
  if (!isNew) return
  const queryProjectId = Number(route.query.projectId)
  const defaultProjectId =
    Number.isFinite(queryProjectId) && queryProjectId > 0
      ? queryProjectId
      : projectStore.currentProjectId
  form.projectId = defaultProjectId ?? null
  form.projectCode = projectCodeById(form.projectId)
}

async function handleProjectChange(projectId: number | null | undefined) {
  const hasSelections = form.tools.length > 0 || form.skills.length > 0
  if (hasSelections) {
    try {
      await ElMessageBox.confirm(
        '切换项目会清空当前已选 Tool / 能力，避免跨项目引用误保存。是否继续？',
        '切换项目',
        { type: 'warning' },
      )
    } catch {
      form.projectId = previousProjectId.value
      return
    }
    form.tools = []
    form.skills = []
  }
  form.projectCode = projectCodeById(projectId)
  previousProjectId.value = projectId ?? null
  await Promise.all([loadToolOptions(), loadCapabilityOptions(), loadRuntimeInstances()])
}

async function loadAgent() {
  if (isNew) return
  pageLoading.value = true
  try {
    const { data } = await getAgent(agentId)
    Object.assign(form, {
      keySlug: data.keySlug ?? '',
      name: data.name,
      description: data.description || '',
      projectId: data.projectId ?? null,
      projectCode: data.projectCode ?? projectCodeById(data.projectId) ?? null,
      visibility: data.visibility || 'PRIVATE',
      intentType: data.intentType || '',
      systemPrompt: data.systemPrompt || '',
      tools: data.tools || [],
      toolRefs: data.toolRefs || [],
      skills: data.skills || [],
      skillRefs: data.skillRefs || [],
      modelInstanceId: data.modelInstanceId || '',
      runtimeType: data.runtimeType || 'AGENTSCOPE',
      runtimePlacement: data.runtimePlacement || 'CENTRAL',
      runtimeConfig: data.runtimeConfig || {},
      graphSpec: data.graphSpec || null,
      maxSteps: data.maxSteps || 5,
      enabled: data.enabled ?? true,
      type: data.type || 'single',
      pipelineAgentIds: data.pipelineAgentIds || [],
      knowledgeBaseGroupId: data.knowledgeBaseGroupId || '',
      promptTemplateId: data.promptTemplateId || '',
      outputSchemaType: data.outputSchemaType || '',
      triggerMode: data.triggerMode || 'all',
      useMultiAgentModel: data.useMultiAgentModel ?? false,
      extra: data.extra || {},
      allowIrreversible: data.allowIrreversible ?? false,
    })
    previousProjectId.value = form.projectId ?? null
    previousRuntimeType.value = form.runtimeType
    if (form.runtimeType === 'LANGGRAPH4J') {
      ensureLangGraph4jGraphSpec()
    }
  } catch {
    ElMessage.error('加载 Agent 失败')
  } finally {
    pageLoading.value = false
  }
}

async function loadToolOptions() {
  try {
    const { data } = await getTools({
      current: 1,
      size: 2000,
      ...(form.projectId != null ? { projectId: form.projectId } : {}),
    })
    toolOptions.value = data?.records && Array.isArray(data.records) ? data.records : []
  } catch {
    toolOptions.value = []
    ElMessage.error('加载 Tool 选项失败')
  }
}

async function loadCapabilityOptions() {
  try {
    const { data } = await listCapabilities({
      current: 1,
      size: 2000,
      ...(form.projectId != null ? { projectId: form.projectId } : {}),
    })
    capabilityOptions.value = data?.records && Array.isArray(data.records) ? data.records : []
  } catch {
    capabilityOptions.value = []
    ElMessage.error('加载能力选项失败')
  }
}

async function loadModelInstances() {
  try {
    const { data } = await getModelInstances({ modelType: 'LLM' })
    const list = data?.data ?? (Array.isArray(data) ? data : [])
    llmModelInstances.value = list.filter((item) => item.status === 'ACTIVE')
    const activeIds = new Set(llmModelInstances.value.map((item) => item.id))
    if (form.modelInstanceId && activeIds.has(form.modelInstanceId)) {
      syncSelectedLlmProvider()
    } else if (form.modelInstanceId) {
      form.modelInstanceId = ''
      selectedLlmProvider.value = ''
    }
  } catch {
    llmModelInstances.value = []
    selectedLlmProvider.value = ''
    ElMessage.error('加载模型实例失败')
  }
}

async function loadRuntimeInstances() {
  if (!form.projectCode) {
    runtimeInstances.value = []
    return
  }
  try {
    const { data } = await listRegistryProjectInstances(form.projectCode)
    runtimeInstances.value = Array.isArray(data) ? data : []
  } catch {
    runtimeInstances.value = []
  }
}

async function loadRuntimeOptions() {
  try {
    const { data } = await getAgentRuntimes()
    runtimeOptions.value = Array.isArray(data) ? data : []
    if (!form.runtimeType || !runtimeOptions.value.some((item) => item.runtimeType === form.runtimeType)) {
      form.runtimeType = runtimeOptions.value.find((item) => item.available)?.runtimeType || 'AGENTSCOPE'
    }
  } catch {
    runtimeOptions.value = [
      {
        runtimeType: 'AGENTSCOPE',
        displayName: 'AgentScope',
        available: true,
        supportsStreaming: true,
        supportsTools: true,
        supportsHandoff: false,
        supportsGraph: false,
        supportsHumanInterrupt: true,
        supportsArtifacts: false,
        supportsCodeWorkspace: false,
        supportsCloudExecution: false,
      },
    ]
  }
}

async function validateRuntimeBeforeSave() {
  try {
    syncRuntimeConfigForSave()
    if (
      form.runtimePlacement !== 'CENTRAL'
      && !availableRuntimeInstances.value.some((item) => item.instanceId === selectedRuntimeInstanceId.value)
    ) {
      ElMessage.error('请选择 Embedded Runtime 纳管实例')
      return false
    }
    const { data } = await validateAgentRuntime(form)
    if (!data.valid) {
      ElMessage.error(data.message || '运行时与模型实例校验失败')
      return false
    }
    return true
  } catch (error) {
    ElMessage.error(resolveRuntimeValidationError(error))
    return false
  }
}

function resolveRuntimeValidationError(error: unknown) {
  const response = (error as { response?: { data?: { message?: string; error?: string } } })?.response
  return response?.data?.message || response?.data?.error || '运行时校验请求失败'
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    syncRuntimeConfigForSave()
    const runtimeValid = await validateRuntimeBeforeSave()
    if (!runtimeValid) return

    if (isNew) {
      await createAgent(form)
      ElMessage.success('创建成功')
    } else {
      await updateAgent(agentId, form)
      ElMessage.success('保存成功')
    }
    router.push('/agent')
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadScanProjects()
  applyNewProjectDefault()
  await loadAgent()
  if (!previousProjectId.value) {
    previousProjectId.value = form.projectId ?? null
  }
  await Promise.all([loadToolOptions(), loadCapabilityOptions(), loadRuntimeInstances(), loadModelInstances(), loadRuntimeOptions()])
})
</script>

<style scoped lang="scss">
.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-card {
  margin-bottom: 16px;
}

.card-header-with-badge {
  display: flex;
  align-items: center;
  gap: 8px;
}

.runtime-config-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.runtime-panel {
  width: 100%;
}

.runtime-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.runtime-alert {
  margin-bottom: 16px;
}

.graph-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.graph-node {
  min-width: 88px;
  padding: 10px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  text-align: center;
  font-weight: 600;
}

.graph-node-primary {
  border-color: #818cf8;
  background: #eef2ff;
  color: #4f46e5;
}

.graph-edge {
  color: #64748b;
  font-size: 18px;
  font-weight: 700;
}

.prompt-editor {
  :deep(textarea) {
    font-family: 'Cascadia Code', 'Fira Code', 'Consolas', monospace;
    font-size: 13px;
    line-height: 1.6;
  }
}

.tag-input-area {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.item-tag {
  margin: 0;
}

.tool-select-area {
  width: 100%;
}

.tool-option {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.tool-option-desc,
.tool-hint {
  color: #64748b;
  font-size: 12px;
}

.tip-icon {
  margin-left: 6px;
  color: #64748b;
  cursor: help;
}

// ── 日间模式覆盖 ──
:global([data-theme="light"]) {
  .tool-option-desc,
  .tool-hint,
  .tip-icon {
    color: #94a3b8;
  }
}
</style>
