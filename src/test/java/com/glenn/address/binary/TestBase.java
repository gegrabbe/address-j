package com.glenn.address.binary;

import com.glenn.address.domain.Entry;
import com.glenn.address.mongo.FileDataUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class TestBase {
    protected static final Logger logger = LoggerFactory.getLogger(TestBase.class);
    protected String testDataFile = "test-data.json";
    protected String outputFilePrefix = "test-binary-output";
    protected String inputFilePrefix = "test-binary-input";

    protected static final List<Parameters> PARAMETERS_LIST = List.of(
            new Parameters(new AvroService(), "Avro", "avro"),
            new Parameters(new BsonService(), "Bson", "bson"),
            new Parameters(new EncodedService(), "Encoded", "addr"),
            new Parameters(new GzipService(), "Gzip", "gz")
    );

    protected List<Entry> testEntries;

    static Stream<Parameters> binaryServiceProvider() {
        return PARAMETERS_LIST.stream();
    }

    @BeforeEach
    void init() {
        File testDataFile = new File(this.testDataFile);
        if (!testDataFile.exists()) {
            throw new IllegalStateException(
                    String.format("""
                                    ##########################################################
                                    Test data file not found: %s. Cannot proceed with tests.
                                    Please provide the test file before running the test.
                                    You can use the GenerateData test to create the file.
                                    """
                            , this.testDataFile)
            );
        }
        testEntries = readTestData();
    }

    @AfterEach
    void cleanup() {
        // Clean up all test output and input files
        for (Parameters tester : PARAMETERS_LIST) {
            deleteFile(tester.outputFile(outputFilePrefix));
            deleteFile(tester.inputFile(inputFilePrefix));
        }
        logger.info("Test cleanup completed");
    }

    protected void deleteFile(String fileName) {
        try {
            Files.deleteIfExists(Paths.get(fileName));
        } catch (Exception e) {
            logger.warn("Failed to delete test file: {}", fileName, e);
        }
    }

    protected boolean entriesMatch(List<Entry> wrote, List<Entry> read) {
        return (!wrote.isEmpty()) && wrote.size() == read.size() && wrote.equals(read);
    }

    protected List<Entry> readTestData() {
        // Read entries from JSON file
        FileDataUtil fileUtil = new FileDataUtil(this.testDataFile);
        List<Entry> entries = fileUtil.readData();

        if (entries == null || entries.isEmpty()) {
            logger.error("No entries read from {}", this.testDataFile);
            return List.of();
        }
        return entries;
    }

    protected void writeOutput(List<Entry> entries, Parameters param) {
        String fileName = param.outputFile(outputFilePrefix);
        BinaryService binaryService = param.tester();
        binaryService.writeEntries(entries, fileName);
        logger.info("Successfully converted {} entries from JSON to {} binary format", entries.size(), param.name());
        logger.info("Output file: {}", new File(fileName).getAbsolutePath());
    }

    protected void copyOutputToInput(Parameters param) {
        // Delete input file and copy output to input for next run
        String outputFile = param.outputFile(this.outputFilePrefix);
        String inputFile = param.inputFile(this.inputFilePrefix);
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

    protected List<Entry> readBinaryEntries(Parameters param) {
        String inputFile = param.inputFile(this.inputFilePrefix);
        List<Entry> entries = param.tester().readEntries(inputFile);
        logger.info("Read {} entries from {}", entries.size(), inputFile);
        return entries;
    }

    protected boolean runBinaryTest(List<Entry> entries, Parameters param) {
        writeOutput(entries, param);
        copyOutputToInput(param);
        List<Entry> readEntries = readBinaryEntries(param);
        boolean valid = entriesMatch(entries, readEntries);
        if (valid) {
            logger.info("!! entries match !!");
        } else {
            logger.error("!! ENTRIES DO NOT MATCH !!");
        }
        return valid;
    }

}
