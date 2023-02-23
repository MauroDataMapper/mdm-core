/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.federation.atom

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlValidator

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import org.grails.web.mapping.DefaultLinkGenerator
import spock.lang.Shared

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

@Slf4j
@Integration
class FeedFunctionalSpec extends BaseFunctionalSpec implements XmlValidator {

    DefaultLinkGenerator grailsLinkGenerator

    @Shared
    String folderId

    @Override
    String getResourcePath() {
        'feeds'
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
        log.debug('CleanupSpec FeedFunctionalSpec')
        cleanUpResources(DataModel, Folder)
    }

    void 'F01 : Test getting published models when no models available'() {
        when:
        HttpResponse<String> localResponse = GET('all', STRING_ARG, false, MimeType.ATOM_XML.name)

        then:
        verifyBaseAtomResponse(localResponse, false, 'localhost', "http://localhost:$serverPort")

        and:
        validateXml('feed', '1.1', localResponse.body())
    }

    void 'F02 : Test getting published models when model available'() {
        given:
        POST("folders/${folderId}/dataModels", [
            label             : 'FunctionalTest DataModel',
            readableByEveryone: true,
            finalised         : true,
            dateFinalised     : OffsetDateTimeConverter.toString(OffsetDateTime.now()),
            modelVersion      : '1.0.0'
        ], MAP_ARG, true)
        verifyResponse(HttpStatus.CREATED, response)

        when:
        HttpResponse<String> localResponse = GET('all', STRING_ARG, false, MimeType.ATOM_XML.name)

        then:
        GPathResult feed = verifyBaseAtomResponse(localResponse, true, 'localhost', "http://localhost:$serverPort")
        feed.entry.size() == 1
        verifyEntry(feed.entry.find {it.title == 'FunctionalTest DataModel'}, 'DataModel', '1.0.0', "http://localhost:$serverPort", 'dataModels', getDataModelExporters())

        and:
        validateXml('feed', '1.1', localResponse.body())
    }

    void 'F03 : Test links render when site url property set'() {
        given:
        POST('admin/properties', [
            key  : ApiPropertyEnum.SITE_URL.toString(),
            value: 'https://www.mauro-data-mapper.com/cdw'
        ], MAP_ARG, true)
        verifyResponse CREATED, response
        String sitePropertyId = responseBody().id

        when:
        HttpResponse<String> xmlResponse = GET('all', STRING_ARG)

        then:
        GPathResult feed = verifyBaseAtomResponse(xmlResponse, true, 'www.mauro-data-mapper.com', 'https://www.mauro-data-mapper.com/cdw', '/cdw')

        when:
        def selfLink = feed.link.find {it.@rel == 'self'}

        then:
        selfLink
        selfLink.@href == 'https://www.mauro-data-mapper.com/cdw/api/feeds/all'

        and:
        verifyEntry(feed.entry.find {it.title == 'FunctionalTest DataModel'}, 'DataModel', '1.0.0', 'https://www.mauro-data-mapper.com/cdw', 'dataModels',
                    getDataModelExporters())

        and:
        validateXml('feed', '1.1', xmlResponse.body())

        cleanup:
        DELETE("admin/properties/$sitePropertyId", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        grailsLinkGenerator.setConfiguredServerBaseURL(null)
    }

    /**
     * Check that the response - which is expected to be XML as Atom, looks OK.
     */
    private GPathResult verifyBaseAtomResponse(HttpResponse<String> xmlResponse, boolean expectEntries, String host, String linkBaseUrl, String contextPath = '') {
        log.warn('XML \n{}', prettyPrintXml(xmlResponse.body()))

        //Use the jsonCapableResponse even though it is a string of XML
        assert xmlResponse.status() == OK

        //Slurp the response
        GPathResult result = new XmlSlurper().parseText(xmlResponse.body())
        assert result.name() == 'feed'
        assert result.namespaceURI() == 'http://www.w3.org/2005/Atom'
        assert result.lookupNamespace('mdm') == 'http://maurodatamapper.com/syndication/extensions/1.0'
        assert result.title == 'Mauro Data Mapper - All Models'
        assert result.id == "tag:$host,2021-01-27:$contextPath/api/feeds/all"
        assert result.author.name == 'Mauro Data Mapper'
        assert result.author.uri == 'http://localhost'
        assert OffsetDateTime.parse(result.updated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        assert result.link.size() == 1
        assert result.link.@rel == 'self'
        assert result.link.@type == 'application/atom+xml'
        assert result.link.@href == "$linkBaseUrl/api/feeds/all"
        assert result.link.@title == 'Mauro Data Mapper - All Models'

        if (expectEntries) {
            assert result.entry.size() > 0
        } else {
            assert result.entry.size() == 0
        }
        result
    }

    private void verifyEntry(def entry, String category, String version, String linkBaseUrl, String modelEndpoint, Map<String, String> exporters) {
        assert entry.id.text() ==~ /urn:uuid:\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
        assert OffsetDateTime.parse(entry.updated.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert OffsetDateTime.parse(entry.published.text(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert entry.category.@term == category
        assert entry.contentItemVersion.text() == version

        assert entry.link.size() == 2
        entry.link.each {it ->
            assert it.@rel == 'alternate'
            String contentType = it.@type
            assert contentType
            String exporterUrl = exporters.get(contentType)
            assert it.@href ==~ /$linkBaseUrl\/api\/${modelEndpoint}\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}\/export\/${exporterUrl}/
        }
    }

    private static Map<String, String> getDataModelExporters() {
        [
            'application/mauro.datamodel+json': 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.2',
            'application/mauro.datamodel+xml' : 'uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelXmlExporterService/5.2'
        ]
    }
}
