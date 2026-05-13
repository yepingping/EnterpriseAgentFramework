<template>
  <div class="page-container model-center">
    <div class="page-header model-hero">
      <div class="hero-copy">
        <div class="eyebrow">Model Center</div>
        <h2>模型中心</h2>
        <p class="page-subtitle">
          统一管理可被知识库、业务索引、智能体和能力编排引用的模型实例。
        </p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="fetchInstances">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click.stop.prevent="openCreate">添加模型</el-button>
      </div>
    </div>

    <div class="model-shell">
      <aside class="provider-panel">
        <div class="panel-title">
          <span>供应商</span>
          <el-tag round>{{ instances.length }}</el-tag>
        </div>
        <button
          v-for="item in providerFilters"
          :key="item.key"
          class="provider-item"
          :class="{ active: filters.provider === item.value }"
          type="button"
          @click="selectProvider(item.value)"
        >
          <span class="provider-mark" :class="{ 'has-logo': item.logo }" :style="{ background: item.logo ? undefined : item.color }">
            <img v-if="item.logo" :src="item.logo" :alt="`${item.label} logo`" loading="lazy" />
            <span v-else>{{ item.shortName }}</span>
          </span>
          <span class="provider-name">{{ item.label }}</span>
          <span class="provider-count">{{ item.count }}</span>
        </button>
      </aside>

      <main class="model-main">
        <section class="stats-grid">
          <div v-for="stat in summaryStats" :key="stat.label" class="stat-card">
            <div class="stat-label">{{ stat.label }}</div>
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-note">{{ stat.note }}</div>
          </div>
        </section>

        <section class="toolbar-card">
          <div class="filter-row">
            <el-input
              v-model="filters.keyword"
              clearable
              placeholder="搜索名称、模型名或供应商"
              class="search-input"
              :prefix-icon="Search"
            />
            <el-select v-model="filters.modelType" clearable placeholder="模型类型" class="type-select" @change="fetchInstances">
              <el-option v-for="item in modelTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="filters.status" clearable placeholder="状态" class="status-select">
              <el-option label="可用" value="ACTIVE" />
              <el-option label="停用" value="DISABLED" />
              <el-option label="异常" value="ERROR" />
            </el-select>
            <el-radio-group v-model="viewMode" class="view-toggle">
              <el-radio-button label="card">
                <el-icon><Grid /></el-icon>
              </el-radio-button>
              <el-radio-button label="table">
                <el-icon><List /></el-icon>
              </el-radio-button>
            </el-radio-group>
          </div>

          <div class="type-tabs">
            <button
              v-for="item in typeFilters"
              :key="item.value || 'all'"
              class="type-tab"
              :class="{ active: filters.modelType === item.value }"
              type="button"
              @click="selectModelType(item.value)"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
            </button>
          </div>
        </section>

        <section v-loading="loading" class="result-card">
          <template v-if="viewMode === 'card'">
            <div v-if="filteredInstances.length" class="instance-grid">
              <article v-for="item in filteredInstances" :key="item.id" class="instance-card">
                <div class="card-top">
                  <div
                    class="model-avatar"
                    :class="{ 'has-logo': providerLogo(item.provider) }"
                    :style="{ background: providerLogo(item.provider) ? undefined : providerColor(item.provider) }"
                  >
                    <img
                      v-if="providerLogo(item.provider)"
                      :src="providerLogo(item.provider)"
                      :alt="`${providerLabel(item.provider)} logo`"
                      loading="lazy"
                    />
                    <span v-else>{{ providerInitial(item.provider) }}</span>
                  </div>
                  <div class="model-title">
                    <h3>{{ item.name }}</h3>
                    <span>{{ item.provider }}</span>
                  </div>
                  <el-tag :type="statusMeta(item.status).type" round>{{ statusMeta(item.status).label }}</el-tag>
                </div>

                <div class="model-tags">
                  <el-tag effect="plain">{{ modelTypeLabel(item.modelType) }}</el-tag>
                  <el-tag effect="plain" type="info">{{ endpointLabel(item.endpointType) }}</el-tag>
                </div>

                <dl class="model-facts">
                  <div>
                    <dt>基础模型</dt>
                    <dd>{{ item.modelName }}</dd>
                  </div>
                  <div>
                    <dt>工作空间</dt>
                    <dd>{{ item.workspaceId || 'default' }}</dd>
                  </div>
                  <div>
                    <dt>更新时间</dt>
                    <dd>{{ formatDate(item.updatedAt) }}</dd>
                  </div>
                </dl>

                <p class="remark">{{ item.remark || '暂无备注' }}</p>

                <div class="card-actions">
                  <el-button link type="success" :loading="testingId === item.id" @click="handleTest(item.id)">测试</el-button>
                  <el-button link type="primary" @click="openEdit(item)">编辑</el-button>
                  <el-popconfirm title="删除这个模型实例？" @confirm="handleDelete(item.id)">
                    <template #reference>
                      <el-button link type="danger">删除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </article>
            </div>
            <el-empty v-else description="没有匹配的模型实例" />
          </template>

          <el-table v-else :data="filteredInstances" border stripe>
            <el-table-column prop="name" label="名称" min-width="170" />
            <el-table-column prop="provider" label="供应商" width="140" />
            <el-table-column prop="modelType" label="类型" width="130">
              <template #default="{ row }">
                <el-tag>{{ modelTypeLabel(row.modelType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="modelName" label="基础模型" min-width="180" />
            <el-table-column prop="endpointType" label="接入方式" width="150">
              <template #default="{ row }">
                <el-tag type="info">{{ endpointLabel(row.endpointType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="workspaceId" label="工作空间" width="120" />
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusMeta(row.status).type">{{ statusMeta(row.status).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" :loading="testingId === row.id" @click="handleTest(row.id)">测试</el-button>
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-popconfirm title="删除这个模型实例？" @confirm="handleDelete(row.id)">
                  <template #reference>
                    <el-button link type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </main>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑模型实例' : '添加模型实例'"
      width="720px"
      class="instance-dialog"
      destroy-on-close
      append-to-body
    >
      <el-form label-width="104px" class="instance-form">
        <div class="form-grid">
          <el-form-item label="名称" required>
            <el-input v-model="form.name" placeholder="例如：默认通义 Max" />
          </el-form-item>
          <el-form-item label="供应商" required>
            <el-select v-model="form.provider" filterable allow-create default-first-option>
              <el-option v-for="item in providerOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型类型" required>
            <el-select v-model="form.modelType">
              <el-option v-for="item in modelTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="基础模型" required>
            <el-input v-model="form.modelName" placeholder="qwen-max / gpt-4.1 / bge-reranker" />
          </el-form-item>
          <el-form-item label="接入方式">
            <el-select v-model="form.endpointType">
              <el-option label="OpenAI 兼容" value="OPENAI_COMPATIBLE" />
              <el-option label="内置适配" value="BUILT_IN" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="可用" value="ACTIVE" />
              <el-option label="停用" value="DISABLED" />
              <el-option label="异常" value="ERROR" />
            </el-select>
          </el-form-item>
          <el-form-item label="工作空间">
            <el-input v-model="form.workspaceId" placeholder="default" />
          </el-form-item>
        </div>

        <section class="friendly-config">
          <div class="config-section-title">
            <span>连接配置</span>
            <small>{{ currentProviderConfig.help }}</small>
          </div>
          <div class="form-grid">
            <el-form-item :label="currentProviderConfig.apiKeyLabel">
              <el-input
                v-model="credentialForm.apiKey"
                type="password"
                show-password
                :placeholder="currentProviderConfig.apiKeyPlaceholder"
              />
            </el-form-item>
            <el-form-item label="接口地址">
              <el-input v-model="credentialForm.baseUrl" :placeholder="currentProviderConfig.baseUrl" />
            </el-form-item>
          </div>
        </section>

        <section class="friendly-config">
          <div class="config-section-title">
            <span>默认参数</span>
            <small>不填则使用模型服务默认值</small>
          </div>
          <div class="form-grid">
            <el-form-item label="温度">
              <el-input-number v-model="optionForm.temperature" :min="0" :max="2" :step="0.1" controls-position="right" />
            </el-form-item>
            <el-form-item label="最大输出">
              <el-input-number v-model="optionForm.maxTokens" :min="1" :max="200000" :step="512" controls-position="right" />
            </el-form-item>
          </div>
        </section>

        <el-collapse class="advanced-config">
          <el-collapse-item title="高级配置（可选）" name="advanced">
            <el-alert
              class="form-alert"
              type="info"
              show-icon
              :closable="false"
              title="高级配置会合并到普通表单生成的配置中，通常不用填写。"
            />
            <el-form-item label="额外凭证">
              <el-input
                v-model="credentialText"
                type="textarea"
                :rows="4"
                placeholder='{"authHeader":"Authorization","authPrefix":"Bearer "}'
              />
            </el-form-item>
            <el-form-item label="额外参数">
              <el-input
                v-model="optionsText"
                type="textarea"
                :rows="4"
                placeholder='{"top_p":0.9}'
              />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="记录用途、限额、适用业务等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound,
  Connection,
  Cpu,
  Grid,
  List,
  MagicStick,
  Microphone,
  Picture,
  Plus,
  Refresh,
  Search,
  VideoCamera,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'
import type { ModelInstance, ModelInstanceRequest, ModelInstanceStatus, ModelType } from '@/types/model'
import {
  createModelInstance,
  deleteModelInstance,
  getModelInstances,
  testModelInstance,
  updateModelInstance,
} from '@/api/model'

type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary'
type ViewMode = 'card' | 'table'

interface ModelTypeOption {
  value: ModelType
  label: string
  icon: Component
}

interface ModelTypeFilter {
  value: ModelType | ''
  label: string
  icon: Component
  count: number
}

interface ProviderCredentialConfig {
  apiKeyLabel: string
  apiKeyPlaceholder: string
  baseUrl: string
  help: string
}

const modelTypeOptions: ModelTypeOption[] = [
  { value: 'LLM', label: '大语言模型', icon: ChatDotRound },
  { value: 'EMBEDDING', label: '向量模型', icon: Connection },
  { value: 'RERANKER', label: '重排模型', icon: Cpu },
  { value: 'STT', label: '语音识别', icon: Microphone },
  { value: 'TTS', label: '语音合成', icon: Microphone },
  { value: 'IMAGE', label: '视觉模型', icon: Picture },
  { value: 'IMAGE_GENERATION', label: '图片生成', icon: MagicStick },
  { value: 'VIDEO', label: '视频模型', icon: VideoCamera },
]

const providerOptions = [
  { label: 'OpenAI', value: 'openai' },
  { label: '通义千问', value: 'tongyi' },
  { label: 'DeepSeek', value: 'deepseek' },
  { label: 'Ollama', value: 'ollama' },
  { label: 'vLLM', value: 'vllm' },
  { label: '智谱 AI', value: 'zhipu' },
  { label: '月之暗面 Kimi', value: 'kimi' },
  { label: 'Anthropic', value: 'anthropic' },
  { label: 'Gemini', value: 'gemini' },
  { label: '硅基流动', value: 'siliconflow' },
  { label: 'Amazon Bedrock', value: 'amazon-bedrock' },
  { label: 'Azure OpenAI', value: 'azure-openai' },
  { label: '腾讯混元', value: 'tencent-hunyuan' },
  { label: '百度千帆', value: 'qianfan' },
  { label: '火山方舟', value: 'volcengine' },
]

const instances = ref<ModelInstance[]>([])
const loading = ref(false)
const saving = ref(false)
const testingId = ref('')
const dialogVisible = ref(false)
const editingId = ref('')
const credentialText = ref('{}')
const optionsText = ref('{}')
const viewMode = ref<ViewMode>('card')

const credentialForm = reactive({
  apiKey: '',
  baseUrl: '',
})

const optionForm = reactive<{
  temperature: number | undefined
  maxTokens: number | undefined
}>({
  temperature: undefined,
  maxTokens: undefined,
})

const filters = reactive({
  keyword: '',
  modelType: '' as ModelType | '',
  provider: '',
  status: '' as ModelInstanceStatus | '',
})

const form = reactive<ModelInstanceRequest>({
  name: '',
  provider: 'openai',
  modelType: 'LLM',
  modelName: '',
  endpointType: 'OPENAI_COMPATIBLE',
  workspaceId: 'default',
  status: 'ACTIVE',
  credential: {},
  defaultOptions: {},
  remark: '',
})

const providerPalette = ['#2563eb', '#0891b2', '#7c3aed', '#059669', '#d97706', '#e11d48', '#475569']

const providerIconDomains: Record<string, string> = {
  openai: 'openai.com',
  tongyi: 'aliyun.com',
  dashscope: 'aliyun.com',
  anthropic: 'anthropic.com',
  'amazon-bedrock': 'aws.amazon.com',
  'azure-openai': 'azure.microsoft.com',
  deepseek: 'deepseek.com',
  gemini: 'gemini.google.com',
  kimi: 'moonshot.cn',
  moonshot: 'moonshot.cn',
  siliconflow: 'siliconflow.cn',
  'tencent-hunyuan': 'cloud.tencent.com',
  qianfan: 'cloud.baidu.com',
  volcengine: 'volcengine.com',
  ollama: 'ollama.com',
  vllm: 'vllm.ai',
  zhipu: 'zhipuai.cn',
}

const providerCredentialConfigs: Record<string, ProviderCredentialConfig> = {
  openai: {
    apiKeyLabel: 'API Key',
    apiKeyPlaceholder: 'sk-...',
    baseUrl: 'https://api.openai.com/v1',
    help: '填写 OpenAI API Key 即可调用。',
  },
  tongyi: {
    apiKeyLabel: 'DashScope Key',
    apiKeyPlaceholder: 'sk-...',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    help: '阿里云百炼/通义千问只需要填写 API Key。',
  },
  dashscope: {
    apiKeyLabel: 'DashScope Key',
    apiKeyPlaceholder: 'sk-...',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    help: '阿里云百炼/通义千问只需要填写 API Key。',
  },
  deepseek: {
    apiKeyLabel: 'API Key',
    apiKeyPlaceholder: 'sk-...',
    baseUrl: 'https://api.deepseek.com',
    help: '填写 DeepSeek API Key 即可调用。',
  },
  kimi: {
    apiKeyLabel: 'Moonshot Key',
    apiKeyPlaceholder: 'sk-...',
    baseUrl: 'https://api.moonshot.ai/v1',
    help: '填写 Moonshot / Kimi API Key 即可调用。',
  },
  moonshot: {
    apiKeyLabel: 'Moonshot Key',
    apiKeyPlaceholder: 'sk-...',
    baseUrl: 'https://api.moonshot.ai/v1',
    help: '填写 Moonshot / Kimi API Key 即可调用。',
  },
  siliconflow: {
    apiKeyLabel: 'API Key',
    apiKeyPlaceholder: 'sk-...',
    baseUrl: 'https://api.siliconflow.cn/v1',
    help: '填写硅基流动 API Key 即可调用。',
  },
  gemini: {
    apiKeyLabel: 'Gemini Key',
    apiKeyPlaceholder: 'AIza...',
    baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai',
    help: '使用 Gemini OpenAI 兼容接口。',
  },
  anthropic: {
    apiKeyLabel: 'Anthropic Key',
    apiKeyPlaceholder: 'sk-ant-...',
    baseUrl: 'https://api.anthropic.com/v1',
    help: '填写 Anthropic API Key；需确认网关兼容 Anthropic OpenAI 接口格式。',
  },
  'azure-openai': {
    apiKeyLabel: 'Azure Key',
    apiKeyPlaceholder: 'Azure OpenAI API Key',
    baseUrl: 'https://{resource}.openai.azure.com/openai/v1',
    help: '替换 resource，并填写 Azure OpenAI Key。',
  },
  'tencent-hunyuan': {
    apiKeyLabel: 'API Key',
    apiKeyPlaceholder: '腾讯混元 API Key',
    baseUrl: 'https://api.hunyuan.cloud.tencent.com/v1',
    help: '填写腾讯混元 OpenAI 兼容 API Key。',
  },
  qianfan: {
    apiKeyLabel: 'API Key',
    apiKeyPlaceholder: '百度千帆 API Key',
    baseUrl: 'https://qianfan.baidubce.com/v2',
    help: '填写百度千帆 API Key。',
  },
  volcengine: {
    apiKeyLabel: '方舟 Key',
    apiKeyPlaceholder: '火山方舟 API Key',
    baseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
    help: '填写火山方舟 API Key。',
  },
  ollama: {
    apiKeyLabel: 'API Key',
    apiKeyPlaceholder: '本地 Ollama 可不填',
    baseUrl: 'http://localhost:11434/v1',
    help: '本地 Ollama 通常只需要确认接口地址。',
  },
  vllm: {
    apiKeyLabel: 'API Key',
    apiKeyPlaceholder: '无鉴权可填 EMPTY',
    baseUrl: 'http://localhost:8000/v1',
    help: '自部署 vLLM 通常只需要确认接口地址。',
  },
}

const filteredInstances = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return instances.value.filter((item) => {
    const matchKeyword = !keyword || [item.name, item.provider, item.modelName, item.remark]
      .filter(Boolean)
      .some((text) => String(text).toLowerCase().includes(keyword))
    const matchProvider = !filters.provider || item.provider === filters.provider
    const matchStatus = !filters.status || item.status === filters.status
    return matchKeyword && matchProvider && matchStatus
  })
})

const currentProviderConfig = computed<ProviderCredentialConfig>(() => providerCredentialConfig(form.provider))

const summaryStats = computed(() => {
  const activeCount = instances.value.filter((item) => item.status === 'ACTIVE').length
  const providerCount = new Set(instances.value.map((item) => item.provider)).size
  const typeCount = new Set(instances.value.map((item) => item.modelType)).size
  return [
    { label: '模型实例', value: instances.value.length, note: '已纳入统一引用' },
    { label: '可用实例', value: activeCount, note: '可被业务调用' },
    { label: '供应商', value: providerCount, note: '接入来源' },
    { label: '能力类型', value: typeCount, note: 'LLM / 向量 / 重排等' },
  ]
})

const providerFilters = computed(() => {
  const counts = new Map<string, number>()
  instances.value.forEach((item) => counts.set(item.provider, (counts.get(item.provider) || 0) + 1))
  const providers = Array.from(counts.entries()).sort((a, b) => a[0].localeCompare(b[0]))
  return [
    {
      key: 'all',
      value: '',
      label: '全部模型',
      shortName: 'ALL',
      logo: '',
      count: instances.value.length,
      color: '#334155',
    },
    ...providers.map(([provider, count], index) => ({
      key: provider,
      value: provider,
      label: providerLabel(provider),
      shortName: providerInitial(provider),
      logo: providerLogo(provider),
      count,
      color: providerPalette[index % providerPalette.length],
    })),
  ]
})

const typeFilters = computed<ModelTypeFilter[]>(() => {
  const all: ModelTypeFilter = { value: '', label: '全部', icon: Grid, count: instances.value.length }
  return [
    all,
    ...modelTypeOptions.map((option) => ({
      ...option,
      count: instances.value.filter((item) => item.modelType === option.value).length,
    })),
  ]
})

function unwrapApiData<T>(data: T | { data?: T } | undefined): T | undefined {
  if (data && typeof data === 'object' && 'data' in data) {
    return (data as { data?: T }).data
  }
  return data as T | undefined
}

function providerLabel(provider: string) {
  return providerOptions.find((item) => item.value === provider)?.label || provider
}

function providerInitial(provider: string) {
  const label = providerLabel(provider)
  return label.slice(0, 3).toUpperCase()
}

function providerLogo(provider: string) {
  const domain = providerIconDomains[provider]
  if (!domain) return ''
  return `https://www.google.com/s2/favicons?domain=${domain}&sz=64`
}

function providerCredentialConfig(provider: string): ProviderCredentialConfig {
  return providerCredentialConfigs[provider] || {
    apiKeyLabel: 'API Key',
    apiKeyPlaceholder: '请输入 API Key',
    baseUrl: '',
    help: '填写供应商提供的 API Key 和 OpenAI 兼容接口地址。',
  }
}

function applyProviderDefaults(provider: string) {
  const config = providerCredentialConfig(provider)
  if (!credentialForm.baseUrl) {
    credentialForm.baseUrl = config.baseUrl
  }
}

function populateFriendlyConfig(credential: Record<string, unknown>, options: Record<string, unknown>) {
  credentialForm.apiKey = stringValue(credential.apiKey ?? credential.token)
  credentialForm.baseUrl = stringValue(credential.baseUrl ?? credential.apiBase ?? credential.endpoint)
  optionForm.temperature = numberValue(options.temperature)
  optionForm.maxTokens = numberValue(options.max_tokens ?? options.maxTokens)
}

function buildCredentialPayload() {
  const advanced = parseJsonObject(credentialText.value, '额外凭证')
  const payload: Record<string, unknown> = { ...advanced }
  if (credentialForm.baseUrl.trim()) {
    payload.baseUrl = credentialForm.baseUrl.trim()
  }
  if (credentialForm.apiKey.trim()) {
    payload.apiKey = credentialForm.apiKey.trim()
  }
  return payload
}

function buildOptionsPayload() {
  const advanced = parseJsonObject(optionsText.value, '额外参数')
  const payload: Record<string, unknown> = { ...advanced }
  if (optionForm.temperature !== undefined) {
    payload.temperature = optionForm.temperature
  }
  if (optionForm.maxTokens !== undefined) {
    payload.max_tokens = optionForm.maxTokens
  }
  return payload
}

function stringValue(value: unknown) {
  return typeof value === 'string' ? value : ''
}

function numberValue(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined
}

function omitKeys(source: Record<string, unknown>, keys: string[]) {
  const omitted = new Set(keys)
  return Object.fromEntries(Object.entries(source).filter(([key]) => !omitted.has(key)))
}

function providerColor(provider: string) {
  const index = providerFilters.value.findIndex((item) => item.value === provider)
  return providerPalette[Math.max(index - 1, 0) % providerPalette.length]
}

function modelTypeLabel(type: ModelType) {
  return modelTypeOptions.find((item) => item.value === type)?.label || type
}

function endpointLabel(endpoint?: string) {
  if (endpoint === 'BUILT_IN') return '内置适配'
  return 'OpenAI 兼容'
}

function statusMeta(status: ModelInstanceStatus): { label: string; type: TagType } {
  if (status === 'ACTIVE') return { label: '可用', type: 'success' }
  if (status === 'ERROR') return { label: '异常', type: 'danger' }
  return { label: '停用', type: 'info' }
}

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

function selectProvider(provider: string) {
  filters.provider = provider
  fetchInstances()
}

function selectModelType(type: ModelType | '') {
  filters.modelType = type
  fetchInstances()
}

function resetForm() {
  editingId.value = ''
  Object.assign(form, {
    name: '',
    provider: filters.provider || 'openai',
    modelType: filters.modelType || 'LLM',
    modelName: '',
    endpointType: 'OPENAI_COMPATIBLE',
    workspaceId: 'default',
    status: 'ACTIVE',
    credential: {},
    defaultOptions: {},
    remark: '',
  })
  credentialForm.apiKey = ''
  credentialForm.baseUrl = providerCredentialConfig(form.provider).baseUrl
  optionForm.temperature = 0.7
  optionForm.maxTokens = 4096
  credentialText.value = '{}'
  optionsText.value = '{}'
}

async function fetchInstances() {
  loading.value = true
  try {
    const { data } = await getModelInstances({
      modelType: filters.modelType || undefined,
      provider: filters.provider || undefined,
    })
    const list = unwrapApiData<ModelInstance[]>(data)
    instances.value = Array.isArray(list) ? list : []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ModelInstance) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    provider: row.provider,
    modelType: row.modelType,
    modelName: row.modelName,
    endpointType: row.endpointType || 'OPENAI_COMPATIBLE',
    workspaceId: row.workspaceId || 'default',
    status: row.status,
    remark: row.remark || '',
  })
  const credential = row.credential || {}
  const options = row.defaultOptions || {}
  populateFriendlyConfig(credential, options)
  if (!credentialForm.baseUrl) {
    applyProviderDefaults(row.provider)
  }
  credentialText.value = JSON.stringify(omitKeys(credential, ['apiKey', 'token', 'baseUrl', 'apiBase', 'endpoint']), null, 2)
  optionsText.value = JSON.stringify(omitKeys(options, ['temperature', 'max_tokens', 'maxTokens']), null, 2)
  dialogVisible.value = true
}

function parseJsonObject(text: string, label: string): Record<string, unknown> {
  try {
    const value = JSON.parse(text || '{}')
    if (!value || Array.isArray(value) || typeof value !== 'object') {
      throw new Error(`${label} 必须是 JSON 对象`)
    }
    return value
  } catch (err) {
    throw new Error(`${label} 格式不正确：${(err as Error).message}`)
  }
}

async function handleSave() {
  saving.value = true
  try {
    const payload: ModelInstanceRequest = {
      ...form,
      credential: buildCredentialPayload(),
      defaultOptions: buildOptionsPayload(),
    }
    if (editingId.value) {
      await updateModelInstance(editingId.value, payload)
    } else {
      await createModelInstance(payload)
    }
    ElMessage.success('模型实例已保存')
    dialogVisible.value = false
    await fetchInstances()
  } catch (err) {
    ElMessage.error((err as Error).message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: string) {
  await deleteModelInstance(id)
  ElMessage.success('模型实例已删除')
  await fetchInstances()
}

async function handleTest(id: string) {
  testingId.value = id
  try {
    const { data } = await testModelInstance(id)
    const result = unwrapApiData(data)
    if (result?.success) {
      const latency = result.latencyMs ? `，耗时 ${result.latencyMs}ms` : ''
      ElMessage.success(`${result.message || '测试通过'}${latency}`)
    } else {
      ElMessage.error(result?.message || '测试失败')
    }
  } catch (err) {
    ElMessage.error((err as Error).message || '测试失败')
  } finally {
    testingId.value = ''
  }
}

watch(
  () => form.provider,
  (provider, previousProvider) => {
    const previousDefault = previousProvider ? providerCredentialConfig(previousProvider).baseUrl : ''
    if (!credentialForm.baseUrl || credentialForm.baseUrl === previousDefault) {
      credentialForm.baseUrl = providerCredentialConfig(provider).baseUrl
    }
  },
)

onMounted(fetchInstances)
</script>

<style scoped lang="scss">
.model-center {
  color: var(--text-primary);
}

.model-hero {
  align-items: center;
}

.hero-copy {
  min-width: 0;
}

.eyebrow {
  margin-bottom: 6px;
  color: var(--accent-color);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.page-subtitle {
  max-width: 680px;
  margin: 8px 0 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.model-shell {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.provider-panel,
.toolbar-card,
.result-card,
.stat-card {
  border: 1px solid var(--border-glass);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-card) 92%, transparent);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(16px);
}

.provider-panel {
  position: sticky;
  top: 16px;
  padding: 14px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  color: var(--text-primary);
  font-weight: 800;
}

.provider-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 46px;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.18s ease;
}

.provider-item:hover,
.provider-item.active {
  border-color: color-mix(in srgb, var(--accent-color) 28%, var(--border-glass));
  background: color-mix(in srgb, var(--accent-color) 9%, transparent);
  color: var(--text-primary);
}

.provider-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 30px;
  border-radius: 8px;
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  overflow: hidden;
}

.provider-mark.has-logo {
  background: color-mix(in srgb, var(--bg-secondary) 86%, #ffffff 14%);
  box-shadow:
    inset 0 0 0 1px color-mix(in srgb, var(--border-glass) 72%, transparent),
    0 8px 16px rgba(15, 23, 42, 0.08);
}

.provider-mark img {
  display: block;
  width: 22px;
  height: 22px;
  object-fit: contain;
  border-radius: 5px;
}

.provider-name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 700;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.provider-count {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.model-main {
  min-width: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.stat-card {
  padding: 16px;
}

.stat-label,
.stat-note {
  color: var(--text-secondary);
  font-size: 12px;
}

.stat-value {
  margin: 8px 0 4px;
  color: var(--text-primary);
  font-size: 26px;
  font-weight: 800;
}

.toolbar-card {
  padding: 14px;
  margin-bottom: 14px;
}

.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1 1 260px;
  min-width: 220px;
}

.type-select,
.status-select {
  width: 150px;
}

.view-toggle {
  margin-left: auto;
}

.view-toggle :deep(.el-radio-button__inner) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  padding: 8px 0;
}

.type-tabs {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.type-tab {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex: 0 0 auto;
  min-height: 36px;
  padding: 8px 10px;
  border: 1px solid var(--border-glass);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-tertiary) 58%, transparent);
  color: var(--text-secondary);
  cursor: pointer;
  font-weight: 700;
  transition: all 0.18s ease;
}

.type-tab strong {
  color: var(--text-muted);
  font-size: 12px;
}

.type-tab:hover,
.type-tab.active {
  border-color: color-mix(in srgb, var(--accent-color) 36%, var(--border-glass));
  background: color-mix(in srgb, var(--accent-color) 11%, transparent);
  color: var(--text-primary);
}

.result-card {
  min-height: 360px;
  padding: 16px;
}

.instance-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.instance-card {
  display: flex;
  flex-direction: column;
  min-height: 250px;
  padding: 16px;
  border: 1px solid var(--border-glass);
  border-radius: 8px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--bg-card) 92%, rgba(99, 102, 241, 0.05)), var(--bg-card));
  transition: all 0.18s ease;
}

