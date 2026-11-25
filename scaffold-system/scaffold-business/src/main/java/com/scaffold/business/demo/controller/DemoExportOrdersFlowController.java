package com.scaffold.business.demo.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.business.flow.FlowSnapshotExecutor;
import com.scaffold.flow.runtime.FlowRuntimeException;
import com.scaffold.business.demo.domain.DemoExportOrdersRequest;
import com.scaffold.business.demo.domain.DemoExportOrdersResponse;
import com.scaffold.common.domain.R;
import com.scaffold.framework.audit.OperLog;
import com.scaffold.framework.security.RequiresPermission;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 导出订单（Flow 接口 {@code export_orders}，发布版本 v3）。 */
@RestController
@RequestMapping("/api/demo/demo")
public class DemoExportOrdersFlowController {

    private final FlowSnapshotExecutor flowExecutor;
    private final ObjectMapper objectMapper;

    public DemoExportOrdersFlowController(FlowSnapshotExecutor flowExecutor, ObjectMapper objectMapper) {
        this.flowExecutor = flowExecutor;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/export_orders")
    @RequiresPermission("sales:sales_order:export")
    @OperLog(module = "测试模型", action = "导出订单")
    @Transactional
    public R<DemoExportOrdersResponse> exportOrders(@RequestBody(required = false) DemoExportOrdersRequest request) {
        Map<String, Object> input = request == null ? Map.of()
                : objectMapper.convertValue(request, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> output = flowExecutor.execute(4L, "export_orders", 3, input);
        try {
            return R.success(objectMapper.convertValue(output, DemoExportOrdersResponse.class));
        } catch (IllegalArgumentException conversionError) {
            throw new FlowRuntimeException("FLOW_EXECUTION_FAILED", "Flow 执行失败");
        }
    }
}
