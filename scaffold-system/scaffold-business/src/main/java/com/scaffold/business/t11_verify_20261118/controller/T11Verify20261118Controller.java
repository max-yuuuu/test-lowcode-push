package com.scaffold.business.t11_verify_20261118.controller;

import com.scaffold.business.t11_verify_20261118.domain.T11Verify20261118;
import com.scaffold.business.t11_verify_20261118.domain.T11Verify20261118Row;
import com.scaffold.business.t11_verify_20261118.domain.T11Verify20261118WriteRequest;
import com.scaffold.business.t11_verify_20261118.domain.T11Verify20261118VerifyPublishRequest;
import com.scaffold.business.t11_verify_20261118.domain.T11Verify20261118VerifyPublishResponse;
import com.scaffold.business.t11_verify_20261118.service.T11Verify20261118Service;
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
 * T11增删改发布验证（表 {@code t11_verify_20261118}）的接口。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖，要加接口请回到设计器，
 * 或在同包的 {@code T11Verify20261118Extension}（C 档）里做，别在这里塞业务分支。
 *
 * <p>五个入口固定成一套（列表 / 导出 / 新增 / 修改 / 删除），路径与权限点由模型编码派生；
 * 返回结构统一走 {@code R}，分页统一走 {@code PageResult}，与底座 {@code /api/system} 同一形状。
 * 自定义动作在设计器里选 {@code custom:{name}}，平台只出一个带 TODO 的桩，实现由人填。
 */
@RestController
@RequestMapping("/api/t11_verify_20261118/t11_verify20261118")
public class T11Verify20261118Controller {

    private final T11Verify20261118Service service;

    public T11Verify20261118Controller(T11Verify20261118Service service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("t11_verify_20261118:t11_verify20261118:list")
    public R<PageResult<T11Verify20261118Row>> list(
            PageQuery query
    ) {
        return R.success(service.page(query));
    }

    /** 单行详情：前端编辑前取整行，避免用列表行填表时把没显示的字段清空 */
    @GetMapping("/{id}")
    @RequiresPermission("t11_verify_20261118:t11_verify20261118:list")
    public R<T11Verify20261118> detail(@PathVariable Long id) {
        return R.success(service.detail(id));
    }

    /** 导出当前筛选条件下的 CSV（上限由 Service 把住），文件名用模型中文名 */
    @GetMapping("/export")
    @RequiresPermission("t11_verify_20261118:t11_verify20261118:list")
    public ResponseEntity<String> export() {
        return CsvResponse.attachment("t11Verify20261118.csv", service.export());
    }

    @PostMapping
    @RequiresPermission("t11_verify_20261118:t11_verify20261118:add")
    @OperLog(module = "T11增删改发布验证", action = "新增")
    public R<Long> create(@RequestBody T11Verify20261118WriteRequest request) {
        return R.success(service.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("t11_verify_20261118:t11_verify20261118:edit")
    @OperLog(module = "T11增删改发布验证", action = "修改")
    public R<Void> update(@PathVariable Long id, @RequestBody T11Verify20261118WriteRequest request) {
        service.update(id, request);
        return R.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("t11_verify_20261118:t11_verify20261118:remove")
    @OperLog(module = "T11增删改发布验证", action = "删除")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.success(null);
    }

    /**
     * T11发布验证接口（自定义接口 {@code verify_publish}，契约 v1）。
     * 实现住 {@code T11Verify20261118VerifyPublishExtension}（C 档）；未实现时返回 {@code OPERATION_NOT_IMPLEMENTED}。
     */
    @PostMapping("/verify_publish")
    @RequiresPermission("t11:verify:publish")
    public R<T11Verify20261118VerifyPublishResponse> verifyPublish(@RequestBody T11Verify20261118VerifyPublishRequest request) {
        return R.success(service.verifyPublish(request));
    }
}
