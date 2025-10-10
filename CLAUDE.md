# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.6 application using Java 21 and MongoDB for managing address book entries. The project uses Gradle for build management and follows a standard Spring Boot application structure.

## Build and Test Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests AddressApplicationTests

# Run the application
./gradlew bootRun

# Clean build artifacts
./gradlew clean
```

## Architecture

### Layer Structure

The application follows a standard layered architecture:

- **Domain Layer** (`com.glenn.address.domain`): Contains domain models as Java records
  - `Entry`: Top-level record containing entryId, Person, Address, and notes
  - `Person`: Personal information (firstName, lastName, age, Gender, MaritalStatus)
  - `Address`: Address details
  - `Gender` and `MaritalStatus`: Enums for person attributes

- **Data Access Layer** (`com.glenn.address.mongo`): MongoDB integration
  - `MongoService`: Core service for all MongoDB operations (implements AutoCloseable)
  - `DatabaseConfig`: Loads MongoDB configuration from `database.properties`
  - `FileDataUtil`: Utility for reading/writing Entry lists to/from JSON files
  - `TestData`: Test data generation

- **Web Layer** (`com.glenn.address.web`): REST API endpoints
  - `AddressApi`: API controller (currently minimal implementation)

### MongoDB Integration

The application uses direct MongoDB Java driver (not Spring Data MongoDB repositories). Key patterns:

- Connection management is handled via `MongoClient` in `MongoService`
- `MongoService` implements AutoCloseable for proper resource management
- Configuration is loaded from `src/main/resources/database.properties`
- Default connection: `mongodb://localhost:27017`, database: `mongo1j`, collection: `entries`

### Data Serialization

- Uses Gson for JSON serialization/deserialization
- Entries are converted to MongoDB Documents via JSON intermediary
- MongoDB's `_id` field is removed when reading from database to maintain clean Entry objects

### Search Capabilities

`MongoService` provides multiple search methods:
- `searchByEntryId(String entryId)`
- `searchByLastName(String lastName)`
- `searchByFirstAndLastName(String firstName, String lastName)`

All search methods return `List<Entry>` and handle MongoException gracefully.

## Configuration

- MongoDB settings: `src/main/resources/database.properties`
- Spring configuration: `src/main/resources/application.properties`
- Logging: `src/main/resources/log4j2.xml`

## Dependencies

Key dependencies in `build.gradle`:
- Spring Boot Web
- Spring Data MongoDB (for session management)
- MongoDB Java Driver (direct usage in MongoService)
- Gson for JSON processing
- Apache Commons Lang3 for utilities

## Running MongoService Standalone

The `MongoService` class has a main method for testing:
```bash
# Compile and run MongoService directly
./gradlew compileJava
java -cp build/classes/java/main:$(./gradlew -q printClasspath) com.glenn.address.mongo.MongoService
```

Note: This expects `input-data.json` file to be present.
