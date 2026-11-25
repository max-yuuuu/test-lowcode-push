package com.scaffold.framework.flow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.HexFormat;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlowManifestLoaderTest {
    private static final String RESOURCE = "META-INF/scaffold/flows/10/20/30/2/3.json";
    private static final String MANIFEST = """
            {"schemaVersion":1,"projectId":10,"modelId":20,"operationId":30,
             "operationVersion":2,"flowVersion":3,
             "graph":{"nodes":[{"id":"start","kind":"start","version":1,"config":{}},
                                  {"id":"end","kind":"end","version":1,"config":{}}],
                      "edges":[{"from":"start","to":"end"}]}}
            """;

    @TempDir Path temp;

    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    void executesOnlySelectedBranchFromPackagedSnapshot(int amount) throws Exception {
        String snapshot = branches("transform", "{}");
        try (var jar = jar(RESOURCE, snapshot)) {
            var output = new PackagedFlowRuntime(new FlowManifestLoader(jar)).execute(reference(snapshot),
                    JsonNodeFactory.instance.objectNode().put("amount", amount), Duration.ofSeconds(2), () -> false);
            assertThat(output.path("result").asText()).isEqualTo(amount == 4 ? "low" : "high");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"query", "write"})
    void rejectsUntrustedDatabaseNodesEvenOnUntakenBranch(String kind) throws Exception {
        String snapshot = branches(kind, "{\"fields\":[\"id\"]}");
        try (var jar = jar(RESOURCE, snapshot)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));
            assertThatThrownBy(() -> runtime.execute(reference(snapshot), JsonNodeFactory.instance.objectNode().put("amount", 5),
                    Duration.ofSeconds(2), () -> false)).hasMessage("FLOW_UNSUPPORTED_NODE");
        }
    }

    @Test
    void rejectsNodeReferencesThatAreNotGuaranteedToHaveExecutedOnAnUntakenBranch() throws Exception {
        String snapshot = branches("transform", "{\"fields\":{\"value\":{\"source\":\"node\",\"nodeId\":\"high\",\"path\":\"result\"}}}");
        try (var jar = jar(RESOURCE, snapshot)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));
            assertThatThrownBy(() -> runtime.execute(reference(snapshot), JsonNodeFactory.instance.objectNode().put("amount", 5),
                    Duration.ofSeconds(2), () -> false)).hasMessage("FLOW_INVALID_GRAPH");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"rules\":[{\"value\":{\"source\":\"input\",\"path\":\"amount\"},\"operator\":\"required\",\"compare\":null,\"message\":\"x\",\"extra\":true}]}",
            "{\"rules\":[{\"value\":{\"source\":\"input\",\"path\":\"broken..path\"},\"operator\":\"required\",\"compare\":null,\"message\":\"x\"}]}",
            "{\"rules\":[{\"value\":{\"source\":\"input\",\"path\":\"amount\"},\"operator\":\"execute\",\"compare\":null,\"message\":\"x\"}]}"
    })
    void rejectsMalformedValidationRulesEvenOnUntakenBranch(String config) throws Exception {
        String snapshot = branches("validate", config);
        try (var jar = jar(RESOURCE, snapshot)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));
            assertThatThrownBy(() -> runtime.execute(reference(snapshot), JsonNodeFactory.instance.objectNode().put("amount", 5),
                    Duration.ofSeconds(2), () -> false)).hasMessage("FLOW_INVALID_GRAPH");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"url\":\"not-allowed\"}", "{\"fields\":{\"x\":{\"source\":\"input\",\"path\":\"broken..path\"}}}"})
    void rejectsInvalidConfigEvenOnUntakenBranch(String config) throws Exception {
        String snapshot = branches("transform", config);
        try (var jar = jar(RESOURCE, snapshot)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));
            assertThatThrownBy(() -> runtime.execute(reference(snapshot), JsonNodeFactory.instance.objectNode().put("amount", 5),
                    Duration.ofSeconds(2), () -> false)).hasMessage("FLOW_INVALID_GRAPH");
        }
    }

    @Test
    void rejectsDisconnectedCycleBeforeExecution() throws Exception {
        String snapshot = MANIFEST.replace("\"edges\":[", "\"edges\":[{\"from\":\"loop\",\"to\":\"loop\"},")
                .replace("\"nodes\":[", "\"nodes\":[{\"id\":\"loop\",\"kind\":\"transform\",\"version\":1,\"config\":{}},");
        try (var jar = jar(RESOURCE, snapshot)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));
            assertThatThrownBy(() -> runtime.execute(reference(snapshot), JsonNodeFactory.instance.objectNode(),
                    Duration.ofSeconds(2), () -> false)).hasMessage("FLOW_INVALID_GRAPH");
        }
    }

    @Test
    void stopsAtMaximumExecutionDepth() throws Exception {
        var nodes = new StringBuilder("{\"id\":\"n0\",\"kind\":\"start\",\"version\":1,\"config\":{}},");
        var edges = new StringBuilder();
        for (int i = 1; i < 35; i++) {
            nodes.append("{\"id\":\"n").append(i).append("\",\"kind\":\"")
                    .append(i == 34 ? "end" : "transform").append("\",\"version\":1,\"config\":{}}")
                    .append(i == 34 ? "" : ",");
            edges.append("{\"from\":\"n").append(i - 1).append("\",\"to\":\"n").append(i).append("\"}")
                    .append(i == 34 ? "" : ",");
        }
        String snapshot = "{\"schemaVersion\":1,\"projectId\":10,\"modelId\":20,\"operationId\":30,\"operationVersion\":2,\"flowVersion\":3,\"graph\":{\"nodes\":["
                + nodes + "],\"edges\":[" + edges + "]}}";
        try (var jar = jar(RESOURCE, snapshot)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));
            assertThatThrownBy(() -> runtime.execute(reference(snapshot), JsonNodeFactory.instance.objectNode(),
                    Duration.ofSeconds(2), () -> false)).hasMessage("FLOW_BUDGET");
        }
    }

    @Test
    void checksCancellationAfterTerminalNodeAndEnforcesDeadline() throws Exception {
        try (var jar = jar(RESOURCE, MANIFEST)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));
            var ref = reference(MANIFEST);
            var input = JsonNodeFactory.instance.objectNode();
            var checks = new java.util.concurrent.atomic.AtomicInteger();
            assertThatThrownBy(() -> runtime.execute(ref, input, Duration.ofSeconds(2), () -> checks.incrementAndGet() == 3))
                    .hasMessage("FLOW_CANCELLED");
            assertThatThrownBy(() -> runtime.execute(ref, input, Duration.ZERO, () -> false)).hasMessage("FLOW_DEADLINE");
        }
    }

    @Test
    void rejectsExecutionTimeoutAboveServerMaximum() throws Exception {
        try (var jar = jar(RESOURCE, MANIFEST)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));
            assertThatThrownBy(() -> runtime.execute(reference(MANIFEST), JsonNodeFactory.instance.objectNode(),
                    Duration.ofSeconds(31), () -> false)).hasMessage("FLOW_BUDGET");
        }
    }

    @Test
    void rejectsRelationalConditionWithNonNumericValues() throws Exception {
        String snapshot = branches("transform", "{}");
        try (var jar = jar(RESOURCE, snapshot)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));
            assertThatThrownBy(() -> runtime.execute(reference(snapshot), JsonNodeFactory.instance.objectNode().put("amount", "five"),
                    Duration.ofSeconds(2), () -> false)).hasMessage("FLOW_INVALID_VALUE");
        }
    }

    private static String branches(String kind, String config) {
        return """
                {"schemaVersion":1,"projectId":10,"modelId":20,"operationId":30,"operationVersion":2,"flowVersion":3,
                 "graph":{"nodes":[
                  {"id":"start","kind":"start","version":1,"config":{}},
                  {"id":"choose","kind":"condition","version":1,"config":{"left":{"source":"input","path":"amount"},
                   "operator":"gte","right":{"source":"literal","value":5},"then":"high","otherwise":"map"}},
                  {"id":"map","kind":"%s","version":1,"config":%s},
                  {"id":"high","kind":"end","version":1,"config":{"output":{"result":{"source":"literal","value":"high"}}}},
                  {"id":"low","kind":"end","version":1,"config":{"output":{"result":{"source":"literal","value":"low"}}}}],
                 "edges":[{"from":"start","to":"choose"},{"from":"choose","to":"high"},
                  {"from":"choose","to":"map"},{"from":"map","to":"low"}]}}
                """.formatted(kind, config);
    }

    @Test
    void loadsPinnedSnapshotFromPackagedClasspath() throws Exception {
        try (var jar = jar(RESOURCE, MANIFEST)) {
            var graph = new FlowManifestLoader(jar).load(reference(MANIFEST));
            assertThat(graph.path("nodes").get(1).path("id").textValue()).isEqualTo("end");
        }
    }

    @Test
    void executesThroughContextOnlyWhenOperationBindingMatchesManifest() throws Exception {
        try (var jar = jar(RESOURCE, MANIFEST)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));
            var reference = reference(MANIFEST);
            var context = context(10, 20, 30, 2);

            var output = runtime.execute(reference, context, () -> false);

            assertThat(output).isEmpty();
        }
    }

    @Test
    void rejectsContextBoundToDifferentOperationBeforeLoadingManifest() throws Exception {
        try (var jar = jar(RESOURCE, MANIFEST)) {
            var runtime = new PackagedFlowRuntime(new FlowManifestLoader(jar));

            assertThatThrownBy(() -> runtime.execute(reference(MANIFEST), context(10, 20, 31, 2), () -> false))
                    .hasMessage("FLOW_BINDING_MISMATCH");
        }
    }

    private static FlowExecutionContext context(long projectId, long modelId, long operationId, int operationVersion) {
        return new FlowExecutionContext(Map.of(), new com.scaffold.framework.security.LoginUser(5L, "tester", 6L),
                6L, projectId, modelId, operationId, operationVersion, "operation:execute", "trace-test",
                Instant.now().plusSeconds(5), Set.of("operation:execute"),
                com.scaffold.framework.datascope.DataScopeCriteria.all());
    }

    @Test
    void rejectsDeeplyNestedInputWithoutRecursiveStackOverflow() throws Exception {
        try (var jar = jar(RESOURCE, MANIFEST)) {
            var input = JsonNodeFactory.instance.objectNode();
            var cursor = input;
            for (int i = 0; i < 256; i++) {
                var child = JsonNodeFactory.instance.objectNode();
                cursor.set("nested", child);
                cursor = child;
            }
            assertThatThrownBy(() -> new PackagedFlowRuntime(new FlowManifestLoader(jar)).execute(reference(MANIFEST),
                    input, Duration.ofSeconds(2), () -> false)).hasMessage("FLOW_BUDGET");
        }
    }

    @Test
    void rejectsOutputThatExpandsBeyondPayloadBudget() throws Exception {
        String snapshot = MANIFEST.replace("{\"id\":\"end\",\"kind\":\"end\",\"version\":1,\"config\":{}}",
                "{\"id\":\"end\",\"kind\":\"end\",\"version\":1,\"config\":{\"output\":{"
                        + "\"copyA\":{\"source\":\"input\",\"path\":\"blob\"},"
                        + "\"copyB\":{\"source\":\"input\",\"path\":\"blob\"}}}}");
        try (var jar = jar(RESOURCE, snapshot)) {
            var input = JsonNodeFactory.instance.objectNode().put("blob", "x".repeat(600_000));
            assertThatThrownBy(() -> new PackagedFlowRuntime(new FlowManifestLoader(jar)).execute(reference(snapshot), input,
                    Duration.ofSeconds(2), () -> false)).hasMessage("FLOW_BUDGET");
        }
    }

    @Test
    void rejectsChangedBytesEvenIfBindingAndShapeStillMatch() throws Exception {
        try (var jar = jar(RESOURCE, MANIFEST + "\n")) {
            assertThatThrownBy(() -> new FlowManifestLoader(jar).load(reference(MANIFEST)))
                    .isInstanceOf(FlowManifestLoader.InvalidManifest.class).hasMessageContaining("digest");
        }
    }

    @Test
    void doesNotFallBackToAnotherPackagedVersion() throws Exception {
        try (var jar = jar(RESOURCE.replace("/3.json", "/2.json"), MANIFEST)) {
            assertThatThrownBy(() -> new FlowManifestLoader(jar).load(reference(MANIFEST)))
                    .isInstanceOf(FlowManifestLoader.InvalidManifest.class).hasMessageContaining("missing");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"projectId", "modelId", "operationId", "operationVersion", "flowVersion"})
    void rejectsWrongBindingEvenWithMatchingDigest(String field) throws Exception {
        var wrong = MANIFEST.replaceFirst("\"" + field + "\":\\d+", "\"" + field + "\":99");
        try (var jar = jar(RESOURCE, wrong)) {
            assertThatThrownBy(() -> new FlowManifestLoader(jar).load(reference(wrong)))
                    .isInstanceOf(FlowManifestLoader.InvalidManifest.class).hasMessageContaining("binding");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"schemaVersion\":2,", "\"schemaVersion\":1,\"schemaVersion\":1,",
            "\"schemaVersion\":1,\"status\":\"DRAFT\",", "\"schemaVersion\":1,\"tenantId\":99,"})
    void rejectsUnsupportedOrAmbiguousManifest(String schema) throws Exception {
        rejectsShape(MANIFEST.replace("\"schemaVersion\":1,", schema));
    }

    @Test
    void rejectsTrailingDocumentAndOversizedResource() throws Exception {
        rejectsShape(MANIFEST + "{}");
        rejectsShape(MANIFEST + " ".repeat(256 * 1024));
    }

    @Test
    void rejectsMalformedGraphShell() throws Exception {
        rejectsShape(MANIFEST.replace("\"config\":{}", "\"config\":[]"));
        rejectsShape(MANIFEST.replace("\"to\":\"end\"", "\"target\":\"end\""));
    }

    private void rejectsShape(String manifest) throws Exception {
        try (var jar = jar(RESOURCE, manifest)) {
            assertThatThrownBy(() -> new FlowManifestLoader(jar).load(reference(manifest)))
                    .isInstanceOf(FlowManifestLoader.InvalidManifest.class);
        }
    }

    private static FlowManifestLoader.Reference reference(String manifest) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256").digest(manifest.getBytes(UTF_8));
        return new FlowManifestLoader.Reference(10, 20, 30, 2, 3, HexFormat.of().formatHex(digest));
    }

    private URLClassLoader jar(String resource, String manifest) throws Exception {
        Path file = Files.createTempFile(temp, "flow-", ".jar");
        try (var jar = new JarOutputStream(Files.newOutputStream(file))) {
            jar.putNextEntry(new JarEntry(resource));
            jar.write(manifest.getBytes(UTF_8));
            jar.closeEntry();
        }
        return new URLClassLoader(new URL[]{file.toUri().toURL()}, null);
    }
}
