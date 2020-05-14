package uk.ac.ox.softeng.maurodatamapper.api.exception

/**
 * @since 20/11/2017
 */
class ApiInternalException extends ApiException {

    ApiInternalException(String errorCode, String message) {
        super(errorCode, message)
    }

    ApiInternalException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause)
    }
}
