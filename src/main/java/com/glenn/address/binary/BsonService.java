package com.glenn.address.binary;

import com.glenn.address.domain.Address;
import com.glenn.address.domain.Entry;
import com.glenn.address.domain.Gender;
import com.glenn.address.domain.MaritalStatus;
import com.glenn.address.domain.Person;
import com.google.gson.Gson;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of BinaryService for reading/writing entries in BSON binary format.
 * Uses MongoDB's BSON codec for efficient document serialization of address book entries.
 */
public class BsonService implements BinaryService {
    private static final Logger logger = LoggerFactory.getLogger(BsonService.class);
    private static final String OUT_FILE_NAME = "output-data.bson";
    private static final String IN_FILE_NAME = "input-data.bson";

    /**
     * Write Entry objects to BSON binary format
     *
     * @param entries    List of Entry objects to serialize
     * @param outputFile Output file path
     */
    @Override
    public void writeEntries(List<Entry> entries, String outputFile) {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            BsonDocumentCodec codec = new BsonDocumentCodec();
            for (Entry entry : entries) {
                BsonDocument document = entryToBsonDocument(entry);
                BasicOutputBuffer buffer = new BasicOutputBuffer();
                BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
                codec.encode(writer, document, EncoderContext.builder().build());
                byte[] bsonBytes = buffer.toByteArray();
                // Write the complete BSON document as-is (it includes the size at the beginning)
                fos.write(bsonBytes);
            }
            logger.info("Successfully wrote {} entries to BSON binary file: {}", entries.size(), outputFile);
        } catch (IOException e) {
            logger.error("Failed to write entries to BSON file: {}", outputFile, e);
            throw new RuntimeException("Failed to write entries to BSON file", e);
        }
    }

    /**
     * Convert Entry object to BSON Document
     */
    private BsonDocument entryToBsonDocument(Entry entry) {
        BsonDocument document = new BsonDocument();

        document.put("entryId", new BsonInt32(entry.entryId()));
        document.put("notes", entry.notes() != null ? new BsonString(entry.notes()) : new BsonNull());

        // Convert Person
        BsonDocument personDoc = new BsonDocument();
        personDoc.put("firstName", entry.person().firstName() != null ? new BsonString(entry.person().firstName()) : new BsonNull());
        personDoc.put("lastName", entry.person().lastName() != null ? new BsonString(entry.person().lastName()) : new BsonNull());
        personDoc.put("age", new BsonInt32(entry.person().age()));
        personDoc.put("gender", entry.person().gender() != null ? new BsonString(entry.person().gender().toString()) : new BsonNull());
        personDoc.put("maritalStatus", entry.person().maritalStatus() != null ? new BsonString(entry.person().maritalStatus().toString()) : new BsonNull());
        document.put("person", personDoc);

        // Convert Address
        BsonDocument addressDoc = new BsonDocument();
        addressDoc.put("street", entry.address().street() != null ? new BsonString(entry.address().street()) : new BsonNull());
        addressDoc.put("city", entry.address().city() != null ? new BsonString(entry.address().city()) : new BsonNull());
        addressDoc.put("state", entry.address().state() != null ? new BsonString(entry.address().state()) : new BsonNull());
        addressDoc.put("zip", entry.address().zip() != null ? new BsonString(entry.address().zip()) : new BsonNull());
        addressDoc.put("email", entry.address().email() != null ? new BsonString(entry.address().email()) : new BsonNull());
        addressDoc.put("phone", entry.address().phone() != null ? new BsonString(entry.address().phone()) : new BsonNull());
        document.put("address", addressDoc);

        return document;
    }

    /**
     * Serialize JSON string to BSON binary format
     * Converts JSON string to a List<Entry> and uses writeEntries to avoid duplication
     */
    @Override
    public void writeString(String jsonString) {
        logger.debug("Writing JSON string to BSON format");
        try {
            Gson gson = new Gson();
            Entry[] entriesArray = gson.fromJson(jsonString, Entry[].class);
            List<Entry> entries = java.util.Arrays.asList(entriesArray);

            if (entries.isEmpty()) {
                logger.warn("No entries found in JSON string");
            }

            writeEntries(entries, OUT_FILE_NAME);
        } catch (Exception e) {
            logger.error("Failed to write JSON string to BSON format", e);
            throw new RuntimeException("Failed to write JSON string to BSON format", e);
        }
    }

    /**
     * Read entries from BSON binary file
     *
     * @param inputFile Path to the BSON binary file
     * @return List of Entry objects read from the file
     */
    @Override
    public List<Entry> readEntries(String inputFile) {
        List<Entry> entries = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            byte[] buffer = fis.readAllBytes();
            int offset = 0;
            BsonDocumentCodec codec = new BsonDocumentCodec();

            while (offset < buffer.length) {
                // Read the BSON document size (first 4 bytes, little-endian)
                if (offset + 4 > buffer.length) {
                    break;
                }

                // Extract the size from the buffer
                byte[] sizeBytes = new byte[4];
                System.arraycopy(buffer, offset, sizeBytes, 0, 4);
                ByteBuffer sizeBuffer = ByteBuffer.wrap(sizeBytes);
                sizeBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                int size = sizeBuffer.getInt();

                // Validate size
                if (size <= 0 || offset + size > buffer.length) {
                    logger.warn("Invalid BSON document size {} at offset {}", size, offset);
                    break;
                }

                // Extract and decode the BSON document
                byte[] docBytes = new byte[size];
                System.arraycopy(buffer, offset, docBytes, 0, size);
                ByteBuffer docBuffer = ByteBuffer.wrap(docBytes);
                docBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
                BsonBinaryReader reader = new BsonBinaryReader(docBuffer);
                BsonDocument document = codec.decode(reader, DecoderContext.builder().build());

                Entry entry = bsonDocumentToEntry(document);
                entries.add(entry);

                offset += size;
            }
            logger.info("Successfully read {} entries from BSON binary file: {}", entries.size(), inputFile);
        } catch (IOException e) {
            logger.error("Failed to read entries from BSON file: {}", inputFile, e);
            throw new RuntimeException("Failed to read entries from BSON file", e);
        } catch (Exception e) {
            logger.error("Error decoding BSON entries from file: {}", inputFile, e);
            throw new RuntimeException("Error decoding BSON entries", e);
        }
        return entries;
    }

    /**
     * Convert BSON Document to Entry object
     */
    private Entry bsonDocumentToEntry(BsonDocument document) {
        BsonDocument personDoc = document.getDocument("person");
        String genderStr = getBsonString(personDoc, "gender");
        String maritalStatusStr = getBsonString(personDoc, "maritalStatus");

        Person person = new Person(
            getBsonString(personDoc, "firstName"),
            getBsonString(personDoc, "lastName"),
            personDoc.getInt32("age").getValue(),
            genderStr != null && !genderStr.isEmpty() ? Gender.valueOf(genderStr) : null,
            maritalStatusStr != null && !maritalStatusStr.isEmpty() ? MaritalStatus.valueOf(maritalStatusStr) : null
        );

        BsonDocument addressDoc = document.getDocument("address");
        Address address = new Address(
            getBsonString(addressDoc, "street"),
            getBsonString(addressDoc, "city"),
            getBsonString(addressDoc, "state"),
            getBsonString(addressDoc, "zip"),
            getBsonString(addressDoc, "email"),
            getBsonString(addressDoc, "phone")
        );

        return new Entry(
            document.getInt32("entryId").getValue(),
            person,
            address,
            getBsonString(document, "notes")
        );
    }

    /**
     * Safely get a string value from a BSON document, handling null values
     */
    private String getBsonString(BsonDocument doc, String key) {
        if (doc.containsKey(key) && !doc.get(key).isNull()) {
            return doc.getString(key).getValue();
        }
        return null;
    }

    /**
     * Main method: Read JSON file and write to BSON binary format
     */
    public static void main(String[] args) {
        String testFile = "export-data.json";
        String outputFile = OUT_FILE_NAME;

        try {
            // Read entries from JSON file
            com.glenn.address.mongo.FileDataUtil fileUtil = new com.glenn.address.mongo.FileDataUtil(testFile);
            List<Entry> entries = fileUtil.readData();

            if (entries == null || entries.isEmpty()) {
                logger.error("No entries found in {}", testFile);
                System.exit(1);
            }

            // Write entries to BSON binary file
            BinaryService binaryService = new BsonService();
            binaryService.writeEntries(entries, outputFile);

            logger.info("Successfully converted {} entries from JSON to BSON binary format", entries.size());
            logger.info("Output file: {}", new File(outputFile).getAbsolutePath());

            // Verify by reading input-data.bson and comparing to entries written to output-data.bson
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
            logger.error("Error during BSON conversion: ", e);
            System.exit(1);
        }
    }
}
