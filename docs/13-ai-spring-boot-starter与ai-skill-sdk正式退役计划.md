# ai-spring-boot-starter 与 ai-skill-sdk 正式退役计划

## 1. 背景

ReachAI 为支持 JDK8 业务系统接入，已经新增并落地两个新模块：

- `reachai-capability-sdk`：Java 8 兼容、无 Spring 强绑定的业务能力声明 SDK。
- `reachai-spring-boot2-starter`：Java 8 兼容、Spring Boot 2.x 业务系统接入 starter。

这两个模块解决的是“业务系统如何接入 ReachAI”的问题，已经覆盖班组系统当前所需的主链路：项目注册、实例心跳、能力扫描同步、本地能力调用端点、签名注册请求、SDK 图同步和嵌入式页面策略上报。

历史模块仍在工程中：

- `ai-skill-sdk`
- `ai-spring-boot-starter`

它们现在不能直接删除。原因是两者承担的职责不同：

- `ai-spring-boot-starter` 主要是旧的业务系统主动接入 starter，适合退役。
- `ai-skill-sdk` 虽然名字像 SDK，但目前仍承载 `ai-agent-service` 内部 Tool / Skill 运行时契约，必须先迁移内部依赖后才能删除。

因此本计划采用“先接入面退役、再内部契约迁移、最后物理删除”的分阶段方案。

## 2. 当前事实

### 2.1 根工程模块

根 `pom.xml` 当前仍包含：

```text
ai-common
reachai-capability-sdk
reachai-spring-boot2-starter
ai-skill-sdk
ai-spring-boot-starter
ai-model-service
ai-skills-service
ai-agent-service
```

### 2.2 业务系统接入侧

以班组系统为例，`qmssmp-teams-construction-service` 已经切换到：

```xml
<artifactId>reachai-capability-sdk</artifactId>
<artifactId>reachai-spring-boot2-starter</artifactId>
```

业务代码使用：

```java
@ReachCapability
@ReachParam
ReachSideEffectLevel
```

这说明新接入链路已经能够替代旧的 `@AiCapability` / `@AiParam` 业务系统接入方式。

### 2.3 中台内部依赖

`ai-agent-service` 当前仍直接依赖：

```xml
<artifactId>ai-skill-sdk</artifactId>
```

并使用其中的内部运行时契约：

- `com.enterprise.ai.skill.AiTool`
- `com.enterprise.ai.skill.AiSkill`
- `com.enterprise.ai.skill.ToolRegistry`
- `com.enterprise.ai.skill.ToolParameter`
- `com.enterprise.ai.skill.SkillMetadata`
- `com.enterprise.ai.skill.SkillKind`
- `com.enterprise.ai.skill.SideEffectLevel`
- `com.enterprise.ai.skill.HitlPolicy`
- `com.enterprise.ai.skill.interaction.InteractionSpec`
- `com.enterprise.ai.skill.interaction.InteractionType`

这些契约服务于 Agent Runtime、MCP、Tool ACL、动态 HTTP Tool、SubAgent、Interactive Form 等中台内部能力，不应直接混入 JDK8 业务接入 SDK。

### 2.4 旧 starter 未迁移或需重新设计的能力

`ai-spring-boot-starter` 中仍存在以下历史能力：

- `EafRegistryClient`：旧注册客户端。
- `EafRegistryProperties`：旧 `eaf.*` 配置前缀。
- `EafCapabilityScanner`：旧 `@AiCapability` / `@AiParam` 扫描。
- `EafAgentGraphScanner` / `EafAgentGraph` / `EafGraph`：旧 SDK 图声明。
- `EafEmbedTokenService` / `EafEmbedTokenEndpoint`：旧业务服务侧 embed token 代理。
- `EafIdentityClient` / `EafUser`：旧用户同步客户端。
- `EmbeddedRuntimeEndpoint` / `EmbeddedRuntimeService`：旧嵌入式 Runtime 入口。
- `RuntimeGovernanceGuard`：旧运行时治理客户端守卫。
- `SdkDescriptionSourceSettingsHolder`：旧描述来源配置。

