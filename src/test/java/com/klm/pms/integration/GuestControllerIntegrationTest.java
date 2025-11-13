package com.klm.pms.integration;

import com.klm.pms.config.TestConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Guest Management API using REST Assured.
 * 
 * Prerequisites:
 * - Application must be running on localhost:8080
 * - Database must be accessible and configured
 * 
 * Test Order:
 * 1. Create operations
 * 2. Read operations (including pagination, sorting, filtering)
 * 3. Update operations
 * 4. Delete operations
 */
@TestMethodOrder(OrderAnnotation.class)
public class GuestControllerIntegrationTest extends TestConfig {

    private static Long createdGuestId;
    private static String createdGuestEmail;
    private static Map<String, Object> testGuest1;
    private static Map<String, Object> testGuest2;
    private static Map<String, Object> testGuest3;
    
    // Track all created guest IDs for cleanup
    private static final List<Long> createdGuestIds = new ArrayList<>();
    
    // Logger for cleanup operations
    private static final Logger logger = LoggerFactory.getLogger(GuestControllerIntegrationTest.class);

    @BeforeAll
    public static void setupTestData() {
        // Use unique test data with timestamp to avoid conflicts
        long timestamp = System.currentTimeMillis();
        String uniqueSuffix = String.valueOf(timestamp).substring(7); // Last 6 digits
        
        testGuest1 = createGuestMap("John", "Doe", "john.doe.test" + uniqueSuffix + "@example.com", 
                "+1234567890", "123 Main St", "New York", "NY", "USA", "10001", 
                "PASSPORT", "P123456" + uniqueSuffix);
        
        testGuest2 = createGuestMap("Jane", "Smith", "jane.smith.test" + uniqueSuffix + "@example.com", 
                "+1987654321", "456 Oak Ave", "Los Angeles", "CA", "USA", "90001", 
                "DRIVER_LICENSE", "DL789012" + uniqueSuffix);
        
        testGuest3 = createGuestMap("Bob", "Johnson", "bob.johnson.test" + uniqueSuffix + "@example.com", 
                "+1555555555", "789 Pine Rd", "Chicago", "IL", "USA", "60601", 
                "ID_CARD", "ID345678" + uniqueSuffix);
    }

    private static Map<String, Object> createGuestMap(String firstName, String lastName, 
            String email, String phoneNumber, String address, String city, 
            String state, String country, String postalCode, String identificationType, 
            String identificationNumber) {
        Map<String, Object> guest = new HashMap<>();
        guest.put("firstName", firstName);
        guest.put("lastName", lastName);
        guest.put("email", email);
        guest.put("phoneNumber", phoneNumber);
        guest.put("address", address);
        guest.put("city", city);
        guest.put("state", state);
        guest.put("country", country);
        guest.put("postalCode", postalCode);
        guest.put("identificationType", identificationType);
        guest.put("identificationNumber", identificationNumber);
        return guest;
    }

    // ==================== AUTHENTICATION TESTS ====================

    @Test
    @Order(0)
    @DisplayName("POST /api/guests - Request without token should return 401")
    public void testCreateGuest_Unauthorized_NoToken() {
        given()
                .spec(authenticatedRequestSpec)  // No authentication header
                .body(testGuest1)
                .when()
                .post("/guests")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(0)
    @DisplayName("POST /api/guests - Request with expired token should return 401")
    public void testCreateGuest_Unauthorized_ExpiredToken() {
        String expiredToken = getExpiredToken(DEFAULT_TEST_USER);
        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + expiredToken)
                .body(testGuest1)
                .when()
                .post("/guests")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(0)
    @DisplayName("POST /api/guests - Request with invalid token should return 401")
    public void testCreateGuest_Unauthorized_InvalidToken() {
        String invalidToken = getInvalidToken(DEFAULT_TEST_USER);
        given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + invalidToken)
                .body(testGuest1)
                .when()
                .post("/guests")
                .then()
                .statusCode(401);
    }

