/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.federation

import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: feed
 *  |   GET   | /api/feeds/all       | Action: index
 * </pre>
 *
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.federation.FeedController
 */
@Integration
@Slf4j
class FeedFunctionalSpec extends FunctionalSpec {

    @Override
    String getResourcePath() {
        ''
    }

    void 'Get Atom feed when not logged in'() {

        when:
        GET("/api/feeds/all", STRING_ARG, true)

        then: "The response is OK with no entries"
        verifyAtomResponse(false)
    }

    void 'Get Atom feed when logged in as reader'() {
    
        given:
        loginReader()
       
        when:
        GET("/api/feeds/all", STRING_ARG, true)

        then: "The response is OK with no entries"
        verifyAtomResponse(true)
    }

    /**
     * Check that the response - which is expected to be XML as Atom, looks OK.
     * Despite the use of jsonCapableResponse below, this is actually XML.
     *
     */
    private void verifyAtomResponse(boolean expectEntries) {
        //Use the jsonCapableResponse even though it is a string of XML
        jsonCapableResponse.status() == OK

        //Slurp the response
        GPathResult result = new XmlSlurper().parseText(jsonCapableResponse.body.value.toString())
        result.name() == "feed"
        result.namespaceURI() == "http://www.w3.org/2005/Atom"
        result.title == "Mauro Data Mapper - All Models"

        if (expectEntries) {
            result.depthFirst().findAll{it.name() == "entry"}.size() > 0
        } else {
            result.depthFirst().findAll{it.name() == "entry"}.size() == 0
        }
        
    }

      
}
