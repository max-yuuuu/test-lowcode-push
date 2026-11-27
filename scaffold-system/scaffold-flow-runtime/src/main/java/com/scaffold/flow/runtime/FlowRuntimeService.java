package com.scaffold.flow.runtime;

import com.scaffold.framework.flow.FlowExecutionContext;
import org.springframework.stereotype.Service;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Executes published Flow snapshots through server-owned adapters with shared execution bounds. */
@Service
public class FlowRuntimeService {
    public ExecutionResult execute(Map<String, Object> document, Map<String, Object> input) {
        return execute(document, input,
                (FlowRuntimeService.DataInvoker) (node, current) -> { throw failure("FLOW_NODE_UNSUPPORTED", "Flow 数据节点未配置服务端适配器"); },
                (FlowRuntimeService.OperationInvoker) (node, current) -> { throw failure("FLOW_NODE_UNSUPPORTED", "Flow operation 节点未配置服务端适配器"); },
                ExecutionOptions.none());
    }

    public ExecutionResult execute(Map<String, Object> document, Map<String, Object> input, OperationInvoker operationInvoker) {
        return execute(document, input,
                (FlowRuntimeService.DataInvoker) (node, current) -> { throw failure("FLOW_NODE_UNSUPPORTED", "Flow 数据节点未配置服务端适配器"); },
                operationInvoker, ExecutionOptions.none());
    }

    public ExecutionResult execute(Map<String, Object> document, Map<String, Object> input,
                                   DataInvoker dataInvoker, OperationInvoker operationInvoker) {
        return execute(document, input, dataInvoker, operationInvoker, ExecutionOptions.none());
    }

    public ExecutionResult execute(Map<String, Object> document, Map<String, Object> input,
                                   DataInvoker dataInvoker, OperationInvoker operationInvoker,
                                   ExecutionOptions options) {
        if (input == null || dataInvoker == null || operationInvoker == null || options == null) throw failure("FLOW_INPUT_INVALID", "Flow 输入无效");
        options.budget().checkPayload(input);
        FlowDocumentValidator.validate(document);
        Map<String, Map<String, Object>> nodes = index(document);
        String current = (String) document.get("start");
        Map<String, Object> output = new LinkedHashMap<>(input);
        String executionTraceId = options.context() != null && options.context().traceId() != null
                && !options.context().traceId().isBlank() ? options.context().traceId() : traceId();
        var definitions = FlowVariables.definitions(document);
        Map<String, Object> variables = new LinkedHashMap<>();
        definitions.forEach((id, definition) -> variables.put(id, definition.get("defaultValue")));
        List<String> path = new ArrayList<>();
        for (; current != null; ) {
            if (options.cancelled()) return new ExecutionResult("cancelled", output, List.copyOf(path), executionTraceId);
            if (options.expired()) throw failure("FLOW_DEADLINE_EXCEEDED", "Flow 执行超时");
            Map<String, Object> node = nodes.get(current);
            if (node == null) throw failure("FLOW_DOCUMENT_INVALID", "Flow 节点不存在");
            String kind = String.valueOf(node.get("kind"));
            String branch = null;
            path.add(current);
            try {
                options.budget().addExecutedNode();
                if (Set.of("input", "validate", "map", "audit", "end").contains(kind)) {
                    if ("map".equals(kind) && node.get("assignments") instanceof List<?> assignments) {
                        for (Object raw : assignments) {
                            Map<?, ?> assignment = (Map<?, ?>) raw;
                            String id = (String) assignment.get("variableId");
                            Object value = FlowVariables.resolve((Map<?, ?>) assignment.get("value"), variables, input, output);
                            if (!FlowVariables.accepts(definitions.get(id), value)) throw failure("FLOW_VARIABLE_TYPE", "赋值与变量类型不符");
                            variables.put(id, value);
                        }
                    }
                    if ("end".equals(kind) && node.get("valueBindings") instanceof Map<?, ?> bindings) {
                        output = FlowVariables.bind(bindings, variables, input, output);
                    }
                } else if ("operation".equals(kind)) {
                    output = new LinkedHashMap<>(operationInvoker.invoke(options.context(), node,
                            node.get("valueBindings") instanceof Map<?, ?> bindings
                                    ? FlowVariables.bind(bindings, variables, input, output) : bindInput(node, output)));
                } else if ("query".equals(kind) || "write".equals(kind)) {
                    output = new LinkedHashMap<>(dataInvoker.invoke(options.context(), node, output));
                    if ("query".equals(kind) && output.get("rows") instanceof List<?> rows) {
                        options.budget().addReadRows(rows.size());
                    }
                    if ("write".equals(kind)) options.budget().addWriteRows(affectedRows(output));
                } else if ("condition".equals(kind)) {
                    branch = conditionTarget(node, variables, input, output, path);
                } else if ("error".equals(kind)) {
                    String code = node.get("code") instanceof String text ? text : "FLOW_BUSINESS_ERROR";
                    throw FlowRuntimeException.business(code, String.valueOf(node.get("message")), List.copyOf(path));
                } else {
                    throw failure("FLOW_NODE_UNSUPPORTED", "Flow 节点暂不支持执行：" + kind);
                }
                options.budget().checkPayload(output);
            } catch (FlowRuntimeException error) {
                if (error.nodePath().isEmpty()) {
                    throw new FlowRuntimeException(error.code(), error.getMessage(), List.copyOf(path), error.business());
                }
                throw error;
            } catch (RuntimeException error) {
                // Adapter implementations may contain SQL, credentials, payloads or stack details.
                // Convert only unexpected failures at this boundary;宿主声明的受控失败（PropagatedError）
                // 原样透传，不被包装成通用 FLOW_NODE_FAILED，也不被误判成成功。
                if (error instanceof PropagatedError) throw error;
                throw new FlowRuntimeException("FLOW_NODE_FAILED", "Flow 节点执行失败", List.copyOf(path));
            }
            current = branch != null ? branch : (String) node.get("next");
            if (options.cancelled()) return new ExecutionResult("cancelled", output, List.copyOf(path), executionTraceId);
            if (options.expired()) throw failure("FLOW_DEADLINE_EXCEEDED", "Flow 执行超时");
        }
        options.budget().checkPayload(output);
        return new ExecutionResult("success", output, List.copyOf(path), executionTraceId);
    }

