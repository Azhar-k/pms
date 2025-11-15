package com.klm.pms.util;

import com.klm.pms.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;

/**
 * Utility class for common validation operations.
 * Provides defensive programming checks and input validation.
 */
public class ValidationUtil {

    private static final Logger logger = LoggerFactory.getLogger(ValidationUtil.class);

    // Pagination constants
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MIN_PAGE_NUMBER = 0;

    private ValidationUtil() {
        // Utility class - prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates that an object is not null.
     *
     * @param object the object to validate
     * @param fieldName the name of the field (for error messages)
     * @throws ValidationException if the object is null
     */
    public static void requireNonNull(Object object, String fieldName) {
        if (object == null) {
            logger.warn("Validation failed: {} is null", fieldName);
            throw new ValidationException(fieldName, fieldName + " cannot be null");
        }
    }

    /**
     * Validates that a string is not null or blank.
     *
     * @param value the string to validate
     * @param fieldName the name of the field (for error messages)
     * @throws ValidationException if the string is null or blank
     */
    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            logger.warn("Validation failed: {} is null or blank", fieldName);
            throw new ValidationException(fieldName, fieldName + " cannot be null or blank");
        }
    }

    /**
     * Validates that a number is positive (greater than zero).
     *
     * @param value the number to validate
     * @param fieldName the name of the field (for error messages)
     * @throws ValidationException if the number is not positive
     */
    public static void requirePositive(Number value, String fieldName) {
        if (value == null) {
            logger.warn("Validation failed: {} is null", fieldName);
            throw new ValidationException(fieldName, fieldName + " cannot be null");
        }
        if (value.doubleValue() <= 0) {
            logger.warn("Validation failed: {} is not positive: {}", fieldName, value);
            throw new ValidationException(fieldName, fieldName + " must be positive");
        }
    }

    /**
     * Validates that a number is non-negative (greater than or equal to zero).
     *
     * @param value the number to validate
     * @param fieldName the name of the field (for error messages)
     * @throws ValidationException if the number is negative
     */
    public static void requireNonNegative(Number value, String fieldName) {
        if (value == null) {
            logger.warn("Validation failed: {} is null", fieldName);
            throw new ValidationException(fieldName, fieldName + " cannot be null");
        }
        if (value.doubleValue() < 0) {
            logger.warn("Validation failed: {} is negative: {}", fieldName, value);
            throw new ValidationException(fieldName, fieldName + " cannot be negative");
        }
    }

    /**
     * Validates pagination parameters and returns normalized values.
     *
     * @param page the page number (0-indexed)
     * @param size the page size
     * @return an array containing [normalizedPage, normalizedSize]
     * @throws ValidationException if pagination parameters are invalid
     */
    public static int[] validateAndNormalizePagination(Integer page, Integer size) {
        int normalizedPage = (page != null && page >= MIN_PAGE_NUMBER) ? page : MIN_PAGE_NUMBER;
        int normalizedSize = (size != null && size >= MIN_PAGE_SIZE && size <= MAX_PAGE_SIZE) 
                ? size : DEFAULT_PAGE_SIZE;

        if (normalizedPage < MIN_PAGE_NUMBER) {
            logger.warn("Validation failed: page number {} is less than minimum {}", normalizedPage, MIN_PAGE_NUMBER);
            throw new ValidationException("page", 
                    String.format("Page number must be >= %d", MIN_PAGE_NUMBER));
        }

        if (normalizedSize < MIN_PAGE_SIZE || normalizedSize > MAX_PAGE_SIZE) {
            logger.warn("Validation failed: page size {} is outside valid range [{}, {}]", 
                    normalizedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE);
            throw new ValidationException("size", 
                    String.format("Page size must be between %d and %d", MIN_PAGE_SIZE, MAX_PAGE_SIZE));
        }

        logger.debug("Pagination validated: page={}, size={}", normalizedPage, normalizedSize);
        return new int[]{normalizedPage, normalizedSize};
    }

    /**
     * Validates that a date range is valid (start date is before end date).
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param startFieldName the name of the start date field
     * @param endFieldName the name of the end date field
     * @throws ValidationException if the date range is invalid
     */
    public static void validateDateRange(LocalDate startDate, LocalDate endDate, 
                                         String startFieldName, String endFieldName) {
        requireNonNull(startDate, startFieldName);
        requireNonNull(endDate, endFieldName);

        if (startDate.isAfter(endDate)) {
            logger.warn("Validation failed: {} ({}) is after {} ({})", 
                    startFieldName, startDate, endFieldName, endDate);
            throw new ValidationException(startFieldName, 
                    String.format("%s must be before or equal to %s", startFieldName, endFieldName));
        }

        if (startDate.isEqual(endDate)) {
            logger.warn("Validation failed: {} ({}) equals {} ({})", 
                    startFieldName, startDate, endFieldName, endDate);
            throw new ValidationException(startFieldName, 
                    String.format("%s must be before %s (dates cannot be equal)", startFieldName, endFieldName));
        }
    }

    /**
     * Validates that a date is not in the past.
     *
     * @param date the date to validate
     * @param fieldName the name of the field
     * @throws ValidationException if the date is in the past
     */
    public static void requireNotInPast(LocalDate date, String fieldName) {
        requireNonNull(date, fieldName);
        if (date.isBefore(LocalDate.now())) {
            logger.warn("Validation failed: {} ({}) is in the past", fieldName, date);
            throw new ValidationException(fieldName, fieldName + " cannot be in the past");
        }
    }

    /**
     * Validates that a collection is not null or empty.
     *
     * @param collection the collection to validate
     * @param fieldName the name of the field
     * @throws ValidationException if the collection is null or empty
     */
    public static void requireNonEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            logger.warn("Validation failed: {} is null or empty", fieldName);
            throw new ValidationException(fieldName, fieldName + " cannot be null or empty");
        }
    }

    /**
     * Validates that a string length is within the specified range.
     *
     * @param value the string to validate
     * @param fieldName the name of the field
     * @param minLength the minimum length (inclusive)
     * @param maxLength the maximum length (inclusive)
     * @throws ValidationException if the string length is invalid
     */
    public static void validateStringLength(String value, String fieldName, int minLength, int maxLength) {
        if (value == null) {
            logger.warn("Validation failed: {} is null", fieldName);
            throw new ValidationException(fieldName, fieldName + " cannot be null");
        }
        int length = value.length();
        if (length < minLength || length > maxLength) {
            logger.warn("Validation failed: {} length {} is outside range [{}, {}]", 
                    fieldName, length, minLength, maxLength);
            throw new ValidationException(fieldName, 
                    String.format("%s length must be between %d and %d characters", fieldName, minLength, maxLength));
        }
    }

    /**
     * Validates that two objects are equal.
     *
     * @param expected the expected value
     * @param actual the actual value
     * @param fieldName the name of the field
     * @throws ValidationException if the values are not equal
     */
    public static void requireEqual(Object expected, Object actual, String fieldName) {
        if (!Objects.equals(expected, actual)) {
            logger.warn("Validation failed: {} expected '{}' but got '{}'", fieldName, expected, actual);
            throw new ValidationException(fieldName, 
                    String.format("%s expected '%s' but got '%s'", fieldName, expected, actual));
        }
    }

    /**
     * Gets the maximum allowed page size.
     *
     * @return the maximum page size
     */
    public static int getMaxPageSize() {
        return MAX_PAGE_SIZE;
    }

    /**
     * Gets the default page size.
     *
     * @return the default page size
     */
    public static int getDefaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }
}

