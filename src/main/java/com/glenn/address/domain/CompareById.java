package com.glenn.address.domain;

import java.util.Comparator;

public class CompareById implements Comparator<Entry> {

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
        return o1.entryId().compareTo(o2.entryId());
    }
}
