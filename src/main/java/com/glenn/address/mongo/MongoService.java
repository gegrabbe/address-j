package com.glenn.address.mongo;

import com.glenn.address.domain.Entry;
import com.google.gson.Gson;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

/**
 * Service class for MongoDB operations on address book entries.
 * Provides CRUD operations and search functionality for entries stored in MongoDB.
 * Implements AutoCloseable for proper resource management of the MongoClient connection.
 */
@Service
public class MongoService implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(MongoService.class);
    private static final String FILE_NAME = "input-data.json";

    private String fileName;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private final Gson gson;
    private boolean initialized = false;

    public MongoService() {
        this.gson = new Gson();
    }

    /**
     * Lazily initializes the MongoDB connection on first use.
     * Ensures the connection is only established when actually needed.
     */
    private synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }

        DatabaseConfig config = new DatabaseConfig();

        try {
            this.mongoClient = MongoClients.create(config.getConnectionString());
            this.database = mongoClient.getDatabase(config.getDatabase());
            this.collection = database.getCollection(config.getCollection());

            logger.info("Connected to MongoDB at {}/{}/{}",
                    config.getConnectionString(), config.getDatabase(), config.getCollection());
            initialized = true;
        } catch (MongoException e) {
            logger.error("Failed to connect to MongoDB", e);
            throw new RuntimeException("Failed to connect to MongoDB", e);
        }
    }

    private List<Entry> getTestData() {
        FileDataUtil fdu = new FileDataUtil(fileName);
        return fdu.readData();
    }

    public void saveToDatabase(List<Entry> updates) {
        ensureInitialized();
        try {
            List<Document> documents = new ArrayList<>();
            for (Entry entry : updates) {
                String json = gson.toJson(entry);
                Document doc = Document.parse(json);
                documents.add(doc);
            }

            if (!documents.isEmpty()) {
                collection.insertMany(documents);
                logger.debug("Successfully saved {} entries to MongoDB", documents.size());
            }
        } catch (MongoException e) {
            logger.error("Failed to save entries to MongoDB", e);
            throw e;
        }
    }

    public void saveEntryToDatabase(Entry update) {
        ensureInitialized();
        try {
            String json = gson.toJson(update);
            Document doc = Document.parse(json);
            collection.insertOne(doc);
            logger.debug("Successfully saved 1 entry to MongoDB: {}", update.entryId());
        } catch (MongoException e) {
            logger.error("Failed to save entry to MongoDB", e);
            throw e;
        }
    }

    public void deleteEntryById(Integer entryId) {
        ensureInitialized();
        try {
            var result = collection.deleteMany(eq("entryId", entryId));
            logger.debug("Successfully deleted {} entries with entryId '{}'", result.getDeletedCount(), entryId);
        } catch (MongoException e) {
            logger.error("Failed to delete entries with entryId: {}", entryId, e);
            throw e;
        }
    }

    public List<Entry> readFromDatabase() {
        ensureInitialized();
        try {
            List<Entry> entries = new ArrayList<>();

            for (Document doc : collection.find()) {
                doc.remove("_id");  // Remove MongoDB's _id field
                String json = doc.toJson();
                Entry entry = gson.fromJson(json, Entry.class);
                entries.add(entry);
            }

            logger.debug("Successfully read {} entries from MongoDB", entries.size());
            return entries;
        } catch (MongoException e) {
            logger.error("Failed to read entries from MongoDB", e);
            return List.of();
        }
    }

    public List<Entry> searchByEntryId(Integer entryId) {
        ensureInitialized();
        try {
            List<Entry> entries = new ArrayList<>();

            for (Document doc : collection.find(eq("entryId", entryId))) {
                doc.remove("_id");
                String json = doc.toJson();
                Entry entry = gson.fromJson(json, Entry.class);
                entries.add(entry);
            }

            logger.debug("Found {} entries with entryId '{}'", entries.size(), entryId);
            return entries;
        } catch (MongoException e) {
            logger.error("Failed to search by entryId", e);
            return List.of();
        }
    }

    public List<Entry> searchByLastName(String lastName) {
        ensureInitialized();
        try {
            List<Entry> entries = new ArrayList<>();

            for (Document doc : collection.find(regex("person.lastName", "^" + lastName, "i"))) {
                doc.remove("_id");
                String json = doc.toJson();
                Entry entry = gson.fromJson(json, Entry.class);
                entries.add(entry);
            }

            logger.debug("Found {} entries with lastName '{}'", entries.size(), lastName);
            return entries;
        } catch (MongoException e) {
            logger.error("Failed to search by lastName", e);
            return List.of();
        }
    }

    public List<Entry> searchByFirstAndLastName(String firstName, String lastName) {
        ensureInitialized();
        try {
            List<Entry> entries = new ArrayList<>();

            for (Document doc : collection.find(and(
                    regex("person.firstName", "^" + firstName, "i"),
                    regex("person.lastName", "^" + lastName, "i")))) {
                doc.remove("_id");
                String json = doc.toJson();
                Entry entry = gson.fromJson(json, Entry.class);
                entries.add(entry);
            }

            logger.debug("Found {} entries with firstName '{}' and lastName '{}'",
                    entries.size(), firstName, lastName);
            return entries;
        } catch (MongoException e) {
            logger.error("Failed to search by firstName and lastName", e);
            return List.of();
        }
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public static void main(String[] args) {
        logger.info("### MongoService test ###");

        try (MongoService msTest = new MongoService()) {
            msTest.setFileName(FILE_NAME);
            List<Entry> updates = msTest.getTestData();
            msTest.saveToDatabase(updates);
            updates = msTest.readFromDatabase();
            logger.info("Read from database: {}", StringUtils.substring(updates.toString(), 0, 100));
            List<Entry> entries = msTest.searchByEntryId(0);
            logger.info("Searched from database: {}", entries
                    .stream()
                    .map(entry -> entry.entryId() + ":" + entry.person().firstName() + ":" + entry.person().lastName())
                    .toList());
        } catch (Exception e) {
            logger.error("MongoService test failed", e);
        }
    }

}
