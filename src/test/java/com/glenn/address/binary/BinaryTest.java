package com.glenn.address.binary;

import com.glenn.address.domain.Entry;
import com.glenn.address.mongo.FileDataUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class BinaryTest {
    private static final Logger logger = LoggerFactory.getLogger(BinaryTest.class);
    private static final String TESTDATA_FILE = "test-data.json";
    private static final String OUTPUT_FILE = "test-binary-output";
    private static final String INPUT_FILE = "test-binary-input";

    private static final List<BinTester> BIN_LIST = List.of(
            new BinTester(new AvroService(), "Avro", "avro", 0L),
            new BinTester(new BsonService(), "Bson", "bson", 0L),
            new BinTester(new EncodedService(), "Encoded", "addr", 0L),
            new BinTester(new GzipService(), "Gzip", "gz", 0L)
    );

    private List<Entry> testEntries;

    record BinTester(BinaryService tester, String name, String extension, long timer) {
        public String outputFile() {
            return OUTPUT_FILE + "." + extension;
        }

        public String inputFile() {
            return INPUT_FILE + "." + extension;
        }
    }

    static Stream<BinTester> binaryServiceProvider() {
        return BIN_LIST.stream();
    }

    @BeforeEach
    void init() {
        File testDataFile = new File(TESTDATA_FILE);
        if (!testDataFile.exists()) {
            throw new IllegalStateException(
                String.format("""
                        ##########################################################
                        Test data file not found: %s. Cannot proceed with tests.
                        Please provide the test file before running the test.
                        You can use the GenerateData test to create the file.
                        """
                        , TESTDATA_FILE)
            );
        }
        testEntries = readTestData(TESTDATA_FILE);
    }

    @AfterEach
    void cleanup() {
        // Clean up all test output and input files
        for (BinTester tester : BIN_LIST) {
            deleteFile(tester.outputFile());
            deleteFile(tester.inputFile());
        }
        logger.info("Test cleanup completed");
    }

    private void deleteFile(String fileName) {
        try {
            Files.deleteIfExists(Paths.get(fileName));
        } catch (Exception e) {
            logger.warn("Failed to delete test file: {}", fileName, e);
        }
    }

    private boolean entriesMatch(List<Entry> wrote, List<Entry> read) {
        return (!wrote.isEmpty()) && wrote.size() == read.size() && wrote.equals(read);
    }

    private List<Entry> readTestData(String fileName) {
        // Read entries from JSON file
        FileDataUtil fileUtil = new FileDataUtil(fileName);
        List<Entry> entries = fileUtil.readData();

        if (entries == null || entries.isEmpty()) {
            logger.error("No entries read from {}", fileName);
            return List.of();
        }
        return entries;
    }

    private void writeOutput(List<Entry> entries, BinTester binTester) {
        String fileName = binTester.outputFile();
        BinaryService binaryService = binTester.tester;
        binaryService.writeEntries(entries, fileName);
        logger.info("Successfully converted {} entries from JSON to {} binary format", entries.size(), binTester.name);
        logger.info("Output file: {}", new File(fileName).getAbsolutePath());
    }

    private void copyOutputToInput(BinTester binTester) {
        // Delete input file and copy output to input for next run
        String outputFile = binTester.outputFile();
        String inputFile = binTester.inputFile();
        FileDataUtil fileUtil2 = new FileDataUtil(inputFile);
        if (fileUtil2.delete(inputFile)) {
            logger.info("Deleted old input file: {}", inputFile);
        }
        if (fileUtil2.copy(outputFile, inputFile)) {
            logger.info("Copied output file to input file: {} -> {}", outputFile, inputFile);
        } else {
            throw new RuntimeException("copy failed. cannot proceed");
        }
    }

    private List<Entry> readBinaryEntries(BinTester binTester) {
        String inputFile = binTester.inputFile();
        List<Entry> entries = binTester.tester.readEntries(inputFile);
        logger.info("Read {} entries from {}", entries.size(), inputFile);
        return entries;
    }

    private boolean runBinaryTest(List<Entry> entries, BinTester binTester) {
        writeOutput(entries, binTester);
        copyOutputToInput(binTester);
        List<Entry> readEntries = readBinaryEntries(binTester);
        boolean valid = entriesMatch(entries, readEntries);
        if(valid) {
            logger.info("!! entries match !!");
        } else {
            logger.error("!! ENTRIES DO NOT MATCH !!");
        }
        return valid;
    }

    @ParameterizedTest(name = "Validate {0} binary service")
    @MethodSource("binaryServiceProvider")
    void testBinaryServiceValidates(BinTester binTester) {
        logger.info("###################### {} ######################", binTester.name);
        Assertions.assertTrue(
            runBinaryTest(testEntries, binTester),
            String.format("Binary validation failed for %s service", binTester.name)
        );
    }

    @ParameterizedTest(name = "Handle empty entries with {0}")
    @MethodSource("binaryServiceProvider")
    void testEmptyEntries(BinTester binTester) {
        logger.info("Testing empty entries with {}", binTester.name);
        List<Entry> emptyEntries = List.of();

        String outputFile = binTester.outputFile();
        binTester.tester.writeEntries(emptyEntries, outputFile);

        List<Entry> readEntries = binTester.tester.readEntries(outputFile);
        Assertions.assertTrue(
            readEntries.isEmpty(),
            String.format("Expected empty entries from %s, got %d entries", binTester.name, readEntries.size())
        );
    }

    @ParameterizedTest(name = "Handle missing input file with {0}")
    @MethodSource("binaryServiceProvider")
    void testMissingInputFile(BinTester binTester) {
        logger.info("Testing missing input file with {}", binTester.name);
        String missingFile = "nonexistent-" + binTester.inputFile();

        Exception exception = Assertions.assertThrows(
            RuntimeException.class,
            () -> binTester.tester.readEntries(missingFile),
            String.format("%s should throw exception for missing file", binTester.name)
        );
        logger.info("Expected exception caught: {}", exception.getMessage());
    }

    @ParameterizedTest(name = "Handle single entry with {0}")
    @MethodSource("binaryServiceProvider")
    void testSingleEntry(BinTester binTester) {
        logger.info("Testing single entry with {}", binTester.name);
        List<Entry> singleEntry = testEntries;
        if (!singleEntry.isEmpty()) {
            List<Entry> entries = List.of(singleEntry.getFirst());

            String outputFile = binTester.outputFile();
            binTester.tester.writeEntries(entries, outputFile);
            copyOutputToInput(binTester);
            List<Entry> readEntries = readBinaryEntries(binTester);

            Assertions.assertTrue(
                entriesMatch(entries, readEntries),
                String.format("Single entry test failed for %s", binTester.name)
            );
        }
    }
}
