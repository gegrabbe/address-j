package com.glenn.address.binary;

import com.glenn.address.domain.Entry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BinaryValidTest extends TestBase {
    private static final Logger logger = LoggerFactory.getLogger(BinaryValidTest.class);

    @ParameterizedTest(name = "Validate {0} binary service")
    @MethodSource("binaryServiceProvider")
    void testBinaryServiceValidates(Parameters param) {
        logger.info("###################### {} ######################", param.name());
        Assertions.assertTrue(
                runBinaryTest(testEntries, param),
                String.format("Binary validation failed for %s service", param.name())
        );
    }

    @ParameterizedTest(name = "Handle empty entries with {0}")
    @MethodSource("binaryServiceProvider")
    void testEmptyEntries(Parameters param) {
        logger.info("Testing empty entries with {}", param.name());
        List<Entry> emptyEntries = List.of();

        String outputFile = param.outputFile(outputFilePrefix);
        param.tester().writeEntries(emptyEntries, outputFile);

        List<Entry> readEntries = param.tester().readEntries(outputFile);
        Assertions.assertTrue(
                readEntries.isEmpty(),
                String.format("Expected empty entries from %s, got %d entries", param.name(), readEntries.size())
        );
    }

    @ParameterizedTest(name = "Handle missing input file with {0}")
    @MethodSource("binaryServiceProvider")
    void testMissingInputFile(Parameters param) {
        logger.info("Testing missing input file with {}", param.name());
        String missingFile = "nonexistent-" + param.inputFile(inputFilePrefix);

        Exception exception = Assertions.assertThrows(
                RuntimeException.class,
                () -> param.tester().readEntries(missingFile),
                String.format("%s should throw exception for missing file", param.name())
        );
        logger.info("Expected exception caught: {}", exception.getMessage());
    }

    @ParameterizedTest(name = "Handle single entry with {0}")
    @MethodSource("binaryServiceProvider")
    void testSingleEntry(Parameters param) {
        logger.info("Testing single entry with {}", param.name());
        List<Entry> singleEntry = testEntries;
        if (!singleEntry.isEmpty()) {
            List<Entry> entries = List.of(singleEntry.getFirst());

            String outputFile = param.outputFile(outputFilePrefix);
            param.tester().writeEntries(entries, outputFile);
            copyOutputToInput(param);
            List<Entry> readEntries = readBinaryEntries(param);

            Assertions.assertTrue(
                    entriesMatch(entries, readEntries),
                    String.format("Single entry test failed for %s", param.name())
            );
        }
    }
}
