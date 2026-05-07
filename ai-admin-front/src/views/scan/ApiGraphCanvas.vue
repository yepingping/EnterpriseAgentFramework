<template>
  <div class="api-graph-canvas">
    <div class="graph-toolbar">
      <div>
        <div class="toolbar-title">接口知识图谱</div>
        <div class="toolbar-subtitle">展示入参、出参和 DTO/VO 之间的引用关系</div>
      </div>
      <div class="toolbar-actions">
        <el-button size="small" :loading="loading" @click="refresh">刷新</el-button>
        <el-button size="small" :loading="rebuilding" @click="rebuild">重建图谱</el-button>
        <el-button size="small" type="warning" plain :loading="regenerating" @click="regenerate">重新生成</el-button>
        <el-button size="small" :loading="inferring" @click="inferModels">推断模型边</el-button>
        <el-button size="small" :loading="inferRR" @click="inferRequestResponse">推断请求/响应边</el-button>
        <el-switch v-model="linkMode" active-text="连线模式" inactive-text="" size="small" />
        <el-switch v-model="showCandidates" active-text="候选边" inactive-text="" size="small" />
        <el-switch v-model="showDtoPanel" active-text="数据模型" inactive-text="" size="small" />
      </div>
    </div>

    <div class="graph-hint">
      <span v-if="linkMode && linkSource">已选源节点：{{ linkSource.label }}，点击目标节点完成连线</span>
      <span v-else-if="linkMode">请点击一个接口或参数作为源节点</span>
      <span v-else>点击接口、参数或连线可查看右侧详情；开启「数据模型」可在画布右侧展示 DTO/VO 面板；开启连线模式可人工补充引用关系。</span>
    </div>

    <div
      ref="graphScrollRef"
      v-loading="loading"
      class="graph-container"
      @scroll="onGraphScroll"
      @wheel="onGraphWheel"
    >
      <div v-if="!graphLayout.apiCards.length && !graphLayout.dtoCards.length" class="empty-state">
        <el-empty description="暂无接口图谱数据，请先扫描或重建图谱" :image-size="88" />
      </div>
      <div
        v-else
        class="graph-stage"
        :style="{ width: `${graphLayout.width}px`, height: `${graphLayout.height}px` }"
      >
        <div class="legend-panel">
          <div class="legend-title">图例</div>
          <div class="legend-line">
            <span class="edge-sample request" />请求引用（入参引用其它接口的出参）
          </div>
          <div class="legend-line">
            <span class="edge-sample response" />响应引用（出参引用其它接口的出参）
          </div>
          <div class="legend-line">
            <span class="edge-sample model" />数据模型引用（共用数据结构）
          </div>
          <div class="legend-node"><span class="node-badge api">API</span>接口</div>
          <div class="legend-node"><span class="field-icon in">入</span>入参（请求）</div>
          <div class="legend-node"><span class="field-icon out">出</span>出参（响应）</div>
          <div class="legend-node"><span class="dto-icon">◇</span>数据模型（DTO/VO）</div>
        </div>

        <svg class="edge-layer" :width="graphLayout.width" :height="graphLayout.height">
          <defs>
            <marker
              v-for="kind in edgeKinds"
              :id="`api-graph-arrow-${kind}`"
              :key="kind"
              markerWidth="8"
              markerHeight="8"
              refX="7"
              refY="4"
              orient="auto"
              markerUnits="strokeWidth"
            >
              <path d="M 0 0 L 8 4 L 0 8 z" :class="['arrow-head', kind]" />
            </marker>
          </defs>
          <g
            v-for="edge in graphLayout.edges"
            :key="edge.id"
            class="edge-group"
            :class="{ candidate: edge.raw.status === 'CANDIDATE' }"
            @click.stop="selectEdge(edge.raw)"
          >
            <path class="edge-hit" :d="edge.path" />
            <path
              class="edge-path"
              :class="edge.visualKind"
              :d="edge.path"
              :marker-end="`url(#api-graph-arrow-${edge.visualKind})`"
            />
            <text
              v-if="edge.label"
              class="edge-label"
              :x="edge.labelX"
              :y="edge.labelY"
            >
              {{ edge.label }}
            </text>
          </g>
        </svg>

        <div
          v-for="card in graphLayout.apiCards"
          :key="card.node.id"
          class="api-card"
          :class="{ active: selectedNode?.id === card.node.id, dragging: draggingApiId === card.node.id }"
          :style="nodeStyle(card)"
          role="button"
          tabindex="0"
          @click="handleNodeClick(card.node)"
          @pointerdown="startApiDrag($event, card)"
        >
          <div class="api-card-header">
            <span class="node-badge api">API</span>
            <div class="api-title-block">
              <strong>{{ card.node.label }}</strong>
              <span>{{ card.method }} {{ card.path }}</span>
            </div>
          </div>
          <div class="api-fields">
            <div class="field-column">
              <div class="field-title">入参</div>
              <el-tooltip
                v-for="field in card.inFields"
                :key="field.node.id"
                placement="top"
                :show-after="280"
                :disabled="!field.description"
                :content="field.description"
                popper-class="api-graph-field-desc-tooltip"
              >
                <button
                  class="field-row"
                  type="button"
                  @click.stop="handleNodeClick(field.node)"
                >
                  <span class="field-icon in">入</span>
                  <span class="field-name">{{ field.name }}</span>
                  <span class="field-type">{{ field.type }}</span>
                </button>
              </el-tooltip>
              <div v-if="!card.inFields.length" class="field-empty">无</div>
            </div>
            <div class="field-column">
              <div class="field-title">出参</div>
              <el-tooltip
                v-for="field in card.outFields"
                :key="field.node.id"
                placement="top"
                :show-after="280"
                :disabled="!field.description"
                :content="field.description"
                popper-class="api-graph-field-desc-tooltip"
              >
                <button
                  class="field-row"
                  type="button"
                  @click.stop="handleNodeClick(field.node)"
                >
                  <span class="field-icon out">出</span>
                  <span class="field-name">{{ field.name }}</span>
                  <span class="field-type">{{ field.type }}</span>
                </button>
              </el-tooltip>
              <div v-if="!card.outFields.length" class="field-empty">无</div>
            </div>
          </div>
        </div>

        <section v-if="showDtoPanel && graphLayout.dtoCards.length" class="dto-panel" :style="dtoPanelStyle">
          <div class="dto-panel-title">数据模型（DTO/VO）</div>
          <button
            v-for="card in graphLayout.dtoCards"
            :key="card.node.id"
            class="dto-card"
            :class="{ active: selectedNode?.id === card.node.id }"
            type="button"
            @click="handleNodeClick(card.node)"
          >
            <div class="dto-title"><span class="dto-icon">◇</span>{{ card.node.label }}</div>
            <div v-for="field in card.fields" :key="field.key" class="dto-field">
              <span>{{ field.name }}</span>
              <span>{{ field.type }}</span>
            </div>
            <div v-if="!card.fields.length" class="dto-field dim">
              <span>rawType</span>
              <span>{{ shortType(card.node.typeName || parseProps(card.node).rawType || '-') }}</span>
            </div>
          </button>
        </section>

        <div class="relationship-note">
          <strong>关系说明</strong>
          <span>蓝色表示请求参数依赖，绿色表示响应字段流向，紫色虚线表示接口共用 DTO/VO 数据结构。</span>
        </div>
      </div>
    </div>

    <el-drawer v-model="detailVisible" :title="detailTitle" size="360px" direction="rtl">
      <template v-if="selectedNode">
        <div class="detail-section">
          <el-tag size="small" :type="nodeTagType(selectedNode.kind)">{{ nodeKindLabel(selectedNode.kind) }}</el-tag>
          <h3>{{ selectedNode.label }}</h3>
          <p v-if="selectedNode.typeName" class="detail-muted">{{ selectedNode.typeName }}</p>
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item v-for="item in selectedNodeDetails" :key="item.label" :label="item.label">
            {{ item.value }}
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="selectedNode.kind === 'API'" class="api-relation-panel">
          <div class="api-relation-header">
            <span class="api-relation-title">接口关联图谱</span>
            <span class="api-relation-zoom-actions">
              <el-button
                text
                type="primary"
                size="small"
                :icon="ZoomIn"
                aria-label="放大查看关联图谱"
                @click="openRelationZoom"
              />
            </span>
          </div>
          <p class="detail-muted">仅展示与当前接口已有连线关系的接口、DTO/VO。</p>
          <div v-if="!selectedApiMiniGraph.cards.length" class="relation-empty">暂无关联关系</div>
          <div
            v-else
            class="relation-mini-graph"
            :style="{ height: `${selectedApiMiniGraph.height}px` }"
          >
            <svg class="relation-mini-edges" :width="selectedApiMiniGraph.width" :height="selectedApiMiniGraph.height">
              <g v-for="edge in selectedApiMiniGraph.edges" :key="edge.id">
                <path class="relation-mini-edge" :class="edge.kind" :d="edge.path" />
                <text class="relation-mini-label" :x="edge.labelX" :y="edge.labelY">{{ edge.label }}</text>
              </g>
            </svg>
            <button
              v-for="card in selectedApiMiniGraph.cards"
              :key="`${card.kind}-${card.node.id}`"
              class="relation-mini-card"
              :class="[card.kind.toLowerCase(), { current: card.current }]"
              :style="{ left: `${card.x}px`, top: `${card.y}px`, width: `${card.width}px` }"
              type="button"
              @click="selectRelatedNode(card.node)"
            >
              <span class="relation-mini-title">
                <span v-if="card.kind === 'API'" class="node-badge api">API</span>
                <span v-else class="dto-icon">◇</span>
                <strong>{{ card.node.label }}</strong>
              </span>
              <span v-if="card.kind === 'API'" class="relation-mini-subtitle">
                {{ parseProps(card.node).httpMethod || 'API' }} {{ parseProps(card.node).endpointPath || parseProps(card.node).contextPath || '-' }}
              </span>
              <span v-else class="relation-mini-subtitle">{{ shortType(card.node.typeName || parseProps(card.node).rawType || '-') }}</span>
            </button>
          </div>
        </div>
        <div v-if="selectedNode.kind === 'FIELD_IN'" class="param-link-panel">
          <el-divider content-position="left">参数来源</el-divider>
          <p class="detail-muted">先选模块、再选接口与其它出参（可多层级子参），会创建“出参 → 当前入参”的请求引用线。</p>
          <el-cascader
            v-model="selectedSourceOutputPath"
            class="param-cascader"
            :popper-class="paramCascaderPopperClass"
            :popper-options="paramCascaderPopperOptions"
            placement="bottom-end"
            filterable
            clearable
            placeholder="选择来源出参"
            :options="sourceOutputOptions"
            :props="paramCascaderProps"
            :show-all-levels="true"
            :disabled="creatingRelationEdge"
            @change="createSelectedOutputToInputEdge"
          />
        </div>
        <div v-else-if="selectedNode.kind === 'FIELD_OUT'" class="param-link-panel">
          <el-divider content-position="left">连接到入参</el-divider>
          <p class="detail-muted">先选模块、再选接口与其它入参后，会创建“当前出参 → 入参”的请求引用线。</p>
          <el-cascader
            v-model="selectedTargetInputPath"
            class="param-cascader"
            :popper-class="paramCascaderPopperClass"
            :popper-options="paramCascaderPopperOptions"
            placement="bottom-end"
            filterable
            clearable
            placeholder="选择目标入参"
            :options="targetInputOptions"
            :props="paramCascaderProps"
            :show-all-levels="true"
            :disabled="creatingRelationEdge"
            @change="createOutputToSelectedInputEdge"
          />
        </div>
      </template>
      <template v-else-if="selectedEdge">
        <div class="detail-section">
          <el-tag size="small" :type="edgeTagType(selectedEdge.kind)">{{ edgeKindLabel(selectedEdge.kind) }}</el-tag>
          <h3>{{ edgeKindLabel(selectedEdge.kind) }}</h3>
          <p class="detail-muted">{{ edgeNodeLabel(selectedEdge.sourceNodeId) }} → {{ edgeNodeLabel(selectedEdge.targetNodeId) }}</p>
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="来源">{{ selectedEdge.source }}</el-descriptions-item>
          <el-descriptions-item label="置信度">{{ selectedEdge.confidence ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ selectedEdge.status ?? '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedEdge.inferStrategy" label="推断策略">
            {{ selectedEdge.inferStrategy }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedEdge.note" label="备注">{{ selectedEdge.note }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="selectedEdge.status === 'CANDIDATE'" class="detail-actions">
          <el-button size="small" type="success" @click="confirmEdge(selectedEdge)">确认候选边</el-button>
          <el-button size="small" type="danger" @click="rejectEdge(selectedEdge)">拒绝</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="edgeDialogVisible" title="选择引用关系" width="360px">
      <el-radio-group v-model="pendingEdgeKind" class="edge-kind-options">
        <el-radio value="REQUEST_REF">请求引用（蓝色）</el-radio>
        <el-radio value="RESPONSE_REF">响应引用（绿色）</el-radio>
        <el-radio value="MODEL_REF">数据模型引用（紫色虚线）</el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="cancelPendingEdge">取消</el-button>
        <el-button type="primary" :loading="creatingEdge" @click="submitPendingEdge">确定连线</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="relationZoomVisible"
      class="relation-zoom-dialog"
      :title="relationZoomTitle"
      width="min(92vw, 920px)"
      align-center
      :z-index="3200"
      append-to-body
      destroy-on-close
      @closed="onRelationZoomClosed"
    >
      <div
        v-if="selectedApiMiniGraph.cards.length"
        class="relation-zoom-viewport"
        :style="{
          width: `${Math.ceil(selectedApiMiniGraph.width * relationZoomScale) + 32}px`,
          height: `${Math.ceil(selectedApiMiniGraph.height * relationZoomScale) + 32}px`,
        }"
      >
        <div
          class="relation-zoom-scaler"
          :style="{
            width: `${selectedApiMiniGraph.width}px`,
            height: `${selectedApiMiniGraph.height}px`,
            transform: `scale(${relationZoomScale})`,
          }"
        >
          <svg class="relation-mini-edges" :width="selectedApiMiniGraph.width" :height="selectedApiMiniGraph.height">
            <g v-for="edge in selectedApiMiniGraph.edges" :key="`zoom-${edge.id}`">
              <path class="relation-mini-edge" :class="edge.kind" :d="edge.path" />
              <text class="relation-mini-label" :x="edge.labelX" :y="edge.labelY">{{ edge.label }}</text>
            </g>
          </svg>
          <button
            v-for="card in selectedApiMiniGraph.cards"
            :key="`zoom-${card.kind}-${card.node.id}`"
            class="relation-mini-card"
            :class="[card.kind.toLowerCase(), { current: card.current }]"
            :style="{ left: `${card.x}px`, top: `${card.y}px`, width: `${card.width}px` }"
            type="button"
            @click="selectRelatedNodeFromZoom(card.node)"
          >
            <span class="relation-mini-title">
              <span v-if="card.kind === 'API'" class="node-badge api">API</span>
              <span v-else class="dto-icon">◇</span>
              <strong>{{ card.node.label }}</strong>
            </span>
            <span v-if="card.kind === 'API'" class="relation-mini-subtitle">
              {{ parseProps(card.node).httpMethod || 'API' }} {{ parseProps(card.node).endpointPath || parseProps(card.node).contextPath || '-' }}
            </span>
            <span v-else class="relation-mini-subtitle">{{ shortType(card.node.typeName || parseProps(card.node).rawType || '-') }}</span>
          </button>
        </div>
      </div>
      <el-empty v-else description="暂无关联关系" :image-size="72" />
    </el-dialog>

    <Teleport to="body">
      <div
        v-show="hScrollDockVisible"
        class="api-graph-hscroll-dock"
        :style="hScrollDockPositionStyle"
        aria-hidden="true"
      >
        <div
          ref="hScrollTrackRef"
          class="api-graph-hscroll-track"
          @scroll.passive="onHScrollDockScroll"
        >
          <div class="api-graph-hscroll-spacer" :style="{ width: `${hScrollContentWidth}px` }" />
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ZoomIn } from '@element-plus/icons-vue'
import {
  confirmApiGraphCandidate,
  getApiGraphSnapshot,
  inferApiGraphModelEdges,
  inferApiGraphRequestResponseEdges,
  listApiGraphCandidates,
  parseApiGraphSnapshot,
  rebuildApiGraph,
  regenerateApiGraph,
  rejectApiGraphCandidate,
  saveApiGraphLayout,
  upsertApiGraphEdge,
  type ApiGraphEdge,
  type ApiGraphEdgeKind,
  type ApiGraphNode,
  type ApiGraphSnapshot,
} from '@/api/apiGraph'
import axios from 'axios'

