package com.scaffold.business.demo.service;


import com.scaffold.business.demo.domain.Demo;
import com.scaffold.business.demo.domain.DemoRow;
import com.scaffold.business.demo.domain.DemoWriteRequest;
import com.scaffold.business.demo.domain.DemoReconcile20261117Request;
import com.scaffold.business.demo.domain.DemoReconcile20261117Response;
import com.scaffold.business.demo.domain.DemoBatchShipRequest;
import com.scaffold.business.demo.domain.DemoBatchShipResponse;
import com.scaffold.business.demo.domain.DemoArchiveOrderRequest;
import com.scaffold.business.demo.domain.DemoArchiveOrderResponse;
import com.scaffold.business.demo.domain.DemoExportOrdersRequest;
import com.scaffold.business.demo.domain.DemoExportOrdersResponse;
import com.scaffold.business.demo.mapper.DemoMapper;
import com.scaffold.common.domain.PageQuery;
import com.scaffold.common.domain.PageResult;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.export.CsvUtils;
import com.scaffold.flow.runtime.CrudFlowLifecycleService;
import com.scaffold.business.flow.FlowSnapshotExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.framework.flow.CrudFlowException;
import com.scaffold.framework.security.LoginUser;
import com.scaffold.framework.security.PermissionResolver;
import com.scaffold.framework.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试模型（表 {@code demo}）的业务入口。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。业务规则、状态流转、
 * 跨实体事务请写到同包的 {@code DemoExtension}（C 档，生成一次后平台不再覆盖）——
 * 下面每个写操作与列表取数都会经过它的钩子。
 *
 * <p>单类 Service，不做 interface + impl 的二次分层 —— 与底座 {@code SystemManagementService}
 * 同一口径，一个模型一个类，读的人不用在两个文件之间跳。
 */
@Service
public class DemoService {

    /** 导出行数上限：导出不该把整表拉进内存，要全量请走数据库导出 */
    private static final int EXPORT_LIMIT = 5000;

    private final DemoMapper mapper;
    private final DemoExtension extension;
    private final CrudFlowLifecycleService lifecycle;
    private final PermissionResolver permissionResolver;
    private final FlowSnapshotExecutor flowSnapshots;
    private final ObjectMapper json;
    private final DemoArchiveExtension archiveExtension;
    private final DemoReconcile20261117Extension reconcile20261117Extension;
    private final DemoBatchShipExtension batchShipExtension;

    public DemoService(DemoMapper mapper, DemoExtension extension,
                               CrudFlowLifecycleService lifecycle, PermissionResolver permissionResolver, FlowSnapshotExecutor flowSnapshots, ObjectMapper json, DemoArchiveExtension archiveExtension, DemoReconcile20261117Extension reconcile20261117Extension, DemoBatchShipExtension batchShipExtension) {
        this.mapper = mapper;
        this.extension = extension;
        this.lifecycle = lifecycle;
        this.permissionResolver = permissionResolver;
        this.flowSnapshots = flowSnapshots;
        this.json = json;
        this.archiveExtension = archiveExtension;
        this.reconcile20261117Extension = reconcile20261117Extension;
        this.batchShipExtension = batchShipExtension;
    }

    /** 分页列表。查询条件逐个平铺在签名上：前端传什么、SQL 查什么，不用翻到 Mapper 才看清楚 */
    public PageResult<DemoRow> page(
            PageQuery query
    ) {
        List<DemoRow> rows = mapper.selectRows(query.offset(), query.normalizedPageSize());
        extension.afterRows(rows);
        long total = mapper.countByQuery();
        return PageResult.of(rows, total, query.normalizedPageNum(), query.normalizedPageSize());
    }

