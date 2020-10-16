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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.BAD_REQUEST
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND

/**
 * @see ReferenceDataTypeController* Controller: dataType
 *  | POST   | /api/referenceDataModels/${referenceDataModelId}/dataTypes       | Action: save   |
 *  | GET    | /api/referenceDataModels/${referenceDataModelId}/dataTypes       | Action: index  |
 *  | DELETE | /api/referenceDataModels/${referenceDataModelId}/dataTypes/${id} | Action: delete |
 *  | PUT    | /api/referenceDataModels/${referenceDataModelId}/dataTypes/${id} | Action: update |
 *  | GET    | /api/referenceDataModels/${referenceDataModelId}/dataTypes/${id} | Action: show   |
 *
 *  | POST   | /api/referenceDataModels/${referenceDataModelId}/dataTypes/${otherReferenceDataModelId}/${dataTypeId} | Action: copyDataType |
 */
@Integration
@Slf4j
class ReferenceDataTypeFunctionalSpec extends ResourceFunctionalSpec<ReferenceDataType> {

    @Shared
    UUID referenceDataModelId

    @Shared
    UUID otherReferenceDataModelId

    @Shared
    Folder folder

    @Shared
    UUID dataClassId

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: FUNCTIONAL_TEST)
        checkAndSave(testAuthority)
        ReferenceDataModel referenceDataModel = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        referenceDataModelId = referenceDataModel.id
        otherReferenceDataModelId = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel 2', createdBy: FUNCTIONAL_TEST,
                                         folder: folder, authority: testAuthority).save(flush: true).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataTypeFunctionalSpec')
        cleanUpResources(ReferenceDataModel, Folder)
        Authority.findByLabel('Test Authority').delete(flush: true)
    }

    @Override
    void cleanUpData() {
        if (referenceDataModelId) {
            GET("referenceDataModels/$otherReferenceDataModelId/dataTypes", MAP_ARG, true)
            def items = response.body().items
            items.each {i ->
                DELETE("referenceDataModels/$otherReferenceDataModelId/dataTypes/$i.id", MAP_ARG, true)
                assert response.status() == HttpStatus.NO_CONTENT
            }
            super.cleanUpData()
        }
    }

    @Override
    String getResourcePath() {
        "referenceDataModels/${referenceDataModelId}/dataTypes"
    }

    @Override
    Map getValidJson() {
        [
            domainType: 'PrimitiveType',
            label     : 'date'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label     : null,
            domainType: 'PrimitiveType'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'describes a date only'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "PrimitiveType",
  "availableActions": ["delete","show","update"],
  "model": "${json-unit.matches:id}",
  "id": "${json-unit.matches:id}",
  "label": "date",
  "breadcrumbs": [
    {
      "domainType": "ReferenceDataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Functional Test ReferenceDataModel"
    }
  ]
}'''
    }


    void "Test the save action correctly persists an instance for enumeration type"() {
        when: "The save action is executed with valid data"
        POST('',
             [
                 domainType       : 'EnumerationType',
                 label            : 'functional enumeration',
                 enumerationValues: [
                     [key: 'a', value: 'wibble'],
                     [key: 'b', value: 'wobble']
                 ]
             ], STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse CREATED, '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "EnumerationType",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "enumerationValues": [
        {
          "index": 0,
          "id": "${json-unit.matches:id}",
          "category": null,
          "value": "wibble",
          "key": "a"
        },
        {
          "index": 1,
          "id": "${json-unit.matches:id}",
          "category": null,
          "value": "wobble",
          "key": "b"
        }
      ],
      "id": "${json-unit.matches:id}",
      "label": "functional enumeration",
      "breadcrumbs": [
        {
          "domainType": "ReferenceDataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Functional Test ReferenceDataModel"
        }
      ]
    }'''
    }

    void "Test the save action correctly persists an instance for reference type"() {

        when: "The save action is executed with valid data"
        POST('',
             [
                 domainType    : 'ReferenceType',
                 label         : 'functional dataclass reference',
                 referenceClass: dataClassId
             ]
             , STRING_ARG)

        then: "The response is correct"
        verifyJsonResponse CREATED, '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "domainType": "ReferenceType",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "functional dataclass reference",
      "breadcrumbs": [
        {
          "domainType": "ReferenceDataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Functional Test ReferenceDataModel"
        }
      ],
      "referenceClass": {
        "domainType": "DataClass",
        "model": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataClass",
        "breadcrumbs": [
          {
            "domainType": "ReferenceDataModel",
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Functional Test ReferenceDataModel"
          }
        ]
      }
    }'''
    }

    void 'test copying primitive type from datamodel to other datamodel'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id

        expect:
        id

        when: 'trying to copy non-existent'
        POST("referenceDataModels/$otherReferenceDataModelId/dataTypes/$referenceDataModelId/${UUID.randomUUID()}", [:], MAP_ARG, true)

        then:
        response.status == NOT_FOUND

        when: 'trying to copy valid'
        POST("referenceDataModels/$otherReferenceDataModelId/dataTypes/$referenceDataModelId/$id", [:], MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        response.body().id != id
        response.body().label == validJson.label
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherReferenceDataModelId.toString()
        response.body().breadcrumbs.size() == 1
        response.body().breadcrumbs[0].id == otherReferenceDataModelId.toString()

        cleanup:
        cleanUpData(id)
    }

    void 'test copying enumeration type from datamodel to other datamodel'() {
        given:
        POST('', [
            domainType       : 'EnumerationType',
            label            : 'functional enumeration',
            enumerationValues: [
                [key: 'a', value: 'wibble'],
                [key: 'b', value: 'wobble']
            ]
        ])
        verifyResponse CREATED, response
        String id = response.body().id

        expect:
        id

        when: 'trying to copy valid'
        POST("referenceDataModels/$otherReferenceDataModelId/dataTypes/$referenceDataModelId/$id", [:], MAP_ARG, true)

        then:
        verifyResponse(CREATED, response)
        response.body().id != id
        response.body().label == 'functional enumeration'
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().model == otherReferenceDataModelId.toString()
        response.body().breadcrumbs.size() == 1
        response.body().breadcrumbs[0].id == otherReferenceDataModelId.toString()

        cleanup:
        cleanUpData(id)
    }

    void 'test copying reference type from datamodel to other datamodel'() {
        given:
        POST('', [
            domainType    : 'ReferenceType',
            label         : 'functional dataclass reference',
            referenceClass: dataClassId
        ])
        verifyResponse CREATED, response
        String id = response.body().id

        expect:
        id

        when: 'trying to copy valid'
        POST("referenceDataModels/$otherReferenceDataModelId/dataTypes/$referenceDataModelId/$id", [:], MAP_ARG, true)

        then:
        verifyResponse(BAD_REQUEST, response)

        cleanup:
        cleanUpData(id)
    }
}