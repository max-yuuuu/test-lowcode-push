package com.scaffold.flow.runtime;

import com.scaffold.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/** 固定 CRUD 生命周期，并显式区分核心事务提交前后的事实。 */
@Service
public class CrudFlowLifecycleService {
    private final PlatformTransactionManager transactions;

    @Autowired
    public CrudFlowLifecycleService(PlatformTransactionManager transactions) {
        if (transactions == null) throw new IllegalArgumentException("CRUD Flow 事务管理器不能为空");
        this.transactions = transactions;
    }

    public Result execute(Map<String, Object> input, Hooks hooks) {
        return execute(input, hooks, () -> false);
    }

    public Result execute(Map<String, Object> input, Hooks hooks, CancellationToken cancellation) {
        if (input == null || hooks == null || cancellation == null) {
            throw new IllegalArgumentException("CRUD Flow 生命周期输入无效");
        }
        try {
            Map<String, Object> current = hooks.authenticate().apply(copy(input));
            current = hooks.validate().apply(copy(current));
            current = hooks.authorize().apply(copy(current));
            current = hooks.beforeValidate().apply(copy(current));
            if (cancellation.cancelled()) {
                return cancelledBeforeCommit(hooks, "Flow 在提交前被取消");
            }

            Persisted persisted = persist(hooks, current);
            if (persisted == null) {
                throw new LifecycleException("CRUD_PERSIST_FAILED", "CRUD 核心写入未返回结果");
            }
            if (!persisted.committed()) {
                throw new LifecycleException("CRUD_PERSIST_FAILED", "CRUD 核心写入未提交");
            }
            if (cancellation.cancelled()) {
                return committedAfterCancel(hooks, persisted.output());
            }

            try {
                hooks.afterPersist().accept(copy(persisted.output()));
            } catch (RuntimeException error) {
                Failure failure = failure(error);
                invokeError(hooks, failure);
                Result result = new Result(Status.PARTIAL, persisted.output(), failure.code(), true, failure.message());
                audit(hooks, result);
                return result;
            }
            Result result = new Result(Status.SUCCESS, persisted.output(), null, true, null);
            audit(hooks, result);
            return result;
        } catch (RuntimeException error) {
            Failure failure = failure(error);
            invokeError(hooks, failure);
            Result result = new Result(Status.FAILED, Map.of(), failure.code(), false, failure.message());
            audit(hooks, result);
            return result;
        }
    }

    private Result cancelledBeforeCommit(Hooks hooks, String message) {
        Result result = new Result(Status.CANCELLED, Map.of(), "FLOW_CANCELLED", false, message);
        audit(hooks, result);
        return result;
    }

    private Result committedAfterCancel(Hooks hooks, Map<String, Object> output) {
        Result result = new Result(Status.COMMITTED, output, "FLOW_CANCELLED_AFTER_COMMIT", true,
                "Flow 取消发生在提交后，已提交状态保持不变");
        audit(hooks, result);
        return result;
    }

    private Persisted persist(Hooks hooks, Map<String, Object> input) {
        TransactionTemplate template = new TransactionTemplate(transactions);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> hooks.persist().apply(copy(input)));
    }

    private Failure failure(RuntimeException error) {
        if (error instanceof BusinessException business) {
            return new Failure(String.valueOf(business.code()), business.getMessage());
        }
        if (error instanceof LifecycleException lifecycle) {
            return new Failure(lifecycle.code(), lifecycle.getMessage());
        }
        return new Failure("CRUD_FLOW_FAILED", "CRUD Flow 执行失败");
    }

    private void invokeError(Hooks hooks, Failure failure) {
        try {
            hooks.onError().apply(failure);
        } catch (RuntimeException ignored) {
        }
    }

    private void audit(Hooks hooks, Result result) {
        try {
            hooks.audit().accept(result);
        } catch (RuntimeException ignored) {
        }
    }

    private Map<String, Object> copy(Map<String, Object> input) {
        return input == null ? new LinkedHashMap<>() : new LinkedHashMap<>(input);
    }

    public enum Status { SUCCESS, FAILED, CANCELLED, PARTIAL, COMMITTED }

    public record Result(Status status, Map<String, Object> output, String errorCode,
                         boolean committed, String message) { }

    public record Persisted(Map<String, Object> output, boolean committed) {
        public Persisted {
            output = output == null ? Map.of() : Map.copyOf(output);
        }
    }

    public record Failure(String code, String message) { }

    public static class LifecycleException extends RuntimeException {
        private final String code;
        public LifecycleException(String code, String message) {
            super(message);
            this.code = code;
        }
        public String code() { return code; }
    }

    public record Hooks(
            Function<Map<String, Object>, Map<String, Object>> authenticate,
            Function<Map<String, Object>, Map<String, Object>> validate,
            Function<Map<String, Object>, Map<String, Object>> authorize,
            Function<Map<String, Object>, Map<String, Object>> beforeValidate,
            Function<Map<String, Object>, Persisted> persist,
            Consumer<Map<String, Object>> afterPersist,
            Function<Failure, Failure> onError,
            Consumer<Result> audit) {
        public Hooks {
            if (authenticate == null || validate == null || authorize == null || beforeValidate == null
                    || persist == null || afterPersist == null || onError == null || audit == null) {
                throw new IllegalArgumentException("CRUD Flow 生命周期钩子不完整");
            }
        }
    }

    @FunctionalInterface
    public interface CancellationToken {
        boolean cancelled();
    }
}