const props = withDefaults(
  defineProps<{ projectId: number, panelExpanded?: boolean }>(),
  { panelExpanded: true },
)

const graphScrollRef = ref<HTMLElement | null>(null)
const hScrollTrackRef = ref<HTMLElement | null>(null)
const hScrollContentWidth = ref(0)
const hScrollNeedsDock = ref(false)
/** 折叠「接口图谱」时不显示底部横向条；画布无需横向滚动时也不显示 */
const hScrollDockVisible = computed(
  () => props.panelExpanded !== false && hScrollNeedsDock.value,
)

const hScrollDockLeft = ref(0)
const hScrollDockWidth = ref(0)
const hScrollDockPositionStyle = computed(() => ({
  left: `${hScrollDockLeft.value}px`,
  width: `${Math.max(0, hScrollDockWidth.value)}px`,
}))

let syncingHorizontalScroll = false
let graphScrollResizeObserver: ResizeObserver | null = null
let hScrollDockLayoutRaf = 0
let dockScrollParent: HTMLElement | null = null
let dockScrollHandler: (() => void) | null = null

function nearestScrollableAncestor(el: HTMLElement): HTMLElement {
  let p: HTMLElement | null = el.parentElement
  while (p) {
    const st = getComputedStyle(p)
    if (/(auto|scroll|overlay)/.test(st.overflowY) || /(auto|scroll|overlay)/.test(st.overflowX))
      return p
    p = p.parentElement
  }
  return document.documentElement
}

function unbindDockScrollListeners() {
  if (dockScrollParent && dockScrollHandler) {
    dockScrollParent.removeEventListener('scroll', dockScrollHandler)
  }
  dockScrollParent = null
  dockScrollHandler = null
}

function bindDockScrollListeners() {
  unbindDockScrollListeners()
  const el = graphScrollRef.value
  if (!el)
    return
  const sp = nearestScrollableAncestor(el)
  dockScrollHandler = () => scheduleHScrollDockLayout()
  sp.addEventListener('scroll', dockScrollHandler, { passive: true })
  dockScrollParent = sp
}

function scheduleHScrollDockLayout() {
  if (hScrollDockLayoutRaf)
    return
  hScrollDockLayoutRaf = requestAnimationFrame(() => {
    hScrollDockLayoutRaf = 0
    refreshHScrollDockMetrics()
  })
}

function refreshHScrollDockMetrics() {
  const el = graphScrollRef.value
  if (!el) {
    hScrollContentWidth.value = 0
    hScrollNeedsDock.value = false
    hScrollDockLeft.value = 0
    hScrollDockWidth.value = 0
    return
  }
  hScrollContentWidth.value = el.scrollWidth
  hScrollNeedsDock.value = el.scrollWidth > el.clientWidth + 1
  const r = el.getBoundingClientRect()
  hScrollDockLeft.value = r.left
  hScrollDockWidth.value = r.width
}

function onGraphScroll() {
  if (syncingHorizontalScroll)
    return
  const main = graphScrollRef.value
  const dock = hScrollTrackRef.value
  if (!main || !dock)
    return
  syncingHorizontalScroll = true
  dock.scrollLeft = main.scrollLeft
  syncingHorizontalScroll = false
  scheduleHScrollDockLayout()
}

function onHScrollDockScroll() {
  if (syncingHorizontalScroll)
    return
  const main = graphScrollRef.value
  const dock = hScrollTrackRef.value
  if (!main || !dock)
    return
  syncingHorizontalScroll = true
  main.scrollLeft = dock.scrollLeft
  syncingHorizontalScroll = false
}

function onGraphWheel(e: WheelEvent) {
  const el = graphScrollRef.value
  if (!el)
    return
  const maxX = el.scrollWidth - el.clientWidth
  if (maxX <= 0)
    return
  const horiz = e.deltaX + (e.shiftKey ? e.deltaY : 0)
  if (!horiz)
    return
  e.preventDefault()
  el.scrollLeft = Math.max(0, Math.min(maxX, el.scrollLeft + horiz))
  syncingHorizontalScroll = true
  if (hScrollTrackRef.value)
    hScrollTrackRef.value.scrollLeft = el.scrollLeft
  syncingHorizontalScroll = false
}

const loading = ref(false)
const rebuilding = ref(false)
const regenerating = ref(false)
const inferring = ref(false)
const inferRR = ref(false)
const linkMode = ref(false)
const showCandidates = ref(false)
const showDtoPanel = ref(false)
const linkSource = ref<ApiGraphNode | null>(null)
const snapshot = ref<ApiGraphSnapshot>({ nodes: [], edges: [], layouts: [] })
const candidateEdges = ref<ApiGraphEdge[]>([])
const selectedNode = ref<ApiGraphNode | null>(null)
const selectedEdge = ref<ApiGraphEdge | null>(null)
const detailVisible = ref(false)
const relationZoomVisible = ref(false)
const relationZoomScale = ref(1.25)
const edgeDialogVisible = ref(false)
const creatingEdge = ref(false)
const creatingRelationEdge = ref(false)
const pendingEdgeKind = ref<ApiGraphEdgeKind>('REQUEST_REF')
const pendingEdge = ref<{ source: ApiGraphNode, target: ApiGraphNode } | null>(null)
const selectedSourceOutputPath = ref<CascaderPath>([])
const selectedTargetInputPath = ref<CascaderPath>([])
const manualApiPositions = ref<Record<number, Point>>({})
const draggingApiId = ref<number | null>(null)
const suppressNextCardClick = ref(false)
let dragState: {
  nodeId: number
  startClientX: number
  startClientY: number
  startX: number
  startY: number
  moved: boolean
} | null = null

const EDGE_COLORS: Record<string, string> = {
  REQUEST_REF: '#3b82f6',
  RESPONSE_REF: '#22c55e',
  MODEL_REF: '#a855f7',
  BELONGS_TO: '#64748b',
}

const NODE_COLORS: Record<string, string> = {
  API: '#6366f1',
  FIELD_IN: '#3b82f6',
  FIELD_OUT: '#22c55e',
  DTO: '#a855f7',
  MODULE: '#64748b',
}

