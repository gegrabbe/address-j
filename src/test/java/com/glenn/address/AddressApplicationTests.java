package com.glenn.address;

import com.glenn.address.mongo.MongoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Assertions;

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

    @Test
    void contextLoads() {
        // Spring context loaded successfully without MongoDB connection
        Assertions.assertNotNull(mongoService);
    }

}
