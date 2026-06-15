package com.enterprise.ai.agent.assist;

import com.enterprise.ai.agent.controller.AiAssistController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AiAssistController.class)
public class PageAssistantRegisterRequestAdvice {

    private static final String REGISTER_EXAMPLE = """
            {
              "sessionId": "rai_page_123",
              "toolName": "Cursor",
              "pageKey": "teamArchive.list",
              "pageName": "班组档案",
              "routePattern": "/teams/archive",
              "framework": "angular",
              "bridgeGlobal": "__REACHAI_PAGE_BRIDGE__",
              "replaceActions": true,
              "files": [
                "src/app/list.component.ts",
                { "path": "src/app/shared/reachai/reachai-page-action.service.ts", "role": "bridge-service" }
              ],
              "actions": [
                {
                  "actionKey": "getPageState",
                  "title": "读取页面状态",
                  "description": "读取筛选、分页和表格",
                  "confirmRequired": false
                }
              ],
              "verification": {
                "browserStatic": { "status": "PASS", "message": "static only" },
                "browserRuntime": { "status": "SKIPPED", "message": "no authenticated browser session" }
              },
              "handoffSummary": "Page assistant MVP ready"
            }""";

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AiAssistController.ApiErrorResponse> handleUnreadableRegisterBody(
            HttpMessageNotReadableException ex) {
        String message = ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("PageAssistantFileEvidence")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AiAssistController.ApiErrorResponse(
                    "Invalid page assistant register JSON. files accepts string shorthand "
                            + "[\"src/app/list.component.ts\"] or object array "
                            + "[{\"path\":\"src/app/list.component.ts\",\"role\":\"page-component\"}]. "
                            + "Example body:\n" + REGISTER_EXAMPLE));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AiAssistController.ApiErrorResponse(
                "Invalid page assistant register JSON. Example body:\n" + REGISTER_EXAMPLE));
    }
}