    /** 单行详情。编辑弹窗打开时先取这一份：列表行只有勾选进列表的字段 */
    public Demo detail(Long id) {
        Demo entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("测试模型不存在或已删除");
        }
        return entity;
    }

    /**
     * 导出 CSV，条件与列表同一套。<b>字典与关联列导出的是取值</b>（字典 code、目标行主键），
     * 不是中文标签 —— 需要中文时在 C 档的 {@code afterRows} 里换，别在导出的时候顺手改口径。
     */
    public String export() {
        List<DemoRow> rows = mapper.selectRows(0, EXPORT_LIMIT);
        extension.afterRows(rows);
        // 导出的列与页面上那一套对齐：开了审计开关时页面有「创建时间」列，CSV 也要有这一列
        List<List<Object>> data = rows.stream()
                .map(row -> CsvUtils.row(row.getId(), row.getName(), row.getCreateTime()))
                .toList();
        return CsvUtils.csv(List.of("主键", "名称", "创建时间"), data);
    }

    /** 新增测试模型，返回新行主键 */
    public Long create(DemoWriteRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("request", request);
        CrudFlowLifecycleService.Result result = lifecycle.execute(input, new CrudFlowLifecycleService.Hooks(
                current -> { SecurityUtils.getLoginUser(); return current; },
                current -> { validateCreate((DemoWriteRequest) current.get("request")); return current; },
                current -> { authorize("add"); return current; },
                current -> {
                    DemoWriteRequest createRequest = (DemoWriteRequest) current.get("request");
                    Demo entity = new Demo();
                    entity.setName(createRequest.getName());
                    extension.beforeCreate(entity);
                    current.put("entity", entity);
                    return current;
                },
                current -> {
                    Demo entity = (Demo) current.get("entity");
                    mapper.insert(entity, SecurityUtils.getUsername());
                    return new CrudFlowLifecycleService.Persisted(Map.of("id", entity.getId(), "entity", entity), true);
                },
                output -> extension.afterCreate((Demo) output.get("entity")),
                failure -> failure,
                ignored -> { }));
        requireSuccess(result);
        return ((Number) result.output().get("id")).longValue();
    }

    /**
     * 修改测试模型。表单提交的是整个表单，勾选了「可编辑」的字段按新值覆盖，
     * 没勾选的（含主键与审计列）一个都不碰。
     */
    public void update(Long id, DemoWriteRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("id", id);
        input.put("request", request);
        CrudFlowLifecycleService.Result result = lifecycle.execute(input, new CrudFlowLifecycleService.Hooks(
                current -> { SecurityUtils.getLoginUser(); return current; },
                current -> { validateUpdate(id, (DemoWriteRequest) current.get("request")); return current; },
                current -> { authorize("edit"); return current; },
                current -> {
                    DemoWriteRequest updateRequest = (DemoWriteRequest) current.get("request");
                    Demo entity = new Demo();
                    entity.setName(updateRequest.getName());
                    extension.beforeUpdate(id, entity);
                    current.put("entity", entity);
                    return current;
                },
                current -> {
                    Demo entity = (Demo) current.get("entity");
                    if (mapper.update(id, entity, SecurityUtils.getUsername()) == 0) {
                        throw new BusinessException("测试模型不存在或已删除");
                    }
                    return new CrudFlowLifecycleService.Persisted(Map.of("id", id, "entity", entity), true);
                },
                output -> extension.afterUpdate(id, (Demo) output.get("entity")),
                failure -> failure,
                ignored -> { }));
        requireSuccess(result);
    }

    /**
     * 删除测试模型。这张表勾了「逻辑删除」：只翻删除标记，历史留在库里。
     * 受影响行数为 0 说明这一行本来就不在了。
     */
    public void delete(Long id) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("id", id);
        CrudFlowLifecycleService.Result result = lifecycle.execute(input, new CrudFlowLifecycleService.Hooks(
                current -> { SecurityUtils.getLoginUser(); return current; },
                current -> current,
                current -> { authorize("remove"); return current; },
                current -> { extension.beforeDelete(id); return current; },
                current -> {
                    if (mapper.deleteById(id, SecurityUtils.getUsername()) == 0) {
                        throw new BusinessException("测试模型不存在或已删除");
                    }
                    return new CrudFlowLifecycleService.Persisted(Map.of("id", id), true);
                },
                output -> extension.afterDelete(id),
                failure -> failure,
                ignored -> { }));
        requireSuccess(result);
    }

    private void validateCreate(DemoWriteRequest request) {
    }

    private void validateUpdate(Long id, DemoWriteRequest request) {
    }

    private void authorize(String suffix) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (!permissionResolver.hasPermission(user, "demo:demo:" + suffix)) {
            throw new BusinessException(403, "无权执行该操作");
        }
    }

    private void requireSuccess(CrudFlowLifecycleService.Result result) {
        if (result.status() == CrudFlowLifecycleService.Status.SUCCESS) {
            return;
        }
        int httpStatus = result.committed() ? 409
                : "403".equals(result.errorCode()) || "OPERATION_FORBIDDEN".equals(result.errorCode()) ? 403 : 400;
        String status = switch (result.status()) {
            case FAILED -> "failed";
            case CANCELLED -> "cancelled";
            case PARTIAL -> "partial";
            case COMMITTED -> "committed";
            case SUCCESS -> "success";
        };
        throw new CrudFlowException(httpStatus, status, result.errorCode(), result.committed(), result.message());
    }

    /**
     * 归档（自定义接口 {@code archive}，契约 v1）。
     * 委托 {@code DemoArchiveExtension}（C 档）；事务边界由实现方自己声明。
     */
    public void archive() {
        archiveExtension.execute();
    }

    /**
     * 对账验证（自定义接口 {@code reconcile_20261117}，契约 v1）。
     * 委托 {@code DemoReconcile20261117Extension}（C 档）；事务边界由实现方自己声明。
     */
    public DemoReconcile20261117Response reconcile20261117(DemoReconcile20261117Request request) {
        return reconcile20261117Extension.execute(request);
    }

    /**
     * 批量发货（自定义接口 {@code batch_ship}，契约 v1）。
     * 委托 {@code DemoBatchShipExtension}（C 档）；事务边界由实现方自己声明。
     */
    public DemoBatchShipResponse batchShip(DemoBatchShipRequest request) {
        return batchShipExtension.execute(request);
    }

    /**
     * 归档订单（自定义接口 {@code archive_order}，契约 v2）。
     * 执行随项目发布的不可变 Flow 快照，结果包含状态、声明输出与 traceId。
     */
    public Map<String, Object> archiveOrder(DemoArchiveOrderRequest request) {
        return flowSnapshots.executeResult("archive_order", 2, json.convertValue(request, new TypeReference<Map<String, Object>>() {}));
    }

    /**
     * 导出订单（自定义接口 {@code export_orders}，契约 v3）。
     * 执行随项目发布的不可变 Flow 快照，结果包含状态、声明输出与 traceId。
     */
    public Map<String, Object> exportOrders(DemoExportOrdersRequest request) {
        return flowSnapshots.executeResult("export_orders", 3, json.convertValue(request, new TypeReference<Map<String, Object>>() {}));
    }
}
