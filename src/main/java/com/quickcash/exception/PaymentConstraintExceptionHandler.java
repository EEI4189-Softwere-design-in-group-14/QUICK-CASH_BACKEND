
package com.quickcash.exception;

import com.quickcash.controller.CollectorPaymentController;

import org.hibernate.exception.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice(
        assignableTypes = CollectorPaymentController.class
)
public class PaymentConstraintExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PaymentConstraintExceptionHandler.class
            );

    private static final String TRANSACTION_UNIQUE_KEY =
            "uk_payment_client_transaction";

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>>
    handleIntegrityViolation(
            DataIntegrityViolationException exception
    ) {

        if (isDuplicateTransactionId(exception)) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "status", 409,
                            "error", "Conflict",
                            "message",
                            "Transaction ID already used"
                    ));
        }

        // Other database errors must not be
        // incorrectly reported as duplicate transactions.
        log.error(
                "Unexpected payment database integrity error",
                exception
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", 500,
                        "error", "Internal Server Error"
                ));
    }

    private boolean isDuplicateTransactionId(
            Throwable exception
    ) {

        Throwable current = exception;

        while (current != null) {

            if (current instanceof ConstraintViolationException
                    constraintException) {

                String constraintName =
                        constraintException.getConstraintName();

                if (containsUniqueKey(constraintName)) {
                    return true;
                }
            }

            if (current instanceof SQLException sqlException) {

                if (sqlException.getErrorCode() == 1062
                        && containsUniqueKey(
                        sqlException.getMessage()
                )) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }

    private boolean containsUniqueKey(String value) {

        return value != null
                && value.toLowerCase(Locale.ROOT)
                .contains(TRANSACTION_UNIQUE_KEY);
    }
}
