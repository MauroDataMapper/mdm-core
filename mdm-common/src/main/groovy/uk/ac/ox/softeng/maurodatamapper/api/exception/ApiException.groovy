package uk.ac.ox.softeng.maurodatamapper.api.exception


import io.micronaut.http.HttpStatus

import java.time.LocalDateTime

/**
 * Created by james on 27/04/2017.
 */
//@SuppressFBWarnings('RANGE_ARRAY_INDEX')
abstract class ApiException extends Exception {

    LocalDateTime dateThrown
    String errorCode
    HttpStatus status

    ApiException(String errorCode, String message) {
        super(message)
        this.errorCode = errorCode
        dateThrown = LocalDateTime.now()
        this.status = HttpStatus.INTERNAL_SERVER_ERROR
    }

    ApiException(String errorCode, String message, Throwable cause) {
        super(message, cause)
        this.errorCode = errorCode
        this.dateThrown = LocalDateTime.now()
    }

    @Override
    String getMessage() {
        cause ? "${super.getMessage()}: ${cause.getMessage()}" : super.getMessage()

    }
}
