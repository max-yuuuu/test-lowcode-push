package com.scaffold.business.demo.controller;

import com.scaffold.business.demo.domain.Demo;
import com.scaffold.business.demo.domain.DemoRow;
import com.scaffold.business.demo.domain.DemoWriteRequest;
import com.scaffold.business.demo.domain.DemoReconcile20261117Request;
import com.scaffold.business.demo.domain.DemoReconcile20261117Response;
import com.scaffold.business.demo.service.DemoService;
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
 * 测试模型（表 {@code demo}）的接口。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖，要加接口请回到设计器，
 * 或在同包的 {@code DemoExtension}（C 档）里做，别在这里塞业务分支。
 *
 * <p>五个入口固定成一套（列表 / 导出 / 新增 / 修改 / 删除），路径与权限点由模型编码派生；
 * 返回结构统一走 {@code R}，分页统一走 {@code PageResult}，与底座 {@code /api/system} 同一形状。
 * 自定义动作在设计器里选 {@code custom:{name}}，平台只出一个带 TODO 的桩，实现由人填。
 */
@RestController
@RequestMapping("/api/demo/demo")
public class DemoController {

    private final DemoService service;

    public DemoController(DemoService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("demo:demo:list")
    public R<PageResult<DemoRow>> list(
            PageQuery query
    ) {
        return R.success(service.page(query));
    }

    /** 单行详情：前端编辑前取整行，避免用列表行填表时把没显示的字段清空 */
    @GetMapping("/{id}")
    @RequiresPermission("demo:demo:list")
    public R<Demo> detail(@PathVariable Long id) {
        return R.success(service.detail(id));
    }

    /** 导出当前筛选条件下的 CSV（上限由 Service 把住），文件名用模型中文名 */
    @GetMapping("/export")
    @RequiresPermission("demo:demo:list")
    public ResponseEntity<String> export() {
        return CsvResponse.attachment("demo.csv", service.export());
    }

    @PostMapping
    @RequiresPermission("demo:demo:add")
    @OperLog(module = "测试模型", action = "新增")
    public R<Long> create(@RequestBody DemoWriteRequest request) {
        return R.success(service.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("demo:demo:edit")
    @OperLog(module = "测试模型", action = "修改")
    public R<Void> update(@PathVariable Long id, @RequestBody DemoWriteRequest request) {
        service.update(id, request);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("demo:demo:remove")
    @OperLog(module = "测试模型", action = "删除")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.success(null);
    }

    /**
     * 归档（自定义接口 {@code archive}，契约 v1）。
     * 实现住 {@code DemoArchiveExtension}（C 档）；未实现时返回 {@code OPERATION_NOT_IMPLEMENTED}。
     */
    @PostMapping("/archive")
    @OperLog(module = "测试模型", action = "归档")
    public R<Void> archive() {
        service.archive();
        return R.success(null);
    }

    /**
     * 对账验证（自定义接口 {@code reconcile_20261117}，契约 v1）。
     * 实现住 {@code DemoReconcile20261117Extension}（C 档）；未实现时返回 {@code OPERATION_NOT_IMPLEMENTED}。
     */
    @PostMapping("/reconcile_20261117")
    @OperLog(module = "测试模型", action = "对账验证")
    public R<DemoReconcile20261117Response> reconcile20261117(@RequestBody DemoReconcile20261117Request request) {
        return R.success(service.reconcile20261117(request));
    }
}
