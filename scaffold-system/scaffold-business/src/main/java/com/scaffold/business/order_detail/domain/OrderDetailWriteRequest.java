package com.scaffold.business.order_detail.domain;


/**
 * 订单明细的新增与修改入参（表 {@code order_detail}）。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖。
 * 只装勾选了「可录入」或「可编辑」的字段 —— 没勾选的字段（含主键、审计列）在这里出现，
 * 等于让前端能改一个改不动的值。
 *
 * <p>必填校验写在 {@code OrderDetailService} 里（与底座同一口径：校验和默认值规则跟业务走，
 * 不靠注解散在入参类上）；新增与修改共用这一份入参，修改时的目标行主键走路径参数。
 */
public class OrderDetailWriteRequest {

    /** 订单号 */
    private String orderName;

    public String getOrderName() { return orderName; }
    public void setOrderName(String orderName) { this.orderName = orderName; }
}
