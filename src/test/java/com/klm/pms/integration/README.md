# Integration Tests for Guest Management API

This directory contains REST Assured integration tests for the Guest Management API.

## Prerequisites

Before running the integration tests, ensure:

1. **Application is Running**: The Spring Boot application must be running on `localhost:8080`
   ```bash
   mvn spring-boot:run
   ```

2. **Database is Accessible**: PostgreSQL database must be running and accessible with the configuration from `application.properties`
   - Database: `pms`
   - Host: `localhost:5432`
   - Username: `postgres`
   - Password: `postgres`

3. **Dependencies**: All Maven dependencies should be downloaded (run `mvn clean install` first)

## Running the Tests

### Run All Integration Tests
```bash
mvn test
```

### Run Only Guest Integration Tests
```bash
mvn test -Dtest=GuestControllerIntegrationTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=GuestControllerIntegrationTest#testCreateGuest_Success
```

### Run Tests with Maven Surefire Plugin
```bash
mvn surefire:test -Dtest=GuestControllerIntegrationTest
```

## Test Structure

The `GuestControllerIntegrationTest` class includes comprehensive tests for:

### 1. Create Operations (Order 1-5)
- ✅ Create guest successfully
- ✅ Create additional guests
- ✅ Create guest with duplicate email (should fail)
- ✅ Create guest with invalid email (should fail)
- ✅ Create guest with missing required fields (should fail)

### 2. Read Operations (Order 10-13)
- ✅ Get guest by ID
- ✅ Get non-existent guest (should return error)
- ✅ Get guest by email
- ✅ Get all guests (non-paginated)

### 3. Pagination Tests (Order 20-22)
- ✅ Pagination with page and size parameters
- ✅ Second page pagination
- ✅ Default pagination values

### 4. Sorting Tests (Order 30-33)
- ✅ Sort by lastName ascending
- ✅ Sort by lastName descending
- ✅ Sort by firstName
- ✅ Sort by email

### 5. Filtering Tests (Order 40-45)
- ✅ Filter by firstName
- ✅ Filter by lastName
- ✅ Filter by email
- ✅ Filter by city
- ✅ Filter by country
- ✅ Filter by identificationType

### 6. Search Tests (Order 50)
- ✅ Search term functionality (searches across firstName, lastName, email, phone, address)

### 7. Combined Tests (Order 60)
- ✅ Combined filtering, pagination, and sorting

### 8. Update Operations (Order 70-72)
- ✅ Update guest successfully
- ✅ Update non-existent guest (should fail)
- ✅ Update with duplicate email (should fail)

### 9. Delete Operations (Order 80-81)
- ✅ Delete guest successfully
- ✅ Delete non-existent guest (should fail)

## Test Configuration

The tests use the `TestConfig` base class which:
- Configures REST Assured base URL: `http://localhost:8080`
- Sets API base path: `/api`
- Configures default content type as JSON
- Enables request/response logging on validation failures

## API Endpoints Tested

- `POST /api/guests` - Create guest
- `GET /api/guests/{id}` - Get guest by ID
- `GET /api/guests/email/{email}` - Get guest by email
- `GET /api/guests` - Get all guests (with pagination, sorting, filtering)
- `PUT /api/guests/{id}` - Update guest
- `DELETE /api/guests/{id}` - Delete guest

## Query Parameters for GET /api/guests

### Pagination
- `page` - Page number (0-indexed, default: 0)
- `size` - Page size (default: 10)

### Sorting
- `sortBy` - Field to sort by (e.g., `lastName`, `firstName`, `email`)
- `sortDir` - Sort direction (`asc` or `desc`, default: `asc`)

### Filtering
- `firstName` - Filter by first name (partial match, case-insensitive)
- `lastName` - Filter by last name (partial match, case-insensitive)
- `email` - Filter by email (partial match, case-insensitive)
- `phoneNumber` - Filter by phone number (partial match)
- `city` - Filter by city (partial match, case-insensitive)
- `state` - Filter by state (partial match, case-insensitive)
- `country` - Filter by country (partial match, case-insensitive)
- `identificationType` - Filter by identification type (exact match)
- `searchTerm` - Search across firstName, lastName, email, phoneNumber, address, city

## Example Test Execution

```bash
# Start the application first
mvn spring-boot:run

# In another terminal, run tests
mvn test -Dtest=GuestControllerIntegrationTest
```

## Troubleshooting

### Tests Fail with Connection Refused
- Ensure the application is running on port 8080
- Check if another process is using port 8080

### Tests Fail with Database Errors
- Verify PostgreSQL is running
- Check database connection settings in `application.properties`
- Ensure database `pms` exists

### Tests Fail with 404 Errors
- Verify the application context path is `/` (not `/api` or other)
- Check that the API endpoints are accessible via Swagger UI: `http://localhost:8080/swagger-ui.html`

### Tests Fail Due to Existing Data
- The tests create test data and may fail if duplicate emails exist
- Consider cleaning up test data between test runs or using unique test data

## Notes

- Tests are ordered using `@Order` annotation to ensure proper execution sequence
- Test data is created in `@BeforeAll` method
- Some tests depend on data created in previous tests
- The tests assume a clean or minimal database state

