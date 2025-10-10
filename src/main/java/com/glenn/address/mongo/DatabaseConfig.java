package com.glenn.address.mongo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String PROPERTIES_FILE = "database.properties";

    private final String host;
    private final int port;
    private final String database;
    private final String collection;

    public DatabaseConfig() {
        Properties props = new Properties();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                logger.error("Unable to find {}", PROPERTIES_FILE);
                throw new RuntimeException("Unable to find " + PROPERTIES_FILE);
            }

            props.load(input);

            this.host = props.getProperty("mongodb.host", "localhost");
            this.port = Integer.parseInt(props.getProperty("mongodb.port", "27017"));
            this.database = props.getProperty("mongodb.database", "mongo1j");
            this.collection = props.getProperty("mongodb.collection", "entries");

            logger.info("Database configuration loaded: host={}, port={}, database={}, collection={}",
                       host, port, database, collection);
        } catch (IOException e) {
            logger.error("Failed to load database configuration", e);
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getCollection() {
        return collection;
    }

    public String getConnectionString() {
        return String.format("mongodb://%s:%d", host, port);
    }
}
