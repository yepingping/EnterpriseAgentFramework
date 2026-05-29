# AI Memory

本目录存放给 AI 编程工具看的项目记忆。它不是对外产品文档，也不是替代 `README.md` 的入口；它的目标是让 Cursor、Codex、Claude Code、Copilot CLI 等工具在跨会话、跨工具协作时快速继承项目上下文。

## 阅读顺序

1. 根目录 `AGENTS.md`：最高优先级执行规则。
2. `.cursor/rules/reachai-project.mdc`：Cursor 自动加载规则。
3. 本目录文件：
   - `PROJECT-MEMORY.md`：项目定位、模块地图、当前事实。
   - `WORKING-RULES.md`：开发、SQL、验证、协作规则。
   - `DECISIONS.md`：已经形成的架构和命名决策。
   - `KNOWN-PITFALLS.md`：历史问题、错误签名和诊断顺序。
   - `VERIFICATION.md`：常用验证命令。
   - `AI-TOOLS.md`：Playwright 浏览器调试和 DBHub MySQL 只读查询约定。
   - `HISTORY-INDEX.md`：历史工作主题索引，帮助后续 Agent 快速定位上下文。

## 维护规则

- 当架构、SQL、运行时、命名或重要工作流发生变化时，同步更新这里。
- 这里可以记录“AI 需要知道的上下文”，但不要记录密码、密钥、真实生产凭据或个人机器路径。
- 如果内容与当前代码冲突，以当前代码为准，并立刻更新记忆。
- 不要把一次性讨论稿堆进这里；只保留会影响未来修改判断的事实和规则。
