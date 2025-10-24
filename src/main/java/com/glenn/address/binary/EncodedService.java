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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Implementation of BinaryService for reading/writing entries in Avro format with three-layer obfuscation.
 * Applies multi-character cipher (ROT13 for letters, ROT5 for digits, character swaps) followed by
 * Base64 encoding, then Avro binary serialization for maximum obfuscation of sensitive data.
 * Data flow:
 * - Writing: Original String → ROT13/ROT5/Swap → Base64 → Avro Binary → .addr file
 * - Reading: .addr file → Avro Binary → Base64 Decode → ROT13/ROT5/Swap → Original String
 * Uses .addr file extension to further obscure that it contains encoded address book data.
 * Provides effective security-through-obscurity for casual protection of personal data.
 */
public class EncodedService implements BinaryService {
    private static final Logger logger = LoggerFactory.getLogger(EncodedService.class);
    private static final String OUT_FILE_NAME = "output-data.addr";
    private static final String IN_FILE_NAME = "input-data.addr";
    private static final String SCHEMA_FILE = "entry-schema.avsc";

    private final Schema schema;

    public EncodedService() {
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

    private String rot(String value) {
        return Rot13.obfuscate(value);
    }

    private String derot(String value) {
        return Rot13.clarify(value);
    }

    private String encode64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode64(String value) {
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to decode Base64 string: {}", value, e);
            return value;
        }
    }

    /**
     * Encodes a string using multi-character cipher followed by Base64 encoding.
     * Applies ROT13/ROT5/Swap first, then Base64 for maximum obfuscation.
     *
     * @param value The string to encode.
     * @return The doubly-encoded string (cipher + Base64), or null if input is null.
     */
    private String encodeString(String value) {
        if (value == null) {
            return null;
        }
        return rot(encode64(value));
    }

    /**
     * Decodes a string by reversing Base64 decoding followed by multi-character cipher.
     * Reverses the encoding process: Base64 decode first, then ROT13/ROT5/Swap deobfuscation.
     *
     * @param value The doubly-encoded string to decode.
     * @return The original unencoded string, or null if input is null.
     */
    private String decodeString(String value) {
        if (value == null) {
            return null;
        }
        return decode64(derot(value));
    }

    /**
     * Writes Entry objects to Avro binary format with three-layer obfuscation.
     * All string fields are obfuscated using ROT13/ROT5/Swap cipher followed by Base64 encoding
     * before being stored in Avro binary format.
     *
     * @param entries    List of Entry objects to serialize
     * @param outputFile Output file path (.addr extension recommended)
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
            logger.info("Successfully wrote {} entries to Avro encoded file: {}", entries.size(), outputFile);
        } catch (IOException e) {
            logger.error("Failed to write entries to Avro encoded file: {}", outputFile, e);
            throw new RuntimeException("Failed to write entries to Avro encoded file", e);
        }
    }

    /**
     * Convert Entry object to Avro GenericRecord with Base64-encoded strings
     */
    private GenericRecord entryToAvroRecord(Entry entry) {
        GenericRecord record = new GenericData.Record(schema);

        record.put("entryId", entry.entryId());
        record.put("notes", encodeString(entry.notes()));

        // Convert Person with Base64 encoding
        Schema personSchema = schema.getField("person").schema();
        GenericRecord personRecord = new GenericData.Record(personSchema);
        personRecord.put("firstName", encodeString(entry.person().firstName()));
        personRecord.put("lastName", encodeString(entry.person().lastName()));
        personRecord.put("age", entry.person().age());
        personRecord.put("gender", entry.person().gender() != null ? encodeString(entry.person().gender().toString()) : null);
        personRecord.put("maritalStatus", entry.person().maritalStatus() != null ? encodeString(entry.person().maritalStatus().toString()) : null);
        record.put("person", personRecord);

        // Convert Address with Base64 encoding
        Schema addressSchema = schema.getField("address").schema();
        GenericRecord addressRecord = new GenericData.Record(addressSchema);
        addressRecord.put("street", encodeString(entry.address().street()));
        addressRecord.put("city", encodeString(entry.address().city()));
        addressRecord.put("state", encodeString(entry.address().state()));
        addressRecord.put("zip", encodeString(entry.address().zip()));
        addressRecord.put("email", encodeString(entry.address().email()));
        addressRecord.put("phone", encodeString(entry.address().phone()));
        record.put("address", addressRecord);

        return record;
    }