const CARD_WIDTH = 268
const API_CARD_GAP_X = 118
const API_CARD_GAP_Y = 48
const API_CARD_TOP = 138
const API_CARD_LEFT = 330
const CARD_HEADER_HEIGHT = 60
const FIELD_TITLE_HEIGHT = 30
const FIELD_ROW_HEIGHT = 30
const FIELD_START_OFFSET = CARD_HEADER_HEIGHT + FIELD_TITLE_HEIGHT + 18
/** 入/出参侧线在行上略偏上时，锚点整体下移（与 FIELD_ROW_HEIGHT/2 一起表示行垂直中心） */
const FIELD_SIDE_ANCHOR_Y_BIAS = 6

function apiFieldEdgeAnchorY(cardTopY: number, fieldIndex: number): number {
  return (
    cardTopY
    + FIELD_START_OFFSET
    + fieldIndex * FIELD_ROW_HEIGHT
    + FIELD_ROW_HEIGHT / 2
    + FIELD_SIDE_ANCHOR_Y_BIAS
  )
}
const DTO_PANEL_WIDTH = 260
const DTO_CARD_HEIGHT = 128
const DTO_CARD_GAP = 16
const DTO_PANEL_TOP = 76
const LEGEND_WIDTH = 240
/** 入参/出参连线先水平伸出卡片边缘，避免竖段被卡片遮挡 */
const EDGE_FIELD_CARD_STUB = 28

type Point = { x: number, y: number }
type DetailItem = { label: string, value: string | number | boolean }
type FieldVM = { node: ApiGraphNode, name: string, type: string, path: string, description: string }
type CascaderPath = Array<string | number>
type ApiCardVM = {
  node: ApiGraphNode
  x: number
  y: number
  width: number
  height: number
  method: string
  path: string
  inFields: FieldVM[]
  outFields: FieldVM[]
}
type DtoFieldVM = { key: string, name: string, type: string }
type ParamCascaderOption = {
  value: string | number
  label: string
  disabled?: boolean
  children?: ParamCascaderOption[]
}
type ParamPathTreeNode = {
  segment: string
  path: string
  node?: ApiGraphNode
  children: Map<string, ParamPathTreeNode>
}
type ApiRelationItem = {
  kind: 'API' | 'DTO'
  node: ApiGraphNode
  edgeKinds: string[]
}
type MiniGraphCard = {
  kind: 'API' | 'DTO'
  node: ApiGraphNode
  x: number
  y: number
  width: number
  current: boolean
}
type MiniGraphEdge = {
  id: string
  kind: EdgeVisualKind
  path: string
  label: string
  labelX: number
  labelY: number
}
type MiniGraphLayout = {
  width: number
  height: number
  cards: MiniGraphCard[]
  edges: MiniGraphEdge[]
}
type DtoCardVM = {
  node: ApiGraphNode
  x: number
  y: number
  width: number
  height: number
  fields: DtoFieldVM[]
}
type EdgeVisualKind = 'REQUEST_REF' | 'RESPONSE_REF' | 'MODEL_REF'
type EdgeVM = {
  id: string
  raw: ApiGraphEdge
  visualKind: EdgeVisualKind
  path: string
  label: string
  labelX: number
  labelY: number
}

const edgeKinds: EdgeVisualKind[] = ['REQUEST_REF', 'RESPONSE_REF', 'MODEL_REF']
const paramCascaderProps = {
  checkStrictly: true,
  emitPath: true,
  expandTrigger: 'hover' as const,
}
/** 抽屉内级联：避免 flip/overflow 重定位抖动；列宽见全局样式 `.api-graph-param-cascader-popper` */
const paramCascaderPopperClass = 'api-graph-param-cascader-popper'
const paramCascaderPopperOptions = {
  strategy: 'fixed' as const,
  modifiers: [
    { name: 'flip', enabled: false },
    { name: 'preventOverflow', enabled: false },
  ],
}
const nodeMap = computed(() => new Map(snapshot.value.nodes.map((node) => [node.id, node])))
const allEdges = computed(() => [...snapshot.value.edges, ...candidateEdges.value].filter((edge) => edge.enabled !== false))
const apiLabelMap = computed(() => {
  const map = new Map<number, string>()
  snapshot.value.nodes
    .filter((node) => node.kind === 'API')
    .forEach((node) => map.set(node.id, node.label))
  return map
})
const fieldNodes = computed(() => snapshot.value.nodes.filter((node) => node.kind === 'FIELD_IN' || node.kind === 'FIELD_OUT'))
const fieldChildrenByParent = computed(() => {
  const map = new Map<number, ApiGraphNode[]>()
  fieldNodes.value.forEach((node) => {
    if (node.parentId != null) {
      if (!map.has(node.parentId)) map.set(node.parentId, [])
      map.get(node.parentId)!.push(node)
    }
  })
  return map
})
const sourceOutputOptions = computed(() => {
  if (!selectedNode.value || selectedNode.value.kind !== 'FIELD_IN') return []
  return buildParamCascaderOptions('FIELD_OUT', selectedNode.value.refId)
})
const targetInputOptions = computed(() => {
  if (!selectedNode.value || selectedNode.value.kind !== 'FIELD_OUT') return []
  return buildParamCascaderOptions('FIELD_IN', selectedNode.value.refId)
})

const graphLayout = computed(() => {
  const showDto = showDtoPanel.value
  const apiNodes = snapshot.value.nodes.filter((node) => node.kind === 'API')
  const dtoNodes = showDto ? snapshot.value.nodes.filter((node) => node.kind === 'DTO') : []
  const fieldNodes = snapshot.value.nodes.filter((node) => node.kind === 'FIELD_IN' || node.kind === 'FIELD_OUT')
  const fieldsByParent = groupBy(fieldNodes, (node) => node.parentId ?? node.refId ?? 0)
  const apiCards: ApiCardVM[] = []
  const dtoCards: DtoCardVM[] = []
  const anchorMap = new Map<number, Point>()

  const columnCount = 3
  const rowHeights: number[] = []
  const preparedCards = apiNodes.map((node, index) => {
    const row = Math.floor(index / columnCount)
    const col = index % columnCount
    const apiFields = fieldsByParent.get(node.id) ?? []
    const inFields = apiFields.filter((field) => field.kind === 'FIELD_IN').map(toFieldVM)
    const outFields = apiFields.filter((field) => field.kind === 'FIELD_OUT').map(toFieldVM)
    const rowCount = Math.max(inFields.length, outFields.length, 1)
    const height = FIELD_START_OFFSET + rowCount * FIELD_ROW_HEIGHT + 18
    rowHeights[row] = Math.max(rowHeights[row] ?? 0, height)
    return {
      node,
      row,
      col,
      width: CARD_WIDTH,
      height,
      method: String(parseProps(node).httpMethod || 'API'),
      path: String(parseProps(node).endpointPath || parseProps(node).contextPath || '-'),
      inFields,
      outFields,
    }
  })
  const rowTops = rowHeights.reduce<number[]>((tops, height, index) => {
    tops[index] = index === 0 ? API_CARD_TOP : tops[index - 1] + rowHeights[index - 1] + API_CARD_GAP_Y
    return tops
  }, [])

  preparedCards.forEach((prepared) => {
    const manualPosition = manualApiPositions.value[prepared.node.id]
    const card: ApiCardVM = {
      node: prepared.node,
      x: manualPosition?.x ?? API_CARD_LEFT + prepared.col * (CARD_WIDTH + API_CARD_GAP_X),
      y: manualPosition?.y ?? rowTops[prepared.row],
      width: prepared.width,
      height: prepared.height,
      method: prepared.method,
      path: prepared.path,
      inFields: prepared.inFields,
      outFields: prepared.outFields,
    }
    apiCards.push(card)
    anchorMap.set(prepared.node.id, { x: card.x + card.width / 2, y: card.y + 34 })
    prepared.inFields.forEach((field, fieldIndex) => {
      anchorMap.set(field.node.id, { x: card.x + 18, y: apiFieldEdgeAnchorY(card.y, fieldIndex) })
    })
    prepared.outFields.forEach((field, fieldIndex) => {
      anchorMap.set(field.node.id, { x: card.x + card.width - 18, y: apiFieldEdgeAnchorY(card.y, fieldIndex) })
    })
  })

  const maxApiRight = Math.max(...apiCards.map((card) => card.x + card.width), API_CARD_LEFT + CARD_WIDTH)
  const dtoPanelX = maxApiRight + 72
  const dtoPanelHeight = dtoNodes.length * (DTO_CARD_HEIGHT + DTO_CARD_GAP) + 70
  const dtoFieldBuckets = buildDtoFieldBuckets(dtoNodes, fieldNodes, fieldsByParent)
  if (showDto) {
    dtoNodes.forEach((node, index) => {
      const card: DtoCardVM = {
        node,
        x: dtoPanelX + 18,
        y: DTO_PANEL_TOP + 56 + index * (DTO_CARD_HEIGHT + DTO_CARD_GAP),
        width: DTO_PANEL_WIDTH - 36,
        height: DTO_CARD_HEIGHT,
        fields: dtoFieldBuckets.get(node.label) ?? [],
      }
      dtoCards.push(card)
      anchorMap.set(node.id, { x: card.x, y: card.y + 30 })
    })
  }
  fieldNodes.forEach((node) => {
    if (anchorMap.has(node.id)) return
    const inherited = findNearestAnchor(node, anchorMap)
    if (inherited) {
      anchorMap.set(node.id, inherited)
    }
  })

  const width = showDto
    ? Math.max(dtoPanelX + DTO_PANEL_WIDTH + 34, maxApiRight + 220, 1120)
    : Math.max(maxApiRight + 220, 1120)
  const apiBottom = Math.max(...apiCards.map((card) => card.y + card.height), API_CARD_TOP + 300)
  const height = showDto
    ? Math.max(apiBottom + 92, DTO_PANEL_TOP + dtoPanelHeight + 28, 620)
    : Math.max(apiBottom + 92, 620)

  const edges = allEdges.value
    .map((edge) => toEdgeVM(edge, anchorMap))
    .filter((edge): edge is EdgeVM => Boolean(edge))

  return {
    width,
    height,
    apiCards,
    dtoCards,
    dtoPanel: showDto
      ? { x: dtoPanelX, y: DTO_PANEL_TOP, width: DTO_PANEL_WIDTH, height: dtoPanelHeight }
      : { x: 0, y: 0, width: 0, height: 0 },
    edges,
  }
})

const dtoPanelStyle = computed(() => ({
  left: `${graphLayout.value.dtoPanel.x}px`,
  top: `${graphLayout.value.dtoPanel.y}px`,
  width: `${graphLayout.value.dtoPanel.width}px`,
  minHeight: `${graphLayout.value.dtoPanel.height}px`,
}))

const detailTitle = computed(() => selectedNode.value ? '节点详情' : '关系详情')
const selectedNodeDetails = computed<DetailItem[]>(() => {
  if (!selectedNode.value) return []
  const node = selectedNode.value
  const props = parseProps(node)
  const base: DetailItem[] = [
    { label: '节点 ID', value: node.id },
    { label: '类型', value: nodeKindLabel(node.kind) },
  ]
  if (node.kind === 'API') {
    base.push(
      { label: 'HTTP 方法', value: String(props.httpMethod || '-') },
      { label: '接口路径', value: String(props.endpointPath || props.contextPath || '-') },
      { label: '描述', value: String(props.aiDescription || props.description || '-') },
    )
  } else if (node.kind === 'FIELD_IN' || node.kind === 'FIELD_OUT') {
    base.push(
      { label: '参数路径', value: String(props.paramPath || node.label) },
      { label: '参数类型', value: node.typeName || '-' },
      { label: '位置', value: String(props.location || '-') },
      { label: '必填', value: props.required === true ? '是' : '否' },
      { label: '描述', value: String(props.description || '-') },
    )
  } else if (node.kind === 'DTO') {
    base.push(
      { label: '模型类型', value: node.typeName || String(props.rawType || '-') },
    )
  }
  return base
})
const selectedApiRelations = computed<ApiRelationItem[]>(() => {
  if (!selectedNode.value || selectedNode.value.kind !== 'API') return []
  return buildApiRelations(selectedNode.value)
})
const selectedApiMiniGraph = computed<MiniGraphLayout>(() => {
  if (!selectedNode.value || selectedNode.value.kind !== 'API') {
    return { width: 420, height: 0, cards: [], edges: [] }
  }
  return buildApiMiniGraph(selectedNode.value, selectedApiRelations.value)
})

