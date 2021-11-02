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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.admin

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.test.csv.CsvComparer
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlComparer
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.junit.Assert

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

@Integration
@Slf4j
class ApiPropertyFunctionalSpec extends FunctionalSpec implements CsvComparer, XmlComparer {

    @Override
    String getResourcePath() {
        'admin/properties'
    }

    Map getValidJson() {
        [key     : 'functional.test.key',
         value   : 'Some random thing',
         category: 'Functional Test']
    }

    Map getInvalidJson() {
        [key     : 'functional test key',
         value   : 'Some random thing',
         category: 'Functional Test']
    }

    Map getValidUpdateJson() {
        [key  : 'functional.test.key',
         value: 'Some different random thing']
    }

    String getValidCsv() {
        "id,key,value,category,publiclyVisible,lastUpdatedBy,createdBy,lastUpdated\r\n" +
        "e2b3398f-f3e5-4d70-8793-25526bbe0dbe,a.csv.key,a.csv.value,csvs,false,updater@example.com,creator@example.com,2021-10-27T11:02:32.682Z"
    }

    String getValidXml() {
        """\
        <?xml version='1.0'?>
        <apiProperty>
            <id>e2b3398f-f3e5-4d70-8793-25526bbe0dbe</id>
            <key>functional.test.key</key>
            <value>Some random thing</value>
            <category>Functional Test</category>
        </apiProperty>""".stripIndent()
    }

    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "key": "functional.test.key",
  "value": "Some random thing",
  "category": "Functional Test",
  "publiclyVisible": false,
  "lastUpdatedBy": "admin@maurodatamapper.com",
  "createdBy": "admin@maurodatamapper.com",
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }

    String getShowXml() {
        """\
        <?xml version='1.0'?>
        <apiProperty>
            <id>91e1c67a-58fb-4610-929a-d823cf926bf2</id>
            <key>functional.test.key</key>
            <value>Some random thing</value>
            <category>Functional Test</category>
            <publiclyVisible>false</publiclyVisible>
            <lastUpdatedBy>admin@maurodatamapper.com</lastUpdatedBy>
            <createdBy>admin@maurodatamapper.com</createdBy>
            <lastUpdated>2021-10-28T13:49:24.804030+01:00</lastUpdated>
        </apiProperty>""".stripIndent()
    }

    String getShowCsv() {
        "id,key,value,category,publiclyVisible,lastUpdatedBy,createdBy,lastUpdated\n" +
        "91e1c67a-58fb-4610-929a-d823cf926bf2,functional.test.key,Some random thing,Functional Test,false,admin@maurodatamapper.com,admin@maurodatamapper.com,2021-10-29T10:33:23.142473+01:00"
    }

    Map getValidJsonCollection() {
        [count: 2,
         items: [
                 [key     : 'functional.test.key1',
                  value   : 'Some random thing1',
                  category: 'Functional Test'],
                 [key     : 'functional.test.key2',
                  value   : 'Some random thing2',
                  category: 'Functional Test']
         ]
        ]
    }

    String getValidXmlCollection() {
        """\
        <?xml version='1.0'?>
        <apiProperties>
            <count>2</count>
            <items>
                <apiProperty>
                    <id>e2b3398f-f3e5-4d70-8793-25526bbe0dbe</id>
                    <key>functional.test.xml.key.1</key>
                    <value>XML value 1</value>
                    <category>XMLTests</category>
                    <publiclyVisible>false</publiclyVisible>
                    <lastUpdatedBy>bootstrap.user@maurodatamapper.com</lastUpdatedBy>
                    <createdBy>example@maurodatamapper.com</createdBy>
                    <lastUpdated>2021-10-26T13:57:57.342140+01:00</lastUpdated>
                </apiProperty>
                <apiProperty>
                    <id>e2b3398f-f3e5-4d70-8793-25526bbe0dbf</id>
                    <key>functional.test.xml.key.2</key>
                    <value>XML value 2</value>
                    <category>XMLTests</category>
                    <publiclyVisible>false</publiclyVisible>
                    <lastUpdatedBy>bootstrap.user@maurodatamapper.com</lastUpdatedBy>
                    <createdBy>example@maurodatamapper.com</createdBy>
                    <lastUpdated>2021-10-26T13:57:57.342140+01:00</lastUpdated>
                </apiProperty>                
            </items>
        </apiProperties>
        """.stripIndent()
    }

    String getValidCsvCollection() {
        "id,key,value,category,publiclyVisible,lastUpdatedBy,createdBy,lastUpdated\r\n" +
        "e2b3398f-f3e5-4d70-8793-25526bbe0dbe,a.csv.collection.key,a.csv.collection.value,csvs,false,updater@example.com,creator@example.com,2021-10-27T11:02:32.682Z\r\n" +
        "d2b3398f-f3e5-4d70-8793-25526bbe0dbe,another.csv.collection.key,another.csv.collection.value,csvs,false,updater@example.com,creator@example.com,2021-10-27T11:02:32.682Z"
    }

    String getPublicIndexJson() {
        '{"count":0,"items":[]}'
    }

    String getAdminIndexJson() {
        '''{
}'''
    }

    /**
     * Items are created by the editor user
     * This ensures that they dont have some possible weird admin protection
     * @return
     */
    String getValidId(Map jsonMap = validJson) {
        loginAdmin()
        POST('', jsonMap)
        verifyResponse CREATED, response
        String id = response.body().id
        logout()
        id
    }

    void removeValidIdObject(String id) {
        removeValidIdObject(id, NO_CONTENT)
    }

    void removeValidIdObject(String id, HttpStatus expectedStatus) {
        if (!id) return
        log.info('Removing valid id {} using DELETE', id)
        loginAdmin()
        DELETE(id)
        verifyResponse expectedStatus, response
        logout()
    }

    void verifySameValidDataCreationResponse() {
        verifyResponse UNPROCESSABLE_ENTITY, response
        assert response.body().total == 1
        assert response.body().errors.first().message
    }

    /*
   * Logged in as admin testing
   * This proves that admin users can mess with items created by other users
   */

    void 'A01 : Test the index action (as admin)'() {
        given:
        loginAdmin()

        when: 'The index action is requested'
        GET('')

        then: 'The response is correct'
        verifyResponse OK, response
        ApiPropertyEnum.values()
            .findAll {!(it in [ApiPropertyEnum.SITE_URL])}
            .each {ape ->
                Assert.assertTrue "${ape.key} should exist", responseBody().items.any {
                    it.key == ape.key &&
                    it.category == 'Email'
                }
            }

        when: 'The index action is requested with XML format'
        GET('?format=xml', STRING_ARG)

        then: 'The response is correct'
        verifyResponse OK, jsonCapableResponse
        ApiPropertyEnum.values()
            .findAll {!(it in [ApiPropertyEnum.SITE_URL])}
            .each {ape ->
                Assert.assertTrue "${ape.key} should exist", jsonCapableResponse.body().toString().contains("<key>${ape.key}</key>")
            }

        when: 'The index action is requested with CSV format'
        GET('?format=csv', STRING_ARG)

        then: 'The response is correct'
        verifyResponse OK, jsonCapableResponse

        and:
        String[] lines = jsonCapableResponse.body().toString().split("\r\n")
        Assert.assertEquals "The header row is correct",
                "id,key,value,category,publiclyVisible,lastUpdatedBy,createdBy,lastUpdated",
                lines[0]

        and:
        ApiPropertyEnum.values()
            .findAll {!(it in [ApiPropertyEnum.SITE_URL])}
            .each {ape ->
                Assert.assertTrue "${ape.key} should exist", jsonCapableResponse.body().toString().contains("${ape.key},")
            }
    }

    void 'A02 : Test the show action correctly renders an instance (as admin)'() {
        given:
        def id = getValidId()
        loginAdmin()

        when: 'When the show action is called to retrieve a resource'
        GET("$id", STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse OK, showJson

        when: 'When the show action is called to retrieve a resource as XML'
        GET("$id?format=xml", STRING_ARG)

        then: 'The response is correct'
        verifyResponse OK, jsonCapableResponse
        Assert.assertTrue "Retrieved XML is as expected", compareXml(getShowXml(), jsonCapableResponse.body().toString())

        when: 'When the show action is called to retrieve a resource as CSV'
        GET("$id?format=csv", STRING_ARG)

        then: 'The response is correct'
        verifyResponse OK, jsonCapableResponse
        Assert.assertTrue "Retrieved CSV is as expected", compareCsv(getShowCsv(), jsonCapableResponse.body().toString())

        cleanup:
        removeValidIdObject(id)
    }

    /*
  * Logged in as admin testing
  * This proves that admin users can mess with items created by other users
  */

    void 'A03 : Test the save action correctly persists an instance (as admin)'() {
        given:
        loginAdmin()

        when:
        POST('', validJson)

        then:
        verifyResponse CREATED, response
        response.body().id

        when: 'Trying to save again using the same info'
        String id1 = response.body().id
        POST('', validJson)

        then:
        verifySameValidDataCreationResponse()
        String id2 = response.body()?.id

        cleanup:
        removeValidIdObject(id1)
        if (id2) {
            removeValidIdObject(id2) // not expecting anything, but just in case
        }
    }

    void 'A03-csv : Test the save action correctly persists an instance from CSV (as admin)'() {
        given:
        loginAdmin()

        when:
        POST('', getValidCsv(), MAP_ARG, false, 'text/csv')

        then:
        verifyResponse CREATED, response
        response.body().id

        when: 'Trying to save again using the same info'
        String id1 = response.body().id
        POST('', getValidCsv(), MAP_ARG, false, 'text/csv')

        then:
        verifySameValidDataCreationResponse()
        String id2 = response.body()?.id

        cleanup:
        removeValidIdObject(id1)
        if (id2) {
            removeValidIdObject(id2) // not expecting anything, but just in case
        }
    }

    void 'A03-xml : Test the save action correctly persists an instance from XML (as admin)'() {
        given:
        loginAdmin()

        when:
        POST('', getValidXml(), MAP_ARG, false, 'application/xml')

        then:
        verifyResponse CREATED, response
        response.body().id

        when: 'Trying to save again using the same info'
        String id1 = response.body().id
        POST('', getValidXml(), MAP_ARG, false, 'application/xml')

        then:
        verifySameValidDataCreationResponse()
        String id2 = response.body()?.id

        cleanup:
        removeValidIdObject(id1)
        if (id2) {
            removeValidIdObject(id2) // not expecting anything, but just in case
        }
    }

    void 'A04 : Test the delete action correctly deletes an instance (as admin)'() {
        given:
        def id = getValidId()
        loginAdmin()

        when: 'When the delete action is executed on an unknown instance'
        DELETE("${UUID.randomUUID()}")

        then: 'The response is correct'
        verifyResponse NOT_FOUND, response

        when: 'When the delete action is executed on an existing instance'
        DELETE("$id")

        then: 'The response is correct'
        verifyResponse NO_CONTENT, response

        cleanup:
        removeValidIdObject(id, NOT_FOUND)
    }

    /*
   * Logged in as admin testing
   * This proves that admin users can mess with items created by other users
   */

    void 'A05 : Test the update action correctly updates an instance (as admin)'() {
        given:
        def id = getValidId()
        loginAdmin()

        when: 'The update action is called with invalid data'
        PUT("$id", invalidJson)

        then: 'The response is correct'
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: 'The update action is called with valid data'
        PUT("$id", validUpdateJson)

        then: 'The response is correct'
        verifyResponse OK, response
        response.body().id == id
        validUpdateJson.each {k, v ->
            if (v instanceof Map) {
                v.each {k1, v1 ->
                    assert response.body()[k][k1] == v1
                }
            } else {
                assert response.body()[k] == v
            }
        }

        cleanup:
        removeValidIdObject(id)
    }

    void 'A06-json Test the apply action correctly persists a collection of instances from JSON'() {
        given:
        loginAdmin()

        when: 'The save action is executed with valid JSON collection data'
        log.debug('Valid content save')
        POST('/apply', getValidJsonCollection(), MAP_ARG, false)

        then: 'The response is correct and contains properties with keys functional.test.key1 and functional.test.key2'
        verifyResponse(OK, response)
        response.body().items.any{it.key == "functional.test.key1"}
        response.body().items.any{it.key == "functional.test.key2"}

        cleanup:
        String id1 = response.body().items.find{it.key == "functional.test.key1"}.id
        String id2 = response.body().items.find{it.key == "functional.test.key2"}.id
        removeValidIdObject(id1)
        removeValidIdObject(id2)
    }

    void 'A06-csv Test the apply action correctly persists a collection of instances from CSV'() {
        given:
        loginAdmin()

        when: 'The save action is executed with valid CSV collection data'
        log.debug('Valid content save')
        POST('/apply', getValidCsvCollection(), MAP_ARG, false, 'text/csv')

        then: 'The response is correct and contains properties with keys functional.test.key1 and functional.test.key2'
        verifyResponse(OK, response)
        response.body().items.any{it.key == "a.csv.collection.key"}
        response.body().items.any{it.key == "another.csv.collection.key"}

        and: 'The lastUpdatedBy and createdBy properties were ignored from the posted data'
        response.body().items.find{it.key == "a.csv.collection.key"}.lastUpdatedBy == "admin@maurodatamapper.com"
        response.body().items.find{it.key == "another.csv.collection.key"}.lastUpdatedBy == "admin@maurodatamapper.com"
        response.body().items.find{it.key == "a.csv.collection.key"}.createdBy == "admin@maurodatamapper.com"
        response.body().items.find{it.key == "another.csv.collection.key"}.createdBy == "admin@maurodatamapper.com"

        cleanup:
        String id1 = response.body().items.find{it.key == "a.csv.collection.key"}.id
        String id2 = response.body().items.find{it.key == "another.csv.collection.key"}.id
        removeValidIdObject(id1)
        removeValidIdObject(id2)
    }

    void 'A06-xml Test the apply action correctly persists a collection of instances from XML'() {
        given:
        loginAdmin()

        when: 'The save action is executed with valid CSV collection data'
        log.debug('Valid content save')
        POST('/apply', getValidXmlCollection(), MAP_ARG, false, 'application/xml')

        then: 'The response is correct and contains properties with keys functional.test.key1 and functional.test.key2'
        verifyResponse(OK, response)
        response.body().items.any{it.key == "functional.test.xml.key.1"}
        response.body().items.any{it.key == "functional.test.xml.key.2"}

        and: 'The lastUpdatedBy and createdBy properties were ignored from the posted data'
        response.body().items.find{it.key == "functional.test.xml.key.1"}.lastUpdatedBy == "admin@maurodatamapper.com"
        response.body().items.find{it.key == "functional.test.xml.key.2"}.lastUpdatedBy == "admin@maurodatamapper.com"
        response.body().items.find{it.key == "functional.test.xml.key.1"}.createdBy == "admin@maurodatamapper.com"
        response.body().items.find{it.key == "functional.test.xml.key.2"}.createdBy == "admin@maurodatamapper.com"

        cleanup:
        String id1 = response.body().items.find{it.key == "functional.test.xml.key.1"}.id
        String id2 = response.body().items.find{it.key == "functional.test.xml.key.2"}.id
        removeValidIdObject(id1)
        removeValidIdObject(id2)
    }

    void 'EXX : Test editor endpoints are all forbidden'() {
        given:
        def id = getValidId()
        loginEditor()

        when: 'index'
        GET('')

        then:
        verifyForbidden(response)

        when: 'show'
        GET(id)

        then:
        verifyForbidden(response)

        when: 'save json'
        POST('', validJson)

        then:
        verifyForbidden(response)

        when: 'save csv'
        POST('', getValidCsv(), MAP_ARG, false, 'text/csv')

        then:
        verifyForbidden(response)

        when: 'save xml'
        POST('', getValidXml(), MAP_ARG, false, 'application/xml')

        then:
        verifyForbidden(response)

        when: 'apply json'
        POST('/apply', getValidJsonCollection(), MAP_ARG, false)

        then:
        verifyForbidden(response)

        when: 'apply csv'
        POST('/apply', getValidCsvCollection(), MAP_ARG, false, 'text/csv')

        then:
        verifyForbidden(response)

        when: 'apply xml'
        POST('/apply', getValidXmlCollection(), MAP_ARG, false, 'application/xml')

        then:
        verifyForbidden(response)

        when: 'update'
        PUT(id, validUpdateJson)

        then:
        verifyForbidden(response)

        when: 'delete'
        DELETE(id)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'LXX : Test not logged endpoints are all forbidden'() {
        given:
        def id = getValidId()

        when: 'index'
        GET('')

        then:
        verifyForbidden(response)

        when: 'show'
        GET(id)

        then:
        verifyForbidden(response)

        when: 'save json'
        POST('', validJson)

        then:
        verifyForbidden(response)

        when: 'save csv'
        POST('', getValidCsv(), MAP_ARG, false, 'text/csv')

        then:
        verifyForbidden(response)

        when: 'save xml'
        POST('', getValidXml(), MAP_ARG, false, 'application/xml')

        then:
        verifyForbidden(response)

        when: 'apply json'
        POST('/apply', getValidJsonCollection(), MAP_ARG, false)

        then:
        verifyForbidden(response)

        when: 'apply csv'
        POST('/apply', getValidCsvCollection(), MAP_ARG, false, 'text/csv')

        then:
        verifyForbidden(response)

        when: 'apply xml'
        POST('/apply', getValidXmlCollection(), MAP_ARG, false, 'application/xml')

        then:
        verifyForbidden(response)

        when: 'update'
        PUT(id, validUpdateJson)

        then:
        verifyForbidden(response)

        when: 'delete'
        DELETE(id)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'NXX : Test logged in/authenticated endpoints are all forbidden'() {
        given:
        def id = getValidId()
        loginAuthenticated()

        when: 'index'
        GET('')

        then:
        verifyForbidden(response)

        when: 'show'
        GET(id)

        then:
        verifyForbidden(response)

        when: 'save json'
        POST('', validJson)

        then:
        verifyForbidden(response)

        when: 'save csv'
        POST('', getValidCsv(), MAP_ARG, false, 'text/csv')

        then:
        verifyForbidden(response)

        when: 'save xml'
        POST('', getValidXml(), MAP_ARG, false, 'application/xml')

        then:
        verifyForbidden(response)

        when: 'apply json'
        POST('/apply', getValidJsonCollection(), MAP_ARG, false)

        then:
        verifyForbidden(response)

        when: 'apply csv'
        POST('/apply', getValidCsvCollection(), MAP_ARG, false, 'text/csv')

        then:
        verifyForbidden(response)

        when: 'apply xml'
        POST('/apply', getValidXmlCollection(), MAP_ARG, false, 'application/xml')

        then:
        verifyForbidden(response)

        when: 'update'
        PUT(id, validUpdateJson)

        then:
        verifyForbidden(response)

        when: 'delete'
        DELETE(id)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }

    void 'RXX : Test reader endpoints are all forbidden'() {
        given:
        def id = getValidId()
        loginReader()

        when: 'index'
        GET('')

        then:
        verifyForbidden(response)

        when: 'show'
        GET(id)

        then:
        verifyForbidden(response)

        when: 'save json'
        POST('', validJson)

        then:
        verifyForbidden(response)

        when: 'save csv'
        POST('', getValidCsv(), MAP_ARG, false, 'text/csv')

        then:
        verifyForbidden(response)

        when: 'save xml'
        POST('', getValidXml(), MAP_ARG, false, 'application/xml')

        then:
        verifyForbidden(response)

        when: 'apply json'
        POST('/apply', getValidJsonCollection(), MAP_ARG, false)

        then:
        verifyForbidden(response)

        when: 'apply csv'
        POST('/apply', getValidCsvCollection(), MAP_ARG, false, 'text/csv')

        then:
        verifyForbidden(response)

        when: 'apply xml'
        POST('/apply', getValidXmlCollection(), MAP_ARG, false, 'application/xml')

        then:
        verifyForbidden(response)

        when: 'update'
        PUT(id, validUpdateJson)

        then:
        verifyForbidden(response)

        when: 'delete'
        DELETE(id)

        then:
        verifyForbidden(response)

        cleanup:
        removeValidIdObject(id)
    }


    def 'check public api property endpoint with no additional properties'() {
        when:
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody() == [count: 0, items: []]

    }

    def 'check public api property endpoint with public additional property'() {
        given:
        Map publicProperty = getValidJson()
        publicProperty.publiclyVisible = true
        String id = getValidId(publicProperty)

        when: 'not logged in'
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items[0].id == id


        when: 'reader'
        loginReader()
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items[0].id == id

        when: 'authenticated'
        loginAuthenticated()
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items[0].id == id

        when: 'editor'
        loginEditor()
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items[0].id == id

        cleanup:
        removeValidIdObject(id)

    }
}
