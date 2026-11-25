package com.scaffold.flow.runtime;

import java.util.Map;
import java.util.Set;

/** 模型 Flow 生命周期；发布快照不可变，修改只能回到新的草稿 revision。 */
public enum FlowStatus {
    DRAFT, VALIDATED, PUBLISHED, DISABLED;
    private static final Map<FlowStatus, Set<FlowStatus>> TRANSITIONS = Map.of(
            DRAFT, Set.of(VALIDATED), VALIDATED, Set.of(DRAFT, PUBLISHED),
            PUBLISHED, Set.of(DISABLED), DISABLED, Set.of(PUBLISHED));
    public boolean canTransitionTo(FlowStatus target) { return TRANSITIONS.getOrDefault(this, Set.of()).contains(target); }
    public boolean isImmutable() { return this == PUBLISHED || this == DISABLED; }
}
