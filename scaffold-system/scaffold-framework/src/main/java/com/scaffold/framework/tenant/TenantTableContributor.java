package com.scaffold.framework.tenant;

import java.util.Collection;

/**
 * 租户表白名单的贡献点：谁建了带 {@code tenant_id} 的表，谁就把自己那几张登记进来。
 *
 * <p>底座只列 {@code sys_*}，平台元数据表由平台侧的实现追加。这件事不能有别的写法：
 * 白名单是一份常量清单，底座把它列全就意味着每个租户仓库里都躺着平台的表名 ——
 * 那些表在租户库里不存在，既不报错也没人清，只有源码归属对了才收得干净。
 */
public interface TenantTableContributor {

    /** 需要自动注入租户条件的表名（小写；实现里给不带前缀的裸表名即可） */
    Collection<String> tenantTables();
}
