package com.scaffold.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@MapperScan({
        "com.scaffold.system.mapper",
        // 业务模块按包根扫：生成代码会往 com.scaffold.business.{模型} 下不断加子包，pom 与本行都不该再改。
        // 代价是这个包里的接口一律按 Mapper 注册，所以业务侧的接口（客户端、SPI 等）要写成类或放别的包
        "com.scaffold.business"
})
@SpringBootApplication(scanBasePackages = "com.scaffold")
public class ScaffoldAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScaffoldAdminApplication.class, args);
    }
}
