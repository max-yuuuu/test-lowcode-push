package com.scaffold.business.e2e_order_trace.controller;

import com.scaffold.business.e2e_order_trace.domain.E2eOrderTrace;
import com.scaffold.business.e2e_order_trace.domain.E2eOrderTraceRow;
import com.scaffold.business.e2e_order_trace.domain.E2eOrderTraceWriteRequest;
import com.scaffold.business.e2e_order_trace.service.E2eOrderTraceService;
import com.scaffold.common.domain.PageQuery;
import com.scaffold.common.domain.PageResult;
import com.scaffold.common.domain.R;
import com.scaffold.framework.audit.OperLog;
import com.scaffold.framework.security.RequiresPermission;
import com.scaffold.framework.web.CsvResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * E2E 订单轨迹（表 {@code e2e_order_trace}）的接口。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖，要加接口请回到设计器，
 * 或在同包的 {@code E2eOrderTraceExtension}（C 档）里做，别在这里塞业务分支。
 *
 * <p>五个入口固定成一套（列表 / 导出 / 新增 / 修改 / 删除），路径与权限点由模型编码派生；
 * 返回结构统一走 {@code R}，分页统一走 {@code PageResult}，与底座 {@code /api/system} 同一形状。
 * 自定义动作在设计器里选 {@code custom:{name}}，平台只出一个带 TODO 的桩，实现由人填。
 */
@RestController
@RequestMapping("/api/e2e_order_trace/e2e_order_trace")
public class E2eOrderTraceController {

    private final E2eOrderTraceService service;

    public E2eOrderTraceController(E2eOrderTraceService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("e2e_order_trace:e2e_order_trace:list")
    public R<PageResult<E2eOrderTraceRow>> list(
            PageQuery query
    ) {
        return R.success(service.page(query));
    }

    /** 单行详情：前端编辑前取整行，避免用列表行填表时把没显示的字段清空 */
    @GetMapping("/{id}")
    @RequiresPermission("e2e_order_trace:e2e_order_trace:list")
    public R<E2eOrderTrace> detail(@PathVariable Long id) {
        return R.success(service.detail(id));
    }

    /** 导出当前筛选条件下的 CSV（上限由 Service 把住），文件名用模型中文名 */
    @GetMapping("/export")
    @RequiresPermission("e2e_order_trace:e2e_order_trace:list")
    public ResponseEntity<String> export() {
        return CsvResponse.attachment("e2eOrderTrace.csv", service.export());
    }

    @PostMapping
    @RequiresPermission("e2e_order_trace:e2e_order_trace:add")
    @OperLog(module = "E2E 订单轨迹", action = "新增")
    public R<Long> create(@RequestBody E2eOrderTraceWriteRequest request) {
        return R.success(service.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("e2e_order_trace:e2e_order_trace:edit")
    @OperLog(module = "E2E 订单轨迹", action = "修改")
    public R<Void> update(@PathVariable Long id, @RequestBody E2eOrderTraceWriteRequest request) {
        service.update(id, request);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("e2e_order_trace:e2e_order_trace:remove")
    @OperLog(module = "E2E 订单轨迹", action = "删除")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.success(null);
    }
}
