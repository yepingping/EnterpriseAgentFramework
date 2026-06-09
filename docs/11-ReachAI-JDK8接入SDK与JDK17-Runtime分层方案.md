# ReachAI JDK8 接入 SDK 与 JDK17 Runtime 分层落地说明

## 核心结论

ReachAI 已按以下边界落地，不再采用“两套完整 SDK”的路线：

```text
JDK8 兼容的业务系统接入 SDK
+
JDK17 的完整 Agent Runtime
```

JDK8 侧只做业务系统接入：声明 Capability、扫描和注册能力、同步 SDK 图、暴露本地 Capability 调用入口。完整 Agent Runtime 只运行在 ReachAI 中台 JDK17 服务中，当前承载点是 `ai-agent-service` 的 `LangGraph4jRuntimeAdapter`、`AgentScopeRuntimeAdapter` 和 `AgentRuntimeAdapter` 体系。

这份文档记录当前已经实现的代码结构、协议边界和验证方式。原先的目标规划已不再作为依据。

## 已落地模块

根工程已新增两个 JDK8 接入模块：

```text
reachai-capability-sdk
reachai-spring-boot2-starter
```

根 `pom.xml` 当前保留的新接入模块和运行时模块：

```text
ai-common
reachai-capability-sdk
reachai-spring-boot2-starter
ai-runtime-contract
ai-model-service
ai-skills-service
ai-agent-service
```

其中：

- `reachai-capability-sdk` 是 Java 8、无 Spring 强绑定的业务能力契约 SDK。
- `reachai-spring-boot2-starter` 是 Java 8、Spring Boot 2.x 业务系统接入 starter。
- `ai-runtime-contract` 是 Java 17 中台内部 Tool / Skill 运行时契约模块。
- `ai-agent-service` 继续作为 Java 17 中台控制面和 Runtime Host。
- 历史 `ai-skill-sdk`、`ai-spring-boot-starter` 已正式退役，不再作为工程模块保留。

## 命名边界

新接入链路使用 ReachAI / Reach / Capability 语义：

- Java 包名使用 `com.enterprise.ai.reach.*`。
- 配置前缀使用 `reachai.*`。
- 注解使用 `@ReachCapability`、`@ReachParam`、`@ReachOutput`。
- 签名头使用 `X-ReachAI-*`。
- 业务实例角色使用 `CAPABILITY_HOST`。

历史代码中仍存在 `Skill`、`EAF` 等命名，它们属于旧实现或历史兼容边界。新增 SDK、starter、文档示例和新协议不再扩散这些命名。

## reachai-capability-sdk

模块位置：

```text
reachai-capability-sdk/
```

主要包结构：

```text
com.enterprise.ai.reach.sdk.annotation
com.enterprise.ai.reach.sdk.auth
com.enterprise.ai.reach.sdk.capability
com.enterprise.ai.reach.sdk.client
com.enterprise.ai.reach.sdk.graph
```

当前已落地能力：

- `ReachCapability`：声明业务能力。
- `ReachParam`：声明入参，支持显式 `name`，避免 JDK8 参数名编译信息缺失。
- `ReachOutput`：声明出参。
- `ReachSideEffectLevel`：声明副作用等级。
- `ReachCapabilityDescriptor` / `ReachCapabilityParameter`：能力描述模型。
- `ReachCapabilityScanner`：扫描 `@ReachCapability` 方法并生成描述。
- `ReachAiSignatureHeaders` / `ReachAiSigner`：生成 `X-ReachAI-*` 签名头。
- `ReachGraph` / `ReachAgentGraph` / `ReachGraphSpec` / `ReachGraphSerializer` / `ReachVars`：生成可提交到中台的 `GraphSpec` 风格图描述。

这个模块不包含本地 Agent Runtime，不提供 `graph.run()` 或 `execute()` 之类的完整图执行 API。业务系统可以声明图，但图的执行由 JDK17 中台 Runtime 完成。

### Capability 示例

