<template>
  <div class="page-container model-instances">
    <div class="page-header">
      <div>
        <h2>模型实例</h2>
        <p class="page-subtitle">把 Provider、模型名、凭证和默认参数沉淀为可复用的数据库资源。</p>
      </div>
      <div class="header-actions">
        <el-select v-model="filters.modelType" clearable placeholder="类型" style="width: 150px" @change="fetchInstances">
          <el-option v-for="item in modelTypes" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="filters.provider" clearable placeholder="Provider" style="width: 160px" @change="fetchInstances">
          <el-option v-for="item in providers" :key="item.name" :label="item.name" :value="item.name" />
        </el-select>
        <el-button :icon="Refresh" @click="fetchInstances" :loading="loading">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增模型</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="instances" border stripe>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="provider" label="Provider" width="130" />
      <el-table-column prop="modelType" label="类型" width="130">
        <template #default="{ row }">
          <el-tag>{{ row.modelType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="modelName" label="模型名" min-width="160" />
      <el-table-column prop="endpointType" label="接入方式" width="170">
        <template #default="{ row }">
          <el-tag :type="row.endpointType === 'OPENAI_COMPATIBLE' ? 'success' : 'info'">
            {{ row.endpointType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="workspaceId" label="工作空间" width="120" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : row.status === 'ERROR' ? 'danger' : 'info'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
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

    <el-drawer v-model="drawerVisible" :title="editingId ? '编辑模型实例' : '新增模型实例'" size="520px">
      <el-form label-width="96px" class="instance-form">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="例如：默认通义 Max" />
        </el-form-item>
        <el-form-item label="Provider" required>
          <el-select v-model="form.provider" allow-create filterable default-first-option placeholder="选择或输入 Provider">
            <el-option v-for="item in providers" :key="item.name" :label="item.name" :value="item.name" />
            <el-option label="openai" value="openai" />
            <el-option label="ollama" value="ollama" />
            <el-option label="vllm" value="vllm" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.modelType">
            <el-option v-for="item in modelTypes" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型名" required>
          <el-input v-model="form.modelName" placeholder="qwen-max / gpt-4.1 / bge-reranker 等" />
        </el-form-item>
        <el-form-item label="接入方式">
          <el-radio-group v-model="form.endpointType">
            <el-radio-button label="BUILT_IN">内置适配</el-radio-button>
            <el-radio-button label="OPENAI_COMPATIBLE">OpenAI 兼容</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="工作空间">
          <el-input v-model="form.workspaceId" placeholder="default" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="DISABLED" value="DISABLED" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
        </el-form-item>
        <el-form-item label="凭证 JSON">
          <el-input
            v-model="credentialText"
            type="textarea"
            :rows="6"
            placeholder='{"apiKey":"sk-...","baseUrl":"https://api.example.com"}'
          />
        </el-form-item>
        <el-form-item label="默认参数">
          <el-input
            v-model="optionsText"
            type="textarea"
            :rows="5"
            placeholder='{"temperature":0.7,"max_tokens":2048}'
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import type { ModelInstance, ModelInstanceRequest, ModelType, ProviderInfo } from '@/types/model'
import {
  createModelInstance,
  deleteModelInstance,
  getModelInstances,
  getProviders,
  testModelInstance,
  updateModelInstance,
} from '@/api/model'

const modelTypes: ModelType[] = ['LLM', 'EMBEDDING', 'RERANKER', 'STT', 'TTS', 'IMAGE', 'IMAGE_GENERATION', 'VIDEO']
const providers = ref<ProviderInfo[]>([])
const instances = ref<ModelInstance[]>([])
const loading = ref(false)
const saving = ref(false)
const testingId = ref('')
const drawerVisible = ref(false)
const editingId = ref('')
const credentialText = ref('{}')
const optionsText = ref('{}')

const filters = reactive({
  modelType: '',
  provider: '',
})

const form = reactive<ModelInstanceRequest>({
  name: '',
  provider: 'tongyi',
  modelType: 'LLM',
  modelName: '',
  endpointType: 'BUILT_IN',
  workspaceId: 'default',
  status: 'ACTIVE',
  credential: {},
  defaultOptions: {},
  remark: '',
})

function resetForm() {
  editingId.value = ''
  Object.assign(form, {
    name: '',
    provider: 'tongyi',
    modelType: 'LLM',
    modelName: '',
    endpointType: 'BUILT_IN',
    workspaceId: 'default',
    status: 'ACTIVE',
    credential: {},
    defaultOptions: {},
    remark: '',
  })
  credentialText.value = '{}'
  optionsText.value = '{}'
}

async function fetchProviders() {
  const { data } = await getProviders()
  providers.value = data?.data ?? (Array.isArray(data) ? data : [])
}

async function fetchInstances() {
  loading.value = true
  try {
    const { data } = await getModelInstances({
      modelType: filters.modelType || undefined,
      provider: filters.provider || undefined,
    })
    instances.value = data?.data ?? (Array.isArray(data) ? data : [])
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  drawerVisible.value = true
}

function openEdit(row: ModelInstance) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    provider: row.provider,
    modelType: row.modelType,
    modelName: row.modelName,
    endpointType: row.endpointType,
    workspaceId: row.workspaceId,
    status: row.status,
    remark: row.remark,
  })
  credentialText.value = JSON.stringify(row.credential || {}, null, 2)
  optionsText.value = JSON.stringify(row.defaultOptions || {}, null, 2)
  drawerVisible.value = true
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
      credential: parseJsonObject(credentialText.value, '凭证 JSON'),
      defaultOptions: parseJsonObject(optionsText.value, '默认参数'),
    }
    if (editingId.value) {
      await updateModelInstance(editingId.value, payload)
    } else {
      await createModelInstance(payload)
    }
    ElMessage.success('模型实例已保存')
    drawerVisible.value = false
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
    const result = data?.data ?? data
    if (result?.success) {
      ElMessage.success(`测试通过 ${result.latencyMs}ms`)
    } else {
      ElMessage.error(result?.message || '测试失败')
    }
  } finally {
    testingId.value = ''
  }
}

onMounted(async () => {
  await fetchProviders()
  await fetchInstances()
})
</script>

<style scoped lang="scss">
.model-instances {
  .page-header {
    align-items: flex-start;
  }
}

.page-subtitle {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.instance-form {
  :deep(.el-select),
  :deep(.el-input),
  :deep(.el-textarea) {
    width: 100%;
  }

  :deep(textarea) {
    font-family: 'Cascadia Code', 'Consolas', monospace;
    font-size: 12px;
  }
}
</style>
