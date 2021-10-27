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
package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlComparer

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.junit.Assert

import java.nio.file.Files
import java.nio.file.Path

@Integration
@Slf4j
class ApiPropertyFunctionalSpec extends ResourceFunctionalSpec<ApiProperty> implements XmlComparer {

    ApiPropertyService apiPropertyService

    @Override
    def checkResourceCount() {
        assert apiPropertyService.count() == 15
    }

    @Override
    void cleanUpData(String id = null) {
        if (id) {
            DELETE(getDeleteEndpoint(id))
            assert response.status() == HttpStatus.NO_CONTENT
            sleep(20)
        }
    }

    @Override
    String getResourcePath() {
        'admin/properties'
    }

    @Override
    Map getValidJson() {
        [key     : 'functional.test.key',
         value   : 'Some random thing',
         category: 'Functional Test']
    }

    @Override
    Map getInvalidJson() {
        [key     : 'functional test key',
         value   : 'Some random thing',
         category: 'Functional Test']
    }

    String getValidCsv() {
        "key,value,publiclyVisible,category\r\na.csv.key,a.csv.value,false,csvs"
    }

    String getValidXml() {
        "<?xml version='1.0'?><apiProperty><key>an.xml.key</key><value>An XML value</value><category>XML</category><publiclyVisible>false</publiclyVisible></apiProperty>"
    }

    String getSaveCollectionPath() {
        "${getSavePath()}/apply"
    }

    Map getValidJsonCollection() {
        [count: 2,
         items: [
                 [key     : 'functional.test.key1',
                  value   : 'Some random thing1',
                  category: 'Functional Test',
                  publiclyVisible: false,
                  lastUpdatedBy: 'hello@example.com',
                  lastUpdated: '2021-10-27T11:02:32.682Z'],
                 [key     : 'functional.test.key2',
                  value   : 'Some random thing2',
                  category: 'Functional Test',
                  publiclyVisible: false,
                  lastUpdatedBy: 'hello@example.com',
                  lastUpdated: '2021-10-27T11:02:32.682Z']
                ]
        ]
    }

    void verifyR1EmptyIndexResponse() {
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().count == 15

        ApiPropertyEnum.values()
            .findAll {!(it in [ApiPropertyEnum.SITE_URL, ApiPropertyEnum.EMAIL_FROM_ADDRESS])}
            .each {ape ->
                Assert.assertTrue "${ape.key} should exist", responseBody().items.any {
                    it.key == ape.key &&
                    it.category == 'Email'
                }
            }
    }

    void verifyR3IndexResponse(String expectedId) {
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().count == 16
        assert responseBody().items.size() == 16
        assert responseBody().items.any {it.id == expectedId}
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "key": "functional.test.key",
  "value": "Some random thing",
  "category": "Functional Test",
  "publiclyVisible": false,
  "lastUpdatedBy": "unlogged_user@mdm-core.com",
  "createdBy": "unlogged_user@mdm-core.com",
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }

    def 'check public api property endpoint with no additional properties'(){
        when:
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody() == [count: 0, items: []]

    }

    def 'check public api property endpoint with public additional property'(){
        given:
        Map publicProperty = getValidJson()
        publicProperty.publiclyVisible = true
        POST('',publicProperty)
        verifyResponse(HttpStatus.CREATED, response)
        assert responseBody().publiclyVisible
        String id = responseBody().id

        when:
        GET('properties', MAP_ARG, true)

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().count == 1
        responseBody().items[0].id == id

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT

    }

