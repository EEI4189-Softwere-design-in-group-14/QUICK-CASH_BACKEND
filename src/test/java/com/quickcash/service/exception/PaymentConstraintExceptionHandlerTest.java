package com.quickcash.exception;

import org.junit.jupiter.api.Test;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentConstraintExceptionHandlerTest {

    private final PaymentConstraintExceptionHandler handler =
            new PaymentConstraintExceptionHandler();

    @Test
    void duplicateTransactionReturns409() {

        SQLException sqlException = new SQLException(
                "Duplicate entry for key " +
                        "'payments.uk_payment_client_transaction'",
                "23000",
                1062
        );

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Duplicate payment transaction",
                        sqlException
                );

        var response =
                handler.handleIntegrityViolation(exception);

        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );
    }

    @Test
    void unrelatedDatabaseErrorIsNot409() {

        SQLException sqlException = new SQLException(
                "Foreign key constraint failed",
                "23000",
                1452
        );

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Database constraint error",
                        sqlException
                );

        var response =
                handler.handleIntegrityViolation(exception);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );
    }
}