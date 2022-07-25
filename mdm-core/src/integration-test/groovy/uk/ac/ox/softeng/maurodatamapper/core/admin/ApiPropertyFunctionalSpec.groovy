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
package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlComparer

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.junit.Assert

import java.nio.file.Files
import java.nio.file.Path

@Integration
@Slf4j
class ApiPropertyFunctionalSpec extends ResourceFunctionalSpec<ApiProperty> implements XmlComparer {

    ApiPropertyService apiPropertyService

    @RunOnce
    @Transactional
    def setup() {
        assert apiPropertyService.count() == 15
    }

    @Override
    int getExpectedInitialResourceCount() {
        15
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
        'id,key,value,category,publiclyVisible,lastUpdatedBy,createdBy,lastUpdated\r\n' +
        'e2b3398f-f3e5-4d70-8793-25526bbe0dbe,a.csv.key,a.csv.value,csvs,false,updater@example.com,creator@example.com,2021-10-27T11:02:32.682Z'
    }

    String getValidXml() {
        """\
        <?xml version='1.0'?>
        <apiProperty>
            <id>e2b3398f-f3e5-4d70-8793-25526bbe0dbe</id>
            <key>an.xml.key</key>
            <value>An XML value</value>
            <category>XML</category>
            <publiclyVisible>false</publiclyVisible>
            <lastUpdatedBy>bootstrap.user@maurodatamapper.com</lastUpdatedBy>
            <createdBy>example@maurodatamapper.com</createdBy>
            <lastUpdated>2021-10-26T13:57:57.342140+01:00</lastUpdated>
        </apiProperty>""".stripIndent()
    }

    String getSaveCollectionPath() {
        "${getSavePath()}/apply"
    }

    Map getValidJsonCollection() {
        [count: 2,
         items: [
                 [id: 'e2b3398f-f3e5-4d70-8793-25526bbe0dbe',
                  key     : 'functional.test.key.one',
                  value   : 'Some random thing1',
                  category: 'Functional Test',
                  publiclyVisible: false,
                  lastUpdatedBy: 'hello@example.com',
                  lastUpdated: '2021-10-27T11:02:32.682Z'],
                 [id: 'e2b3398f-f3e5-4d70-8793-25526bbe0dbf',
                  key     : 'functional.test.key.two',
                  value   : 'Some random thing2',
                  category: 'Functional Test',
                  publiclyVisible: false,
                  lastUpdatedBy: 'hello@example.com',
                  lastUpdated: '2021-10-27T11:02:32.682Z']
                ]
        ]
    }