```java
@ReachCapability(
    name = "contract.query",
    title = "查询合同",
    description = "根据合同编号查询合同详情",
    domain = "contract",
    module = "review",
    sideEffect = ReachSideEffectLevel.READ
)
public ContractDTO queryContract(
    @ReachParam(name = "contractNo", description = "合同编号", required = true)
    String contractNo,
    @ReachParam(name = "includeAttachments", description = "是否返回附件", required = false)
    Boolean includeAttachments
) {
    return contractService.query(contractNo, includeAttachments);
}
```

### Graph 示例

```java
ReachAgentGraph graph = ReachGraph.agent("contract-review")
    .name("合同审查助手")
    .modelInstanceId("default-qwen")
    .llm("classify")
        .name("识别审查类型")
        .systemPrompt("判断用户需要审查合同风险、抽取条款还是生成意见")
        .outputAlias("intent")
    .capability("queryContract")
        .name("查询合同")
        .qualifiedName("contract.query")
        .input("contractNo", ReachVars.input("contractNo"))
        .outputAlias("contract")
    .answer("final")
        .from("contract")
    .edge(ReachGraph.START, "classify")
    .edge("classify", "queryContract")
    .edge("queryContract", "final")
    .edge("final", ReachGraph.END)
    .build();

String graphSpecJson = ReachGraphSerializer.toJson(graph.getGraphSpec());
```

## reachai-spring-boot2-starter

模块位置：

```text
reachai-spring-boot2-starter/
```

主要类：

```text
ReachAiRegistryProperties
ReachAiRegistryAutoConfiguration
ReachCapabilityBeanScanner
ReachAiRegistryClient
ReachAiRegistryTransport
ReachAiHttpRegistryTransport
ReachCapabilityInvoker
ReachCapabilityEndpoint
```

当前已落地能力：

- Spring Boot 2 自动配置通过 `META-INF/spring.factories` 生效。
- 通过 `reachai.*` 配置绑定注册中心、项目、实例和能力扫描参数。
- 启动时扫描 Spring Bean 中的 `@ReachCapability`。
- 向平台注册项目。
- 发送实例心跳。
- 同步 Capability 到平台能力资产目录。
- 暴露本地 Capability 调用入口：`POST /reachai/capabilities/{capabilityName}/invoke`。
- 注册同步时将能力标记为 `REACHAI_CAPABILITY_HTTP` 调用协议。
- 心跳 metadata 将业务系统标记为 `CAPABILITY_HOST`，而不是完整 Agent Runtime。
- 注册中心不可用时采用 fail-soft，不阻断业务系统启动。

### 配置示例

```yaml
reachai:
  enabled: true
  registry-url: http://reachai.example.com
  app-key: demo-key
  app-secret: demo-secret
  project-code: contract-service
  project-name: 合同系统
  base-url: http://contract-service.internal
  environment: prod
  sync-on-startup: true
  heartbeat-on-startup: true
```

### 调用入口

业务系统 starter 暴露：

```text
POST /reachai/capabilities/{capabilityName}/invoke
Content-Type: application/json
```

请求体是按 Capability 参数名组织的 JSON 对象：

```json
{
  "contractNo": "HT-001",
  "includeAttachments": true
}
```

`ReachCapabilityInvoker` 会根据 `{capabilityName}` 找到对应 `@ReachCapability` 方法，并按参数名反射调用。

## 中台注册与 Runtime 支持

### 注册中心签名头

`ai-agent-service` 的注册入口已经支持新签名头：

```text
X-ReachAI-App-Key
X-ReachAI-Timestamp
X-ReachAI-Nonce
X-ReachAI-Signature
```

当前为了历史链路平滑，服务端仍兼容旧头；新 SDK 和新文档只使用 `X-ReachAI-*`。

涉及代码：

```text
ai-agent-service/src/main/java/com/enterprise/ai/agent/controller/AiRegistryController.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/controller/EmbedChatController.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/identity/EmbedCorsFilter.java
```

### Runtime Registry 角色

