import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { join } from 'node:path'
import vm from 'node:vm'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const modulePath = join(process.cwd(), 'src/views/registry/pageAssistantOnboardingPrompt.ts')
const source = readFileSync(modulePath, 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2020,
  },
})

const sandbox = {
  exports: {},
  require,
  console,
}

vm.runInNewContext(compiled.outputText, sandbox, { filename: modulePath })

const { buildPageAssistantOnboardingPrompt } = sandbox.exports
assert.equal(typeof buildPageAssistantOnboardingPrompt, 'function')

const prompt = buildPageAssistantOnboardingPrompt({
  toolName: 'Cursor',
  platformUrl: 'http://localhost:18603',
  project: {
    id: 7,
    projectCode: 'bzjs3',
    name: '班组系统',
    appKey: 'bzjs3',
  },
  page: {
    pageKey: 'teamArchive.list',
    name: '班组档案',
    routePattern: '/teams/archive',
  },
  actions: [
    {
      actionKey: 'getPageState',
      title: '读取页面状态',
      description: '读取当前筛选条件、分页、表格数据',
      confirmRequired: false,
    },
    {
      actionKey: 'openRowAction',
      title: '打开周期管理',
      description: '打开行内周期管理入口',
      confirmRequired: true,
    },
  ],
  progress: {
    aiCodingAccessKey: 'rai_test_key',
    appSecretEnv: 'REACHAI_REGISTRY_APP_SECRET',
    sessionId: 'rai_page_123',
    manifestUrl: 'http://localhost:18603/api/ai-assist/projects/7/page-assistant/onboarding-manifest?aiCodingKey=rai_test_key',
    latestSessionUrl: 'http://localhost:18603/api/ai-assist/projects/7/page-assistant/sessions/latest?aiCodingKey=rai_test_key',
    stepReportUrl: 'http://localhost:18603/api/ai-assist/projects/7/page-assistant/sessions/rai_page_123/steps/{stepKey}/report?aiCodingKey=rai_test_key',
    targetBindUrl: 'http://localhost:18603/api/ai-assist/projects/7/page-assistant/sessions/rai_page_123/target?aiCodingKey=rai_test_key',
    catalogSyncUrl: 'http://localhost:18603/api/ai-assist/projects/7/page-assistant/sessions/rai_page_123/catalog/sync?aiCodingKey=rai_test_key',
    checksRunUrl: 'http://localhost:18603/api/ai-assist/projects/7/page-assistant/sessions/rai_page_123/checks/run?aiCodingKey=rai_test_key',
    registerPageUrl: 'http://localhost:18603/api/ai-assist/projects/7/page-assistant/pages/register?aiCodingKey=rai_test_key',
    scaffoldCommand: '.\\scripts\\reachai-page-assistant.ps1 scaffold -ManifestUrl "http://localhost:18603/api/ai-assist/projects/7/page-assistant/onboarding-manifest?aiCodingKey=rai_test_key" -Framework angular -OutputDir ".\\src\\app\\shared\\reachai"',
    verifyCommand: '.\\scripts\\reachai-page-assistant.ps1 verify -ManifestUrl "http://localhost:18603/api/ai-assist/projects/7/page-assistant/onboarding-manifest?aiCodingKey=rai_test_key" -FrontendUrl "http://localhost:9200" -Route "/teams/archive" -PageKey "teamArchive.list"',
  },
})

assert.match(prompt, /当前任务是“页面助手接入”/)
assert.match(prompt, /项目级 SDK \/ 网关 \/ embed token broker/)
assert.match(prompt, /不要为了简单查询绕过页面去新增后端 API Tool/)
assert.match(prompt, /必须先询问用户确认具体要改造哪一个业务页面/)
assert.match(prompt, /不要把候选页面列表回传 ReachAI 平台/)
assert.match(prompt, /App Key：bzjs3/)
assert.match(prompt, /AI Coding 接入秘钥：rai_test_key/)
assert.match(prompt, /App Secret 环境变量：REACHAI_REGISTRY_APP_SECRET/)
assert.match(prompt, /页面助手接入清单 URL/)
assert.match(prompt, /步骤进度回传 URL/)
assert.match(prompt, /目标页绑定 URL/)
assert.match(prompt, /页面动作目录同步 URL/)
assert.match(prompt, /页面助手自检 URL/)
assert.match(prompt, /页面一键注册 URL/)
assert.match(prompt, /registerPage/)
assert.match(prompt, /本机 PowerShell\/curl/)
assert.match(prompt, /远端 WebFetch/)
assert.match(prompt, /reachai-page-assistant\.ps1 scaffold/)
assert.match(prompt, /reachai-page-assistant\.ps1 verify/)
assert.match(prompt, /window\.__REACHAI_PAGE_BRIDGE__/)
assert.match(prompt, /绑定目标页时/)
assert.match(prompt, /同步目录时/)
assert.match(prompt, /page-assistant/)
assert.match(prompt, /route/)
assert.match(prompt, /页面组件文件/)
assert.match(prompt, /筛选表单字段/)
assert.match(prompt, /查询 \/ 重置按钮/)
assert.match(prompt, /表格列/)
assert.match(prompt, /分页/)
assert.match(prompt, /行操作按钮/)
assert.match(prompt, /getPageState/)
assert.match(prompt, /setFilters/)
assert.match(prompt, /search/)
assert.match(prompt, /reset/)
assert.match(prompt, /readTable/)
assert.match(prompt, /openRowAction/)
assert.match(prompt, /新增、编辑、删除、审批、导出/)
assert.match(prompt, /不要绕过页面权限/)
assert.match(prompt, /最小构建 \/ 类型检查/)
assert.match(prompt, /page-manifest/)
assert.match(prompt, /route-detection/)
assert.match(prompt, /handoff-summary/)
assert.doesNotMatch(prompt, /secret-must-not-leak/)
assert.doesNotMatch(prompt, /App Secret：/)

console.log('page assistant onboarding prompt assertions passed')
