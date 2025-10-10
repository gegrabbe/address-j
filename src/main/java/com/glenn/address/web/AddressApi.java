package com.glenn.address.web;

import com.glenn.address.domain.Entry;
import com.glenn.address.mongo.MongoService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entries")
public class AddressApi {
    private static final Logger logger = LoggerFactory.getLogger(AddressApi.class);
    private final MongoService mongoService;

    public AddressApi() {
        this.mongoService = new MongoService("input-data.json");
    }

    @GetMapping
    public ResponseEntity<List<Entry>> getAllEntries() {
        logger.debug("#### getAllEntries ####");
        try {
            List<Entry> entries = mongoService.readFromDatabase();
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            logger.error("Failed to retrieve all entries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{entryId}")
    public ResponseEntity<List<Entry>> getEntryById(@PathVariable String entryId) {
        logger.debug("#### getEntryById ####");
        try {
            List<Entry> entries = mongoService.searchByEntryId(entryId);
            if (entries.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            logger.error("Failed to retrieve entry by id: {}", entryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search/lastName/{lastName}")
    public ResponseEntity<List<Entry>> searchByLastName(@PathVariable String lastName) {
        logger.debug("#### searchByLastName ####");
        try {
            List<Entry> entries = mongoService.searchByLastName(lastName);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            logger.error("Failed to search by lastName: {}", lastName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search/name/{firstName}/{lastName}")
    public ResponseEntity<List<Entry>> searchByFirstAndLastName(@PathVariable String firstName,
                                                                @PathVariable String lastName) {
        logger.debug("#### searchByFirstAndLastName ####");
        try {
            List<Entry> entries = mongoService.searchByFirstAndLastName(firstName, lastName);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            logger.error("Failed to search by firstName: {} and lastName: {}", firstName, lastName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/saveList")
    public ResponseEntity<Void> saveEntries(@RequestBody List<Entry> entries) {
        logger.debug("#### saveEntries ####");
        try {
            mongoService.saveToDatabase(entries);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            logger.error("Failed to create entries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Void> saveOneEntry(@RequestBody Entry entry) {
        logger.debug("#### saveOneEntry ####");
        try {
            mongoService.saveEntryToDatabase(entry);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            logger.error("Failed to create entries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Closing MongoDB connection");
        mongoService.close();
    }

}