其中，业务注册与能力扫描已由 `reachai-spring-boot2-starter` 替代；embed token 代理后续应迁到业务网关统一 Broker；嵌入式 Runtime 不应放在 JDK8 业务系统 SDK 中。

## 3. 目标

### 3.1 总目标

正式退役历史业务系统接入链路，保留并强化新的 ReachAI 接入面，同时把中台内部 Tool / Capability 运行时契约从 `ai-skill-sdk` 中剥离出来，最终删除旧模块。

### 3.2 完成态

退役完成后，根工程模块应收敛为：

```text
ai-common
reachai-capability-sdk
reachai-spring-boot2-starter
ai-runtime-contract
ai-model-service
ai-skills-service
ai-agent-service
```

其中 `ai-runtime-contract` 是建议新增模块名，用于承载中台内部运行时契约。也可以命名为 `ai-agent-kernel-api`，但本文推荐 `ai-runtime-contract`，因为它表达的是运行时契约而不是业务系统 SDK。

### 3.3 非目标

本计划不做以下事情：

- 不把完整 Agent Runtime 下沉到业务系统 SDK。
- 不要求 JDK8 业务系统依赖 `ai-agent-service` 或 LangGraph4j。
- 不把 `ai-skill-sdk` 里的所有类简单复制到 `reachai-capability-sdk`。
- 不在退役过程中重命名数据库中的兼容敏感字段，例如 `skill_kind`、`spec_json`、`skill_refs_json`。
- 不为旧 `eaf.*` 配置长期维护双入口。

## 4. 退役原则

1. 新业务系统只使用 `reachai-capability-sdk` 和 `reachai-spring-boot2-starter`。
2. `ai-spring-boot-starter` 只允许作为历史兼容模块存在，不再新增功能。
3. `ai-skill-sdk` 先改名换位，再删除；不能在中台依赖未迁移前直接移除。
4. JDK8 接入 SDK 只负责声明、扫描、注册、签名和本地能力调用，不负责 Agent Runtime。
5. 中台内部运行时契约必须和业务接入 SDK 分离，避免再次出现 JDK8/JDK17、Spring Boot 2/Spring Boot 3 混用问题。
6. 退役过程必须保留可验证节点，每阶段都能独立编译、测试和回滚。

## 5. 目标模块边界

### 5.1 reachai-capability-sdk

定位：业务系统接入契约。

保留内容：

- `@ReachCapability`
- `@ReachParam`
- `@ReachOutput`
- `ReachSideEffectLevel`
- `ReachCapabilityDescriptor`
- `ReachCapabilityParameter`
- `ReachCapabilityScanner`
- `ReachAiSigner`
- `ReachAiSignatureHeaders`
- `ReachGraph`
- `ReachAgentGraph`
- `ReachGraphSpec`
- `ReachGraphSerializer`
- `ReachVars`

不放入：

- `AiTool`
- `AiSkill`
- `ToolRegistry`
- Agent Runtime 适配器
- LangGraph4j / AgentScope 依赖
- Spring Boot 3 自动配置

### 5.2 reachai-spring-boot2-starter

定位：JDK8 / Spring Boot 2 业务系统接入 starter。

保留和强化内容：

- `reachai.*` 配置。
- 项目注册。
- 实例心跳。
- 能力扫描同步。
- SDK 图同步。
- `/reachai/capabilities/{name}/invoke` 本地能力调用端点。
- `X-ReachAI-*` 签名。
- embed policy 上报。

不再加入：

- embed token 代理缓存。
- 完整 Agent Runtime。
- 用户体系批量同步。
- RunOps / Trace / Tool ACL 客户端治理逻辑。

### 5.3 ai-runtime-contract

定位：中台内部运行时契约。

建议从 `ai-skill-sdk` 迁入：

