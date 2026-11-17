package com.scaffold.business.demo.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 测试模型的列表行（表 {@code demo}）。
 *
 * <p>本类由低代码设计器按模型定义生成，属 B 档：每次发布整文件覆盖，需要改列表出参请回到设计器，
 * 或在同包的 {@code DemoExtension}（C 档）里加工。
 *
 * <p>只装勾选进列表的字段（主键恒在：行操作、编辑、删除都靠它），列名到驼峰的别名写在
 * {@code DemoMapper} 的查询里。字典与关联字段这里是<b>取值</b>（字典 code、目标行主键），
 * 中文标签由前端物料按字典解析，不在后端拼进 SQL。
 */
public class DemoRow {

    /** 主键 */
    private Long id;

    /** 名称 */
    private String name;

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
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
