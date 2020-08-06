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
package uk.ac.ox.softeng.maurodatamapper.datamodel.tree

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * @since 08/04/2020
 */
@Integration
@Slf4j
class TreeItemFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    UUID dataModelId

    @Shared
    UUID otherDataModelId

    @Shared
    Folder folder

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        assert Folder.count() == 0
        assert DataModel.count() == 0
        folder = new Folder(label: 'Functional Test Folder', createdBy: 'functionalTest@test.com').save(flush: true)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost").save(flush: true)
        DataModel dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: 'functionalTest@test.com',
                                            folder: folder, authority: testAuthority).save(flush: true)
        dataModelId = dataModel.id
        otherDataModelId = new DataModel(label: 'Functional Test DataModel 2', createdBy: 'functionalTest@test.com',
                                         folder: folder).save(flush: true).id

        new DataClass(label: 'Functional Test DataClass', createdBy: 'functionalTest@test.com',
                      dataModel: dataModel).save(flush: true)

        Classifier classifier = new Classifier(label: 'Functional Test Classifier', createdBy: 'functionalTest@test.com').save(flush: true)
        new Classifier(label: 'Functional Test Classifier 2', createdBy: 'functionalTest@test.com').save(flush: true)
        dataModel.addToClassifiers(classifier).save(flush: true)

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(DataModel, Folder, Classifier, DataClass)
    }

    @Override
    String getResourcePath() {
        'tree'
    }

    void 'test folder tree'() {
        when:
        GET('folders', STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test DataModel",
        "hasChildren": true,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard",
        "authority" : "${json-unit.matches:authority}"
      },
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test DataModel 2",
        "hasChildren": false,
        "deleted": false,
        "finalised": false,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard",
        "authority": "${json-unit.matches:authority}" 
      }
    ]
  }
]''')
    }

    void 'test classifiers tree'() {
        when:
        GET('classifiers', STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Classifier",
    "label": "Functional Test Classifier",
    "hasChildren": false,
    "deleted": false
  },
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Classifier",
    "label": "Functional Test Classifier 2",
    "hasChildren": false,
    "deleted": false
  }
]''')
    }

    void 'test tree for datamodel with content'() {
        when:
        GET("folders/dataModels/${dataModelId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Functional Test DataClass",
    "hasChildren": false,
    "modelId": "${json-unit.matches:id}"
  }
]''')
    }

    void 'test tree for datamodel with no content'() {
        when:
        GET("folders/dataModels/${otherDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, '''[]''')
    }

    void 'test getting model superseded models'() {
        given: 'finalised model is created'
        POST("folders/${folder.id}/dataModels", [
            label: 'Functional Test Model model superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("dataModels/$firstId/finalise", [:], MAP_ARG, true)
        verifyResponse OK, response
        PUT("dataModels/$firstId/newModelVersion", [label: 'Functional Test DataModel reader'], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when:
        GET('admin/tree/folders/dataModels/modelSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Model model superseded",
        "hasChildren": false,
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard",
        "authority": "${json-unit.matches:authority}"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId)
        cleanUpData(secondId)
    }

    void 'test getting documentation superseded models'() {
        given: 'finalised model is created'
        POST("folders/${folder.id}/dataModels", [
            label: 'Functional Test Model doc superseded'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("dataModels/$firstId/finalise", [:], MAP_ARG, true)
        verifyResponse OK, response
        PUT("dataModels/$firstId/newDocumentationVersion", [:], MAP_ARG, true)
        verifyResponse CREATED, response
        String secondId = response.body().id

        expect:
        firstId
        secondId

        when:
        GET('admin/tree/folders/dataModels/documentationSuperseded', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Model doc superseded",
        "hasChildren": false,
        "deleted": false,
        "finalised": true,
        "superseded": true,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard",
        "authority": "${json-unit.matches:authority}"
      }
    ]
  }
]
'''

        cleanup:
        cleanUpData(firstId)
        cleanUpData(secondId)
    }

    void 'test getting deleted models'() {
        given: 'finalised model is created'
        POST("folders/${folder.id}/dataModels", [
            label: 'Functional Test Model deleted'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String firstId = response.body().id
        PUT("dataModels/$firstId/finalise", [:], MAP_ARG, true)

        expect:
        firstId

        when:
        DELETE("dataModels/$firstId", MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        response.body().id == firstId
        response.body().deleted

        when:
        GET('admin/tree/folders/dataModels/deleted', STRING_ARG, true)

        then:
        verifyJsonResponse OK, '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
      {
        "id": "${json-unit.matches:id}",
        "domainType": "DataModel",
        "label": "Functional Test Model deleted",
        "hasChildren": false,
        "deleted": true,
        "finalised": true,
        "superseded": false,
        "documentationVersion": "1.0.0",
        "folder": "${json-unit.matches:id}",
        "type": "Data Standard",
        "authority": "${json-unit.matches:authority}"
      }
    ]
  }
]
'''
        cleanup:
        cleanUpData(firstId)
    }
}
