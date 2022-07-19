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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

/**
 * @see ReferenceDataValueController
 | GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues                            | Action: index
 | POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues                            | Action: save
 | GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${referenceDataValueId}    | Action: show
 | PUT    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${referenceDataValueId}    | Action: update
 | DELETE | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${referenceDataValueId}    | Action: delete
 */
@Integration
@Slf4j
class ReferenceDataValueFunctionalSpec extends ResourceFunctionalSpec<ReferenceDataValue> {

    @Shared
    UUID referenceDataModelId

    @Shared
    UUID referenceDataElementId

    @Shared
    Folder folder

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')

        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)

        ReferenceDataModel referenceDataModel = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel', createdBy: FUNCTIONAL_TEST, folder: folder,
                                                                       authority: testAuthority).save(flush: true)
        ReferenceDataType referenceDataType = new ReferencePrimitiveType(label: 'Functional Test ReferenceDataType', createdBy: FUNCTIONAL_TEST,
                                                                         referenceDataModel: referenceDataModel).save(flush: true)

        referenceDataModelId = referenceDataModel.id
        referenceDataElementId = new ReferenceDataElement(label: 'Functional Test ReferenceDataElement', createdBy: FUNCTIONAL_TEST, referenceDataModel: referenceDataModel,
                                                          referenceDataType: referenceDataType, minMultiplicity: 0, maxMultiplicity: 1).save(flush: true).id

        assert referenceDataModelId
        assert referenceDataElementId

        sessionFactory.currentSession.flush()
    }

    @Transactional
    void cleanupSpec() {
        log.debug('CleanupSpec ReferenceDataValueFunctionalSpec')
        cleanUpResources(ReferenceDataModel, ReferenceDataElement, ReferenceDataType, Folder)
    }

    @Override
    String getResourcePath() {
        "referenceDataModels/$referenceDataModelId/referenceDataValues"
    }

    @Override
    Map getValidJson() {
        [
            rowNumber           : 1,
            value               : 'Functional Test ReferenceDataValue',
            referenceDataElement: referenceDataElementId,
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            rowNumber           : -1,
            value               : null,
            referenceDataElement: null,
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
    "id": "${json-unit.matches:id}",
    "rowNumber": 1,
    "value": "Functional Test ReferenceDataValue",
    "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Functional Test ReferenceDataElement",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
            {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test ReferenceDataModel",
                "domainType": "ReferenceDataModel",
                "finalised": false
            }
        ],
        "columnNumber": 0,
        "referenceDataType": {
            "id": "${json-unit.matches:id}",
            "domainType": "ReferencePrimitiveType",
            "label": "Functional Test ReferenceDataType",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
                {
                    "id": "${json-unit.matches:id}",
                    "label": "Functional Test ReferenceDataModel",
                    "domainType": "ReferenceDataModel",
                    "finalised": false
                }
            ]
        },
        "minMultiplicity": 0,
        "maxMultiplicity": 1
    }
}'''
    }

    void 'R3A: Test the row and column number constraint'() {
        Map value1 = [
            rowNumber           : 1,
            value               : 'First Value',
            referenceDataElement: referenceDataElementId,
        ]

        Map value2 = [
            rowNumber           : 1,
            value               : 'Second Value',
            referenceDataElement: referenceDataElementId,
        ]

        Map value3 = [
            rowNumber           : 2,
            value               : 'Second Value',
            referenceDataElement: referenceDataElementId,
        ]

        when: 'The save action is executed with valid data'
        POST(getSavePath(), value1, MAP_ARG, true)

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        response.body().id

        when: 'List is called'
        String id = response.body().id
        GET('')

        then: 'We now list one value'
        verifyR3IndexResponse(id)

        when: 'The save action is executed with the same row number and data element'
        POST(getSavePath(), value2, MAP_ARG, true)

        then: 'The response is unprocessable'
        response.status == HttpStatus.UNPROCESSABLE_ENTITY

        when: 'List is called'
        GET('')

        then: 'We still the same one reference data value'
        verifyR3IndexResponse(id)

        when: 'The save action is executed with the same row number and data element'
        POST(getSavePath(), value3, MAP_ARG, true)

        then: 'The response is CREATED'
        response.status == HttpStatus.CREATED
        String id3 = response.body().id

        cleanup:
        DELETE(getDeleteEndpoint(id))
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE(getDeleteEndpoint(id3))
        assert response.status() == HttpStatus.NO_CONTENT
    }
}
