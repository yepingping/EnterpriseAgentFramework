import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const javaRoot = 'ai-agent-service/src/main/java'
const testRoot = 'ai-agent-service/src/test/java'
let failures = 0

// Runtime Host should not depend on Platform Control implementations. Keep this
// empty unless a transition needs a reviewed, temporary exception.
const runtimeHostPlatformAllowlist = new Map([
])

function toPosix(value) {
  return value.replace(/\\/g, '/')
}

function walkJava(rel) {
  const base = path.join(root, rel)
  if (!fs.existsSync(base)) {
    return []
  }
  const files = []
  const stack = [base]
  while (stack.length > 0) {
    const current = stack.pop()
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const target = path.join(current, entry.name)
      if (entry.isDirectory()) {
        stack.push(target)
      } else if (entry.name.endsWith('.java')) {
        files.push(toPosix(path.relative(root, target)))
      }
    }
  }
  return files.sort()
}

function read(rel) {
  return fs.readFileSync(path.join(root, rel), 'utf8')
}

function packageName(text) {
  return text.match(/^package\s+([^;]+);/m)?.[1] ?? ''
}

function imports(text) {
  return [...text.matchAll(/^import\s+(?:static\s+)?([^;]+);/gm)].map((match) => match[1])
}

function javaFiles() {
  return [...walkJava(javaRoot), ...walkJava(testRoot)]
}

function sourceFiles() {
  return walkJava(javaRoot)
}

function sourceInfos() {
  return sourceFiles().map((file) => {
    const text = read(file)
    return { file, text, pkg: packageName(text), imports: imports(text) }
  })
}

function report(label, matches) {
  if (matches.length === 0) {
    return
  }
  console.error(`[domain dependency] ${label}`)
  for (const match of matches) {
    const reason = match.reason ? ` (${match.reason})` : ''
    console.error(`  ${match.file}: ${match.importName}${reason}`)
  }
  failures += matches.length
}

function isUnderPackage(pkg, prefix) {
  return pkg === prefix || pkg.startsWith(`${prefix}.`)
}

const infos = sourceInfos()

report('runtime contract must not import implementation domains',
  infos.flatMap((info) => {
    if (!isUnderPackage(info.pkg, 'com.enterprise.ai.agent.runtime')
        || isUnderPackage(info.pkg, 'com.enterprise.ai.agent.runtime.host')) {
      return []
    }
    return info.imports
      .filter((importName) => /^(com\.enterprise\.ai\.agent\.runtime\.host|com\.enterprise\.ai\.agent\.platform\.control|com\.enterprise\.ai\.agent\.capability\.catalog)\./.test(importName))
      .map((importName) => ({ file: info.file, importName }))
  }))

report('capability.catalog must not import runtime.host or platform.control',
  infos.flatMap((info) => {
    if (!isUnderPackage(info.pkg, 'com.enterprise.ai.agent.capability.catalog')) {
      return []
    }
    return info.imports
      .filter((importName) => /^(com\.enterprise\.ai\.agent\.runtime\.host|com\.enterprise\.ai\.agent\.platform\.control)\./.test(importName))
      .map((importName) => ({ file: info.file, importName }))
  }))

report('runtime.host must not import platform.control',
  infos.flatMap((info) => {
    if (!isUnderPackage(info.pkg, 'com.enterprise.ai.agent.runtime.host')) {
      return []
    }
    return info.imports
      .filter((importName) => importName.startsWith('com.enterprise.ai.agent.platform.control.'))
      .filter((importName) => {
        const key = `${info.file}::${importName}`
        return !runtimeHostPlatformAllowlist.has(key)
      })
      .map((importName) => ({ file: info.file, importName }))
  }))

const staleRootRefs = [
  {
    pattern: /com\.enterprise\.ai\.agent\.controller(?:\.|;|\s|$)/,
    description: 'com.enterprise.ai.agent.controller'
  },
  {
    pattern: /com\.enterprise\.ai\.agent\.service(?:\.|;|\s|$)/,
    description: 'com.enterprise.ai.agent.service'
  },
  {
    pattern: /com\.enterprise\.ai\.agent\.client\.ScannerServiceClient(?:;|\s|$)/,
    description: 'com.enterprise.ai.agent.client.ScannerServiceClient'
  },
  {
    pattern: /com\.enterprise\.ai\.agent\.config\.DomainProperties(?:;|\s|$)/,
    description: 'com.enterprise.ai.agent.config.DomainProperties'
  },
  {
    pattern: /com\.enterprise\.ai\.agent\.config\.ToolRetrievalProperties(?:;|\s|$)/,
    description: 'com.enterprise.ai.agent.config.ToolRetrievalProperties'
  },
  {
    pattern: /com\.enterprise\.ai\.agent\.config\.ToolRateLimitProperties(?:;|\s|$)/,
    description: 'com.enterprise.ai.agent.config.ToolRateLimitProperties'
  }
]

for (const { pattern, description } of staleRootRefs) {
  const matches = javaFiles().flatMap((file) => {
    const lines = read(file).split(/\r?\n/)
    return lines.flatMap((line, index) => (
      pattern.test(line)
        ? [{ file: `${file}:${index + 1}`, importName: description }]
        : []
    ))
  })
  report(`stale root package reference: ${description}`, matches)
}

if (runtimeHostPlatformAllowlist.size > 0) {
  console.log(`runtime.host -> platform.control allowlist entries: ${runtimeHostPlatformAllowlist.size}`)
}

if (failures > 0) {
  console.error(`backend domain dependency check failed: ${failures} issue(s)`)
  process.exit(1)
}

console.log('backend domain dependency check passed')
