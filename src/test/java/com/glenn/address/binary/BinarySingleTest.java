package com.glenn.address.binary;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinarySingleTest extends TestBase {
    private static final Logger logger = LoggerFactory.getLogger(BinarySingleTest.class);

    public BinarySingleTest() {
        testDataFile = "test-data.json";
        outputFilePrefix = "output-data";
        inputFilePrefix = "input-data";
    }

    @Override
    void cleanup() {
        // keep the files
    }

    @Test
    void runSingleTests() {
        logger.info("##### runSingleTests #####");
        for (Parameters param : PARAMETERS_LIST) {
            Assertions.assertTrue(runBinaryTest(testEntries, param));
        }
    }
}
