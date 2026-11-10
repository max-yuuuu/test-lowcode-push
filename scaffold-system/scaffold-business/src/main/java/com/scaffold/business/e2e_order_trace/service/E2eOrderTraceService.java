package com.scaffold.business.e2e_order_trace.service;


import com.scaffold.business.e2e_order_trace.domain.E2eOrderTrace;
import com.scaffold.business.e2e_order_trace.domain.E2eOrderTraceRow;
import com.scaffold.business.e2e_order_trace.domain.E2eOrderTraceWriteRequest;
import com.scaffold.business.e2e_order_trace.mapper.E2eOrderTraceMapper;
import com.scaffold.common.domain.PageQuery;
import com.scaffold.common.domain.PageResult;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.export.CsvUtils;
import com.scaffold.framework.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * E2E 订单轨迹（表 {@code e2e_order_trace}）的业务入口。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。业务规则、状态流转、
 * 跨实体事务请写到同包的 {@code E2eOrderTraceExtension}（C 档，生成一次后平台不再覆盖）——
 * 下面每个写操作与列表取数都会经过它的钩子。
 *
 * <p>单类 Service，不做 interface + impl 的二次分层 —— 与底座 {@code SystemManagementService}
 * 同一口径，一个模型一个类，读的人不用在两个文件之间跳。
 */
@Service
public class E2eOrderTraceService {

    /** 导出行数上限：导出不该把整表拉进内存，要全量请走数据库导出 */
    private static final int EXPORT_LIMIT = 5000;

    private final E2eOrderTraceMapper mapper;
    private final E2eOrderTraceExtension extension;

    public E2eOrderTraceService(E2eOrderTraceMapper mapper, E2eOrderTraceExtension extension) {
        this.mapper = mapper;
        this.extension = extension;
    }

    /** 分页列表。查询条件逐个平铺在签名上：前端传什么、SQL 查什么，不用翻到 Mapper 才看清楚 */
    public PageResult<E2eOrderTraceRow> page(
            PageQuery query
    ) {
        List<E2eOrderTraceRow> rows = mapper.selectRows(query.offset(), query.normalizedPageSize());
        extension.afterRows(rows);
        long total = mapper.countByQuery();
        return PageResult.of(rows, total, query.normalizedPageNum(), query.normalizedPageSize());
    }

    /** 单行详情。编辑弹窗打开时先取这一份：列表行只有勾选进列表的字段 */
    public E2eOrderTrace detail(Long id) {
        E2eOrderTrace entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("E2E 订单轨迹不存在或已删除");
        }
        return entity;
    }

    /**
     * 导出 CSV，条件与列表同一套。<b>字典与关联列导出的是取值</b>（字典 code、目标行主键），
     * 不是中文标签 —— 需要中文时在 C 档的 {@code afterRows} 里换，别在导出的时候顺手改口径。
     */
    public String export() {
        List<E2eOrderTraceRow> rows = mapper.selectRows(0, EXPORT_LIMIT);
        extension.afterRows(rows);
        // 导出的列与页面上那一套对齐：开了审计开关时页面有「创建时间」列，CSV 也要有这一列
        List<List<Object>> data = rows.stream()
                .map(row -> CsvUtils.row(row.getTraceId(), row.getTraceId(), row.getCreateTime()))
                .toList();
        return CsvUtils.csv(List.of("轨迹号", "轨迹号", "创建时间"), data);
    }

    /** 新增E2E 订单轨迹，返回新行主键 */
    @Transactional
    public Long create(E2eOrderTraceWriteRequest request) {
        if (request.getTraceId() == null) {
            throw new BusinessException("轨迹号不能为空");
        }
        E2eOrderTrace entity = new E2eOrderTrace();
        entity.setTraceId(request.getTraceId());
        entity.setTraceName(request.getTraceName());
        extension.beforeCreate(entity);
        mapper.insert(entity, SecurityUtils.getUsername());
        extension.afterCreate(entity);
        return entity.getTraceId();
    }

    /**
     * 修改E2E 订单轨迹。表单提交的是整个表单，勾选了「可编辑」的字段按新值覆盖，
     * 没勾选的（含主键与审计列）一个都不碰。
     */
    @Transactional
    public void update(Long id, E2eOrderTraceWriteRequest request) {
        E2eOrderTrace entity = new E2eOrderTrace();
        entity.setTraceName(request.getTraceName());
        extension.beforeUpdate(id, entity);
        if (mapper.update(id, entity, SecurityUtils.getUsername()) == 0) {
            throw new BusinessException("E2E 订单轨迹不存在或已删除");
        }
        extension.afterUpdate(id, entity);
    }

    /**
     * 删除E2E 订单轨迹。这张表勾了「逻辑删除」：只翻删除标记，历史留在库里。
     * 受影响行数为 0 说明这一行本来就不在了。
     */
    @Transactional
    public void delete(Long id) {
        extension.beforeDelete(id);
        if (mapper.deleteById(id, SecurityUtils.getUsername()) == 0) {
            throw new BusinessException("E2E 订单轨迹不存在或已删除");
        }
        extension.afterDelete(id);
    }
}
