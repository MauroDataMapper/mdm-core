package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.util.Pair

class AdminService {

    ApiPropertyService apiPropertyService

    List<ApiPropertyEnum> getApiProperties() throws ApiInternalException {
        List<ApiPropertyEnum> props = apiPropertyService.list()
        if (!props) {
            throw new ApiInternalException('AS01', "Api Properties have not been loaded. " +
                                                   "Please contact the System Administrator")
        }
        return props
    }

    def getAndUpdateApiProperties(User user, Map<String, String> newValues) throws ApiInternalException {
        apiPropertyService.listAndUpdateApiProperties(newValues, user)
    }

    def getAndUpdateApiProperty(User user, Pair<String, String> newValue) throws ApiInternalException {
        apiPropertyService.findAndUpdateByKey(newValue.aValue, newValue.bValue, user)
        apiPropertyService.list()
    }
}