    /**
     * Serializes a JSON string to Avro encoded binary format with three-layer obfuscation.
     * Converts JSON string to Entry objects and applies three-layer obfuscation (ROT13/ROT5/Swap + Base64 + Avro).
     * Delegates to writeEntries() to avoid code duplication.
     */
    @Override
    public void writeString(String jsonString) {
        logger.debug("Writing JSON string to Avro encoded format");
        try {
            Gson gson = new Gson();
            Entry[] entriesArray = gson.fromJson(jsonString, Entry[].class);
            List<Entry> entries = Arrays.asList(entriesArray);

            if (entries.isEmpty()) {
                logger.warn("No entries found in JSON string");
            }

            writeEntries(entries, OUT_FILE_NAME);
        } catch (Exception e) {
            logger.error("Failed to write JSON string to Avro encoded format", e);
            throw new RuntimeException("Failed to write JSON string to Avro encoded format", e);
        }
    }

    /**
     * Reads entries from Avro encoded binary file with automatic deobfuscation.
     * Automatically reverses the three-layer obfuscation (Avro → Base64 decode → ROT13/ROT5/Swap deobfuscate)
     * to restore original Entry objects with plaintext strings.
     *
     * @param inputFile Path to the Avro encoded binary file (.addr format)
     * @return List of Entry objects with deobfuscated plaintext strings
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
            logger.info("Successfully read {} entries from Avro encoded binary file: {}", entries.size(), inputFile);
        } catch (IOException e) {
            logger.error("Failed to read entries from Avro encoded file: {}", inputFile, e);
            throw new RuntimeException("Failed to read entries from Avro encoded file", e);
        }
        return entries;
    }

    /**
     * Convert Avro GenericRecord to Entry object, decoding Base64 strings
     */
    private Entry avroRecordToEntry(GenericRecord record) {
        GenericRecord personRecord = (GenericRecord) record.get("person");
        String genderStr = decodeString(toString(personRecord.get("gender")));
        String maritalStatusStr = decodeString(toString(personRecord.get("maritalStatus")));

        Person person = new Person(
            decodeString(toString(personRecord.get("firstName"))),
            decodeString(toString(personRecord.get("lastName"))),
            (Integer) personRecord.get("age"),
            genderStr != null ? Gender.valueOf(genderStr) : null,
            maritalStatusStr != null ? MaritalStatus.valueOf(maritalStatusStr) : null
        );

        GenericRecord addressRecord = (GenericRecord) record.get("address");
        Address address = new Address(
            decodeString(toString(addressRecord.get("street"))),
            decodeString(toString(addressRecord.get("city"))),
            decodeString(toString(addressRecord.get("state"))),
            decodeString(toString(addressRecord.get("zip"))),
            decodeString(toString(addressRecord.get("email"))),
            decodeString(toString(addressRecord.get("phone")))
        );

        return new Entry(
            (Integer) record.get("entryId"),
            person,
            address,
            decodeString(toString(record.get("notes")))
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
     * Main method: Read JSON file and write to Avro encoded binary format
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

            // Write entries to Avro encoded file
            BinaryService binaryService = new EncodedService();
            binaryService.writeEntries(entries, outputFile);

            logger.info("Successfully converted {} entries from JSON to Avro encoded format", entries.size());
            logger.info("Output file: {}", new File(outputFile).getAbsolutePath());

            // Delete input file and copy output to input for next run
            FileDataUtil fileUtil2 = new FileDataUtil(IN_FILE_NAME);
            if (fileUtil2.delete(IN_FILE_NAME)) {
                logger.info("Deleted old input file: {}", IN_FILE_NAME);
            }
            if (fileUtil2.copy(outputFile, IN_FILE_NAME)) {
                logger.info("Copied output file to input file: {} -> {}", outputFile, IN_FILE_NAME);
            } else {
                throw new RuntimeException("copy failed. cannot proceed");
            }

            // Verify by reading input-data.abook and comparing to entries written to output-data.abook
            List<Entry> readEntries = binaryService.readEntries(IN_FILE_NAME);
            logger.info("\nRead {} entries from " + IN_FILE_NAME, readEntries.size());

            if (entries.equals(readEntries)) {
                logger.info("✓ Verification successful: Entries match between output and input files");
                logger.info("Read in data: {}", (readEntries.toString()).substring(0,200));
            } else {
                logger.error("✗ Verification failed: Entries do not match");
                logger.error("  Written entries: {}", entries.size());
                logger.error("  Read entries: {}", readEntries.size());
            }

        } catch (Exception e) {
            logger.error("Error during Avro encoded conversion: ", e);
            System.exit(1);
        }
    }
}
