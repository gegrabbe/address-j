package com.glenn.address.web;

import com.glenn.address.domain.CompareById;
import com.glenn.address.domain.CompareByLastName;
import com.glenn.address.domain.Entry;
import com.glenn.address.mongo.FileDataUtil;
import com.glenn.address.mongo.MongoService;
import com.mongodb.MongoWriteException;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/entries")
public class AddressApi {
    private static final Logger logger = LoggerFactory.getLogger(AddressApi.class);
    public static final String DATABASE_ERROR = "Database Error";
    public static final String EXPORT_DATA_FILE = "export-data.json";
    public static final String IMPORT_DATA_FILE = "import-data.json";
    private final MongoService mongoService;

    @SuppressWarnings("unused")
    public AddressApi() {
        this.mongoService = new MongoService("input-data.json");
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
            List<Entry> entries = mongoService.readFromDatabase();
            return ResponseEntity.ok(entries);
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
            List<Entry> entries = mongoService.searchByLastName(lastName);
            return ResponseEntity.ok(entries);
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
            List<Entry> entries = mongoService.searchByFirstAndLastName(firstName, lastName);
            return ResponseEntity.ok(entries);
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
    public ResponseEntity<?> export(@RequestParam(required = false) String fileName) {
        logger.debug("#### export ####");
        try {
            if (StringUtils.isEmpty(fileName)) {
                fileName = EXPORT_DATA_FILE;
            }
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
    public ResponseEntity<?> importData(@RequestParam(required = false) String fileName) {
        logger.debug("#### importData ####");
        try {
            if (StringUtils.isEmpty(fileName)) {
                fileName = IMPORT_DATA_FILE;
            }
            ResponseEntity<?> responseEntity = fileNameCheck(fileName, true);
            if (responseEntity != null) {
                return responseEntity;
            }
            List<Integer> oldEntriesIds = sortById(mongoService.readFromDatabase())
                    .stream().map(Entry::entryId).toList();

            int maxId = 0;
            if (!oldEntriesIds.isEmpty()) {
                maxId = oldEntriesIds.getLast();
            }

            List<Entry> newEntries = fixNewEntryIds(new FileDataUtil(fileName).readData(), maxId);
            mongoService.saveToDatabase(newEntries);
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

    private List<Entry> fixNewEntryIds(List<Entry> newEntries, Integer maxId) {
        int index = maxId;
        List<Entry> fixed = new ArrayList<>();
        for (Entry entry : newEntries) {
            index++;
            fixed.add(new Entry(index, entry.person(), entry.address(), entry.notes()));
        }
        return fixed;
    }

    private ResponseEntity<?> fileNameCheck(String fileName, boolean mustExist) {
        if (fileName.startsWith("/")) {
            ErrorResponse errorResponse = new ErrorResponse("Invalid File Name", "File name cannot begin with /");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        if (fileName.contains(":")) {
            ErrorResponse errorResponse = new ErrorResponse("Invalid File Name", "File name cannot contain :");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        if (mustExist && !(new File(fileName).canRead())) {
            ErrorResponse errorResponse = new ErrorResponse("Invalid File Name", "File does not exist");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
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