- `AiTool`
- `AiSkill`
- `ToolParameter`
- `ToolRegistry`
- `SkillMetadata`
- `SkillKind`
- `SideEffectLevel`
- `HitlPolicy`
- `InteractionSpec`
- `InteractionType`

建议包名：

```text
com.enterprise.ai.runtime.contract
com.enterprise.ai.runtime.contract.interaction
```

是否保留历史 `Skill` 命名：

- Java 类型短期可以保留 `AiSkill`、`SkillMetadata`、`SkillKind`，降低迁移风险。
- 包名和文档应表述为 Runtime Contract。
- 产品文案继续使用 Capability / 能力。

### 5.4 ai-agent-service

定位：JDK17 中台 Runtime Host 与控制面。

迁移后依赖：

```xml
<artifactId>ai-runtime-contract</artifactId>
```

不再依赖：

```xml
<artifactId>ai-skill-sdk</artifactId>
```

## 6. 分阶段计划

### 阶段 0：冻结旧入口

目标：阻止旧模块继续扩散。

动作：

1. 在 `ai-spring-boot-starter/pom.xml` 和 `ai-skill-sdk/pom.xml` 增加清晰说明，标记为 deprecated。
2. 在旧 starter 的 README 或模块说明中写明：新业务系统禁止继续接入该 starter。
3. 管理端“接入示例”改成 `reachai-spring-boot2-starter`、`reachai-capability-sdk`、`@ReachCapability`、`@ReachParam`。
4. 根 README 模块表改为：
   - 新接入 SDK：`reachai-capability-sdk`
   - 新接入 starter：`reachai-spring-boot2-starter`
   - 历史模块：`ai-skill-sdk`、`ai-spring-boot-starter`

验收：

- 全仓文档中不会再推荐新系统接入 `ai-spring-boot-starter`。
- 管理端复制示例不再输出旧 artifactId。
- 旧模块仍可编译，但不再作为推荐入口。

建议验证命令：

```powershell
rg -n "ai-spring-boot-starter|@AiCapability|@AiParam|eaf\\." README.md docs ai-admin-front/src
mvn -pl reachai-spring-boot2-starter -am test
```

### 阶段 1：补齐新接入 SDK 与 starter 的缺口

目标：确保旧 starter 中仍有价值的业务接入能力在新 starter 中有明确替代。

动作：

1. 对照 `EafRegistryClient` 和 `ReachAiRegistryClient`，确认以下能力已覆盖：
   - register project
   - heartbeat
   - offline
   - sync capabilities
   - sync agent graphs
   - signature headers
   - embed policy
2. 对照 `EafCapabilityScanner` 和 `ReachCapabilityBeanScanner`，确认以下能力已覆盖：
   - 注解扫描。
   - 参数名显式声明。
   - 请求体字段语义。
   - 副作用等级。
   - 本地 invoke endpoint。
3. 对旧 `EafEmbedTokenService` 做替代决策：
   - 不迁入 `reachai-spring-boot2-starter`。
   - 迁到业务网关侧统一 Token Broker。
4. 对旧 `EafIdentityClient` 做替代决策：
   - 如果只是嵌入式对话鉴权，不进入新 starter。
   - 如果平台需要企业用户同步，设计为独立的 `identity-sync` 管理接口或网关/身份中心集成。
5. 对旧 `EmbeddedRuntimeEndpoint` 做替代决策：
   - 不迁入新 starter。
   - 业务系统只暴露 Capability Host，本地不运行完整 Agent。

验收：

- 形成一张旧类到新方案的迁移矩阵。
- 班组系统只依赖新 SDK 仍能注册、心跳、同步能力并被页面嵌入调用。
- 新 starter 测试覆盖注册、心跳、能力同步、embed policy、本地能力调用。

建议验证命令：

```powershell
mvn -pl reachai-capability-sdk,reachai-spring-boot2-starter -am test
```

### 阶段 2：新增 ai-runtime-contract

目标：把 `ai-skill-sdk` 从“业务接入 SDK”身份中解放出来，变成中台内部运行时契约。