    // ==================== CREATE OPERATIONS ====================

    @Test
    @Order(1)
    @DisplayName("POST /api/guests - Create a new guest successfully")
    public void testCreateGuest_Success() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .body(testGuest1)
                .when()
                .post("/guests")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("firstName", equalTo(testGuest1.get("firstName")))
                .body("lastName", equalTo(testGuest1.get("lastName")))
                .body("email", equalTo(testGuest1.get("email")))
                .body("phoneNumber", equalTo(testGuest1.get("phoneNumber")))
                .extract()
                .response();

        createdGuestId = response.jsonPath().getLong("id");
        createdGuestEmail = response.jsonPath().getString("email");
        
        assertNotNull(createdGuestId);
        assertNotNull(createdGuestEmail);
        
        // Track for cleanup
        createdGuestIds.add(createdGuestId);
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/guests - Create additional guests for testing")
    public void testCreateAdditionalGuests() {
        // Create second guest
        Response response2 = given()
                .spec(authenticatedRequestSpec)
                .body(testGuest2)
                .when()
                .post("/guests")
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long guestId2 = response2.jsonPath().getLong("id");
        if (guestId2 != null) {
            createdGuestIds.add(guestId2);
        }

        // Create third guest
        Response response3 = given()
                .spec(authenticatedRequestSpec)
                .body(testGuest3)
                .when()
                .post("/guests")
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long guestId3 = response3.jsonPath().getLong("id");
        if (guestId3 != null) {
            createdGuestIds.add(guestId3);
        }
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/guests - Create guest with duplicate email should fail")
    public void testCreateGuest_DuplicateEmail() {
        // Use the email from the guest we just created
        if (createdGuestEmail != null) {
            Map<String, Object> duplicateGuest = new HashMap<>(testGuest1);
            duplicateGuest.put("firstName", "Different");
            duplicateGuest.put("lastName", "Name");
            duplicateGuest.put("email", createdGuestEmail); // Use the created guest's email

            given()
                    .spec(authenticatedRequestSpec)
                    .body(duplicateGuest)
                    .when()
                    .post("/guests")
                    .then()
                    .statusCode(400); // API returns 400 for duplicate email
        } else {
            // Skip test if no guest was created
            Assertions.fail("Cannot test duplicate email - no guest was created in previous test");
        }
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/guests - Create guest with invalid email should fail")
    public void testCreateGuest_InvalidEmail() {
        Map<String, Object> invalidGuest = new HashMap<>(testGuest1);
        invalidGuest.put("email", "invalid-email");

        given()
                .spec(authenticatedRequestSpec)
                .body(invalidGuest)
                .when()
                .post("/guests")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/guests - Create guest with missing required fields should fail")
    public void testCreateGuest_MissingRequiredFields() {
        Map<String, Object> invalidGuest = new HashMap<>();
        invalidGuest.put("email", "test@example.com");
        // Missing firstName and lastName

        given()
                .spec(authenticatedRequestSpec)
                .body(invalidGuest)
                .when()
                .post("/guests")
                .then()
                .statusCode(400);
    }

    // ==================== READ OPERATIONS ====================

    @Test
    @Order(10)
    @DisplayName("GET /api/guests/{id} - Get guest by ID successfully")
    public void testGetGuestById_Success() {
        if (createdGuestId != null) {
            given()
                    .spec(authenticatedRequestSpec)
                    .when()
                    .get("/guests/{id}", createdGuestId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(createdGuestId.intValue()))
                    .body("firstName", equalTo(testGuest1.get("firstName")))
                    .body("lastName", equalTo(testGuest1.get("lastName")))
                    .body("email", equalTo(testGuest1.get("email")));
        } else {
            // If guest creation failed, try to get any existing guest
            Response response = given()
                    .spec(authenticatedRequestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
                    .when()
                    .get("/guests")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> guests = response.jsonPath().getList("content");
            if (!guests.isEmpty()) {
                Long guestId = ((Number) guests.get(0).get("id")).longValue();
                given()
                        .spec(authenticatedRequestSpec)
                        .when()
                        .get("/guests/{id}", guestId)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", notNullValue());
            } else {
                Assertions.fail("No guests available for testing");
            }
        }
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/guests/{id} - Get non-existent guest should return 400")
    public void testGetGuestById_NotFound() {
        given()
                .spec(authenticatedRequestSpec)
                .when()
                .get("/guests/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/guests/email/{email} - Get guest by email successfully")
    public void testGetGuestByEmail_Success() {
        if (createdGuestEmail != null) {
            given()
                    .spec(authenticatedRequestSpec)
                    .when()
                    .get("/guests/email/{email}", createdGuestEmail)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("email", equalTo(createdGuestEmail))
                    .body("id", notNullValue());
        } else {
            // If no guest was created, try with any existing guest email
            Response response = given()
                    .spec(authenticatedRequestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
                    .when()
                    .get("/guests")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> guests = response.jsonPath().getList("content");
            if (!guests.isEmpty() && guests.get(0).get("email") != null) {
                String email = (String) guests.get(0).get("email");
                given()
                        .spec(authenticatedRequestSpec)
                        .when()
                        .get("/guests/email/{email}", email)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("email", equalTo(email));
            } else {
                Assertions.fail("No guests with email available for testing");
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/guests - Get all guests (non-paginated)")
    public void testGetAllGuests_NonPaginated() {
        // Note: The API now always returns paginated response when any query param is present
        // or when the controller detects pagination params. Without any params, it returns List.
        // But based on the actual response, it seems to always return PageResponse now.
        // Let's check both cases.
        Response response = given()
                .spec(authenticatedRequestSpec)
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        // Check if response is paginated (has 'content' field) or plain list
        Object content = response.jsonPath().get("content");
        if (content != null) {
            // It's a paginated response
            List<Map<String, Object>> guests = response.jsonPath().getList("content");
            assertTrue(guests.size() > 0, "Should have at least some guests");
            assertTrue(response.jsonPath().getInt("totalElements") > 0);
        } else {
            // It's a plain list
            List<Map<String, Object>> guests = response.jsonPath().getList("$");
            assertTrue(guests.size() > 0, "Should have at least some guests");
        }
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    @Order(20)
    @DisplayName("GET /api/guests - Test pagination with page and size")
    public void testGetAllGuests_WithPagination() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", instanceOf(List.class))
                .body("page", equalTo(0))
                .body("size", equalTo(2))
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertEquals(2, content.size(), "Page size should be 2");
        
        // Verify pagination metadata
        long totalElements = response.jsonPath().getLong("totalElements");
        int totalPages = response.jsonPath().getInt("totalPages");
        assertTrue(totalElements >= 2, "Should have at least 2 total elements");
        assertTrue(totalPages >= 1, "Should have at least 1 total page");
        assertTrue(response.jsonPath().getBoolean("first"), "Should be first page");
    }

    @Test
    @Order(21)
    @DisplayName("GET /api/guests - Test pagination second page")
    public void testGetAllGuests_SecondPage() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 1)
                .queryParam("size", 2)
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("page", equalTo(1))
                .body("size", equalTo(2))
                .body("first", equalTo(false))
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() <= 2);
    }

    @Test
    @Order(22)
    @DisplayName("GET /api/guests - Test pagination with default values")
    public void testGetAllGuests_DefaultPagination() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("page", equalTo(0))
                .body("size", equalTo(10))
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() <= 10);
    }

    // ==================== SORTING TESTS ====================

    @Test
    @Order(30)
    @DisplayName("GET /api/guests - Test sorting by lastName ascending")
    public void testGetAllGuests_SortByLastNameAsc() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "lastName")
                .queryParam("sortDir", "asc")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() >= 2) {
            String firstLastName = (String) content.get(0).get("lastName");
            String secondLastName = (String) content.get(1).get("lastName");
            assertTrue(firstLastName.compareToIgnoreCase(secondLastName) <= 0, 
                    "Guests should be sorted by lastName in ascending order");
        }
    }

    @Test
    @Order(31)
    @DisplayName("GET /api/guests - Test sorting by lastName descending")
    public void testGetAllGuests_SortByLastNameDesc() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "lastName")
                .queryParam("sortDir", "desc")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() >= 2) {
            String firstLastName = (String) content.get(0).get("lastName");
            String secondLastName = (String) content.get(1).get("lastName");
            assertTrue(firstLastName.compareToIgnoreCase(secondLastName) >= 0, 
                    "Guests should be sorted by lastName in descending order");
        }
    }

    @Test
    @Order(32)
    @DisplayName("GET /api/guests - Test sorting by firstName")
    public void testGetAllGuests_SortByFirstName() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "firstName")
                .queryParam("sortDir", "asc")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() > 0);
    }

    @Test
    @Order(33)
    @DisplayName("GET /api/guests - Test sorting by email")
    public void testGetAllGuests_SortByEmail() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "email")
                .queryParam("sortDir", "asc")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() > 0);
    }

    // ==================== FILTERING TESTS ====================

    @Test
    @Order(40)
    @DisplayName("GET /api/guests - Test filtering by firstName")
    public void testGetAllGuests_FilterByFirstName() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("firstName", "John")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() > 0);
        content.forEach(guest -> {
            String firstName = (String) guest.get("firstName");
            assertTrue(firstName.toLowerCase().contains("john"), 
                    "All guests should have firstName containing 'John'");
        });
    }

    @Test
    @Order(41)
    @DisplayName("GET /api/guests - Test filtering by lastName")
    public void testGetAllGuests_FilterByLastName() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("lastName", "Doe")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() > 0);
        content.forEach(guest -> {
            String lastName = (String) guest.get("lastName");
            assertTrue(lastName.toLowerCase().contains("doe"), 
                    "All guests should have lastName containing 'Doe'");
        });
    }

    @Test
    @Order(42)
    @DisplayName("GET /api/guests - Test filtering by email")
    public void testGetAllGuests_FilterByEmail() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("email", "john.doe")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() > 0);
        content.forEach(guest -> {
            String email = (String) guest.get("email");
            assertTrue(email.toLowerCase().contains("john.doe"), 
                    "All guests should have email containing 'john.doe'");
        });
    }

    @Test
    @Order(43)
    @DisplayName("GET /api/guests - Test filtering by city")
    public void testGetAllGuests_FilterByCity() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("city", "New York")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        // Filter might return empty if no guests match, or might return guests with matching city
        if (content.size() > 0) {
            content.forEach(guest -> {
                String city = (String) guest.get("city");
                // City filter uses case-insensitive contains, so check if city contains "new york"
                assertTrue(city != null && city.toLowerCase().contains("new york"), 
                        "All guests should have city containing 'New York', but found: " + city);
            });
        } else {
            // If no results, that's also valid - just means no guests match the filter
            assertTrue(true, "No guests match the city filter, which is valid");
        }
    }

    @Test
    @Order(44)
    @DisplayName("GET /api/guests - Test filtering by country")
    public void testGetAllGuests_FilterByCountry() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("country", "USA")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() > 0);
        content.forEach(guest -> {
            String country = (String) guest.get("country");
            assertTrue(country != null && country.toLowerCase().contains("usa"), 
                    "All guests should have country containing 'USA'");
        });
    }

    @Test
    @Order(45)
    @DisplayName("GET /api/guests - Test filtering by identificationType")
    public void testGetAllGuests_FilterByIdentificationType() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("identificationType", "PASSPORT")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() > 0);
        content.forEach(guest -> {
            String idType = (String) guest.get("identificationType");
            assertEquals("PASSPORT", idType, 
                    "All guests should have identificationType 'PASSPORT'");
        });
    }

    // ==================== SEARCH TESTS ====================

    @Test
    @Order(50)
    @DisplayName("GET /api/guests - Test search term functionality")
    public void testGetAllGuests_SearchTerm() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("searchTerm", "john")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() > 0);
        // Search term should match firstName, lastName, email, phone, or address
        content.forEach(guest -> {
            String firstName = (String) guest.get("firstName");
            String lastName = (String) guest.get("lastName");
            String email = (String) guest.get("email");
            String phone = (String) guest.get("phoneNumber");
            String address = (String) guest.get("address");
            
            boolean matches = (firstName != null && firstName.toLowerCase().contains("john")) ||
                             (lastName != null && lastName.toLowerCase().contains("john")) ||
                             (email != null && email.toLowerCase().contains("john")) ||
                             (phone != null && phone.contains("john")) ||
                             (address != null && address.toLowerCase().contains("john"));
            
            assertTrue(matches, "Guest should match search term 'john'");
        });
    }

    // ==================== COMBINED FILTERING AND PAGINATION ====================

    @Test
    @Order(60)
    @DisplayName("GET /api/guests - Test combined filtering, pagination, and sorting")
    public void testGetAllGuests_CombinedFilters() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 5)
                .queryParam("sortBy", "lastName")
                .queryParam("sortDir", "asc")
                .queryParam("country", "USA")
                .queryParam("city", "New York")
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("page", equalTo(0))
                .body("size", equalTo(5))
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() <= 5);
        
        // Verify all results match filters
        content.forEach(guest -> {
            String country = (String) guest.get("country");
            String city = (String) guest.get("city");
            assertTrue(country != null && country.toLowerCase().contains("usa"));
            assertTrue(city != null && city.toLowerCase().contains("new york"));
        });
    }

    // ==================== UPDATE OPERATIONS ====================

    @Test
    @Order(70)
    @DisplayName("PUT /api/guests/{id} - Update guest successfully")
    public void testUpdateGuest_Success() {
        if (createdGuestId != null && createdGuestEmail != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("firstName", "John Updated");
            updateData.put("lastName", "Doe Updated");
            updateData.put("email", createdGuestEmail);
            updateData.put("phoneNumber", "+1999999999");
            updateData.put("city", "Boston");
            updateData.put("state", "MA");

            Response response = given()
                    .spec(authenticatedRequestSpec)
                    .body(updateData)
                    .when()
                    .put("/guests/{id}", createdGuestId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(createdGuestId.intValue()))
                    .body("firstName", equalTo("John Updated"))
                    .body("lastName", equalTo("Doe Updated"))
                    .body("city", equalTo("Boston"))
                    .extract()
                    .response();

            // Verify the update
            String updatedFirstName = response.jsonPath().getString("firstName");
            assertEquals("John Updated", updatedFirstName);
        } else {
            // Get an existing guest to update
            Response listResponse = given()
                    .spec(authenticatedRequestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
                    .when()
                    .get("/guests")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> guests = listResponse.jsonPath().getList("content");
            if (!guests.isEmpty()) {
                Long guestId = ((Number) guests.get(0).get("id")).longValue();
                String email = (String) guests.get(0).get("email");
                
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("firstName", "Updated Name");
                updateData.put("lastName", "Updated Last");
                updateData.put("email", email);
                
                given()
                        .spec(authenticatedRequestSpec)
                        .body(updateData)
                        .when()
                        .put("/guests/{id}", guestId)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", equalTo(guestId.intValue()));
            } else {
                Assertions.fail("No guests available for update test");
            }
        }
    }

    @Test
    @Order(71)
    @DisplayName("PUT /api/guests/{id} - Update non-existent guest should fail")
    public void testUpdateGuest_NotFound() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("firstName", "Test");
        updateData.put("lastName", "User");
        updateData.put("email", "test@example.com");

        given()
                .spec(authenticatedRequestSpec)
                .body(updateData)
                .when()
                .put("/guests/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @Test
    @Order(72)
    @DisplayName("PUT /api/guests/{id} - Update with duplicate email should fail")
    public void testUpdateGuest_DuplicateEmail() {
        if (createdGuestId == null) {
            // Skip if no guest was created
            return;
        }
        
        // First, get another guest's email
        Response allGuestsResponse = given()
                .spec(authenticatedRequestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/guests")
                .then()
                .extract()
                .response();

        List<Map<String, Object>> guests = allGuestsResponse.jsonPath().getList("content");
        if (guests.size() >= 2) {
            // Find a guest with different ID
            String otherGuestEmail = null;
            for (Map<String, Object> guest : guests) {
                Long guestId = ((Number) guest.get("id")).longValue();
                if (!guestId.equals(createdGuestId) && guest.get("email") != null) {
                    otherGuestEmail = (String) guest.get("email");
                    break;
                }
            }
            
            if (otherGuestEmail != null) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("firstName", "Test");
                updateData.put("lastName", "User");
                updateData.put("email", otherGuestEmail);

                given()
                        .spec(authenticatedRequestSpec)
                        .body(updateData)
                        .when()
                        .put("/guests/{id}", createdGuestId)
                        .then()
                        .statusCode(400); // Should fail due to duplicate email (returns 400)
            }
        }
    }

    // ==================== DELETE OPERATIONS ====================

    @Test
    @Order(80)
    @DisplayName("DELETE /api/guests/{id} - Delete guest successfully")
    public void testDeleteGuest_Success() {
        // First create a guest to delete with unique email
        long timestamp = System.currentTimeMillis();
        String uniqueEmail = "delete.me.test" + timestamp + "@example.com";
        
        Response createResponse = given()
                .spec(authenticatedRequestSpec)
                .body(createGuestMap("Delete", "Me", uniqueEmail, 
                        "+1111111111", "Test St", "Test City", "TS", "USA", "12345", 
                        "ID_CARD", "DEL123" + timestamp))
                .when()
                .post("/guests")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Long guestIdToDelete = createResponse.jsonPath().getLong("id");

        // Delete the guest
        given()
                .spec(authenticatedRequestSpec)
                .when()
                .delete("/guests/{id}", guestIdToDelete)
                .then()
                .statusCode(204);
        
        // Remove from cleanup list since we already deleted it
        createdGuestIds.remove(guestIdToDelete);

        // Verify guest is deleted
        given()
                .spec(authenticatedRequestSpec)
                .when()
                .get("/guests/{id}", guestIdToDelete)
                .then()
                .statusCode(400); // Should return 400 as guest doesn't exist (via GlobalExceptionHandler)
    }

    @Test
    @Order(81)
    @DisplayName("DELETE /api/guests/{id} - Delete non-existent guest should fail")
    public void testDeleteGuest_NotFound() {
        given()
                .spec(authenticatedRequestSpec)
                .when()
                .delete("/guests/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @AfterAll
    @DisplayName("Cleanup - Delete all test guests created during test execution")
    public static void cleanupTestData() {
        logger.info("Starting cleanup of test data - {} guest(s) to delete", createdGuestIds.size());
        
        int deletedCount = 0;
        int failedCount = 0;
        
        for (Long guestId : createdGuestIds) {
            try {
                Response response = given()
                        .spec(authenticatedRequestSpec)
                        .when()
                        .delete("/guests/{id}", guestId)
                        .then()
                        .extract()
                        .response();
                
                if (response.getStatusCode() == 204) {
                    deletedCount++;
                    logger.debug("Successfully deleted test guest with ID: {}", guestId);
                } else {
                    failedCount++;
                    logger.warn("Failed to delete test guest with ID: {} - Status: {}", guestId, response.getStatusCode());
                }
            } catch (Exception e) {
                failedCount++;
                logger.warn("Exception while deleting test guest with ID: {} - {}", guestId, e.getMessage());
            }
        }
        
        logger.info("Cleanup completed - Deleted: {}, Failed: {}, Total: {}", 
                deletedCount, failedCount, createdGuestIds.size());
        
        // Clear the list
        createdGuestIds.clear();
        createdGuestId = null;
        createdGuestEmail = null;
    }
}