    // Duplicate keys in the same file
    Map getInvalidJsonCollection() {
        [count: 2,
         items: [
             [id: 'e2b3398f-f3e5-4d70-8793-25526bbe0dbe',
              key     : 'functional.test.key.ten',
              value   : 'Some random thing1',
              category: 'Functional Test',
              publiclyVisible: false,
              lastUpdatedBy: 'hello@example.com',
              lastUpdated: '2021-10-27T11:02:32.682Z'],
             [id: 'e2b3398f-f3e5-4d70-8793-25526bbe0dbf',
              key     : 'functional.test.key.ten',
              value   : 'Some random thing2',
              category: 'Functional Test',
              publiclyVisible: false,
              lastUpdatedBy: 'hello@example.com',
              lastUpdated: '2021-10-27T11:02:32.682Z']
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
                    <key>functional.test.xml.key.one</key>
                    <value>XML value 1</value>
                    <category>XMLTests</category>
                    <publiclyVisible>false</publiclyVisible>
                    <lastUpdatedBy>bootstrap.user@maurodatamapper.com</lastUpdatedBy>
                    <createdBy>example@maurodatamapper.com</createdBy>
                    <lastUpdated>2021-10-26T13:57:57.342140+01:00</lastUpdated>
                </apiProperty>
                <apiProperty>
                    <id>e2b3398f-f3e5-4d70-8793-25526bbe0dbf</id>
                    <key>functional.test.xml.key.two</key>
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

    String getInvalidXmlCollection() {
        """\
        <?xml version='1.0'?>
        <apiProperties>
            <count>2</count>
            <items>
                <apiProperty>
                    <id>e2b3398f-f3e5-4d70-8793-25526bbe0dbe</id>
                    <key>functional.test.xml.key.ten</key>
                    <value>XML value 1</value>
                    <category>XMLTests</category>
                    <publiclyVisible>false</publiclyVisible>
                    <lastUpdatedBy>bootstrap.user@maurodatamapper.com</lastUpdatedBy>
                    <createdBy>example@maurodatamapper.com</createdBy>
                    <lastUpdated>2021-10-26T13:57:57.342140+01:00</lastUpdated>
                </apiProperty>
                <apiProperty>
                    <id>e2b3398f-f3e5-4d70-8793-25526bbe0dbf</id>
                    <key>functional.test.xml.key.ten</key>
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
        'id,key,value,category,publiclyVisible,lastUpdatedBy,createdBy,lastUpdated\r\n' +
        'e2b3398f-f3e5-4d70-8793-25526bbe0dbe,a.csv.collection.key,a.csv.collection.value,csvs,false,updater@example.com,creator@example.com,2021-10-27T11:02:32.682Z\r\n' +
        'd2b3398f-f3e5-4d70-8793-25526bbe0dbe,another.csv.collection.key,another.csv.collection.value,csvs,false,updater@example.com,creator@example.com,2021-10-27T11:02:32' +
        '.682Z'
    }

    String getInvalidCsvCollection() {
        'id,key,value,category,publiclyVisible,lastUpdatedBy,createdBy,lastUpdated\r\n' +
        'e2b3398f-f3e5-4d70-8793-25526bbe0dbe,a.csv.collection.key,a.csv.collection.value,csvs,false,updater@example.com,creator@example.com,2021-10-27T11:02:32.682Z\r\n' +
        'd2b3398f-f3e5-4d70-8793-25526bbe0dbe,a.csv.collection.key,another.csv.collection.value,csvs,false,updater@example.com,creator@example.com,2021-10-27T11:02:32.682Z'
    }


    void verifyR1EmptyIndexResponse() {
        verifyResponse(HttpStatus.OK, response)
        assert responseBody().count == 15

        ApiPropertyEnum.values()
            .findAll {!(it in [ApiPropertyEnum.SITE_URL,
                               ApiPropertyEnum.EMAIL_FROM_ADDRESS,
                               ApiPropertyEnum.SECURITY_RESTRICT_CLASSIFIER_CREATE,
                               ApiPropertyEnum.SECURITY_RESTRICT_ROOT_FOLDER])}
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
        String[] lines = csv.split('\r\n')
        assert lines.size() == 16 //header + 15 rows
        assert lines[0] == 'id,key,value,category,publiclyVisible,lastUpdatedBy,createdBy,lastUpdated'
        String id = lines[1].split(",")[0]

        when:
        GET("${id}?format=csv", STRING_ARG)

        then:
        verifyResponse(HttpStatus.OK, jsonCapableResponse)
        String csv2 = jsonCapableResponse.body().toString()
        String[] lines2 = csv2.split('\r\n')
        assert lines2.size() == 2 //header + 1 rows
        assert lines2[0] == 'id,key,value,category,publiclyVisible,lastUpdatedBy,createdBy,lastUpdated'
    }

    void 'check index endpoint for XML'(){
        given:
        Path expectedIndexPath = xmlResourcesPath.resolve('apiProperties.xml')
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
        Path expectedShowPath = xmlResourcesPath.resolve('apiProperty.xml')
        if (!Files.exists(expectedShowPath)) {
            Assert.fail("Expected export file ${expectedShowPath} does not exist")
        }
        String expectedShowXml = Files.readString(expectedShowPath)

        // This is just to retrieve a valid ID for the GET of XML
        when:
        GET('')

        then:
        responseBody().count == 15
        responseBody().items[0].key == 'email.invite_edit.body'
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
        response.body().items.findAll {it.key == 'functional.test.key.one'}.size() == 1
        response.body().items.findAll {it.key == 'functional.test.key.two'}.size() == 1

        and: 'The lastUpdatedBy property was ignored from the posted data'
        response.body().items.find {it.key == 'functional.test.key.one'}.lastUpdatedBy == 'unlogged_user@mdm-core.com'
        response.body().items.find {it.key == 'functional.test.key.two'}.lastUpdatedBy == 'unlogged_user@mdm-core.com'

        when: 'Replay the POST'
        POST(getSaveCollectionPath(), getValidJsonCollection(), MAP_ARG, true)

        then: 'The response in unprocessable'
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)

        when: 'Get the properties'
        GET("")

        then: 'There are not duplicates of functional.test.key.one and functional.test.key.two'
        verifyResponse(HttpStatus.OK, response)
        response.body().items.findAll {it.key == 'functional.test.key.one'}.size() == 1
        response.body().items.findAll {it.key == 'functional.test.key.two'}.size() == 1

        cleanup:
        String id1 = response.body().items.find {it.key == 'functional.test.key.one'}.id
        String id2 = response.body().items.find {it.key == 'functional.test.key.two'}.id
        DELETE(getDeleteEndpoint(id1))
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE(getDeleteEndpoint(id2))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'Test the apply action correctly persists a collection of instances from XML'() {
        when: 'The save action is executed with valid XML collection data'
        log.debug('Valid content save')
        POST(getSaveCollectionPath(), getValidXmlCollection(), MAP_ARG, true, 'application/xml')

        then: 'The response is correct and contains properties with keys functional.test.xml.key.1 and functional.test.xml.key.2'
        verifyResponse(HttpStatus.OK, response)
        response.body().items.findAll {it.key == 'functional.test.xml.key.one'}.size() == 1
        response.body().items.findAll {it.key == 'functional.test.xml.key.two'}.size() == 1

        and: 'The lastUpdatedBy property was ignored from the posted data'
        response.body().items.find {it.key == 'functional.test.xml.key.one'}.lastUpdatedBy == 'unlogged_user@mdm-core.com'
        response.body().items.find {it.key == 'functional.test.xml.key.two'}.lastUpdatedBy == 'unlogged_user@mdm-core.com'

        when: 'Replay the POST'
        POST(getSaveCollectionPath(), getValidXmlCollection(), MAP_ARG, true, 'application/xml')

        then: 'The response in unprocessable'
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)

        when: 'Get the properties'
        GET("")

        then: 'There are not duplicates of functional.test.xml.key.one and functional.test.xml.key.two'
        verifyResponse(HttpStatus.OK, response)
        response.body().items.findAll {it.key == 'functional.test.xml.key.one'}.size() == 1
        response.body().items.findAll {it.key == 'functional.test.xml.key.two'}.size() == 1

        cleanup:
        String id1 = response.body().items.find {it.key == 'functional.test.xml.key.one'}.id
        String id2 = response.body().items.find {it.key == 'functional.test.xml.key.two'}.id
        DELETE(getDeleteEndpoint(id1))
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE(getDeleteEndpoint(id2))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'Test the apply action correctly persists a collection of instances from CSV'() {
        when: 'The save action is executed with valid CSV collection data'
        log.debug('Valid content save')
        POST(getSaveCollectionPath(), getValidCsvCollection(), MAP_ARG, true, 'text/csv')

        then: 'The response is correct and contains properties with keys a.csv.collection.key and another.csv.collection.key'
        verifyResponse(HttpStatus.OK, response)
        response.body().items.findAll {it.key == 'a.csv.collection.key'}.size() == 1
        response.body().items.findAll {it.key == 'another.csv.collection.key'}.size() == 1

        and: 'The lastUpdatedBy property was ignored from the posted data'
        response.body().items.find {it.key == 'a.csv.collection.key'}.lastUpdatedBy == 'unlogged_user@mdm-core.com'
        response.body().items.find {it.key == 'another.csv.collection.key'}.lastUpdatedBy == 'unlogged_user@mdm-core.com'

        when: 'Replay the POST'
        POST(getSaveCollectionPath(), getValidCsvCollection(), MAP_ARG, true, 'text/csv')

        then: 'The response in unprocessable'
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)

        when: 'Get the properties'
        GET("")

        then: 'There are not duplicates in the response'
        verifyResponse(HttpStatus.OK, response)
        response.body().items.findAll {it.key == 'a.csv.collection.key'}.size() == 1
        response.body().items.findAll {it.key == 'another.csv.collection.key'}.size() == 1

        cleanup:
        String id1 = response.body().items.find {it.key == 'a.csv.collection.key'}.id
        String id2 = response.body().items.find {it.key == 'another.csv.collection.key'}.id
        DELETE(getDeleteEndpoint(id1))
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE(getDeleteEndpoint(id2))
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'Test the apply action does not persist a collection containing properties with duplicate keys'() {
        given: 'Count the existing properties'
        GET("")
        verifyResponse HttpStatus.OK, response
        def propertyCount = response.body().count

        when: 'The save action is executed with invalid JSON collection data'
        POST(getSaveCollectionPath(), getInvalidJsonCollection(), MAP_ARG, true)

        then: 'The response in unprocessable'
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)

        when: 'The save action is executed with invalid XML collection data'
        POST(getSaveCollectionPath(), getInvalidXmlCollection(), MAP_ARG, true, 'application/xml')

        then: 'The response in unprocessable'
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)

        when: 'The save action is executed with invalid CSV collection data'
        POST(getSaveCollectionPath(), getInvalidCsvCollection(), MAP_ARG, true, 'text/csv')

        then: 'The response in unprocessable'
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)

        when: 'Count the properties'
        GET("")
        verifyResponse HttpStatus.OK, response

        then: 'The count has not changed'
        response.body().count == propertyCount
    }
}