平台 Runtime Registry 已区分两类实例：

```text
AGENT_RUNTIME
CAPABILITY_HOST
```

判断规则：

- metadata 中 `runtimeRole=CAPABILITY_HOST` 时视为能力接入侧。
- metadata 中 `runtimePlacement=CAPABILITY_HOST` 时视为能力接入侧。
- metadata 中 `runtimeTypes` 包含 `CAPABILITY_HOST` 时视为能力接入侧。
- 其他平台 Runtime Adapter 默认视为 `AGENT_RUNTIME`。

涉及代码：

```text
ai-agent-service/src/main/java/com/enterprise/ai/agent/runtime/RuntimeRegistryEntry.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/runtime/RuntimeRegistryService.java
ai-agent-service/src/main/java/com/enterprise/ai/agent/runtime/RuntimeInstanceRoles.java
```

前端展示已同步：

```text
ai-admin-front/src/types/agent.ts
ai-admin-front/src/utils/registryLabels.ts
ai-admin-front/src/views/registry/RuntimeRegistry.vue
```

Runtime Registry 页面会同时展示 Agent Runtime 与 Capability Host 数量，并支持按角色过滤。

### 发布与运行校验

Capability Host 不能作为 Agent Runtime 执行目标。平台已在两个位置做约束：

- 发布校验：`AgentReleaseValidationService`。
- 运行调度：`EmbeddedRuntimeDispatchService`。

如果用户在 Agent 配置里选择了 Capability Host 作为嵌入式或混合 Runtime 目标，平台会拒绝：

```text
Capability Host 只能提供业务能力调用，不能作为 Agent Runtime 执行目标
```

前端 `AgentEdit.vue` 也会过滤在线实例列表，只允许选择真正的 Agent Runtime 实例。

## Runtime 调用 Capability Host

中台 Runtime 调用 JDK8 Capability Host 的关键协议已经落地在动态 HTTP Tool 中：

```text
ai-agent-service/src/main/java/com/enterprise/ai/agent/tools/dynamic/DynamicHttpAiTool.java
```

当能力元数据中包含：

```json
{
  "invokeProtocol": "REACHAI_CAPABILITY_HTTP"
}
```

Runtime 会按 Capability Host 协议发送请求：

- HTTP body 使用完整参数 Map。
- 不再使用旧动态 HTTP Tool 的“单个 BODY 参数值”模式。
- 自动透传新的 `X-ReachAI-*` 运行上下文头。
- 为历史链路保留旧上下文头，但新业务侧应读取 `X-ReachAI-*`。

示例：

```json
{
  "contractNo": "HT-001",
  "includeAttachments": true
}
```

这解决了多参数 Capability 只发送最后一个 BODY 值的问题。

## 数据流

### 业务系统启动注册

```text
Spring Boot 2 业务系统启动
  -> ReachAiRegistryAutoConfiguration 创建注册组件
  -> ReachCapabilityBeanScanner 扫描 @ReachCapability
  -> ReachAiRegistryClient 注册项目
  -> ReachAiRegistryClient 发送 CAPABILITY_HOST 心跳
  -> ReachAiRegistryClient 同步 Capability
  -> ai-agent-service 生成能力资产和调用元数据
```

### 中台执行 Agent 并调用业务能力

```text
用户触发 Agent
  -> ai-agent-service 加载 AgentDefinition / GraphSpec
  -> LangGraph4jRuntimeAdapter 执行图
  -> Capability/Tool 节点解析到动态 HTTP Tool
  -> DynamicHttpAiTool 识别 REACHAI_CAPABILITY_HTTP
  -> Runtime 发送 JSON 参数到 /reachai/capabilities/{name}/invoke
  -> JDK8 业务系统 ReachCapabilityEndpoint 接收请求
  -> ReachCapabilityInvoker 调用本地 @ReachCapability 方法
  -> 结果回到 Runtime，写入节点输出、Trace 和 RunOps
```

### Runtime Registry 展示

