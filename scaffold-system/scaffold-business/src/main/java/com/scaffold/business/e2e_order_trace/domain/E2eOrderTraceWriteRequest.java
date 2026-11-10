package com.scaffold.business.e2e_order_trace.domain;


/**
 * E2E 订单轨迹的新增与修改入参（表 {@code e2e_order_trace}）。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。
 * 只装勾选了「可录入」或「可编辑」的字段 —— 没勾选的字段（含主键、审计列）在这里出现，
 * 等于让前端能改一个改不动的值。
 *
 * <p>必填校验写在 {@code E2eOrderTraceService} 里（与底座同一口径：校验和默认值规则跟业务走，
 * 不靠注解散在入参类上）；新增与修改共用这一份入参，修改时的目标行主键走路径参数。
 */
public class E2eOrderTraceWriteRequest {

    /** 轨迹号 */
    private Long traceId;

    /** 文本输入 */
    private String traceName;

    public Long getTraceId() { return traceId; }
    public void setTraceId(Long traceId) { this.traceId = traceId; }
    public String getTraceName() { return traceName; }
    public void setTraceName(String traceName) { this.traceName = traceName; }
}
