package com.scaffold.business.t11_demo.controller;

import com.scaffold.business.t11_demo.domain.T11Demo;
import com.scaffold.business.t11_demo.domain.T11DemoRow;
import com.scaffold.business.t11_demo.domain.T11DemoWriteRequest;
import com.scaffold.business.t11_demo.service.T11DemoService;
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
 * T11 演示（表 {@code t11_demo}）的接口。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖，要加接口请回到设计器，
 * 或在同包的 {@code T11DemoExtension}（C 档）里做，别在这里塞业务分支。
 *
 * <p>五个入口固定成一套（列表 / 导出 / 新增 / 修改 / 删除），路径与权限点由模型编码派生；
 * 返回结构统一走 {@code R}，分页统一走 {@code PageResult}，与底座 {@code /api/system} 同一形状。
 * 自定义动作在设计器里选 {@code custom:{name}}，平台只出一个带 TODO 的桩，实现由人填。
 */
@RestController
@RequestMapping("/api/t11_demo/t11_demo")
public class T11DemoController {

    private final T11DemoService service;

    public T11DemoController(T11DemoService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("t11_demo:t11_demo:list")
    public R<PageResult<T11DemoRow>> list(
            PageQuery query,
            @RequestParam(required = false) String name
    ) {
        return R.success(service.page(query, name));
    }

    /** 单行详情：前端编辑前取整行，避免用列表行填表时把没显示的字段清空 */
    @GetMapping("/{id}")
    @RequiresPermission("t11_demo:t11_demo:list")
    public R<T11Demo> detail(@PathVariable Long id) {
        return R.success(service.detail(id));
    }

    /** 导出当前筛选条件下的 CSV（上限由 Service 把住），文件名用模型中文名 */
    @GetMapping("/export")
    @RequiresPermission("t11_demo:t11_demo:list")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String name
    ) {
        return CsvResponse.attachment("t11Demo.csv", service.export(name));
    }

    @PostMapping
    @RequiresPermission("t11_demo:t11_demo:add")
    @OperLog(module = "T11 演示", action = "新增")
    public R<Long> create(@RequestBody T11DemoWriteRequest request) {
        return R.success(service.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("t11_demo:t11_demo:edit")
    @OperLog(module = "T11 演示", action = "修改")
    public R<Void> update(@PathVariable Long id, @RequestBody T11DemoWriteRequest request) {
        service.update(id, request);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("t11_demo:t11_demo:remove")
    @OperLog(module = "T11 演示", action = "删除")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.success(null);
    }
}
