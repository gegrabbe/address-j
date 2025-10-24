package com.glenn.address.binary;

import com.glenn.address.domain.Entry;
import com.glenn.address.mongo.FileDataUtil;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Implementation of BinaryService for reading/writing entries in gzip-compressed JSON format.
 * Provides maximum compression ratio with flexibility for variable data structures.
 */
public class GzipService implements BinaryService {
    private static final Logger logger = LoggerFactory.getLogger(GzipService.class);
    private static final String OUT_FILE_NAME = "output-data.gz";
    private static final String IN_FILE_NAME = "input-data.gz";

    /**
     * Write Entry objects to gzip-compressed JSON format
     *
     * @param entries    List of Entry objects to serialize
     * @param outputFile Output file path
     */
    @Override
    public void writeEntries(List<Entry> entries, String outputFile) {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             OutputStreamWriter osw = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)) {

            Gson gson = new Gson();
            gson.toJson(entries, osw);
            osw.flush();

            logger.info("Successfully wrote {} entries to gzip file: {}", entries.size(), outputFile);
        } catch (IOException e) {
            logger.error("Failed to write entries to gzip file: {}", outputFile, e);
            throw new RuntimeException("Failed to write entries to gzip file", e);
        }
    }

    /**
     * Serialize JSON string to gzip-compressed format
     *
     * @param jsonString JSON string to compress
     */
    @Override
    public void writeString(String jsonString) {
        logger.debug("Writing JSON string to gzip format");
        try {
            Gson gson = new Gson();
            Entry[] entriesArray = gson.fromJson(jsonString, Entry[].class);
            List<Entry> entries = Arrays.asList(entriesArray);

            if (entries.isEmpty()) {
                logger.warn("No entries found in JSON string");
            }

            writeEntries(entries, OUT_FILE_NAME);
        } catch (Exception e) {
            logger.error("Failed to write JSON string to gzip format", e);
            throw new RuntimeException("Failed to write JSON string to gzip format", e);
        }
    }

    /**
     * Read entries from gzip-compressed JSON file
     *
     * @param inputFile Path to the gzip file
     * @return List of Entry objects read from the file
     */
    @Override
    public List<Entry> readEntries(String inputFile) {
        List<Entry> entries = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(inputFile);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             InputStreamReader isr = new InputStreamReader(gzis, StandardCharsets.UTF_8)) {

            Gson gson = new Gson();
            Entry[] entriesArray = gson.fromJson(isr, Entry[].class);

            if (entriesArray != null) {
                entries = Arrays.asList(entriesArray);
            }

            logger.info("Successfully read {} entries from gzip file: {}", entries.size(), inputFile);
        } catch (IOException e) {
            logger.error("Failed to read entries from gzip file: {}", inputFile, e);
            throw new RuntimeException("Failed to read entries from gzip file", e);
        }
        return entries;
    }

    /**
     * Main method: Read JSON file and write to gzip-compressed format
     */
    public static void main(String[] args) {
        String testFile = "export-data.json";
        if(args.length > 0) {
            testFile = args[0];
        }
        String outputFile = OUT_FILE_NAME;

        try {
            // Read entries from JSON file
            FileDataUtil fileUtil = new FileDataUtil(testFile);
            List<Entry> entries = fileUtil.readData();

            if (entries == null || entries.isEmpty()) {
                logger.error("No entries found in {}", testFile);
                System.exit(1);
            }

            // Write entries to gzip file
            BinaryService binaryService = new GzipService();
            binaryService.writeEntries(entries, outputFile);

            logger.info("Successfully converted {} entries from JSON to gzip format", entries.size());
            logger.info("Output file: {}", new File(outputFile).getAbsolutePath());

            // Verify by reading input-data.gz and comparing to entries written to output-data.gz
            List<Entry> readEntries = binaryService.readEntries(IN_FILE_NAME);
            logger.info("\nRead {} entries from " + IN_FILE_NAME, readEntries.size());

            if (entries.equals(readEntries)) {
                logger.info("✓ Verification successful: Entries match between output and input files");
            } else {
                logger.info("✗ Verification failed: Entries do not match");
                logger.info("  Written entries: {}", entries.size());
                logger.info("  Read entries: {}", readEntries.size());
            }

        } catch (Exception e) {
            logger.error("Error during gzip conversion: ", e);
            System.exit(1);
        }
    }
}
