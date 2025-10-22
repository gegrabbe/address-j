package com.glenn.address.domain;

import java.util.Comparator;

/**
 * Comparator for sorting Entry objects by person's last name in ascending alphabetical order.
 * Handles null entries and missing names gracefully by returning equality.
 */
public class CompareByLastName implements Comparator<Entry> {

    @Override
    public int compare(Entry o1, Entry o2) {
        if(o1 == o2) {
            return 0;
        }
        if(o1 == null) {
            return 1;
        }
        if(o2 == null) {
            return -1;
        }
        try {
            return o1.person().lastName().compareTo(o2.person().lastName());
        } catch (Exception e) {
            return 0;
        }
    }
}