    void 'Test the save action correctly persists an instance from CSV'() {
        when: 'The save action is executed with valid CSV data'
        log.debug('Valid content save')
        POST(getSavePath(), getValidCsv(), MAP_ARG, true, 'text/csv')

        then: 'The response is correct'
        verifyResponse(HttpStatus.CREATED, response)

        cleanup:
        DELETE(getDeleteEndpoint(response.body().id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'Test the save action correctly persists an instance from XML'() {
        when: 'The save action is executed with valid XML data'
        log.debug('Valid content save')
        POST(getSavePath(), getValidXml(), MAP_ARG, true, 'application/xml')

        then: 'The response is correct'
        verifyResponse(HttpStatus.CREATED, response)

        cleanup:
        DELETE(getDeleteEndpoint(response.body().id))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'check index and show endpoints for CSV'(){
        when:
        GET('?format=csv', STRING_ARG)

        then:
        verifyResponse(HttpStatus.OK, jsonCapableResponse)
        String csv = jsonCapableResponse.body().toString()
        String[] lines = csv.split("\r\n")
        assert lines.size() == 17 //header + 16 rows
        assert lines[0] == "ID,Key,Value,Category,Publicly Visible,Last Updated By,Created By,Last Updated"
        String id = lines[1].split(",")[0]

        when:
        GET("${id}?format=csv", STRING_ARG)

        then:
        verifyResponse(HttpStatus.OK, jsonCapableResponse)
        String csv2 = jsonCapableResponse.body().toString()
        String[] lines2 = csv2.split("\r\n")
        assert lines2.size() == 2 //header + 1 rows
        assert lines2[0] == "ID,Key,Value,Category,Publicly Visible,Last Updated By,Created By,Last Updated"
    }

    void 'check index endpoint for XML'(){
        given:
        Path expectedIndexPath = xmlResourcesPath.resolve("apiProperties.xml")
        if (!Files.exists(expectedIndexPath)) {
            Assert.fail("Expected export file ${expectedIndexPath} does not exist")
        }
        String expectedIndexXml = Files.readString(expectedIndexPath)

        when:
        GET('?format=xml', STRING_ARG)

        then:
        verifyResponse(HttpStatus.OK, jsonCapableResponse)
        String actualIndexXml = jsonCapableResponse.body().toString()
        assert compareXml(expectedIndexXml, actualIndexXml)
    }

    void 'check show endpoint for XML'(){
        given:
        Path expectedShowPath = xmlResourcesPath.resolve("apiProperty.xml")
        if (!Files.exists(expectedShowPath)) {
            Assert.fail("Expected export file ${expectedShowPath} does not exist")
        }
        String expectedShowXml = Files.readString(expectedShowPath)

        // This is just to retrieve a valid ID for the GET of XML
        when:
        GET('')

        then:
        responseBody().count == 16
        responseBody().items[0].key == "email.invite_edit.body"
        String id = responseBody().items[0].id

        when:
        GET("${id}?format=xml", STRING_ARG)

        then:
        verifyResponse(HttpStatus.OK, jsonCapableResponse)
        String actualShowXml = jsonCapableResponse.body().toString()
        assert compareXml(expectedShowXml, actualShowXml)
    }

    void 'Test the apply action correctly persists a collection of instances from JSON'() {
        when: 'The save action is executed with valid JSON collection data'
        log.debug('Valid content save')
        POST(getSaveCollectionPath(), getValidJsonCollection(), MAP_ARG, true)

        then: 'The response is correct and contains properties with keys functional.test.key1 and functional.test.key2'
        verifyResponse(HttpStatus.OK, response)
        response.body().items.any{it.key == "functional.test.key1"}
        response.body().items.any{it.key == "functional.test.key2"}

        and: 'The lastUpdatedBy property was ignored from the posted data'
        response.body().items.find{it.key == "functional.test.key1"}.lastUpdatedBy == "unlogged_user@mdm-core.com"
        response.body().items.find{it.key == "functional.test.key2"}.lastUpdatedBy == "unlogged_user@mdm-core.com"

        cleanup:
        String id1 = response.body().items.find{it.key == "functional.test.key1"}.id
        String id2 = response.body().items.find{it.key == "functional.test.key2"}.id
        DELETE(getDeleteEndpoint(id1))
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE(getDeleteEndpoint(id2))
        assert response.status() == HttpStatus.NO_CONTENT
    }
}
