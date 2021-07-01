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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * @see EnumerationValueController* Controller: enumerationValue
 *  | POST   | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues       | Action: save   |
 *  | GET    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues       | Action: index  |
 *  | DELETE | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id} | Action: delete |
 *  | PUT    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id} | Action: update |
 *  | GET    | /api/dataModels/${dataModelId}/enumerationTypes/${enumerationTypeId}/enumerationValues/${id} | Action: show   |
 */
@Integration
@Slf4j
class EnumerationValueFunctionalSpec extends ResourceFunctionalSpec<EnumerationValue> {

    @Shared
    UUID dataModelId

    @Shared
    Folder folder

    @Shared
    UUID dataTypeId

    @Shared
        enumerationValueId

    @Shared
    boolean onceBeforeHasRun = false

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        Authority testAuthority = Authority.findByLabel('Test Authority')
        checkAndSave(testAuthority)
        DataModel dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        dataModelId = dataModel.id

        dataTypeId = new EnumerationType(label: 'Functional Test DataType', createdBy: FUNCTIONAL_TEST,
                                         dataModel: dataModel)
            .addToEnumerationValues(key: 'A', value: 'A value', createdBy: FUNCTIONAL_TEST)
            .save(flush: true).id

        enumerationValueId = EnumerationValue.findByKey('A').id