动作：

1. 新增模块：

```text
ai-runtime-contract/
```

2. 将 `ai-skill-sdk` 中运行时契约类迁入新模块：

```text
AiTool
AiSkill
ToolParameter
ToolRegistry
SkillMetadata
SkillKind
SideEffectLevel
HitlPolicy
interaction/InteractionSpec
interaction/InteractionType
```

3. 新包名建议：

```text
com.enterprise.ai.runtime.contract
com.enterprise.ai.runtime.contract.interaction
```

4. `ai-agent-service` 改依赖：

```xml
<artifactId>ai-runtime-contract</artifactId>
```

5. 批量替换 `ai-agent-service` 中 import：

```text
com.enterprise.ai.skill.*
-> com.enterprise.ai.runtime.contract.*
```

6. 保留类型名，不在这一阶段大改 `AiSkill`、`SkillMetadata` 等类名。

验收：

- `ai-agent-service` 不再依赖 `ai-skill-sdk`。
- `ai-agent-service` 中不存在 `import com.enterprise.ai.skill`。
- Tool、MCP、SubAgent、Interactive Form、Dynamic HTTP Tool 测试通过。

建议验证命令：

```powershell
rg -n "com\\.enterprise\\.ai\\.skill" ai-agent-service
mvn -pl ai-agent-service -am test
```

### 阶段 3：兼容桥与过渡发布

目标：降低删除旧 SDK 的一次性风险。

动作：

1. 暂时保留 `ai-skill-sdk`，但改成兼容桥模块：
   - 只依赖 `ai-runtime-contract`。
   - 旧包名下类标记 `@Deprecated`。
   - 对接口类可用继承或类型别名式包装。
2. 如果 Java 类型无法安全桥接，保留源类一版，但文档标记只用于历史编译。
3. `ai-spring-boot-starter` 暂时保留，但不再参与推荐接入。
4. 发布一个过渡版本，允许下游先从旧包迁移到新包。

验收：

- 根工程全量编译通过。
- 已知业务系统不再新增旧依赖。
- 管理端示例和 docs 已全部指向新 SDK。

建议验证命令：

```powershell
mvn test
rg -n "ai-spring-boot-starter|ai-skill-sdk|@AiCapability|@AiParam|eaf\\." README.md docs ai-admin-front/src
```

### 阶段 4：删除 ai-spring-boot-starter

目标：删除旧业务系统接入 starter。

前置条件：

- 新 starter 已覆盖业务接入主链路。
- 网关 Token Broker 方案已经落地或旧 `EafEmbedTokenService` 已明确废弃。
- 管理端和文档不再引用旧 starter 作为推荐入口。
- 没有业务系统继续依赖 `ai-spring-boot-starter`。

动作：

1. 从根 `pom.xml` 删除：

```xml
<module>ai-spring-boot-starter</module>
```

2. 删除 dependencyManagement 中旧 starter 相关条目，如果存在。
3. 删除目录：

```text
ai-spring-boot-starter/
```

4. 删除或改写文档中的旧 starter 推荐内容。
5. 更新 `docs/11-ReachAI-JDK8接入SDK与JDK17-Runtime分层方案.md`，说明旧 starter 已退役。

验收：

- `rg -n "ai-spring-boot-starter|com\\.enterprise\\.ai\\.spring" .` 只剩历史变更说明或退役说明。
- 根工程编译通过。

建议验证命令：

```powershell
mvn test
rg -n "ai-spring-boot-starter|com\\.enterprise\\.ai\\.spring" .
```

### 阶段 5：删除 ai-skill-sdk

目标：删除旧 SDK 模块。

前置条件：

- `ai-agent-service` 已迁移到 `ai-runtime-contract`。
- 全仓生产代码不存在 `com.enterprise.ai.skill` import。
- 旧兼容桥已经过至少一个过渡版本。
- 没有外部系统继续依赖 `ai-skill-sdk`。

动作：

1. 从根 `pom.xml` 删除：

