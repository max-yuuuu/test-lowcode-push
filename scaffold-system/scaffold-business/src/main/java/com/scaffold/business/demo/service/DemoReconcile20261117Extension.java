package com.scaffold.business.demo.service;

import com.scaffold.business.demo.domain.OperationNotImplementedException;
import com.scaffold.business.demo.domain.DemoReconcile20261117Request;
import com.scaffold.business.demo.domain.DemoReconcile20261117Response;
import org.springframework.stereotype.Component;

/**
 * 对账验证的人工实现点（接口标识 {@code reconcile_20261117}，契约 v1，表 {@code demo}）。
 *
 * <p>本类属 <b>C 档</b>：只在接口首次发布时生成这一份骨架，之后平台永不覆盖。
 * 默认实现显式抛 {@code OperationNotImplementedException}（错误码 {@code OPERATION_NOT_IMPLEMENTED}），
 * 调用方拿到的是结构化失败而不是静默成功 —— 把方法体换成真实业务逻辑即为「实现补齐」。
 *
 * <p><b>方法签名是平台契约。</b>B 档 {@code DemoService} 按这个签名委托调用，
 * 改名或改参数会在下一次发布时直接编译失败。契约升版本（入参/出参形状变化）时 DTO 会重新生成，
 * 这里的签名要跟着新 DTO 调整 —— 编译器会告诉你哪里不匹配。
 *
 * <p>需要读库就在构造器里注入 {@code DemoMapper}，需要动别的业务就注入对应的 Service；
 * 跨表写入的事务边界请自己加 {@code @Transactional}，平台不替骨架方法包事务。
 */
@Component
public class DemoReconcile20261117Extension {

    /** 对账验证：TODO 人工实现，替换下面的抛错语句 */
    public DemoReconcile20261117Response execute(DemoReconcile20261117Request request) {
        throw new OperationNotImplementedException("reconcile_20261117", 1, "对账验证");
    }
}