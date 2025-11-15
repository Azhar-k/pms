package com.klm.pms.util;

import java.math.BigDecimal;

/**
 * Constants used throughout the application.
 * Centralizes magic numbers and strings for better maintainability.
 */
public final class Constants {

    private Constants() {
        // Utility class - prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // Tax and pricing constants
    public static final BigDecimal TAX_RATE = new BigDecimal("0.10"); // 10% tax rate
    public static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;

    // Pagination constants
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MIN_PAGE_SIZE = 1;
    public static final int MAX_PAGE_SIZE = 100;

    // Date and time constants
    public static final int MIN_NIGHTS_FOR_RESERVATION = 1;
    public static final int MAX_NIGHTS_FOR_RESERVATION = 365; // 1 year maximum

    // String length limits
    public static final int MAX_EMAIL_LENGTH = 255;
    public static final int MAX_PHONE_LENGTH = 20;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_ROOM_NUMBER_LENGTH = 20;
    public static final int MAX_RESERVATION_NUMBER_LENGTH = 50;

    // Room capacity limits
    public static final int MIN_ROOM_CAPACITY = 1;
    public static final int MAX_ROOM_CAPACITY = 20;

    // Audit and security constants
    public static final String SYSTEM_USER = "SYSTEM";
    public static final String AUDIT_ENTITY_GUEST = "Guest";
    public static final String AUDIT_ENTITY_ROOM = "Room";
    public static final String AUDIT_ENTITY_RESERVATION = "Reservation";
    public static final String AUDIT_ENTITY_INVOICE = "Invoice";
    public static final String AUDIT_ENTITY_ROOM_TYPE = "RoomType";
    public static final String AUDIT_ENTITY_RATE_TYPE = "RateType";

    // JWT and security constants
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String USERNAME_ATTRIBUTE = "username";
    public static final String ROLES_ATTRIBUTE = "roles";

    // Error messages
    public static final String ERROR_ENTITY_NOT_FOUND = "%s not found with identifier: %s";
    public static final String ERROR_DUPLICATE_ENTITY = "%s with %s '%s' already exists";
    public static final String ERROR_INVALID_DATE_RANGE = "Check-in date must be before check-out date";
    public static final String ERROR_DATE_IN_PAST = "%s cannot be in the past";
    public static final String ERROR_ROOM_NOT_AVAILABLE = "Room is not available for the selected dates";
    public static final String ERROR_EXCEEDS_CAPACITY = "Number of guests exceeds room capacity";
    public static final String ERROR_INVALID_STATUS_TRANSITION = "Invalid status transition: %s -> %s";
}

