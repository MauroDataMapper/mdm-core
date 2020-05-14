package uk.ac.ox.softeng.maurodatamapper.api.exception

/**
 * Created by james on 27/04/2017.
 */
class ApiUnauthorizedException extends ApiException {

    ApiUnauthorizedException(String errorCode, String message) {
        super(errorCode, message)
    }

    ApiUnauthorizedException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause)
    }
}
