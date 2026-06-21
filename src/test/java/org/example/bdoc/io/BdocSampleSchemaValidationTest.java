package org.example.bdoc.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BdocSampleSchemaValidationTest {

    private final BdocXmlSerializer serializer = new BdocXmlSerializer();

    @ParameterizedTest(name = "valid sample: {0}")
    @MethodSource("validSamples")
    @DisplayName("All valid sample documents should pass XSD validation")
    void validSamplesShouldPass(Path sample) {
        String xml = read(sample);
        assertDoesNotThrow(() -> serializer.validate(xml));
        assertDoesNotThrow(() -> serializer.fromXml(xml));
    }

    @ParameterizedTest(name = "invalid sample: {0}")
    @MethodSource("invalidSamples")
    @DisplayName("All invalid sample documents should fail XSD validation")
    void invalidSamplesShouldFail(Path sample) {
        String xml = read(sample);
        assertThrows(BdocValidationException.class, () -> serializer.validate(xml));
    }

    static Stream<Path> validSamples() {
        return requiredSampleFiles("samples/valid");
    }

    static Stream<Path> invalidSamples() {
        return requiredSampleFiles("samples/invalid");
    }

    private static Stream<Path> requiredSampleFiles(String resourceDir) {
        List<Path> files = listSampleFiles(resourceDir);

        if (files.isEmpty()) {
            throw new IllegalStateException(
                    "No .bdoc files found in test resources: " + resourceDir +
                            ". Check that files exist under src/test/resources/" + resourceDir
            );
        }

        return files.stream();
    }

    private static List<Path> listSampleFiles(String resourceDir) {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(resourceDir);
            if (url == null) {
                throw new IllegalStateException(
                        "Resource directory not found in classpath: " + resourceDir +
                                ". Check that src/test/resources/" + resourceDir + " exists."
                );
            }

            Path dir = Path.of(url.toURI());

            try (Stream<Path> stream = Files.list(dir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".bdoc"))
                        .sorted()
                        .toList();
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to list sample files for: " + resourceDir, e);
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read sample file: " + path, e);
        }
    }
}