```xml
<module>ai-skill-sdk</module>
```

2. 删除 dependencyManagement 中：

```xml
<artifactId>ai-skill-sdk</artifactId>
```

3. 删除目录：

```text
ai-skill-sdk/
```

4. 更新 README、docs、AGENTS.md、docs/ai-memory 中模块说明。
5. 如果仍需历史说明，只保留“已退役模块”章节，不再保留接入教程。

验收：

- `rg -n "ai-skill-sdk|com\\.enterprise\\.ai\\.skill" .` 不再命中生产代码和构建配置。
- 根工程全量测试通过。

建议验证命令：

```powershell
mvn test
rg -n "ai-skill-sdk|com\\.enterprise\\.ai\\.skill" .
```

## 7. 旧能力迁移矩阵

| 旧能力 | 旧位置 | 新位置或处理方式 | 退役策略 |
| --- | --- | --- | --- |
| `@AiCapability` | `ai-skill-sdk` | `@ReachCapability` in `reachai-capability-sdk` | 新业务禁止使用旧注解 |
| `@AiParam` | `ai-skill-sdk` | `@ReachParam` in `reachai-capability-sdk` | 新业务禁止使用旧注解 |
| `@AiOutput` | `ai-skill-sdk` | `@ReachOutput` in `reachai-capability-sdk` | 新业务禁止使用旧注解 |
| `SideEffectLevel` | `ai-skill-sdk` | 业务侧用 `ReachSideEffectLevel`；中台侧迁入 `ai-runtime-contract` | 双边拆分 |
| `AiTool` | `ai-skill-sdk` | `ai-runtime-contract` | 中台内部迁移 |
| `AiSkill` | `ai-skill-sdk` | `ai-runtime-contract` | 中台内部迁移 |
| `ToolRegistry` | `ai-skill-sdk` | `ai-runtime-contract` | 中台内部迁移 |
| `ToolParameter` | `ai-skill-sdk` | `ai-runtime-contract` | 中台内部迁移 |
| `SkillMetadata` | `ai-skill-sdk` | `ai-runtime-contract` | 中台内部迁移 |
| `InteractionSpec` | `ai-skill-sdk` | `ai-runtime-contract` | 中台内部迁移 |
| `EafRegistryClient` | `ai-spring-boot-starter` | `ReachAiRegistryClient` | 已有替代 |
| `EafRegistryProperties` | `ai-spring-boot-starter` | `ReachAiRegistryProperties` | 配置前缀从 `eaf.*` 转为 `reachai.*` |
| `EafCapabilityScanner` | `ai-spring-boot-starter` | `ReachCapabilityBeanScanner` / `ReachCapabilityScanner` | 已有替代 |
| `EafAgentGraphScanner` | `ai-spring-boot-starter` | `ReachGraph` / `ReachAgentGraph` / `ReachGraphSerializer` | 已有替代，继续补测试 |
| `EafEmbedTokenService` | `ai-spring-boot-starter` | 业务网关 Token Broker | 不迁入新 starter |
| `EafIdentityClient` | `ai-spring-boot-starter` | 独立身份同步方案或网关/身份中心集成 | 另行设计 |
| `EmbeddedRuntimeEndpoint` | `ai-spring-boot-starter` | ReachAI 中台 Runtime | 不迁入新 starter |
| `RuntimeGovernanceGuard` | `ai-spring-boot-starter` | 中台治理 + 注册中心策略 + 网关治理 | 不作为业务 SDK 必备能力 |

## 8. 风险与对策

### 8.1 误删 ai-skill-sdk 导致 ai-agent-service 编译失败

风险：`ai-agent-service` 仍使用 `AiTool`、`AiSkill`、`ToolRegistry` 等类型。

对策：先新增 `ai-runtime-contract` 并迁移 import，再删除旧模块。

### 8.2 把中台运行时契约塞进 reachai-capability-sdk

风险：业务系统 SDK 被中台运行时污染，再次引入 Java / Spring 版本冲突。

