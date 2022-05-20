package uk.ac.ox.softeng.maurodatamapper.testing.functional.federation

import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogueType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.test.BaseSubscribedCatalogueFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getADMIN

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
class AtomSubscribedCatalogueFunctionalSpec extends BaseSubscribedCatalogueFunctionalSpec {
    @Override
    Map getValidJson() {
        [
            url                    : subscribedCatalogueUrl,
            apiKey                 : UUID.randomUUID().toString(),
            label                  : 'Functional Test Label',
            subscribedCatalogueType: subscribedCatalogueType.label,
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            url   : 'wibble',
            apiKey: '67421316-66a5-4830-9156-b1ba77bba5d1'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Functional Test Description Updated'
        ]
    }

    @Override
    String getExpectedShowJson() {
        """{
    "apiKey": "\${json-unit.matches:id}",
    "description": "Functional Test Description",
    "id": "\${json-unit.matches:id}",
    "label": "Functional Test Label",
    "subscribedCatalogueType": "$subscribedCatalogueType.label",
    "refreshPeriod": 7,
    "url": "$subscribedCatalogueUrl"
}"""
    }

    @Override
    String getExpectedOpenAccessShowJson() {
        """{
    "description": "Functional Test Description",
    "id": "\${json-unit.matches:id}",
    "label": "Functional Test Label",
    "subscribedCatalogueType": "$subscribedCatalogueType.label",
    "refreshPeriod": 7,
    "url": "$subscribedCatalogueUrl"
}"""
    }

    @Override
    String getExpectedIndexJson() {
        """{
    "count": 1,
    "items": [
        {
            "id": "\${json-unit.matches:id}",
            "url": "$subscribedCatalogueUrl",
            "label": "Functional Test Label",
            "subscribedCatalogueType": "$subscribedCatalogueType.label",
            "description": "Functional Test Description",
            "refreshPeriod": 7
        }
    ]
}"""
    }

    @Override
    SubscribedCatalogueType getSubscribedCatalogueType() {
        SubscribedCatalogueType.ATOM
    }

    @Override
    String getSubscribedCatalogueUrl() {
        "http://localhost:$serverPort/api/feeds/all".toString()
    }