async function createSelectedOutputToInputEdge() {
  if (!selectedNode.value || selectedNode.value.kind !== 'FIELD_IN') return
  const sourceNodeId = selectedCascaderNodeId(selectedSourceOutputPath.value)
  if (!sourceNodeId) return
  const source = nodeMap.value.get(sourceNodeId)
  if (!source) return
  await createParamReferenceEdge(source, selectedNode.value)
  selectedSourceOutputPath.value = []
}

async function createOutputToSelectedInputEdge() {
  if (!selectedNode.value || selectedNode.value.kind !== 'FIELD_OUT') return
  const targetNodeId = selectedCascaderNodeId(selectedTargetInputPath.value)
  if (!targetNodeId) return
  const target = nodeMap.value.get(targetNodeId)
  if (!target) return
  await createParamReferenceEdge(selectedNode.value, target)
  selectedTargetInputPath.value = []
}

async function createParamReferenceEdge(source: ApiGraphNode, target: ApiGraphNode) {
  creatingRelationEdge.value = true
  try {
    await upsertApiGraphEdge(props.projectId, {
      sourceNodeId: source.id,
      targetNodeId: target.id,
      kind: 'REQUEST_REF',
      note: 'manual-param-source',
    })
    ElMessage.success('参数引用关系已创建')
    await loadGraph()
  } catch {
    ElMessage.error('创建参数引用关系失败')
  } finally {
    creatingRelationEdge.value = false
  }
}

function syncManualPositionsFromSnapshot(next: ApiGraphSnapshot) {
  const apiIds = new Set(next.nodes.filter((n) => n.kind === 'API').map((n) => n.id))
  const nextPositions: Record<number, Point> = {}
  for (const lo of next.layouts) {
    if (apiIds.has(lo.nodeId)) {
      nextPositions[lo.nodeId] = { x: lo.x, y: lo.y }
    }
  }
  manualApiPositions.value = nextPositions
}

async function loadGraph() {
  loading.value = true
  try {
    const { data } = await getApiGraphSnapshot(props.projectId)
    const nextSnapshot = parseApiGraphSnapshot(data)
    snapshot.value = nextSnapshot
    syncManualPositionsFromSnapshot(nextSnapshot)
    try {
      candidateEdges.value = showCandidates.value
        ? (await listApiGraphCandidates(props.projectId)).data
        : []
    } catch {
      candidateEdges.value = []
    }
  } catch (e) {
    console.error('[ApiGraph] loadGraph failed', e)
    if (!axios.isAxiosError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '加载接口图谱失败')
    }
  } finally {
    loading.value = false
    nextTick(() => refreshHScrollDockMetrics())
  }
}

function handleNodeClick(node: ApiGraphNode) {
  if (suppressNextCardClick.value) {
    suppressNextCardClick.value = false
    return
  }
  if (linkMode.value) {
    if (!linkSource.value) {
      linkSource.value = node
      return
    }
    if (linkSource.value.id === node.id) {
      linkSource.value = null
      return
    }
    const normalizedParamEdge = normalizeParamReferencePair(linkSource.value, node)
    if (normalizedParamEdge) {
      void createEdge(normalizedParamEdge.source, normalizedParamEdge.target, 'REQUEST_REF')
      return
    }

    pendingEdge.value = { source: linkSource.value, target: node }
    pendingEdgeKind.value = inferDefaultEdgeKind(linkSource.value, node)
    edgeDialogVisible.value = true
    return
  }
  selectedNode.value = node
  selectedEdge.value = null
  detailVisible.value = true
}

function normalizeParamReferencePair(source: ApiGraphNode, target: ApiGraphNode) {
  if (source.kind === 'FIELD_OUT' && target.kind === 'FIELD_IN') {
    return { source, target }
  }
  if (source.kind === 'FIELD_IN' && target.kind === 'FIELD_OUT') {
    return { source: target, target: source }
  }
  return null
}

function selectEdge(edge: ApiGraphEdge) {
  selectedEdge.value = edge
  selectedNode.value = null
  detailVisible.value = true
}

function selectRelatedNode(node: ApiGraphNode) {
  selectedNode.value = node
  selectedEdge.value = null
  detailVisible.value = true
}

const relationZoomTitle = computed(() => {
  const n = selectedNode.value
  if (n?.kind === 'API') return `接口关联图谱 · ${n.label}`
  return '接口关联图谱'
})

function openRelationZoom() {
  if (!selectedNode.value || selectedNode.value.kind !== 'API') return
  if (!selectedApiMiniGraph.value.cards.length) {
    ElMessage.info('暂无关联关系')
    return
  }
  relationZoomScale.value = 1.25
  relationZoomVisible.value = true
}

function onRelationZoomClosed() {
  relationZoomScale.value = 1.25
}

function selectRelatedNodeFromZoom(node: ApiGraphNode) {
  selectRelatedNode(node)
  relationZoomVisible.value = false
}

async function submitPendingEdge() {
  if (!pendingEdge.value) return
  await createEdge(pendingEdge.value.source, pendingEdge.value.target, pendingEdgeKind.value)
}

function cancelPendingEdge() {
  edgeDialogVisible.value = false
  pendingEdge.value = null
  linkSource.value = null
}

async function createEdge(source: ApiGraphNode, target: ApiGraphNode, kind: ApiGraphEdgeKind) {
  creatingEdge.value = true
  try {
    await upsertApiGraphEdge(props.projectId, {
      sourceNodeId: source.id,
      targetNodeId: target.id,
      kind,
      note: 'manual',
    })
    ElMessage.success('连线成功')
    edgeDialogVisible.value = false
    pendingEdge.value = null
    linkSource.value = null
    await loadGraph()
  } catch {
    ElMessage.error('连线失败')
  } finally {
    creatingEdge.value = false
  }
}

async function confirmEdge(edge: ApiGraphEdge) {
  try {
    await confirmApiGraphCandidate(props.projectId, edge.id)
    ElMessage.success('已确认')
    selectedEdge.value = null
    await loadGraph()
  } catch {
    ElMessage.error('确认失败')
  }
}

async function rejectEdge(edge: ApiGraphEdge) {
  try {
    await rejectApiGraphCandidate(props.projectId, edge.id, 'manual-reject')
    ElMessage.success('已拒绝')
    selectedEdge.value = null
    await loadGraph()
  } catch {
    ElMessage.error('拒绝失败')
  }
}

async function refresh() {
  await loadGraph()
}

async function rebuild() {
  rebuilding.value = true
  loading.value = true
  try {
    const { data } = await rebuildApiGraph(props.projectId)
    const nextSnapshot = parseApiGraphSnapshot(data)
    snapshot.value = nextSnapshot
    syncManualPositionsFromSnapshot(nextSnapshot)
    try {
      candidateEdges.value = showCandidates.value
        ? (await listApiGraphCandidates(props.projectId)).data
        : []
    } catch {
      candidateEdges.value = []
      ElMessage.warning('候选边列表刷新失败，已暂时隐藏候选边')
    }
    const apiCount = nextSnapshot.nodes.filter((n) => n.kind === 'API').length
    ElMessage.success(`图谱重建完成（${apiCount} 个接口）`)
  } catch (e) {
    console.error('[ApiGraph] rebuild failed', e)
    if (!axios.isAxiosError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '图谱重建失败')
    }
  } finally {
    rebuilding.value = false
    loading.value = false
  }
}

