import type { AgentDefinition, AgentForm, AgentGraphNode, AgentGraphSpec } from '@/types/agent'
import type { CanvasNode, CanvasEdge, CanvasSnapshot, CanvasNodeKind } from '@/types/studio'

/**
 * 画布 → Agent 定义：
 * 1. tool 节点 ref → tools[]，skill 节点 ref → skills[]（各自去重、保序）；
 * 2. knowledge 节点取首个 groupId 写入 knowledgeBaseGroupId；
 * 3. 画布整体序列化成 canvasJson 存储。
 */
export function canvasToDefinition(
  base: AgentForm,
  snapshot: CanvasSnapshot,
): AgentForm {
  const tools: string[] = []
  const skills: string[] = []
  const seenTools = new Set<string>()
  const seenSkills = new Set<string>()
  let knowledgeGroupId: string | undefined

  for (const node of snapshot.nodes) {
    if (node.data.kind === 'tool' && node.data.ref) {
      if (!seenTools.has(node.data.ref)) {
        seenTools.add(node.data.ref)
        tools.push(node.data.ref)
      }
    }
    if (node.data.kind === 'skill' && node.data.ref) {
      if (!seenSkills.has(node.data.ref)) {
        seenSkills.add(node.data.ref)
        skills.push(node.data.ref)
      }
    }
    if (node.data.kind === 'knowledge' && node.data.groupId && !knowledgeGroupId) {
      knowledgeGroupId = node.data.groupId
    }
  }

  return {
    ...base,
    tools,
    skills,
    knowledgeBaseGroupId: knowledgeGroupId ?? base.knowledgeBaseGroupId ?? '',
    canvasJson: JSON.stringify(snapshot),
    graphSpec: canvasToGraphSpec(base, snapshot),
  }
}

function canvasToGraphSpec(base: AgentForm, snapshot: CanvasSnapshot): AgentGraphSpec {
  const explicitLlmNodes = snapshot.nodes.filter((node) => node.data.kind === 'llm')
  const llmNodeId = explicitLlmNodes[0]?.id || uniqueNodeId('llm', snapshot.nodes.map((node) => node.id))
  const canvasGraphNodes: AgentGraphNode[] = snapshot.nodes
    .filter((node) => node.data.kind !== 'start' && node.data.kind !== 'end' && node.data.kind !== 'llm')
    .map((node) => {
      if (node.data.kind === 'tool') {
        return {
          id: node.id,
          type: 'TOOL',
          name: node.data.label,
          ref: {
            kind: 'TOOL',
            name: node.data.ref,
            qualifiedName: node.data.ref,
          },
          config: {
            inputMapping: node.data.inputMapping || {},
            outputAlias: node.data.outputAlias,
          },
        }
      }
      if (node.data.kind === 'skill') {
        return {
          id: node.id,
          type: 'CAPABILITY',
          name: node.data.label,
          ref: {
            kind: 'CAPABILITY',
            name: node.data.ref,
            qualifiedName: node.data.ref,
          },
          config: {
            inputMapping: node.data.inputMapping || {},
            outputAlias: node.data.outputAlias,
          },
        }
      }
      return {
        id: node.id,
        type: 'TOOL',
        name: node.data.label,
        ref: {
          kind: 'TOOL',
          name: 'search_knowledge',
          qualifiedName: 'search_knowledge',
        },
        config: {
          knowledgeBaseGroupId: node.data.groupId,
          args: {
            knowledgeBaseGroupId: node.data.groupId,
          },
        },
      }
    })
  const rawEdges = snapshot.edges
    .map((edge) => ({
      from: edge.source,
      to: edge.target,
      condition: edge.condition || edge.label,
    }))
    .filter((edge) => edge.from !== 'end' && edge.to !== 'start')
  const startTargets = rawEdges.filter((edge) => edge.from === 'start').map((edge) => edge.to)
  const graphNodes: AgentGraphNode[] = [
    ...(explicitLlmNodes.length
      ? explicitLlmNodes.map((node) => ({
          id: node.id,
          type: 'LLM' as const,
          name: node.data.label || node.id,
          config: {
            modelInstanceId: base.modelInstanceId,
          },
        }))
      : [
          {
            id: llmNodeId,
            type: 'LLM' as const,
            name: 'LLM',
            config: {
              modelInstanceId: base.modelInstanceId,
            },
          },
        ]),
    ...canvasGraphNodes,
  ]
  const graphEdges = rawEdges
    .filter((edge) => edge.from !== 'start')
    .map((edge) => ({ ...edge }))
  const firstCanvasTarget = startTargets.find((target) => target !== 'end')
  if (firstCanvasTarget) {
    graphEdges.unshift({ from: llmNodeId, to: firstCanvasTarget, condition: 'always' })
  } else {
    graphEdges.unshift({ from: llmNodeId, to: 'END', condition: 'always' })
  }
  graphEdges.unshift({ from: 'START', to: llmNodeId, condition: 'always' })
  const firstNode = llmNodeId
  const finishNodes = graphNodes
    .filter((node) => graphEdges.some((edge) => edge.from === node.id && edge.to === 'end'))
    .map((node) => node.id)

  return {
    code: base.keySlug || base.name || 'agent_graph',
    name: base.name || 'Agent Graph',
    mode: 'WORKFLOW',
    runtimeHint: base.runtimeType,
    nodes: graphNodes,
    edges: graphEdges,
    entry: firstNode,
    finish: finishNodes.length > 0 ? finishNodes : firstNode ? [firstNode] : [],
  }
}

