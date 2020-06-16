/*
 * Copyright 2020 University of Oxford
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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

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
class DataClassFunctionalSpec extends ResourceFunctionalSpec<DataClass> {

    @Shared
    UUID dataModelId

    @Shared
    UUID otherDataModelId

    @Shared
    UUID dataTypeId

    @Shared
    Folder folder

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        assert Folder.count() == 0
        assert DataModel.count() == 0
        folder = new Folder(label: 'Functional Test Folder', createdBy: 'functionalTest@test.com').save(flush: true)
        DataModel dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: 'functionalTest@test.com',
                                            folder: folder).save(flush: true)
        dataModelId = dataModel.id
        otherDataModelId = new DataModel(label: 'Functional Test DataModel 2', createdBy: 'functionalTest@test.com',
                                         folder: folder).save(flush: true).id

        dataTypeId = new PrimitiveType(label: 'string', createdBy: 'functionalTest@test.com',
                                       dataModel: dataModel).save(flush: true).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataClassFunctionalSpec')
        cleanUpResources(DataType, DataModel, Folder)
    }

    @Override
    void cleanUpData() {
        if (otherDataModelId) {
            GET(getResourcePath(otherDataModelId), MAP_ARG, true)
            def items = response.body().items
            items.each {i ->
                DELETE("${getResourcePath(otherDataModelId)}/$i.id", MAP_ARG, true)
                assert response.status() == HttpStatus.NO_CONTENT
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
        response.body().description == 'Adding a description to the DataClass'
    }

    void 'Test getting all content of an empty DataClass'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id

        when:
        GET("$id/content")

        then:
        response.status() == OK
        response.body().count == 0
        response.body().items.size() == 0

    }

    void 'Test getting all content of a DataClass'() {
        given: 'setup some dataclasses and dataelements'
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id
        POST("$id/dataClasses", validJson)
        verifyResponse CREATED, response
        String childId = response.body().id
        POST("$id/dataElements", [
            label          : 'Functional Test DataElement',
            maxMultiplicity: 2,
            minMultiplicity: 0,
            dataType       : dataTypeId.toString()
        ])
        verifyResponse CREATED, response
        String elementId = response.body().id

        expect:
        id
        childId
        elementId

        when:
        GET("${id}/content")

        then:
        response.status() == OK
        response.body().count == 2
        response.body().items.size() == 2

        and:
        response.body().items[0].id == childId
        response.body().items[0].label == validJson.label
        response.body().items[0].breadcrumbs.size() == 2

        and:
        response.body().items[1].id == elementId
        response.body().items[1].label == 'Functional Test DataElement'
        response.body().items[1].maxMultiplicity == 2
        response.body().items[1].minMultiplicity == 0
        response.body().items[1].dataType.id == dataTypeId.toString()
        response.body().items[1].breadcrumbs.size() == 2
    }

    void 'Test getting all DataClasses of the test DataModel'() {
        given: 'setup some dataclasses'
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id
        POST("$id/dataClasses", validJson)
        verifyResponse CREATED, response
        String childId = response.body().id

        expect:
        id
        childId

        when:
        GET("dataModels/${dataModelId}/allDataClasses", MAP_ARG, true)

        then:
        response.status() == OK
        response.body().count == 2
        response.body().items.size() == 2

        and:
        response.body().items.any {it.id == id}
        response.body().items.any {it.id == childId}

        and:
        response.body().items.find {it.id == id}.breadcrumbs.size() == 1
        response.body().items.find {it.id == childId}.breadcrumbs.size() == 2
    }

    void 'test copying from datamodel root to other datamodel root'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id

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
        response.body().id != id
        response.body().label == validJson.label
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherDataModelId.toString()
        response.body().breadcrumbs.size() == 1
        response.body().breadcrumbs[0].id == otherDataModelId.toString()
    }

    void 'test copying from datamodel to dataclass'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id
        POST("${getResourcePath(otherDataModelId)}", validJson, MAP_ARG, true)
        verifyResponse CREATED, response
        String otherId = response.body().id

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
        response.body().id != id
        response.body().label == validJson.label
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherDataModelId.toString()
        response.body().breadcrumbs.size() == 2
        response.body().breadcrumbs[0].id == otherDataModelId.toString()
        response.body().breadcrumbs[1].id == otherId
    }

    @Rollback
    void 'test searching for metadata "mdk1" in content dataclass'() {
        given:
        POST('dataModels/import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
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
        def importedId = response.body().items[0].id
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
        POST('dataModels/import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
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
        def importedId = response.body().items[0].id
        def term = 'mdk1'
        def id = DataClass.findByLabel('emptyclass').id

        expect:
        id

        when:
        GET("${getResourcePath(importedId)}/${id}/search?search=${term}", MAP_ARG, true)

        then:
        verifyResponse OK, response
        response.body().count == 0
        response.body().items.size() == 0

        cleanup:
        sessionFactory.currentSession.flush()
        DELETE("dataModels/${importedId}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }
}