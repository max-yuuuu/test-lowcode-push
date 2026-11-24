package com.scaffold.business.demo.domain;

import java.util.List;

/**
 * 批量发货的入参（接口标识 {@code batch_ship}，契约 v1，表 {@code demo}）。
 *
 * <p>本类由低代码设计器按接口契约生成，属 B 档：每次发布整文件覆盖。
 * 字段与中文说明来自契约 schema，改字段请回到设计器改契约（会产生新版本），别在这里手改。
 *
 * <p>实现写在同模型 service 包的 {@code DemoBatchShipExtension}（C 档）里，
 * 平台首次生成后不再覆盖那份。
 */
public class DemoBatchShipRequest {

    /** 待发货订单（必填） */
    private List<OrdersItem> orders;

    /** 操作人（必填） */
    private String operator;

    /** 待发货订单 */
    public static class OrdersItem {

        /** 订单编号（必填） */
        private Long orderId;

        /** 发货仓库编码（必填） */
        private String warehouseCode;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public String getWarehouseCode() { return warehouseCode; }
        public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    }

    public List<OrdersItem> getOrders() { return orders; }
    public void setOrders(List<OrdersItem> orders) { this.orders = orders; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}