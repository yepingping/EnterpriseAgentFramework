<template>
  <div class="node-specific-panel">
    <el-divider>大模型推理</el-divider>
    <el-form-item label="模型实例">
      <el-select v-model="config.modelInstanceId" filterable placeholder="选择模型实例" style="width: 100%">
        <el-option v-for="item in modelOptions" :key="item.id" :label="`${item.name} / ${item.modelName}`" :value="item.id" />
      </el-select>
    </el-form-item>
    <el-form-item label="系统提示词">
      <el-input v-model="config.systemPrompt" type="textarea" :rows="5" placeholder="节点级 system prompt；为空时运行时回退 Agent 配置" />
    </el-form-item>
    <el-form-item label="用户提示词">
      <el-input v-model="config.userPrompt" type="textarea" :rows="5" placeholder="例如：请基于 {{ input }} 和 {{ knowledge_output }} 回答" />
    </el-form-item>
    <el-form-item label="上下文变量">
      <el-select v-model="config.contextVariables" multiple filterable allow-create default-first-option style="width: 100%">
        <el-option v-for="item in variableOptions" :key="item" :label="item" :value="item" />
      </el-select>
    </el-form-item>
    <el-form-item label="输出格式">
      <el-segmented v-model="config.outputFormat" :options="['text', 'json']" />
    </el-form-item>
    <el-form-item label="模型参数">
      <el-input :model-value="formatMap(paramsAsText)" type="textarea" :rows="4" placeholder="temperature = 0.2&#10;maxTokens = 1024" @update:model-value="updateParams" />
    </el-form-item>
    <template v-if="config.outputFormat === 'json'">
      <div class="field-table-head">
        <strong>输出结构</strong>
        <el-button size="small" type="primary" plain @click="addField(config.outputSchema || (config.outputSchema = []))">添加字段</el-button>
      </div>
      <div v-for="(field, index) in config.outputSchema" :key="index" class="field-row schema-row">
        <el-input v-model="field.name" placeholder="字段名" />
        <el-select v-model="field.type" style="width: 110px">
          <el-option label="string" value="string" />
          <el-option label="number" value="number" />
          <el-option label="integer" value="integer" />
          <el-option label="boolean" value="boolean" />
          <el-option label="object" value="object" />
          <el-option label="array" value="array" />
        </el-select>
        <el-switch v-model="field.required" active-text="必填" />
        <el-input v-model="field.defaultValue" placeholder="默认值" />
        <el-button text type="danger" @click="config.outputSchema?.splice(index, 1)">删除</el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CanvasNodeData, LlmNodeConfig } from '@/types/studio'
import type { ModelInstance } from '@/types/model'
import { addField, formatMap, parseMap } from './panelUtils'

const props = defineProps<{
  data: CanvasNodeData
  modelOptions: ModelInstance[]
  variableOptions: string[]
}>()

const config = computed<LlmNodeConfig>(() => {
  props.data.llmConfig ||= {
    modelInstanceId: '',
    systemPrompt: '',
    userPrompt: '{{ input }}',
    contextVariables: ['input'],
    modelParams: {},
    outputFormat: 'text',
    outputSchema: [],
  }
  return props.data.llmConfig
})

const paramsAsText = computed(() => {
  const out: Record<string, string> = {}
  Object.entries(config.value.modelParams || {}).forEach(([key, value]) => {
    out[key] = String(value)
  })
  return out
})

function updateParams(text: string) {
  const parsed = parseMap(text)
  const params: Record<string, string | number | boolean> = {}
  Object.entries(parsed).forEach(([key, value]) => {
    if (value === 'true' || value === 'false') params[key] = value === 'true'
    else if (value !== '' && Number.isFinite(Number(value))) params[key] = Number(value)
    else params[key] = value
  })
  config.value.modelParams = params
}
</script>
