/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.testing.functional.federation.publish

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlComparer
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import io.micronaut.http.HttpResponse

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: publish
 *  | GET | /api/published/models                                   | Action: index         |
 *  | GET | /api/published/models/${publishedModelId}/newerVersions | Action: newerVersions |
 * </pre>
 *
 * @see uk.ac.ox.softeng.maurodatamapper.federation.publish.PublishController
 */
@Integration
@Slf4j
class PublishFunctionalSpec extends FunctionalSpec implements XmlComparer {

    @Override
    String getResourcePath() {
        'published'
    }

    @Transactional
    String getFinalisedDataModelId() {
        DataModel.findByLabel('Finalised Example Test DataModel').id.toString()
    }

    Tuple2<String, String> getNewerDataModelIds() {
        loginAdmin()

        PUT("dataModels/${getFinalisedDataModelId()}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerPublicId = response.body().id

        PUT("dataModels/${newerPublicId}/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        PUT("dataModels/${newerPublicId}/readByEveryone", [:], MAP_ARG, true)
        verifyResponse OK, response

        PUT("dataModels/${newerPublicId}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerId = response.body().id

        PUT("dataModels/${newerId}/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        logout()

        new Tuple(newerPublicId, newerId)
    }

    void 'L01J : Get published models when not logged in as JSON'() {
        when:
        GET('models')

        then: 'The response is OK with no entries'
        verifyResponse(OK, response)
        verifyBaseJsonResponse(responseBody(), false)
    }

    void 'L01X : Get published models when not logged in as XML'() {
        when:
        HttpResponse<String> xmlResponse = GET('models?format=xml', STRING_ARG)

        then: "The response is OK with no entries"
        verifyResponse(OK, xmlResponse)
        verifyBaseXmlResponse(xmlResponse, false)
    }

    void 'R01J : Get published models when logged in as reader as JSON'() {
        given:
        loginReader()

        when:
        GET('models')

        then:
        verifyResponse(OK, response)
        verifyBaseJsonResponse(responseBody(), true)
        responseBody().publishedModels.size() == 3

        and:
        verifyJsonPublishedModel(responseBody().publishedModels.find {it.title == 'Simple Test CodeSet 1.0.0' }, 'CodeSet', 'codeSets', getCodeSetExporters())
        verifyJsonPublishedModel(responseBody().publishedModels.find {it.title == 'Complex Test CodeSet 1.0.0' }, 'CodeSet', 'codeSets', getCodeSetExporters())
        verifyJsonPublishedModel(responseBody().publishedModels.find {it.title == 'Finalised Example Test DataModel 1.0.0' }, 'DataModel', 'dataModels', getDataModelExporters())
    }

    void 'R01X : Get published models when logged in as reader as XML'() {
        given:
        loginReader()

        when:
        HttpResponse<String> xmlResponse = GET('models?format=xml', STRING_ARG)

        then:
        verifyResponse(OK, xmlResponse)
        GPathResult result = verifyBaseXmlResponse(xmlResponse, true)
        result.publishedModels.children().size() == 3

        and:
        verifyXmlPublishedModel(result.publishedModels.publishedModel.find {it.title == 'Simple Test CodeSet 1.0.0' }, 'CodeSet', 'codeSets', getCodeSetExporters())
        verifyXmlPublishedModel(result.publishedModels.publishedModel.find {it.title == 'Complex Test CodeSet 1.0.0' }, 'CodeSet', 'codeSets', getCodeSetExporters())
        verifyXmlPublishedModel(result.publishedModels.publishedModel.find {it.title == 'Finalised Example Test DataModel 1.0.0' }, 'DataModel', 'dataModels', getDataModelExporters())
    }

    void 'L02J : Test the newerVersions endpoint with no newer versions (as not logged in) as JSON'() {
        when:
        GET("models/${getFinalisedDataModelId()}/newerVersions")

        then:
        verifyResponse NOT_FOUND, response
    }

    void 'L02X : Test the newerVersions endpoint with no newer versions (as not logged in) as XML'() {
        when:
        GET("models/${getFinalisedDataModelId()}/newerVersions?format=xml")

        then:
        verifyResponse NOT_FOUND, response
    }

    void 'R02J : Test the newerVersions endpoint with no newer versions (as reader) as JSON'() {
        given:
        loginReader()

        when:
        GET("models/${getFinalisedDataModelId()}/newerVersions")

        then:
        verifyResponse OK, response
        verifyBaseNewerVersionsJsonResponse(responseBody(), false)
    }

    void 'R02X : Test the newerVersions endpoint with no newer versions (as reader) as XML'() {
        given:
        loginReader()

        when:
        HttpResponse<String> xmlResponse = GET("models/${getFinalisedDataModelId()}/newerVersions?format=xml", STRING_ARG)

        then:
        verifyResponse OK, xmlResponse
        verifyBaseNewerVersionsXmlResponse(xmlResponse, false)
    }

    void 'L03J : Test the newerVersions endpoint with newer versions (as not logged in) as JSON'() {
        given:
        Tuple tuple = getNewerDataModelIds()
        String newerPublicId = tuple.v1
        String newerId = tuple.v2

        when:
        GET("models/${getFinalisedDataModelId()}/newerVersions")

        then:
        verifyResponse NOT_FOUND, response

        cleanup:
        loginAdmin()
        DELETE("dataModels/$newerPublicId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$newerId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    void 'L03X : Test the newerVersions endpoint with newer versions (as not logged in) as XML'() {
        given:
        Tuple tuple = getNewerDataModelIds()
        String newerPublicId = tuple.v1
        String newerId = tuple.v2

        when:
        GET("models/${getFinalisedDataModelId()}/newerVersions?format=xml")

        then:
        verifyResponse NOT_FOUND, response

        cleanup:
        loginAdmin()
        DELETE("dataModels/$newerPublicId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$newerId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    void 'R03J : Test the newerVersions endpoint with newer versions (as reader) as JSON'() {
        given:
        Tuple tuple = getNewerDataModelIds()
        String newerPublicId = tuple.v1
        String newerId = tuple.v2
        loginReader()

        when:
        GET("models/${getFinalisedDataModelId()}/newerVersions")

        then:
        verifyResponse OK, response
        verifyBaseNewerVersionsJsonResponse(responseBody(), true)
        responseBody().newerPublishedModels.size() == 2

        and:
        verifyJsonPublishedModel(responseBody().newerPublishedModels.find {it.title == 'Finalised Example Test DataModel 2.0.0' }, 'DataModel', 'dataModels', getDataModelExporters(), true)
        verifyJsonPublishedModel(responseBody().newerPublishedModels.find {it.title == 'Finalised Example Test DataModel 3.0.0' }, 'DataModel', 'dataModels', getDataModelExporters(), true)

        cleanup:
        loginAdmin()
        DELETE("dataModels/$newerPublicId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$newerId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    void 'R03X : Test the newerVersions endpoint with newer versions (as reader) as XML'() {
        given:
        Tuple tuple = getNewerDataModelIds()
        String newerPublicId = tuple.v1
        String newerId = tuple.v2
        loginReader()

        when:
        HttpResponse<String> xmlResponse = GET("models/${getFinalisedDataModelId()}/newerVersions?format=xml", STRING_ARG)

        then:
        verifyResponse OK, xmlResponse
        GPathResult result = verifyBaseNewerVersionsXmlResponse(xmlResponse, true)
        result.newerPublishedModels.children().size() == 2

        and:
        verifyXmlPublishedModel(result.newerPublishedModels.publishedModel.find {it.title == 'Finalised Example Test DataModel 2.0.0' }, 'DataModel', 'dataModels', getDataModelExporters(), true)
        verifyXmlPublishedModel(result.newerPublishedModels.publishedModel.find {it.title == 'Finalised Example Test DataModel 3.0.0' }, 'DataModel', 'dataModels', getDataModelExporters(), true)

        cleanup:
        loginAdmin()
        DELETE("dataModels/$newerPublicId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$newerId?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    private void verifyJsonPublishedModel(Map publishedModel, String modelType, String modelEndpoint, Map<String, String> exporters, boolean newerVersion = false) {
        assert publishedModel
        assert publishedModel.modelId ==~ /\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
        assert publishedModel.label
        assert Version.from(publishedModel.version)
        assert publishedModel.title == publishedModel.label + ' ' + publishedModel.version
        assert publishedModel.modelType == modelType
        assert OffsetDateTime.parse(publishedModel.datePublished, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert OffsetDateTime.parse(publishedModel.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert OffsetDateTime.parse(publishedModel.dateCreated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert publishedModel.links.each {link ->
            assert link.contentType
            String exporterUrl = exporters.get(link.contentType)
            assert link.url ==~ /http:\/\/localhost:$serverPort\/api\/$modelEndpoint\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}\/export\\/$exporterUrl/
        }
        if (newerVersion) assert publishedModel.previousModelId ==~ /\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
    }

    private void verifyXmlPublishedModel(GPathResult publishedModel, String modelType, String modelEndpoint, Map<String, String> exporters, boolean newerVersion = false) {
        assert publishedModel
        assert publishedModel.modelId.text() ==~ /\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
        assert publishedModel.label.text()
        assert Version.from(publishedModel.version.text())
        assert publishedModel.title == publishedModel.label.text() + ' ' + publishedModel.version.text()
        assert publishedModel.modelType == modelType
        assert OffsetDateTime.parse(publishedModel.datePublished.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert OffsetDateTime.parse(publishedModel.lastUpdated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert OffsetDateTime.parse(publishedModel.dateCreated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert publishedModel.links.link.each {link ->
            assert link.contentType.text()
            String exporterUrl = exporters.get(link.contentType.text())
            assert link.url.text() ==~ /http:\/\/localhost:$serverPort\/api\/$modelEndpoint\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}\/export\\/$exporterUrl/
        }
        if (newerVersion) assert publishedModel.previousModelId.text() ==~ /\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
    }

    private void verifyBaseJsonResponse(Map<String, Object> responseBody, boolean expectEntries) {
        assert responseBody.authority.label == 'Mauro Data Mapper'
        assert responseBody.authority.url == 'http://localhost'
        assert OffsetDateTime.parse(responseBody.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        if (expectEntries) {
            assert responseBody.publishedModels.size() > 0
        } else {
            assert responseBody.publishedModels.size() == 0
        }
    }

    private GPathResult verifyBaseXmlResponse(HttpResponse<String> xmlResponse, boolean expectEntries) {
        log.warn('XML \n{}', prettyPrintXml(xmlResponse.body()))

        GPathResult result = new XmlSlurper().parseText(xmlResponse.body())
        assert result.name() == 'index'
        assert result.authority.label == 'Mauro Data Mapper'
        assert result.authority.url == 'http://localhost'
        assert OffsetDateTime.parse(result.lastUpdated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        if (expectEntries) {
            assert result.publishedModels.publishedModel.size() > 0
            assert result.publishedModels.publishedModel.size() == result.publishedModels.children().size()
        } else {
            assert result.publishedModels.children().size() == 0
        }

        result
    }

    private void verifyBaseNewerVersionsJsonResponse(Map<String, Object> responseBody, boolean expectEntries) {
        assert OffsetDateTime.parse(responseBody.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        if (expectEntries) {
            assert responseBody.newerPublishedModels.size() > 0
        } else {
            assert responseBody.newerPublishedModels.size() == 0
        }
    }

    private GPathResult verifyBaseNewerVersionsXmlResponse(HttpResponse<String> xmlResponse, boolean expectEntries) {
        log.warn('XML \n{}', prettyPrintXml(xmlResponse.body()))

        GPathResult result = new XmlSlurper().parseText(xmlResponse.body())
        assert result.name() == 'newerVersions'
        assert OffsetDateTime.parse(result.lastUpdated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        if (expectEntries) {
            assert result.newerPublishedModels.publishedModel.size() > 0
            assert result.newerPublishedModels.publishedModel.size() == result.newerPublishedModels.children().size()
        } else {
            assert result.newerPublishedModels.children().size() == 0
        }

        result
    }

    private static Map<String, String> getDataModelExporters() {
        [
            'application/mdm+json': 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1',
            'application/mdm+xml' : 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelXmlExporterService/5.1'
        ]
    }

    private static Map<String, String> getCodeSetExporters() {
        [
            'application/mdm+json': 'uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
            'application/mdm+xml' : 'uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetXmlExporterService/5.0'
        ]
    }
}