async function regenerate() {
  try {
    await ElMessageBox.confirm(
      '将清空本项目全部接口图谱（节点、连线、画布布局），再按当前扫描结果重新生成。手工连线与卡片位置会丢失，节点 ID 也会重新分配。是否继续？',
      '重新生成图谱',
      {
        confirmButtonText: '清空并重新生成',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
  } catch {
    return
  }
  regenerating.value = true
  loading.value = true
  try {
    const { data } = await regenerateApiGraph(props.projectId)
    const nextSnapshot = parseApiGraphSnapshot(data)
    snapshot.value = nextSnapshot
    syncManualPositionsFromSnapshot(nextSnapshot)
    selectedNode.value = null
    selectedEdge.value = null
    detailVisible.value = false
    relationZoomVisible.value = false
    linkSource.value = null
    try {
      candidateEdges.value = showCandidates.value
        ? (await listApiGraphCandidates(props.projectId)).data
        : []
    } catch {
      candidateEdges.value = []
      ElMessage.warning('候选边列表刷新失败，已暂时隐藏候选边')
    }
    const apiCount = nextSnapshot.nodes.filter((n) => n.kind === 'API').length
    ElMessage.success(`图谱已重新生成（${apiCount} 个接口）`)
  } catch (e) {
    console.error('[ApiGraph] regenerate failed', e)
    if (!axios.isAxiosError(e)) {
      ElMessage.error(e instanceof Error ? e.message : '图谱重新生成失败')
    }
  } finally {
    regenerating.value = false
    loading.value = false
  }
}

async function inferModels() {
  inferring.value = true
  try {
    const { data } = await inferApiGraphModelEdges(props.projectId)
    ElMessage.success(`推断完成，生成 ${data.generated} 条模型边`)
    await loadGraph()
  } catch {
    ElMessage.error('推断失败')
  } finally {
    inferring.value = false
  }
}

async function inferRequestResponse() {
  inferRR.value = true
  try {
    const { data } = await inferApiGraphRequestResponseEdges(props.projectId)
    ElMessage.success(`推断完成，生成 ${data.generated} 条请求/响应边`)
    await loadGraph()
  } catch {
    ElMessage.error('推断失败')
  } finally {
    inferRR.value = false
  }
}

watch(() => props.projectId, loadGraph)
watch(showCandidates, loadGraph)
watch(linkMode, (enabled) => {
  if (!enabled) {
    linkSource.value = null
    pendingEdge.value = null
    edgeDialogVisible.value = false
  }
})
watch(() => selectedNode.value?.id, () => {
  selectedSourceOutputPath.value = []
  selectedTargetInputPath.value = []
})

watch(detailVisible, (open) => {
  if (!open) relationZoomVisible.value = false
})

watch(() => selectedNode.value?.kind, (k) => {
  if (k !== 'API') relationZoomVisible.value = false
})

watch(
  () => [
    graphLayout.value.width,
    graphLayout.value.height,
    graphLayout.value.apiCards.length,
    graphLayout.value.dtoCards.length,
  ],
  () => nextTick(() => refreshHScrollDockMetrics()),
)

watch(showDtoPanel, () => nextTick(() => refreshHScrollDockMetrics()))

watch(
  () => props.panelExpanded,
  () => {
    nextTick(() => {
      bindDockScrollListeners()
      refreshHScrollDockMetrics()
    })
  },
)

watch(draggingApiId, (id) => {
  if (id === null)
    nextTick(() => refreshHScrollDockMetrics())
})

function onWindowResizeForDock() {
  scheduleHScrollDockLayout()
}

onMounted(() => {
  loadGraph()
  const ro = new ResizeObserver(() => {
    scheduleHScrollDockLayout()
  })
  graphScrollResizeObserver = ro
  window.addEventListener('resize', onWindowResizeForDock, { passive: true })
  nextTick(() => {
    if (graphScrollRef.value)
      ro.observe(graphScrollRef.value)
    bindDockScrollListeners()
    refreshHScrollDockMetrics()
  })
})
onBeforeUnmount(() => {
  window.removeEventListener('pointermove', handleApiDragMove)
  window.removeEventListener('resize', onWindowResizeForDock)
  unbindDockScrollListeners()
  graphScrollResizeObserver?.disconnect()
  graphScrollResizeObserver = null
})

function groupBy<T>(items: T[], keyFn: (item: T) => number) {
  const map = new Map<number, T[]>()
  items.forEach((item) => {
    const key = keyFn(item)
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(item)
  })
  return map
}

function parseProps(node: ApiGraphNode): Record<string, any> {
  if (!node.propsJson) return {}
  try {
    const parsed = JSON.parse(node.propsJson)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function toFieldVM(node: ApiGraphNode): FieldVM {
  const props = parseProps(node)
  const descRaw = props.description ?? props.aiDescription
  const description = descRaw != null && String(descRaw).trim() ? String(descRaw).trim() : ''
  return {
    node,
    name: String(props.paramPath || node.label),
    type: shortType(node.typeName || '-'),
    path: String(props.paramPath || node.label),
    description,
  }
}

function buildParamCascaderOptions(
  kind: 'FIELD_IN' | 'FIELD_OUT',
  excludedApiNodeId: number | null,
): ParamCascaderOption[] {
  const fieldsByApi = new Map<number, ApiGraphNode[]>()
  fieldNodes.value
    .filter((node) => node.kind === kind)
    .forEach((node) => {
      const apiNodeId = resolveApiNodeId(node)
      if (!apiNodeId || apiNodeId === excludedApiNodeId) return
      if (!fieldsByApi.has(apiNodeId)) fieldsByApi.set(apiNodeId, [])
      fieldsByApi.get(apiNodeId)!.push(node)
    })

  const moduleNodesById = new Map<number, ApiGraphNode>()
  for (const n of snapshot.value.nodes) {
    if (n.kind === 'MODULE') moduleNodesById.set(n.id, n)
  }

  const apiOptions: ParamCascaderOption[] = snapshot.value.nodes
    .filter((node) => node.kind === 'API' && node.id !== excludedApiNodeId && fieldsByApi.has(node.id))
    .sort((a, b) => a.label.localeCompare(b.label, 'zh-Hans-CN'))
    .map((apiNode) => ({
      value: `api-${apiNode.id}`,
      label: apiNode.label || `接口 #${apiNode.id}`,
      children: buildParamPathTree(fieldsByApi.get(apiNode.id) ?? [], kind),
    }))
    .filter((option) => (option.children?.length ?? 0) > 0)

  type ModuleGroupKey = number | 'none'
  const grouped = new Map<ModuleGroupKey, ParamCascaderOption[]>()
  for (const opt of apiOptions) {
    const apiId = Number(String(opt.value).replace(/^api-/, ''))
    const apiNode = nodeMap.value.get(apiId)
    let modKey: ModuleGroupKey = 'none'
    if (apiNode?.parentId != null) {
      const parent = nodeMap.value.get(apiNode.parentId)
      if (parent?.kind === 'MODULE') modKey = parent.id
    }
    if (!grouped.has(modKey)) grouped.set(modKey, [])
    grouped.get(modKey)!.push(opt)
  }

  const rows: { modKey: ModuleGroupKey; label: string; children: ParamCascaderOption[] }[] = []
  for (const [modKey, children] of grouped) {
    const label =
      modKey === 'none'
        ? '未归属模块'
        : moduleNodesById.get(modKey)?.label || `模块 #${modKey}`
    rows.push({ modKey, label, children })
  }
  rows.sort((a, b) => {
    if (a.modKey === 'none') return 1
    if (b.modKey === 'none') return -1
    return a.label.localeCompare(b.label, 'zh-Hans-CN')
  })

  return rows.map((row) => ({
    value: row.modKey === 'none' ? 'mod-none' : `mod-${row.modKey}`,
    label: row.label,
    children: row.children,
  }))
}

function buildApiRelations(apiNode: ApiGraphNode): ApiRelationItem[] {
  const relationMap = new Map<string, ApiRelationItem>()

  allEdges.value.forEach((edge) => {
    const sourceNode = nodeMap.value.get(edge.sourceNodeId)
    const targetNode = nodeMap.value.get(edge.targetNodeId)
    if (!sourceNode || !targetNode) return

    const sourceBelongsToApi = isNodeInApi(sourceNode, apiNode.id)
    const targetBelongsToApi = isNodeInApi(targetNode, apiNode.id)
    if (!sourceBelongsToApi && !targetBelongsToApi) return

    const otherNode = sourceBelongsToApi ? targetNode : sourceNode
    collectRelationItem(relationMap, otherNode, edge.kind)

    if (edge.kind === 'MODEL_REF') {
      const selectedField = sourceBelongsToApi ? sourceNode : targetNode
      collectDtoRelationForField(relationMap, selectedField, edge.kind)
      collectDtoRelationForField(relationMap, otherNode, edge.kind)
    }
  })

  return Array.from(relationMap.values())
    .sort((a, b) => {
      if (a.kind !== b.kind) return a.kind === 'API' ? -1 : 1
      return a.node.label.localeCompare(b.node.label, 'zh-Hans-CN')
    })
}

function buildApiMiniGraph(apiNode: ApiGraphNode, relations: ApiRelationItem[]): MiniGraphLayout {
  const width = 420
  const cardWidth = 172
  const cardHeight = 74
  const gapX = 24
  const gapY = 28
  const currentCard: MiniGraphCard = {
    kind: 'API',
    node: apiNode,
    x: (width - cardWidth) / 2,
    y: 12,
    width: cardWidth,
    current: true,
  }
  const cards: MiniGraphCard[] = [currentCard]
  const edges: MiniGraphEdge[] = []
  const relationCards = relations.map((relation, index) => {
    const col = index % 2
    const row = Math.floor(index / 2)
    return {
      kind: relation.kind,
      node: relation.node,
      x: 24 + col * (cardWidth + gapX),
      y: currentCard.y + cardHeight + gapY + row * (cardHeight + gapY),
      width: cardWidth,
      current: false,
    } satisfies MiniGraphCard
  })
  cards.push(...relationCards)

  relationCards.forEach((card, index) => {
    const relation = relations[index]
    const start = { x: currentCard.x + currentCard.width / 2, y: currentCard.y + cardHeight }
    const end = { x: card.x + card.width / 2, y: card.y }
    const edgeKind = relation.edgeKinds.includes(edgeKindLabel('MODEL_REF'))
      ? 'MODEL_REF'
      : relation.edgeKinds.includes(edgeKindLabel('RESPONSE_REF'))
        ? 'RESPONSE_REF'
        : 'REQUEST_REF'
    const { path, labelX, labelY } = buildOrthogonalRoundedPath(start.x, start.y, end.x, end.y, {
      radius: 10,
      bendT: 0.5,
    })
    edges.push({
      id: `${apiNode.id}-${card.kind}-${card.node.id}`,
      kind: edgeKind,
      path,
      label: relation.edgeKinds.join('、'),
      labelX,
      labelY: labelY + 4,
    })
  })

  const lastCard = relationCards[relationCards.length - 1]
  const height = lastCard ? lastCard.y + cardHeight + 18 : 0
  return { width, height, cards, edges }
}

function collectRelationItem(map: Map<string, ApiRelationItem>, node: ApiGraphNode, edgeKind: string) {
  const relationNode = relationNodeFromGraphNode(node)
  if (!relationNode) return
  const key = `${relationNode.kind}-${relationNode.node.id}`
  if (!map.has(key)) {
    map.set(key, { ...relationNode, edgeKinds: [] })
  }
  const item = map.get(key)!
  const label = edgeKindLabel(edgeKind)
  if (!item.edgeKinds.includes(label)) item.edgeKinds.push(label)
}

function relationNodeFromGraphNode(node: ApiGraphNode): Pick<ApiRelationItem, 'kind' | 'node'> | null {
  if (node.kind === 'API') return { kind: 'API', node }
  if (node.kind === 'DTO') return { kind: 'DTO', node }

  const apiNodeId = resolveApiNodeId(node)
  if (apiNodeId && selectedNode.value?.id !== apiNodeId) {
    const apiNode = nodeMap.value.get(apiNodeId)
    if (apiNode?.kind === 'API') return { kind: 'API', node: apiNode }
  }

  const dtoNode = dtoNodeFromField(node)
  return dtoNode ? { kind: 'DTO', node: dtoNode } : null
}

function collectDtoRelationForField(map: Map<string, ApiRelationItem>, node: ApiGraphNode, edgeKind: string) {
  const dtoNode = dtoNodeFromField(node)
  if (!dtoNode) return
  collectRelationItem(map, dtoNode, edgeKind)
}

function dtoNodeFromField(node: ApiGraphNode) {
  if (node.kind !== 'FIELD_IN' && node.kind !== 'FIELD_OUT') return null
  const dtoName = simpleType(node.typeName)
  if (!dtoName) return null
  return snapshot.value.nodes.find((candidate) => candidate.kind === 'DTO' && candidate.label === dtoName) ?? null
}

function isNodeInApi(node: ApiGraphNode, apiNodeId: number) {
  if (node.id === apiNodeId) return true
  return resolveApiNodeId(node) === apiNodeId
}

function buildParamPathTree(fields: ApiGraphNode[], kind?: 'FIELD_IN' | 'FIELD_OUT'): ParamCascaderOption[] {
  const roots = new Map<string, ParamPathTreeNode>()
  fields
    .slice()
    .sort((a, b) => fieldPath(a).localeCompare(fieldPath(b), 'zh-Hans-CN'))
    .forEach((node) => {
      const segments = fieldPath(node).split('.').map((segment) => segment.trim()).filter(Boolean)
      const normalizedSegments = segments.length ? segments : [node.label]
      let cursor = roots
      let currentPath = ''
      normalizedSegments.forEach((segment, index) => {
        currentPath = currentPath ? `${currentPath}.${segment}` : segment
        if (!cursor.has(segment)) {
          cursor.set(segment, {
            segment,
            path: currentPath,
            children: new Map<string, ParamPathTreeNode>(),
          })
        }
        const treeNode = cursor.get(segment)!
        if (index === normalizedSegments.length - 1) {
          treeNode.node = node
        }
        cursor = treeNode.children
      })
    })
  return mapParamPathTreeToOptions(roots, kind)
}

function mapParamPathTreeToOptions(nodes: Map<string, ParamPathTreeNode>, kind?: 'FIELD_IN' | 'FIELD_OUT'): ParamCascaderOption[] {
  return Array.from(nodes.values())
    .sort((a, b) => a.segment.localeCompare(b.segment, 'zh-Hans-CN'))
    .map((treeNode) => {
      let children = mapParamPathTreeToOptions(treeNode.children, kind)

      // 如果 paramPath 树没有下级，但从 snapshot 的 parentId 关系中能找到子节点，也加入
      if (children.length === 0 && treeNode.node) {
        children = buildSnapshotChildrenOptions(treeNode.node.id, kind)
      }

      const type = treeNode.node ? shortType(treeNode.node.typeName || '-') : ''
      // 仅禁用「无图谱节点且无下级」的占位项。中间路径段若无节点但有子级，必须可展开，
      // 否则 el-cascader 在父级 disabled 时无法进入更深层（用户只能选到第一级 DTO/VO）。
      const disabled = !treeNode.node && children.length === 0
      return {
        value: treeNode.node?.id ?? `path-${treeNode.path}`,
        label: `${treeNode.segment}${type && type !== '-' ? `：${type}` : ''}`,
        disabled,
        ...(children.length ? { children } : {}),
      }
    })
}

/** 从 snapshot 的 parentId 关系中递归构建子级选项，支持任意深度 */
function buildSnapshotChildrenOptions(parentId: number, kind?: 'FIELD_IN' | 'FIELD_OUT'): ParamCascaderOption[] {
  const rawChildren = fieldChildrenByParent.value.get(parentId) ?? []
  const children = kind ? rawChildren.filter((c) => c.kind === kind) : rawChildren
  if (children.length === 0) return []
  return children
    .slice()
    .sort((a, b) => a.label.localeCompare(b.label, 'zh-Hans-CN'))
    .map((child) => {
      const grandChildren = buildSnapshotChildrenOptions(child.id, kind)
      return {
        value: child.id,
        label: `${child.label}${child.typeName && shortType(child.typeName) !== '-' ? `：${shortType(child.typeName)}` : ''}`,
        ...(grandChildren.length ? { children: grandChildren } : {}),
      }
    })
}

function resolveApiNodeId(node: ApiGraphNode) {
  if (node.refId && nodeMap.value.get(node.refId)?.kind === 'API') return node.refId

  let current: ApiGraphNode | undefined = node
  const visited = new Set<number>()
  while (current && !visited.has(current.id)) {
    visited.add(current.id)
    const parent: ApiGraphNode | undefined = current.parentId ? nodeMap.value.get(current.parentId) : undefined
    if (!parent) break
    if (parent.kind === 'API') return parent.id
    if (parent.refId && nodeMap.value.get(parent.refId)?.kind === 'API') return parent.refId
    current = parent
  }
  return null
}

function selectedCascaderNodeId(path: CascaderPath) {
  const leaf = path[path.length - 1]
  if (typeof leaf === 'number' && Number.isFinite(leaf)) return leaf
  if (typeof leaf === 'string' && /^\d+$/.test(leaf)) return Number(leaf)
  return null
}

function fieldPath(node: ApiGraphNode) {
  const props = parseProps(node)
  if (props.paramPath) return String(props.paramPath)

  const parts: string[] = [node.label]
  let current = node
  const visited = new Set<number>([node.id])
  while (current.parentId) {
    const parent = nodeMap.value.get(current.parentId)
    if (!parent || visited.has(parent.id) || parent.kind === 'API') break
    parts.unshift(parent.label)
    visited.add(parent.id)
    current = parent
  }
  return parts.join('.')
}

function shortType(type: string) {
  return type
    .replace(/^java\.lang\./, '')
    .replace(/^java\.util\./, '')
    .replace(/com\.[\w.]+\./g, '')
}

function simpleType(type?: string | null) {
  if (!type) return ''
  const cleaned = shortType(type)
  const genericMatch = cleaned.match(/<\s*([^>]+)\s*>/)
  return (genericMatch?.[1] || cleaned).split('.').pop() || cleaned
}

function buildDtoFieldBuckets(
  dtoNodes: ApiGraphNode[],
  fieldNodes: ApiGraphNode[],
  fieldsByParent: Map<number, ApiGraphNode[]>,
) {
  const dtoLabels = new Set(dtoNodes.map((node) => node.label))
  const buckets = new Map<string, DtoFieldVM[]>()
  fieldNodes.forEach((field) => {
    const dtoLabel = simpleType(field.typeName)
    if (!dtoLabels.has(dtoLabel)) return
    const children = fieldsByParent.get(field.id) ?? []
    if (!buckets.has(dtoLabel)) buckets.set(dtoLabel, [])
    const bucket = buckets.get(dtoLabel)!
    children.forEach((child) => {
      const vm = toFieldVM(child)
      const name = vm.path.split('.').pop() || vm.name
      const key = `${name}:${vm.type}`
      if (!bucket.some((item) => item.key === key)) {
        bucket.push({ key, name, type: vm.type })
      }
    })
  })
  return buckets
}

function findNearestAnchor(node: ApiGraphNode, anchorMap: Map<number, Point>) {
  let current: ApiGraphNode | undefined = node
  const visited = new Set<number>()
  while (current && !visited.has(current.id)) {
    visited.add(current.id)
    const parentId = current.parentId ?? current.refId
    if (!parentId) return null
    const anchored = anchorMap.get(parentId)
    if (anchored) {
      return {
        x: anchored.x + (node.kind === 'FIELD_IN' ? -8 : 8),
        y: anchored.y,
      }
    }
    current = nodeMap.value.get(parentId)
  }
  return null
}

/** 去掉 path 开头的 M x y，便于与前置水平段拼接 */
function stripLeadingSvgMove(d: string): string {
  const s = d.trimStart()
  const m = s.match(/^M\s*([\d.-]+)\s*([\d.-]+)\s+(.*)/)
  return m?.[3]?.trimStart() ?? s
}

/**
 * 正交路径上 90° 圆角对应的 SVG 小弧 sweep（large-arc=0）。
 * 用拐角前一点 → 拐角 → 拐角后一点的走向算叉积（屏幕坐标 y 向下），
 * 与手写 hDir*vDir 相比，在卡片拖动、起终点相对位置变化时更稳定。
 */
function svgSweepForCorner(prev: Point, corner: Point, next: Point): 0 | 1 {
  const vInX = corner.x - prev.x
  const vInY = corner.y - prev.y
  const vOutX = next.x - corner.x
  const vOutY = next.y - corner.y
  const cross = vInX * vOutY - vInY * vOutX
  /* SVG y 向下：小弧 sweep 与「数学系 y 向上」叉积符号相反，用 cross>0 */
  return cross > 0 ? 1 : 0
}

/** 入参锚点在卡片左侧：先向左伸出；出参在右侧：先向右伸出 */
function fieldEdgeHorizStubOut(node: ApiGraphNode, anchor: Point): Point {
  if (node.kind === 'FIELD_IN') return { x: anchor.x - EDGE_FIELD_CARD_STUB, y: anchor.y }
  if (node.kind === 'FIELD_OUT') return { x: anchor.x + EDGE_FIELD_CARD_STUB, y: anchor.y }
  return { ...anchor }
}

/** 连到入参/出参锚点前，先水平接到「卡片外」再竖向进入锚点 */
function fieldEdgeHorizStubIn(node: ApiGraphNode, anchor: Point): Point {
  if (node.kind === 'FIELD_IN') return { x: anchor.x - EDGE_FIELD_CARD_STUB, y: anchor.y }
  if (node.kind === 'FIELD_OUT') return { x: anchor.x + EDGE_FIELD_CARD_STUB, y: anchor.y }
  return { ...anchor }
}

function composeGraphEdgePath(
  source: ApiGraphNode,
  target: ApiGraphNode,
  anchorSource: Point,
  anchorTarget: Point,
  orthoOpts?: { radius?: number; bendT?: number },
): { path: string; labelX: number; labelY: number } {
  const srcRun = fieldEdgeHorizStubOut(source, anchorSource)
  const tgtRun = fieldEdgeHorizStubIn(target, anchorTarget)
  const opt = { radius: 14, bendT: 0.5, ...orthoOpts }

  const hasSrcStub = anchorSource.x !== srcRun.x || anchorSource.y !== srcRun.y
  const hasTgtStub = anchorTarget.x !== tgtRun.x || anchorTarget.y !== tgtRun.y

  const core = buildOrthogonalRoundedPath(srcRun.x, srcRun.y, tgtRun.x, tgtRun.y, opt)
  let inner = stripLeadingSvgMove(core.path)

  /** 水平 stub 末端与「先竖后横」首段竖线衔接处加小圆弧（须接到 core 首条竖线的终点 y1，避免破坏后续 A） */
  const STUB_JOIN_R = 10
  let path: string
  if (hasSrcStub && core.verticalFirst) {
    const firstLeg = core.path.match(/^M\s*([\d.-]+)\s*([\d.-]+)\s+L\s*([\d.-]+)\s*([\d.-]+)\s+/)
    const y1 = firstLeg ? Number(firstLeg[4]) : NaN
    const xLeg = firstLeg ? Number(firstLeg[3]) : NaN
    if (
      firstLeg
      && Number.isFinite(y1)
      && Number.isFinite(xLeg)
      && Math.abs(xLeg - srcRun.x) < 0.5
      && Math.abs(y1 - srcRun.y) > 0.5
    ) {
      const hStub = Math.sign(srcRun.x - anchorSource.x) || 1
      const vGo = Math.sign(y1 - srcRun.y) || 1
      const vertLeg = Math.abs(y1 - srcRun.y)
      const rr = Math.min(
        STUB_JOIN_R,
        EDGE_FIELD_CARD_STUB - 2,
        vertLeg * 0.38,
      )
      /** 90° 小弧的竖向抬升必须 ≤ 竖边长度，否则 A 会走长弧/畸形弧 */
      const r = Math.min(Math.max(4, rr), Math.max(vertLeg, 1e-6))
      const x0 = srcRun.x - hStub * r
      const arcEndY = srcRun.y + vGo * r
      const sweep = svgSweepForCorner(
        { x: x0, y: srcRun.y },
        { x: srcRun.x, y: srcRun.y },
        { x: srcRun.x, y: arcEndY },
      )
      const head = `M ${anchorSource.x} ${anchorSource.y} L ${x0} ${srcRun.y} A ${r} ${r} 0 0 ${sweep} ${srcRun.x} ${arcEndY} L ${srcRun.x} ${y1}`
      const rest = core.path.replace(/^M\s*[\d.-]+\s*[\d.-]+\s+L\s*[\d.-]+\s*[\d.-]+\s*/, '')
      path = `${head} ${rest}`.replace(/\s+/g, ' ').trim()
    } else {
      path = `M ${anchorSource.x} ${anchorSource.y} L ${srcRun.x} ${srcRun.y} ${inner}`.replace(/\s+/g, ' ').trim()
    }
  } else if (hasSrcStub) {
    path = `M ${anchorSource.x} ${anchorSource.y} L ${srcRun.x} ${srcRun.y} ${inner}`.replace(/\s+/g, ' ').trim()
  } else {
    path = core.path.trim()
  }

  if (hasTgtStub && core.verticalFirst) {
    const arcEnd = core.path.match(
      /A\s+[\d.-]+\s+[\d.-]+\s+0\s+0\s+[01]\s+([\d.-]+)\s+([\d.-]+)\s+L\s+([\d.-]+)\s+([\d.-]+)\s*$/,
    )
    const ax = arcEnd ? Number(arcEnd[1]) : NaN
    const ay4 = arcEnd ? Number(arcEnd[2]) : NaN
    const lx = arcEnd ? Number(arcEnd[3]) : NaN
    const ly = arcEnd ? Number(arcEnd[4]) : NaN
    if (
      arcEnd
      && Number.isFinite(ax)
      && Number.isFinite(ay4)
      && Number.isFinite(lx)
      && Number.isFinite(ly)
      && Math.abs(ax - lx) < 0.5
      && Math.abs(lx - tgtRun.x) < 0.5
      && Math.abs(ly - tgtRun.y) < 0.5
      && Math.abs(anchorTarget.y - tgtRun.y) < 0.5
    ) {
      const y4 = ay4
      const hGo = Math.sign(anchorTarget.x - tgtRun.x) || 1
      const vIn = Math.sign(tgtRun.y - y4) || 1
      const vertLeg = Math.abs(tgtRun.y - y4)
      const horizLeg = Math.abs(anchorTarget.x - tgtRun.x)
      const rr = Math.min(
        STUB_JOIN_R,
        horizLeg * 0.42,
        vertLeg * 0.38,
      )
      /** 90° 小弧：半径不能超过参与圆角的两条直角边长度 */
      const r = Math.min(Math.max(4, rr), Math.max(vertLeg, 1e-6), Math.max(horizLeg, 1e-6))
      const sweep = svgSweepForCorner(
        { x: tgtRun.x, y: y4 },
        { x: tgtRun.x, y: tgtRun.y },
        { x: anchorTarget.x, y: anchorTarget.y },
      )
      const y0 = tgtRun.y - vIn * r
      const xArc = tgtRun.x + hGo * r
      const tail = ` L ${tgtRun.x} ${tgtRun.y}`
      const pos = path.lastIndexOf(tail)
      if (pos >= 0) {
        const fillet = ` L ${tgtRun.x} ${y0} A ${r} ${r} 0 0 ${sweep} ${xArc} ${tgtRun.y} L ${anchorTarget.x} ${anchorTarget.y}`
        path = `${path.slice(0, pos)}${fillet}`.replace(/\s+/g, ' ').trim()
      } else {
        path = `${path} L ${anchorTarget.x} ${anchorTarget.y}`.replace(/\s+/g, ' ').trim()
      }
    } else {
      path = `${path} L ${anchorTarget.x} ${anchorTarget.y}`.replace(/\s+/g, ' ').trim()
    }
  } else if (hasTgtStub) {
    path = `${path} L ${anchorTarget.x} ${anchorTarget.y}`.replace(/\s+/g, ' ').trim()
  }

  return { path, labelX: core.labelX, labelY: core.labelY }
}

/**
 * 正交折线 + 90° 圆角（SVG 圆弧），用于接口图谱连线。
 * 竖向跨度明显大于横向时用「先竖后横」，否则「先横后竖」。
 */
function buildOrthogonalRoundedPath(
  sx: number,
  sy: number,
  tx: number,
  ty: number,
  opts?: { radius?: number; bendT?: number },
): { path: string; labelX: number; labelY: number; verticalFirst: boolean } {
  const radius = opts?.radius ?? 12
  const bendT = opts?.bendT ?? 0.5
  const dx = tx - sx
  const dy = ty - sy
  const adx = Math.abs(dx)
  const ady = Math.abs(dy)

  if (adx < 1 && ady < 1) {
    return { path: `M ${sx} ${sy} L ${tx} ${ty}`, labelX: (sx + tx) / 2, labelY: (sy + ty) / 2 - 8, verticalFirst: false }
  }
  if (ady < 2) {
    return { path: `M ${sx} ${sy} L ${tx} ${ty}`, labelX: (sx + tx) / 2, labelY: sy - 10, verticalFirst: false }
  }
  if (adx < 2) {
    return { path: `M ${sx} ${sy} L ${tx} ${ty}`, labelX: sx - 10, labelY: (sy + ty) / 2, verticalFirst: false }
  }

  const verticalFirst = ady > adx * 1.15
  const hDir = dx >= 0 ? 1 : -1
  const vDir = dy >= 0 ? 1 : -1

  if (!verticalFirst) {
    const mx = sx + dx * bendT
    const rad = Math.max(
      3,
      Math.min(
        radius,
        Math.abs(mx - sx) * 0.42,
        Math.abs(tx - mx) * 0.42,
        ady * 0.48 - 1,
      ),
    )
    const x1 = mx - hDir * rad
    const y2 = sy + vDir * rad
    const y3 = ty - vDir * rad
    const x4 = mx + hDir * rad
    const sweep1 = svgSweepForCorner({ x: x1, y: sy }, { x: mx, y: sy }, { x: mx, y: y3 })
    const sweep2 = svgSweepForCorner({ x: mx, y: y3 }, { x: mx, y: ty }, { x: x4, y: ty })
    const path = [
      `M ${sx} ${sy}`,
      `L ${x1} ${sy}`,
      `A ${rad} ${rad} 0 0 ${sweep1} ${mx} ${y2}`,
      `L ${mx} ${y3}`,
      `A ${rad} ${rad} 0 0 ${sweep2} ${x4} ${ty}`,
      `L ${tx} ${ty}`,
    ].join(' ')
    return { path, labelX: mx, labelY: (y2 + y3) / 2 - 8, verticalFirst: false }
  }

  const my = sy + dy * bendT
  const rad = Math.max(
    3,
    Math.min(
      radius,
      Math.abs(my - sy) * 0.42,
      Math.abs(ty - my) * 0.42,
      adx * 0.48 - 1,
    ),
  )
  const y1 = my - vDir * rad
  const x2 = sx + hDir * rad
  const x3 = tx - hDir * rad
  const y4 = my + vDir * rad
  const sweep1 = svgSweepForCorner({ x: sx, y: y1 }, { x: sx, y: my }, { x: x3, y: my })
  const sweep2 = svgSweepForCorner({ x: x3, y: my }, { x: tx, y: my }, { x: tx, y: y4 })
  const path = [
    `M ${sx} ${sy}`,
    `L ${sx} ${y1}`,
    `A ${rad} ${rad} 0 0 ${sweep1} ${x2} ${my}`,
    `L ${x3} ${my}`,
    `A ${rad} ${rad} 0 0 ${sweep2} ${tx} ${y4}`,
    `L ${tx} ${ty}`,
  ].join(' ')
  return { path, labelX: (x2 + x3) / 2, labelY: my - 8, verticalFirst: true }
}

function toEdgeVM(edge: ApiGraphEdge, anchorMap: Map<number, Point>): EdgeVM | null {
  const source = nodeMap.value.get(edge.sourceNodeId)
  const target = nodeMap.value.get(edge.targetNodeId)
  if (!source || !target) return null
  if (edge.kind === 'BELONGS_TO' && target.kind !== 'DTO') return null

  const sourcePoint = anchorMap.get(edge.sourceNodeId)
  const targetPoint = anchorMap.get(edge.targetNodeId)
  if (!sourcePoint || !targetPoint) return null

  const visualKind: EdgeVisualKind = edge.kind === 'BELONGS_TO' ? 'MODEL_REF' : edge.kind as EdgeVisualKind
  const { path, labelX, labelY } = composeGraphEdgePath(source, target, sourcePoint, targetPoint, {
    radius: 14,
    bendT: 0.5,
  })
  return {
    id: String(edge.id),
    raw: edge,
    visualKind,
    path,
    label: edgeLabel(edge, target),
    labelX,
    labelY,
  }
}

function edgeLabel(edge: ApiGraphEdge, target: ApiGraphNode) {
  if (edge.kind === 'REQUEST_REF') return `请求引用 ${target.label}`
  if (edge.kind === 'RESPONSE_REF') return `响应引用 ${target.label}`
  return ''
}

function nodeStyle(card: ApiCardVM) {
  return {
    left: `${card.x}px`,
    top: `${card.y}px`,
    width: `${card.width}px`,
    minHeight: `${card.height}px`,
  }
}

function startApiDrag(event: PointerEvent, card: ApiCardVM) {
  if (event.button !== 0) return
  const target = event.target as HTMLElement | null
  if (target?.closest('.field-row')) return

  dragState = {
    nodeId: card.node.id,
    startClientX: event.clientX,
    startClientY: event.clientY,
    startX: card.x,
    startY: card.y,
    moved: false,
  }
  draggingApiId.value = card.node.id
  window.addEventListener('pointermove', handleApiDragMove)
  window.addEventListener('pointerup', stopApiDrag, { once: true })
}

function handleApiDragMove(event: PointerEvent) {
  if (!dragState) return
  const deltaX = event.clientX - dragState.startClientX
  const deltaY = event.clientY - dragState.startClientY
  if (!dragState.moved && Math.hypot(deltaX, deltaY) < 4) return

  dragState.moved = true
  suppressNextCardClick.value = true
  manualApiPositions.value = {
    ...manualApiPositions.value,
    [dragState.nodeId]: {
      x: Math.max(LEGEND_WIDTH + 48, dragState.startX + deltaX),
      y: Math.max(24, dragState.startY + deltaY),
    },
  }
}

async function persistApiCardLayout(nodeId: number, pos: Point) {
  try {
    await saveApiGraphLayout(props.projectId, {
      positions: [{ nodeId, x: pos.x, y: pos.y }],
    })
  } catch {
    /* agentRequest 拦截器已提示 */
  }
}

function stopApiDrag() {
  const state = dragState
  if (state?.moved) {
    suppressNextCardClick.value = true
    window.setTimeout(() => {
      suppressNextCardClick.value = false
    }, 0)
    const pos = manualApiPositions.value[state.nodeId]
    if (pos) {
      void persistApiCardLayout(state.nodeId, pos)
    }
  }
  dragState = null
  draggingApiId.value = null
  window.removeEventListener('pointermove', handleApiDragMove)
}

function inferDefaultEdgeKind(source: ApiGraphNode, target: ApiGraphNode): ApiGraphEdgeKind {
  if (target.kind === 'FIELD_IN') return 'REQUEST_REF'
  if (source.kind === 'FIELD_OUT') return 'RESPONSE_REF'
  if (source.kind === 'DTO' || target.kind === 'DTO') return 'MODEL_REF'
  return 'REQUEST_REF'
}

function nodeKindLabel(kind: string) {
  const labels: Record<string, string> = {
    API: '接口',
    FIELD_IN: '入参',
    FIELD_OUT: '出参',
    DTO: '数据模型',
    MODULE: '模块',
  }
  return labels[kind] || kind
}

function edgeKindLabel(kind: string) {
  const labels: Record<string, string> = {
    REQUEST_REF: '请求引用',
    RESPONSE_REF: '响应引用',
    MODEL_REF: '数据模型引用',
    BELONGS_TO: '从属关系',
  }
  return labels[kind] || kind
}

function nodeTagType(kind: string) {
  if (kind === 'API') return 'primary'
  if (kind === 'DTO') return 'warning'
  if (kind === 'FIELD_OUT') return 'success'
  return 'info'
}

function edgeTagType(kind: string) {
  if (kind === 'RESPONSE_REF') return 'success'
  if (kind === 'MODEL_REF') return 'warning'
  return 'primary'
}

function edgeNodeLabel(nodeId: number) {
  const node = nodeMap.value.get(nodeId)
  return node ? node.label : `#${nodeId}`
}
</script>

<style scoped lang="scss">
.api-graph-canvas {
  position: relative;
}

.graph-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.toolbar-title {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
}

.toolbar-subtitle,
.graph-hint {
  font-size: 12px;
  color: var(--text-secondary);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.graph-hint {
  padding: 9px 12px;
  margin-bottom: 12px;
  border: 1px solid rgba(59, 130, 246, 0.18);
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.06);
}

.graph-container {
  width: 100%;
  min-height: 620px;
  overflow-y: auto;
  overflow-x: hidden;
  border: 1px solid var(--border-glass);
  border-radius: 14px;
  background:
    radial-gradient(circle at 20% 20%, rgba(59, 130, 246, 0.08), transparent 26%),
    radial-gradient(circle at 80% 18%, rgba(168, 85, 247, 0.08), transparent 24%),
    linear-gradient(180deg, var(--bg-secondary), var(--bg-tertiary));
}

.empty-state {
  display: flex;
  min-height: 520px;
  align-items: center;
  justify-content: center;
}

.graph-stage {
  position: relative;
  min-width: 100%;
  min-height: 620px;
}

.legend-panel,
.api-card,
.dto-panel,
.relationship-note {
  position: absolute;
  border: 1px solid rgba(59, 130, 246, 0.25);
  background: color-mix(in srgb, var(--bg-primary) 88%, transparent);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.10);
  backdrop-filter: blur(10px);
}

.legend-panel {
  left: 18px;
  top: 18px;
  width: v-bind('`${LEGEND_WIDTH}px`');
  padding: 14px;
  border-color: var(--border-glass);
  border-radius: 12px;
  color: var(--text-primary);
  font-size: 12px;
  line-height: 1.8;
}

.legend-title {
  margin-bottom: 6px;
  font-weight: 700;
}

.legend-line,
.legend-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.edge-sample {
  display: inline-block;
  width: 38px;
  height: 0;
  border-top: 2px solid;
}

.edge-sample.request {
  border-color: #3b82f6;
}

.edge-sample.response {
  border-color: #22c55e;
}

.edge-sample.model {
  border-color: #a855f7;
  border-top-style: dashed;
}

.node-badge {
  display: inline-flex;
  min-width: 32px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.node-badge.api {
  background: linear-gradient(135deg, #60a5fa, #2563eb);
}

.field-icon {
  display: inline-flex;
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 700;
}

.field-icon.in {
  color: #0f766e;
  background: rgba(45, 212, 191, 0.18);
  border: 1px solid rgba(20, 184, 166, 0.42);
}

.field-icon.out {
  color: #15803d;
  background: rgba(74, 222, 128, 0.18);
  border: 1px solid rgba(34, 197, 94, 0.42);
}

.dto-icon {
  display: inline-flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  color: #7c3aed;
  font-weight: 700;
}

.edge-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.edge-group {
  cursor: pointer;
  pointer-events: auto;
}

.edge-path {
  fill: none;
  stroke-width: 2.2;
  filter: drop-shadow(0 2px 4px rgba(15, 23, 42, 0.12));
}

.edge-hit {
  fill: none;
  stroke: transparent;
  stroke-width: 14;
}

.edge-path.REQUEST_REF {
  stroke: #3b82f6;
}

.edge-path.RESPONSE_REF {
  stroke: #22c55e;
}

.edge-path.MODEL_REF {
  stroke: #a855f7;
  stroke-dasharray: 7 5;
}

.edge-group.candidate .edge-path {
  opacity: 0.55;
}

.arrow-head.REQUEST_REF {
  fill: #3b82f6;
}

.arrow-head.RESPONSE_REF {
  fill: #22c55e;
}

.arrow-head.MODEL_REF {
  fill: #a855f7;
}

.edge-label {
  fill: #2563eb;
  font-size: 12px;
  font-weight: 600;
  paint-order: stroke;
  pointer-events: none;
  stroke: var(--bg-primary);
  stroke-width: 4px;
}

.api-card,
.dto-card {
  cursor: pointer;
  color: var(--text-primary);
  font: inherit;
  text-align: left;
}

.api-card {
  padding: 0;
  overflow: hidden;
  border-radius: 10px;
  cursor: grab;
  user-select: none;
  touch-action: none;
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
}

.api-card.dragging {
  cursor: grabbing;
  transition: none;
  z-index: 5;
}

.api-card:hover,
.api-card.active,
.dto-card:hover,
.dto-card.active {
  border-color: #3b82f6;
  box-shadow: 0 18px 48px rgba(37, 99, 235, 0.18);
  transform: translateY(-1px);
}

.api-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 14px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.16);
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.08), transparent);
}

.api-title-block {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.api-title-block strong,
.api-title-block span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.api-title-block span {
  color: var(--text-secondary);
  font-size: 12px;
}

.api-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
}

.field-column {
  min-width: 0;
  padding: 10px 12px 12px;
}

.field-column + .field-column {
  border-left: 1px solid rgba(59, 130, 246, 0.13);
}

.field-title {
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.field-row {
  display: flex;
  width: 100%;
  height: 30px;
  align-items: center;
  gap: 5px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  font: inherit;
}

.field-row:hover .field-name {
  color: #2563eb;
}

.field-name,
.field-type {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-name {
  flex: 1;
  font-size: 12px;
  font-weight: 600;
}

.field-type {
  max-width: 58px;
  color: var(--text-secondary);
  font-size: 11px;
}

.field-empty {
  color: var(--text-tertiary);
  font-size: 12px;
}

.dto-panel {
  padding: 14px 16px 18px;
  border-color: rgba(168, 85, 247, 0.36);
  border-radius: 12px;
  background: color-mix(in srgb, var(--bg-primary) 86%, rgba(168, 85, 247, 0.08));
}

.dto-panel-title {
  margin-bottom: 14px;
  color: #7c3aed;
  font-weight: 700;
}

.dto-card {
  display: block;
  width: 100%;
  min-height: 128px;
  padding: 12px;
  margin-bottom: 16px;
  border: 1px solid rgba(168, 85, 247, 0.36);
  border-radius: 9px;
  background: color-mix(in srgb, var(--bg-primary) 90%, rgba(168, 85, 247, 0.05));
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
}

.dto-title {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 8px;
  color: #7c3aed;
  font-weight: 700;
}

.dto-field {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-primary);
  font-size: 12px;
  line-height: 1.75;
}

.dto-field span:last-child {
  color: var(--text-secondary);
}

.dto-field.dim {
  color: var(--text-secondary);
}

.relationship-note {
  left: 300px;
  bottom: 24px;
  display: flex;
  max-width: 620px;
  gap: 10px;
  padding: 12px 16px;
  border-color: rgba(245, 158, 11, 0.34);
  border-radius: 10px;
  background: rgba(245, 158, 11, 0.08);
  color: var(--text-secondary);
  font-size: 12px;
}

.relationship-note strong {
  color: #b45309;
  white-space: nowrap;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-section h3 {
  margin: 10px 0 4px;
  color: var(--text-primary);
}

.detail-muted {
  margin: 0;
  color: var(--text-secondary);
  font-size: 12px;
  word-break: break-all;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

.api-relation-panel {
  margin-top: 18px;
}

.api-relation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 6px;
}

.api-relation-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.api-relation-zoom-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 0;
}

.api-relation-zoom-actions :deep(.el-button) {
  padding: 4px 6px;
}

.relation-zoom-viewport {
  overflow: auto;
  box-sizing: border-box;
  max-width: 100%;
  max-height: min(78vh, 680px);
  padding: 12px;
  border: 1px solid var(--border-glass);
  border-radius: 12px;
  background:
    radial-gradient(circle at 28% 18%, rgba(59, 130, 246, 0.08), transparent 28%),
    color-mix(in srgb, var(--bg-tertiary) 88%, transparent);
}

.relation-zoom-scaler {
  position: relative;
  transform-origin: top left;
}

.relation-empty {
  padding: 10px 12px;
  margin-top: 10px;
  border: 1px dashed var(--border-glass);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 12px;
}

.relation-mini-graph {
  position: relative;
  width: 100%;
  min-height: 164px;
  margin-top: 12px;
  overflow: auto hidden;
  border: 1px solid var(--border-glass);
  border-radius: 12px;
  background:
    radial-gradient(circle at 28% 18%, rgba(59, 130, 246, 0.08), transparent 28%),
    color-mix(in srgb, var(--bg-tertiary) 88%, transparent);
}

.relation-mini-edges {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.relation-mini-edge {
  fill: none;
  stroke-width: 1.8;
  opacity: 0.88;
}

.relation-mini-edge.REQUEST_REF {
  stroke: #3b82f6;
}

.relation-mini-edge.RESPONSE_REF {
  stroke: #22c55e;
}

.relation-mini-edge.MODEL_REF {
  stroke: #a855f7;
  stroke-dasharray: 6 4;
}

.relation-mini-label {
  fill: #2563eb;
  font-size: 10px;
  paint-order: stroke;
  pointer-events: none;
  stroke: var(--bg-primary);
  stroke-width: 3px;
  text-anchor: middle;
}

.relation-mini-card {
  position: absolute;
  display: flex;
  min-height: 74px;
  flex-direction: column;
  gap: 6px;
  padding: 10px;
  border: 1px solid rgba(59, 130, 246, 0.28);
  border-radius: 10px;
  background: color-mix(in srgb, var(--bg-primary) 90%, transparent);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  color: var(--text-primary);
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.relation-mini-card.dto {
  border-color: rgba(168, 85, 247, 0.36);
}

.relation-mini-card.current {
  border-color: #3b82f6;
  box-shadow: 0 12px 30px rgba(37, 99, 235, 0.16);
}

.relation-mini-card:hover {
  transform: translateY(-1px);
}

.relation-mini-title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
}

.relation-mini-title strong,
.relation-mini-subtitle {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-mini-title strong {
  min-width: 0;
  font-size: 12px;
}

.relation-mini-subtitle {
  color: var(--text-secondary);
  font-size: 11px;
}

.param-link-panel {
  margin-top: 18px;
}

.param-link-panel :deep(.el-select),
.param-cascader {
  width: 100%;
  margin-top: 12px;
}

.edge-kind-options {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

:deep(.el-drawer) {
  background: var(--bg-primary);
}

@media (max-width: 900px) {
  .graph-toolbar {
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
  }
}
</style>

<style lang="scss">
/* 关联图放大对话框：视口居中 */
.el-overlay-dialog:has(.relation-zoom-dialog) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.relation-zoom-dialog.el-dialog {
  margin: 0 !important;
}

.api-graph-field-desc-tooltip {
  max-width: min(400px, 85vw);
  line-height: 1.45;
  word-break: break-word;
}

/* 参数来源 / 目标入参级联：固定每列宽度；列从右向左排（一级在最右，子级依次向左） */
.api-graph-param-cascader-popper.el-popper {
  --el-cascader-menu-min-width: 216px;
}

.api-graph-param-cascader-popper {
  .el-cascader-panel {
    display: flex;
    flex-direction: row-reverse;
    flex-wrap: nowrap;
  }

  .el-cascader-menu {
    box-sizing: border-box;
    min-width: 216px !important;
    width: 216px !important;
    max-width: 216px !important;
    flex: 0 0 216px !important;
  }

  /* 子级在左侧展开，箭头改为朝左 */
  .el-cascader-node__postfix .el-icon {
    transform: scaleX(-1);
  }

  .el-cascader-node__label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
  }
}

/* Teleport 到 body：横向条宽度/水平位置与 .graph-container 对齐，底边贴视口 */
.api-graph-hscroll-dock {
  position: fixed;
  bottom: 0;
  z-index: 90;
  box-sizing: border-box;
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: color-mix(in srgb, var(--el-bg-color, #fff) 94%, transparent);
  border-top: 1px solid var(--el-border-color-lighter, #ebeef5);
  box-shadow: 0 -6px 20px rgba(15, 23, 42, 0.06);
}

.api-graph-hscroll-track {
  overflow-x: auto;
  overflow-y: hidden;
  height: 14px;
  scrollbar-width: thin;
  scrollbar-color: rgba(100, 116, 139, 0.45) transparent;
}

.api-graph-hscroll-spacer {
  height: 1px;
  pointer-events: none;
}
</style>
