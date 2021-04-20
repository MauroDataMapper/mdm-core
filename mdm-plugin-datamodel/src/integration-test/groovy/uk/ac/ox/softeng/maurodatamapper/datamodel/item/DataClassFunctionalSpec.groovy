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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.OrderedResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import java.time.OffsetDateTime

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @see DataClassController* Controller: dataClass
 *  | POST   | /api/dataModels/${dataModelId}/dataClasses       | Action: save   |
 *  | GET    | /api/dataModels/${dataModelId}/dataClasses       | Action: index  |
 *  | DELETE | /api/dataModels/${dataModelId}/dataClasses/${id} | Action: delete |
 *  | PUT    | /api/dataModels/${dataModelId}/dataClasses/${id} | Action: update |
 *  | GET    | /api/dataModels/${dataModelId}/dataClasses/${id} | Action: show   |
 *
 *  | POST   | /api/dataModels/${dataModelId}/dataClasses/${otherDataModelId}/${otherDataClassId} | Action: copyDataClass |
 *  | POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${otherDataModelId}/${otherDataClassId} | Action: copyDataClass |
 *
 *  | GET    | /api/dataModels/${dataModelId}/allDataClasses                               | Action: index   |
 *  | GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/content           | Action: content |
 *
 *  | GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search | Action: search |
 *  | POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search | Action: search |
 */
@Integration
@Slf4j
class DataClassFunctionalSpec extends OrderedResourceFunctionalSpec<DataClass> {

    @Shared
    UUID dataModelId

    @Shared
    UUID otherDataModelId

    @Shared
    UUID finalisedDataModelId

    @Shared
    UUID dataTypeId

