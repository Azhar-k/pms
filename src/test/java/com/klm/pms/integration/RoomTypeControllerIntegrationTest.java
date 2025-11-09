package com.klm.pms.integration;

import com.klm.pms.config.TestConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Room Type Management API using REST Assured.
 * 
 * Prerequisites:
 * - Application must be running on localhost:8080
 * - Database must be accessible and configured
 * 
 * Test Order:
 * 1. Create operations
 * 2. Read operations
 * 3. Update operations
 * 4. Delete operations
 */
@TestMethodOrder(OrderAnnotation.class)
public class RoomTypeControllerIntegrationTest extends TestConfig {

    private static Long createdRoomTypeId;
    private static String createdRoomTypeName;
    private static Map<String, Object> testRoomType1;
    private static Map<String, Object> testRoomType2;
    private static Map<String, Object> testRoomType3;
    
    // Track all created room type IDs for cleanup
    private static final List<Long> createdRoomTypeIds = new ArrayList<>();
    
    // Logger for cleanup operations
    private static final Logger logger = LoggerFactory.getLogger(RoomTypeControllerIntegrationTest.class);

    @BeforeAll
    public static void setupTestData() {
        // Use unique test data with timestamp to avoid conflicts
        long timestamp = System.currentTimeMillis();
        String uniqueSuffix = String.valueOf(timestamp).substring(7); // Last 6 digits
        
        testRoomType1 = createRoomTypeMap("SINGLE_TEST_" + uniqueSuffix, 
                "Single room for testing", new BigDecimal("99.99"), 1, 
                "WiFi, TV", 25, true, false, true, true, true, "SINGLE");
        
        testRoomType2 = createRoomTypeMap("DOUBLE_TEST_" + uniqueSuffix, 
                "Double room for testing", new BigDecimal("149.99"), 2, 
                "WiFi, TV, Mini Bar", 35, true, true, true, true, true, "DOUBLE");
        
        testRoomType3 = createRoomTypeMap("SUITE_TEST_" + uniqueSuffix, 
                "Suite room for testing", new BigDecimal("299.99"), 4, 
                "WiFi, TV, Mini Bar, Safe, Jacuzzi", 60, true, true, true, true, true, "KING");
    }

    private static Map<String, Object> createRoomTypeMap(String name, String description, 
            BigDecimal basePricePerNight, Integer maxOccupancy, String amenities, 
            Integer defaultRoomSize, Boolean hasBalcony, Boolean hasView, 
            Boolean hasMinibar, Boolean hasSafe, Boolean hasAirConditioning, String bedType) {
        Map<String, Object> roomType = new HashMap<>();
        roomType.put("name", name);
        roomType.put("description", description);
        roomType.put("basePricePerNight", basePricePerNight);
        roomType.put("maxOccupancy", maxOccupancy);
        roomType.put("amenities", amenities);
        roomType.put("defaultRoomSize", defaultRoomSize);
        roomType.put("hasBalcony", hasBalcony);
        roomType.put("hasView", hasView);
        roomType.put("hasMinibar", hasMinibar);
        roomType.put("hasSafe", hasSafe);
        roomType.put("hasAirConditioning", hasAirConditioning);
        roomType.put("bedType", bedType);
        return roomType;
    }

    // ==================== CREATE OPERATIONS ====================

