package com.glenn.address.mongo;

import com.glenn.address.domain.CompareById;
import com.glenn.address.domain.Entry;

import java.util.List;

public class NextEntryId {
    private Integer maxId = 0;

    public NextEntryId(MongoService mongoService) {
        load(mongoService);
    }

    private void load(MongoService mongoService) {
        List<Entry> entries = mongoService.readFromDatabase();
        if (!entries.isEmpty()) {
            entries.sort(new CompareById());
            maxId = entries.getLast().entryId();
        }
    }

    public Integer next() {
        return ++maxId;
    }

}
