package com.scaffold.business.demo.domain;


/**
 * 导出订单的出参（接口标识 {@code export_orders}，契约 v3，表 {@code demo}）。
 *
 * <p>本类由低代码设计器按接口契约生成，属 B 档：每次发布整文件覆盖。
 * 字段与中文说明来自契约 schema，改字段请回到设计器改契约（会产生新版本），别在这里手改。
 *
 * <p>实现由已发布的 Flow 快照执行，平台每次发布按契约与快照整文件覆盖。
 */
public class DemoExportOrdersResponse {

    /** 文件名称（必填） */
    private String fileName;

    /** 下载地址（必填） */
    private String downloadUrl;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}