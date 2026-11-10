package com.scaffold.business.e2e_order_trace.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * E2E 订单轨迹（表 {@code e2e_order_trace}）。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖，请不要在这里改业务逻辑，
 * 需要扩展请写到同包的 {@code E2eOrderTraceExtension}（C 档，生成一次后平台不再覆盖）。
 */
public class E2eOrderTrace {

    /** 轨迹号（列 {@code trace_id}，类型 {@code bigint}） */
    private Long traceId;

    /** 轨迹号（列 {@code trace_name}，类型 {@code varchar(255)}） */
    private String traceName;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新人 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public Long getTraceId() { return traceId; }
    public void setTraceId(Long traceId) { this.traceId = traceId; }
    public String getTraceName() { return traceName; }
    public void setTraceName(String traceName) { this.traceName = traceName; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
