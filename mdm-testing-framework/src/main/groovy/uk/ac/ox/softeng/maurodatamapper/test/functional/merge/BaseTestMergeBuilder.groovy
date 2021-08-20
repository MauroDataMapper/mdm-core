package uk.ac.ox.softeng.maurodatamapper.test.functional.merge

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

import java.nio.charset.Charset

import static uk.ac.ox.softeng.maurodatamapper.test.http.RestClientInterface.MAP_ARG

import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * @since 03/08/2021
 */
abstract class BaseTestMergeBuilder {

    BaseFunctionalSpec functionalSpec

    BaseTestMergeBuilder(BaseFunctionalSpec functionalSpec) {
        this.functionalSpec = functionalSpec
    }

    abstract TestMergeData buildComplexModelsForMerging(String folderId)

    HttpResponse<Map> POST(String resourceEndpoint, Map body) {
        functionalSpec.POST(resourceEndpoint, body, MAP_ARG, true)
    }

    HttpResponse<Map> PUT(String resourceEndpoint, Map body) {
        functionalSpec.PUT(resourceEndpoint, body, MAP_ARG, true)
    }

    HttpResponse<Map> DELETE(String resourceEndpoint) {
        functionalSpec.DELETE(resourceEndpoint, MAP_ARG, true)
    }

    HttpResponse<Map> GET(String resourceEndpoint) {
        functionalSpec.GET(resourceEndpoint, MAP_ARG, true)
    }

    void verifyResponse(HttpStatus expectedStatus, HttpResponse<Map> response) {
        functionalSpec.verifyResponse(expectedStatus, response)
    }

    HttpResponse<Map> getResponse() {
        functionalSpec.response
    }

    Map<String, Object> responseBody() {
        functionalSpec.responseBody()
    }

    String getIdFromPath(String rootResourceId, String path, boolean mustExist = true) {
        functionalSpec.GET("$rootResourceId/path/${URLEncoder.encode(path, Charset.defaultCharset())}")
        if (mustExist) {
            verifyResponse OK, response
            assert responseBody().id
            return responseBody().id
        }
        verifyResponse NOT_FOUND, response
        null

    }

    void cleanupTestMergeData(TestMergeData mergeData) {
        if (!mergeData) return
        if (mergeData.source) functionalSpec.cleanUpData(mergeData.source)
        if (mergeData.target) functionalSpec.cleanUpData(mergeData.target)
        if (mergeData.commonAncestor) functionalSpec.cleanUpData(mergeData.commonAncestor)

        if (mergeData.otherMap) {

            if (mergeData.otherMap.caTerminology) {
                DELETE("terminologies/$mergeData.otherMap.caTerminology?permanent=true")
                response.status() == NO_CONTENT
            }
            if (mergeData.otherMap.sourceTerminology) {
                DELETE("terminologies/$mergeData.otherMap.sourceTerminology?permanent=true")
                response.status() == NO_CONTENT
            }
            if (mergeData.otherMap.targetTerminology) {
                DELETE("terminologies/$mergeData.otherMap.targetTerminology?permanent=true")
                response.status() == NO_CONTENT
            }
        }
    }
}