        sessionFactory.currentSession.flush()
        onceBeforeHasRun = true
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec EnumerationValueFunctionalSpec')
        cleanUpResources(EnumerationType, DataType, DataModel, Folder)
    }

    @Override
    void cleanUpData() {
        if (dataModelId) {
            sleep(20)
            GET('')
            assert response.status() == OK
            def items = response.body().items
            items.each { i ->
                if (i.key != 'A') {
                    DELETE(getDeleteEndpoint(i.id))
                    assert response.status() == HttpStatus.NO_CONTENT
                    sleep(20)
                }
            }
        }
    }

    @Override
    String getResourcePath() {
        "dataModels/$dataModelId/enumerationTypes/$dataTypeId/enumerationValues"
    }

    @Override
    Map getValidJson() {
        [
            key  : 'O',
            value: 'Other'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            key  : null,
            value: 'Affirmative'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            value: 'Optional'
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "index": 1,
  "id": "${json-unit.matches:id}",
  "category": null, 
  "value": "Other",
  "key": "O"
}'''
    }

    @Override
    void verifyR1EmptyIndexResponse() {
        verifyResponse(OK, response)
        assert response.body().count == 1
        assert response.body().items.size() == 1
        assert response.body().items[0].key == 'A'
        assert response.body().items[0].value == 'A value'
        assert response.body().items[0].index == 0
    }

    @Override
    void verifyR3IndexResponse(String id) {
        verifyResponse(OK, response)
        assert response.body().count == 2
        assert response.body().items.size() == 2
        assert response.body().items[0].key == 'A'
        assert response.body().items[0].value == 'A value'
        assert response.body().items[0].index == 0
        assert response.body().items[1].key == validJson.key
        assert response.body().items[1].value == validJson.value
        assert response.body().items[1].index == 1
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        response.body().value == 'Optional'
    }

    @Override
    int getExpectedInitialResourceCount() {
        // It appears the system runs all the superclass oncebefore and before methods before getting to this class
        // Therefore the first time this runs there will be no resource
        onceBeforeHasRun ? 1 : 0
    }

    void 'test saving with category'() {
        when: 'The save action is executed with valid data'
        POST('', [
            key     : 'O',
            value   : 'Other',
            category: 'unknown'
        ])

        then:
        verifyResponse(CREATED, response)
        response.body().category == 'unknown'

        when:
        GET('')

        then:
        verifyResponse(OK, response)
        assert response.body().count == 2
        assert response.body().items.size() == 2
        assert response.body().items[0].key == 'A'
        assert response.body().items[0].value == 'A value'
        assert response.body().items[0].index == 0
        assert response.body().items[0].category == null
        assert response.body().items[1].key == validJson.key
        assert response.body().items[1].value == validJson.value
        assert response.body().items[1].index == 1
        assert response.body().items[1].category == 'unknown'
    }

    void 'test updating category'() {
        when: 'The save action is executed with valid data'
        POST('', [
            key     : 'O',
            value   : 'Other',
            category: 'unknown'
        ])

        then:
        verifyResponse(CREATED, response)
        response.body().category == 'unknown'

        when:

        when:
        PUT("$enumerationValueId", [
            category: 'alpha'
        ])

        then:
        verifyResponse(OK, response)
        response.body().category == 'alpha'

        when:
        GET('')

        then:
        verifyResponse(OK, response)
        assert response.body().count == 2
        assert response.body().items.size() == 2
        assert response.body().items[0].key == 'A'
        assert response.body().items[0].value == 'A value'
        assert response.body().items[0].index == 0
        assert response.body().items[0].category == 'alpha'
        assert response.body().items[1].key == validJson.key
        assert response.body().items[1].value == validJson.value
        assert response.body().items[1].index == 1
        assert response.body().items[1].category == 'unknown'
    }

    void 'test ordering on insertion without defining index value'() {
        when: 'Adding C to list [A]'
        POST('', [
            key  : 'C',
            value: 'C'
        ])

        then: 'order should be [A,C]'
        verifyResponse(CREATED, response)
        response.body().index == 1

        when: 'Adding D to list [A,C]'
        POST('', [
            key  : 'D',
            value: 'D'
        ])

        then: 'order should be [A,C,D]'
        verifyResponse(CREATED, response)
        response.body().index == 2

        when: 'Adding B to list [A,C,D]'
        POST('', [
            key  : 'B',
            value: 'B'
        ])

        then: 'order should be [A,C,D, B]'
        verifyResponse(CREATED, response)
        response.body().index == 3

        when:
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].key == 'A'
        response.body().items[0].index == 0
        response.body().items[1].key == 'C'
        response.body().items[1].index == 1
        response.body().items[2].key == 'D'
        response.body().items[2].index == 2
        response.body().items[3].key == 'B'
        response.body().items[3].index == 3
    }

    void 'test ordering on insertion when defining index value'() {
        when: 'Adding C to list [A]'
        POST('', [
            key  : 'C',
            value: 'C'
        ])

        then: 'order should be [A,C]'
        verifyResponse(CREATED, response)
        response.body().index == 1

        when: 'Adding D to list [A,C]'
        POST('', [
            key  : 'D',
            value: 'D'
        ])

        then: 'order should be [A,C,D]'
        verifyResponse(CREATED, response)
        response.body().index == 2

        when: 'Adding B to list [A,C,D]'
        POST('', [
            key  : 'B',
            value: 'B',
            index: '1'
        ])

        then: 'order should be [A,B,C,D]'
        verifyResponse(CREATED, response)
        response.body().index == 1

        when:
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].key == 'A'
        response.body().items[0].index == 0
        response.body().items[1].key == 'B'
        response.body().items[1].index == 1
        response.body().items[2].key == 'C'
        response.body().items[2].index == 2
        response.body().items[3].key == 'D'
        response.body().items[3].index == 3
    }

    void 'test ordering update'() {
        when: 'Adding C to list [A]'
        POST('', [
            key  : 'C',
            value: 'C'
        ])

        then: 'order should be [A,C]'
        verifyResponse(CREATED, response)
        response.body().index == 1

        when: 'Adding D to list [A,C]'
        POST('', [
            key  : 'D',
            value: 'D'
        ])

        then: 'order should be [A,C,D]'
        verifyResponse(CREATED, response)
        response.body().index == 2

        when: 'Adding B to list [A,C,D]'
        POST('', [
            key  : 'B',
            value: 'B'
        ])

        then: 'order should be [A,B,C,D]'
        verifyResponse(CREATED, response)
        response.body().index == 3

        when:
        String id = response.body().id
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].key == 'A'
        response.body().items[0].index == 0
        response.body().items[1].key == 'C'
        response.body().items[1].index == 1
        response.body().items[2].key == 'D'
        response.body().items[2].index == 2
        response.body().items[3].key == 'B'
        response.body().items[3].index == 3

        when:
        log.debug('Updating B to index 1')
        PUT(id, [index: 1])

        then:
        verifyResponse(OK, response)
        response.body().index == 1

        when:
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].index == 0
        response.body().items[1].index == 1
        response.body().items[2].index == 2
        response.body().items[3].index == 3
        response.body().items[0].key == 'A'
        response.body().items[1].key == 'B'
        response.body().items[2].key == 'C'
        response.body().items[3].key == 'D'

        when:
        log.debug('Updating B to index 0')
        PUT(id, [index: 0])

        then:
        verifyResponse(OK, response)
        response.body().index == 0

        when:
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].index == 0
        response.body().items[1].index == 1
        response.body().items[2].index == 2
        response.body().items[3].index == 3
        response.body().items[0].key == 'B'
        response.body().items[1].key == 'A'
        response.body().items[2].key == 'C'
        response.body().items[3].key == 'D'

        when:
        log.debug('Updating B to index 3')
        PUT(id, [index: 3])

        then:
        verifyResponse(OK, response)
        response.body().index == 3

        when:
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].index == 0
        response.body().items[1].index == 1
        response.body().items[2].index == 2
        response.body().items[3].index == 3
        response.body().items[0].key == 'A'
        response.body().items[1].key == 'C'
        response.body().items[2].key == 'D'
        response.body().items[3].key == 'B'

        when:
        log.debug('Updating B to index 2')
        PUT(id, [index: 2])

        then:
        verifyResponse(OK, response)
        response.body().index == 2

        when:
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].index == 0
        response.body().items[1].index == 1
        response.body().items[2].index == 2
        response.body().items[3].index == 3
        response.body().items[0].key == 'A'
        response.body().items[1].key == 'C'
        response.body().items[2].key == 'B'
        response.body().items[3].key == 'D'

        when:
        log.debug('Updating B to index 4 (end of list so returned index should be 3)')
        PUT(id, [index: 4])

        then:
        verifyResponse(OK, response)
        response.body().index == 3

        when:
        GET('')

        then:
        verifyResponse(OK, response)
        response.body().items[0].index == 0
        response.body().items[1].index == 1
        response.body().items[2].index == 2
        response.body().items[3].index == 3
        response.body().items[0].key == 'A'
        response.body().items[1].key == 'C'
        response.body().items[2].key == 'D'
        response.body().items[3].key == 'B'
    }
}