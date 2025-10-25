package com.glenn.address.binary;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class BinaryTimerTest extends TestBase {
    private static final Logger logger = LoggerFactory.getLogger(BinaryTimerTest.class);

    record TimeTest(long timer, Parameters param) {
    }

    private void report(List<TimeTest> timers) {
        logger.info("##### time report #####");
        timers.sort(Comparator.comparingLong(obj -> obj.timer));
        for (TimeTest ttest : timers) {
            logger.info(String.format("%-8s: %dms", ttest.param.name(), ttest.timer));
        }
    }

    @Test
    void runTimerTests() {
        logger.info("##### runTimerTests #####");
        List<TimeTest> timers = new LinkedList<>();
        for (Parameters param : PARAMETERS_LIST) {
            Assertions.assertTrue(runBinaryTest(testEntries, param));
        }
        for (Parameters param : PARAMETERS_LIST) {
            long ts = System.currentTimeMillis();
            Assertions.assertTrue(runBinaryTest(testEntries, param));
            Assertions.assertTrue(runBinaryTest(testEntries, param));
            Assertions.assertTrue(runBinaryTest(testEntries, param));
            Assertions.assertTrue(runBinaryTest(testEntries, param));
            Assertions.assertTrue(runBinaryTest(testEntries, param));
            Assertions.assertTrue(runBinaryTest(testEntries, param));
            Assertions.assertTrue(runBinaryTest(testEntries, param));
            Assertions.assertTrue(runBinaryTest(testEntries, param));
            Assertions.assertTrue(runBinaryTest(testEntries, param));
            Assertions.assertTrue(runBinaryTest(testEntries, param));
            timers.add(new TimeTest(System.currentTimeMillis() - ts, param));
        }
        report(timers);
    }
}
