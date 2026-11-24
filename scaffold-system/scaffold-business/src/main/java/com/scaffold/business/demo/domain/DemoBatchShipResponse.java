package com.scaffold.business.demo.domain;

import java.util.List;

/**
 * 批量发货的出参（接口标识 {@code batch_ship}，契约 v1，表 {@code demo}）。
 *
 * <p>本类由低代码设计器按接口契约生成，属 B 档：每次发布整文件覆盖。
 * 字段与中文说明来自契约 schema，改字段请回到设计器改契约（会产生新版本），别在这里手改。
 *
 * <p>实现写在同模型 service 包的 {@code DemoBatchShipExtension}（C 档）里，
 * 平台首次生成后不再覆盖那份。
 */
public class DemoBatchShipResponse {

    /** 处理结果（必填） */
    private List<ResultsItem> results;

    /** 处理结果 */
    public static class ResultsItem {

        /** 订单编号（必填） */
        private Long orderId;

        /** 是否成功（必填） */
        private Boolean success;

        /** 结果说明 */
        private String message;

        public Long getOrderId() { return orderId; }
        public void setOrderId(Long orderId) { this.orderId = orderId; }
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public List<ResultsItem> getResults() { return results; }
    public void setResults(List<ResultsItem> results) { this.results = results; }
}