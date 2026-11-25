package com.scaffold.business.flow;

import com.scaffold.flow.runtime.ModelMetadataProvider;

import java.util.List;
import java.util.Map;

/** 交付侧元数据来源：直接读快照内嵌的模型/字段元数据，不依赖平台元数据表。 */
public final class ResourceModelMetadataProvider implements ModelMetadataProvider {
    private final Map<String, Object> models;

    public ResourceModelMetadataProvider(Map<String, Object> models) {
        this.models = models == null ? Map.of() : models;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> selectRuntimeModel(long modelId) {
        Object entry = models.get(String.valueOf(modelId));
        return entry instanceof Map<?, ?> map && map.get("model") instanceof Map<?, ?> model
                ? (Map<String, Object>) model : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> selectFields(long modelId) {
        Object entry = models.get(String.valueOf(modelId));
        return entry instanceof Map<?, ?> map && map.get("fields") instanceof List<?> fields
                ? (List<Map<String, Object>>) fields : List.of();
    }
}
