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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

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

    //@Shared
    //UUID dataClassId

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        ReferenceDataModel referenceDataModel = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        referenceDataModelId = referenceDataModel.id
        otherReferenceDataModelId = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel 2', createdBy: FUNCTIONAL_TEST,
                                         folder: folder, authority: testAuthority).save(flush: true).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec ReferenceDataTypeFunctionalSpec')
        cleanUpResources(ReferenceDataModel, Folder)
    }

    @Override
    void cleanUpData() {
        if (referenceDataModelId) {
            GET("referenceDataModels/$otherReferenceDataModelId/referenceDataTypes", MAP_ARG, true)
            def items = response.body().items
            items.each {i ->
                DELETE("referenceDataModels/$otherReferenceDataModelId/referenceDataTypes/$i.id", MAP_ARG, true)
                assert response.status() == HttpStatus.NO_CONTENT
            }
            super.cleanUpData()
        }
    }

    @Override
    String getResourcePath() {
        "referenceDataModels/${referenceDataModelId}/referenceDataTypes"
    }

    @Override
    Map getValidJson() {
        [
            domainType: 'ReferencePrimitiveType',
            label     : 'date'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label     : null,
            domainType: 'ReferencePrimitiveType'
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
  "domainType": "ReferencePrimitiveType",
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


    void 'Test the save action correctly persists an instance for enumeration type'() {
        when: 'The save action is executed with valid data'
        POST('',
             [
                 domainType                : 'ReferenceEnumerationType',
                 label                     : 'functional enumeration',
                 referenceEnumerationValues: [
                     [key: 'a', value: 'wibble'],
                     [key: 'b', value: 'wobble']
                 ]
             ], STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse CREATED, '''{
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceEnumerationType",
      "label": "functional enumeration",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "domainType": "ReferenceDataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Functional Test ReferenceDataModel"
        }
      ],
      "availableActions": ["delete","show","update"],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
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
      ]
    }'''
    }


    void 'test copying primitive type from referencedatamodel to other referencedatamodel'() {
        given:
        POST('', validJson)
        verifyResponse CREATED, response
        String id = response.body().id

        expect:
        id

        when: 'trying to copy non-existent'
        POST("referenceDataModels/$otherReferenceDataModelId/referenceDataTypes/$referenceDataModelId/${UUID.randomUUID()}", [:], MAP_ARG, true)

        then:
        response.status == NOT_FOUND

        when: 'trying to copy valid'
        POST("referenceDataModels/$otherReferenceDataModelId/referenceDataTypes/$referenceDataModelId/$id", [:], MAP_ARG, true)

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

    void 'test copying enumeration type from referencedatamodel to other referencedatamodel'() {
        given:
        POST('', [
            domainType       : 'ReferenceEnumerationType',
            label            : 'functional enumeration',
            referenceEnumerationValues: [
                [key: 'a', value: 'wibble'],
                [key: 'b', value: 'wobble']
            ]
        ])
        verifyResponse CREATED, response
        String id = response.body().id

        expect:
        id

        when: 'trying to copy valid'
        POST("referenceDataModels/$otherReferenceDataModelId/referenceDataTypes/$referenceDataModelId/$id", [:], MAP_ARG, true)

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
}