/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.federation.publish

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlValidator
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * @see PublishController* Controller: publish
 *  | GET | /api/published/models                                   | Action: index         |
 *  | GET | /api/published/models/${publishedModelId}/newerVersions | Action: newerVersions |
 *
 */
@Slf4j
@Integration
class PublishFunctionalSpec extends BaseFunctionalSpec implements XmlValidator {

    @Shared
    String folderId

    @Shared
    String dataModelId

    @Override
    String getResourcePath() {
        'published'
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data for FeedFunctionalSpec')
        folderId = new Folder(label: 'Functional Test Folder', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id.toString()
        assert folderId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PublishFunctionalSpec')
        cleanUpResources(DataModel, Folder)
    }

    Tuple2<String, String> getNewerDataModelIds() {
        PUT("dataModels/${dataModelId}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerId1 = response.body().id

        PUT("dataModels/${newerId1}/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        PUT("dataModels/${newerId1}/newBranchModelVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String newerId2 = response.body().id

        PUT("dataModels/${newerId2}/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response

        new Tuple(newerId1, newerId2)
    }

    void 'P01J : Get published models as JSON'() {
        when:
        GET('models')

        then:
        verifyResponse OK, response
        verifyBaseJsonResponse(responseBody(), false)
    }

    void 'P01X : Get published models as XML'() {
        when:
        HttpResponse<String> xmlResponse = GET('models?format=xml', STRING_ARG)

        then:
        verifyResponse OK, xmlResponse
        verifyBaseXmlResponse(xmlResponse, false)

        and:
        validateXml('publishedModels', '1.0', xmlResponse.body())
    }

    void 'P02J : Get published models when model available as JSON'() {
        given:
        String publishedDateStr = '2021-06-28T12:36:37Z'
        POST("folders/${folderId}/dataModels", [
            label             : 'FunctionalTest DataModel',
            readableByEveryone: true,
            finalised         : true,
            dateFinalised     : publishedDateStr,
            description       : 'Some random desc',
            modelVersion      : '1.0.0'
        ], MAP_ARG, true)
        verifyResponse(HttpStatus.CREATED, response)
        dataModelId = responseBody().id

        when:
        GET('models')

        then:
        verifyResponse OK, response
        verifyBaseJsonResponse(responseBody(), true)
        responseBody().publishedModels.size() == 1

        and:
        verifyJsonPublishedModel(responseBody().publishedModels.find {it.label == 'FunctionalTest DataModel' && it.version == '1.0.0'}, 'DataModel', 'dataModels', getDataModelExporters())

        when:
        Map<String, Object> publishedModel = responseBody().publishedModels.first()

        then:
        publishedModel.modelId == dataModelId
        publishedModel.description == 'Some random desc'
        publishedModel.datePublished == publishedDateStr
    }

    void 'P02X : Get published models when model available as XML'() {
        given:
        String publishedDateStr = '2021-06-28T12:36:37Z'

        when:
        HttpResponse<String> xmlResponse = GET('models?format=xml', STRING_ARG)

        then:
        verifyResponse OK, xmlResponse
        GPathResult result = verifyBaseXmlResponse(xmlResponse, true)
        result.publishedModels.children().size() == 1

        and:
        verifyXmlPublishedModel(result.publishedModels.publishedModel.find {it.label == 'FunctionalTest DataModel' && it.version == '1.0.0'}, 'DataModel', 'dataModels', getDataModelExporters())

        when:
        GPathResult publishedModel = result.publishedModels.publishedModel.first()

        then:
        publishedModel.modelId == dataModelId
        publishedModel.description == 'Some random desc'
        publishedModel.datePublished.text() == publishedDateStr

        and:
        validateXml('publishedModels', '1.0', xmlResponse.body())
    }

    void 'N01J : Test the newerVersions endpoint (with no newer versions) as JSON'() {
        when:
        GET("models/$dataModelId/newerVersions")

        then:
        verifyResponse OK, response
        verifyBaseNewerVersionsJsonResponse(responseBody(), false)
    }

    void 'N01X : Test the newerVersions endpoint (with no newer versions) as XML'() {
        when:
        HttpResponse<String> xmlResponse = GET("models/$dataModelId/newerVersions?format=xml", STRING_ARG)

        then:
        verifyResponse OK, xmlResponse
        verifyBaseNewerVersionsXmlResponse(xmlResponse, false)

        and:
        validateXml('newerPublishedModels', '1.0', xmlResponse.body())
    }

    void 'N02J : Test the newerVersions endpoint (with newer versions) as JSON'() {
        given:
        Tuple tuple = getNewerDataModelIds()

        when:
        GET("models/$dataModelId/newerVersions")

        then:
        verifyResponse OK, response
        verifyBaseNewerVersionsJsonResponse(responseBody(), true)
        responseBody().newerPublishedModels.size() == 2

        and:
        verifyJsonPublishedModel(responseBody().newerPublishedModels.find {it.label == 'FunctionalTest DataModel' && it.version == '2.0.0'}, 'DataModel', 'dataModels', getDataModelExporters(),
                                 true)
        verifyJsonPublishedModel(responseBody().newerPublishedModels.find {it.label == 'FunctionalTest DataModel' && it.version == '3.0.0'}, 'DataModel', 'dataModels', getDataModelExporters(),
                                 true)
        responseBody().newerPublishedModels.each {publishedModel ->
            assert publishedModel.description == 'Some random desc'
        }

        cleanup:
        DELETE("dataModels/${tuple.v1}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/${tuple.v2}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    void 'N02X : Test the newerVersions endpoint (with newer versions) as XML'() {
        given:
        Tuple tuple = getNewerDataModelIds()

        when:
        HttpResponse<String> xmlResponse = GET("models/$dataModelId/newerVersions?format=xml", STRING_ARG)

        then:
        verifyResponse OK, xmlResponse
        GPathResult result = verifyBaseNewerVersionsXmlResponse(xmlResponse, true)
        result.newerPublishedModels.children().size() == 2

        and:
        verifyXmlPublishedModel(result.newerPublishedModels.publishedModel.find {it.label == 'FunctionalTest DataModel' && it.version == '2.0.0'}, 'DataModel', 'dataModels',
                                getDataModelExporters(), true)
        verifyXmlPublishedModel(result.newerPublishedModels.publishedModel.find {it.label == 'FunctionalTest DataModel' && it.version == '3.0.0'}, 'DataModel', 'dataModels',
                                getDataModelExporters(), true)
        result.newerPublishedModels.publishedModel.each {publishedModel ->
            assert publishedModel.description == 'Some random desc'
        }

        and:
        validateXml('newerPublishedModels', '1.0', xmlResponse.body())

        cleanup:
        DELETE("dataModels/${tuple.v1}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/${tuple.v2}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    private void verifyJsonPublishedModel(Map publishedModel, String modelType, String modelEndpoint, Map<String, String> exporters, boolean newerVersion = false) {
        assert publishedModel
        assert publishedModel.modelId ==~ /\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
        assert publishedModel.label
        assert Version.from(publishedModel.version)
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
            'application/mauro.datamodel+json': 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.2',
            'application/mauro.datamodel+xml' : 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelXmlExporterService/5.2'
        ]
    }
}
