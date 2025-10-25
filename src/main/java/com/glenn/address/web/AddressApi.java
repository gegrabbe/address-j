package com.glenn.address.web;

import com.glenn.address.domain.CompareById;
import com.glenn.address.domain.CompareByLastName;
import com.glenn.address.domain.Entry;
import com.glenn.address.mongo.FileDataUtil;
import com.glenn.address.mongo.MongoService;
import com.glenn.address.mongo.NextEntryId;
import com.mongodb.MongoWriteException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

/**
 * REST API controller for address book entry management.
 * Provides endpoints for CRUD operations and searching address entries through HTTP.
 * Handles request routing and response formatting for the address book API.
 */
@RestController
@RequestMapping("/api/entries")
public class AddressApi {
    private static final Logger logger = LoggerFactory.getLogger(AddressApi.class);
    public static final String DATABASE_ERROR = "Database Error";
    public static final String EXPORT_DATA_FILE = "export-data.json";
    public static final String IMPORT_DATA_FILE = "import-data.json";
    private final MongoService mongoService;

    @SuppressWarnings("unused")
    @Autowired
    public AddressApi(MongoService mongoService) {
        this.mongoService = mongoService;
    }

    public static List<Entry> sortById(List<Entry> entries) {
        entries.sort(new CompareById());
        return entries;
    }

    public static List<Entry> sortByLastName(List<Entry> entries) {
        entries.sort(new CompareByLastName());
        return entries;
    }

    @GetMapping
    @SuppressWarnings("unused")
    public ResponseEntity<?> getAllEntries() {
        logger.debug("#### getAllEntries ####");
        try {
            return ResponseEntity.ok(mongoService.readFromDatabase());
        } catch (Exception e) {
            logger.error("Failed to retrieve all entries", e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/sortById")
    @SuppressWarnings("unused")
    public ResponseEntity<?> getAllEntriesSortedById() {
        logger.debug("#### getAllEntriesSortedById ####");
        try {
            return ResponseEntity.ok(sortById(mongoService.readFromDatabase()));
        } catch (Exception e) {
            logger.error("Failed to retrieve and sort entries by ID", e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/sortByLastName")
    @SuppressWarnings("unused")
    public ResponseEntity<?> getAllEntriesSortedByLastName() {
        logger.debug("#### getAllEntriesSortedByLastName ####");
        try {
            return ResponseEntity.ok(sortByLastName(mongoService.readFromDatabase()));
        } catch (Exception e) {
            logger.error("Failed to retrieve and sort entries by last name", e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{entryId}")
    @SuppressWarnings("unused")
    public ResponseEntity<?> getEntryById(@PathVariable Integer entryId) {
        logger.debug("#### getEntryById ####");
        try {
            List<Entry> entries = mongoService.searchByEntryId(entryId);
            if (entries.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            logger.error("Failed to retrieve entry by id: {}", entryId, e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/search/lastName/{lastName}")
    @SuppressWarnings("unused")
    public ResponseEntity<?> searchByLastName(@PathVariable String lastName) {
        logger.debug("#### searchByLastName ####");
        try {
            return ResponseEntity.ok(mongoService.searchByLastName(lastName));
        } catch (Exception e) {
            logger.error("Failed to search by lastName: {}", lastName, e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/search/name/{firstName}/{lastName}")
    @SuppressWarnings("unused")
    public ResponseEntity<?> searchByFirstAndLastName(@PathVariable String firstName,
                                                      @PathVariable String lastName) {
        logger.debug("#### searchByFirstAndLastName ####");
        try {
            return ResponseEntity.ok(mongoService.searchByFirstAndLastName(firstName, lastName));
        } catch (Exception e) {
            logger.error("Failed to search by firstName: {} and lastName: {}", firstName, lastName, e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/saveList")
    @SuppressWarnings("unused")
    public ResponseEntity<?> saveEntries(@RequestBody List<Entry> entries) {
        logger.debug("#### saveEntries ####");
        try {
            mongoService.saveToDatabase(entries);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (MongoWriteException we) {
            String msg = duplicateMsg(we);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, msg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            logger.error("Failed to create entries", e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/save")
    @SuppressWarnings("unused")
    public ResponseEntity<?> saveOneEntry(@RequestBody Entry entry) {
        logger.debug("#### saveOneEntry ####");
        try {
            mongoService.saveEntryToDatabase(entry);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (MongoWriteException we) {
            String msg = duplicateMsg(we);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, msg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            logger.error("Failed to save entry - unexpected error", e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/{entryId}")
    @SuppressWarnings("unused")
    public ResponseEntity<?> deleteEntryById(@PathVariable Integer entryId) {
        logger.debug("#### deleteEntryById ####");
        try {
            mongoService.deleteEntryById(entryId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Failed to delete entry by id: {}", entryId, e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/export")
    @SuppressWarnings("unused")
    public ResponseEntity<?> export(@RequestParam(required = false, defaultValue = EXPORT_DATA_FILE) String fileName) {
        logger.debug("#### export ####");
        try {
            ResponseEntity<?> responseEntity = fileNameCheck(fileName, false);
            if (responseEntity != null) {
                return responseEntity;
            }
            new FileDataUtil(fileName).writeData(sortById(mongoService.readFromDatabase()));
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            logger.error("Failed to export - unexpected error", e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/importData")
    @SuppressWarnings("unused")
    public ResponseEntity<?> importData(@RequestParam(required = false, defaultValue = IMPORT_DATA_FILE) String fileName) {
        logger.debug("#### importData ####");
        try {
            ResponseEntity<?> responseEntity = fileNameCheck(fileName, true);
            if (responseEntity != null) {
                return responseEntity;
            }
            mongoService.saveToDatabase(fixNewEntryIds(new FileDataUtil(fileName).readData(),
                    new NextEntryId(mongoService)));
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (MongoWriteException we) {
            String msg = duplicateMsg(we);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, msg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            logger.error("Failed to import - unexpected error", e);
            ErrorResponse errorResponse = new ErrorResponse(DATABASE_ERROR, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private List<Entry> fixNewEntryIds(List<Entry> newEntries, NextEntryId nextEntryId) {
        return newEntries.stream()
                .map(entry -> new Entry(nextEntryId.next(), entry.person(), entry.address(), entry.notes()))
                .toList();
    }

    private ResponseEntity<?> fileNameCheck(String fileName, boolean mustExist) {
        if (fileName.startsWith("/")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid File Name", "File name cannot begin with /"));
        }
        if (fileName.contains(":")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid File Name", "File name cannot contain :"));
        }
        if (!fileName.endsWith(".json")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid File Name", "File name must end with .json"));
        }
        if (mustExist && !(new File(fileName).canRead())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid File Name", "File does not exist"));
        }
        return null;
    }

    private String duplicateMsg(MongoWriteException we) {
        logger.error("Failed to save entry - duplicate key", we);
        String msg = we.getMessage();
        if (we.getMessage().contains("duplicate key error")) {
            msg = "Duplicate Entry ID Exists";
        }
        return msg;
    }

    @PreDestroy
    @SuppressWarnings("unused")
    public void cleanup() {
        logger.info("Closing MongoDB connection");
        mongoService.close();
    }

}
