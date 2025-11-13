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
 * Integration tests for Rate Type Management API using REST Assured.
 * 
 * Prerequisites:
 * - Application must be running on localhost:8080
 * - Database must be accessible and configured
 * - At least one room type must exist in the database (for room type rate operations)
 * 
 * Test Order:
 * 1. Create operations
 * 2. Read operations
 * 3. Update operations
 * 4. Room type rate management operations
 * 5. Delete operations
 */
@TestMethodOrder(OrderAnnotation.class)
public class RateTypeControllerIntegrationTest extends TestConfig {

    private static Long createdRateTypeId;
    private static String createdRateTypeName;
    private static Map<String, Object> testRateType1;
    private static Map<String, Object> testRateType2;
    private static Map<String, Object> testRateType3;
    
    // Track room types for room type rate operations
    private static Long roomTypeId1;
    private static Long roomTypeId2;
    
    // Track all created rate type IDs for cleanup
    private static final List<Long> createdRateTypeIds = new ArrayList<>();
    
    // Logger for cleanup operations
    private static final Logger logger = LoggerFactory.getLogger(RateTypeControllerIntegrationTest.class);

    @BeforeAll
    public static void setupTestData() {
        // Use unique test data with timestamp to avoid conflicts
        long timestamp = System.currentTimeMillis();
        String uniqueSuffix = String.valueOf(timestamp).substring(7); // Last 6 digits
        
        testRateType1 = createRateTypeMap("STANDARD_TEST_" + uniqueSuffix, 
                "Standard rate type for testing");
        
        testRateType2 = createRateTypeMap("PREMIUM_TEST_" + uniqueSuffix, 
                "Premium rate type for testing");
        
        testRateType3 = createRateTypeMap("DISCOUNT_TEST_" + uniqueSuffix, 
                "Discount rate type for testing");
        
        // Get or create room types for room type rate operations
        setupRoomTypes();
    }
    
    private static void setupRoomTypes() {
        // Try to get existing room types
        Response response = given()
                .spec(authenticatedRequestSpec)
                .when()
                .get("/room-types")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        List<Map<String, Object>> roomTypes = response.jsonPath().getList("$");
        
        if (roomTypes.size() >= 2) {
            // Use existing room types
            roomTypeId1 = ((Number) roomTypes.get(0).get("id")).longValue();
            roomTypeId2 = ((Number) roomTypes.get(1).get("id")).longValue();
        } else {
            // Create room types if not enough exist
            logger.warn("Not enough room types found. Creating test room types for rate type tests.");
            
            // Create first room type
            Map<String, Object> roomType1 = new HashMap<>();
            roomType1.put("name", "RT_FOR_RATE_TEST_1_" + System.currentTimeMillis());
            roomType1.put("description", "Room type for rate testing");
            roomType1.put("basePricePerNight", new BigDecimal("100.00"));
            roomType1.put("maxOccupancy", 2);
            
            Response createResponse1 = given()
                    .spec(authenticatedRequestSpec)
                    .body(roomType1)
                    .when()
                    .post("/room-types")
                    .then()
                    .statusCode(201)
                    .extract()
                    .response();
            
            roomTypeId1 = createResponse1.jsonPath().getLong("id");
            
            // Create second room type
            Map<String, Object> roomType2 = new HashMap<>();
            roomType2.put("name", "RT_FOR_RATE_TEST_2_" + System.currentTimeMillis());
            roomType2.put("description", "Room type for rate testing");
            roomType2.put("basePricePerNight", new BigDecimal("150.00"));
            roomType2.put("maxOccupancy", 3);
            
            Response createResponse2 = given()
                    .spec(authenticatedRequestSpec)
                    .body(roomType2)
                    .when()
                    .post("/room-types")
                    .then()
                    .statusCode(201)
                    .extract()
                    .response();
            
            roomTypeId2 = createResponse2.jsonPath().getLong("id");
        }
    }

    private static Map<String, Object> createRateTypeMap(String name, String description) {
        Map<String, Object> rateType = new HashMap<>();
        rateType.put("name", name);
        rateType.put("description", description);
        return rateType;
    }
    
    private static Map<String, Object> createRoomTypeRateMap(Long roomTypeId, BigDecimal rate) {
        Map<String, Object> roomTypeRate = new HashMap<>();
        roomTypeRate.put("roomTypeId", roomTypeId);
        roomTypeRate.put("rate", rate);
        return roomTypeRate;
    }

