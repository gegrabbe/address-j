package com.glenn.address.mongo;

import com.glenn.address.domain.Entry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for reading and writing address book entries to/from JSON files.
 * Provides serialization and deserialization of Entry objects using Gson with pretty printing.
 */
public class FileDataUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileDataUtil.class);

    private final String fileName;

    public FileDataUtil(String fileName) {
        this.fileName = fileName;
    }

    public void writeData(List<Entry> entries) {
        // Create Gson instance with pretty printing
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(entries);

        // Write JSON to file
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(json);
            logger.info("Successfully wrote {} entries to input-data.json", entries.size());
            logger.debug("JSON content: \n{}", StringUtils.substring(json, 0 ,100));
        } catch (IOException e) {
            logger.error("Failed to write JSON to file", e);
            throw new RuntimeException(e);
        }
    }

    public List<Entry> readData() {
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(fileName)) {
            Entry[] entriesArray = gson.fromJson(reader, Entry[].class);
            List<Entry> entries = Arrays.asList(entriesArray);
            logger.info("Successfully read {} entries from {}", entries.size(), fileName);
            logger.debug("Read entries: {}", StringUtils.substring(entries.toString(), 0 ,100));
            return entries;
        } catch (IOException e) {
            logger.error("Failed to read JSON from file: {}", fileName, e);
            throw new RuntimeException(e);
        }
    }

}
