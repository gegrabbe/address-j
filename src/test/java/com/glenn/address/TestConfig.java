package com.glenn.address;

import com.glenn.address.mongo.MongoService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test configuration that provides a mocked MongoService to prevent
 * MongoDB connection attempts during testing.
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public MongoService mongoService() {
        MongoService mockService = mock(MongoService.class);
        when(mockService.readFromDatabase()).thenReturn(java.util.List.of());
        return mockService;
    }
}
