package com.glenn.address.binary;

import com.glenn.address.domain.Entry;

import java.util.List;

public interface BinaryService {
    void writeEntries(List<Entry> entries, String outputFile);

    void writeString(String jsonString);

    List<Entry> readEntries(String inputFile);
}
