package com.glenn.address;

import com.glenn.address.domain.*;
import com.glenn.address.mongo.FileDataUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class GenerateData {
    private static final Logger logger = LoggerFactory.getLogger(GenerateData.class);
    private static final String OUT_FILE_NAME = "test-data.json";
    private static final List<Gender> GENDER_LIST = List.of(Gender.MALE, Gender.FEMALE);
    private int index = 1;
    private final RandomUtils randi = RandomUtils.insecure();
    private final RandomStringUtils rands = RandomStringUtils.insecure();

    private String lc(String value) {
        return StringUtils.capitalize(value.trim().toLowerCase());
    }

    public Entry createEntry() {
        return new Entry(index++,
                new Person(
                        lc(rands.nextAlphabetic(5)),
                        lc(rands.nextAlphabetic(7)),
                        randi.randomInt(10, 90),
                        GENDER_LIST.get(randi.randomInt(0,2)),
                        MaritalStatus.MARRIED
                ),
                new Address(
                        String.format("%d %s street", randi.randomInt(100, 9999), lc(rands.nextAlphabetic(7))),
                        lc(rands.nextAlphabetic(7)),
                        rands.nextAlphabetic(2).toUpperCase(),
                        rands.nextNumeric(5),
                        String.format("%s@%s.com", rands.nextAlphabetic(7).toLowerCase(), rands.nextAlphabetic(7).toLowerCase()),
                        rands.nextNumeric(10)
                ),
                lc(rands.nextAlphabetic(5)) + " " + lc(rands.nextAlphabetic(6)));
    }

    public static void main(String[] args) {
        GenerateData gend = new GenerateData();
        logger.info("##### Begin GenerateData #####");
        List<Entry> entries = new LinkedList<>();
        for (int xx = 0; xx < 10000; xx++) {
            entries.add(gend.createEntry());
        }

        FileDataUtil fdu = new FileDataUtil(OUT_FILE_NAME);
        fdu.writeData(entries);
    }
}
