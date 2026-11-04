package com.scaffold.business.sales_order.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售订单主表的列表行（表 {@code sales_order}）。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖，需要改列表出参请回到设计器，
 * 或在同包的 {@code SalesOrderExtension}（C 档）里加工。
 *
 * <p>只装勾选进列表的字段（主键恒在：行操作、编辑、删除都靠它），列名到驼峰的别名写在
 * {@code SalesOrderMapper} 的查询里。字典与关联字段这里是<b>取值</b>（字典 code、目标行主键），
 * 中文标签由前端物料按字典解析，不在后端拼进 SQL。
 */
public class SalesOrderRow {

    /** 主键 */
    private Long id;

    /** 订单编号 */
    private String orderNo;

    /** 订单金额 */
    private BigDecimal amount;

    /** 状态 */
    private Integer status;

    /** 交货日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDate;

    /** 备注 */
    private String remark;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
