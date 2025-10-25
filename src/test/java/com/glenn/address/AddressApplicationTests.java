package com.glenn.address;

import com.glenn.address.domain.*;
import com.glenn.address.mongo.MongoService;
import com.glenn.address.web.AddressApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static com.glenn.address.TestConfig.JOHN_SMITH;

/**
 * Test class for verifying Spring Boot application context loads successfully.
 * Validates that all Spring configuration and component initialization works correctly.
 * Disables MongoDB auto-configuration and uses TestConfig to provide a mocked MongoService.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})
@Import(TestConfig.class)
class AddressApplicationTests {

    @Autowired
    MongoService mongoService;
    AddressApi api;

    @BeforeEach
    void init() {
        api = new AddressApi(mongoService);
    }

    @Test
    void contextLoads() {
        // Spring context loaded successfully without MongoDB connection
        assertNotNull(mongoService);
    }

    @Test
    void testGetAllEntries() {
        ResponseEntity<?> response = api.getAllEntries();
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Get the body as Object, then cast to List<Entry>
        @SuppressWarnings("unchecked")
        List<Entry> body = (List<Entry>) response.getBody();
        assertNotNull(body);
        assertEquals(TestConfig.testEntries(), body);
    }

    @Test
    void testGetAllEntriesSortedById() {
        ResponseEntity<?> response = api.getAllEntriesSortedById();
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        List<Entry> body = (List<Entry>) response.getBody();
        assertNotNull(body);
        assertFalse(body.isEmpty());
        // Verify entries are sorted by ID
        int size = body.size();
        for (int i = 0; i < size - 1; i++) {
            assertTrue(body.get(i).entryId() <= body.get(i + 1).entryId());
        }
    }

    @Test
    void testGetAllEntriesSortedByLastName() {
        ResponseEntity<?> response = api.getAllEntriesSortedByLastName();
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        List<Entry> body = (List<Entry>) response.getBody();
        assertNotNull(body);
        assertFalse(body.isEmpty());
        // Verify entries are sorted by last name
        int size = body.size();
        for (int i = 0; i < size - 1; i++) {
            assertTrue(body.get(i).person().lastName()
                    .compareTo(body.get(i + 1).person().lastName()) <= 0);
        }
    }

    @Test
    void testGetEntryById() {
        int entryId = JOHN_SMITH.entryId();
        ResponseEntity<?> response = api.getEntryById(entryId);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        List<Entry> body = (List<Entry>) response.getBody();
        assertNotNull(body);
        assertFalse(body.isEmpty());
        // Verify all returned entries have the requested ID
        for (Entry entry : body) {
            assertEquals(entryId, entry.entryId());
        }
    }

    @Test
    void testSearchByLastName() {
        String lastName = JOHN_SMITH.person().lastName();
        ResponseEntity<?> response = api.searchByLastName(lastName);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        List<Entry> body = (List<Entry>) response.getBody();
        assertNotNull(body);
        // Verify all returned entries match the last name (if not empty)
        for (Entry entry : body) {
            assertEquals(lastName, entry.person().lastName());
        }
    }

    @Test
    void testSearchByFirstAndLastName() {
        String firstName = JOHN_SMITH.person().firstName();
        String lastName = JOHN_SMITH.person().lastName();
        ResponseEntity<?> response = api.searchByFirstAndLastName(firstName, lastName);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        List<Entry> body = (List<Entry>) response.getBody();
        assertNotNull(body);
        // Verify all returned entries match both names (if not empty)
        for (Entry entry : body) {
            assertEquals(firstName, entry.person().firstName());
            assertEquals(lastName, entry.person().lastName());
        }
    }

    @Test
    void testSaveEntries() {
        List<Entry> entriesToSave = TestConfig.testEntries();
        ResponseEntity<?> response = api.saveEntries(entriesToSave);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void testSaveOneEntry() {
        Entry entryToSave = new Entry(
                999,
                new Person("Test", "User", 30, Gender.MALE, MaritalStatus.SINGLE),
                new Address("123 Main St", "TestCity", "TestState", "12345", "test@example.com", "555-1234"),
                "Test notes"
        );
        ResponseEntity<?> response = api.saveOneEntry(entryToSave);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void testDeleteEntryById() {
        int entryId = 1;
        ResponseEntity<?> response = api.deleteEntryById(entryId);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testImportData() {
        ResponseEntity<?> response = api.importData("unit-test-import-data.json");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testExport() {
        String fileName = "unit-test-export-data.json";
        ResponseEntity<?> response = api.export(fileName);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify the file was created
        File exportedFile = new File(fileName);
        assertTrue(exportedFile.exists(), "Exported file should exist");

        // Clean up - delete the test export file
        assertTrue(exportedFile.delete(), "Failed to delete test export file");
    }

}
