package com.scaffold.admin;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 底座启动模块的装配边界（方案 task2）：底座 admin 只扫底座自己的 Mapper，
 * 平台侧（generator / lowcode）的 Mapper 包归平台启动模块，不许混进底座。
 *
 * <p>纯反射断言，不起 Spring 上下文：这条不变式是「底座可独立交付」的根，
 * 一旦底座 @MapperScan 里出现平台包，交付项目就会去注册一堆不存在表对应的 Mapper。
 */
class ScaffoldAdminApplicationMapperScanTest {

    @Test
    void baseStartupScansOnlyBaseMappers() {
        MapperScan scan = ScaffoldAdminApplication.class.getAnnotation(MapperScan.class);
        assertThat(scan).as("底座启动类必须带 @MapperScan").isNotNull();
        assertThat(scan.value())
                .as("底座只扫系统模块与业务模块的 Mapper，平台侧 generator/lowcode 归平台启动模块")
                .containsExactly("com.scaffold.system.mapper", "com.scaffold.business");
    }
}