    // ==================== CREATE OPERATIONS ====================

    @Test
    @Order(1)
    @DisplayName("POST /api/rate-types - Create a new rate type successfully")
    public void testCreateRateType_Success() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .body(testRateType1)
                .when()
                .post("/rate-types")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("name", equalTo(testRateType1.get("name")))
                .body("description", equalTo(testRateType1.get("description")))
                .extract()
                .response();

        createdRateTypeId = response.jsonPath().getLong("id");
        createdRateTypeName = response.jsonPath().getString("name");
        
        assertNotNull(createdRateTypeId);
        assertNotNull(createdRateTypeName);
        
        // Track for cleanup
        createdRateTypeIds.add(createdRateTypeId);
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/rate-types - Create rate type with room type rates")
    public void testCreateRateType_WithRoomTypeRates() {
        if (roomTypeId1 == null || roomTypeId2 == null) {
            Assertions.fail("Room types not available for testing");
            return;
        }
        
        Map<String, Object> rateTypeWithRates = new HashMap<>(testRateType2);
        List<Map<String, Object>> roomTypeRates = new ArrayList<>();
        roomTypeRates.add(createRoomTypeRateMap(roomTypeId1, new BigDecimal("120.00")));
        roomTypeRates.add(createRoomTypeRateMap(roomTypeId2, new BigDecimal("180.00")));
        rateTypeWithRates.put("roomTypeRates", roomTypeRates);
        
        Response response = given()
                .spec(authenticatedRequestSpec)
                .body(rateTypeWithRates)
                .when()
                .post("/rate-types")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("name", equalTo(testRateType2.get("name")))
                .body("roomTypeRates", notNullValue())
                .extract()
                .response();
        
        Long rateTypeId = response.jsonPath().getLong("id");
        if (rateTypeId != null) {
            createdRateTypeIds.add(rateTypeId);
        }
        
        List<Map<String, Object>> returnedRates = response.jsonPath().getList("roomTypeRates");
        assertTrue(returnedRates.size() >= 2, "Should have at least 2 room type rates");
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/rate-types - Create additional rate type for testing")
    public void testCreateAdditionalRateType() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .body(testRateType3)
                .when()
                .post("/rate-types")
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long rateTypeId = response.jsonPath().getLong("id");
        if (rateTypeId != null) {
            createdRateTypeIds.add(rateTypeId);
        }
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/rate-types - Create rate type with duplicate name should fail")
    public void testCreateRateType_DuplicateName() {
        // Use the name from the rate type we just created
        if (createdRateTypeName != null) {
            Map<String, Object> duplicateRateType = new HashMap<>(testRateType1);
            duplicateRateType.put("name", createdRateTypeName); // Use the created rate type's name
            duplicateRateType.put("description", "Different description");

            given()
                    .spec(authenticatedRequestSpec)
                    .body(duplicateRateType)
                    .when()
                    .post("/rate-types")
                    .then()
                    .statusCode(400); // API returns 400 for duplicate name
        } else {
            // Skip test if no rate type was created
            Assertions.fail("Cannot test duplicate name - no rate type was created in previous test");
        }
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/rate-types - Create rate type with missing required fields should fail")
    public void testCreateRateType_MissingRequiredFields() {
        Map<String, Object> invalidRateType = new HashMap<>();
        invalidRateType.put("description", "Test description");
        // Missing name

        given()
                .spec(authenticatedRequestSpec)
                .body(invalidRateType)
                .when()
                .post("/rate-types")
                .then()
                .statusCode(400);
    }

    // ==================== READ OPERATIONS ====================

    @Test
    @Order(10)
    @DisplayName("GET /api/rate-types/{id} - Get rate type by ID successfully")
    public void testGetRateTypeById_Success() {
        if (createdRateTypeId != null) {
            given()
                    .spec(authenticatedRequestSpec)
                    .when()
                    .get("/rate-types/{id}", createdRateTypeId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(createdRateTypeId.intValue()))
                    .body("name", equalTo(testRateType1.get("name")))
                    .body("description", equalTo(testRateType1.get("description")));
        } else {
            // If rate type creation failed, try to get any existing rate type
            Response response = given()
                    .spec(authenticatedRequestSpec)
                    .when()
                    .get("/rate-types")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> rateTypes = response.jsonPath().getList("$");
            if (!rateTypes.isEmpty()) {
                Long rateTypeId = ((Number) rateTypes.get(0).get("id")).longValue();
                given()
                        .spec(authenticatedRequestSpec)
                        .when()
                        .get("/rate-types/{id}", rateTypeId)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", notNullValue());
            } else {
                Assertions.fail("No rate types available for testing");
            }
        }
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/rate-types/{id} - Get non-existent rate type should return 400")
    public void testGetRateTypeById_NotFound() {
        given()
                .spec(authenticatedRequestSpec)
                .when()
                .get("/rate-types/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/rate-types/name/{name} - Get rate type by name successfully")
    public void testGetRateTypeByName_Success() {
        if (createdRateTypeName != null) {
            given()
                    .spec(authenticatedRequestSpec)
                    .when()
                    .get("/rate-types/name/{name}", createdRateTypeName)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("name", equalTo(createdRateTypeName))
                    .body("id", notNullValue());
        } else {
            // If no rate type was created, try with any existing rate type name
            Response response = given()
                    .spec(authenticatedRequestSpec)
                    .when()
                    .get("/rate-types")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> rateTypes = response.jsonPath().getList("$");
            if (!rateTypes.isEmpty() && rateTypes.get(0).get("name") != null) {
                String name = (String) rateTypes.get(0).get("name");
                given()
                        .spec(authenticatedRequestSpec)
                        .when()
                        .get("/rate-types/name/{name}", name)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("name", equalTo(name));
            } else {
                Assertions.fail("No rate types with name available for testing");
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/rate-types - Get all rate types")
    public void testGetAllRateTypes() {
        Response response = given()
                .spec(authenticatedRequestSpec)
                .when()
                .get("/rate-types")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> rateTypes = response.jsonPath().getList("$");
        assertTrue(rateTypes.size() > 0, "Should have at least some rate types");
    }

    // ==================== UPDATE OPERATIONS ====================

    @Test
    @Order(70)
    @DisplayName("PUT /api/rate-types/{id} - Update rate type successfully")
    public void testUpdateRateType_Success() {
        if (createdRateTypeId != null && createdRateTypeName != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", createdRateTypeName); // Keep same name
            updateData.put("description", "Updated description for testing");

            Response response = given()
                    .spec(authenticatedRequestSpec)
                    .body(updateData)
                    .when()
                    .put("/rate-types/{id}", createdRateTypeId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(createdRateTypeId.intValue()))
                    .body("description", equalTo("Updated description for testing"))
                    .extract()
                    .response();

            // Verify the update
            String updatedDescription = response.jsonPath().getString("description");
            assertEquals("Updated description for testing", updatedDescription);
        } else {
            // Get an existing rate type to update
            Response listResponse = given()
                    .spec(authenticatedRequestSpec)
                    .when()
                    .get("/rate-types")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> rateTypes = listResponse.jsonPath().getList("$");
            if (!rateTypes.isEmpty()) {
                Long rateTypeId = ((Number) rateTypes.get(0).get("id")).longValue();
                String name = (String) rateTypes.get(0).get("name");
                
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("name", name);
                updateData.put("description", "Updated description");
                
                given()
                        .spec(authenticatedRequestSpec)
                        .body(updateData)
                        .when()
                        .put("/rate-types/{id}", rateTypeId)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", equalTo(rateTypeId.intValue()));
            } else {
                Assertions.fail("No rate types available for update test");
            }
        }
    }

    @Test
    @Order(71)
    @DisplayName("PUT /api/rate-types/{id} - Update non-existent rate type should fail")
    public void testUpdateRateType_NotFound() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", "NON_EXISTENT");
        updateData.put("description", "Test");

        given()
                .spec(authenticatedRequestSpec)
                .body(updateData)
                .when()
                .put("/rate-types/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @Test
    @Order(72)
    @DisplayName("PUT /api/rate-types/{id} - Update with duplicate name should fail")
    public void testUpdateRateType_DuplicateName() {
        if (createdRateTypeId == null) {
            // Skip if no rate type was created
            return;
        }
        
        // First, get another rate type's name
        Response allRateTypesResponse = given()
                .spec(authenticatedRequestSpec)
                .when()
                .get("/rate-types")
                .then()
                .extract()
                .response();

        List<Map<String, Object>> rateTypes = allRateTypesResponse.jsonPath().getList("$");
        if (rateTypes.size() >= 2) {
            // Find a rate type with different ID
            String otherRateTypeName = null;
            for (Map<String, Object> rateType : rateTypes) {
                Long rateTypeId = ((Number) rateType.get("id")).longValue();
                if (!rateTypeId.equals(createdRateTypeId) && rateType.get("name") != null) {
                    otherRateTypeName = (String) rateType.get("name");
                    break;
                }
            }
            
            if (otherRateTypeName != null) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("name", otherRateTypeName);
                updateData.put("description", "Test");

                given()
                        .spec(authenticatedRequestSpec)
                        .body(updateData)
                        .when()
                        .put("/rate-types/{id}", createdRateTypeId)
                        .then()
                        .statusCode(400); // Should fail due to duplicate name (returns 400)
            }
        }
    }

    // ==================== ROOM TYPE RATE OPERATIONS ====================

    @Test
    @Order(80)
    @DisplayName("POST /api/rate-types/{rateTypeId}/room-type-rates - Add room type rate successfully")
    public void testAddRoomTypeRate_Success() {
        if (createdRateTypeId == null || roomTypeId1 == null) {
            Assertions.fail("Rate type or room type not available for testing");
            return;
        }
        
        Map<String, Object> roomTypeRate = createRoomTypeRateMap(roomTypeId1, new BigDecimal("150.00"));
        
        Response response = given()
                .spec(authenticatedRequestSpec)
                .body(roomTypeRate)
                .when()
                .post("/rate-types/{rateTypeId}/room-type-rates", createdRateTypeId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(createdRateTypeId.intValue()))
                .extract()
                .response();
        
        List<Map<String, Object>> roomTypeRates = response.jsonPath().getList("roomTypeRates");
        assertTrue(roomTypeRates.size() > 0, "Should have at least one room type rate");
    }

    @Test
    @Order(81)
    @DisplayName("POST /api/rate-types/{rateTypeId}/room-type-rates - Add duplicate room type rate should fail")
    public void testAddRoomTypeRate_Duplicate() {
        if (createdRateTypeId == null || roomTypeId1 == null) {
            return; // Skip if prerequisites not met
        }
        
        // Try to add the same room type rate again
        Map<String, Object> roomTypeRate = createRoomTypeRateMap(roomTypeId1, new BigDecimal("160.00"));
        
        given()
                .spec(authenticatedRequestSpec)
                .body(roomTypeRate)
                .when()
                .post("/rate-types/{rateTypeId}/room-type-rates", createdRateTypeId)
                .then()
                .statusCode(400); // Should fail due to duplicate room type rate
    }

    @Test
    @Order(82)
    @DisplayName("PUT /api/rate-types/{rateTypeId}/room-type-rates/{roomTypeId} - Update room type rate successfully")
    public void testUpdateRoomTypeRate_Success() {
        if (createdRateTypeId == null || roomTypeId1 == null) {
            return; // Skip if prerequisites not met
        }
        
        BigDecimal newRate = new BigDecimal("175.00");
        
        Response response = given()
                .spec(authenticatedRequestSpec)
                .queryParam("rate", newRate)
                .when()
                .put("/rate-types/{rateTypeId}/room-type-rates/{roomTypeId}", createdRateTypeId, roomTypeId1)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(createdRateTypeId.intValue()))
                .extract()
                .response();
        
        // Verify the rate was updated
        List<Map<String, Object>> roomTypeRates = response.jsonPath().getList("roomTypeRates");
        boolean found = false;
        for (Map<String, Object> rate : roomTypeRates) {
            Long rtId = ((Number) rate.get("roomTypeId")).longValue();
            if (rtId.equals(roomTypeId1)) {
                BigDecimal rateValue = new BigDecimal(rate.get("rate").toString());
                assertEquals(0, newRate.compareTo(rateValue), "Rate should be updated");
                found = true;
                break;
            }
        }
        assertTrue(found, "Room type rate should be found in response");
    }

    @Test
    @Order(83)
    @DisplayName("GET /api/rate-types/{rateTypeId}/room-type-rates/{roomTypeId} - Get rate for room type successfully")
    public void testGetRateForRoomType_Success() {
        if (createdRateTypeId == null || roomTypeId1 == null) {
            return; // Skip if prerequisites not met
        }
        
        Response response = given()
                .spec(authenticatedRequestSpec)
                .when()
                .get("/rate-types/{rateTypeId}/room-type-rates/{roomTypeId}", createdRateTypeId, roomTypeId1)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();
        
        BigDecimal rate = new BigDecimal(response.getBody().asString());
        assertTrue(rate.compareTo(BigDecimal.ZERO) > 0, "Rate should be positive");
    }

    @Test
    @Order(84)
    @DisplayName("DELETE /api/rate-types/{rateTypeId}/room-type-rates/{roomTypeId} - Remove room type rate successfully")
    public void testRemoveRoomTypeRate_Success() {
        if (createdRateTypeId == null || roomTypeId2 == null) {
            return; // Skip if prerequisites not met
        }
        
        // First add a room type rate to delete
        Map<String, Object> roomTypeRate = createRoomTypeRateMap(roomTypeId2, new BigDecimal("200.00"));
        
        given()
                .spec(authenticatedRequestSpec)
                .body(roomTypeRate)
                .when()
                .post("/rate-types/{rateTypeId}/room-type-rates", createdRateTypeId)
                .then()
                .statusCode(200);
        
        // Now delete it
        given()
                .spec(authenticatedRequestSpec)
                .when()
                .delete("/rate-types/{rateTypeId}/room-type-rates/{roomTypeId}", createdRateTypeId, roomTypeId2)
                .then()
                .statusCode(204);
        
        // Verify it's deleted
        given()
                .spec(authenticatedRequestSpec)
                .when()
                .get("/rate-types/{rateTypeId}/room-type-rates/{roomTypeId}", createdRateTypeId, roomTypeId2)
                .then()
                .statusCode(400); // Should return 400 as rate doesn't exist
    }

    // ==================== DELETE OPERATIONS ====================

    @Test
    @Order(90)
    @DisplayName("DELETE /api/rate-types/{id} - Delete rate type successfully")
    public void testDeleteRateType_Success() {
        // First create a rate type to delete with unique name
        long timestamp = System.currentTimeMillis();
        String uniqueName = "DELETE_ME_TEST_" + timestamp;
        
        Response createResponse = given()
                .spec(authenticatedRequestSpec)
                .body(createRateTypeMap(uniqueName, "To be deleted"))
                .when()
                .post("/rate-types")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Long rateTypeIdToDelete = createResponse.jsonPath().getLong("id");

        // Delete the rate type
        given()
                .spec(authenticatedRequestSpec)
                .when()
                .delete("/rate-types/{id}", rateTypeIdToDelete)
                .then()
                .statusCode(204);
        
        // Remove from cleanup list since we already deleted it
        createdRateTypeIds.remove(rateTypeIdToDelete);

        // Verify rate type is deleted
        given()
                .spec(authenticatedRequestSpec)
                .when()
                .get("/rate-types/{id}", rateTypeIdToDelete)
                .then()
                .statusCode(400); // Should return 400 as rate type doesn't exist (via GlobalExceptionHandler)
    }

    @Test
    @Order(91)
    @DisplayName("DELETE /api/rate-types/{id} - Delete non-existent rate type should fail")
    public void testDeleteRateType_NotFound() {
        given()
                .spec(authenticatedRequestSpec)
                .when()
                .delete("/rate-types/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @AfterAll
    @DisplayName("Cleanup - Delete all test rate types created during test execution")
    public static void cleanupTestData() {
        logger.info("Starting cleanup of test data - {} rate type(s) to delete", createdRateTypeIds.size());
        
        int deletedCount = 0;
        int failedCount = 0;
        
        for (Long rateTypeId : createdRateTypeIds) {
            try {
                Response response = given()
                        .spec(authenticatedRequestSpec)
                        .when()
                        .delete("/rate-types/{id}", rateTypeId)
                        .then()
                        .extract()
                        .response();
                
                if (response.getStatusCode() == 204) {
                    deletedCount++;
                    logger.debug("Successfully deleted test rate type with ID: {}", rateTypeId);
                } else {
                    failedCount++;
                    logger.warn("Failed to delete test rate type with ID: {} - Status: {}", rateTypeId, response.getStatusCode());
                }
            } catch (Exception e) {
                failedCount++;
                logger.warn("Exception while deleting test rate type with ID: {} - {}", rateTypeId, e.getMessage());
            }
        }
        
        logger.info("Cleanup completed - Deleted: {}, Failed: {}, Total: {}", 
                deletedCount, failedCount, createdRateTypeIds.size());
        
        // Clear the list
        createdRateTypeIds.clear();
        createdRateTypeId = null;
        createdRateTypeName = null;
    }
}