    private static long affectedRows(Map<String, Object> output) {
        Object value = output.get("affected");
        if (!(value instanceof Number number) || number.longValue() < 0 || number.longValue() != number.doubleValue()) {
            throw new FlowRuntimeException("FLOW_NODE_INVALID", "Flow 写入节点必须返回非负 affected 条数");
        }
        return number.longValue();
    }

    /**
     * 分支判定：eq/neq 同为数字比数值、其余按对象相等；gt/gte/lt/lte 只接受两个数字，
     * 不做字符串→数字的隐式转换（"10" 与 10 不是一回事，静默转换会让汇率/金额类判断出错）。
     */
    private static String conditionTarget(Map<String, Object> node, Map<String, Object> variables,
                                          Map<String, Object> input, Map<String, Object> output, List<String> path) {
        Map<?, ?> when = (Map<?, ?>) node.get("when");
        Object left = FlowVariables.resolve((Map<?, ?>) when.get("left"), variables, input, output);
        Object right = FlowVariables.resolve((Map<?, ?>) when.get("right"), variables, input, output);
        String operator = String.valueOf(when.get("operator"));
        boolean whenThen;
        if (Set.of("gt", "gte", "lt", "lte").contains(operator)) {
            if (!(left instanceof Number l) || !(right instanceof Number r)) {
                throw new FlowRuntimeException("FLOW_CONDITION_TYPE", "数值比较需要左右值都是数字", List.copyOf(path));
            }
            double a = l.doubleValue();
            double b = r.doubleValue();
            whenThen = switch (operator) {
                case "gt" -> a > b;
                case "gte" -> a >= b;
                case "lt" -> a < b;
                default -> a <= b;
            };
        } else {
            boolean equal = left instanceof Number l && right instanceof Number r
                    ? l.doubleValue() == r.doubleValue()
                    : Objects.equals(left, right);
            whenThen = "eq".equals(operator) == equal;
        }
        return (String) node.get(whenThen ? "then" : "else");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> bindInput(Map<String, Object> node, Map<String, Object> current) {
        Object raw = node.get("inputBindings");
        if (!(raw instanceof Map<?, ?> bindings) || bindings.isEmpty()) return new LinkedHashMap<>(current);
        Map<String, Object> bound = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : bindings.entrySet()) {
            String target = (String) entry.getKey();
            bound.put(target, valueAt(current, String.valueOf(entry.getValue())));
        }
        return bound;
    }

