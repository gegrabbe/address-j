package com.glenn.address.mongo;

import com.glenn.address.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TestData {
    private static final Logger logger = LoggerFactory.getLogger(TestData.class);
    private static final String FILE_NAME = "input-data.json";

    private static final List<Person> PERSON_LIST = List.of(
            new Person("Glenn", "Grabbe", 60, Gender.MALE, MaritalStatus.MARRIED),
            new Person("Joy", "Grabbe", 57, Gender.FEMALE, MaritalStatus.MARRIED),
            new Person("Vince", "Grabbe", 27, Gender.MALE, MaritalStatus.SINGLE)
    );
    private static final List<Address> ADDRESS_LIST = List.of(
            new Address("123 Main Street", "Franklin", "TN", "37069", "ggrabbe@minstrel.com", "6155551212"),
            new Address("123 Main Street", "Franklin", "TN", "37069", "jcgrabbe@minstrel.com", "6155551212"),
            new Address("123 Simpson Lane", "Soldiers Grove", "WI", "25123", "vagrabbe@minstrel.com", "9815551212")
    );

    public static void main(String[] args) {
        logger.info("## Starting TestData ##");
        List<Entry> entries = new ArrayList<>();

        int index = 0;
        for (Person person : PERSON_LIST) {
            entries.add(new Entry(""+index, person, ADDRESS_LIST.get(index), "index: "+(index)));
            index++;
        }

        FileDataUtil fdu = new FileDataUtil(FILE_NAME);
        fdu.writeData(entries);

        List<Entry> updates = fdu.readData();
    }
}
