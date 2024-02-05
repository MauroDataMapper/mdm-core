/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndCopyingInDataModelsFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Unroll

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * <pre>
 * Controller: dataClass
 *  |  POST    | /api/dataModels/${dataModelId}/dataClasses        | Action: save
 *  |  GET     | /api/dataModels/${dataModelId}/dataClasses        | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: update
 *  |  GET     | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: show
 *
 *  |  POST    | /api/dataModels/${dataModelId}/dataClasses/${otherDataModelId}/${otherDataClassId}  | Action: copyDataClass
 *
 *  |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search  | Action: search
 *  |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search  | Action: search
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassController
 */
@Integration
@Slf4j
class DataClassFunctionalSpec extends UserAccessAndCopyingInDataModelsFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getComplexDataModelId()}/dataClasses"
    }

    @Override
    String getEditsPath() {
        'dataClasses'
    }

    @Transactional
    @Override
    String getExpectedTargetId() {
        getDataClassIdByLabel 'parent'
    }

    @Transactional
    String getDataClassIdByLabel(String label) {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(complexDataModelId), label).get().id.toString()
    }

    @Transactional
    String getSimpleDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(simpleDataModelId), 'simple').get().id.toString()
    }

    @Transactional
    String getFinalisedDataModelId() {
        DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id.toString()
    }

    @Transactional
    String getFinalisedDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(getFinalisedDataModelId()), 'Finalised Data Class').get().id.toString()
    }

    @Transactional
    String getFinalisedDataClass2Id() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(getFinalisedDataModelId()), 'Another Data Class').get().id.toString()
    }

    @Transactional
    String getFinalisedDataElementId() {
        DataElement.byDataClassIdAndLabel(Utils.toUuid(getFinalisedDataClassId()), 'Finalised Data Element').get().id.toString()
    }

    @Transactional
    String getFinalisedDataElementId2() {
        DataElement.byDataClassIdAndLabel(Utils.toUuid(getFinalisedDataClassId()), 'Another DataElement').get().id.toString()
    }

    @Transactional
    String getFinalisedDataTypeId() {
        PrimitiveType.byDataModelIdAndLabel(Utils.toUuid(getFinalisedDataModelId()), 'Finalised Data Type').get().id.toString()
    }

    @Transactional
    String getComplexStringDataTypeId() {
        PrimitiveType.byDataModelIdAndLabel(Utils.toUuid(getComplexDataModelId()), 'string').get().id.toString()
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
            label: 'parent'
        ]
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataClass",
  "model": "${json-unit.matches:id}",
  "id": "${json-unit.matches:id}",
  "label": "A new DataClass",
  "path": "dm:Complex Test DataModel$main|dc:A new DataClass",
  "breadcrumbs": [
    {
      "domainType": "DataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Complex Test DataModel"
    }
  ],
  "availableActions": [
    "show"
  ]
}'''
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 3,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "emptyclass",
      "path": "dm:Complex Test DataModel$main|dc:emptyclass",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "description": "dataclass with desc"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "parent",
      "path": "dm:Complex Test DataModel$main|dc:parent",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "maxMultiplicity": -1,
      "minMultiplicity": 1
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "content",
      "path": "dm:Complex Test DataModel$main|dc:content",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "description": "A dataclass with elements",
      "maxMultiplicity": 1,
      "minMultiplicity": 0
    }
  ]
}'''
    }

    @Override
    void verifyCopiedResponseBody(HttpResponse<Map> response) {
        Map body = response.body()

        assert body.id
        assert body.domainType == 'DataClass'
        assert body.label == 'parent'
        assert body.model == getSimpleDataModelId()
        assert body.breadcrumbs
        assert body.breadcrumbs.size() == 1
        assert body.breadcrumbs.first().id == getSimpleDataModelId()
        assert body.breadcrumbs.first().label == 'Simple Test DataModel'
        assert body.breadcrumbs.first().domainType == 'DataModel'
        assert body.breadcrumbs.first().finalised == false

        assert body.availableActions == getEditorModelItemAvailableActions().sort()
        assert body.lastUpdated
        assert body.maxMultiplicity == -1
        assert body.minMultiplicity == 1
    }

    void 'S01 : test searching for metadata "mdk1" in empty dataclass'() {
        given:
        def term = 'mdk1'
        def id = getDataClassIdByLabel('emptyclass')

        when: 'not logged in'
        GET("$id/search?searchTerm=$term")

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("$id/search?searchTerm=$term")

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'S02 : test searching for metadata "mdk1" in content dataclass'() {
        given:
        def term = 'mdk1'
        def id = getDataClassIdByLabel('content')

        when: 'not logged in'
        GET("$id/search?searchTerm=$term")

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("$id/search?searchTerm=$term")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'DataElement'
        responseBody().items.first().label == 'ele1'
        responseBody().items.first().breadcrumbs.size() == 2
    }

    void 'S03 : test searching for label "ele*" in content dataclass'() {
        given:
        def term = 'ele*'
        def id = getDataClassIdByLabel('content')

        when: 'not logged in'
        GET("$id/search?searchTerm=$term")

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("$id/search?searchTerm=$term&sort=label")

        then:
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items[0].domainType == 'DataElement'
        responseBody().items[0].label == 'ele1'
        responseBody().items[0].breadcrumbs.size() == 2
        responseBody().items[1].domainType == 'DataElement'
        responseBody().items[1].label == 'element2'
        responseBody().items[1].breadcrumbs.size() == 2
    }

    void 'S04 : test searching for label "ele*" in content dataclass using post'() {
        given:
        def term = 'ele*'
        def id = getDataClassIdByLabel('content')

        when: 'not logged in'
        POST("$id/search", [searchTerm: term])

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        POST("$id/search", [searchTerm: term, sort: 'label'])

        then:
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items[0].domainType == 'DataElement'
        responseBody().items[0].label == 'ele1'
        responseBody().items[0].breadcrumbs.size() == 2
        responseBody().items[1].domainType == 'DataElement'
        responseBody().items[1].label == 'element2'
        responseBody().items[1].breadcrumbs.size() == 2
    }

    void 'R06 : Test extending a DataClass (as reader)'() {
        given:
        // Get a DC which we will a DC as an extension
        String id = getValidId()

        // Get a DC which inside the same DM we can extend
        loginEditor()
        POST('', [
            label: 'Another DataClass'
        ])
        verifyResponse CREATED, response
        String internalExtendableId = responseBody().id
        logout()

        // Get a DC which we will not be able to extend
        String nonExtendableId = getSimpleDataClassId()

        // Get a DC which we will extend
        String externalExtendableId = getFinalisedDataClassId()

        expect:
        id
        internalExtendableId
        nonExtendableId
        externalExtendableId

        when: 'trying to extend non-existent'
        loginReader()
        PUT("${id}/extends/$simpleDataModelId/${UUID.randomUUID()}", [:])

        then:
        response.status() == FORBIDDEN

        when: 'trying to extend existing DC but not finalised model'
        PUT("${id}/extends/$simpleDataModelId/$nonExtendableId", [:])

        then:
        response.status() == FORBIDDEN

        when: 'trying to extend existing DC in finalised model'
        PUT("${id}/extends/$finalisedDataModelId/$externalExtendableId", [:])

        then:
        response.status() == FORBIDDEN

        when: 'trying to extend existing DC in same model'
        PUT("${id}/extends/$complexDataModelId/$internalExtendableId", [:])

        then:
        response.status() == FORBIDDEN

        cleanup:
        removeValidIdObject(id)
        removeValidIdObject(internalExtendableId)
    }

    void 'E06 : Test extending a DataClass (as editor)'() {
        given:
        // Get a DC which we will a DC as an extension
        String id = getValidId()

        // Get a DC which inside the same DM we can extend
        loginEditor()
        POST('', [
            label: 'Another DataClass'
        ])
        verifyResponse CREATED, response
        String internalExtendableId = responseBody().id
        logout()

        // Get a DC which we will not be able to extend
        String nonExtendableId = getSimpleDataClassId()

        // Get a DC which we will extend
        String externalExtendableId = getFinalisedDataClassId()

        expect:
        id
        internalExtendableId
        nonExtendableId
        externalExtendableId

        when: 'trying to extend non-existent'
        loginEditor()
        PUT("${id}/extends/$simpleDataModelId/${UUID.randomUUID()}", [:])

        then:
        response.status() == NOT_FOUND

        when: 'trying to extend existing DC but not finalised model'
        PUT("${id}/extends/$simpleDataModelId/$nonExtendableId", [:])

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
        PUT("${id}/extends/$complexDataModelId/$internalExtendableId", [:])

        then:
        response.status() == OK
        responseBody().extendsDataClasses.size() == 2
        responseBody().extendsDataClasses.any {it.id == externalExtendableId && it.model == finalisedDataModelId.toString()}
        responseBody().extendsDataClasses.any {it.id == internalExtendableId && it.model == complexDataModelId.toString()}

        cleanup:
        removeValidIdObject(id)
        removeValidIdObject(internalExtendableId)
    }

    @Unroll
    void 'IMI01 : test importing DataElement (as #info)'() {
        given:
        Map data = configureImportDataElement()
        if (user) loginUser(userEmailAddresses[user])

        when: 'importing non-existent'
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/${data.nonImportableId}", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'importing non importable id'
        PUT("$data.id/dataElements/$simpleDataModelId/$data.otherId/$data.nonImportableId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'importing internal id'
        PUT("$data.id/dataElements/$complexDataModelId/$data.id/$data.internalId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'importing with same label id'
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.sameLabelId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'importing same model importableId id'
        PUT("$data.id/dataElements/$complexDataModelId/$data.sameModelDataClassId/$data.sameModelImportableId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'importing importable id'
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.importableId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        cleanup:
        cleanupImportData(data)

        where:
        user << [null, 'authenticated']
        info = user ?: 'not logged in'
    }

    void 'IMI02 : test importing DataElement (as reader)'() {
        given:
        Map data = configureImportDataElement()
        loginReader()

        when: 'importing non-existent'
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/${data.nonImportableId}", [:])

        then:
        verifyForbidden(response)

        when: 'importing non importable id'
        PUT("$data.id/dataElements/$simpleDataModelId/$data.otherId/$data.nonImportableId", [:])

        then:
        verifyForbidden(response)

        when: 'importing internal id'
        PUT("$data.id/dataElements/$complexDataModelId/$data.id/$data.internalId", [:])

        then:
        verifyForbidden(response)

        when: 'importing with same label id'
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.sameLabelId", [:])

        then:
        verifyForbidden(response)

        when: 'importing same model importableId id'
        PUT("$data.id/dataElements/$complexDataModelId/$data.sameModelDataClassId/$data.sameModelImportableId", [:])

        then:
        verifyForbidden(response)

        when: 'importing importable id'
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.importableId", [:])

        then:
        verifyForbidden(response)

        when: 'getting list of dataelements'
        GET("$data.id/dataElements")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 1
        responseBody().items.any {it.id == data.internalId && !it.imported}

        cleanup:
        cleanupImportData(data)
    }

    @Unroll
    void 'IMI03 : test importing DataElement (as #info)'() {
        given:
        Map data = configureImportDataElement()
        loginUser(userEmailAddresses[user])

        when: 'importing non-existent'
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/${data.nonImportableId}", [:])

        then:
        verifyResponse NOT_FOUND, response

        when: 'importing non importable id'
        PUT("$data.id/dataElements/$simpleDataModelId/$data.otherId/$data.nonImportableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "DataElement [${data.nonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing internal id'
        PUT("$data.id/dataElements/$complexDataModelId/$data.id/$data.internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataElement [${data.internalId}] to be imported belongs to the DataClass already"

        when: 'importing with same label id'
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.sameLabelId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "Property [importedDataElements] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel." +
        "item.DataClass] has non-unique values [Another DataElement] on property [label]"

        when: 'importing same model importableId id'
        PUT("$data.id/dataElements/$complexDataModelId/$data.sameModelDataClassId/$data.sameModelImportableId", [:])

        then:
        verifyResponse OK, response

        when: 'importing importable id'
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataelements'
        GET("$data.id/dataElements")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 3
        responseBody().items.any {it.id == data.internalId && !it.imported}
        responseBody().items.any {it.id == data.sameModelImportableId && it.imported}
        responseBody().items.any {it.id == data.importableId && it.imported}

        cleanup:
        cleanupImportData(data)

        where:
        user << ['admin', 'editor']
        info = user
    }

    @Unroll
    void 'IMI04 : test importing DataElement and removing (as #info)'() {
        given:
        Map data = configureImportDataElement()
        loginEditor()
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.importableId", [:])
        verifyResponse OK, response
        logout()
        if (user) loginUser(userEmailAddresses[user])
        String randomId = UUID.randomUUID().toString()

        when: 'removing non-existent'
        DELETE("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$randomId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'removing internal id'
        DELETE("$data.id/dataElements/$complexDataModelId/$data.id/$data.internalId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'removing importable id'
        DELETE("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.importableId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        cleanup:
        cleanupImportData(data)

        where:
        user << [null, 'authenticated']
        info = user ?: 'not logged in'
    }

    void 'IMI05 : test importing DataElement and removing (as reader)'() {
        given:
        Map data = configureImportDataElement()
        loginEditor()
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.importableId", [:])
        verifyResponse OK, response
        logout()
        loginReader()
        String randomId = UUID.randomUUID().toString()

        when: 'removing non-existent'
        DELETE("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$randomId", [:])

        then:
        verifyForbidden(response)

        when: 'removing internal id'
        DELETE("$data.id/dataElements/$complexDataModelId/$data.id/$data.internalId", [:])

        then:
        verifyForbidden(response)

        when: 'removing importable id'
        DELETE("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.importableId", [:])

        then:
        verifyForbidden(response)

        when: 'getting list of dataElements'
        GET("$data.id/dataElements")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 2
        responseBody().items.any {it.id == data.internalId}
        responseBody().items.any {it.id == data.importableId && it.imported}

        cleanup:
        cleanupImportData(data)

    }

    @Unroll
    void 'IMI06 : test importing DataElement and removing (as #info)'() {
        given:
        Map data = configureImportDataElement()
        loginEditor()
        PUT("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.importableId", [:])
        verifyResponse OK, response
        logout()
        loginUser(userEmailAddresses[user])
        String randomId = UUID.randomUUID().toString()

        when: 'removing non-existent'
        DELETE("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$randomId", [:])

        then:
        verifyNotFound(response, data.randomId)

        when: 'removing internal id'
        DELETE("$data.id/dataElements/$complexDataModelId/$data.id/$data.internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataElement [${data.internalId}] belongs to the DataClass and cannot be removed as an import"

        when: 'removing importable id'
        DELETE("$data.id/dataElements/$finalisedDataModelId/$data.finalisedId/$data.importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataElements'
        GET("$data.id/dataElements")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 1
        responseBody().items.any {it.id == data.internalId}

        cleanup:
        cleanupImportData(data)

        where:
        user << ['admin', 'editor']
        info = user
    }

    @Unroll
    void 'IMI07 : test importing DataClasses (as #info)'() {
        given:
        Map data = configureImportDataClass()
        if (user) loginUser(userEmailAddresses[user])

        when: 'importing non-existent'
        PUT("$data.id/dataClasses/$finalisedDataModelId/${data.nonImportableId}", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'importing non importable id'
        PUT("$data.id/dataClasses/$simpleDataModelId/$data.nonImportableId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'importing internal id'
        PUT("$data.id/dataClasses/$complexDataModelId/$data.internalId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'importing with same label id'
        PUT("$data.id/dataClasses/$finalisedDataModelId/$data.sameLabelId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'importing same model importableId id'
        PUT("$data.id/dataClasses/$complexDataModelId/$data.sameModelImportableId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'importing importable id'
        PUT("$data.id/dataClasses/$finalisedDataModelId/$data.importableId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        cleanup:
        cleanupImportData(data)

        where:
        user << [null, 'authenticated']
        info = user ?: 'not logged in'
    }

    void 'IMI08 : test importing DataClasses (as reader)'() {
        given:
        Map data = configureImportDataClass()
        loginReader()

        when: 'importing non-existent'
        PUT("$data.id/dataClasses/$finalisedDataModelId/${data.nonImportableId}", [:])

        then:
        verifyForbidden(response)

        when: 'importing non importable id'
        PUT("$data.id/dataClasses/$simpleDataModelId/$data.nonImportableId", [:])

        then:
        verifyForbidden(response)

        when: 'importing internal id'
        PUT("$data.id/dataClasses/$complexDataModelId/$data.internalId", [:])

        then:
        verifyForbidden(response)

        when: 'importing with same label id'
        PUT("$data.id/dataClasses/$finalisedDataModelId/$data.sameLabelId", [:])

        then:
        verifyForbidden(response)

        when: 'importing same model importableId id'
        PUT("$data.id/dataClasses/$complexDataModelId/$data.sameModelImportableId", [:])

        then:
        verifyForbidden(response)

        when: 'importing importable id'
        PUT("$data.id/dataClasses/$finalisedDataModelId/$data.importableId", [:])

        then:
        verifyForbidden(response)

        when: 'getting list of dataclasses'
        GET("$data.id/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 1
        responseBody().items.any {it.id == data.internalId && !it.imported}

        cleanup:
        cleanupImportData(data)
    }

    @Unroll
    void 'IMI09 : test importing DataClasses (as #info)'() {
        given:
        Map data = configureImportDataClass()
        loginUser(userEmailAddresses[user])

        when: 'importing non-existent'
        PUT("$data.id/dataClasses/$finalisedDataModelId/${data.nonImportableId}", [:])

        then:
        verifyNotFound(response, data.nonImportableId)

        when: 'importing non importable id'
        PUT("$data.id/dataClasses/$simpleDataModelId/$data.nonImportableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "DataClass [${data.nonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing internal id'
        PUT("$data.id/dataClasses/$complexDataModelId/$data.internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataClass [${data.internalId}] to be imported belongs to the DataClass already"

        when: 'importing with same label id'
        PUT("$data.id/dataClasses/$finalisedDataModelId/$data.sameLabelId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "Property [importedDataClasses] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel." +
        "item.DataClass] has non-unique values [Another Data Class] on property [label]"

        when: 'importing same model importableId id'
        PUT("$data.id/dataClasses/$complexDataModelId/$data.sameModelImportableId", [:])

        then:
        verifyResponse OK, response

        when: 'importing importable id'
        PUT("$data.id/dataClasses/$finalisedDataModelId/$data.importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataclasses'
        GET("$data.id/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 3
        responseBody().items.any {it.id == data.internalId && !it.imported}
        responseBody().items.any {it.id == data.sameModelImportableId && it.imported}
        responseBody().items.any {it.id == data.importableId && it.imported}

        cleanup:
        cleanupImportData(data)

        where:
        user << ['admin', 'editor']
        info = user
    }

    @Unroll
    void 'IMI10 : test importing DataClass and removing (as #info)'() {
        given:
        Map data = configureImportDataClass()
        loginEditor()
        PUT("$data.id/dataClasses/$finalisedDataModelId/$data.importableId", [:])
        verifyResponse OK, response
        logout()
        if (user) loginUser(userEmailAddresses[user])
        String randomId = UUID.randomUUID().toString()

        when: 'removing non-existent'
        DELETE("$data.id/dataClasses/$finalisedDataModelId/$randomId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'removing internal id'
        DELETE("$data.id/dataClasses/$data.dataModelId/$data.internalId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        when: 'removing importable id'
        DELETE("$data.id/dataClasses/$finalisedDataModelId/$data.importableId", [:])

        then:
        verifyNotFound(response, complexDataModelId)

        cleanup:
        cleanupImportData(data)

        where:
        user << [null, 'authenticated']
        info = user ?: 'not logged in'
    }

    void 'IMI11 : test importing DataClass and removing (as reader)'() {
        given:
        Map data = configureImportDataClass()
        loginEditor()
        PUT("$data.id/dataClasses/$finalisedDataModelId/$data.importableId", [:])
        verifyResponse OK, response
        logout()
        loginReader()
        String randomId = UUID.randomUUID().toString()

        when: 'removing non-existent'
        DELETE("$data.id/dataClasses/$finalisedDataModelId/$randomId", [:])

        then:
        verifyForbidden(response)

        when: 'removing internal id'
        DELETE("$data.id/dataClasses/$data.dataModelId/$data.internalId", [:])

        then:
        verifyForbidden(response)

        when: 'removing importable id'
        DELETE("$data.id/dataClasses/$finalisedDataModelId/$data.importableId", [:])

        then:
        verifyForbidden(response)

        when: 'getting list of dataclasses'
        GET("$data.id/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 2
        responseBody().items.any {it.id == data.internalId}
        responseBody().items.any {it.id == data.importableId && it.imported}

        cleanup:
        cleanupImportData(data)

        where:
        user << ['admin', 'editor']
        info = user
    }

    @Unroll
    void 'IMI12 : test importing DataClass and removing (as #info)'() {
        given:
        Map data = configureImportDataClass()
        loginEditor()
        PUT("$data.id/dataClasses/$finalisedDataModelId/$data.importableId", [:])
        verifyResponse OK, response
        logout()
        loginUser(userEmailAddresses[user])
        String randomId = UUID.randomUUID().toString()

        when: 'removing non-existent'
        DELETE("$data.id/dataClasses/$finalisedDataModelId/$randomId", [:])

        then:
        verifyNotFound(response, randomId)

        when: 'removing internal id'
        DELETE("$data.id/dataClasses/$complexDataModelId/$data.internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataClass [${data.internalId}] belongs to the DataClass and cannot be removed as an import"

        when: 'removing importable id'
        DELETE("$data.id/dataClasses/$finalisedDataModelId/$data.importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataclasses'
        GET("$data.id/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 1
        responseBody().items.any {it.id == data.internalId}


        cleanup:
        cleanupImportData(data)

        where:
        user << ['admin', 'editor']
        info = user
    }

    Map configureImportDataClass() {

        Map data = [:]

        data.id = getValidId()
        loginEditor()
        POST('', [
            label: 'Functional Test DataClass 2'
        ])
        verifyResponse CREATED, response
        data.sameModelImportableId = responseBody().id
        // Get internal DT
        POST("$data.id/dataClasses", [
            label: 'Another Data Class'])
        verifyResponse CREATED, response
        data.internalId = responseBody().id

        data.importableId = getFinalisedDataClassId()

        data.sameLabelId = getFinalisedDataClass2Id()

        data.nonImportableId = getSimpleDataClassId()

        logout()
        data
    }

    Map configureImportDataElement() {
        Map data = [:]
        // Get DataClass
        data.id = getValidId()
        loginEditor()
        POST('', [
            label: 'Functional Test DataClass 2'
        ])
        verifyResponse CREATED, response
        data.sameModelDataClassId = responseBody().id

        // Get an other DataModel DataClass
        data.otherId = getSimpleDataClassId()

        // Get finalised DataModel DataClass
        data.finalisedId = getFinalisedDataClassId()

        // Get other DataType
        POST("dataModels/$simpleDataModelId/dataTypes", [
            label     : 'Functional Test DataType',
            domainType: 'PrimitiveType'], MAP_ARG, true)
        verifyResponse CREATED, response
        data.otherDataTypeId = responseBody().id

        // Get internal DE
        POST("$data.id/dataElements", [
            label   : 'Another DataElement',
            dataType: getComplexStringDataTypeId()])
        verifyResponse CREATED, response
        data.internalId = responseBody().id

        POST("$data.sameModelDataClassId/dataElements", [
            label   : 'Functional Test DataElement 2',
            dataType: getComplexStringDataTypeId()])
        verifyResponse CREATED, response
        data.sameModelImportableId = responseBody().id

        data.importableId = getFinalisedDataElementId()

        data.sameLabelId = getFinalisedDataElementId2()

        POST("dataModels/$simpleDataModelId/dataClasses/$data.otherId/dataElements", [
            label   : 'Functional Test DataElement 4',
            dataType: data.otherDataTypeId], MAP_ARG, true)
        verifyResponse CREATED, response
        data.nonImportableId = responseBody().id
        logout()
        data
    }

    void cleanupImportData(Map data) {
        removeValidIdObject(data.id)
        if (data.otherDataTypeId) {
            removeValidIdObject(data.sameModelDataClassId)
            loginEditor()
            DELETE("dataModels/$simpleDataModelId/dataTypes/$data.otherDataTypeId", MAP_ARG, true)
            assert response.status() == HttpStatus.NO_CONTENT
            logout()
        } else {
            removeValidIdObject(data.sameModelImportableId)
        }
    }


    /*
    void 'Test getting all DataClasses of a DataModel'() {
        when: 'not logged in'
        def response = restGet("${apiPath}/dataModels/${testDataModel.id}/allDataClasses")

        then:
        verifyUnauthorised response

        when: 'logged in'
        loginEditor()
        response = restGet("${apiPath}/dataModels/${testDataModel.id}/allDataClasses")

        then:
        verifyResponse OK, response, '''{
  "count": 4,
  "items": [
    {
      "domainType": "DataClass",
      "dataModel": "${json-unit.matches:id}",
      "description": "dataclass with desc",
      "id": "${json-unit.matches:id}",
      "label": "emptyclass",
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel"
        }
      ]
    },
    {
      "domainType": "DataClass",
      "dataModel": "${json-unit.matches:id}",
      "maxMultiplicity": -1,
      "id": "${json-unit.matches:id}",
      "label": "parent",
      "minMultiplicity": 1,
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel"
        }
      ]
    },
    {
      "domainType": "DataClass",
      "dataModel": "${json-unit.matches:id}",
      "parentDataClass": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "child",
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
          "label": "parent"
        }
      ]
    },
    {
      "domainType": "DataClass",
      "semanticLinks": [
        {
          "domainType": "CatalogueSemanticLink",
          "linkType": "Does Not Refine",
          "id": "${json-unit.matches:id}",
          "source": {
            "domainType": "DataClass",
            "dataModel": "${json-unit.matches:id}",
            "id": "${json-unit.matches:id}",
            "label": "content",
            "breadcrumbs": [
              {
                "domainType": "DataModel",
                "finalised": false,
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel"
              }
            ]
          },
          "target": {
            "domainType": "DataClass",
            "dataModel": "${json-unit.matches:id}",
            "id": "${json-unit.matches:id}",
            "label": "parent",
            "breadcrumbs": [
              {
                "domainType": "DataModel",
                "finalised": false,
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel"
              }
            ]
          }
        }
      ],
      "dataModel": "${json-unit.matches:id}",
      "description": "A dataclass with elements",
      "maxMultiplicity": 1,
      "id": "${json-unit.matches:id}",
      "label": "content",
      "minMultiplicity": 0,
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel"
        }
      ]
    }
  ]
}'''
    }

    void 'Test getting all content of a DataClass'() {
        when: 'not logged in'
        def response = restGet("${DataClass.findByDataModelAndLabel(testDataModel, 'parent').id}/content")

        then:
        verifyUnauthorised response

        when: 'logged in'
        loginEditor()
        response = restGet("${DataClass.findByDataModelAndLabel(testDataModel, 'parent').id}/content")

        then:
        verifyResponse OK, response, '''{
  "count": 2,
  "items": [
    {
      "domainType": "DataClass",
      "dataModel": "${json-unit.matches:id}",
      "parentDataClass": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "child",
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
          "label": "parent"
        }
      ]
    },
    {
      "domainType": "DataElement",
      "dataClass": "${json-unit.matches:id}",
      "dataType": {
        "domainType": "ReferenceType",
        "dataModel": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "child",
        "breadcrumbs": [
          {
            "domainType": "DataModel",
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel"
          }
        ],
        "referenceClass": {
          "domainType": "DataClass",
          "dataModel": "${json-unit.matches:id}",
          "parentDataClass": "${json-unit.matches:id}",
          "id": "${json-unit.matches:id}",
          "label": "child",
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
              "label": "parent"
            }
          ]
        }
      },
      "dataModel": "${json-unit.matches:id}",
      "maxMultiplicity": 1,
      "id": "${json-unit.matches:id}",
      "label": "child",
      "minMultiplicity": 1,
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
          "label": "parent"
        }
      ]
    }
  ]
}
'''
    }


    */
}