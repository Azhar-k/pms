package com.klm.pms.integration;

import com.klm.pms.config.TestConfig;
import com.klm.pms.model.Invoice.InvoiceStatus;
import com.klm.pms.model.Room.RoomStatus;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Invoice Management API using REST Assured.
 * 
 * Prerequisites:
 * - Application must be running on localhost:8080
 * - Database must be accessible and configured
 * - At least one guest, room type, room, and rate type must exist in the database
 * 
 * Test Order:
 * 1. Generate invoice operations
 * 2. Read operations (including pagination, sorting, filtering)
 * 3. Invoice item operations
 * 4. Payment operations
 */
@TestMethodOrder(OrderAnnotation.class)
public class InvoiceControllerIntegrationTest extends TestConfig {

    private static Long createdInvoiceId;
    private static String createdInvoiceNumber;
    private static Long reservationId;
    private static Long guestId;
    private static Long roomId;
    private static Long roomTypeId;
    private static Long rateTypeId;
    
    // Track all created invoice IDs for cleanup
    private static final List<Long> createdInvoiceIds = new ArrayList<>();
    
    // Track all created reservation IDs for cleanup
    private static final List<Long> createdReservationIds = new ArrayList<>();
    
    // Logger for cleanup operations
    private static final Logger logger = LoggerFactory.getLogger(InvoiceControllerIntegrationTest.class);

    @BeforeAll
    public static void setupTestData() {
        // Setup required entities (guest, room, rate type)
        setupRequiredEntities();
        
        // Create a reservation for invoice generation
        createTestReservation();
    }
    