    @Shared
    Folder folder

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: FUNCTIONAL_TEST)
        checkAndSave(testAuthority)

        DataModel dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        dataModelId = dataModel.id
        otherDataModelId = new DataModel(label: 'Functional Test DataModel 2', createdBy: FUNCTIONAL_TEST,
                                         folder: folder, authority: testAuthority).save(flush: true).id

        finalisedDataModelId = new DataModel(label: 'Functional Test DataModel 3', createdBy: FUNCTIONAL_TEST,
                                             finalised: true, dateFinalised: OffsetDateTime.now(), modelVersion: Version.from('1'),
                                             folder: folder, authority: testAuthority).save(flush: true).id

        dataTypeId = new PrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                       dataModel: dataModel).save(flush: true).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataClassFunctionalSpec')
        cleanUpResources(DataType, DataModel, Folder)
        Authority.findByLabel('Test Authority').delete(flush: true)
    }

    @Override
    void cleanUpData() {
        if (otherDataModelId) {
            sleep(20)
            GET(getResourcePath(otherDataModelId), MAP_ARG, true)
            def items = responseBody().items
            items.each {i ->
                DELETE("${getResourcePath(otherDataModelId)}/$i.id", MAP_ARG, true)
                assert response.status() == HttpStatus.NO_CONTENT
                sleep(20)
            }
            super.cleanUpData()
        }
    }

    @Override
    String getResourcePath() {
        getResourcePath(dataModelId)
    }

    String getResourcePath(def dataModelId) {
        "dataModels/${dataModelId}/dataClasses"
    }

    @Override
    Map getValidJson() {
        [
            label: 'A new DataClass'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label: null
        ]
    }


    @Override
    Map getValidUpdateJson() {
        [
            description: 'Adding a description to the DataClass'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataClass",
  "availableActions": ["delete","show","update"],
  "model": "${json-unit.matches:id}",
  "id": "${json-unit.matches:id}",
  "label": "A new DataClass",
  "breadcrumbs": [
    {
      "domainType": "DataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataModel"
    }
  ]
}'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        responseBody().description == 'Adding a description to the DataClass'
    }

    void 'Test getting all content of an empty DataClass'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id

        when:
        GET("$id/content")

        then:
        response.status() == OK
        responseBody().count == 0
        responseBody().items.size() == 0

    }

    void 'Test getting all content of a DataClass'() {
        given: 'setup some dataclasses and dataelements'
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id
        POST("$id/dataClasses", validJson)
        verifyResponse CREATED, response
        String childId = responseBody().id
        POST("$id/dataElements", [
            label          : 'Functional Test DataElement',
            maxMultiplicity: 2,
            minMultiplicity: 0,
            dataType       : dataTypeId.toString()
        ])
        verifyResponse CREATED, response
        String elementId = responseBody().id

        expect:
        id
        childId
        elementId

        when:
        GET("${id}/content")

        then:
        response.status() == OK
        responseBody().count == 2
        responseBody().items.size() == 2

        and:
        responseBody().items[0].id == childId
        responseBody().items[0].label == validJson.label
        responseBody().items[0].breadcrumbs.size() == 2

        and:
        responseBody().items[1].id == elementId
        responseBody().items[1].label == 'Functional Test DataElement'
        responseBody().items[1].maxMultiplicity == 2
        responseBody().items[1].minMultiplicity == 0
        responseBody().items[1].dataType.id == dataTypeId.toString()
        responseBody().items[1].breadcrumbs.size() == 2
    }

    void 'Test getting all DataClasses of the test DataModel'() {
        given: 'setup some dataclasses'
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id
        POST("$id/dataClasses", validJson)
        verifyResponse CREATED, response
        String childId = responseBody().id

        expect:
        id
        childId

        when:
        GET("dataModels/${dataModelId}/allDataClasses", MAP_ARG, true)

        then:
        response.status() == OK
        responseBody().count == 2
        responseBody().items.size() == 2

        and:
        responseBody().items.any {it.id == id}
        responseBody().items.any {it.id == childId}

        and:
        responseBody().items.find {it.id == id}.breadcrumbs.size() == 1
        responseBody().items.find {it.id == childId}.breadcrumbs.size() == 2
    }

    void 'test copying from datamodel root to other datamodel root'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id

        expect:
        id

        when: 'trying to copy non-existent'
        POST("${getResourcePath(otherDataModelId)}/$dataModelId/${UUID.randomUUID()}", [:], MAP_ARG, true)

        then:
        response.status() == NOT_FOUND

        when: 'trying to copy valid'
        POST("${getResourcePath(otherDataModelId)}/$dataModelId/$id", [:], MAP_ARG, true)

        then:
        response.status() == CREATED
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().availableActions == ['delete', 'show', 'update']
        responseBody().model == otherDataModelId.toString()
        responseBody().breadcrumbs.size() == 1
        responseBody().breadcrumbs[0].id == otherDataModelId.toString()
    }

    void 'test copying from datamodel to dataclass'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id
        POST("${getResourcePath(otherDataModelId)}", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String otherId = responseBody().id

        expect:
        id
        otherId

        when: 'trying to copy non-existent'
        POST("${getResourcePath(otherDataModelId)}/$otherId/dataClasses/$dataModelId/${UUID.randomUUID()}", [:], MAP_ARG, true)

        then:
        response.status() == NOT_FOUND

        when: 'trying to copy valid'
        POST("${getResourcePath(otherDataModelId)}/$otherId/dataClasses/$dataModelId/$id", [:], MAP_ARG, true)

        then:
        response.status() == CREATED
        responseBody().id != id
        responseBody().label == validJson.label
        responseBody().availableActions == ['delete', 'show', 'update']
        responseBody().model == otherDataModelId.toString()
        responseBody().breadcrumbs.size() == 2
        responseBody().breadcrumbs[0].id == otherDataModelId.toString()
        responseBody().breadcrumbs[1].id == otherId
    }

    @Rollback
    void 'test searching for metadata "mdk1" in content dataclass'() {
        given:
        POST('dataModels/import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folder.id.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ], MAP_ARG, true)

        verifyResponse CREATED, response
        def importedId = responseBody().items[0].id
        def term = 'mdk1'
        def id = DataClass.findByLabel('content').id

        when: 'not logged in'
        GET("${getResourcePath(importedId)}/${id}/search?search=${term}", STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''{
      "count": 1,
      "items": [
        {
          "domainType": "DataElement",
          "id": "${json-unit.matches:id}",
          "model": "${json-unit.matches:id}",
          "label": "ele1",
          "breadcrumbs": [
            {
              "domainType": "DataModel",
              "finalised": false,
              "id": "${json-unit.matches:id}",
              "label": "Complex Test DataModel"
            },
            {
              "domainType": "DataClass",
              "id": "${json-unit.matches:id}",
              "label": "content"
            }
          ]
        }
      ]
    }'''

        cleanup:
        sessionFactory.currentSession.flush()
        DELETE("dataModels/${importedId}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT

    }

    @Rollback
    void 'test searching for metadata "mdk1" in empty dataclass'() {
        given:
        POST('dataModels/import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folder.id.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexDataModel').toList()
            ]
        ], MAP_ARG, true)

        verifyResponse CREATED, response
        def importedId = responseBody().items[0].id
        def term = 'mdk1'
        def id = DataClass.findByLabel('emptyclass').id

        expect:
        id

        when:
        GET("${getResourcePath(importedId)}/${id}/search?search=${term}", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.size() == 0

        cleanup:
        sessionFactory.currentSession.flush()
        DELETE("dataModels/${importedId}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'OR5: Test ordering when there are parent and child data classes, and that the children of a child are ordered separately to children'() {
        /**
         dataModel
         -> dataClass "emptyclass" @ 0
         -> dataClass "parent" @ 1
         -> dataClass "content" @ 2
         */
        given: 'Three resources with indices 0, 1 and 2'
        String aId = createNewItem(getValidLabelJson('emptyclass', 0))
        String bId = createNewItem(getValidLabelJson('parent', 1))
        String cId = createNewItem(getValidLabelJson('content', 2))

        /**
         dataModel
         -> dataClass "emptyclass" @ 0
         -> dataClass "parent" @ 1
         -> dataClass "child1" @ 0
         -> dataClass "child2" @ 1
         -> dataClass "child3" @ 2
         -> dataClass "content" @ 2
         */
        when: 'Three children are added to parent and are then listed'
        POST("${bId}/dataClasses", [label: "child1", index: 0])
        String child1Id = responseBody().id
        POST("${bId}/dataClasses", [label: "child2", index: 1])
        String child2Id = responseBody().id
        POST("${bId}/dataClasses", [label: "child3", index: 2])
        String child3Id = responseBody().id
        GET("${bId}/dataClasses")

        then: 'All children of parent are listed correctly'
        responseBody().items[0].id == child1Id
        responseBody().items[0].parentDataClass == bId
        responseBody().items[1].id == child2Id
        responseBody().items[1].parentDataClass == bId
        responseBody().items[2].id == child3Id
        responseBody().items[2].parentDataClass == bId


        when: 'All items are listed'
        GET('')

        then: 'They are in the order emptyclass, parent, content'
        responseBody().items[0].label == 'emptyclass'
        responseBody().items[1].label == 'parent'
        responseBody().items[2].label == 'content'

        /**
         dataModel
         -> dataClass "parent" @ 0
         -> dataClass "child1" @ 0
         -> dataClass "child2" @ 1
         -> dataClass "child3" @ 2
         -> dataClass "content" @ 1
         -> dataClass "emptyclass" @ 2
         */
        when: 'emptyclass is PUT at the bottom of the list'
        PUT(aId, getValidLabelJson('emptyclass', 2))

        then: 'The item is updated'
        response.status == HttpStatus.OK

        when: 'All items are listed'
        GET('')

        then: 'They are in the order parent, content, emptyclass'
        log.debug(responseBody().toString())
        responseBody().items[0].label == 'parent'
        responseBody().items[1].label == 'content'
        responseBody().items[2].label == 'emptyclass'


        when: 'All children of parent are listed'
        GET("${bId}/dataClasses")

        then: 'All children of parent are listed correctly'
        responseBody().items[0].id == child1Id
        responseBody().items[0].parentDataClass == bId
        responseBody().items[1].id == child2Id
        responseBody().items[1].parentDataClass == bId
        responseBody().items[2].id == child3Id
        responseBody().items[2].parentDataClass == bId
    }

    void 'Test extending a DataClass'() {
        given:
        // Get a DC which we will a DC as an extension
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id

        // Get a DC which inside the same DM we can extend
        POST('', [
            label: 'Another DataClass'
        ])
        verifyResponse CREATED, response
        String internalExtendableId = responseBody().id


        // Get a DC which we will not be able to extend
        POST(getResourcePath(otherDataModelId), validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String nonExtendableId = responseBody().id

        // Get a DC which we will extend
        POST(getResourcePath(finalisedDataModelId), validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String externalExtendableId = responseBody().id

        expect:
        id
        internalExtendableId
        nonExtendableId
        externalExtendableId

        when: 'trying to extend non-existent'
        PUT("${id}/extends/$otherDataModelId/${UUID.randomUUID()}", [:])

        then:
        response.status() == NOT_FOUND

        when: 'trying to extend existing DC but not finalised model'
        PUT("${id}/extends/$otherDataModelId/$nonExtendableId", [:])

        then:
        response.status() == UNPROCESSABLE_ENTITY
        responseBody().errors.first().message == "DataClass [${nonExtendableId}] to be extended does not belong to a finalised DataModel"

        when: 'trying to extend existing DC in finalised model'
        PUT("${id}/extends/$finalisedDataModelId/$externalExtendableId", [:])

        then:
        response.status() == OK
        responseBody().extendsDataClasses.size() == 1
        responseBody().extendsDataClasses.first().id == externalExtendableId
        responseBody().extendsDataClasses.first().model == finalisedDataModelId.toString()

        when: 'trying to extend existing DC in same model'
        PUT("${id}/extends/$dataModelId/$internalExtendableId", [:])

        then:
        response.status() == OK
        responseBody().extendsDataClasses.size() == 2
        responseBody().extendsDataClasses.any {it.id == externalExtendableId && it.model == finalisedDataModelId.toString()}
        responseBody().extendsDataClasses.any {it.id == internalExtendableId && it.model == dataModelId.toString()}

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE(getDeleteEndpoint(internalExtendableId))
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("dataModels/$otherDataModelId/dataClasses/$nonExtendableId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("dataModels/$finalisedDataModelId/dataClasses/$externalExtendableId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }
}