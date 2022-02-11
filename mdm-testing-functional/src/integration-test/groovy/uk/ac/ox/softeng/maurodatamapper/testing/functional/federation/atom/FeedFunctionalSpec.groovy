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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.federation.atom

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlComparer
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.CREATED
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

    @Override
    String getResourcePath() {
        ''
    }

    void 'Get Atom feed when not logged in'() {
        when:
        HttpResponse<String> xmlResponse = GET("feeds/all", STRING_ARG)

        then: "The response is OK with no entries"
        verifyBaseAtomResponse(xmlResponse, false, 'localhost')
    }

    void 'Get Atom feed when logged in as reader'() {
        given:
        loginReader()

        when:
        HttpResponse<String> xmlResponse = GET("feeds/all", STRING_ARG)

        then:
        GPathResult feed = verifyBaseAtomResponse(xmlResponse, true, 'localhost')
        feed.entry.size() == 3
        verifyEntry(feed.entry.find { it.title == 'Simple Test CodeSet 1.0.0' }, 'CodeSet', "http://localhost:$serverPort", 'codeSets')
        verifyEntry(feed.entry.find { it.title == 'Complex Test CodeSet 1.0.0' }, 'CodeSet', "http://localhost:$serverPort", 'codeSets')
        verifyEntry(feed.entry.find { it.title == 'Finalised Example Test DataModel 1.0.0' }, 'DataModel', "http://localhost:$serverPort", 'dataModels')
    }

    void 'test links render when site url property set'() {
        given:
        loginAdmin()
        POST('admin/properties', [
            key  : ApiPropertyEnum.SITE_URL.toString(),
            value: 'https://www.mauro-data-mapper.com/cdw'
        ])
        verifyResponse CREATED, response

        when:
        HttpResponse<String> xmlResponse = GET("feeds/all", STRING_ARG)

        then:
        GPathResult feed = verifyBaseAtomResponse(xmlResponse, true, 'www.mauro-data-mapper.com', '/cdw')

        when:
        def selfLink = feed.link.find { it.@rel == 'self' }

        then:
        selfLink
        selfLink.@href == 'https://www.mauro-data-mapper.com/cdw/api/feeds/all'

        and:
        verifyEntry(feed.entry.find { it.title == 'Simple Test CodeSet 1.0.0' }, 'CodeSet', 'https://www.mauro-data-mapper.com/cdw', 'codeSets')
        verifyEntry(feed.entry.find { it.title == 'Complex Test CodeSet 1.0.0' }, 'CodeSet', 'https://www.mauro-data-mapper.com/cdw', 'codeSets')
        verifyEntry(feed.entry.find { it.title == 'Finalised Example Test DataModel 1.0.0' }, 'DataModel', 'https://www.mauro-data-mapper.com/cdw', 'dataModels')
    }

    /**
     * Check that the response - which is expected to be XML as Atom, looks OK.
     */
    private GPathResult verifyBaseAtomResponse(HttpResponse<String> xmlResponse, boolean expectEntries, String host, String contextPath = '') {
        log.warn('XML \n{}', prettyPrintXml(xmlResponse.body()))

        //Use the jsonCapableResponse even though it is a string of XML
        xmlResponse.status() == OK

        //Slurp the response
        GPathResult result = new XmlSlurper().parseText(xmlResponse.body())
        assert result.name() == 'feed'
        assert result.namespaceURI() == 'http://www.w3.org/2005/Atom'
        assert result.title == 'Mauro Data Mapper - All Models'
        assert result.id == "tag:$host,2021-01-27:$contextPath/api/feeds/all"
        assert result.author.name == 'Mauro Data Mapper'
        assert result.author.uri == 'http://localhost'

        assert result.link.size() == 2

        if (expectEntries) {
            assert result.entry.size() > 0
        } else {
            assert result.entry.size() == 0
        }
        result
    }

    private void verifyEntry(def entry, String category, String linkBaseUrl, String modelEndpoint) {
        assert entry.id.text() ==~ /urn:uuid:\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
        assert entry.updated.text()
        assert entry.published.text()
        assert entry.category.@term == category
        assert entry.link.size() == 2

        def selfLink = entry.link.find { it.@rel == 'self' }
        assert selfLink.@href ==~ /$linkBaseUrl\/api\/${modelEndpoint}\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/

        def altLink = entry.link.find { it.@rel == 'alternate' }
        assert altLink.@href ==~ /$linkBaseUrl\/api\/${modelEndpoint}\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
    }
}
