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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.federation.atom

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlComparer
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import io.micronaut.http.HttpResponse
import org.grails.web.mapping.DefaultLinkGenerator

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: feed
 *  |   GET   | /api/feeds/all       | Action: index
 * </pre>
 *
 * @see uk.ac.ox.softeng.maurodatamapper.federation.atom.FeedController
 */
@Integration
@Slf4j
class FeedFunctionalSpec extends FunctionalSpec implements XmlComparer {

    DefaultLinkGenerator grailsLinkGenerator

    @Override
    String getResourcePath() {
        ''
    }

    void 'F01 : Get Atom feed when not logged in'() {
        when:
        HttpResponse<String> xmlResponse = GET('feeds/all', STRING_ARG)

        then: "The response is OK with no entries"
        verifyBaseAtomResponse(xmlResponse, false, 'localhost', "http://localhost:$serverPort")
    }

    void 'F02 : Get Atom feed when logged in as reader'() {
        given:
        loginReader()

        when:
        HttpResponse<String> xmlResponse = GET('feeds/all', STRING_ARG)

        then:
        GPathResult feed = verifyBaseAtomResponse(xmlResponse, true, 'localhost', "http://localhost:$serverPort")
        feed.entry.size() == 3
        verifyEntry(feed.entry.find {it.title == 'Simple Test CodeSet'}, 'CodeSet', '1.0.0', "http://localhost:$serverPort", 'codeSets', getCodeSetExporters())
        verifyEntry(feed.entry.find {it.title == 'Complex Test CodeSet'}, 'CodeSet', '1.0.0', "http://localhost:$serverPort", 'codeSets', getCodeSetExporters())
        verifyEntry(feed.entry.find {it.title == 'Finalised Example Test DataModel'}, 'DataModel', '1.0.0', "http://localhost:$serverPort", 'dataModels', getDataModelExporters())
    }

    void 'F03 : Test links render when site url property set'() {
        given:
        loginAdmin()
        POST('admin/properties', [
            key  : ApiPropertyEnum.SITE_URL.toString(),
            value: 'https://www.mauro-data-mapper.com/cdw'
        ])
        verifyResponse CREATED, response
        String sitePropertyId = responseBody().id

        when:
        HttpResponse<String> xmlResponse = GET('feeds/all', STRING_ARG)

        then:
        GPathResult feed = verifyBaseAtomResponse(xmlResponse, true, 'www.mauro-data-mapper.com', 'https://www.mauro-data-mapper.com/cdw', '/cdw')

        when:
        def selfLink = feed.link.find {it.@rel == 'self'}

        then:
        selfLink
        selfLink.@href == 'https://www.mauro-data-mapper.com/cdw/api/feeds/all'

        and:
        verifyEntry(feed.entry.find {it.title == 'Simple Test CodeSet'}, 'CodeSet', '1.0.0', 'https://www.mauro-data-mapper.com/cdw', 'codeSets', getCodeSetExporters())
        verifyEntry(feed.entry.find {it.title == 'Complex Test CodeSet'}, 'CodeSet', '1.0.0', 'https://www.mauro-data-mapper.com/cdw', 'codeSets', getCodeSetExporters())
        verifyEntry(feed.entry.find {it.title == 'Finalised Example Test DataModel'}, 'DataModel', '1.0.0', 'https://www.mauro-data-mapper.com/cdw', 'dataModels',
                    getDataModelExporters())

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

    private static Map<String, String> getCodeSetExporters() {
        [
            'application/mauro.codeset+json': 'uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/4.0',
            'application/mauro.codeset+xml' : 'uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetXmlExporterService/5.0'
        ]
    }
}
