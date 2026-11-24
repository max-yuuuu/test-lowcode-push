package com.scaffold.business.demo.service;


import com.scaffold.business.demo.domain.Demo;
import com.scaffold.business.demo.domain.DemoRow;
import com.scaffold.business.demo.domain.DemoWriteRequest;
import com.scaffold.business.demo.domain.DemoReconcile20261117Request;
import com.scaffold.business.demo.domain.DemoReconcile20261117Response;
import com.scaffold.business.demo.domain.DemoBatchShipRequest;
import com.scaffold.business.demo.domain.DemoBatchShipResponse;
import com.scaffold.business.demo.mapper.DemoMapper;
import com.scaffold.common.domain.PageQuery;
import com.scaffold.common.domain.PageResult;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.export.CsvUtils;
import com.scaffold.framework.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final DemoArchiveExtension archiveExtension;
    private final DemoReconcile20261117Extension reconcile20261117Extension;
    private final DemoBatchShipExtension batchShipExtension;

    public DemoService(DemoMapper mapper, DemoExtension extension, DemoArchiveExtension archiveExtension, DemoReconcile20261117Extension reconcile20261117Extension, DemoBatchShipExtension batchShipExtension) {
        this.mapper = mapper;
        this.extension = extension;
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
    @Transactional
    public Long create(DemoWriteRequest request) {
        Demo entity = new Demo();
        entity.setName(request.getName());
        extension.beforeCreate(entity);
        mapper.insert(entity, SecurityUtils.getUsername());
        extension.afterCreate(entity);
        return entity.getId();
    }

    /**
     * 修改测试模型。表单提交的是整个表单，勾选了「可编辑」的字段按新值覆盖，
     * 没勾选的（含主键与审计列）一个都不碰。
     */
    @Transactional
    public void update(Long id, DemoWriteRequest request) {
        Demo entity = new Demo();
        entity.setName(request.getName());
        extension.beforeUpdate(id, entity);
        if (mapper.update(id, entity, SecurityUtils.getUsername()) == 0) {
            throw new BusinessException("测试模型不存在或已删除");
        }
        extension.afterUpdate(id, entity);
    }

    /**
     * 删除测试模型。这张表勾了「逻辑删除」：只翻删除标记，历史留在库里。
     * 受影响行数为 0 说明这一行本来就不在了。
     */
    @Transactional
    public void delete(Long id) {
        extension.beforeDelete(id);
        if (mapper.deleteById(id, SecurityUtils.getUsername()) == 0) {
            throw new BusinessException("测试模型不存在或已删除");
        }
        extension.afterDelete(id);
    }

    /**
     * 归档（自定义接口 {@code archive}，契约 v1）：委托
     * {@code DemoArchiveExtension}（C 档）。骨架默认抛 {@code OPERATION_NOT_IMPLEMENTED}；
     * 事务边界由实现方自己声明，平台不替骨架方法包事务。
     */
    public void archive() {
        archiveExtension.execute();
    }

    /**
     * 对账验证（自定义接口 {@code reconcile_20261117}，契约 v1）：委托
     * {@code DemoReconcile20261117Extension}（C 档）。骨架默认抛 {@code OPERATION_NOT_IMPLEMENTED}；
     * 事务边界由实现方自己声明，平台不替骨架方法包事务。
     */
    public DemoReconcile20261117Response reconcile20261117(DemoReconcile20261117Request request) {
        return reconcile20261117Extension.execute(request);
    }

    /**
     * 批量发货（自定义接口 {@code batch_ship}，契约 v1）：委托
     * {@code DemoBatchShipExtension}（C 档）。骨架默认抛 {@code OPERATION_NOT_IMPLEMENTED}；
     * 事务边界由实现方自己声明，平台不替骨架方法包事务。
     */
    public DemoBatchShipResponse batchShip(DemoBatchShipRequest request) {
        return batchShipExtension.execute(request);
    }
}
