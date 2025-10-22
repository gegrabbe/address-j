package com.glenn.address.binary;

import com.glenn.address.domain.Entry;

import java.util.List;

/**
 * Interface defining methods for serializing and deserializing address book entries
 * to various binary and compressed formats.
 * Implementations provide format-specific read/write functionality for Entry objects.
 */
public interface BinaryService {
    void writeEntries(List<Entry> entries, String outputFile);

    void writeString(String jsonString);

    List<Entry> readEntries(String inputFile);
}
