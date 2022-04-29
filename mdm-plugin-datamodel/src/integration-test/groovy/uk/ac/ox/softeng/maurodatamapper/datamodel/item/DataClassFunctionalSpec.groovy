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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.OrderedResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Retry
import spock.lang.Shared

import java.time.OffsetDateTime

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
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
    UUID finalisedDataTypeId

    @Shared
    UUID otherDataTypeId

    @Shared
    Folder folder

    @Shared
    UUID versionedFolderId

    @Shared
    UUID otherVersionedFolderId

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)

        DataModel dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        dataModelId = dataModel.id
        DataModel otherDataModel = new DataModel(label: 'Functional Test DataModel 2', createdBy: FUNCTIONAL_TEST,
                                                 folder: folder, authority: testAuthority).save(flush: true)
        otherDataModelId = otherDataModel.id

        DataModel finalisedDataModel = new DataModel(label: 'Functional Test DataModel 3', createdBy: FUNCTIONAL_TEST,
                                                     finalised: true, dateFinalised: OffsetDateTime.now(), modelVersion: Version.from('1'),
                                                     folder: folder, authority: testAuthority).save(flush: true)
        finalisedDataModelId = finalisedDataModel.id

        dataTypeId = new PrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                       dataModel: dataModel).save(flush: true).id
        finalisedDataTypeId = new PrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                                dataModel: finalisedDataModel).save(flush: true).id
        otherDataTypeId = new PrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                            dataModel: otherDataModel).save(flush: true).id

        versionedFolderId = new VersionedFolder(label: 'Functional Test VersionedFolder', createdBy: FUNCTIONAL_TEST, authority: testAuthority).save(flush: true).id
        assert versionedFolderId
        otherVersionedFolderId = new VersionedFolder(label: 'Functional Test VersionedFolder 2', createdBy: FUNCTIONAL_TEST, authority: testAuthority).save(flush: true).id
        assert otherVersionedFolderId

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataClassFunctionalSpec')
        cleanUpResources(DataType, DataModel, Folder, Classifier)
    }

    @Override
    void cleanUpData() {
        if (otherDataModelId) {
            sleep(20)
            GET(getResourcePath(otherDataModelId), MAP_ARG, true)
            def items = responseBody().items
            items.each { i ->
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

    Map getValidCopyLabel() {
        [
            copyLabel: 'Renamed Copy Label'
        ]
    }

    Map getInvalidCopyLabel() {
        [
            copyLabel: null
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

    String createNewItem(String savePath, Map model) {
        POST(savePath, model, MAP_ARG, true)
        verifyResponse(CREATED, response)
        response.body().id
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
        responseBody().items.any { it.id == id }
        responseBody().items.any { it.id == childId }

        and:
        responseBody().items.find { it.id == id }.breadcrumbs.size() == 1
        responseBody().items.find { it.id == childId }.breadcrumbs.size() == 2
    }

    void 'CC01 : test copying from datamodel root to other datamodel root'() {
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

    void 'CC02 : test copying from datamodel to dataclass'() {
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
    void 'CC03 : test copying to a datamodel with a data element and datatype'() {
        // There is an error around hibernate search in this scenario
        // The metadata and classifiers of the datatype arent initialised and cause a "could not initialize proxy - no Session" exception
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id
        POST("$id/dataElements", [
            label   : 'Functional Test DataElement',
            dataType: dataTypeId
        ])
        verifyResponse CREATED, response

        POST("$id/dataElements", [
            label   : 'Functional Test DataElement 2',
            dataType: [label: 'wibble', domainType: 'PrimitiveType']
        ])
        verifyResponse CREATED, response

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
        String copyId = responseBody().id

        when:
        GET("${getResourcePath(otherDataModelId)}/$copyId/dataElements", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any { it.label == 'Functional Test DataElement' }
        responseBody().items.any { it.label == 'Functional Test DataElement 2' }

        when:
        GET("dataModels/$otherDataModelId/dataTypes", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any { it.label == 'wibble' }
        responseBody().items.any { it.label == 'string' }
    }

    @Rollback
    void 'CC04 : Test copying a dataClass with a user assigned label, aka: a save as function'() {

        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id
        verifyResponse CREATED, response

        when: 'trying to copy and rename valid'
        POST("${getResourcePath(otherDataModelId)}/$dataModelId/$id", getValidCopyLabel(), MAP_ARG, true)

        then:
        response.status() == CREATED
        responseBody().id != id
        responseBody().label == 'Renamed Copy Label'

        when: 'trying to copy and rename with null label'
        POST("${getResourcePath(otherDataModelId)}/$dataModelId/$id", getInvalidCopyLabel(), MAP_ARG, true)

        then:
        response.status() == CREATED
        responseBody().id != id
        responseBody().label == validJson.label

        when: 'trying to copy and rename non-existent object'
        POST("${getResourcePath(otherDataModelId)}/$dataModelId/${UUID.randomUUID()}", getValidCopyLabel(), MAP_ARG, true)

        then:
        response.status() == NOT_FOUND
    }

    @Rollback
    void 'CC05 : Test copying a dataModel with a data element and datatype renaming the dataClass'() {

        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id
        //create element
        POST("$id/dataElements", [
            label   : 'Functional Test DataElement 3',
            dataType: dataTypeId
        ])
        verifyResponse CREATED, response
        //create element 2
        POST("$id/dataElements", [
            label   : 'Functional Test DataElement 4',
            dataType: [label: 'wobble', domainType: 'PrimitiveType']
        ])
        verifyResponse CREATED, response

        when: 'trying to copy valid'
        POST("${getResourcePath(otherDataModelId)}/$dataModelId/$id", getValidCopyLabel(), MAP_ARG, true)

        then:
        response.status() == CREATED
        responseBody().id != id
        responseBody().label == 'Renamed Copy Label'
        responseBody().availableActions == ['delete', 'show', 'update']
        responseBody().model == otherDataModelId.toString()
        responseBody().breadcrumbs.size() == 1
        responseBody().breadcrumbs[0].id == otherDataModelId.toString()
        String copyId = responseBody().id

        when:
        GET("${getResourcePath(otherDataModelId)}/$copyId/dataElements", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().items.any { it.label == 'Functional Test DataElement 3' }
        responseBody().items.any { it.label == 'Functional Test DataElement 4' }

        when:
        GET("dataModels/$otherDataModelId/dataTypes", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().items.any { it.label == 'wobble' }
        responseBody().items.any { it.label == 'string' }
    }

    void 'CC06 : test copying a dataclass with ordered dataclasses, dataelements and enumerationvalues'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id

        for (int i in 1..5) {
            POST("$id/dataClasses", [label: 'Child Data Class ' + i, idx: i])
            verifyResponse CREATED, response
        }

        Map enumerationTypeData = [
            label            : 'Functional Test Enumeration Type',
            domainType       : 'EnumerationType',
            enumerationValues: (1..5).collect {i ->
                [
                    key  : 'Key ' + i,
                    value: 'Value ' + i,
                    index: i - 1
                ]
            }
        ]
        POST("dataModels/$dataModelId/dataTypes", enumerationTypeData, MAP_ARG, true)
        verifyResponse CREATED, response
        String enumerationTypeId = responseBody().id

        for (int i in 1..5) {
            POST("$id/dataElements",
                 [label: 'Data Element ' + i, dataType: enumerationTypeId, idx: i - 1])
            verifyResponse CREATED, response
        }

        when:
        GET("$id/dataClasses?sort=idx")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 5
        responseBody().items.eachWithIndex {dc, i ->
            assert dc.domainType == 'DataClass'
            assert dc.label.startsWith('Child Data Class') && dc.label.endsWith((i + 1).toString())
        }

        when:
        GET("$id/dataElements")

        then:
        verifyResponse OK, response
        responseBody().items.eachWithIndex {de, i ->
            assert de.domainType == 'DataElement'
            assert de.label.startsWith('Data Element') && de.label.endsWith((i + 1).toString())
        }

        when:
        GET("dataModels/$dataModelId/dataTypes/$enumerationTypeId", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().domainType == 'EnumerationType'
        responseBody().enumerationValues.every {ev ->
            ev.key.startsWith('Key') && ev.key.endsWith((ev.index + 1).toString()) &&
            ev.value.startsWith('Value') && ev.value.endsWith((ev.index + 1).toString())
        }

        when:
        POST("${getResourcePath(otherDataModelId)}/$dataModelId/$id", [:], MAP_ARG, true)

        then:
        verifyResponse CREATED, response
        String copyId = responseBody().id

        when:
        GET("${getResourcePath(otherDataModelId)}/$copyId/dataClasses?sort=idx", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.size() == 5
        responseBody().items.eachWithIndex {dc, i ->
            assert dc.domainType == 'DataClass'
            assert dc.label.startsWith('Child Data Class') && dc.label.endsWith((i + 1).toString())
        }

        when:
        GET("${getResourcePath(otherDataModelId)}/$copyId/dataElements", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().items.eachWithIndex {de, i ->
            assert de.domainType == 'DataElement'
            assert de.label.startsWith('Data Element') && de.label.endsWith((i + 1).toString())
        }
        String copyEnumerationTypeId = responseBody().items[0].dataType.id

        when:
        GET("dataModels/$otherDataModelId/dataTypes/$copyEnumerationTypeId", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().domainType == 'EnumerationType'
        responseBody().enumerationValues.every {ev ->
            ev.key.startsWith('Key') && ev.key.endsWith((ev.index + 1).toString()) &&
            ev.value.startsWith('Value') && ev.value.endsWith((ev.index + 1).toString())
        }

        cleanup:
        cleanUpData()
        DELETE("dataModels/$dataModelId/dataTypes/$enumerationTypeId", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$otherDataModelId/dataTypes/$copyEnumerationTypeId", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    void 'CC07 : test copying a dataclass without index order'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = responseBody().id

        for (int i in 1..5) {
            POST("$id/dataClasses", [label: 'Child Data Class ' + i, idx: i])
            verifyResponse CREATED, response
        }

        when:
        GET("$id/dataClasses?sort=idx")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 5
        responseBody().items.eachWithIndex {dc, i ->
            assert dc.domainType == 'DataClass'
            assert dc.label.startsWith('Child Data Class') && dc.label.endsWith((i + 1).toString())
        }

        when:
        String originalId = responseBody().items[0].id
        POST("$id/dataClasses/$dataModelId/$originalId", [copyLabel: 'Child Copied Class'])

        then:
        verifyResponse CREATED, response
        responseBody().label == 'Child Copied Class'

        when:
        GET("$id/dataClasses?sort=idx")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 6
        responseBody().items.take(5).eachWithIndex {dc, i ->
            assert dc.domainType == 'DataClass'
            assert dc.label.startsWith('Child Data Class') && dc.label.endsWith((i + 1).toString())
        }

        and:
        responseBody().items[5].domainType == 'DataClass'
        responseBody().items[5].label == 'Child Copied Class'

        cleanup:
        cleanUpData()
    }

    @Transactional
    UUID getDataClassId(String label) {
        DataClass.findByLabel(label).id
    }

    @Retry
    void 'test searching for metadata "mdk1" in content dataclass'() {
        given:
        POST('dataModels/import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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
        def id = getDataClassId('content')

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
        DELETE("dataModels/${importedId}?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'test searching for metadata "mdk1" in empty dataclass'() {
        given:
        POST('dataModels/import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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
        def id = getDataClassId('emptyclass')

        expect:
        id

        when:
        GET("${getResourcePath(importedId)}/${id}/search?search=${term}", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.size() == 0

        cleanup:
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
        POST("${bId}/dataClasses", [label: 'child1', index: 0])
        String child1Id = responseBody().id
        POST("${bId}/dataClasses", [label: 'child2', index: 1])
        String child2Id = responseBody().id
        POST("${bId}/dataClasses", [label: 'child3', index: 2])
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
        GET('?sort=idx')

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
        verifyResponse OK, response

        when: 'All items are listed'
        GET('?sort=idx')

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
        verifyResponse NOT_FOUND, response

        when: 'trying to extend existing DC but not finalised model'
        PUT("${id}/extends/$otherDataModelId/$nonExtendableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataClass [${nonExtendableId}] to be extended does not belong to a finalised DataModel"

        when: 'trying to extend existing DC in finalised model'
        PUT("${id}/extends/$finalisedDataModelId/$externalExtendableId", [:])

        then:
        verifyResponse OK, response
        responseBody().extendsDataClasses.size() == 1
        responseBody().extendsDataClasses.first().id == externalExtendableId
        responseBody().extendsDataClasses.first().model == finalisedDataModelId.toString()

        when: 'trying to extend existing DC in same model'
        PUT("${id}/extends/$dataModelId/$internalExtendableId", [:])

        then:
        verifyResponse OK, response
        responseBody().extendsDataClasses.size() == 2
        responseBody().extendsDataClasses.any { it.id == externalExtendableId && it.model == finalisedDataModelId.toString() }
        responseBody().extendsDataClasses.any { it.id == internalExtendableId && it.model == dataModelId.toString() }

        cleanup:
        cleanUpData(id)
        cleanUpData(internalExtendableId)
        DELETE("dataModels/$otherDataModelId/dataClasses/$nonExtendableId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("dataModels/$finalisedDataModelId/dataClasses/$externalExtendableId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'IMI01 : test importing DataElement'() {
        given:
        // Get DataClass
        String id = createNewItem(validJson)
        String sameModelDataClassId = createNewItem([
            label: 'Functional Test DataClass 2'
        ])

        // Get an other DataModel DataClass
        String otherId = createNewItem(getResourcePath(otherDataModelId), [
            label: 'Functional Test DataClass 3'
        ])

        // Get finalised DataModel DataClass
        String finalisedId = createNewItem(getResourcePath(finalisedDataModelId), [
            label: 'Functional Test DataClass 4'
        ])

        // Get internal DE
        POST("$id/dataElements", [
            label   : 'Functional Test DataElement',
            dataType: dataTypeId])
        verifyResponse CREATED, response
        String internalId = responseBody().id

        POST("$sameModelDataClassId/dataElements", [
            label   : 'Functional Test DataElement 2',
            dataType: dataTypeId])
        verifyResponse CREATED, response
        String sameModelImportableId = responseBody().id

        POST("${getResourcePath(finalisedDataModelId)}/$finalisedId/dataElements", [
            label   : 'Functional Test DataElement 3',
            dataType: finalisedDataTypeId], MAP_ARG, true)
        verifyResponse CREATED, response
        String importableId = responseBody().id

        POST("${getResourcePath(finalisedDataModelId)}/$finalisedId/dataElements", [
            label   : 'Functional Test DataElement',
            dataType: finalisedDataTypeId], MAP_ARG, true)
        verifyResponse CREATED, response
        String sameLabelId = responseBody().id

        POST("${getResourcePath(otherDataModelId)}/$otherId/dataElements", [
            label   : 'Functional Test DataElement 4',
            dataType: otherDataTypeId], MAP_ARG, true)
        verifyResponse CREATED, response
        String nonImportableId = responseBody().id

        when: 'importing non-existent'
        PUT("$id/dataElements/$finalisedDataModelId/$finalisedId/${nonImportableId}", [:])

        then:
        verifyResponse NOT_FOUND, response

        when: 'importing non importable id'
        PUT("$id/dataElements/$otherDataModelId/$otherId/$nonImportableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "DataElement [${nonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing internal id'
        PUT("$id/dataElements/$dataModelId/$id/$internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataElement [${internalId}] to be imported belongs to the DataClass already"

        when: 'importing with same label id'
        PUT("$id/dataElements/$finalisedDataModelId/$finalisedId/$sameLabelId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "Property [importedDataElements] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel." +
        "item.DataClass] has non-unique values [Functional Test DataElement] on property [label]"

        when: 'importing same model importableId id'
        PUT("$id/dataElements/$dataModelId/$sameModelDataClassId/$sameModelImportableId", [:])

        then:
        verifyResponse OK, response

        when: 'importing importable id'
        PUT("$id/dataElements/$finalisedDataModelId/$finalisedId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataelements'
        log.info 'getting list of dataelements'
        GET("$id/dataElements")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 3
        responseBody().items.any { it.id == internalId && !it.imported }
        responseBody().items.any { it.id == sameModelImportableId && it.imported }
        responseBody().items.any { it.id == importableId && it.imported }

        cleanup:
        cleanUpData(id)
        cleanUpData(sameModelDataClassId)
        DELETE("dataModels/$otherDataModelId/dataClasses/$otherId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("dataModels/$finalisedDataModelId/dataClasses/$finalisedId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'IMI02 : test importing DataElement and removing'() {
        given:
        // Get DataClass
        String id = createNewItem(validJson)

        // Get finalised DataModel DataClass
        String finalisedId = createNewItem(getResourcePath(finalisedDataModelId), [
            label: 'Functional Test DataClass 3'
        ])

        // Get internal DE
        POST("$id/dataElements", [
            label   : 'Functional Test DataElement',
            dataType: dataTypeId])
        verifyResponse CREATED, response
        String internalId = responseBody().id

        POST("${getResourcePath(finalisedDataModelId)}/$finalisedId/dataElements", [
            label   : 'Functional Test DataElement 2',
            dataType: finalisedDataTypeId], MAP_ARG, true)
        verifyResponse CREATED, response
        String importableId = responseBody().id

        when: 'importing importable id'
        PUT("$id/dataElements/$finalisedDataModelId/$finalisedId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'removing non-existent'
        DELETE("$id/dataElements/$finalisedDataModelId/$finalisedId/${UUID.randomUUID()}", [:])

        then:
        verifyResponse NOT_FOUND, response

        when: 'removing internal id'
        DELETE("$id/dataElements/$dataModelId/$id/$internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataElement [${internalId}] belongs to the DataClass and cannot be removed as an import"

        when: 'removing importable id'
        DELETE("$id/dataElements/$finalisedDataModelId/$finalisedId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of datatypes'
        GET("$id/dataElements")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 1
        responseBody().items.any { it.id == internalId }

        cleanup:
        cleanUpData(id)
        DELETE("dataModels/$finalisedDataModelId/dataClasses/$finalisedId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'IMI03 : test importing DataClasses'() {
        given:
        // Get DataClass
        String id = createNewItem(validJson)

        String sameModelImportableId = createNewItem([
            label: 'Functional Test DataClass 4'])

        POST("$id/dataClasses", [
            label: 'Functional Test DataClass'])
        verifyResponse CREATED, response
        String internalId = responseBody().id

        POST("${getResourcePath(finalisedDataModelId)}", [
            label: 'Functional Test DataClass 2'], MAP_ARG, true)
        verifyResponse CREATED, response
        String importableId = responseBody().id

        POST("${getResourcePath(finalisedDataModelId)}", [
            label: 'Functional Test DataClass',], MAP_ARG, true)
        verifyResponse CREATED, response
        String sameLabelId = responseBody().id

        POST("${getResourcePath(otherDataModelId)}", [
            label: 'Functional Test DataClass 3'], MAP_ARG, true)
        verifyResponse CREATED, response
        String nonImportableId = responseBody().id

        when: 'importing non-existent'
        PUT("$id/dataClasses/$finalisedDataModelId/${nonImportableId}", [:])

        then:
        verifyResponse NOT_FOUND, response

        when: 'importing non importable id'
        PUT("$id/dataClasses/$otherDataModelId/$nonImportableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "DataClass [${nonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing internal id'
        PUT("$id/dataClasses/$dataModelId/$internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataClass [${internalId}] to be imported belongs to the DataClass already"

        when: 'importing with same label id'
        PUT("$id/dataClasses/$finalisedDataModelId/$sameLabelId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "Property [importedDataClasses] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel." +
        "item.DataClass] has non-unique values [Functional Test DataClass] on property [label]"

        when: 'importing dc into itself id'
        PUT("$id/dataClasses/$dataModelId/$id", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataClass [${id}] cannot be imported into itself"

        when: 'importing same model importableId id'
        PUT("$id/dataClasses/$dataModelId/$sameModelImportableId", [:])

        then:
        verifyResponse OK, response

        when: 'importing importable id'
        PUT("$id/dataClasses/$finalisedDataModelId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataclasses'
        log.info 'getting list of dataclasses'
        GET("$id/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 3
        responseBody().items.any { it.id == internalId && !it.imported }
        responseBody().items.any { it.id == importableId && it.imported }
        responseBody().items.any { it.id == sameModelImportableId && it.imported }

        cleanup:
        cleanUpData(id)
        cleanUpData(sameModelImportableId)
        DELETE("dataModels/$otherDataModelId/dataClasses/$nonImportableId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("dataModels/$finalisedDataModelId/dataClasses/$sameLabelId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("dataModels/$finalisedDataModelId/dataClasses/$importableId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'IMI04 : test importing DataClass and removing'() {
        given:
        // Get DataClass
        String id = createNewItem(validJson)

        POST("$id/dataClasses", [
            label: 'Functional Test DataClass'])
        verifyResponse CREATED, response
        String internalId = responseBody().id

        POST("${getResourcePath(finalisedDataModelId)}", [
            label: 'Functional Test DataClass 2'], MAP_ARG, true)
        verifyResponse CREATED, response
        String importableId = responseBody().id

        when: 'importing importable id'
        PUT("$id/dataClasses/$finalisedDataModelId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'removing non-existent'
        DELETE("$id/dataClasses/$finalisedDataModelId/${UUID.randomUUID()}", [:])

        then:
        verifyResponse NOT_FOUND, response

        when: 'removing internal id'
        DELETE("$id/dataClasses/$dataModelId/$internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataClass [${internalId}] belongs to the DataClass and cannot be removed as an import"

        when: 'removing importable id'
        DELETE("$id/dataClasses/$finalisedDataModelId/$importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataclasses'
        GET("$id/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 1
        responseBody().items.any {it.id == internalId}

        cleanup:
        cleanUpData(id)
        DELETE("dataModels/$finalisedDataModelId/dataClasses/$importableId", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void 'IMI05 : test importing DataClasses inside same VF'() {
        given:
        // Get DataModel inside VF
        POST("folders/${versionedFolderId}/dataModels", [
            label: 'Functional Test Model 1'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String sameVfId = responseBody().id

        // Get second DataModel inside same VF
        POST("folders/${versionedFolderId}/dataModels", [
            label: 'Functional Test Model 2'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String sameVfId2 = responseBody().id

        POST("folders/${otherVersionedFolderId}/dataModels", [
            label: 'Functional Test Model 3'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String otherVfId = responseBody().id


        POST("${getResourcePath(sameVfId)}", [
            label: 'Functional Test DataClass',], MAP_ARG, true)
        verifyResponse CREATED, response
        String sameVfIdDcId = responseBody().id

        POST("${getResourcePath(sameVfId2)}", [
            label: 'Functional Test DataClass 2',], MAP_ARG, true)
        verifyResponse CREATED, response
        String vfImportableId = responseBody().id

        POST("${getResourcePath(finalisedDataModelId)}", [
            label: 'Functional Test DataClass 3'], MAP_ARG, true)
        verifyResponse CREATED, response
        String finalisedImportableId = responseBody().id

        POST("${getResourcePath(otherDataModelId)}", [
            label: 'Functional Test DataClass 3'], MAP_ARG, true)
        verifyResponse CREATED, response
        String nonImportableId = responseBody().id

        POST("${getResourcePath(otherVfId)}", [
            label: 'Functional Test DataClass 5',], MAP_ARG, true)
        verifyResponse CREATED, response
        String otherVfNonImportableId = responseBody().id


        when: 'importing non finalised different DM DC'
        PUT("${getResourcePath(sameVfId)}/${sameVfIdDcId}/dataClasses/$otherDataModelId/$nonImportableId", [:], MAP_ARG, true)

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "DataClass [${nonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing non finalised different DM different VF DC'
        PUT("${getResourcePath(sameVfId)}/${sameVfIdDcId}/dataClasses/$otherVfId/$otherVfNonImportableId", [:], MAP_ARG, true)

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "DataClass [${otherVfNonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing finalised different DM  DC'
        PUT("${getResourcePath(sameVfId)}/${sameVfIdDcId}/dataClasses/$finalisedDataModelId/$finalisedImportableId", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        when: 'importing non finalised different DM same VF DC'
        PUT("${getResourcePath(sameVfId)}/${sameVfIdDcId}/dataClasses/$sameVfId2/$vfImportableId", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

    }
}
