# ReachAI Verification Commands

这些命令是 AI 编程工具完成修改后的常用验证入口。根据任务范围选择最小但真实的验证集合。

## General

```powershell
git status --short
git diff --check
```

## Backend

根目录编译：

```powershell
mvn clean install
```

目标模块编译：

```powershell
mvn -pl ai-agent-service -am compile
mvn -pl ai-skills-service -am compile
mvn -pl ai-model-service -am compile
mvn -pl ai-spring-boot-starter -am compile
```

目标测试示例：

```powershell
mvn -pl ai-agent-service "-Dtest=LlmWorkflowDraftGeneratorTest,WorkflowDraftGenerationServiceTest,WorkflowDraftEditServiceTest" test
mvn -pl ai-spring-boot-starter test
```

## Frontend

```powershell
cd ai-admin-front
npm run build
```

类型检查：

```powershell
cd ai-admin-front
npx vue-tsc --noEmit
```

## SQL

检查目标表、列、索引是否在基线和升级脚本中同时出现：

```powershell
rg -n "target_table|target_column|target_index" sql\init.sql sql\upgrade-*.sql
```

有 MySQL 环境时执行：

```powershell
mysql -uroot -p < sql/init.sql
mysql -uroot -p ai_text_service < sql/upgrade-YYYYMMDD-short-name.sql
```

如果 Cursor/Codex 已配置 `dbhub_ai_mysql`，可用它做只读 live-schema 验证，例如：

```text
Use dbhub_ai_mysql to check whether target_table exists and list its columns and indexes. Do not modify data.
```

## Browser UI

如果 Cursor/Codex 已配置 Playwright MCP，可用它做 UI 验证和截图，例如：

```text
Use Playwright to open the local admin frontend, capture a screenshot, and report visible layout, console, and network errors.
```

## Docs And Links

检查 Markdown 中的本地链接和关键路径时，优先用 `rg` 做静态确认：

```powershell
rg -n "docs/ai-memory|AGENTS.md|sql/init.sql|GraphSpec|AgentRuntimeAdapter" README.md docs AGENTS.md .cursor/rules
```

## When Verification Cannot Run

最终回复必须明确说明：

- 哪个验证没跑。
- 为什么没跑。
- 已经做了什么替代检查。