    private Object valueAt(Map<String, Object> current, String path) {
        Object value = current;
        for (String segment : path.split("\\.")) {
            if (!(value instanceof Map<?, ?> map) || !map.containsKey(segment)) return null;
            value = map.get(segment);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> index(Map<String, Object> document) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Object raw : (List<?>) document.get("nodes")) {
            result.put((String) ((Map<String, Object>) raw).get("id"), (Map<String, Object>) raw);
        }
        return result;
    }

    private String traceId() {
        String requestTraceId = MDC.get("traceId");
        if (requestTraceId != null && !requestTraceId.isBlank()) return requestTraceId;
        return Long.toHexString(ThreadLocalRandom.current().nextLong()) + Long.toHexString(System.nanoTime());
    }

    private FlowRuntimeException failure(String code, String message) {
        return new FlowRuntimeException(code, message);
    }

    public record ExecutionOptions(CancellationToken cancellation, Instant deadline,
                                   FlowExecutionContext context, FlowExecutionBudget budget) {
        public ExecutionOptions(CancellationToken cancellation, Instant deadline) {
            this(cancellation, deadline, null, new FlowExecutionBudget());
        }
        public ExecutionOptions {
            cancellation = cancellation == null ? () -> false : cancellation;
            budget = budget == null ? new FlowExecutionBudget() : budget;
        }
        public static ExecutionOptions none() { return new ExecutionOptions(() -> false, null); }
        public static ExecutionOptions forCurrentThread() {
            Thread executionThread = Thread.currentThread();
            return new ExecutionOptions(executionThread::isInterrupted, null);
        }
        public static ExecutionOptions withTimeout(Duration timeout) {
            if (timeout == null || timeout.isNegative() || timeout.isZero()) return none();
            return new ExecutionOptions(() -> false, Instant.now(Clock.systemUTC()).plus(timeout));
        }
        public static ExecutionOptions forContext(FlowExecutionContext context) {
            return forContext(context, new FlowExecutionBudget());
        }
        public static ExecutionOptions forContext(FlowExecutionContext context, FlowExecutionBudget budget) {
            if (context == null) return new ExecutionOptions(Thread.currentThread()::isInterrupted, null, null, budget);
            return new ExecutionOptions(Thread.currentThread()::isInterrupted, context.deadline(), context, budget);
        }
        public ExecutionOptions withContext(FlowExecutionContext context) {
            return new ExecutionOptions(cancellation, deadline, context, budget);
        }
        public boolean cancelled() { return cancellation.cancelled(); }
        public boolean expired() { return deadline != null && !Instant.now(Clock.systemUTC()).isBefore(deadline); }
    }

    @FunctionalInterface
    public interface CancellationToken {
        boolean cancelled();
    }

    @FunctionalInterface
    public interface DataInvoker {
        Map<String, Object> invoke(Map<String, Object> node, Map<String, Object> input);

        default Map<String, Object> invoke(FlowExecutionContext context, Map<String, Object> node,
                                           Map<String, Object> input) {
            return invoke(node, input);
        }
    }

    @FunctionalInterface
    public interface ContextDataInvoker extends DataInvoker {
        @Override
        Map<String, Object> invoke(FlowExecutionContext context, Map<String, Object> node,
                                   Map<String, Object> input);

        @Override
        default Map<String, Object> invoke(Map<String, Object> node, Map<String, Object> input) {
            throw new UnsupportedOperationException("Context data invoker requires trusted context");
        }
    }

    @FunctionalInterface
    public interface ContextOperationInvoker extends OperationInvoker {
        @Override
        Map<String, Object> invoke(FlowExecutionContext context, Map<String, Object> node,
                                   Map<String, Object> input);

        @Override
        default Map<String, Object> invoke(Map<String, Object> node, Map<String, Object> input) {
            throw new UnsupportedOperationException("Context operation invoker requires trusted context");
        }
    }

    @FunctionalInterface
    public interface OperationInvoker {
        Map<String, Object> invoke(Map<String, Object> node, Map<String, Object> input);

        default Map<String, Object> invoke(FlowExecutionContext context, Map<String, Object> node,
                                           Map<String, Object> input) {
            return invoke(node, input);
        }
    }

    public record ExecutionResult(String status, Map<String, Object> output,
                                  List<String> nodePath, String traceId) { }
}
