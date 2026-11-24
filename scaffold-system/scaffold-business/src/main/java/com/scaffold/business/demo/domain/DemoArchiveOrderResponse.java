package com.scaffold.business.demo.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * 归档订单的出参（接口标识 {@code archive_order}，契约 v2，表 {@code demo}）。
 *
 * <p>本类由低代码设计器按接口契约生成，属 B 档：每次发布整文件覆盖。
 * 字段与中文说明来自契约 schema，改字段请回到设计器改契约（会产生新版本），别在这里手改。
 *
 * <p>实现由已发布的 Flow 快照执行，平台每次发布按契约与快照整文件覆盖。
 */
public class DemoArchiveOrderResponse {

    /** 是否归档成功（必填） */
    private Boolean archived;

    /** 归档时间（必填） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime archivedAt;

    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
}