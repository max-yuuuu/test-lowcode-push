package com.scaffold.framework.flow;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PackagedFlowConfiguration {
    @Bean
    FlowManifestLoader flowManifestLoader(ApplicationContext application) {
        return new FlowManifestLoader(application.getClassLoader());
    }

    @Bean
    PackagedFlowRuntime packagedFlowRuntime(FlowManifestLoader loader) {
        return new PackagedFlowRuntime(loader);
    }
}
