package com.glenn.address;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the Address Book application.
 * Bootstraps the Spring Boot application and initializes all configured components.
 */
@SpringBootApplication
public class AddressApplication {

	public static void main(String[] args) {
		SpringApplication.run(AddressApplication.class, args);
	}

}
