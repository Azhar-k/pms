# Integration Testing Guide

This document provides instructions for running REST Assured integration tests for the Property Management System (PMS) API.

## Quick Start

### 1. Start the Application

First, ensure your application is running:

```bash
mvn spring-boot:run
```

The application should start on `http://localhost:8080`

### 2. Verify Application is Running

Check that the application is accessible:
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/api-docs

### 3. Run Tests

In a separate terminal, run the integration tests:

```bash
# Run all tests
mvn test

# Run only Guest integration tests
mvn test -Dtest=GuestControllerIntegrationTest

# Run with verbose output
mvn test -Dtest=GuestControllerIntegrationTest -X
```

## Test Coverage

### Guest Management API Tests

The `GuestControllerIntegrationTest` covers:

#### ✅ CRUD Operations
- Create guest (with validation)
- Read guest by ID and email
- Update guest information
- Delete guest

#### ✅ Pagination
- Page-based pagination
- Size configuration
- Pagination metadata (totalElements, totalPages, first, last)

#### ✅ Sorting
- Sort by lastName (ascending/descending)
- Sort by firstName
- Sort by email
- Custom sort direction

#### ✅ Filtering
- Filter by firstName
- Filter by lastName
- Filter by email
- Filter by city
- Filter by state
- Filter by country
- Filter by identificationType

#### ✅ Search
- Full-text search across multiple fields
- Case-insensitive search

#### ✅ Combined Features
- Pagination + Sorting + Filtering
- Multiple filters combined

## Test Structure

```
src/test/java/com/klm/pms/
├── config/
│   └── TestConfig.java              # Base test configuration
└── integration/
    ├── GuestControllerIntegrationTest.java  # Guest API tests
    └── README.md                    # Detailed test documentation
```

## Configuration

### Test Configuration (`TestConfig.java`)

- **Base URL**: `http://localhost:8080`
- **API Path**: `/api`
- **Content Type**: `application/json`
- **Logging**: Enabled on validation failures

### Database Requirements

- **Database**: PostgreSQL
- **Name**: `pms`
- **Host**: `localhost:5432`
- **Username**: `postgres`
- **Password**: `postgres`

## Running Specific Tests

### Run a Single Test Method

```bash
mvn test -Dtest=GuestControllerIntegrationTest#testCreateGuest_Success
```

### Run Tests by Order

Tests are ordered using `@Order` annotation:
- Order 1-5: Create operations
- Order 10-13: Read operations
- Order 20-22: Pagination tests
- Order 30-33: Sorting tests
- Order 40-45: Filtering tests
- Order 50: Search tests
- Order 60: Combined tests
- Order 70-72: Update operations
- Order 80-81: Delete operations

### Run Tests with Maven Profiles

You can create Maven profiles for different test environments:

```xml
<profiles>
    <profile>
        <id>integration-tests</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <includes>
                            <include>**/*IntegrationTest.java</include>
                        </includes>
                    </configuration>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

Then run:
```bash
mvn test -Pintegration-tests
```

## Troubleshooting

### Connection Refused Error

**Problem**: `java.net.ConnectException: Connection refused`

**Solution**: 
- Ensure the application is running on port 8080
- Check if port 8080 is available: `netstat -an | grep 8080`
- Verify the application started successfully

### Database Connection Error

**Problem**: Database connection failures

**Solution**:
- Verify PostgreSQL is running: `pg_isready`
- Check database credentials in `application.properties`
- Ensure database `pms` exists: `psql -U postgres -c "CREATE DATABASE pms;"`

### Test Failures Due to Existing Data

**Problem**: Tests fail because of duplicate emails or existing data

**Solution**:
- Clean up test data between runs
- Use unique test data (timestamps, UUIDs)
- Consider using test database or test containers

### 404 Not Found Errors

**Problem**: API endpoints return 404

**Solution**:
- Verify context path is `/` (not `/api` or other)
- Check Swagger UI to confirm endpoint paths
- Ensure CORS is configured correctly

### Timeout Issues

**Problem**: Tests timeout waiting for responses

**Solution**:
- Increase REST Assured timeout:
  ```java
  RestAssured.config = RestAssured.config()
      .httpClientConfig(HttpClientConfig.httpClientConfig()
          .setParam("http.connection.timeout", 10000)
          .setParam("http.socket.timeout", 10000));
  ```

## Best Practices

1. **Always start the application before running tests**
2. **Use unique test data** to avoid conflicts
3. **Clean up test data** after tests complete
4. **Run tests in order** (they use `@Order` annotation)
5. **Check logs** for detailed error messages
6. **Verify database state** before and after tests

## Next Steps

After Guest Management tests are working, you can create similar tests for:
- Room Management API
- Reservation Management API
- Invoice Management API

Follow the same pattern used in `GuestControllerIntegrationTest`.

## Additional Resources

- [REST Assured Documentation](https://rest-assured.io/)
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)