```text
项目实例心跳
  -> RuntimeRegistryService 读取 metadata
  -> 计算 runtimeRole
  -> RuntimeRegistry.vue 展示 Agent Runtime / Capability Host
  -> AgentEdit.vue 只允许选择 AGENT_RUNTIME
```

## 企业级边界

### 安全

- 注册、心跳、同步使用 `ReachAiSigner` 生成签名头。
- 新协议统一使用 `X-ReachAI-*`。
- 服务端读取新头，兼容旧头。
- Capability 调用会携带 trace、session、user、tenant、roles 等上下文头。

### 治理

- Capability 元数据包含副作用等级、领域、模块、入参、出参和 endpoint。
- Capability Host 不进入 Agent Runtime 目标池。
- 平台 Runtime 仍承担 Tool ACL、Guard、Trace、RunOps 和发布校验。

### 版本与隔离

- JDK8 SDK 只依赖 GraphSpec JSON 合同，不依赖平台 Java 17 Runtime 实现类。
- 业务系统通过 projectCode 和 capabilityName/qualifiedName 形成稳定引用。
- 业务实例下线后不会被当作可用 Runtime 目标。

### 可观测

- Runtime 调用 Capability Host 时保留 traceId、sessionId 和用户上下文。
- 节点执行、远程能力调用、模型调用仍由中台 Trace / RunOps 汇总。
- JDK8 SDK 只承担轻量接入，不下沉完整运行观测系统。

## 已验证

后端相关测试：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -pl ai-agent-service test
```

当前验证结果：

```text
Tests run: 198, Failures: 0, Errors: 0, Skipped: 0
```

JDK8 SDK 和 Spring Boot 2 starter 测试：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -pl reachai-spring-boot2-starter -am test
```

当前验证结果：

```text
reachai-capability-sdk: Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
reachai-spring-boot2-starter: Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

字节码版本验证：

```powershell
$classes = Get-ChildItem -Path reachai-capability-sdk\target\classes,reachai-spring-boot2-starter\target\classes -Recurse -Filter *.class
$versions = foreach ($class in $classes) { (& javap -verbose $class.FullName | Select-String 'major version').ToString().Trim() }
$versions | Sort-Object -Unique
```

当前验证结果：

```text
major version: 52
```

前端验证：

```powershell
$env:PATH='C:\Users\w8123\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;' + $env:PATH
npx vue-tsc --noEmit
npm run build
```

当前验证结果：类型检查和构建通过。构建中存在 Vite chunk size warning，不影响本次分层能力。

文档与空白检查：

```powershell
git diff --check
```

当前验证结果：通过，仅有 Windows LF/CRLF 提示。

## 当前边界

已经完成：

- JDK8 纯 SDK 模块。
- Spring Boot 2 接入 starter。
- Reach 命名的新注解、配置和签名头。
- Capability Host 心跳和注册元数据。
- Capability Host 本地调用 endpoint。
- Runtime Registry 角色区分。
- Agent 发布和运行时禁止把 Capability Host 当作 Agent Runtime。
- Runtime 调用 `REACHAI_CAPABILITY_HTTP` 时发送完整参数 body。
- 前端 Runtime Registry 和 Agent Runtime 选择器适配。

仍保留的历史边界：

- 旧 `ai-skill-sdk` 与 `ai-spring-boot-starter` 已从工程模块中移除；中台内部运行时契约由 `ai-runtime-contract` 承载。
- 服务端仍兼容旧签名头，便于已有链路平滑过渡。
- 完整 Runtime 仍在 `ai-agent-service` 内，尚未抽成独立 `reachai-agent-runtime` 模块。
- SQL 表名和部分内部类名仍包含历史 `skill` 语义，当前不为旧数据兼容做额外迁移。

后续如要继续推进，优先做两件事：

- 用一个真实 JDK8/Spring Boot 2 demo app 做端到端接入样例。
- 将旧 starter 的文档入口逐步切到新 `reachai-spring-boot2-starter`，避免新用户继续接入旧链路。
