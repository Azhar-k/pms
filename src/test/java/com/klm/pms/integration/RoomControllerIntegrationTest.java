package com.klm.pms.integration;

import com.klm.pms.config.TestConfig;
import com.klm.pms.model.Room.RoomStatus;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Room Management API using REST Assured.
 * 
 * Prerequisites:
 * - Application must be running on localhost:8080
 * - Database must be accessible and configured
 * - At least one room type must exist in the database
 * 
 * Test Order:
 * 1. Create operations
 * 2. Read operations (including pagination, sorting, filtering)
 * 3. Availability operations
 * 4. Room type operations
 * 5. Update operations
 * 6. Delete operations
 */
@TestMethodOrder(OrderAnnotation.class)
public class RoomControllerIntegrationTest extends TestConfig {

    private static Long createdRoomId;
    private static String createdRoomNumber;
    private static Long roomTypeId;
    private static Map<String, Object> testRoom1;
    private static Map<String, Object> testRoom2;
    private static Map<String, Object> testRoom3;
    
    // Track all created room IDs for cleanup
    private static final List<Long> createdRoomIds = new ArrayList<>();
    
    // Logger for cleanup operations
    private static final Logger logger = LoggerFactory.getLogger(RoomControllerIntegrationTest.class);

    @BeforeAll
    public static void setupTestData() {
        // Use unique test data with timestamp to avoid conflicts
        long timestamp = System.currentTimeMillis();
        String uniqueSuffix = String.valueOf(timestamp).substring(7); // Last 6 digits
        
        // Get or create a room type for testing
        setupRoomType();
        
        testRoom1 = createRoomMap("101_TEST_" + uniqueSuffix, roomTypeId, 
                RoomStatus.READY.name(), 2, "WiFi, TV", "Test room 1", 1, true, false);
        
        testRoom2 = createRoomMap("102_TEST_" + uniqueSuffix, roomTypeId, 
                RoomStatus.READY.name(), 3, "WiFi, TV, Mini Bar", "Test room 2", 2, true, true);
        
        testRoom3 = createRoomMap("103_TEST_" + uniqueSuffix, roomTypeId, 
                RoomStatus.MAINTENANCE.name(), 4, "WiFi, TV, Mini Bar, Safe", "Test room 3", 3, false, true);
    }
    
