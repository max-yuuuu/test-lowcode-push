package com.scaffold.business.demo.controller;

import com.scaffold.common.domain.R;
import com.scaffold.flow.runtime.FlowRuntimeException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** Flow 执行错误只返回稳定错误码与节点路径，不向调用方暴露内部异常。 */
@RestControllerAdvice(basePackages = "com.scaffold.business.demo")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FlowErrorAdvice {

    @ExceptionHandler(FlowRuntimeException.class)
    public ResponseEntity<R<Map<String, Object>>> handleFlowFailure(FlowRuntimeException ex) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("errorCode", ex.code());
        data.put("nodePath", ex.nodePath());
        int status = switch (ex.code()) {
            case "FLOW_OPERATION_FORBIDDEN" -> 403;
            case "FLOW_CANCELLED" -> 409;
            case "FLOW_DEADLINE_EXCEEDED" -> 408;
            case "FLOW_INPUT_BINDING_INVALID", "FLOW_LIMIT_INVALID" -> 400;
            default -> ex.business() ? 400 : 500;
        };
        String message = switch (ex.code()) {
            case "FLOW_OPERATION_FORBIDDEN" -> "无权调用该接口";
            case "FLOW_CANCELLED" -> "Flow 执行已取消";
            case "FLOW_DEADLINE_EXCEEDED" -> "Flow 执行超时";
            case "FLOW_INPUT_BINDING_INVALID", "FLOW_LIMIT_INVALID" -> "Flow 请求参数无效";
            default -> ex.business() ? ex.getMessage() : "Flow 执行失败";
        };
        return ResponseEntity.status(status).body(new R<>(status, message, data, MDC.get("traceId")));
    }
}
