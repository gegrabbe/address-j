package com.glenn.address;

import com.glenn.address.domain.*;
import com.glenn.address.mongo.FileDataUtil;
import com.glenn.address.mongo.MongoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration that provides a mocked MongoService to prevent
 * MongoDB connection attempts during testing.
 */
@TestConfiguration
public class TestConfig {
    private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);
    public static final List<Entry> TEST_ENTRIES = testEntries();
    public static final Entry JOHN_SMITH = testJohnSmith();

    public static List<Entry> testEntries() {
        try {
            final FileDataUtil fileUtil = new FileDataUtil("export-data.json");
            return fileUtil.readData();
        } catch (Exception e) {
            logger.error("no data", e);
        }
        return List.of();
    }

    public static Entry testJohnSmith() {
        return new Entry(
                99901,
                new Person("John", "Smith", 30, Gender.MALE, MaritalStatus.SINGLE),
                new Address("123 Main St", "Reno", "NV", "12345", "test@example.com", "888-555-1234"),
                "Test notes"
        );
    }

    @Bean
    @Primary
    public MongoService mongoService() {
        MongoService mockService = mock(MongoService.class);
        when(mockService.readFromDatabase()).thenReturn(TEST_ENTRIES);
        when(mockService.searchByEntryId(any())).thenReturn(List.of(JOHN_SMITH));
        when(mockService.searchByLastName(any())).thenReturn(List.of(JOHN_SMITH));
        when(mockService.searchByFirstAndLastName(any(), any())).thenReturn(List.of(JOHN_SMITH));
        return mockService;
    }
}
