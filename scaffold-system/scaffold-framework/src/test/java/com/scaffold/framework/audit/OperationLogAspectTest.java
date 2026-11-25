package com.scaffold.framework.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationLogAspectTest {

    @Test
    void credentialsAreMaskedInAuditParameters() {
        OperationLogAspect aspect = new OperationLogAspect(null, new ObjectMapper());
        ObjectNode parameters = new ObjectMapper().createObjectNode();
        parameters.put("credential", "raw-ai-key");
        parameters.put("nested", "keep-visible");

        aspect.maskSensitiveFields(parameters);

        assertThat(parameters.path("credential").asText()).isEqualTo("******");
        assertThat(parameters.path("nested").asText()).isEqualTo("keep-visible");
    }
}
