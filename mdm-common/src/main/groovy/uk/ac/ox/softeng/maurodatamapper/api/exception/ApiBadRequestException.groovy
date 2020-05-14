package uk.ac.ox.softeng.maurodatamapper.api.exception

import io.micronaut.http.HttpStatus

/**
 * Created by james on 27/04/2017.
 */
class ApiBadRequestException extends ApiException {

    ApiBadRequestException(String errorCode, String message) {
        super(errorCode, message)
        status = HttpStatus.BAD_REQUEST
    }

    ApiBadRequestException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause)
        status = HttpStatus.BAD_REQUEST
    }
}
