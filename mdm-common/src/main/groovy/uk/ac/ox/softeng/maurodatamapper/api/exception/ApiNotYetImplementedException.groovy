package uk.ac.ox.softeng.maurodatamapper.api.exception

/**
 * @since 12/03/2020
 */
class ApiNotYetImplementedException extends ApiException {

    ApiNotYetImplementedException(String errorCode, String functionality) {
        super(errorCode, "The functionality [$functionality] is not yet implemented")
    }
}
