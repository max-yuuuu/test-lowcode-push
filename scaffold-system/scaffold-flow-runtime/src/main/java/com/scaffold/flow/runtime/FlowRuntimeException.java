package com.scaffold.flow.runtime;

import java.util.List;

/** Stable, safe runtime failure; internal causes are intentionally not exposed to callers. */
public final class FlowRuntimeException extends RuntimeException {
    private final String code;
    private final List<String> nodePath;
    private final boolean business;

    public FlowRuntimeException(String code, String message) {
        this(code, message, List.of());
    }

    public FlowRuntimeException(String code, String message, List<String> nodePath) {
        this(code, message, nodePath, false);
    }

    public FlowRuntimeException(String code, String message, List<String> nodePath, boolean business) {
        super(message);
        this.code = code;
        this.nodePath = nodePath == null ? List.of() : List.copyOf(nodePath);
        this.business = business;
    }

    /** error 节点抛出的受控业务失败：code/message 来自已校验的节点配置，可原样回给调用方。 */
    public static FlowRuntimeException business(String code, String message, List<String> nodePath) {
        return new FlowRuntimeException(code, message, nodePath, true);
    }

    public boolean business() { return business; }

    public String code() { return code; }
    public List<String> nodePath() { return nodePath; }
}