    /**
     * Test the publishedModels endpoint. This would be on a remote host, but in this functional test
     * we use the localhost. Test setup and execution is as follows:
     * 1. Login as Admin and create an API Key for Admin
     * 2. Subscribe to the local catalogue (in real life this would be remote), specifying the API key created above
     * 3. Get the local /publishedModels endpoint. In real life this would connect to the Atom feed on the remote,
     * but here we use the local.
     * 4. Cleanup
     */
    void 'A07a : Test the publishedModels endpoint'() {

        given:
        Map apiKeyJson = [
            name      : 'Functional Test',
            expiryDate: LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ]

        when:
        loginAdmin()
        POST("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys", apiKeyJson, MAP_ARG, true)

        then:
        verifyResponse CREATED, response
        String apiKey = responseBody().apiKey

        when:
        //note: using a groovy string like "http://localhost:$serverPort/" causes the url to be stripped when saving
        Map subscriptionJson = [
            url                    : subscribedCatalogueUrl,
            apiKey                 : apiKey,
            label                  : 'Functional Test Label',
            subscribedCatalogueType: subscribedCatalogueType.label,
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)

        then:
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        verifyBaseJsonResponse(responseBody(), true)
        responseBody().items.size() == 3

        and:
        verifyJsonPublishedModel(responseBody().items.find {it.label == 'Finalised Example Test DataModel 1.0.0'}, 'dataModels',
                                 getDataModelExporters())
        verifyJsonPublishedModel(responseBody().items.find {it.label == 'Simple Test CodeSet 1.0.0'}, 'codeSets', getCodeSetExporters())
        verifyJsonPublishedModel(responseBody().items.find {it.label == 'Complex Test CodeSet 1.0.0'}, 'codeSets', getCodeSetExporters())

        cleanup:
        DELETE("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys/${apiKey}", MAP_ARG, true)
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A07b : Test the publishedModels endpoint (without API key)'() {
        given:
        loginAdmin()

        when:
        Map subscriptionJson = [
            url                    : subscribedCatalogueUrl,
            apiKey                 : '',
            label                  : 'Functional Test Label',
            subscribedCatalogueType: subscribedCatalogueType.label,
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)

        then:
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        verifyBaseJsonResponse(responseBody(), false)

        cleanup:
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A08a : Test the newerVersions endpoint (with no newer versions)'() {
        given:
        Map apiKeyJson = [
            name      : 'Functional Test',
            expiryDate: LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ]
        loginAdmin()
        POST("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys", apiKeyJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String apiKey = responseBody().apiKey
        Map subscriptionJson = [
            url                    : subscribedCatalogueUrl,
            apiKey                 : apiKey,
            label                  : 'Functional Test Label',
            subscribedCatalogueType: subscribedCatalogueType.label,
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        String finalisedDataModelId = getFinalisedDataModelId()
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedDataModelId}/newerVersions", MAP_ARG, true)

        then:
        verifyResponse OK, response
        !responseBody()

        cleanup:
        DELETE("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys/${apiKey}", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A08b : Test the newerVersions endpoint (with newer versions and API key)'() {
        given:
        String finalisedDataModelId = getFinalisedDataModelId()
        Tuple tuple = getNewerDataModelIds()
        String newerPublicId = tuple.v1
        String newerId = tuple.v2
        Map apiKeyJson = [
            name      : 'Functional Test',
            expiryDate: LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ]
        loginAdmin()
        POST("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys", apiKeyJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String apiKey = responseBody().apiKey
        Map subscriptionJson = [
            url                    : subscribedCatalogueUrl,
            apiKey                 : apiKey,
            label                  : 'Functional Test Label',
            subscribedCatalogueType: subscribedCatalogueType.label,
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedDataModelId}/newerVersions", MAP_ARG, true)

        then:
        verifyResponse OK, response
        !responseBody()

        cleanup:
        DELETE("dataModels/${newerId}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/${newerPublicId}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys/${apiKey}", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    void 'A08c : Test the newerModels endpoint (with newer models, without API key)'() {
        given:
        String finalisedDataModelId = getFinalisedDataModelId()
        Tuple tuple = getNewerDataModelIds()
        String newerPublicId = tuple.v1
        String newerId = tuple.v2
        loginAdmin()
        Map subscriptionJson = [
            url                    : subscribedCatalogueUrl,
            apiKey                 : '',
            label                  : 'Functional Test Label',
            subscribedCatalogueType: subscribedCatalogueType.label,
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
        POST('', subscriptionJson)
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id
        PUT("dataModels/${finalisedDataModelId}/readByEveryone", [:], MAP_ARG, true)
        verifyResponse OK, response

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedDataModelId}/newerVersions", MAP_ARG, true)

        then:
        verifyResponse OK, response
        !responseBody()

        cleanup:
        DELETE("dataModels/$newerPublicId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$newerId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$finalisedDataModelId/readByEveryone", MAP_ARG, true)
        verifyResponse OK, response
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }

    private void verifyJsonPublishedModel(Map publishedModel, String modelEndpoint, Map<String, String> exporters) {
        assert publishedModel
        assert publishedModel.modelId ==~ /urn:uuid:\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
        assert publishedModel.label
        assert OffsetDateTime.parse(publishedModel.datePublished, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert OffsetDateTime.parse(publishedModel.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert publishedModel.links.each {link ->
            assert link.contentType
            String exporterUrl = exporters.get(link.contentType)
            assert link.url ==~ /http:\/\/localhost:$serverPort\/api\/$modelEndpoint\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}\/export\\/$exporterUrl/
        }
    }
}
