<template>
  <component
    :is="panel"
    v-if="panel"
    :data="data"
    :model-options="modelOptions"
    :knowledge-options="knowledgeOptions"
    :variable-options="variableOptions"
    :credential-options="credentialOptions"
    :param-source-hints="paramSourceHints"
    :project-id="projectId"
    :project-code="projectCode"
    :options="toolLikeOptions"
    @credential-created="$emit('credentialCreated', $event)"
  />
  <div v-else class="node-specific-panel">
    <el-divider>节点配置</el-divider>
    <el-alert title="该节点没有专属配置项" type="info" :closable="false" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CanvasNodeData } from '@/types/studio'
import type { ModelInstance } from '@/types/model'
import type { KnowledgeBase } from '@/types/knowledge'
import type { ToolInfo } from '@/types/tool'
import type { CapabilityInfo } from '@/types/capability'
import type { WorkflowCredential } from '@/types/workflowCredential'
import type { ApiGraphParamSourceHint } from '@/api/apiGraph'
import LlmConfigPanel from './LlmConfigPanel.vue'
import KnowledgeConfigPanel from './KnowledgeConfigPanel.vue'
import HttpConfigPanel from './HttpConfigPanel.vue'
import ParameterConfigPanel from './ParameterConfigPanel.vue'
import ConditionConfigPanel from './ConditionConfigPanel.vue'
import ToolConfigPanel from './ToolConfigPanel.vue'
import AnswerConfigPanel from './AnswerConfigPanel.vue'
import CodeConfigPanel from './CodeConfigPanel.vue'
import IntentClassifierConfigPanel from './IntentClassifierConfigPanel.vue'
import VariableAggregateConfigPanel from './VariableAggregateConfigPanel.vue'
import ApprovalConfigPanel from './ApprovalConfigPanel.vue'
import LoopConfigPanel from './LoopConfigPanel.vue'
import KnowledgeWriteConfigPanel from './KnowledgeWriteConfigPanel.vue'
import DocumentExtractConfigPanel from './DocumentExtractConfigPanel.vue'
import McpConfigPanel from './McpConfigPanel.vue'

const props = defineProps<{
  data: CanvasNodeData
  modelOptions: ModelInstance[]
  knowledgeOptions: KnowledgeBase[]
  toolOptions: ToolInfo[]
  capabilityOptions: CapabilityInfo[]
  variableOptions: string[]
  credentialOptions: WorkflowCredential[]
  paramSourceHints: ApiGraphParamSourceHint[]
  projectId?: number | null
  projectCode?: string | null
}>()

defineEmits<{
  credentialCreated: [credential: WorkflowCredential]
}>()

const registry = {
  llm: LlmConfigPanel,
  knowledge: KnowledgeConfigPanel,
  http: HttpConfigPanel,
  parameter: ParameterConfigPanel,
  condition: ConditionConfigPanel,
  answer: AnswerConfigPanel,
  code: CodeConfigPanel,
  classifier: IntentClassifierConfigPanel,
  aggregate: VariableAggregateConfigPanel,
  approval: ApprovalConfigPanel,
  loop: LoopConfigPanel,
  knowledgeWrite: KnowledgeWriteConfigPanel,
  documentExtract: DocumentExtractConfigPanel,
  mcp: McpConfigPanel,
  tool: ToolConfigPanel,
  skill: ToolConfigPanel,
}

const panel = computed(() => registry[props.data.kind as keyof typeof registry])
const toolLikeOptions = computed(() => props.data.kind === 'skill' ? props.capabilityOptions : props.toolOptions)
</script>

<style lang="scss">
.node-specific-panel {
  .field-table-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin: 10px 0;

    strong {
      color: #334155;
      font-size: 13px;
    }
  }

  .field-row,
  .condition-row,
  .condition-group-head {
    display: grid;
    grid-template-columns: minmax(92px, 1fr) auto auto minmax(100px, 0.8fr) minmax(120px, 1.1fr) auto;
    gap: 8px;
    align-items: center;
    margin-bottom: 8px;
  }

  .schema-row {
    grid-template-columns: minmax(120px, 1fr) auto auto minmax(110px, 0.8fr) auto;
  }

  .condition-group {
    margin-bottom: 12px;
    padding: 10px;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    background: #f8fafc;
  }

  .condition-group-head {
    grid-template-columns: 1fr auto auto;
  }

  .condition-row {
    grid-template-columns: minmax(110px, 1fr) auto minmax(100px, 1fr) auto;
  }

  .classifier-row,
  .aggregate-row {
    display: grid;
    grid-template-columns: minmax(90px, 0.8fr) minmax(120px, 1fr) minmax(160px, 1.4fr) auto;
    gap: 8px;
    align-items: center;
    margin-bottom: 8px;
  }

  .aggregate-row {
    grid-template-columns: minmax(110px, 0.8fr) minmax(180px, 1.6fr) auto;
  }

  .tool-params,
  .param-hints {
    margin: 12px 0;
  }

  .param-hint-row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto auto;
    gap: 8px;
    align-items: center;
    padding: 8px 0;
    border-top: 1px solid #e2e8f0;
    font-size: 12px;

    div {
      min-width: 0;
    }

    strong,
    span {
      display: block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    span {
      color: #64748b;
    }
  }

  .tool-param-row {
    display: grid;
    grid-template-columns: minmax(120px, 1fr) 80px 80px;
    gap: 8px;
    padding: 6px 0;
    border-top: 1px solid #e2e8f0;
    font-size: 12px;

    span,
    em {
      color: #64748b;
      font-style: normal;
    }
  }
}
</style>
