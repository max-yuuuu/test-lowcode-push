package com.scaffold.business.t11_verify_20261118.controller;

import com.scaffold.business.t11_verify_20261118.domain.OperationNotImplementedException;
import com.scaffold.common.domain.R;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自定义接口错误的翻译层（本模块专用）。
 *
 * <p>本类由低代码设计器生成，属 B 档：每次发布整文件覆盖，同模块多个模型共用这一份。
 * 只接 {@code OperationNotImplementedException}，其余异常仍走底座统一异常处理 ——
 * 优先级压到最高是为了抢在底座兜底之前把 {@code errorCode}（{@code OPERATION_NOT_IMPLEMENTED}）
 * 放进响应体，前端与调用方靠它区分「未实现」与普通业务失败。
 */
@RestControllerAdvice(basePackages = "com.scaffold.business.t11_verify_20261118")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OperationErrorAdvice {

    @ExceptionHandler(OperationNotImplementedException.class)
    public ResponseEntity<R<Map<String, Object>>> handleNotImplemented(OperationNotImplementedException ex) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("errorCode", "OPERATION_NOT_IMPLEMENTED");
        data.put("operationCode", ex.operationCode());
        data.put("operationVersion", ex.operationVersion());
        return ResponseEntity.status(ex.code()).body(new R<>(ex.code(), ex.getMessage(), data, MDC.get("traceId")));
    }
}