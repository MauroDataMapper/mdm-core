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

import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.ADMIN

@Integration
@Slf4j
class SubscribedCatalogueFunctionalSpec extends FunctionalSpec {

    @Shared
    Path resourcesPath

    @Override
    String getResourcePath() {
        "subscribedCatalogues"
    }

    String getValidId() {
        loginAdmin()
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()
        id
    }

    void removeValidIdObject(String id) {
        loginAdmin()
        DELETE(id)
        verifyResponse NO_CONTENT, response
        logout()
    }

    @Transactional
    void cleanUpRoles(String... ids) {
        log.info('Cleaning up roles and groups')
        log.debug('Cleaning up {} roles for ids {}', SecurableResourceGroupRole.count(), ids)
        SecurableResourceGroupRole.bySecurableResourceIds(ids.collect { Utils.toUuid(it) }).deleteAll()
        sessionFactory.currentSession.flush()
    }    

    Map getValidJson() {
        [
            url: 'http://localhost:' + serverPort,
            apiKey: '67421316-66a5-4830-9156-b1ba77bba5d1',
            label: 'Functional Test Label',
            description: 'Functional Test Description',
            refreshPeriod: 7
        ]
    }

    Map getInvalidJson() {
        [
            url: null,
            apiKey: '67421316-66a5-4830-9156-b1ba77bba5d1'
        ]
    }

    Map getValidUpdateJson() {
        [
            description: 'Functional Test Description Updated'
        ]
    }   

