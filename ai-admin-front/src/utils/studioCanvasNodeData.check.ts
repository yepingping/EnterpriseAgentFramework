import type { AgentForm, AgentGraphSpec } from '@/types/agent'
import { definitionToCanvas } from './studio'

function assertPresent<T>(value: T, message: string): asserts value is NonNullable<T> {
  if (value === null || value === undefined) {
    throw new Error(message)
  }
}

function assertEqual(actual: unknown, expected: unknown, message?: string) {
  if (actual !== expected) {
    throw new Error(message ?? `Expected ${String(expected)}, got ${String(actual)}`)
  }
}

function assertDeepEqual(actual: unknown, expected: unknown, message?: string) {
  const actualJson = JSON.stringify(actual)
  const expectedJson = JSON.stringify(expected)
  if (actualJson !== expectedJson) {
    throw new Error(message ?? `Expected ${expectedJson}, got ${actualJson}`)
  }
}

const base = {
  name: 'Test Workflow',
  keySlug: 'test-workflow',
  modelInstanceId: 'llm-1',
  systemPrompt: '',
} as AgentForm

function canvasSnapshot(nodes: unknown[]) {
  return definitionToCanvas({
    ...base,
    canvasJson: JSON.stringify({ version: 2, nodes, edges: [] }),
  })
}

function canvasSnapshotWithGraphSpec(nodes: unknown[], graphSpec: AgentGraphSpec) {
  return definitionToCanvas({
    ...base,
    graphSpec,
    canvasJson: JSON.stringify({ version: 2, nodes, edges: [] }),
  })
}

const classifierSnapshot = canvasSnapshot([{
  id: 'classifier-1',
  type: 'classifier',
  position: { x: 0, y: 0 },
  data: {
    label: 'Intent Router',
    kind: 'classifier',
    classifierConfig: {
      inputExpression: 'input',
      strategy: 'HYBRID',
      classes: [{ id: 'search_intent', label: 'Search', keywords: ['查询'] }],
      defaultRoute: 'else',
      modelInstanceId: 'llm-1',
      confidenceThreshold: 0.8,
      llmPrompt: 'route intent',
    },
  },
}])

const classifierNode = classifierSnapshot.nodes.find((node) => node.id === 'classifier-1')
assertPresent(classifierNode, 'classifier node should exist')
assertEqual(classifierNode.data.configVersion, 2)
assertEqual(classifierNode.data.classifierConfig?.strategy, 'HYBRID')
assertEqual(classifierNode.data.classifierConfig?.classes?.[0]?.id, 'search_intent')

const pageActionSnapshot = canvasSnapshot([{
  id: 'page-action-1',
  type: 'pageAction',
  position: { x: 0, y: 0 },
  data: {
    label: 'Search Action',
    kind: 'pageAction',
    pageActionConfig: {
      actionKey: 'orders.search',
      projectCode: 'orders',
      pageKey: 'orders.list',
      routePattern: '/orders',
      title: 'Search',
      confirm: false,
      args: { keyword: '{{ input }}' },
      outputAlias: 'search_result',
      metadata: { source: 'ai-coding' },
    },
  },
}])

const pageActionNode = pageActionSnapshot.nodes.find((node) => node.id === 'page-action-1')
assertPresent(pageActionNode, 'page action node should exist')
assertEqual(pageActionNode.data.configVersion, 2)
assertEqual(pageActionNode.data.pageActionConfig?.actionKey, 'orders.search')
assertEqual(pageActionNode.data.pageActionConfig?.projectCode, 'orders')

const graphHydratedSnapshot = canvasSnapshotWithGraphSpec([
  {
    id: 'router',
    type: 'classifier',
    position: { x: 10, y: 20 },
    data: {
      label: 'Router From Canvas',
      kind: 'classifier',
    },
  },
  {
    id: 'search_action',
    type: 'pageAction',
    position: { x: 30, y: 40 },
    data: {
      label: 'Search From Canvas',
      kind: 'pageAction',
    },
  },
], {
  code: 'workflow',
  name: 'Test Workflow',
  mode: 'WORKFLOW',
  entry: 'input',
  nodes: [
    {
      id: 'router',
      type: 'INTENT_CLASSIFIER',
      name: 'Intent Router',
      config: {
        inputExpression: 'input',
        strategy: 'HYBRID',
        classes: [
          { id: 'query_intent', label: '查询', keywords: ['查询', '搜索'] },
          { id: 'reset_intent', label: '重置', keywords: ['重置'] },
        ],
        defaultRoute: 'else',
      },
    },
    {
      id: 'search_action',
      type: 'PAGE_ACTION',
      name: '执行查询',
      config: {
        projectCode: 'orders',
        pageKey: 'orders.list',
        routePattern: '/orders',
        actionKey: 'search',
        title: '执行查询',
        outputAlias: 'search_result',
      },
    },
  ],
  edges: [],
})

const hydratedClassifier = graphHydratedSnapshot.nodes.find((node) => node.id === 'router')
assertPresent(hydratedClassifier, 'hydrated classifier should exist')
assertEqual(hydratedClassifier.position.x, 10)
assertEqual(hydratedClassifier.position.y, 20)
assertEqual(hydratedClassifier.data.label, 'Intent Router')
assertEqual(hydratedClassifier.data.classifierConfig?.strategy, 'HYBRID')
assertDeepEqual(
  hydratedClassifier.data.classifierConfig?.classes?.map((item) => item.id),
  ['query_intent', 'reset_intent'],
)

const hydratedPageAction = graphHydratedSnapshot.nodes.find((node) => node.id === 'search_action')
assertPresent(hydratedPageAction, 'hydrated page action should exist')
assertEqual(hydratedPageAction.position.x, 30)
assertEqual(hydratedPageAction.position.y, 40)
assertEqual(hydratedPageAction.data.label, '执行查询')
assertEqual(hydratedPageAction.data.pageActionConfig?.actionKey, 'search')
assertEqual(hydratedPageAction.data.pageActionConfig?.projectCode, 'orders')

console.log('studio canvas node data checks passed')
