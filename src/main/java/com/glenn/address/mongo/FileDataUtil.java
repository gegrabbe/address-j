package com.glenn.address.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.glenn.address.domain.Entry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for reading and writing address book entries to/from JSON files.
 * Provides serialization and deserialization of Entry objects using Jackson with pretty printing.
 */
public class FileDataUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileDataUtil.class);

    private final String fileName;
    private final ObjectMapper objectMapper;

    public FileDataUtil(String fileName) {
        this.fileName = fileName;
        this.objectMapper = createObjectMapper();
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);  // pretty print to file
        return mapper;
    }

    public void writeData(List<Entry> entries) {
        try {
            String json = objectMapper.writeValueAsString(entries);
            Files.writeString(Paths.get(fileName), json);
            logger.info("Successfully wrote {} entries to input-data.json", entries.size());
            logger.debug("JSON content: \n{}", StringUtils.substring(json, 0, 100));
        } catch (IOException e) {
            logger.error("Failed to write JSON to file", e);
            throw new RuntimeException(e);
        }
    }

    public List<Entry> readData() {
        try {
            String json = Files.readString(Paths.get(fileName));
            Entry[] entriesArray = objectMapper.readValue(json, Entry[].class);
            List<Entry> entries = Arrays.asList(entriesArray);
            logger.info("Successfully read {} entries from {}", entries.size(), fileName);
            logger.debug("Read entries: {}", StringUtils.substring(entries.toString(), 0, 100));
            return entries;
        } catch (IOException e) {
            logger.error("Failed to read JSON from file: {}", fileName, e);
            throw new RuntimeException(e);
        }
    }

    public boolean delete(String deleteName) {
        File file = new File(deleteName);
        return file.delete();
    }

    public boolean copy(String fromName, String toName) {
        try {
            Files.copy(Paths.get(fromName), Paths.get(toName));
            logger.info("Successfully copied file from {} to {}", fromName, toName);
            return true;
        } catch (IOException e) {
            logger.error("Failed to copy file from {} to {}", fromName, toName, e);
            return false;
        }
    }

}
