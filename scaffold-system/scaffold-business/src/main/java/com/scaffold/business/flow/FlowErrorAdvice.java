package com.scaffold.business.flow;

import com.scaffold.common.domain.R;
import com.scaffold.flow.runtime.FlowRuntimeException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.scaffold.business")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FlowErrorAdvice {

    @ExceptionHandler(FlowRuntimeException.class)
    public ResponseEntity<R<Map<String, Object>>> handleFlow(FlowRuntimeException ex) {
        HttpStatus status = switch (ex.code()) {
            case "FLOW_DEADLINE_EXCEEDED" -> HttpStatus.REQUEST_TIMEOUT;
            case "FLOW_CANCELLED" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("errorCode", ex.code());
        data.put("status", "FLOW_CANCELLED".equals(ex.code()) ? "cancelled" : "failed");
        String message = ex.business() ? ex.getMessage() : safeMessage(ex.code());
        return ResponseEntity.status(status)
                .body(new R<>(status.value(), message, data, MDC.get("traceId")));
    }

    private String safeMessage(String code) {
        return switch (code) {
            case "FLOW_DEADLINE_EXCEEDED" -> "Flow 执行超时";
            case "FLOW_CANCELLED" -> "Flow 已取消";
            case "FLOW_BUDGET_EXCEEDED" -> "Flow 执行预算已耗尽";
            case "FLOW_RECURSION_LIMIT" -> "Flow 调用层级超过上限";
            case "FLOW_NODE_UNSUPPORTED" -> "Flow 节点暂不支持执行";
            case "FLOW_OUTPUT_INVALID" -> "Flow 输出格式无效";
            case "FLOW_NODE_FAILED" -> "Flow 节点执行失败";
            default -> "Flow 执行失败";
        };
    }
}
