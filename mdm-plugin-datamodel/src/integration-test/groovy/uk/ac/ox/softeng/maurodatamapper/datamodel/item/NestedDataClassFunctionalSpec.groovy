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


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

/**
 * @see DataClassController* Controller: dataClass
 *  | POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses       | Action: save   |
 *  | GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses       | Action: index  |
 *  | DELETE | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id} | Action: delete |
 *  | PUT    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id} | Action: update |
 *  | GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id} | Action: show   |
 *
 */
@Integration
@Slf4j
class NestedDataClassFunctionalSpec extends ResourceFunctionalSpec<DataClass> {

    @Shared
    UUID dataModelId

    @Shared
    UUID otherDataModelId

    @Shared
    UUID dataTypeId

    @Shared
    Folder folder

    @Shared
    UUID dataClassId

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        assert Folder.count() == 0
        assert DataModel.count() == 0
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        DataModel dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: FUNCTIONAL_TEST,
                                            folder: folder, authority: testAuthority).save(flush: true)
        dataModelId = dataModel.id
        otherDataModelId = new DataModel(label: 'Functional Test DataModel 2', createdBy: FUNCTIONAL_TEST,
                                         folder: folder, authority: testAuthority).save(flush: true).id
        DataClass dataClass = new DataClass(label: 'Functional Test DataClass', createdBy: FUNCTIONAL_TEST,
                                            dataModel: dataModel).save(flush: true)
        dataClassId = dataClass.id

        dataTypeId = new PrimitiveType(label: 'string', createdBy: FUNCTIONAL_TEST,
                                       dataModel: dataModel).save(flush: true).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec NestedDataClassFunctionalSpec')
        cleanUpResources(DataType, DataModel, Folder)
    }

    @Override
    int getExpectedInitialResourceCount() {
        dataClassId ? 1 : 0
    }

    @Override
    String getResourcePath() {
        getResourcePath(dataModelId, dataClassId)
    }

    String getResourcePath(def dataModelId, def dataClassId) {
        "dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses"
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
  "parentDataClass": "${json-unit.matches:id}",
  "id": "${json-unit.matches:id}",
  "label": "A new DataClass",
  "breadcrumbs": [
    {
      "domainType": "DataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataModel"
    },
    {
      "domainType": "DataClass",
      "id": "${json-unit.matches:id}",
      "label": "Functional Test DataClass"
    }
  ]
}'''
    }
}