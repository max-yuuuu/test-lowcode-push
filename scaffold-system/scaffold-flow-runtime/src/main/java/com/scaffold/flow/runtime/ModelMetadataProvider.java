package com.scaffold.flow.runtime;

import java.util.List;
import java.util.Map;

/** 宿主注入的模型元数据来源：运行时自身不持有平台 mapper，平台与交付项目各自适配。 */
public interface ModelMetadataProvider {
    /** 取存活模型（tableName/projectId/status/deleted/enableLogicDelete/enableAuditField/enableDataScope）。 */
    Map<String, Object> selectRuntimeModel(long modelId);

    /** 取模型字段元数据（fieldCode/columnName/isPrimary/isInsert/isEdit/isAutoIncrement）。 */
    List<Map<String, Object>> selectFields(long modelId);
}