对策：`reachai-capability-sdk` 只保留业务接入声明、签名、图声明和扫描契约；中台运行时契约单独成模块。

### 8.3 embed token 代理迁移方向不清晰

风险：如果继续放在每个业务服务，会导致每个服务复制 token 逻辑。

对策：旧 `EafEmbedTokenService` 不迁入新 starter，统一规划到 `qmssmp-gateway` 或业务网关层的 Token Broker。

### 8.4 文档和管理端示例继续推荐旧 SDK

风险：新接入系统继续复制旧 artifactId 和旧注解。

对策：阶段 0 优先改管理端示例和 README，旧名只出现在退役说明和历史说明中。

### 8.5 外部系统仍依赖旧包名

风险：直接删除会影响尚未迁移的系统。

对策：保留一个过渡版本，旧包名标记 deprecated；删除前由业务系统清单确认无依赖。

## 9. 推荐时间线

| 阶段 | 内容 | 建议工期 | 可并行性 |
| --- | --- | --- | --- |
| 阶段 0 | 冻结旧入口，更新文档和管理端示例 | 0.5-1 天 | 可立即做 |
| 阶段 1 | 补齐新 starter 缺口与迁移矩阵 | 1-2 天 | 可与网关 Token Broker 设计并行 |
| 阶段 2 | 新增 `ai-runtime-contract` 并迁移 `ai-agent-service` | 1-2 天 | 需集中处理 |
| 阶段 3 | 兼容桥与过渡发布 | 0.5-1 天 | 依赖阶段 2 |
| 阶段 4 | 删除 `ai-spring-boot-starter` | 0.5 天 | 依赖阶段 1 与网关 token 结论 |
| 阶段 5 | 删除 `ai-skill-sdk` | 0.5 天 | 依赖阶段 2/3 和外部依赖确认 |

## 10. 建议执行顺序

推荐先执行：

1. 更新 README、docs、管理端接入示例，阻止新接入继续使用旧 starter。
2. 完成网关 Token Broker 设计，替代旧 `EafEmbedTokenService`。
3. 新增 `ai-runtime-contract`，迁移 `ai-agent-service` 内部 Tool / Skill 契约。
4. 删除 `ai-spring-boot-starter`。
5. 等一个过渡窗口后删除 `ai-skill-sdk`。

不推荐的顺序：

1. 直接删除 `ai-skill-sdk`。
2. 把 `AiTool`、`AiSkill`、`ToolRegistry` 放进 `reachai-capability-sdk`。
3. 在 `reachai-spring-boot2-starter` 里继续补齐旧 starter 的所有 Runtime 能力。

## 11. 最终验收清单

- [x] 新业务系统接入文档只推荐 `reachai-capability-sdk` 和 `reachai-spring-boot2-starter`。
- [x] 管理端注册中心示例不再展示旧 artifactId。
- [ ] `qmssmp-teams-construction-service` 等业务系统不依赖旧 SDK。
- [x] `ai-agent-service` 不依赖 `ai-skill-sdk`。
- [x] 全仓生产代码不存在 `import com.enterprise.ai.skill`。
- [x] 全仓生产代码不存在 `import com.enterprise.ai.spring.registry`。
- [x] 根 `pom.xml` 不再包含 `ai-skill-sdk` 和 `ai-spring-boot-starter` module。
- [x] 根工程 `mvn test` 通过。
- [x] 退役说明已写入 README 或 docs 索引。

## 12. 当前结论

当前已完成 `ai-skill-sdk` 和 `ai-spring-boot-starter` 的工程内退役。

`ai-spring-boot-starter` 已从根 Maven module 和源码目录中删除。嵌入式页面对话所需的 Token Broker 不再放在旧 starter 中补能力，后续应走业务网关或统一平台入口设计。

`ai-skill-sdk` 承载过的中台内部 Tool / Skill 运行时契约已迁入 `ai-runtime-contract`，`ai-agent-service` 已切换到新契约模块。