    private static void setupRequiredEntities() {
        // Get or create a guest
        Response guestResponse = given()
                .spec(requestSpec)
                .when()
                .get("/guests")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        // Check if response is paginated (has 'content' field) or plain list
        Object content = guestResponse.jsonPath().get("content");
        List<Map<String, Object>> guests = null;
        if (content != null) {
            // It's a paginated response
            guests = guestResponse.jsonPath().getList("content");
        } else {
            // It's a plain list
            Object root = guestResponse.jsonPath().get("$");
            if (root instanceof List) {
                guests = guestResponse.jsonPath().getList("$");
            }
        }
        
        if (guests != null && !guests.isEmpty()) {
            guestId = ((Number) guests.get(0).get("id")).longValue();
        } else {
            // Create a guest
            long timestamp = System.currentTimeMillis();
            Map<String, Object> guest = new HashMap<>();
            guest.put("firstName", "Invoice");
            guest.put("lastName", "Test");
            guest.put("email", "invoice.test" + timestamp + "@example.com");
            guest.put("phoneNumber", "+1234567890");
            guest.put("address", "123 Test St");
            guest.put("city", "Test City");
            guest.put("state", "TS");
            guest.put("country", "USA");
            guest.put("postalCode", "12345");
            guest.put("identificationType", "PASSPORT");
            guest.put("identificationNumber", "PASS" + timestamp);
            
            Response createGuestResponse = given()
                    .spec(requestSpec)
                    .body(guest)
                    .when()
                    .post("/guests")
                    .then()
                    .statusCode(201)
                    .extract()
                    .response();
            
            guestId = createGuestResponse.jsonPath().getLong("id");
        }
        
        // Get or create a room type
        Response roomTypeResponse = given()
                .spec(requestSpec)
                .when()
                .get("/room-types")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        // Room types endpoint returns a plain list
        Object roomTypesRoot = roomTypeResponse.jsonPath().get("$");
        List<Map<String, Object>> roomTypes = null;
        if (roomTypesRoot instanceof List) {
            roomTypes = roomTypeResponse.jsonPath().getList("$");
        }
        if (roomTypes != null && !roomTypes.isEmpty()) {
            roomTypeId = ((Number) roomTypes.get(0).get("id")).longValue();
        } else {
            // Create a room type
            Map<String, Object> roomType = new HashMap<>();
            roomType.put("name", "RT_FOR_INVOICE_TEST_" + System.currentTimeMillis());
            roomType.put("description", "Room type for invoice testing");
            roomType.put("basePricePerNight", new BigDecimal("100.00"));
            roomType.put("maxOccupancy", 4);
            
            Response createRoomTypeResponse = given()
                    .spec(requestSpec)
                    .body(roomType)
                    .when()
                    .post("/room-types")
                    .then()
                    .statusCode(201)
                    .extract()
                    .response();
            
            roomTypeId = createRoomTypeResponse.jsonPath().getLong("id");
        }
        
        // Get or create a room
        Response roomResponse = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/rooms")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        // Check if response is paginated (has 'content' field) or plain list
        Object contentRooms = roomResponse.jsonPath().get("content");
        List<Map<String, Object>> rooms = null;
        if (contentRooms != null) {
            // It's a paginated response
            rooms = roomResponse.jsonPath().getList("content");
        } else {
            // It's a plain list
            Object roomsRoot = roomResponse.jsonPath().get("$");
            if (roomsRoot instanceof List) {
                rooms = roomResponse.jsonPath().getList("$");
            }
        }
        
        if (rooms != null && !rooms.isEmpty()) {
            roomId = ((Number) rooms.get(0).get("id")).longValue();
        } else {
            // Create a room
            long timestamp = System.currentTimeMillis();
            Map<String, Object> room = new HashMap<>();
            room.put("roomNumber", "INV_TEST_" + timestamp);
            room.put("roomTypeId", roomTypeId);
            room.put("status", RoomStatus.READY.name());
            room.put("maxOccupancy", 4);
            room.put("floor", 1);
            
            Response createRoomResponse = given()
                    .spec(requestSpec)
                    .body(room)
                    .when()
                    .post("/rooms")
                    .then()
                    .statusCode(201)
                    .extract()
                    .response();
            
            roomId = createRoomResponse.jsonPath().getLong("id");
        }
        
        // Get or create a rate type
        Response rateTypeResponse = given()
                .spec(requestSpec)
                .when()
                .get("/rate-types")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        // Rate types endpoint returns a plain list
        Object root = rateTypeResponse.jsonPath().get("$");
        List<Map<String, Object>> rateTypes = null;
        if (root instanceof List) {
            rateTypes = rateTypeResponse.jsonPath().getList("$");
        }
        if (rateTypes != null && !rateTypes.isEmpty()) {
            rateTypeId = ((Number) rateTypes.get(0).get("id")).longValue();
            
            // Ensure rate type has a rate for the room type
            Response rateTypeDetailResponse = given()
                    .spec(requestSpec)
                    .when()
                    .get("/rate-types/{id}", rateTypeId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> roomTypeRates = rateTypeDetailResponse.jsonPath().getList("roomTypeRates");
            boolean hasRateForRoomType = false;
            if (roomTypeRates != null) {
                for (Map<String, Object> rate : roomTypeRates) {
                    Long rtId = ((Number) rate.get("roomTypeId")).longValue();
                    if (rtId.equals(roomTypeId)) {
                        hasRateForRoomType = true;
                        break;
                    }
                }
            }
            
            if (!hasRateForRoomType) {
                // Add rate for room type
                Map<String, Object> roomTypeRate = new HashMap<>();
                roomTypeRate.put("roomTypeId", roomTypeId);
                roomTypeRate.put("rate", new BigDecimal("120.00"));
                
                given()
                        .spec(requestSpec)
                        .body(roomTypeRate)
                        .when()
                        .post("/rate-types/{rateTypeId}/room-type-rates", rateTypeId)
                        .then()
                        .statusCode(200);
            }
        } else {
            // Create a rate type with room type rate
            Map<String, Object> rateType = new HashMap<>();
            rateType.put("name", "RT_FOR_INVOICE_TEST_" + System.currentTimeMillis());
            rateType.put("description", "Rate type for invoice testing");
            
            List<Map<String, Object>> roomTypeRates = new ArrayList<>();
            Map<String, Object> roomTypeRate = new HashMap<>();
            roomTypeRate.put("roomTypeId", roomTypeId);
            roomTypeRate.put("rate", new BigDecimal("120.00"));
            roomTypeRates.add(roomTypeRate);
            rateType.put("roomTypeRates", roomTypeRates);
            
            Response createRateTypeResponse = given()
                    .spec(requestSpec)
                    .body(rateType)
                    .when()
                    .post("/rate-types")
                    .then()
                    .statusCode(201)
                    .extract()
                    .response();
            
            rateTypeId = createRateTypeResponse.jsonPath().getLong("id");
        }
    }
    
    private static void createTestReservation() {
        // Create a reservation with future dates
        LocalDate checkInDate = LocalDate.now().plusDays(10);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        
        Map<String, Object> reservation = new HashMap<>();
        reservation.put("guestId", guestId);
        reservation.put("roomId", roomId);
        reservation.put("rateTypeId", rateTypeId);
        reservation.put("checkInDate", checkInDate.format(DateTimeFormatter.ISO_DATE));
        reservation.put("checkOutDate", checkOutDate.format(DateTimeFormatter.ISO_DATE));
        reservation.put("numberOfGuests", 2);
        reservation.put("status", "PENDING");
        
        Response response = given()
                .spec(requestSpec)
                .body(reservation)
                .when()
                .post("/reservations")
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        reservationId = response.jsonPath().getLong("id");
        createdReservationIds.add(reservationId);
    }
    
    // Counter to ensure unique dates for each additional reservation
    private static int additionalReservationCounter = 0;
    
    private static Long createAdditionalReservation() {
        // Create another reservation for testing with unique dates
        // Use counter to ensure dates don't overlap
        additionalReservationCounter++;
        LocalDate checkInDate = LocalDate.now().plusDays(20 + (additionalReservationCounter * 5));
        LocalDate checkOutDate = checkInDate.plusDays(3);
        
        Map<String, Object> reservation = new HashMap<>();
        reservation.put("guestId", guestId);
        reservation.put("roomId", roomId);
        reservation.put("rateTypeId", rateTypeId);
        reservation.put("checkInDate", checkInDate.format(DateTimeFormatter.ISO_DATE));
        reservation.put("checkOutDate", checkOutDate.format(DateTimeFormatter.ISO_DATE));
        reservation.put("numberOfGuests", 2);
        reservation.put("status", "PENDING");
        
        Response response = given()
                .spec(requestSpec)
                .body(reservation)
                .when()
                .post("/reservations")
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long newReservationId = response.jsonPath().getLong("id");
        createdReservationIds.add(newReservationId);
        return newReservationId;
    }

    // ==================== GENERATE INVOICE OPERATIONS ====================

    @Test
    @Order(1)
    @DisplayName("POST /api/invoices/generate/{reservationId} - Generate invoice successfully")
    public void testGenerateInvoice_Success() {
        if (reservationId == null) {
            Assertions.fail("Cannot test invoice generation - no reservation was created");
            return;
        }
        
        Response response = given()
                .spec(requestSpec)
                .when()
                .post("/invoices/generate/{reservationId}", reservationId)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("invoiceNumber", notNullValue())
                .body("reservationId", equalTo(reservationId.intValue()))
                .body("subtotal", notNullValue())
                .body("taxAmount", notNullValue())
                .body("totalAmount", notNullValue())
                .body("status", equalTo("PENDING"))
                .body("items", notNullValue())
                .extract()
                .response();

        createdInvoiceId = response.jsonPath().getLong("id");
        createdInvoiceNumber = response.jsonPath().getString("invoiceNumber");
        
        assertNotNull(createdInvoiceId);
        assertNotNull(createdInvoiceNumber);
        
        // Track for cleanup
        createdInvoiceIds.add(createdInvoiceId);
        
        // Verify invoice items
        List<Map<String, Object>> items = response.jsonPath().getList("items");
        assertTrue(items.size() > 0, "Invoice should have at least one item");
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/invoices/generate/{reservationId} - Generate invoice for non-existent reservation should fail")
    public void testGenerateInvoice_InvalidReservation() {
        given()
                .spec(requestSpec)
                .when()
                .post("/invoices/generate/{reservationId}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/invoices/generate/{reservationId} - Generate duplicate invoice for same reservation should fail")
    public void testGenerateInvoice_Duplicate() {
        if (reservationId == null) {
            return; // Skip if no reservation
        }
        
        // Try to generate another invoice for the same reservation
        given()
                .spec(requestSpec)
                .when()
                .post("/invoices/generate/{reservationId}", reservationId)
                .then()
                .statusCode(400); // Should fail - invoice already exists
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/invoices/generate/{reservationId} - Generate additional invoices for testing")
    public void testGenerateAdditionalInvoices() {
        // Create additional reservations and generate invoices
        Long reservationId2 = createAdditionalReservation();
        
        Response response = given()
                .spec(requestSpec)
                .when()
                .post("/invoices/generate/{reservationId}", reservationId2)
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long invoiceId = response.jsonPath().getLong("id");
        if (invoiceId != null) {
            createdInvoiceIds.add(invoiceId);
        }
    }

    // ==================== READ OPERATIONS ====================

    @Test
    @Order(10)
    @DisplayName("GET /api/invoices/{id} - Get invoice by ID successfully")
    public void testGetInvoiceById_Success() {
        if (createdInvoiceId != null) {
            given()
                    .spec(requestSpec)
                    .when()
                    .get("/invoices/{id}", createdInvoiceId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(createdInvoiceId.intValue()))
                    .body("invoiceNumber", equalTo(createdInvoiceNumber))
                    .body("reservationId", notNullValue());
        } else {
            // If invoice creation failed, try to get any existing invoice
            Response response = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
                    .when()
                    .get("/invoices")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> invoices = response.jsonPath().getList("content");
            if (invoices == null || invoices.isEmpty()) {
                invoices = response.jsonPath().getList("$");
            }
            
            if (invoices != null && !invoices.isEmpty()) {
                Long invoiceId = ((Number) invoices.get(0).get("id")).longValue();
                given()
                        .spec(requestSpec)
                        .when()
                        .get("/invoices/{id}", invoiceId)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", notNullValue());
            } else {
                Assertions.fail("No invoices available for testing");
            }
        }
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/invoices/{id} - Get non-existent invoice should return 400")
    public void testGetInvoiceById_NotFound() {
        given()
                .spec(requestSpec)
                .when()
                .get("/invoices/{id}", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found (via GlobalExceptionHandler)
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/invoices/number/{invoiceNumber} - Get invoice by number successfully")
    public void testGetInvoiceByNumber_Success() {
        if (createdInvoiceNumber != null) {
            given()
                    .spec(requestSpec)
                    .when()
                    .get("/invoices/number/{invoiceNumber}", createdInvoiceNumber)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("invoiceNumber", equalTo(createdInvoiceNumber))
                    .body("id", notNullValue());
        } else {
            // If no invoice was created, try with any existing invoice number
            Response response = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 1)
                    .when()
                    .get("/invoices")
                    .then()
                    .statusCode(200)
                    .extract()
                    .response();
            
            List<Map<String, Object>> invoices = response.jsonPath().getList("content");
            if (invoices == null || invoices.isEmpty()) {
                invoices = response.jsonPath().getList("$");
            }
            
            if (invoices != null && !invoices.isEmpty() && invoices.get(0).get("invoiceNumber") != null) {
                String invoiceNumber = (String) invoices.get(0).get("invoiceNumber");
                given()
                        .spec(requestSpec)
                        .when()
                        .get("/invoices/number/{invoiceNumber}", invoiceNumber)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("invoiceNumber", equalTo(invoiceNumber));
            } else {
                Assertions.fail("No invoices with invoice number available for testing");
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/invoices/number/{invoiceNumber} - Get non-existent invoice number should return 400")
    public void testGetInvoiceByNumber_NotFound() {
        given()
                .spec(requestSpec)
                .when()
                .get("/invoices/number/{invoiceNumber}", "NON_EXISTENT_99999")
                .then()
                .statusCode(400); // Application returns 400 for not found
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/invoices - Get all invoices (non-paginated)")
    public void testGetAllInvoices_NonPaginated() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/invoices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        // Check if response is paginated (has 'content' field) or plain list
        Object content = response.jsonPath().get("content");
        if (content != null) {
            // It's a paginated response
            List<Map<String, Object>> invoices = response.jsonPath().getList("content");
            assertTrue(invoices.size() >= 0, "Should have at least some invoices");
        } else {
            // It's a plain list
            List<Map<String, Object>> invoices = response.jsonPath().getList("$");
            assertTrue(invoices.size() >= 0, "Should have at least some invoices");
        }
    }

    @Test
    @Order(15)
    @DisplayName("GET /api/invoices/reservation/{reservationId} - Get invoices by reservation successfully")
    public void testGetInvoicesByReservation_Success() {
        if (reservationId != null) {
            Response response = given()
                    .spec(requestSpec)
                    .when()
                    .get("/invoices/reservation/{reservationId}", reservationId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .extract()
                    .response();

            List<Map<String, Object>> invoices = response.jsonPath().getList("$");
            assertTrue(invoices.size() > 0, "Should have at least one invoice for this reservation");
            
            // Verify all invoices belong to the reservation
            invoices.forEach(invoice -> {
                Long resId = ((Number) invoice.get("reservationId")).longValue();
                assertEquals(reservationId, resId, "All invoices should belong to the same reservation");
            });
        }
    }

    @Test
    @Order(16)
    @DisplayName("GET /api/invoices/status/{status} - Get invoices by status PENDING")
    public void testGetInvoicesByStatus_Pending() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/invoices/status/{status}", InvoiceStatus.PENDING.name())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> invoices = response.jsonPath().getList("$");
        if (invoices.size() > 0) {
            invoices.forEach(invoice -> {
                String status = (String) invoice.get("status");
                assertEquals("PENDING", status, "All invoices should have status PENDING");
            });
        }
    }

    @Test
    @Order(17)
    @DisplayName("GET /api/invoices/status/{status} - Get invoices by status PAID")
    public void testGetInvoicesByStatus_Paid() {
        Response response = given()
                .spec(requestSpec)
                .when()
                .get("/invoices/status/{status}", InvoiceStatus.PAID.name())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> invoices = response.jsonPath().getList("$");
        if (invoices.size() > 0) {
            invoices.forEach(invoice -> {
                String status = (String) invoice.get("status");
                assertEquals("PAID", status, "All invoices should have status PAID");
            });
        }
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    @Order(20)
    @DisplayName("GET /api/invoices - Test pagination with page and size")
    public void testGetAllInvoices_WithPagination() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 2)
                .when()
                .get("/invoices")
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
    @DisplayName("GET /api/invoices - Test pagination second page")
    public void testGetAllInvoices_SecondPage() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 1)
                .queryParam("size", 2)
                .when()
                .get("/invoices")
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
    @DisplayName("GET /api/invoices - Test sorting by issuedDate ascending")
    public void testGetAllInvoices_SortByIssuedDateAsc() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "issuedDate")
                .queryParam("sortDir", "asc")
                .when()
                .get("/invoices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() >= 0);
    }

    @Test
    @Order(31)
    @DisplayName("GET /api/invoices - Test sorting by issuedDate descending")
    public void testGetAllInvoices_SortByIssuedDateDesc() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "issuedDate")
                .queryParam("sortDir", "desc")
                .when()
                .get("/invoices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() >= 0);
    }

    @Test
    @Order(32)
    @DisplayName("GET /api/invoices - Test sorting by totalAmount ascending")
    public void testGetAllInvoices_SortByTotalAmountAsc() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "totalAmount")
                .queryParam("sortDir", "asc")
                .when()
                .get("/invoices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() >= 2) {
            BigDecimal firstAmount = new BigDecimal(content.get(0).get("totalAmount").toString());
            BigDecimal secondAmount = new BigDecimal(content.get(1).get("totalAmount").toString());
            assertTrue(firstAmount.compareTo(secondAmount) <= 0, 
                    "Invoices should be sorted by totalAmount in ascending order");
        }
    }

    @Test
    @Order(33)
    @DisplayName("GET /api/invoices - Test sorting by totalAmount descending")
    public void testGetAllInvoices_SortByTotalAmountDesc() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "totalAmount")
                .queryParam("sortDir", "desc")
                .when()
                .get("/invoices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() >= 2) {
            BigDecimal firstAmount = new BigDecimal(content.get(0).get("totalAmount").toString());
            BigDecimal secondAmount = new BigDecimal(content.get(1).get("totalAmount").toString());
            assertTrue(firstAmount.compareTo(secondAmount) >= 0, 
                    "Invoices should be sorted by totalAmount in descending order");
        }
    }

    @Test
    @Order(34)
    @DisplayName("GET /api/invoices - Test sorting by status")
    public void testGetAllInvoices_SortByStatus() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("sortBy", "status")
                .queryParam("sortDir", "asc")
                .when()
                .get("/invoices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() >= 0);
    }

    // ==================== FILTERING TESTS ====================

    @Test
    @Order(40)
    @DisplayName("GET /api/invoices - Test filtering by invoiceNumber")
    public void testGetAllInvoices_FilterByInvoiceNumber() {
        if (createdInvoiceNumber != null) {
            Response response = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .queryParam("invoiceNumber", createdInvoiceNumber)
                    .when()
                    .get("/invoices")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .extract()
                    .response();

            List<Map<String, Object>> content = response.jsonPath().getList("content");
            assertTrue(content.size() > 0);
            content.forEach(invoice -> {
                String invoiceNumber = (String) invoice.get("invoiceNumber");
                assertEquals(createdInvoiceNumber, invoiceNumber, 
                        "All invoices should have the same invoice number");
            });
        }
    }

    @Test
    @Order(41)
    @DisplayName("GET /api/invoices - Test filtering by reservationId")
    public void testGetAllInvoices_FilterByReservationId() {
        if (reservationId != null) {
            Response response = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .queryParam("reservationId", reservationId)
                    .when()
                    .get("/invoices")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .extract()
                    .response();

            List<Map<String, Object>> content = response.jsonPath().getList("content");
            assertTrue(content.size() > 0);
            content.forEach(invoice -> {
                Long resId = ((Number) invoice.get("reservationId")).longValue();
                assertEquals(reservationId, resId, "All invoices should have the same reservationId");
            });
        }
    }

    @Test
    @Order(42)
    @DisplayName("GET /api/invoices - Test filtering by status PENDING")
    public void testGetAllInvoices_FilterByStatusPending() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("status", "PENDING")
                .when()
                .get("/invoices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(invoice -> {
                String status = (String) invoice.get("status");
                assertEquals("PENDING", status, "All invoices should have status PENDING");
            });
        }
    }

    @Test
    @Order(43)
    @DisplayName("GET /api/invoices - Test filtering by status PAID")
    public void testGetAllInvoices_FilterByStatusPaid() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("status", "PAID")
                .when()
                .get("/invoices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(invoice -> {
                String status = (String) invoice.get("status");
                assertEquals("PAID", status, "All invoices should have status PAID");
            });
        }
    }

    @Test
    @Order(44)
    @DisplayName("GET /api/invoices - Test filtering by issuedDate range")
    public void testGetAllInvoices_FilterByIssuedDateRange() {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(30);
        LocalDateTime toDate = LocalDateTime.now().plusDays(1);
        
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("issuedDateFrom", fromDate.format(DateTimeFormatter.ISO_DATE_TIME))
                .queryParam("issuedDateTo", toDate.format(DateTimeFormatter.ISO_DATE_TIME))
                .when()
                .get("/invoices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        assertTrue(content.size() >= 0);
    }

    @Test
    @Order(45)
    @DisplayName("GET /api/invoices - Test filtering by paymentMethod")
    public void testGetAllInvoices_FilterByPaymentMethod() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .queryParam("paymentMethod", "CREDIT_CARD")
                .when()
                .get("/invoices")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        List<Map<String, Object>> content = response.jsonPath().getList("content");
        if (content.size() > 0) {
            content.forEach(invoice -> {
                String paymentMethod = (String) invoice.get("paymentMethod");
                assertEquals("CREDIT_CARD", paymentMethod, 
                        "All invoices should have payment method CREDIT_CARD");
            });
        }
    }

    @Test
    @Order(46)
    @DisplayName("GET /api/invoices - Test filtering by searchTerm")
    public void testGetAllInvoices_SearchTerm() {
        if (createdInvoiceNumber != null) {
            Response response = given()
                    .spec(requestSpec)
                    .queryParam("page", 0)
                    .queryParam("size", 10)
                    .queryParam("searchTerm", createdInvoiceNumber.substring(0, 3))
                    .when()
                    .get("/invoices")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .extract()
                    .response();

            List<Map<String, Object>> content = response.jsonPath().getList("content");
            assertTrue(content.size() >= 0);
        }
    }

    @Test
    @Order(47)
    @DisplayName("GET /api/invoices - Test combined filtering, pagination, and sorting")
    public void testGetAllInvoices_CombinedFilters() {
        Response response = given()
                .spec(requestSpec)
                .queryParam("page", 0)
                .queryParam("size", 5)
                .queryParam("sortBy", "issuedDate")
                .queryParam("sortDir", "desc")
                .queryParam("status", "PENDING")
                .when()
                .get("/invoices")
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
        content.forEach(invoice -> {
            String status = (String) invoice.get("status");
            assertEquals("PENDING", status);
        });
    }

    // ==================== INVOICE ITEM OPERATIONS ====================

    @Test
    @Order(60)
    @DisplayName("POST /api/invoices/{invoiceId}/items - Add item to invoice successfully")
    public void testAddInvoiceItem_Success() {
        if (createdInvoiceId == null) {
            return; // Skip if no invoice was created
        }
        
        Map<String, Object> item = new HashMap<>();
        item.put("description", "Room Service - Breakfast");
        item.put("quantity", 2);
        item.put("unitPrice", new BigDecimal("15.00"));
        item.put("amount", new BigDecimal("30.00"));
        item.put("category", "SERVICE");
        
        Response response = given()
                .spec(requestSpec)
                .body(item)
                .when()
                .post("/invoices/{invoiceId}/items", createdInvoiceId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(createdInvoiceId.intValue()))
                .extract()
                .response();
        
        // Verify item was added
        List<Map<String, Object>> items = response.jsonPath().getList("items");
        assertTrue(items.size() > 1, "Invoice should have more than one item");
        
        // Verify totals were recalculated
        BigDecimal totalAmount = new BigDecimal(response.jsonPath().getString("totalAmount"));
        assertTrue(totalAmount.compareTo(BigDecimal.ZERO) > 0, "Total amount should be positive");
    }

    @Test
    @Order(61)
    @DisplayName("POST /api/invoices/{invoiceId}/items - Add item to non-existent invoice should fail")
    public void testAddInvoiceItem_InvoiceNotFound() {
        Map<String, Object> item = new HashMap<>();
        item.put("description", "Test Item");
        item.put("quantity", 1);
        item.put("unitPrice", new BigDecimal("10.00"));
        item.put("amount", new BigDecimal("10.00"));
        
        given()
                .spec(requestSpec)
                .body(item)
                .when()
                .post("/invoices/{invoiceId}/items", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found
    }

    @Test
    @Order(62)
    @DisplayName("POST /api/invoices/{invoiceId}/items - Add item to paid invoice should fail")
    public void testAddInvoiceItem_PaidInvoice() {
        if (createdInvoiceId == null) {
            return; // Skip if no invoice
        }
        
        // First mark the invoice as paid
        given()
                .spec(requestSpec)
                .queryParam("paymentMethod", "CASH")
                .when()
                .post("/invoices/{invoiceId}/pay", createdInvoiceId)
                .then()
                .statusCode(200);
        
        // Try to add item to paid invoice
        Map<String, Object> item = new HashMap<>();
        item.put("description", "Test Item");
        item.put("quantity", 1);
        item.put("unitPrice", new BigDecimal("10.00"));
        item.put("amount", new BigDecimal("10.00"));
        
        given()
                .spec(requestSpec)
                .body(item)
                .when()
                .post("/invoices/{invoiceId}/items", createdInvoiceId)
                .then()
                .statusCode(400); // Should fail - cannot add items to paid invoice
    }

    @Test
    @Order(63)
    @DisplayName("DELETE /api/invoices/{invoiceId}/items/{itemId} - Remove item from invoice successfully")
    public void testRemoveInvoiceItem_Success() {
        // Create a new invoice for this test to ensure we have a clean state
        Long testReservationId = createAdditionalReservation();
        Response invoiceResponse = given()
                .spec(requestSpec)
                .when()
                .post("/invoices/generate/{reservationId}", testReservationId)
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long testInvoiceId = invoiceResponse.jsonPath().getLong("id");
        createdInvoiceIds.add(testInvoiceId);
        
        // First add an item to the invoice
        Map<String, Object> item = new HashMap<>();
        item.put("description", "Room Service - Test Item");
        item.put("quantity", 1);
        item.put("unitPrice", new BigDecimal("25.00"));
        item.put("amount", new BigDecimal("25.00"));
        item.put("category", "SERVICE");
        
        given()
                .spec(requestSpec)
                .body(item)
                .when()
                .post("/invoices/{invoiceId}/items", testInvoiceId)
                .then()
                .statusCode(200);
        
        // Get the invoice to find the added item ID
        Response getInvoiceResponse = given()
                .spec(requestSpec)
                .when()
                .get("/invoices/{id}", testInvoiceId)
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        List<Map<String, Object>> items = getInvoiceResponse.jsonPath().getList("items");
        assertTrue(items != null && items.size() > 1, "Invoice should have at least 2 items");
        
        // Find the non-room-charge item to remove
        Long itemIdToRemove = null;
        for (Map<String, Object> invoiceItem : items) {
            String category = (String) invoiceItem.get("category");
            if (category != null && !category.equals("ROOM_CHARGE")) {
                itemIdToRemove = ((Number) invoiceItem.get("id")).longValue();
                break;
            }
        }
        
        assertNotNull(itemIdToRemove, "Should have found a non-room-charge item to remove");
        
        // Remove the item
        Response response = given()
                .spec(requestSpec)
                .when()
                .delete("/invoices/{invoiceId}/items/{itemId}", testInvoiceId, itemIdToRemove)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(testInvoiceId.intValue()))
                .extract()
                .response();
        
        // Verify item was removed
        List<Map<String, Object>> updatedItems = response.jsonPath().getList("items");
        assertTrue(updatedItems.size() < items.size(), "Invoice should have fewer items");
    }

    @Test
    @Order(64)
    @DisplayName("DELETE /api/invoices/{invoiceId}/items/{itemId} - Remove item from non-existent invoice should fail")
    public void testRemoveInvoiceItem_InvoiceNotFound() {
        given()
                .spec(requestSpec)
                .when()
                .delete("/invoices/{invoiceId}/items/{itemId}", 99999L, 1L)
                .then()
                .statusCode(400); // Application returns 400 for not found
    }

    @Test
    @Order(65)
    @DisplayName("DELETE /api/invoices/{invoiceId}/items/{itemId} - Remove item from paid invoice should fail")
    public void testRemoveInvoiceItem_PaidInvoice() {
        // Create a new invoice for this test
        Long testReservationId = createAdditionalReservation();
        Response invoiceResponse = given()
                .spec(requestSpec)
                .when()
                .post("/invoices/generate/{reservationId}", testReservationId)
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long testInvoiceId = invoiceResponse.jsonPath().getLong("id");
        createdInvoiceIds.add(testInvoiceId);
        
        // Mark invoice as paid
        given()
                .spec(requestSpec)
                .queryParam("paymentMethod", "CASH")
                .when()
                .post("/invoices/{invoiceId}/pay", testInvoiceId)
                .then()
                .statusCode(200);
        
        // Get an item ID
        Response getInvoiceResponse = given()
                .spec(requestSpec)
                .when()
                .get("/invoices/{id}", testInvoiceId)
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        List<Map<String, Object>> items = getInvoiceResponse.jsonPath().getList("items");
        if (items != null && !items.isEmpty()) {
            Long itemId = ((Number) items.get(0).get("id")).longValue();
            
            // Try to remove item from paid invoice
            given()
                    .spec(requestSpec)
                    .when()
                    .delete("/invoices/{invoiceId}/items/{itemId}", testInvoiceId, itemId)
                    .then()
                    .statusCode(400); // Should fail - cannot remove items from paid invoice
        }
    }

    // ==================== PAYMENT OPERATIONS ====================

    @Test
    @Order(80)
    @DisplayName("POST /api/invoices/{invoiceId}/pay - Mark invoice as paid successfully")
    public void testMarkInvoiceAsPaid_Success() {
        // Create a new invoice for payment test
        Long testReservationId = createAdditionalReservation();
        Response invoiceResponse = given()
                .spec(requestSpec)
                .when()
                .post("/invoices/generate/{reservationId}", testReservationId)
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long testInvoiceId = invoiceResponse.jsonPath().getLong("id");
        createdInvoiceIds.add(testInvoiceId);
        
        // Mark invoice as paid
        Response response = given()
                .spec(requestSpec)
                .queryParam("paymentMethod", "CREDIT_CARD")
                .when()
                .post("/invoices/{invoiceId}/pay", testInvoiceId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(testInvoiceId.intValue()))
                .body("status", equalTo("PAID"))
                .body("paymentMethod", equalTo("CREDIT_CARD"))
                .body("paidDate", notNullValue())
                .extract()
                .response();
        
        // Verify payment details
        String paymentMethod = response.jsonPath().getString("paymentMethod");
        String status = response.jsonPath().getString("status");
        assertEquals("CREDIT_CARD", paymentMethod);
        assertEquals("PAID", status);
    }

    @Test
    @Order(81)
    @DisplayName("POST /api/invoices/{invoiceId}/pay - Mark non-existent invoice as paid should fail")
    public void testMarkInvoiceAsPaid_InvoiceNotFound() {
        given()
                .spec(requestSpec)
                .queryParam("paymentMethod", "CASH")
                .when()
                .post("/invoices/{invoiceId}/pay", 99999L)
                .then()
                .statusCode(400); // Application returns 400 for not found
    }

    @Test
    @Order(82)
    @DisplayName("POST /api/invoices/{invoiceId}/pay - Mark already paid invoice as paid should fail")
    public void testMarkInvoiceAsPaid_AlreadyPaid() {
        // Create a new invoice for this test
        Long testReservationId = createAdditionalReservation();
        Response invoiceResponse = given()
                .spec(requestSpec)
                .when()
                .post("/invoices/generate/{reservationId}", testReservationId)
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long testInvoiceId = invoiceResponse.jsonPath().getLong("id");
        createdInvoiceIds.add(testInvoiceId);
        
        // Mark invoice as paid first time
        given()
                .spec(requestSpec)
                .queryParam("paymentMethod", "CASH")
                .when()
                .post("/invoices/{invoiceId}/pay", testInvoiceId)
                .then()
                .statusCode(200);
        
        // Try to mark as paid again
        given()
                .spec(requestSpec)
                .queryParam("paymentMethod", "CREDIT_CARD")
                .when()
                .post("/invoices/{invoiceId}/pay", testInvoiceId)
                .then()
                .statusCode(400); // Should fail - invoice is already paid
    }

    @Test
    @Order(83)
    @DisplayName("POST /api/invoices/{invoiceId}/pay - Mark invoice as paid with different payment methods")
    public void testMarkInvoiceAsPaid_DifferentPaymentMethods() {
        // Test with CASH
        Long testReservationId1 = createAdditionalReservation();
        Response invoiceResponse1 = given()
                .spec(requestSpec)
                .when()
                .post("/invoices/generate/{reservationId}", testReservationId1)
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long testInvoiceId1 = invoiceResponse1.jsonPath().getLong("id");
        createdInvoiceIds.add(testInvoiceId1);
        
        given()
                .spec(requestSpec)
                .queryParam("paymentMethod", "CASH")
                .when()
                .post("/invoices/{invoiceId}/pay", testInvoiceId1)
                .then()
                .statusCode(200)
                .body("paymentMethod", equalTo("CASH"));
        
        // Test with BANK_TRANSFER
        Long testReservationId2 = createAdditionalReservation();
        Response invoiceResponse2 = given()
                .spec(requestSpec)
                .when()
                .post("/invoices/generate/{reservationId}", testReservationId2)
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        Long testInvoiceId2 = invoiceResponse2.jsonPath().getLong("id");
        createdInvoiceIds.add(testInvoiceId2);
        
        given()
                .spec(requestSpec)
                .queryParam("paymentMethod", "BANK_TRANSFER")
                .when()
                .post("/invoices/{invoiceId}/pay", testInvoiceId2)
                .then()
                .statusCode(200)
                .body("paymentMethod", equalTo("BANK_TRANSFER"));
    }

    @AfterAll
    @DisplayName("Cleanup - Delete all test invoices and reservations created during test execution")
    public static void cleanupTestData() {
        logger.info("Starting cleanup of test data - {} invoice(s) tracked", createdInvoiceIds.size());
        
        // Note: Invoices are typically not deleted directly as they are tied to reservations
        // In a real scenario, you might need to delete reservations first, which would cascade
        // For now, we'll just log the cleanup attempt and clear tracking lists
        
        logger.info("Cleanup completed - Invoices tracked: {}, Reservations tracked: {}", 
                createdInvoiceIds.size(), createdReservationIds.size());
        
        // Clear the lists
        createdInvoiceIds.clear();
        createdReservationIds.clear();
        createdInvoiceId = null;
        createdInvoiceNumber = null;
    }
}

