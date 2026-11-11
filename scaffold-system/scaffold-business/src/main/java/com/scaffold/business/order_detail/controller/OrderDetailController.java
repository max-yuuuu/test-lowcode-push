package com.scaffold.business.order_detail.controller;

import com.scaffold.business.order_detail.domain.OrderDetail;
import com.scaffold.business.order_detail.domain.OrderDetailRow;
import com.scaffold.business.order_detail.domain.OrderDetailWriteRequest;
import com.scaffold.business.order_detail.service.OrderDetailService;
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
 * 订单明细（表 {@code order_detail}）的接口。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖，要加接口请回到设计器，
 * 或在同包的 {@code OrderDetailExtension}（C 档）里做，别在这里塞业务分支。
 *
 * <p>五个入口固定成一套（列表 / 导出 / 新增 / 修改 / 删除），路径与权限点由模型编码派生；
 * 返回结构统一走 {@code R}，分页统一走 {@code PageResult}，与底座 {@code /api/system} 同一形状。
 * 自定义动作在设计器里选 {@code custom:{name}}，平台只出一个带 TODO 的桩，实现由人填。
 */
@RestController
@RequestMapping("/api/order_detail/order_detail")
public class OrderDetailController {

    private final OrderDetailService service;

    public OrderDetailController(OrderDetailService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("order_detail:order_detail:list")
    public R<PageResult<OrderDetailRow>> list(
            PageQuery query
    ) {
        return R.success(service.page(query));
    }

    /** 单行详情：前端编辑前取整行，避免用列表行填表时把没显示的字段清空 */
    @GetMapping("/{id}")
    @RequiresPermission("order_detail:order_detail:list")
    public R<OrderDetail> detail(@PathVariable Long id) {
        return R.success(service.detail(id));
    }

    /** 导出当前筛选条件下的 CSV（上限由 Service 把住），文件名用模型中文名 */
    @GetMapping("/export")
    @RequiresPermission("order_detail:order_detail:list")
    public ResponseEntity<String> export() {
        return CsvResponse.attachment("orderDetail.csv", service.export());
    }

    @PostMapping
    @RequiresPermission("order_detail:order_detail:add")
    @OperLog(module = "订单明细", action = "新增")
    public R<Long> create(@RequestBody OrderDetailWriteRequest request) {
        return R.success(service.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("order_detail:order_detail:edit")
    @OperLog(module = "订单明细", action = "修改")
    public R<Void> update(@PathVariable Long id, @RequestBody OrderDetailWriteRequest request) {
        service.update(id, request);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("order_detail:order_detail:remove")
    @OperLog(module = "订单明细", action = "删除")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.success(null);
    }
}