function uniqueNodeId(baseId: string, existingIds: string[]) {
  if (!existingIds.includes(baseId)) return baseId
  let index = 1
  while (existingIds.includes(`${baseId}-${index}`)) {
    index += 1
  }
  return `${baseId}-${index}`
}

/**
 * Agent 定义 → 画布：
 * - 若 `canvasJson` 存在且可解析，优先使用；
 * - 否则按 `tools[]` 与 `skills[]` 顺序生成最简画布（start → 各节点 → end）。
 */
export function definitionToCanvas(def: AgentDefinition): CanvasSnapshot {
  if (def.canvasJson) {
    try {
      const parsed = JSON.parse(def.canvasJson) as CanvasSnapshot
      if (Array.isArray(parsed.nodes) && Array.isArray(parsed.edges)) {
        return parsed
      }
    } catch {
      // 回落到自动生成
    }
  }

  if (def.graphSpec?.nodes?.length) {
    return graphSpecToCanvas(def.graphSpec)
  }

  const nodes: CanvasNode[] = []
  const edges: CanvasEdge[] = []

  nodes.push({
    id: 'start',
    type: 'start',
    position: { x: 60, y: 220 },
    data: { label: '开始', kind: 'start' },
  })

  const chain: { name: string; kind: 'tool' | 'skill' }[] = []
  for (const name of def.tools ?? []) {
    chain.push({ name, kind: 'tool' })
  }
  for (const name of def.skills ?? []) {
    chain.push({ name, kind: 'skill' })
  }
  chain.forEach((item, idx) => {
    nodes.push({
      id: `node-${idx}`,
      type: item.kind,
      position: { x: 260 + idx * 220, y: 220 },
      data: {
        label: item.name,
        kind: item.kind,
        ref: item.name,
        outputAlias: item.name.replace(/[^A-Za-z0-9_]+/g, '_'),
        inputMapping: {},
      },
    })
    edges.push({
      id: `e-start-${idx}`,
      source: idx === 0 ? 'start' : `node-${idx - 1}`,
      target: `node-${idx}`,
      condition: 'always',
      label: 'always',
    })
  })

  if (def.knowledgeBaseGroupId) {
    nodes.push({
      id: 'kb',
      type: 'knowledge',
      position: { x: 260, y: 60 },
      data: { label: def.knowledgeBaseGroupId, kind: 'knowledge', groupId: def.knowledgeBaseGroupId },
    })
  }

  const lastIdx = chain.length - 1
  nodes.push({
    id: 'end',
    type: 'end',
    position: { x: 260 + (chain.length || 1) * 220, y: 220 },
    data: { label: '结束', kind: 'end' },
  })
  edges.push({
    id: 'e-to-end',
    source: lastIdx >= 0 ? `node-${lastIdx}` : 'start',
    target: 'end',
    condition: 'always',
    label: 'always',
  })

  return { nodes, edges }
}

function graphSpecToCanvas(graphSpec: AgentGraphSpec): CanvasSnapshot {
  const nodes: CanvasNode[] = [
    {
      id: 'start',
      type: 'start',
      position: { x: 60, y: 220 },
      data: { label: '开始', kind: 'start' },
    },
  ]
  ;(graphSpec.nodes || []).forEach((node, idx) => {
    const kind = graphNodeKindToCanvas(node.type)
    const config = node.config || {}
    const inputMapping = (config.inputMapping || {}) as Record<string, string>
    nodes.push({
      id: node.id,
      type: kind,
      position: { x: 260 + idx * 220, y: 220 },
      data: {
        label: node.name || node.id,
        kind,
        ref: node.ref?.name || node.ref?.qualifiedName,
        qualifiedName: node.ref?.qualifiedName || null,
        projectCode: node.ref?.projectCode || null,
        outputAlias: typeof config.outputAlias === 'string' ? config.outputAlias : undefined,
        inputMapping,
      },
    })
  })
  nodes.push({
    id: 'end',
    type: 'end',
    position: { x: 260 + Math.max(graphSpec.nodes?.length || 1, 1) * 220, y: 220 },
    data: { label: '结束', kind: 'end' },
  })
  const edges: CanvasEdge[] = (graphSpec.edges || []).map((edge, idx) => {
    const condition = edge.condition || 'always'
    return {
      id: `graph-e-${idx}`,
      source: canvasEndpoint(edge.from),
      target: canvasEndpoint(edge.to),
      condition,
      label: condition,
    }
  })
  return { nodes, edges }
}

function graphNodeKindToCanvas(type: AgentGraphNode['type']): CanvasNodeKind {
  if (type === 'LLM') return 'llm'
  if (type === 'CAPABILITY') return 'skill'
  return 'tool'
}

function canvasEndpoint(endpoint: string) {
  if (endpoint === 'START') return 'start'
  if (endpoint === 'END') return 'end'
  return endpoint
}

const KIND_COLOR: Record<CanvasNodeKind, { bg: string; border: string }> = {
  start: { bg: '#ecf5ff', border: '#409eff' },
  end: { bg: '#f0f9eb', border: '#67c23a' },
  llm: { bg: '#eef2ff', border: '#6366f1' },
  skill: { bg: '#fdf6ec', border: '#e6a23c' },
  tool: { bg: '#f4f4f5', border: '#909399' },
  knowledge: { bg: '#fef0f0', border: '#f56c6c' },
}

export function kindColor(kind: CanvasNodeKind) {
  return KIND_COLOR[kind]
}
