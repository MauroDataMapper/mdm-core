package uk.ac.ox.softeng.maurodatamapper.core.api.exception

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException

class ApiDiffException extends ApiException {

    ApiDiffException(String errorCode, String message) {
        super(errorCode, message)
    }
}
