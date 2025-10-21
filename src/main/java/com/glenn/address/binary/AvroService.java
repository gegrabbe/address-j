package com.glenn.address.binary;

import com.glenn.address.domain.Address;
import com.glenn.address.domain.Entry;
import com.glenn.address.domain.Gender;
import com.glenn.address.domain.MaritalStatus;
import com.glenn.address.domain.Person;
import com.glenn.address.mongo.FileDataUtil;
import com.google.gson.Gson;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AvroService implements BinaryService {
    private static final Logger logger = LoggerFactory.getLogger(AvroService.class);
    private static final String OUT_FILE_NAME = "output-data.avro";
    private static final String IN_FILE_NAME = "input-data.avro";
    private static final String SCHEMA_FILE = "entry-schema.avsc";

    private final Schema schema;

    public AvroService() {
        this.schema = loadSchema();
    }

    /**
     * Load Avro schema from resource file
     */
    private Schema loadSchema() {
        try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(SCHEMA_FILE)) {
            if (schemaStream == null) {
                throw new RuntimeException("Schema file not found: " + SCHEMA_FILE);
            }
            String schemaJson = new String(schemaStream.readAllBytes(), StandardCharsets.UTF_8);
            return new Schema.Parser().parse(schemaJson);
        } catch (IOException e) {
            logger.error("Failed to load Avro schema", e);
            throw new RuntimeException("Failed to load Avro schema", e);
        }
    }

    /**
     * Write Entry objects to Avro binary format
     *
     * @param entries    List of Entry objects to serialize
     * @param outputFile Output file path
     */
    @Override
    public void writeEntries(List<Entry> entries, String outputFile) {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            Encoder encoder = EncoderFactory.get().binaryEncoder(fos, null);
            DatumWriter<GenericRecord> datumWriter = new org.apache.avro.generic.GenericDatumWriter<>(schema);

            for (Entry entry : entries) {
                GenericRecord avroRecord = entryToAvroRecord(entry);
                datumWriter.write(avroRecord, encoder);
            }
            encoder.flush();
            logger.info("Successfully wrote {} entries to Avro binary file: {}", entries.size(), outputFile);
        } catch (IOException e) {
            logger.error("Failed to write entries to Avro file: {}", outputFile, e);
            throw new RuntimeException("Failed to write entries to Avro file", e);
        }
    }

    /**
     * Convert Entry object to Avro GenericRecord
     */
    private GenericRecord entryToAvroRecord(Entry entry) {
        GenericRecord record = new GenericData.Record(schema);

        record.put("entryId", entry.entryId());
        record.put("notes", entry.notes());

        // Convert Person
        Schema personSchema = schema.getField("person").schema();
        GenericRecord personRecord = new GenericData.Record(personSchema);
        personRecord.put("firstName", entry.person().firstName());
        personRecord.put("lastName", entry.person().lastName());
        personRecord.put("age", entry.person().age());
        personRecord.put("gender", entry.person().gender() != null ? entry.person().gender().toString() : null);
        personRecord.put("maritalStatus", entry.person().maritalStatus() != null ? entry.person().maritalStatus().toString() : null);
        record.put("person", personRecord);

        // Convert Address
        Schema addressSchema = schema.getField("address").schema();
        GenericRecord addressRecord = new GenericData.Record(addressSchema);
        addressRecord.put("street", entry.address().street());
        addressRecord.put("city", entry.address().city());
        addressRecord.put("state", entry.address().state());
        addressRecord.put("zip", entry.address().zip());
        addressRecord.put("email", entry.address().email());
        addressRecord.put("phone", entry.address().phone());
        record.put("address", addressRecord);

        return record;
    }

    /**
     * Serialize JSON string to Avro binary format
     * Converts JSON string to a List<Entry> and uses writeEntries to avoid duplication
     */
    @Override
    public void writeString(String jsonString) {
        logger.debug("Writing JSON string to Avro format");
        try {
            Gson gson = new Gson();
            Entry[] entriesArray = gson.fromJson(jsonString, Entry[].class);
            List<Entry> entries = java.util.Arrays.asList(entriesArray);

            if (entries.isEmpty()) {
                logger.warn("No entries found in JSON string");
            }

            writeEntries(entries, OUT_FILE_NAME);
        } catch (Exception e) {
            logger.error("Failed to write JSON string to Avro format", e);
            throw new RuntimeException("Failed to write JSON string to Avro format", e);
        }
    }

    /**
     * Read entries from Avro binary file
     *
     * @param inputFile Path to the Avro binary file
     * @return List of Entry objects read from the file
     */
    @Override
    public List<Entry> readEntries(String inputFile) {
        List<Entry> entries = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            Decoder decoder = DecoderFactory.get().binaryDecoder(fis, null);
            DatumReader<GenericRecord> datumReader = new org.apache.avro.generic.GenericDatumReader<>(schema);

            GenericRecord avroRecord;
            while (true) {
                try {
                    avroRecord = datumReader.read(null, decoder);
                    Entry entry = avroRecordToEntry(avroRecord);
                    entries.add(entry);
                } catch (java.io.EOFException e) {
                    // End of file reached
                    break;
                }
            }
            logger.info("Successfully read {} entries from Avro binary file: {}", entries.size(), inputFile);
        } catch (IOException e) {
            logger.error("Failed to read entries from Avro file: {}", inputFile, e);
            throw new RuntimeException("Failed to read entries from Avro file", e);
        }
        return entries;
    }

    /**
     * Convert Avro GenericRecord to Entry object
     */
    private Entry avroRecordToEntry(GenericRecord record) {
        GenericRecord personRecord = (GenericRecord) record.get("person");
        String genderStr = toString(personRecord.get("gender"));
        String maritalStatusStr = toString(personRecord.get("maritalStatus"));

        Person person = new Person(
            toString(personRecord.get("firstName")),
            toString(personRecord.get("lastName")),
            (Integer) personRecord.get("age"),
            genderStr != null ? Gender.valueOf(genderStr) : null,
            maritalStatusStr != null ? MaritalStatus.valueOf(maritalStatusStr) : null
        );

        GenericRecord addressRecord = (GenericRecord) record.get("address");
        Address address = new Address(
            toString(addressRecord.get("street")),
            toString(addressRecord.get("city")),
            toString(addressRecord.get("state")),
            toString(addressRecord.get("zip")),
            toString(addressRecord.get("email")),
            toString(addressRecord.get("phone"))
        );

        return new Entry(
            (Integer) record.get("entryId"),
            person,
            address,
            toString(record.get("notes"))
        );
    }

    /**
     * Convert Avro Utf8 or String to String
     */
    private String toString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Main method: Read JSON file and write to Avro binary format
     */
    public static void main(String[] args) {
        String testFile = "export-data.json";
        String outputFile = OUT_FILE_NAME;

        try {
            // Read entries from JSON file
            FileDataUtil fileUtil = new FileDataUtil(testFile);
            List<Entry> entries = fileUtil.readData();

            if (entries == null || entries.isEmpty()) {
                logger.error("No entries found in {}", testFile);
                System.exit(1);
            }

            // Write entries to Avro binary file
            BinaryService binaryService = new AvroService();
            binaryService.writeEntries(entries, outputFile);

            logger.info("Successfully converted {} entries from JSON to Avro binary format", entries.size());
            logger.info("Output file: {}", new File(outputFile).getAbsolutePath());

            // Verify by reading input-data.bin and comparing to entries written to output-data.bin
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
            logger.error("Error during Avro conversion: ", e);
            System.exit(1);
        }
    }
}