.instance-card:hover {
  border-color: color-mix(in srgb, var(--accent-color) 34%, var(--border-glass));
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.10);
  transform: translateY(-2px);
}

.card-top {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.model-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  color: #fff;
  font-size: 11px;
  font-weight: 900;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
  overflow: hidden;
}

.model-avatar.has-logo {
  background: color-mix(in srgb, var(--bg-secondary) 84%, #ffffff 16%);
  box-shadow:
    inset 0 0 0 1px color-mix(in srgb, var(--border-glass) 70%, transparent),
    0 10px 20px rgba(15, 23, 42, 0.10);
}

.model-avatar img {
  display: block;
  width: 28px;
  height: 28px;
  object-fit: contain;
  border-radius: 6px;
}

.model-title {
  min-width: 0;
}

.model-title h3 {
  overflow: hidden;
  margin: 0 0 4px;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-title span,
.remark {
  color: var(--text-secondary);
  font-size: 12px;
}

.model-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.model-facts {
  display: grid;
  gap: 10px;
  margin: 16px 0 0;
}

.model-facts div {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
}

.model-facts dt {
  color: var(--text-muted);
  font-size: 12px;
}

.model-facts dd {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remark {
  min-height: 18px;
  margin: 14px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--border-glass);
}

.instance-form {
  max-height: calc(82vh - 144px);
  overflow-y: auto;
  padding-right: 8px;
}

:deep(.instance-dialog) {
  max-width: calc(100vw - 32px);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.form-alert {
  margin: 4px 0 18px;
}

.friendly-config {
  margin-bottom: 18px;
  padding: 14px 14px 2px;
  border: 1px solid var(--border-glass);
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-secondary) 76%, #ffffff 24%);
}

.config-section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.config-section-title span {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 800;
}

.config-section-title small {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
  text-align: right;
}

.advanced-config {
  margin-bottom: 16px;
  border: 1px solid var(--border-glass);
  border-radius: 8px;
  padding: 0 14px 12px;
}

.advanced-config :deep(.el-collapse-item__header) {
  font-weight: 750;
}

.instance-form :deep(.el-select) {
  width: 100%;
}

.instance-form :deep(.el-input-number) {
  width: 100%;
}

.instance-form :deep(textarea) {
  font-family: 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
}

@media (max-width: 1180px) {
  .model-shell {
    grid-template-columns: 1fr;
  }

  .provider-panel {
    position: static;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 8px;
  }

  .panel-title {
    grid-column: 1 / -1;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .stats-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .view-toggle {
    margin-left: 0;
  }

  .instance-grid {
    grid-template-columns: 1fr;
  }

  .type-select,
  .status-select {
    width: 100%;
  }
}
</style>
