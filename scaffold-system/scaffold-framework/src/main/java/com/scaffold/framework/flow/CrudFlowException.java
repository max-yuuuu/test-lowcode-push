package com.scaffold.framework.flow;

/** CRUD 生命周期失败；committed 用于区分回滚失败与已提交后的部分成功。 */
public class CrudFlowException extends RuntimeException {
    private final int httpStatus;
    private final String status;
    private final String errorCode;
    private final boolean committed;

    public CrudFlowException(int httpStatus, String status, String errorCode, boolean committed, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.status = status;
        this.errorCode = errorCode;
        this.committed = committed;
    }

    public int httpStatus() { return httpStatus; }
    public String status() { return status; }
    public String errorCode() { return errorCode; }
    public boolean committed() { return committed; }
}
