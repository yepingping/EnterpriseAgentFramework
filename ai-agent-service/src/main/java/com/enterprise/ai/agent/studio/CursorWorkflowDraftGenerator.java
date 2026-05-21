package com.enterprise.ai.agent.studio;

import com.enterprise.ai.agent.graph.AgentGraphSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class CursorWorkflowDraftGenerator implements WorkflowDraftGenerator {

    private static final String PROVIDER = "CURSOR_SDK";
    private final WorkflowDraftProperties properties;

    public CursorWorkflowDraftGenerator() {
        this(new WorkflowDraftProperties());
    }

    @Autowired
    public CursorWorkflowDraftGenerator(WorkflowDraftProperties properties) {
        this.properties = properties == null ? new WorkflowDraftProperties() : properties;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean supports(WorkflowDraftGenerationRequest request) {
        return properties.isCursorEnabled() && request != null && StringUtils.hasText(request.getRequirement());
    }

    public String unavailableReason() {
        return properties.getCursorUnavailableReason();
    }

    @Override
    public WorkflowDraftGenerationResult generate(WorkflowDraftGenerationRequest request) {
        if (!supports(request)) {
            throw new IllegalArgumentException("requirement is required");
        }
        if (requiresIntentRouting(request.getRequirement())) {
            return intentRoutingDraft(request);
        }
        WorkflowDraftResource matched = bestResource(request);
        List<String> warnings = new ArrayList<>();
        List<WorkflowDraftPlaceholder> placeholders = new ArrayList<>();

        AgentGraphSpec.Node actionNode;
        if (matched == null) {
            String reason = "没有匹配到已注册 Tool/Capability，请在发布前补全该节点";
            warnings.add(reason);
            placeholders.add(new WorkflowDraftPlaceholder("placeholder_tool", "TOOL", "待配置业务能力", reason));
            actionNode = AgentGraphSpec.Node.builder()
                    .id("placeholder_tool")
                    .type("TOOL")
                    .name("待配置业务能力")
                    .description("AI 根据需求生成的占位业务节点")
                    .layout(AgentGraphSpec.Layout.NodeLayout.builder().x(520D).y(220D).build())
                    .config(Map.of(
                            "configVersion", 2,
                            "needsConfiguration", true,
                            "placeholderReason", reason,
                            "requestedRequirement", request.getRequirement(),
                            "source", "AI_DRAFT",
                            "inputMapping", Map.of("input", "$lastOutput"),
                            "outputAlias", "business_result"))
                    .build();
        } else if ("KNOWLEDGE".equalsIgnoreCase(matched.getKind())) {
            actionNode = AgentGraphSpec.Node.builder()
                    .id("knowledge_retrieval")
                    .type("KNOWLEDGE_RETRIEVAL")
                    .name(firstText(matched.getDescription(), matched.getName(), "知识检索"))
                    .description(matched.getDescription())
                    .layout(AgentGraphSpec.Layout.NodeLayout.builder().x(520D).y(220D).build())
                    .config(Map.of(
                            "configVersion", 2,
                            "source", "AI_DRAFT",
                            "knowledgeBaseCodes", List.of(matched.getName()),
                            "query", "input",
                            "topK", 5,
                            "searchMode", "hybrid",
                            "rerankEnabled", true,
                            "outputAlias", "knowledge_context"))
                    .build();
        } else {
            String type = "SKILL".equalsIgnoreCase(matched.getKind()) || "CAPABILITY".equalsIgnoreCase(matched.getKind())
                    ? "CAPABILITY" : "TOOL";
            actionNode = AgentGraphSpec.Node.builder()
                    .id("business_action")
                    .type(type)
                    .name(firstText(matched.getDescription(), matched.getName(), "业务能力"))
                    .description(matched.getDescription())
                    .ref(AgentGraphSpec.CapabilityRef.builder()
                            .kind(type)
                            .name(matched.getName())
                            .qualifiedName(matched.getQualifiedName())
                            .definitionId(matched.getDefinitionId())
                            .projectCode(matched.getProjectCode())
                            .build())
                    .layout(AgentGraphSpec.Layout.NodeLayout.builder().x(520D).y(220D).build())
                    .config(Map.of(
                            "configVersion", 2,
                            "source", "AI_DRAFT",
                            "inputMapping", Map.of("input", "$lastOutput"),
                            "outputAlias", "business_result"))
                    .build();
        }

        AgentGraphSpec graphSpec = AgentGraphSpec.builder()
                .code(slug(firstText(request.getAgentId(), request.getAgentName(), "ai_generated_graph")))
                .name(firstText(request.getAgentName(), "AI 生成流程草稿"))
                .mode("WORKFLOW")
                .runtimeHint("LANGGRAPH4J")
                .layout(AgentGraphSpec.Layout.builder().engine("vue-flow").direction("LR").build())
                .entry("user_input")
                .finishNode("reply")
                .node(userInputNode())
                .node(llmNode(request))
                .node(actionNode)
                .node(replyNode())
                .edge(edge("e-start-input", "START", "user_input", "always"))
                .edge(edge("e-input-llm", "user_input", "llm_plan", "always"))
                .edge(edge("e-llm-action", "llm_plan", actionNode.getId(), "always"))
                .edge(edge("e-action-reply", actionNode.getId(), "reply", "success"))
                .edge(edge("e-reply-end", "reply", "END", "always"))
                .build();

        return new WorkflowDraftGenerationResult(
                PROVIDER,
                canvasSnapshot(graphSpec, actionNode, matched, !placeholders.isEmpty()),
                graphSpec,
                warnings,
                placeholders);
    }

    private WorkflowDraftGenerationResult intentRoutingDraft(WorkflowDraftGenerationRequest request) {
        String primaryRoute = primaryIntentRoute(request.getRequirement());
        String primaryLabel = primaryIntentLabel(primaryRoute);
        List<Map<String, Object>> classes = List.of(Map.of(
                "id", primaryRoute,
                "label", primaryLabel,
                "description", primaryLabel + "时进入大模型回答分支",
                "keywords", primaryIntentKeywords(primaryRoute)));
        AgentGraphSpec.Node classifier = AgentGraphSpec.Node.builder()
                .id("intent_classifier")
                .type("INTENT_CLASSIFIER")
                .name("意图识别")
                .description("识别用户输入意图，并按分类路由到后续分支")
                .layout(AgentGraphSpec.Layout.NodeLayout.builder().x(280D).y(220D).build())
                .config(Map.of(
                        "configVersion", 2,
                        "source", "AI_DRAFT",
                        "inputMapping", Map.of("input", "params.question"),
                        "inputExpression", "params.question",
                        "classes", classes,
                        "defaultRoute", "irrelevant",
                        "outputAlias", "intent_result"))
                .input(AgentGraphSpec.Port.builder().id("input").name("input").type("string").required(true).source("params.question").build())
                .output(AgentGraphSpec.Port.builder().id("intent_result").name("intent_result").type("object").build())
                .output(AgentGraphSpec.Port.builder().id("intent_result.route").name("intent_result.route").type("string").build())
                .build();
        AgentGraphSpec.Node answerWithModel = AgentGraphSpec.Node.builder()
                .id("metro_answer")
                .type("LLM")
                .name("地铁问题回答")
                .description("对命中的地铁类问题调用大模型回答")
                .layout(AgentGraphSpec.Layout.NodeLayout.builder().x(560D).y(140D).build())
                .config(Map.of(
                        "configVersion", 2,
                        "modelInstanceId", firstText(request.getModelInstanceId(), ""),
                        "systemPrompt", "你是地铁问答助手，只回答与地铁、线路、站点、票价、换乘相关的问题。",
                        "userPrompt", "{{ params.question }}",
                        "outputFormat", "text",
                        "inputMapping", Map.of("question", "params.question", "intent", "intent_result.route"),
                        "outputAlias", "metro_answer",
                        "source", "AI_DRAFT"))
                .input(AgentGraphSpec.Port.builder().id("question").name("question").type("string").required(true).source("params.question").build())
                .input(AgentGraphSpec.Port.builder().id("intent").name("intent").type("string").required(false).source("intent_result.route").build())
                .output(AgentGraphSpec.Port.builder().id("metro_answer").name("metro_answer").type("string").build())
                .build();
        AgentGraphSpec.Node unknownReply = AgentGraphSpec.Node.builder()
                .id("unknown_reply")
                .type("ANSWER")
                .name("回复不知道")
                .description("对不相关问题直接回复兜底话术")
                .layout(AgentGraphSpec.Layout.NodeLayout.builder().x(560D).y(320D).build())
                .config(Map.of(
                        "configVersion", 2,
                        "template", "我不知道",
                        "writeToAnswer", true,
                        "inputMapping", Map.of("intent", "intent_result.route"),
                        "outputAlias", "unknown_answer",
                        "source", "AI_DRAFT"))
                .input(AgentGraphSpec.Port.builder().id("intent").name("intent").type("string").required(false).source("intent_result.route").build())
                .output(AgentGraphSpec.Port.builder().id("unknown_answer").name("unknown_answer").type("string").build())
                .build();

        AgentGraphSpec graphSpec = AgentGraphSpec.builder()
                .code(slug(firstText(request.getAgentId(), request.getAgentName(), "ai_generated_graph")))
                .name(firstText(request.getAgentName(), "AI 生成流程草稿"))
                .mode("WORKFLOW")
                .runtimeHint("LANGGRAPH4J")
                .layout(AgentGraphSpec.Layout.builder().engine("vue-flow").direction("LR").build())
                .entry("user_input")
                .finishNode("metro_answer")
                .finishNode("unknown_reply")
                .node(userInputNode())
                .node(classifier)
                .node(answerWithModel)
                .node(unknownReply)
                .edge(edge("e-start-input", "START", "user_input", "always"))
                .edge(edge("e-input-classifier", "user_input", "intent_classifier", "always"))
                .edge(routeEdge("e-classifier-metro", "intent_classifier", "metro_answer", primaryRoute))
                .edge(routeEdge("e-classifier-unknown", "intent_classifier", "unknown_reply", "irrelevant"))
                .edge(edge("e-metro-end", "metro_answer", "END", "always"))
                .edge(edge("e-unknown-end", "unknown_reply", "END", "always"))
                .build();

        return new WorkflowDraftGenerationResult(
                PROVIDER,
                intentRoutingCanvasSnapshot(graphSpec, request, classes, primaryRoute),
                graphSpec,
                List.of(),
                List.of());
    }

    private boolean requiresIntentRouting(String requirement) {
        String value = firstText(requirement);
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = normalize(value);
        boolean asksIntent = containsAny(normalized, "意图", "用户意图", "识别意图", "intent");
        boolean asksClassifier = containsAny(normalized,
                "意图分类", "意图识别", "识别用户意图", "分类",
                "classifier", "classify", "intent classification");
        boolean asksBranch = containsAny(normalized,
                "分支", "不同的分支", "走不同", "路由", "如果", "否则", "不相关", "相关",
                "branch", "route", "routing");
        return (asksIntent || asksClassifier) && asksBranch;
    }

    private String primaryIntentRoute(String requirement) {
        String value = normalize(firstText(requirement));
        if (containsAny(value, "地铁", "线路", "站点", "换乘", "票价", "metro", "subway")) {
            return "metro_question";
        }
        if (containsAny(value, "订单", "物流", "售后", "支付", "状态", "order")) {
            return "order_question";
        }
        return "matched_intent";
    }

    private String primaryIntentLabel(String route) {
        return switch (route) {
            case "metro_question" -> "地铁相关问题";
            case "order_question" -> "订单相关问题";
            default -> "匹配的业务意图";
        };
    }

    private List<String> primaryIntentKeywords(String route) {
        return switch (route) {
            case "metro_question" -> List.of("地铁", "线路", "站点", "换乘", "票价");
            case "order_question" -> List.of("订单", "物流", "售后", "支付", "状态");
            default -> List.of("业务", "问题", "咨询");
        };
    }

    private AgentGraphSpec.Node llmNode(WorkflowDraftGenerationRequest request) {
        return AgentGraphSpec.Node.builder()
                .id("llm_plan")
                .type("LLM")
                .name("需求理解")
                .description("理解用户输入并整理业务调用参数")
                .layout(AgentGraphSpec.Layout.NodeLayout.builder().x(280D).y(220D).build())
                .config(Map.of(
                        "configVersion", 2,
                        "modelInstanceId", firstText(request.getModelInstanceId(), ""),
                        "systemPrompt", "你是企业业务流程编排助手，负责把用户需求整理为后续节点可调用的结构化上下文。",
                        "userPrompt", "{{ params.question }}",
                        "outputFormat", "text",
                        "source", "AI_DRAFT"))
                .build();
    }

    private AgentGraphSpec.Node userInputNode() {
        return AgentGraphSpec.Node.builder()
                .id("user_input")
                .type("USER_INPUT")
                .name("用户输入")
                .description("定义工作流入口字段，并把运行时参数写入 params")
                .layout(AgentGraphSpec.Layout.NodeLayout.builder().x(280D).y(220D).build())
                .config(Map.of(
                        "configVersion", 2,
                        "source", "AI_DRAFT",
                        "fields", List.of(Map.of(
                                "name", "question",
                                "type", "string",
                                "required", true,
                                "description", "用户问题",
                                "source", "input.message")),
                        "outputAlias", "params"))
                .build();
    }

    private AgentGraphSpec.Node replyNode() {
        return AgentGraphSpec.Node.builder()
                .id("reply")
                .type("ANSWER")
                .name("回复用户")
                .description("组装最终回复")
                .layout(AgentGraphSpec.Layout.NodeLayout.builder().x(760D).y(220D).build())
                .config(Map.of(
                        "configVersion", 2,
                        "template", "{{ lastOutput }}",
                        "writeToAnswer", true,
                        "source", "AI_DRAFT"))
                .build();
    }

    private AgentGraphSpec.Edge edge(String id, String from, String to, String condition) {
        return AgentGraphSpec.Edge.builder()
                .id(id)
                .from(from)
                .to(to)
                .condition(condition)
                .layout(AgentGraphSpec.Layout.EdgeLayout.builder().label(condition).style("smoothstep").build())
                .build();
    }

    private AgentGraphSpec.Edge routeEdge(String id, String from, String to, String route) {
        return AgentGraphSpec.Edge.builder()
                .id(id)
                .from(from)
                .to(to)
                .condition("route:" + route)
                .sourceHandle(route)
                .layout(AgentGraphSpec.Layout.EdgeLayout.builder().label("route:" + route).style("smoothstep").build())
                .build();
    }

    private WorkflowDraftResource bestResource(WorkflowDraftGenerationRequest request) {
        List<WorkflowDraftResource> resources = new ArrayList<>();
        if (request.getTools() != null) {
            resources.addAll(request.getTools());
        }
        if (request.getCapabilities() != null) {
            resources.addAll(request.getCapabilities());
        }
        if (request.getKnowledgeBases() != null) {
            resources.addAll(request.getKnowledgeBases());
        }
        String requirement = normalize(request.getRequirement());
        return resources.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(resource -> score(requirement, resource)))
                .filter(resource -> score(requirement, resource) > 0)
                .orElse(null);
    }

    private int score(String requirement, WorkflowDraftResource resource) {
        String haystack = normalize(String.join(" ",
                firstText(resource.getName(), ""),
                firstText(resource.getQualifiedName(), ""),
                firstText(resource.getDescription(), "")));
        int score = 0;
        for (String token : haystack.split("[^\\p{IsAlphabetic}\\p{IsDigit}]+")) {
            if (token.length() >= 2 && requirement.contains(token)) {
                score += token.length();
            }
        }
        if (sharesCjkPair(requirement, haystack)) {
            score += 6;
        }
        return score;
    }

    private boolean sharesCjkPair(String left, String right) {
        for (int i = 0; i < left.length() - 1; i++) {
            String pair = left.substring(i, i + 2);
            if (pair.codePoints().allMatch(code -> Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN)
                    && right.contains(pair)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> canvasSnapshot(AgentGraphSpec graph,
                                               AgentGraphSpec.Node actionNode,
                                               WorkflowDraftResource matched,
                                               boolean placeholder) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(canvasNode("start", "start", 60, 220, data("开始", "start", Map.of())));
        nodes.add(canvasNode("llm_plan", "llm", 280, 220, data("需求理解", "llm", Map.of(
                "description", "理解用户输入并整理业务调用参数",
                "llmConfig", Map.of("userPrompt", "{{ params.question }}", "outputFormat", "text"),
                "source", "CANVAS"))));
        nodes.add(userInputCanvasNode(280, 120));
        Map<String, Object> actionExtra = new LinkedHashMap<>();
        actionExtra.put("description", actionNode.getDescription());
        actionExtra.put("source", "CANVAS");
        actionExtra.put("outputAlias", firstText(text(actionNode.getConfig().get("outputAlias")), "business_result"));
        if ("KNOWLEDGE_RETRIEVAL".equalsIgnoreCase(actionNode.getType())) {
            actionExtra.put("knowledgeConfig", Map.of(
                    "knowledgeBaseCodes", List.of(matched == null ? "" : firstText(matched.getName(), "")),
                    "query", "input",
                    "topK", 5,
                    "similarityThreshold", 0.5,
                    "searchMode", "hybrid",
                    "rerankEnabled", true,
                    "directReturnEnabled", false,
                    "directReturnThreshold", 0.85));
        } else {
            actionExtra.put("toolConfig", Map.of(
                    "ref", matched == null ? "" : firstText(matched.getName(), ""),
                    "qualifiedName", matched == null ? "" : firstText(matched.getQualifiedName(), ""),
                    "projectCode", matched == null ? "" : firstText(matched.getProjectCode(), ""),
                    "inputMapping", Map.of("input", "$lastOutput")));
        }
        if (placeholder) {
            actionExtra.put("needsConfiguration", true);
            actionExtra.put("placeholderReason", firstText(text(actionNode.getConfig().get("placeholderReason")), "请补全业务能力"));
        }
        nodes.add(canvasNode(actionNode.getId(), canvasKind(actionNode), 520, 220,
                data(actionNode.getName(), canvasKind(actionNode), actionExtra)));
        nodes.add(canvasNode("reply", "answer", 760, 220, data("回复用户", "answer", Map.of(
                "description", "组装最终回复",
                "answerConfig", Map.of("template", "{{ lastOutput }}"),
                "source", "CANVAS"))));
        nodes.add(canvasNode("end", "end", 1000, 220, data("结束", "end", Map.of())));

        List<Map<String, Object>> edges = List.of(
                canvasEdge("e-start-input", "start", "user_input", "always"),
                canvasEdge("e-input-llm", "user_input", "llm_plan", "always"),
                canvasEdge("e-llm-action", "llm_plan", actionNode.getId(), "always"),
                canvasEdge("e-action-reply", actionNode.getId(), "reply", "success"),
                canvasEdge("e-reply-end", "reply", "end", "always")
        );
        return Map.of("version", 2, "nodes", nodes, "edges", edges, "graphCode", graph.getCode());
    }

    private Map<String, Object> intentRoutingCanvasSnapshot(AgentGraphSpec graph,
                                                            WorkflowDraftGenerationRequest request,
                                                            List<Map<String, Object>> classes,
                                                            String primaryRoute) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(userInputCanvasNode(280, 120));
        nodes.add(canvasNode("start", "start", 60, 220, data("开始", "start", Map.of())));
        nodes.add(canvasNode("intent_classifier", "classifier", 280, 220, data("意图识别", "classifier", Map.of(
                "description", "识别用户输入意图，并按分类路由到后续分支",
                "classifierConfig", Map.of(
                        "inputExpression", "params.question",
                        "classes", classes,
                        "defaultRoute", "irrelevant"),
                "inputMapping", Map.of("input", "params.question"),
                "outputAlias", "intent_result",
                "inputs", List.of(Map.of("id", "input", "name", "input", "type", "string", "required", true, "source", "params.question")),
                "outputs", List.of(
                        Map.of("id", "intent_result", "name", "intent_result", "type", "object", "source", "classifier"),
                        Map.of("id", "intent_result.route", "name", "intent_result.route", "type", "string", "source", "classifier"),
                        Map.of("id", primaryRoute, "name", primaryIntentLabel(primaryRoute), "type", "any", "source", "classifier"),
                        Map.of("id", "irrelevant", "name", "不相关", "type", "any", "source", "classifier")),
                "source", "CANVAS"))));
        nodes.add(canvasNode("metro_answer", "llm", 560, 140, data("地铁问题回答", "llm", Map.of(
                "description", "对命中的地铁类问题调用大模型回答",
                "llmConfig", Map.of(
                        "modelInstanceId", firstText(request.getModelInstanceId(), ""),
                        "systemPrompt", "你是地铁问答助手，只回答与地铁、线路、站点、票价、换乘相关的问题。",
                        "userPrompt", "{{ params.question }}",
                        "outputFormat", "text"),
                "inputMapping", Map.of("question", "params.question", "intent", "intent_result.route"),
                "outputAlias", "metro_answer",
                "inputs", List.of(
                        Map.of("id", "question", "name", "question", "type", "string", "required", true, "source", "params.question"),
                        Map.of("id", "intent", "name", "intent", "type", "string", "required", false, "source", "intent_result.route")),
                "outputs", List.of(Map.of("id", "metro_answer", "name", "metro_answer", "type", "string")),
                "source", "CANVAS"))));
        nodes.add(canvasNode("unknown_reply", "answer", 560, 320, data("回复不知道", "answer", Map.of(
                "description", "对不相关问题直接回复兜底话术",
                "answerConfig", Map.of("template", "我不知道"),
                "inputMapping", Map.of("intent", "intent_result.route"),
                "outputAlias", "unknown_answer",
                "inputs", List.of(Map.of("id", "intent", "name", "intent", "type", "string", "required", false, "source", "intent_result.route")),
                "outputs", List.of(Map.of("id", "unknown_answer", "name", "unknown_answer", "type", "string")),
                "source", "CANVAS"))));
        nodes.add(canvasNode("end", "end", 820, 220, data("结束", "end", Map.of())));

        List<Map<String, Object>> edges = List.of(
                canvasEdge("e-start-input", "start", "user_input", "always"),
                canvasEdge("e-input-classifier", "user_input", "intent_classifier", "always"),
                canvasEdge("e-classifier-metro", "intent_classifier", "metro_answer", "route:" + primaryRoute, primaryRoute, null),
                canvasEdge("e-classifier-unknown", "intent_classifier", "unknown_reply", "route:irrelevant", "irrelevant", null),
                canvasEdge("e-metro-end", "metro_answer", "end", "always"),
                canvasEdge("e-unknown-end", "unknown_reply", "end", "always")
        );
        return Map.of("version", 2, "nodes", nodes, "edges", edges, "graphCode", graph.getCode());
    }

    private Map<String, Object> canvasNode(String id, String type, int x, int y, Map<String, Object> data) {
        return Map.of("id", id, "type", type, "position", Map.of("x", x, "y", y), "data", data);
    }

    private Map<String, Object> userInputCanvasNode(int x, int y) {
        return canvasNode("user_input", "userInput", x, y, data("用户输入", "userInput", Map.of(
                "description", "定义工作流入口字段，并把运行时参数写入 params",
                "outputAlias", "params",
                "userInputConfig", Map.of(
                        "outputAlias", "params",
                        "fields", List.of(Map.of(
                                "name", "question",
                                "type", "string",
                                "required", true,
                                "description", "用户问题",
                                "source", "input.message"))),
                "outputs", List.of(
                        Map.of("id", "params", "name", "params", "type", "object"),
                        Map.of("id", "params.question", "name", "params.question", "type", "string", "required", true)),
                "source", "CANVAS")));
    }

    private Map<String, Object> data(String label, String kind, Map<String, Object> extra) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("label", label);
        data.put("kind", kind);
        data.put("configVersion", 2);
        data.putAll(extra);
        return data;
    }

    private Map<String, Object> canvasEdge(String id, String source, String target, String condition) {
        return canvasEdge(id, source, target, condition, null, null);
    }

    private Map<String, Object> canvasEdge(String id,
                                           String source,
                                           String target,
                                           String condition,
                                           String sourceHandle,
                                           String targetHandle) {
        Map<String, Object> edge = new LinkedHashMap<>();
        edge.put("id", id);
        edge.put("source", source);
        edge.put("target", target);
        edge.put("condition", condition);
        edge.put("label", condition);
        edge.put("type", "smoothstep");
        edge.put("markerEnd", "arrowclosed");
        edge.put("interactionWidth", 18);
        if (StringUtils.hasText(sourceHandle)) {
            edge.put("sourceHandle", sourceHandle);
        }
        if (StringUtils.hasText(targetHandle)) {
            edge.put("targetHandle", targetHandle);
        }
        return edge;
    }

    private String canvasKind(AgentGraphSpec.Node node) {
        if ("KNOWLEDGE_RETRIEVAL".equalsIgnoreCase(node.getType())) {
            return "knowledge";
        }
        return "CAPABILITY".equalsIgnoreCase(node.getType()) ? "skill" : "tool";
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalize(String value) {
        return firstText(value).toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... tokens) {
        String normalized = normalize(value);
        for (String token : tokens) {
            if (StringUtils.hasText(token) && normalized.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String slug(String value) {
        String slug = normalize(value).replaceAll("[^a-z0-9_\\-]+", "_").replaceAll("_+", "_");
        return StringUtils.hasText(slug) ? slug : "ai_generated_graph";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
