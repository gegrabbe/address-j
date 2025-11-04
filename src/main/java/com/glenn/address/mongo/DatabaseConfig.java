package com.glenn.address.mongo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Manages MongoDB connection configuration loaded from application.properties.
 * Provides access to MongoDB host, port, database name, and collection name settings.
 * Configuration values are injected from Spring application properties.
 */
@Component
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    private String host = "localhost";
    private int port = 27017;
    private String database = "mongo1j";
    private String collection = "entries";

    public DatabaseConfig() {
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void setCollection(String collection) {
        this.collection = collection;
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
