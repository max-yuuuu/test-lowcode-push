package com.scaffold.business.demo.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.business.flow.FlowSnapshotExecutor;
import com.scaffold.flow.runtime.FlowRuntimeException;
import com.scaffold.business.demo.domain.DemoArchiveOrderRequest;
import com.scaffold.business.demo.domain.DemoArchiveOrderResponse;
import com.scaffold.common.domain.R;
import com.scaffold.framework.audit.OperLog;
import com.scaffold.framework.security.RequiresPermission;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 归档订单（Flow 接口 {@code archive_order}，发布版本 v2）。 */
@RestController
@RequestMapping("/api/demo/demo")
public class DemoArchiveOrderFlowController {

    private final FlowSnapshotExecutor flowExecutor;
    private final ObjectMapper objectMapper;

    public DemoArchiveOrderFlowController(FlowSnapshotExecutor flowExecutor, ObjectMapper objectMapper) {
        this.flowExecutor = flowExecutor;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/archive_order")
    @RequiresPermission("sales:sales_order:archive")
    @OperLog(module = "测试模型", action = "归档订单")
    @Transactional
    public R<DemoArchiveOrderResponse> archiveOrder(@RequestBody(required = false) DemoArchiveOrderRequest request) {
        Map<String, Object> input = request == null ? Map.of()
                : objectMapper.convertValue(request, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> output = flowExecutor.execute(4L, "archive_order", 2, input);
        try {
            return R.success(objectMapper.convertValue(output, DemoArchiveOrderResponse.class));
        } catch (IllegalArgumentException conversionError) {
            throw new FlowRuntimeException("FLOW_EXECUTION_FAILED", "Flow 执行失败");
        }
    }
}