    /*
      * Logged in as editor testing
      */
    void 'E02 : Test the show and index action correctly renders an instance for set user (as editor)'() {
        given:
        String id = getValidId()
        loginEditor()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse OK, response

        when: 'When the index action is called'
        GET('')

        then:
        verifyResponse OK, response        

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /*
     * Logged out testing
     */
    void 'L02 : Test the show and index action does not render an instance for set user (not logged in)'() {
        given:
        String id = getValidId()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse NOT_FOUND, response

        when: 'When the index action is called'
        GET('')

        then:
        verifyResponse NOT_FOUND, response        

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /*
     * Logged out testing
     */
    void 'L02a : Test the index action for format opml responds when not logged in'() {
        given:
        String id = getValidId()

        //note: need to assign the GET return to response because this is not json
        when: 'When the index action is called for opml format'
        response = GET('?format=opml', STRING_ARG)

        then:
        verifyResponse OK, response     
        assert response.body().startsWith('<?xml version="1.0" encoding="UTF-8"?><opml version="2.0">')   

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }    

    /**
     * Testing when logged in as a no access/authenticated user
     */
    void 'N02 : Test the show and index action correctly renders an instance for set user (as no access/authenticated)'() {
        given:
        String id = getValidId()
        loginAuthenticated()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse OK, response

        when: 'When the index action is called'
        GET('')

        then:
        verifyResponse OK, response        

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

    /**
     * Testing when logged in as a reader only user
     */
    void 'R02 : Test the show and index action correctly renders an instance for set user (as reader)'() {
        given:
        String id = getValidId()
        loginReader()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse OK, response

        when: 'When the index action is called'
        GET('')

        then:
        verifyResponse OK, response        

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

   /*
    * Logged in as admin testing
    */
    void 'A02 : Test the show action correctly renders an instance for set user (as admin)'() {
        given:
        String id = getValidId()
        loginAdmin()

        when: 'When the show action is called to retrieve a resource'
        GET(id)

        then:
        verifyResponse OK, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

  /*
   * Logged in as editor testing
   */
   void 'E03a : Test the save action is forbidden (as editor)'() {
        given:
        loginEditor()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response
    }

   void 'E03b : Test the save action is forbidden when using  PUT (as editor)'() {
        given:
        String id = getValidId()
        loginEditor()

        when: 'The save action is executed with invalid data'
        PUT(id, invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        PUT(id, validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)   
        cleanUpRoles(id)     
    }    

    void 'E04 : Test the delete action is forbidden (as editor)'() {
        given:
        String id = getValidId()
        loginEditor()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)   
        cleanUpRoles(id)       
    }

    /*
     * Logged out testing
     */

   void 'L03a : Test the save action is not found (as not logged in)'() {
        given:

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response
    }

   void 'L03b : Test the save action is not found when using  PUT (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'The save action is executed with invalid data'
        PUT(id, invalidJson)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        when: 'The save action is executed with valid data'
        PUT(id, validJson)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        cleanup:
        removeValidIdObject(id) 
        cleanUpRoles(id)       
    }    

    void 'L04 : Test the delete action is not found (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is not found'
        verifyResponse NOT_FOUND, response

        cleanup:
        removeValidIdObject(id)  
        cleanUpRoles(id)        
    }

    /**
     * Testing when logged in as a no access/authenticated user
     */

   void 'N03a : Test the save action is forbidden (as authenticated)'() {
        given:
        loginAuthenticated()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response
    }

   void 'N03b : Test the save action is forbidden when using  PUT (as authenticated)'() {
        given:
        String id = getValidId()
        loginAuthenticated()

        when: 'The save action is executed with invalid data'
        PUT(id, invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        PUT(id, validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)    
    }    

    void 'N04 : Test the delete action is forbidden (as authenticated)'() {
        given:
        String id = getValidId()
        loginAuthenticated()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)      
    }

    /**
     * Testing when logged in as a reader only user
     */

   void 'R03a : Test the save action is forbidden (as reader)'() {
        given:
        loginReader()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response
    }

   void 'R03b : Test the save action is forbidden when using  PUT (as reader)'() {
        given:
        String id = getValidId()
        loginReader()

        when: 'The save action is executed with invalid data'
        PUT(id, invalidJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        when: 'The save action is executed with valid data'
        PUT(id, validJson)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }    

    void 'R04 : Test the delete action is forbidden (as reader)'() {
        given:
        String id = getValidId()
        loginReader()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is forbidden'
        verifyResponse FORBIDDEN, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)   
    }

    /*
    * Logged in as admin testing
    * This proves that admin users can mess with items created by other users
    */
   void 'A03a : Test the save action is ok (as admin)'() {
        given:
        loginAdmin()

        when: 'The save action is executed with no content'
        POST('', [:])

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with invalid data'
        POST('', invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse CREATED, response
        String id = responseBody().id

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }

   void 'A03b : Test the save action is OK when using PUT (as admin)'() {
        given:
        String id = getValidId()
        loginAdmin()

        when: 'The save action is executed with invalid data'
        PUT(id, invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The save action is executed with valid data'
        PUT(id, validJson)

        then: 'The response is not found'
        verifyResponse OK, response

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id) 
    }

   void 'A03c : Test that duplicate label is unprocessable (as admin)'() {
        given:
        loginAdmin()

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse CREATED, response
        String id = responseBody().id

        when: 'The save action is executed again with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response    

        cleanup:
        removeValidIdObject(id)
        cleanUpRoles(id)
    }       

    void 'A04 : Test the delete action is ok (as admin)'() {
        given:
        String id = getValidId()
        loginAdmin()

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is not correct'
        verifyResponse NO_CONTENT, response

        cleanup:
        cleanUpRoles(id) 
    }

    /**
     * Test the availableModels endpoint. This would be on a remote host, but in this functional test
     * we use the localhost. Test setup and execution is as follows:
     * 1. Login as Admin and create an API Key for Admin
     * 2. Subscribe to the local catalogue (in real life this would be remote), specifying the API key created above
     * 3. Get the local /availableModels endpoint. In real life this would connect to /api/feeds/all on the remote,
     * but here we use the local.
     * 4. Cleanup
     */
    void 'Test the availableModels endpoint'() {
        
        given:
        Map apiKeyJson = [
            name: "Functional Test",
            expiryDate : LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)
        ]

        when:
        loginAdmin()
        POST("/api/catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys", apiKeyJson, MAP_ARG, true)

        then:
        verifyResponse CREATED, response
        String apiKey = responseBody().apiKey

        when:
        //note: using a groovy string like "http://localhost:$serverPort/" causes the url to be stripped when saving
        Map subscriptionJson = [
            url: 'http://localhost:' + serverPort + '/',
            apiKey: apiKey,
            label: 'Functional Test Label',
            description: 'Functional Test Description',
            refreshPeriod: 7
        ]
        POST('', subscriptionJson)
        
        then:
        verifyResponse CREATED, response
        String subscribedCatalogueId = responseBody().id

        when:
        GET("${subscribedCatalogueId}/availableModels", STRING_ARG)

        then:
        verifyJsonResponse OK, '''
        {
            "count": 1,
            "items": [
                {
                    "modelId": "${json-unit.matches:id}",
                    "label": "Simple Test CodeSet 1.0.0",
                    "description": "Simple Test CodeSet",
                    "modelType": "CodeSet"
                }
            ]
        }'''

        cleanup:
        DELETE("/api/catalogueUsers/${getUserByEmailAddress(ADMIN).id}/apiKeys/${apiKey}", MAP_ARG, true)
        removeValidIdObject(subscribedCatalogueId)
        cleanUpRoles(subscribedCatalogueId)
    }
}