    @Test
    @Order(1)
    @DisplayName("POST /api/room-types - Create a new room type successfully")
    public void testCreateRoomType_Success() {
        Response response = given()
                .spec(requestSpec)
                .body(testRoomType1)
                .when()
                .post("/room-types")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("name", equalTo(testRoomType1.get("name")))
                .body("description", equalTo(testRoomType1.get("description")))
                .body("basePricePerNight", equalTo(((BigDecimal) testRoomType1.get("basePricePerNight")).floatValue()))
                .body("maxOccupancy", equalTo(testRoomType1.get("maxOccupancy")))
                .extract()
                .response();

        createdRoomTypeId = response.jsonPath().getLong("id");
        createdRoomTypeName = response.jsonPath().getString("name");
        
        assertNotNull(createdRoomTypeId);
        assertNotNull(createdRoomTypeName);
        
        // Track for cleanup
        createdRoomTypeIds.add(createdRoomTypeId);
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/room-types - Create additional room types for testing")
    public void testCreateAdditionalRoomTypes() {
        // Create second room type
        Response response2 = given()
                .spec(requestSpec)
                .body(testRoomType2)
                .when()
                .post("/room-types")
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long roomTypeId2 = response2.jsonPath().getLong("id");
        if (roomTypeId2 != null) {
            createdRoomTypeIds.add(roomTypeId2);
        }

        // Create third room type
        Response response3 = given()
                .spec(requestSpec)
                .body(testRoomType3)
                .when()
                .post("/room-types")
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long roomTypeId3 = response3.jsonPath().getLong("id");
        if (roomTypeId3 != null) {
            createdRoomTypeIds.add(roomTypeId3);
        }
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/room-types - Create room type with duplicate name should fail")
    public void testCreateRoomType_DuplicateName() {
        // Use the name from the room type we just created
        if (createdRoomTypeName != null) {
            Map<String, Object> duplicateRoomType = new HashMap<>(testRoomType1);
            duplicateRoomType.put("name", createdRoomTypeName); // Use the created room type's name
            duplicateRoomType.put("description", "Different description");

            given()
                    .spec(requestSpec)
                    .body(duplicateRoomType)
                    .when()
                    .post("/room-types")
                    .then()
                    .statusCode(400); // API returns 400 for duplicate name
        } else {
            // Skip test if no room type was created
            Assertions.fail("Cannot test duplicate name - no room type was created in previous test");
        }
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/room-types - Create room type with missing required fields should fail")
    public void testCreateRoomType_MissingRequiredFields() {
        Map<String, Object> invalidRoomType = new HashMap<>();
        invalidRoomType.put("description", "Test description");
        // Missing name, basePricePerNight, and maxOccupancy

        given()
                .spec(requestSpec)
                .body(invalidRoomType)
                .when()
                .post("/room-types")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/room-types - Create room type with invalid base price should fail")
    public void testCreateRoomType_InvalidBasePrice() {
        Map<String, Object> invalidRoomType = new HashMap<>(testRoomType1);
        invalidRoomType.put("name", "INVALID_PRICE_TEST_" + System.currentTimeMillis());
        invalidRoomType.put("basePricePerNight", -100.00); // Negative price

        given()
                .spec(requestSpec)
                .body(invalidRoomType)
                .when()
                .post("/room-types")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/room-types - Create room type with invalid max occupancy should fail")
    public void testCreateRoomType_InvalidMaxOccupancy() {
        Map<String, Object> invalidRoomType = new HashMap<>(testRoomType1);
        invalidRoomType.put("name", "INVALID_OCCUPANCY_TEST_" + System.currentTimeMillis());
        invalidRoomType.put("maxOccupancy", 0); // Invalid occupancy

        given()
                .spec(requestSpec)
                .body(invalidRoomType)
                .when()
                .post("/room-types")
                .then()
                .statusCode(400);
    }

    // ==================== READ OPERATIONS ====================

    @Test
    @Order(10)
    @DisplayName("GET /api/room-types/{id} - Get room type by ID successfully")
    public void testGetRoomTypeById_Success() {
        if (createdRoomTypeId != null) {
            given()
                    .spec(requestSpec)
                    .when()
                    .get("/room-types/{id}", createdRoomTypeId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(createdRoomTypeId.intValue()))
                    .body("name", equalTo(testRoomType1.get("name")))
                    .body("description", equalTo(testRoomType1.get("description")));
        } else {
            // If room type creation failed, try to get any existing room type
            Response response = given()
                    .spec(requestSpec)
                    .when()
                    .get("/room-types")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> roomTypes = response.jsonPath().getList("$");
            if (!roomTypes.isEmpty()) {
                Long roomTypeId = ((Number) roomTypes.get(0).get("id")).longValue();
                given()
                        .spec(requestSpec)
                        .when()
                        .get("/room-types/{id}", roomTypeId)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", notNullValue());
            } else {
                Assertions.fail("No room types available for testing");
            }
        }
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/room-types/{id} - Get non-existent room type should return 400")
    public void testGetRoomTypeById_NotFound() {
        given()
                .spec(requestSpec)
                .when()
                .get("/room-types/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/room-types/name/{name} - Get room type by name successfully")
    public void testGetRoomTypeByName_Success() {
        if (createdRoomTypeName != null) {
            given()
                    .spec(requestSpec)
                    .when()
                    .get("/room-types/name/{name}", createdRoomTypeName)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("name", equalTo(createdRoomTypeName))
                    .body("id", notNullValue());
        } else {
            // If no room type was created, try with any existing room type name
            Response response = given()
                    .spec(requestSpec)
                    .when()
                    .get("/room-types")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> roomTypes = response.jsonPath().getList("$");
            if (!roomTypes.isEmpty() && roomTypes.get(0).get("name") != null) {
                String name = (String) roomTypes.get(0).get("name");
                given()
                        .spec(requestSpec)
                        .when()
                        .get("/room-types/name/{name}", name)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("name", equalTo(name));
            } else {
                Assertions.fail("No room types with name available for testing");
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/room-types - Get all room types")
    public void testGetAllRoomTypes() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/room-types")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> roomTypes = response.jsonPath().getList("$");
        assertTrue(roomTypes.size() > 0, "Should have at least some room types");
    }

    // ==================== UPDATE OPERATIONS ====================

    @Test
    @Order(70)
    @DisplayName("PUT /api/room-types/{id} - Update room type successfully")
    public void testUpdateRoomType_Success() {
        if (createdRoomTypeId != null && createdRoomTypeName != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", createdRoomTypeName); // Keep same name
            updateData.put("description", "Updated description for testing");
            updateData.put("basePricePerNight", new BigDecimal("119.99"));
            updateData.put("maxOccupancy", 2);
            updateData.put("amenities", "WiFi, TV, Updated Amenities");

            Response response = given()
                    .spec(requestSpec)
                    .body(updateData)
                    .when()
                    .put("/room-types/{id}", createdRoomTypeId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(createdRoomTypeId.intValue()))
                    .body("description", equalTo("Updated description for testing"))
                    .extract()
                    .response();

            // Verify the update
            String updatedDescription = response.jsonPath().getString("description");
            assertEquals("Updated description for testing", updatedDescription);
        } else {
            // Get an existing room type to update
            Response listResponse = given()
                    .spec(requestSpec)
                    .when()
                    .get("/room-types")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> roomTypes = listResponse.jsonPath().getList("$");
            if (!roomTypes.isEmpty()) {
                Long roomTypeId = ((Number) roomTypes.get(0).get("id")).longValue();
                String name = (String) roomTypes.get(0).get("name");
                
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("name", name);
                updateData.put("description", "Updated description");
                updateData.put("basePricePerNight", new BigDecimal("199.99"));
                updateData.put("maxOccupancy", 3);
                
                given()
                        .spec(requestSpec)
                        .body(updateData)
                        .when()
                        .put("/room-types/{id}", roomTypeId)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", equalTo(roomTypeId.intValue()));
            } else {
                Assertions.fail("No room types available for update test");
            }
        }
    }

    @Test
    @Order(71)
    @DisplayName("PUT /api/room-types/{id} - Update non-existent room type should fail")
    public void testUpdateRoomType_NotFound() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "NON_EXISTENT");
        updateData.put("description", "Test");
        updateData.put("basePricePerNight", new BigDecimal("100.00"));
        updateData.put("maxOccupancy", 2);

        given()
                .spec(requestSpec)
                .body(updateData)
                .when()
                .put("/room-types/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @Test
    @Order(72)
    @DisplayName("PUT /api/room-types/{id} - Update with duplicate name should fail")
    public void testUpdateRoomType_DuplicateName() {
        if (createdRoomTypeId == null) {
            // Skip if no room type was created
            return;
        }
        
        // First, get another room type's name
        Response allRoomTypesResponse = given()
                .spec(requestSpec)
                .when()
                .get("/room-types")
                .then()
                .extract()
                .response();

        List<Map<String, Object>> roomTypes = allRoomTypesResponse.jsonPath().getList("$");
        if (roomTypes.size() >= 2) {
            // Find a room type with different ID
            String otherRoomTypeName = null;
            for (Map<String, Object> roomType : roomTypes) {
                Long roomTypeId = ((Number) roomType.get("id")).longValue();
                if (!roomTypeId.equals(createdRoomTypeId) && roomType.get("name") != null) {
                    otherRoomTypeName = (String) roomType.get("name");
                    break;
                }
            }
            
            if (otherRoomTypeName != null) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("name", otherRoomTypeName);
                updateData.put("description", "Test");
                updateData.put("basePricePerNight", new BigDecimal("100.00"));
                updateData.put("maxOccupancy", 2);

                given()
                        .spec(requestSpec)
                        .body(updateData)
                        .when()
                        .put("/room-types/{id}", createdRoomTypeId)
                        .then()
                        .statusCode(400); // Should fail due to duplicate name (returns 400)
            }
        }
    }

    // ==================== DELETE OPERATIONS ====================

    @Test
    @Order(80)
    @DisplayName("DELETE /api/room-types/{id} - Delete room type successfully")
    public void testDeleteRoomType_Success() {
        // First create a room type to delete with unique name
        long timestamp = System.currentTimeMillis();
        String uniqueName = "DELETE_ME_TEST_" + timestamp;
        
        Response createResponse = given()
                .spec(requestSpec)
                .body(createRoomTypeMap(uniqueName, "To be deleted", 
                        new BigDecimal("50.00"), 1, "Basic", 20, false, false, false, false, false, "SINGLE"))
                .when()
                .post("/room-types")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Long roomTypeIdToDelete = createResponse.jsonPath().getLong("id");

        // Delete the room type
        given()
                .spec(requestSpec)
                .when()
                .delete("/room-types/{id}", roomTypeIdToDelete)
                .then()
                .statusCode(204);
        
        // Remove from cleanup list since we already deleted it
        createdRoomTypeIds.remove(roomTypeIdToDelete);

        // Verify room type is deleted
        given()
                .spec(requestSpec)
                .when()
                .get("/room-types/{id}", roomTypeIdToDelete)
                .then()
                .statusCode(400); // Should return 400 as room type doesn't exist (via GlobalExceptionHandler)
    }

    @Test
    @Order(81)
    @DisplayName("DELETE /api/room-types/{id} - Delete non-existent room type should fail")
    public void testDeleteRoomType_NotFound() {
        given()
                .spec(requestSpec)
                .when()
                .delete("/room-types/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @AfterAll
    @DisplayName("Cleanup - Delete all test room types created during test execution")
    public static void cleanupTestData() {
        logger.info("Starting cleanup of test data - {} room type(s) to delete", createdRoomTypeIds.size());
        
        int deletedCount = 0;
        int failedCount = 0;
        
        for (Long roomTypeId : createdRoomTypeIds) {
            try {
                Response response = given()
                        .spec(requestSpec)
                        .when()
                        .delete("/room-types/{id}", roomTypeId)
                        .then()
                        .extract()
                        .response();
                
                if (response.getStatusCode() == 204) {
                    deletedCount++;
                    logger.debug("Successfully deleted test room type with ID: {}", roomTypeId);
                } else {
                    failedCount++;
                    logger.warn("Failed to delete test room type with ID: {} - Status: {}", roomTypeId, response.getStatusCode());
                }
            } catch (Exception e) {
                failedCount++;
                logger.warn("Exception while deleting test room type with ID: {} - {}", roomTypeId, e.getMessage());
            }
        }
        
        logger.info("Cleanup completed - Deleted: {}, Failed: {}, Total: {}", 
                deletedCount, failedCount, createdRoomTypeIds.size());
        
        // Clear the list
        createdRoomTypeIds.clear();
        createdRoomTypeId = null;
        createdRoomTypeName = null;
    }
}

