package com.scaffold.business.sales_order.controller;

import com.scaffold.business.sales_order.domain.SalesOrder;
import com.scaffold.business.sales_order.domain.SalesOrderRow;
import com.scaffold.business.sales_order.domain.SalesOrderWriteRequest;
import com.scaffold.business.sales_order.service.SalesOrderService;
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

import java.math.BigDecimal;

/**
 * 销售订单主表（表 {@code sales_order}）的接口。
 *
 * <p>本类由元搭低代码平台按模型定义生成，属 B 档：每次发布整文件覆盖，要加接口请回到设计器，
 * 或在同包的 {@code SalesOrderExtension}（C 档）里做，别在这里塞业务分支。
 *
 * <p>五个入口固定成一套（列表 / 导出 / 新增 / 修改 / 删除），路径与权限点由模型编码派生；
 * 返回结构统一走 {@code R}，分页统一走 {@code PageResult}，与底座 {@code /api/system} 同一形状。
 * 自定义动作在设计器里选 {@code custom:{name}}，平台只出一个带 TODO 的桩，实现由人填。
 */
@RestController
@RequestMapping("/api/sales_order/sales_order")
public class SalesOrderController {

    private final SalesOrderService service;

    public SalesOrderController(SalesOrderService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("sales_order:sales_order:list")
    public R<PageResult<SalesOrderRow>> list(
            PageQuery query,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) BigDecimal amountStart,
            @RequestParam(required = false) BigDecimal amountEnd,
            @RequestParam(required = false) Integer status
    ) {
        return R.success(service.page(query, orderNo, amountStart, amountEnd, status));
    }

    /** 单行详情：前端编辑前取整行，避免用列表行填表时把没显示的字段清空 */
    @GetMapping("/{id}")
    @RequiresPermission("sales_order:sales_order:list")
    public R<SalesOrder> detail(@PathVariable Long id) {
        return R.success(service.detail(id));
    }

    /** 导出当前筛选条件下的 CSV（上限由 Service 把住），文件名用模型中文名 */
    @GetMapping("/export")
    @RequiresPermission("sales_order:sales_order:list")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) BigDecimal amountStart,
            @RequestParam(required = false) BigDecimal amountEnd,
            @RequestParam(required = false) Integer status
    ) {
        return CsvResponse.attachment("salesOrder.csv", service.export(orderNo, amountStart, amountEnd, status));
    }

    @PostMapping
    @RequiresPermission("sales_order:sales_order:add")
    @OperLog(module = "销售订单主表", action = "新增")
    public R<Long> create(@RequestBody SalesOrderWriteRequest request) {
        return R.success(service.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("sales_order:sales_order:edit")
    @OperLog(module = "销售订单主表", action = "修改")
    public R<Void> update(@PathVariable Long id, @RequestBody SalesOrderWriteRequest request) {
        service.update(id, request);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sales_order:sales_order:remove")
    @OperLog(module = "销售订单主表", action = "删除")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.success(null);
    }
}
