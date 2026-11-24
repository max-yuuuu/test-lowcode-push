package com.scaffold.business.demo.domain;


/**
 * 归档订单的入参（接口标识 {@code archive_order}，契约 v2，表 {@code demo}）。
 *
 * <p>本类由低代码设计器按接口契约生成，属 B 档：每次发布整文件覆盖。
 * 字段与中文说明来自契约 schema，改字段请回到设计器改契约（会产生新版本），别在这里手改。
 *
 * <p>实现由已发布的 Flow 快照执行，平台每次发布按契约与快照整文件覆盖。
 */
public class DemoArchiveOrderRequest {

    /** 订单编号（必填） */
    private Long orderId;

    /** 归档备注 */
    private String remark;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}