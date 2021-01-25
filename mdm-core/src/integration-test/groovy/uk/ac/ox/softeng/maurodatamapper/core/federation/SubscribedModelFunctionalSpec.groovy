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
package uk.ac.ox.softeng.maurodatamapper.core.federation

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.federation.SubscribedModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore

import groovy.util.logging.Slf4j

import spock.lang.Shared

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.federation.SubscribedModelController* Controller: subscribedModel
 *  | POST   | /api/subscribedModels       | Action: save   |
 *  | GET    | /api/subscribedModels       | Action: index  |
 *  | DELETE | /api/subscribedModels/${id} | Action: delete |
 *  | PUT    | /api/subscribedModels/${id} | Action: update |
 *  | GET    | /api/subscribedModels/${id} | Action: show   |
 *
 */
@Integration
@Slf4j
class SubscribedModelFunctionalSpec extends ResourceFunctionalSpec<SubscribedModel> {

    @Shared
    UUID subscribedCatalogueId

    @Shared
    UUID folderId

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data for SubscribedModelFunctionalSpec')
        sessionFactory.currentSession.flush()
        folderId = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert folderId

        subscribedCatalogueId = new SubscribedCatalogue(url: 'http://functional-test.example.com',
                                                        apiKey: '67421316-66a5-4830-9156-b1ba77bba5d1',
                                                        label: 'Functional Test Label',
                                                        description: 'Functional Test Description',
                                                        refreshPeriod: 7,
                                                        createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert subscribedCatalogueId
        
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec SubscribedModelFunctionalSpec')
        cleanUpResources(Folder, SubscribedCatalogue)
    }    

    @Override
    String getResourcePath() {
        "subscribedCatalogues/${getSubscribedCatalogueId()}/subscribedModels"
    }

    Map getValidJson() {
        [
            subscribedModelId: '67421316-66a5-4830-9156-b1ba77bba5d1',
            folderId: getFolderId(),
            subscribedModelType: 'DataModel'
        ]
    }

    Map getInvalidJson() {
        [
            subscribedModelId: null,
            folderId: getFolderId()
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "subscribedModelId": "${json-unit.matches:id}",
  "folderId": "${json-unit.matches:id}"
}'''
    }
}
