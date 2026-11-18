package com.scaffold.business.t11_verify_20261118.domain;

import com.scaffold.common.exception.BusinessException;

/**
 * 自定义接口尚未实现（错误码 {@code OPERATION_NOT_IMPLEMENTED}）。
 *
 * <p>本类由低代码设计器生成，属 B 档：每次发布整文件覆盖。C 档扩展点的默认实现抛它，
 * 同包 controller 的 {@code OperationErrorAdvice} 把它翻成带 {@code errorCode} 的失败响应 ——
 * 不会被统一返回层包装成成功。业务代码里不要抛它来表达别的含义。
 */
public class OperationNotImplementedException extends BusinessException {

    private final String operationCode;
    private final int operationVersion;

    public OperationNotImplementedException(String operationCode, int operationVersion, String operationName) {
        super(400, "接口「" + operationName + "」的实现尚未补齐，暂不可调用");
        this.operationCode = operationCode;
        this.operationVersion = operationVersion;
    }

    /** 接口标识（设计器里登记的契约编码） */
    public String operationCode() {
        return operationCode;
    }

    /** 抛错时的契约版本号 */
    public int operationVersion() {
        return operationVersion;
    }
}