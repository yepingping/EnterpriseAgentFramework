import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8')
}

function assertContains(file, pattern, message) {
  const body = read(file)
  if (pattern instanceof RegExp ? !pattern.test(body) : !body.includes(pattern)) {
    throw new Error(`${file}: ${message}`)
  }
}

assertContains('src/types/context.ts', 'export interface ContextBindingRequest', 'binding request type is missing')
assertContains('src/types/context.ts', 'bindings?: ContextBindingRequest[]', 'context item create request must support initial bindings')
assertContains('src/types/context.ts', 'evidence?: ContextEvidenceRequest[]', 'context item create request must support initial evidence')
assertContains('src/types/context.ts', 'export interface ContextMemoryCandidateCreateRequest', 'candidate create request type is missing')
assertContains('src/types/context.ts', 'namespaceId?: number', 'candidate list params must support namespaceId')
assertContains('src/types/context.ts', 'candidateType?: string', 'candidate list params must support candidateType')
assertContains('src/types/context.ts', 'sourceType?: string', 'candidate list params must support sourceType')
assertContains('src/types/context.ts', 'pageInstanceId?: string', 'candidate list params must support page pageKey filter')
assertContains('src/types/context.ts', 'origin?: string', 'candidate list params must support source channel origin')
assertContains('src/api/context.ts', 'createContextMemoryCandidate', 'candidate create API wrapper is missing')

assertContains('src/views/settings/EmbedOpsMonitor.vue', 'openPageMemoryWorkbench', 'page memory button handler is missing')
assertContains('src/views/settings/EmbedOpsMonitor.vue', 'PageMemoryWorkbench', 'page management must mount page memory workbench')
assertContains('src/views/settings/PageMemoryWorkbench.vue', '页面记忆工作台', 'page memory drawer title is missing')
assertContains('src/views/settings/PageMemoryWorkbench.vue', '手工填写', 'manual memory table is missing')
assertContains('src/views/settings/PageMemoryWorkbench.vue', 'AI Coding 工具扫描', 'AI coding scan table is missing')
assertContains('src/views/settings/PageMemoryWorkbench.vue', '从附件中提取', 'attachment extraction table is missing')
assertContains('src/views/settings/PageMemoryWorkbench.vue', 'saveManualPageMemory', 'manual page memory save flow is missing')
assertContains('src/views/settings/PageMemoryWorkbench.vue', 'saveAttachmentCandidate', 'attachment candidate save flow is missing')
assertContains('src/views/settings/PageMemoryWorkbench.vue', 'approvePageMemoryCandidate', 'candidate approve flow is missing')
assertContains('src/views/settings/PageMemoryWorkbench.vue', 'rejectPageMemoryCandidate', 'candidate reject flow is missing')
assertContains('src/views/settings/PageMemoryWorkbench.vue', 'pageMemoryNamespace', 'page namespace state is missing')

console.log('Page memory workbench UI contract check passed')
