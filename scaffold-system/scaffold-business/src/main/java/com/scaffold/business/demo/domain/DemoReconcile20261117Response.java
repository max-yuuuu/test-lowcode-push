package com.scaffold.business.demo.domain;


/**
 * 对账验证的出参（接口标识 {@code reconcile_20261117}，契约 v1，表 {@code demo}）。
 *
 * <p>本类由低代码设计器按接口契约生成，属 B 档：每次发布整文件覆盖。
 * 字段与中文说明来自契约 schema，改字段请回到设计器改契约（会产生新版本），别在这里手改。
 *
 * <p>实现写在同模型 service 包的 {@code DemoReconcile20261117Extension}（C 档）里，
 * 平台首次生成后不再覆盖那份。
 */
public class DemoReconcile20261117Response {

    /** 是否成功（必填） */
    private Boolean success;

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
}