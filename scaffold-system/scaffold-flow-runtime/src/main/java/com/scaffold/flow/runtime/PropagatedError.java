package com.scaffold.flow.runtime;

/**
 * 宿主适配器抛出的、需要原样透传的受控失败（如权限或业务错误）。
 * 运行时不解读其语义，只保证不被包装成通用的 FLOW_NODE_FAILED。
 */
public interface PropagatedError {
}
