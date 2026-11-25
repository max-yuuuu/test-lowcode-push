package com.scaffold.framework.flow;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/** Loads packaged Flow data. It does not authorize an operation or validate node semantics. */
public final class FlowManifestLoader {
    private static final int MAX_BYTES = 256 * 1024;
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(64).build())
            .build()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private final ClassLoader application;

    public FlowManifestLoader(ClassLoader application) {
        this.application = Objects.requireNonNull(application);
    }

    /** All fields, including the digest, must be constants from the generated server adapter. */
    public record Reference(long projectId, long modelId, long operationId, int operationVersion,
                            int flowVersion, String sha256) {
        public Reference {
            if (projectId <= 0 || modelId <= 0 || operationId <= 0 || operationVersion <= 0 || flowVersion <= 0
                    || sha256 == null || !SHA256.matcher(sha256).matches()) {
                throw new IllegalArgumentException("Invalid Flow manifest reference");
            }
        }

        private String resource() {
            return "META-INF/scaffold/flows/" + projectId + "/" + modelId + "/" + operationId + "/"
                    + operationVersion + "/" + flowVersion + ".json";
        }
    }

    /** Returns a fresh graph; callers must validate it against their execution allowlist before use. */
    public JsonNode load(Reference reference) {
        try (var stream = application.getResourceAsStream(reference.resource())) {
            if (stream == null) {
                throw new InvalidManifest("Flow manifest missing from application");
            }
            byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new InvalidManifest("Flow manifest exceeds size limit");
            }
            if (!HexFormat.of().formatHex(digest(bytes)).equals(reference.sha256())) {
                throw new InvalidManifest("Flow manifest digest mismatch");
            }
            JsonNode manifest = JSON.readTree(bytes);
            if (!fields(manifest, "schemaVersion", "projectId", "modelId", "operationId",
                    "operationVersion", "flowVersion", "graph") || !number(manifest.path("schemaVersion"), 1)) {
                throw new InvalidManifest("Unsupported Flow manifest schema");
            }
            if (!number(manifest.path("projectId"), reference.projectId())
                    || !number(manifest.path("modelId"), reference.modelId())
                    || !number(manifest.path("operationId"), reference.operationId())
                    || !number(manifest.path("operationVersion"), reference.operationVersion())
                    || !number(manifest.path("flowVersion"), reference.flowVersion())) {
                throw new InvalidManifest("Flow manifest binding mismatch");
            }
            JsonNode graph = manifest.path("graph");
            validateGraphShape(graph);
            return graph;
        } catch (IOException e) {
            // Parser exceptions may contain manifest values.
            throw new InvalidManifest("Flow manifest unreadable or malformed");
        }
    }

    private static void validateGraphShape(JsonNode graph) {
        JsonNode nodes = graph.path("nodes");
        JsonNode edges = graph.path("edges");
        if (!fields(graph, "nodes", "edges") || !nodes.isArray() || nodes.isEmpty() || nodes.size() > 200
                || !edges.isArray() || edges.size() > 500) {
            throw new InvalidManifest("Invalid Flow manifest graph shape");
        }
        for (JsonNode node : nodes) {
            JsonNode version = node.path("version");
            if (!fields(node, "id", "kind", "version", "config") || !text(node.path("id"))
                    || !text(node.path("kind")) || !version.isIntegralNumber() || !version.canConvertToInt()
                    || version.intValue() <= 0 || !node.path("config").isObject()) {
                throw new InvalidManifest("Invalid Flow manifest node shape");
            }
        }
        for (JsonNode edge : edges) {
            if (!fields(edge, "from", "to") || !text(edge.path("from")) || !text(edge.path("to"))) {
                throw new InvalidManifest("Invalid Flow manifest edge shape");
            }
        }
    }

    private static boolean fields(JsonNode node, String... names) {
        if (node == null || !node.isObject() || node.size() != names.length) {
            return false;
        }
        for (String name : names) {
            if (!node.hasNonNull(name)) {
                return false;
            }
        }
        return true;
    }

    private static boolean number(JsonNode node, long expected) {
        return node.isIntegralNumber() && node.canConvertToLong() && node.longValue() == expected;
    }

    private static boolean text(JsonNode node) {
        return node.isTextual() && !node.textValue().isBlank();
    }

    private static byte[] digest(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static final class InvalidManifest extends IllegalStateException {
        private InvalidManifest(String message) {
            super(message);
        }
    }
}
