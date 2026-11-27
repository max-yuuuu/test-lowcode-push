package com.scaffold.flow.runtime;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mutable usage counters shared by one root Flow invocation and all nested calls. */
public final class FlowExecutionBudget {
    public static final int MAX_PAYLOAD_BYTES = 1_048_576;
    public static final int MAX_READ_ROWS = 1_000;
    public static final int MAX_WRITE_ROWS = 1_000;
    static final int MAX_EXECUTED_NODES = 64;
    private static final int MAX_VALUE_DEPTH = 64;
    private static final int MAX_VALUE_ELEMENTS = 100_000;

    private final int maxPayloadBytes;
    private final long maxReadRows;
    private final long maxWriteRows;
    private long readRows;
    private long writeRows;
    private int executedNodes;

    public FlowExecutionBudget() {
        this(MAX_PAYLOAD_BYTES, MAX_READ_ROWS, MAX_WRITE_ROWS);
    }

    public FlowExecutionBudget(int maxPayloadBytes, long maxReadRows, long maxWriteRows) {
        if (maxPayloadBytes <= 0 || maxReadRows <= 0 || maxWriteRows <= 0) {
            throw new IllegalArgumentException("Flow 预算上限必须为正数");
        }
        this.maxPayloadBytes = maxPayloadBytes;
        this.maxReadRows = maxReadRows;
        this.maxWriteRows = maxWriteRows;
    }

    public void checkPayload(Object value) {
        if (payloadBytes(value, maxPayloadBytes) > maxPayloadBytes) throw budgetExceeded();
    }

    public void addReadRows(long count) {
        addRows(count, maxReadRows, true);
    }

    public void addWriteRows(long count) {
        addRows(count, maxWriteRows, false);
    }

    public void addExecutedNode() {
        if (executedNodes >= MAX_EXECUTED_NODES) throw budgetExceeded();
        executedNodes++;
    }

    public long readRows() { return readRows; }
    public long writeRows() { return writeRows; }

    private void addRows(long count, long maximum, boolean read) {
        if (count < 0) throw new FlowRuntimeException("FLOW_NODE_INVALID", "Flow 节点返回的条数无效");
        long current = read ? readRows : writeRows;
        if (count > maximum - current) throw budgetExceeded();
        if (read) readRows += count;
        else writeRows += count;
    }

    private static long payloadBytes(Object root, int maximum) {
        record Entry(Object value, int depth) { }
        List<Entry> pending = new ArrayList<>();
        pending.add(new Entry(root, 1));
        long bytes = 0;
        int visited = 0;
        while (!pending.isEmpty()) {
            Entry entry = pending.remove(pending.size() - 1);
            if (++visited > MAX_VALUE_ELEMENTS) throw budgetExceeded();
            Object value = entry.value();
            if (value == null) {
                bytes += 4;
            } else if (value instanceof Map<?, ?> map) {
                bytes += 2;
                for (Map.Entry<?, ?> item : map.entrySet()) {
                    if (!(item.getKey() instanceof String key)) {
                        throw new FlowRuntimeException("FLOW_INPUT_INVALID", "Flow 对象键必须是字符串");
                    }
                    bytes += key.getBytes(StandardCharsets.UTF_8).length + 2;
                    if (entry.depth() >= MAX_VALUE_DEPTH) throw budgetExceeded();
                    pending.add(new Entry(item.getValue(), entry.depth() + 1));
                }
            } else if (value instanceof List<?> list) {
                bytes += 2;
                if (entry.depth() >= MAX_VALUE_DEPTH) throw budgetExceeded();
                for (Object item : list) pending.add(new Entry(item, entry.depth() + 1));
            } else {
                bytes += String.valueOf(value).getBytes(StandardCharsets.UTF_8).length;
            }
            if (bytes > maximum) throw budgetExceeded();
        }
        return bytes;
    }

    private static FlowRuntimeException budgetExceeded() {
        return new FlowRuntimeException("FLOW_BUDGET_EXCEEDED", "Flow 执行资源超过上限");
    }
}