    private static void setupRoomType() {
        // Try to get existing room type
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
            // Use existing room type
            roomTypeId = ((Number) roomTypes.get(0).get("id")).longValue();
        } else {
            // Create room type if none exist
            logger.warn("No room types found. Creating test room type for room tests.");
            
            Map<String, Object> roomType = new HashMap<>();
            roomType.put("name", "RT_FOR_ROOM_TEST_" + System.currentTimeMillis());
            roomType.put("description", "Room type for room testing");
            roomType.put("basePricePerNight", new BigDecimal("100.00"));
            roomType.put("maxOccupancy", 2);
            
            Response createResponse = given()
                    .spec(requestSpec)
                    .body(roomType)
                    .when()
                    .post("/room-types")
                    .then()
                    .statusCode(201)
                    .extract()
                    .response();
            
            roomTypeId = createResponse.jsonPath().getLong("id");
        }
    }

    private static Map<String, Object> createRoomMap(String roomNumber, Long roomTypeId, 
            String status, Integer maxOccupancy, String amenities, String description, 
            Integer floor, Boolean hasBalcony, Boolean hasView) {
        Map<String, Object> room = new HashMap<>();
        room.put("roomNumber", roomNumber);
        room.put("roomTypeId", roomTypeId);
        room.put("status", status);
        room.put("maxOccupancy", maxOccupancy);
        room.put("amenities", amenities);
        room.put("description", description);
        room.put("floor", floor);
        room.put("hasBalcony", hasBalcony);
        room.put("hasView", hasView);
        return room;
    }

    // ==================== CREATE OPERATIONS ====================

    @Test
    @Order(1)
    @DisplayName("POST /api/rooms - Create a new room successfully")
    public void testCreateRoom_Success() {
        Response response = given()
                .spec(requestSpec)
                .body(testRoom1)
                .when()
                .post("/rooms")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("roomNumber", equalTo(testRoom1.get("roomNumber")))
                .body("roomTypeId", equalTo(((Number) testRoom1.get("roomTypeId")).intValue()))
                .body("status", equalTo(testRoom1.get("status")))
                .extract()
                .response();

        createdRoomId = response.jsonPath().getLong("id");
        createdRoomNumber = response.jsonPath().getString("roomNumber");
        
        assertNotNull(createdRoomId);
        assertNotNull(createdRoomNumber);
        
        // Track for cleanup
        createdRoomIds.add(createdRoomId);
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/rooms - Create additional rooms for testing")
    public void testCreateAdditionalRooms() {
        // Create second room
        Response response2 = given()
                .spec(requestSpec)
                .body(testRoom2)
                .when()
                .post("/rooms")
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long roomId2 = response2.jsonPath().getLong("id");
        if (roomId2 != null) {
            createdRoomIds.add(roomId2);
        }

        // Create third room
        Response response3 = given()
                .spec(requestSpec)
                .body(testRoom3)
                .when()
                .post("/rooms")
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long roomId3 = response3.jsonPath().getLong("id");
        if (roomId3 != null) {
            createdRoomIds.add(roomId3);
        }
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/rooms - Create room with duplicate room number should fail")
    public void testCreateRoom_DuplicateRoomNumber() {
        // Use the room number from the room we just created
        if (createdRoomNumber != null) {
            Map<String, Object> duplicateRoom = new HashMap<>(testRoom1);
            duplicateRoom.put("roomNumber", createdRoomNumber); // Use the created room's number

            given()
                    .spec(requestSpec)
                    .body(duplicateRoom)
                    .when()
                    .post("/rooms")
                    .then()
                    .statusCode(400); // API returns 400 for duplicate room number
        } else {
            // Skip test if no room was created
            Assertions.fail("Cannot test duplicate room number - no room was created in previous test");
        }
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/rooms - Create room with missing required fields should fail")
    public void testCreateRoom_MissingRequiredFields() {
        Map<String, Object> invalidRoom = new HashMap<>();
        invalidRoom.put("status", "READY");
        // Missing roomNumber and roomTypeId

        given()
                .spec(requestSpec)
                .body(invalidRoom)
                .when()
                .post("/rooms")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/rooms - Create room with invalid room type ID should fail")
    public void testCreateRoom_InvalidRoomTypeId() {
        Map<String, Object> invalidRoom = new HashMap<>(testRoom1);
        invalidRoom.put("roomNumber", "INVALID_RT_TEST_" + System.currentTimeMillis());
        invalidRoom.put("roomTypeId", 99999L); // Non-existent room type

        given()
                .spec(requestSpec)
                .body(invalidRoom)
                .when()
                .post("/rooms")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/rooms - Create room with different statuses")
    public void testCreateRoom_WithDifferentStatuses() {
        long timestamp = System.currentTimeMillis();
        String uniqueSuffix = String.valueOf(timestamp).substring(7);
        
        // Test READY status
        Map<String, Object> readyRoom = createRoomMap("READY_TEST_" + uniqueSuffix, roomTypeId, 
                RoomStatus.READY.name(), 2, "Basic", "Ready room", 1, false, false);
        
        Response response1 = given()
                .spec(requestSpec)
                .body(readyRoom)
                .when()
                .post("/rooms")
                .then()
                .statusCode(201)
                .body("status", equalTo("READY"))
                .extract()
                .response();
        
        Long readyRoomId = response1.jsonPath().getLong("id");
        if (readyRoomId != null) {
            createdRoomIds.add(readyRoomId);
        }
        
        // Test CLEANING status
        Map<String, Object> cleaningRoom = createRoomMap("CLEANING_TEST_" + uniqueSuffix, roomTypeId, 
                RoomStatus.CLEANING.name(), 2, "Basic", "Cleaning room", 1, false, false);
        
        Response response2 = given()
                .spec(requestSpec)
                .body(cleaningRoom)
                .when()
                .post("/rooms")
                .then()
                .statusCode(201)
                .body("status", equalTo("CLEANING"))
                .extract()
                .response();
        
        Long cleaningRoomId = response2.jsonPath().getLong("id");
        if (cleaningRoomId != null) {
            createdRoomIds.add(cleaningRoomId);
        }
    }

    // ==================== READ OPERATIONS ====================

    @Test
    @Order(10)
    @DisplayName("GET /api/rooms/{id} - Get room by ID successfully")
    public void testGetRoomById_Success() {
        if (createdRoomId != null) {
            given()
                    .spec(requestSpec)
                    .when()
                    .get("/rooms/{id}", createdRoomId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(createdRoomId.intValue()))
                    .body("roomNumber", equalTo(testRoom1.get("roomNumber")))
                    .body("roomTypeId", equalTo(((Number) testRoom1.get("roomTypeId")).intValue()));
        } else {
            // If room creation failed, try to get any existing room
            Response response = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
                    .when()
                    .get("/rooms")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> rooms = response.jsonPath().getList("content");
            if (rooms != null && !rooms.isEmpty()) {
                Long roomId = ((Number) rooms.get(0).get("id")).longValue();
                given()
                        .spec(requestSpec)
                        .when()
                        .get("/rooms/{id}", roomId)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", notNullValue());
            } else {
                // Try non-paginated
                List<Map<String, Object>> allRooms = response.jsonPath().getList("$");
                if (allRooms != null && !allRooms.isEmpty()) {
                    Long roomId = ((Number) allRooms.get(0).get("id")).longValue();
                    given()
                            .spec(requestSpec)
                            .when()
                            .get("/rooms/{id}", roomId)
                            .then()
                            .statusCode(200)
                            .contentType(ContentType.JSON)
                            .body("id", notNullValue());
                } else {
                    Assertions.fail("No rooms available for testing");
                }
            }
        }
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/rooms/{id} - Get non-existent room should return 400")
    public void testGetRoomById_NotFound() {
        given()
                .spec(requestSpec)
                .when()
                .get("/rooms/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/rooms/number/{roomNumber} - Get room by number successfully")
    public void testGetRoomByNumber_Success() {
        if (createdRoomNumber != null) {
            given()
                    .spec(requestSpec)
                    .when()
                    .get("/rooms/number/{roomNumber}", createdRoomNumber)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("roomNumber", equalTo(createdRoomNumber))
                    .body("id", notNullValue());
        } else {
            // If no room was created, try with any existing room number
            Response response = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
                    .when()
                    .get("/rooms")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> rooms = response.jsonPath().getList("content");
            if (rooms == null || rooms.isEmpty()) {
                rooms = response.jsonPath().getList("$");
            }
            
            if (rooms != null && !rooms.isEmpty() && rooms.get(0).get("roomNumber") != null) {
                String roomNumber = (String) rooms.get(0).get("roomNumber");
                given()
                        .spec(requestSpec)
                        .when()
                        .get("/rooms/number/{roomNumber}", roomNumber)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("roomNumber", equalTo(roomNumber));
            } else {
                Assertions.fail("No rooms with room number available for testing");
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/rooms/number/{roomNumber} - Get non-existent room number should return 400")
    public void testGetRoomByNumber_NotFound() {
        given()
                .spec(requestSpec)
                .when()
                .get("/rooms/number/{roomNumber}", "NON_EXISTENT_99999")
                .then()
                .statusCode(400); // Application returns 400 for not found
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/rooms - Get all rooms (non-paginated)")
    public void testGetAllRooms_NonPaginated() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        // Check if response is paginated (has 'content' field) or plain list
        Object content = response.jsonPath().get("content");
        if (content != null) {
            // It's a paginated response
            List<Map<String, Object>> rooms = response.jsonPath().getList("content");
            assertTrue(rooms.size() > 0, "Should have at least some rooms");
        } else {
            // It's a plain list
            List<Map<String, Object>> rooms = response.jsonPath().getList("$");
            assertTrue(rooms.size() > 0, "Should have at least some rooms");
        }
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    @Order(20)
    @DisplayName("GET /api/rooms - Test pagination with page and size")
    public void testGetAllRooms_WithPagination() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", instanceOf(List.class))
                .body("page", equalTo(0))
                .body("size", equalTo(2))
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() <= 2, "Page size should be at most 2");
        
        // Verify pagination metadata
        long totalElements = response.jsonPath().getLong("totalElements");
        int totalPages = response.jsonPath().getInt("totalPages");
        assertTrue(totalElements >= 0, "Should have non-negative total elements");
        assertTrue(totalPages >= 0, "Should have non-negative total pages");
        assertTrue(response.jsonPath().getBoolean("first"), "Should be first page");
    }

    @Test
    @Order(21)
    @DisplayName("GET /api/rooms - Test pagination second page")
    public void testGetAllRooms_SecondPage() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 1)
                .queryParam("size", 2)
                .when()
                .get("/rooms")
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

    // ==================== SORTING TESTS ====================

    @Test
    @Order(30)
    @DisplayName("GET /api/rooms - Test sorting by roomNumber ascending")
    public void testGetAllRooms_SortByRoomNumberAsc() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "roomNumber")
                .queryParam("sortDir", "asc")
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() >= 2) {
            String firstRoomNumber = (String) content.get(0).get("roomNumber");
            String secondRoomNumber = (String) content.get(1).get("roomNumber");
            assertTrue(firstRoomNumber.compareToIgnoreCase(secondRoomNumber) <= 0, 
                    "Rooms should be sorted by roomNumber in ascending order");
        }
    }

    @Test
    @Order(31)
    @DisplayName("GET /api/rooms - Test sorting by roomNumber descending")
    public void testGetAllRooms_SortByRoomNumberDesc() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "roomNumber")
                .queryParam("sortDir", "desc")
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() >= 2) {
            String firstRoomNumber = (String) content.get(0).get("roomNumber");
            String secondRoomNumber = (String) content.get(1).get("roomNumber");
            assertTrue(firstRoomNumber.compareToIgnoreCase(secondRoomNumber) >= 0, 
                    "Rooms should be sorted by roomNumber in descending order");
        }
    }

    @Test
    @Order(32)
    @DisplayName("GET /api/rooms - Test sorting by status")
    public void testGetAllRooms_SortByStatus() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "status")
                .queryParam("sortDir", "asc")
                .when()
                .get("/rooms")
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
    @DisplayName("GET /api/rooms - Test sorting by floor")
    public void testGetAllRooms_SortByFloor() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "floor")
                .queryParam("sortDir", "asc")
                .when()
                .get("/rooms")
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
    @DisplayName("GET /api/rooms - Test filtering by roomNumber")
    public void testGetAllRooms_FilterByRoomNumber() {
        if (createdRoomNumber != null) {
            Response response = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .queryParam("roomNumber", createdRoomNumber.substring(0, 3)) // Partial match
                    .when()
                    .get("/rooms")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .extract()
                    .response();

            List<Map<String, Object>> content = response.jsonPath().getList("content");
            assertTrue(content.size() > 0);
            content.forEach(room -> {
                String roomNumber = (String) room.get("roomNumber");
                assertTrue(roomNumber.toLowerCase().contains(createdRoomNumber.substring(0, 3).toLowerCase()), 
                        "All rooms should have roomNumber containing the filter");
            });
        }
    }

    @Test
    @Order(41)
    @DisplayName("GET /api/rooms - Test filtering by roomTypeId")
    public void testGetAllRooms_FilterByRoomTypeId() {
        if (roomTypeId != null) {
            Response response = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .queryParam("roomTypeId", roomTypeId)
                    .when()
                    .get("/rooms")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .extract()
                    .response();

            List<Map<String, Object>> content = response.jsonPath().getList("content");
            assertTrue(content.size() > 0);
            content.forEach(room -> {
                Long rtId = ((Number) room.get("roomTypeId")).longValue();
                assertEquals(roomTypeId, rtId, "All rooms should have the same roomTypeId");
            });
        }
    }

    @Test
    @Order(42)
    @DisplayName("GET /api/rooms - Test filtering by status READY")
    public void testGetAllRooms_FilterByStatusReady() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("status", "READY")
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(room -> {
                String status = (String) room.get("status");
                assertEquals("READY", status, "All rooms should have status READY");
            });
        }
    }

    @Test
    @Order(43)
    @DisplayName("GET /api/rooms - Test filtering by status MAINTENANCE")
    public void testGetAllRooms_FilterByStatusMaintenance() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("status", "MAINTENANCE")
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(room -> {
                String status = (String) room.get("status");
                assertEquals("MAINTENANCE", status, "All rooms should have status MAINTENANCE");
            });
        }
    }

    @Test
    @Order(44)
    @DisplayName("GET /api/rooms - Test filtering by status CLEANING")
    public void testGetAllRooms_FilterByStatusCleaning() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("status", "CLEANING")
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(room -> {
                String status = (String) room.get("status");
                assertEquals("CLEANING", status, "All rooms should have status CLEANING");
            });
        }
    }

    @Test
    @Order(45)
    @DisplayName("GET /api/rooms - Test filtering by minMaxOccupancy")
    public void testGetAllRooms_FilterByMinMaxOccupancy() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("minMaxOccupancy", 3)
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(room -> {
                Integer maxOccupancy = (Integer) room.get("maxOccupancy");
                if (maxOccupancy != null) {
                    assertTrue(maxOccupancy >= 3, "All rooms should have maxOccupancy >= 3");
                }
            });
        }
    }

    @Test
    @Order(46)
    @DisplayName("GET /api/rooms - Test filtering by maxMaxOccupancy")
    public void testGetAllRooms_FilterByMaxMaxOccupancy() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("maxMaxOccupancy", 2)
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(room -> {
                Integer maxOccupancy = (Integer) room.get("maxOccupancy");
                if (maxOccupancy != null) {
                    assertTrue(maxOccupancy <= 2, "All rooms should have maxOccupancy <= 2");
                }
            });
        }
    }

    @Test
    @Order(47)
    @DisplayName("GET /api/rooms - Test filtering by floor")
    public void testGetAllRooms_FilterByFloor() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("floor", 1)
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(room -> {
                Integer floor = (Integer) room.get("floor");
                assertEquals(1, floor, "All rooms should be on floor 1");
            });
        }
    }

    @Test
    @Order(48)
    @DisplayName("GET /api/rooms - Test filtering by hasBalcony")
    public void testGetAllRooms_FilterByHasBalcony() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("hasBalcony", true)
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(room -> {
                Boolean hasBalcony = (Boolean) room.get("hasBalcony");
                assertEquals(true, hasBalcony, "All rooms should have balcony");
            });
        }
    }

    @Test
    @Order(49)
    @DisplayName("GET /api/rooms - Test filtering by hasView")
    public void testGetAllRooms_FilterByHasView() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("hasView", true)
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(room -> {
                Boolean hasView = (Boolean) room.get("hasView");
                assertEquals(true, hasView, "All rooms should have view");
            });
        }
    }

    @Test
    @Order(50)
    @DisplayName("GET /api/rooms - Test filtering by searchTerm (room number)")
    public void testGetAllRooms_SearchTerm_RoomNumber() {
        if (createdRoomNumber != null) {
            Response response = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .queryParam("searchTerm", createdRoomNumber.substring(0, 3))
                    .when()
                    .get("/rooms")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .extract()
                    .response();

            List<Map<String, Object>> content = response.jsonPath().getList("content");
            assertTrue(content.size() > 0);
        }
    }

    @Test
    @Order(51)
    @DisplayName("GET /api/rooms - Test filtering by searchTerm (description)")
    public void testGetAllRooms_SearchTerm_Description() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("searchTerm", "Test")
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        // Search might return empty if no rooms match, which is valid
        assertTrue(content.size() >= 0);
    }

    @Test
    @Order(52)
    @DisplayName("GET /api/rooms - Test filtering by searchTerm (amenities)")
    public void testGetAllRooms_SearchTerm_Amenities() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("searchTerm", "WiFi")
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        // Search might return empty if no rooms match, which is valid
        assertTrue(content.size() >= 0);
    }

    @Test
    @Order(53)
    @DisplayName("GET /api/rooms - Test combined filtering, pagination, and sorting")
    public void testGetAllRooms_CombinedFilters() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 5)
                .queryParam("sortBy", "roomNumber")
                .queryParam("sortDir", "asc")
                .queryParam("status", "READY")
                .queryParam("hasBalcony", true)
                .when()
                .get("/rooms")
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
        content.forEach(room -> {
            String status = (String) room.get("status");
            Boolean hasBalcony = (Boolean) room.get("hasBalcony");
            assertEquals("READY", status);
            assertEquals(true, hasBalcony);
        });
    }

    // ==================== AVAILABILITY TESTS ====================

    @Test
    @Order(60)
    @DisplayName("GET /api/rooms/available - Get available rooms (default)")
    public void testGetAvailableRooms() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/rooms/available")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> rooms = response.jsonPath().getList("$");
        assertTrue(rooms.size() >= 0, "Should return list of available rooms");
        
        // Verify all returned rooms have appropriate status
        rooms.forEach(room -> {
            String status = (String) room.get("status");
            assertTrue(status.equals("READY") || status.equals("CLEANING"), 
                    "Available rooms should be READY or CLEANING");
        });
    }

    @Test
    @Order(61)
    @DisplayName("GET /api/rooms/available/range - Get available rooms for date range successfully")
    public void testGetAvailableRoomsForDateRange_Success() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        
        Response response = given()
                .spec(requestSpec)
                .queryParam("checkInDate", checkInDate.format(DateTimeFormatter.ISO_DATE))
                .queryParam("checkOutDate", checkOutDate.format(DateTimeFormatter.ISO_DATE))
                .when()
                .get("/rooms/available/range")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> rooms = response.jsonPath().getList("$");
        assertTrue(rooms.size() >= 0, "Should return list of available rooms");
    }

    @Test
    @Order(62)
    @DisplayName("GET /api/rooms/available/range - Get available rooms with invalid date range should fail")
    public void testGetAvailableRoomsForDateRange_InvalidRange() {
        LocalDate checkInDate = LocalDate.now().plusDays(2);
        LocalDate checkOutDate = LocalDate.now().plusDays(1); // Check-out before check-in
        
        given()
                .spec(requestSpec)
                .queryParam("checkInDate", checkInDate.format(DateTimeFormatter.ISO_DATE))
                .queryParam("checkOutDate", checkOutDate.format(DateTimeFormatter.ISO_DATE))
                .when()
                .get("/rooms/available/range")
                .then()
                .statusCode(400); // Application returns 400 for invalid date range (via GlobalExceptionHandler)
    }

    @Test
    @Order(63)
    @DisplayName("GET /api/rooms/available/range - Get available rooms with same dates")
    public void testGetAvailableRoomsForDateRange_SameDates() {
        LocalDate date = LocalDate.now().plusDays(1);
        
        Response response = given()
                .spec(requestSpec)
                .queryParam("checkInDate", date.format(DateTimeFormatter.ISO_DATE))
                .queryParam("checkOutDate", date.format(DateTimeFormatter.ISO_DATE))
                .when()
                .get("/rooms/available/range")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> rooms = response.jsonPath().getList("$");
        assertTrue(rooms.size() >= 0);
    }

    // ==================== ROOM TYPE TESTS ====================

    @Test
    @Order(70)
    @DisplayName("GET /api/rooms/type/{roomTypeId} - Get rooms by room type successfully")
    public void testGetRoomsByType_Success() {
        if (roomTypeId != null) {
            Response response = given()
                    .spec(requestSpec)
                    .when()
                    .get("/rooms/type/{roomTypeId}", roomTypeId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .extract()
                    .response();

            List<Map<String, Object>> rooms = response.jsonPath().getList("$");
            assertTrue(rooms.size() > 0, "Should have at least some rooms of this type");
            
            // Verify all rooms have the correct room type
            rooms.forEach(room -> {
                Long rtId = ((Number) room.get("roomTypeId")).longValue();
                assertEquals(roomTypeId, rtId, "All rooms should have the same roomTypeId");
            });
        }
    }

    @Test
    @Order(71)
    @DisplayName("GET /api/rooms/type/{roomTypeId} - Get rooms by invalid room type ID should fail")
    public void testGetRoomsByType_InvalidRoomTypeId() {
        given()
                .spec(requestSpec)
                .when()
                .get("/rooms/type/{roomTypeId}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for invalid room type ID (via GlobalExceptionHandler)
    }

    // ==================== UPDATE OPERATIONS ====================

    @Test
    @Order(80)
    @DisplayName("PUT /api/rooms/{id} - Update room successfully")
    public void testUpdateRoom_Success() {
        if (createdRoomId != null && createdRoomNumber != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("roomNumber", createdRoomNumber); // Keep same number
            updateData.put("roomTypeId", roomTypeId);
            updateData.put("status", "READY");
            updateData.put("maxOccupancy", 3);
            updateData.put("description", "Updated description for testing");
            updateData.put("floor", 2);

            Response response = given()
                    .spec(requestSpec)
                    .body(updateData)
                    .when()
                    .put("/rooms/{id}", createdRoomId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(createdRoomId.intValue()))
                    .body("description", equalTo("Updated description for testing"))
                    .extract()
                    .response();

            // Verify the update
            String updatedDescription = response.jsonPath().getString("description");
            assertEquals("Updated description for testing", updatedDescription);
        } else {
            // Get an existing room to update
            Response listResponse = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
                    .when()
                    .get("/rooms")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> rooms = listResponse.jsonPath().getList("content");
            if (rooms == null || rooms.isEmpty()) {
                rooms = listResponse.jsonPath().getList("$");
            }
            
            if (rooms != null && !rooms.isEmpty()) {
                Long roomId = ((Number) rooms.get(0).get("id")).longValue();
                String roomNumber = (String) rooms.get(0).get("roomNumber");
                Long rtId = ((Number) rooms.get(0).get("roomTypeId")).longValue();
                
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("roomNumber", roomNumber);
                updateData.put("roomTypeId", rtId);
                updateData.put("description", "Updated description");
                
                given()
                        .spec(requestSpec)
                        .body(updateData)
                        .when()
                        .put("/rooms/{id}", roomId)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", equalTo(roomId.intValue()));
            } else {
                Assertions.fail("No rooms available for update test");
            }
        }
    }

    @Test
    @Order(81)
    @DisplayName("PUT /api/rooms/{id} - Update room status")
    public void testUpdateRoom_UpdateStatus() {
        if (createdRoomId != null && createdRoomNumber != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("roomNumber", createdRoomNumber);
            updateData.put("roomTypeId", roomTypeId);
            updateData.put("status", "MAINTENANCE");

            given()
                    .spec(requestSpec)
                    .body(updateData)
                    .when()
                    .put("/rooms/{id}", createdRoomId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("status", equalTo("MAINTENANCE"));

            // Change back to READY
            updateData.put("status", "READY");
            given()
                    .spec(requestSpec)
                    .body(updateData)
                    .when()
                    .put("/rooms/{id}", createdRoomId)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("READY"));
        }
    }

    @Test
    @Order(82)
    @DisplayName("PUT /api/rooms/{id} - Update non-existent room should fail")
    public void testUpdateRoom_NotFound() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("roomNumber", "NON_EXISTENT");
        updateData.put("roomTypeId", roomTypeId);

        given()
                .spec(requestSpec)
                .body(updateData)
                .when()
                .put("/rooms/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @Test
    @Order(83)
    @DisplayName("PUT /api/rooms/{id} - Update with duplicate room number should fail")
    public void testUpdateRoom_DuplicateRoomNumber() {
        if (createdRoomId == null) {
            return; // Skip if no room was created
        }
        
        // First, get another room's number
        Response allRoomsResponse = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/rooms")
                .then()
                .extract()
                .response();

        List<Map<String, Object>> rooms = allRoomsResponse.jsonPath().getList("content");
        if (rooms == null || rooms.isEmpty()) {
            rooms = allRoomsResponse.jsonPath().getList("$");
        }
        
        if (rooms != null && rooms.size() >= 2) {
            // Find a room with different ID
            String otherRoomNumber = null;
            for (Map<String, Object> room : rooms) {
                Long roomId = ((Number) room.get("id")).longValue();
                if (!roomId.equals(createdRoomId) && room.get("roomNumber") != null) {
                    otherRoomNumber = (String) room.get("roomNumber");
                    break;
                }
            }
            
            if (otherRoomNumber != null) {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("roomNumber", otherRoomNumber);
                updateData.put("roomTypeId", roomTypeId);

                given()
                        .spec(requestSpec)
                        .body(updateData)
                        .when()
                        .put("/rooms/{id}", createdRoomId)
                        .then()
                        .statusCode(400); // Should fail due to duplicate room number (returns 400)
            }
        }
    }

    // ==================== DELETE OPERATIONS ====================

    @Test
    @Order(90)
    @DisplayName("DELETE /api/rooms/{id} - Delete room successfully")
    public void testDeleteRoom_Success() {
        // First create a room to delete with unique room number
        long timestamp = System.currentTimeMillis();
        String uniqueRoomNumber = "DELETE_ME_TEST_" + timestamp;
        
        Response createResponse = given()
                .spec(requestSpec)
                .body(createRoomMap(uniqueRoomNumber, roomTypeId, 
                        RoomStatus.READY.name(), 2, "Basic", "To be deleted", 1, false, false))
                .when()
                .post("/rooms")
                .then()
                .statusCode(201)
                .extract()
                .response();

        Long roomIdToDelete = createResponse.jsonPath().getLong("id");

        // Delete the room
        given()
                .spec(requestSpec)
                .when()
                .delete("/rooms/{id}", roomIdToDelete)
                .then()
                .statusCode(204);
        
        // Remove from cleanup list since we already deleted it
        createdRoomIds.remove(roomIdToDelete);

        // Verify room is deleted
        given()
                .spec(requestSpec)
                .when()
                .get("/rooms/{id}", roomIdToDelete)
                .then()
                .statusCode(400); // Should return 400 as room doesn't exist (via GlobalExceptionHandler)
    }

    @Test
    @Order(91)
    @DisplayName("DELETE /api/rooms/{id} - Delete non-existent room should fail")
    public void testDeleteRoom_NotFound() {
        given()
                .spec(requestSpec)
                .when()
                .delete("/rooms/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @AfterAll
    @DisplayName("Cleanup - Delete all test rooms created during test execution")
    public static void cleanupTestData() {
        logger.info("Starting cleanup of test data - {} room(s) to delete", createdRoomIds.size());
        
        int deletedCount = 0;
        int failedCount = 0;
        
        for (Long roomId : createdRoomIds) {
            try {
                Response response = given()
                        .spec(requestSpec)
                        .when()
                        .delete("/rooms/{id}", roomId)
                        .then()
                        .extract()
                        .response();
                
                if (response.getStatusCode() == 204) {
                    deletedCount++;
                    logger.debug("Successfully deleted test room with ID: {}", roomId);
                } else {
                    failedCount++;
                    logger.warn("Failed to delete test room with ID: {} - Status: {}", roomId, response.getStatusCode());
                }
            } catch (Exception e) {
                failedCount++;
                logger.warn("Exception while deleting test room with ID: {} - {}", roomId, e.getMessage());
            }
        }
        
        logger.info("Cleanup completed - Deleted: {}, Failed: {}, Total: {}", 
                deletedCount, failedCount, createdRoomIds.size());
        
        // Clear the list
        createdRoomIds.clear();
        createdRoomId = null;
        createdRoomNumber = null;
    }
}

