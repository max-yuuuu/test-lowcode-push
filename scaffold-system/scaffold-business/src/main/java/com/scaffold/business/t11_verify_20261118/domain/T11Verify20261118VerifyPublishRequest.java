package com.scaffold.business.t11_verify_20261118.domain;


/**
 * T11发布验证接口的入参（接口标识 {@code verify_publish}，契约 v1，表 {@code t11_verify_20261118}）。
 *
 * <p>本类由低代码设计器按接口契约生成，属 B 档：每次发布整文件覆盖。
 * 字段与中文说明来自契约 schema，改字段请回到设计器改契约（会产生新版本），别在这里手改。
 *
 * <p>实现写在同模型 service 包的 {@code T11Verify20261118VerifyPublishExtension}（C 档）里，
 * 平台首次生成后不再覆盖那份。
 */
public class T11Verify20261118VerifyPublishRequest {

    /** 编号（必填） */
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}