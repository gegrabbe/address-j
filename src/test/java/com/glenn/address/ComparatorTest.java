package com.glenn.address;

import com.glenn.address.domain.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class ComparatorTest {

    private static final List<Person> PERSON_LIST = List.of(
            new Person("Glenn", "Smith", 60, Gender.MALE, MaritalStatus.MARRIED),
            new Person("Joy", "Jones", 57, Gender.FEMALE, MaritalStatus.MARRIED),
            new Person("Vince", "Alba", 27, Gender.MALE, MaritalStatus.SINGLE)
    );
    private static final List<Address> ADDRESS_LIST = List.of(
            new Address("123 Main Street", "Franklin", "TN", "37069", "aaaaa@minstrel.com", "6155551212"),
            new Address("123 Main Street", "Franklin", "TN", "37069", "bbbbb@minstrel.com", "6155551212"),
            new Address("123 Simpson Lane", "Soldiers Grove", "WI", "25123", "ccccc@minstrel.com", "9815551212")
    );
    private static final List<Integer> INDEXES = List.of(5, 3, 9);

    private final List<Entry> entries = new ArrayList<>();
    private final CompareById byId = new CompareById();
    private final CompareByLastName byLastName = new CompareByLastName();

    @BeforeEach
    void init() {
        int index = 0;
        for (Person person : PERSON_LIST) {
            entries.add(new Entry(INDEXES.get(index), person, ADDRESS_LIST.get(index), "index: " + (index)));
            index++;
        }
    }

    @Test
    void testCompareById() {
        List<Entry> slist = entries.stream().sorted(byId).toList();
        System.out.printf("\nSorted: %s\n", slist);
        Assertions.assertTrue(
                slist.get(0).entryId()
                        < slist.get(1).entryId()
                        &&
                        slist.get(1).entryId()
                                < slist.get(2).entryId());
    }

    @Test
    void testCompareByLastName() {
        List<Entry> slist = entries.stream().sorted(byLastName).toList();
        System.out.printf("\nSorted: %s\n", slist);
        Assertions.assertTrue(
                slist.get(0).person().lastName().compareTo(
                        slist.get(1).person().lastName()) < 0
                        &&
                        slist.get(1).person().lastName().compareTo(
                                slist.get(2).person().lastName()) < 0);
    }
}
