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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.modelimport

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.ModelImport
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.functional.CatalogueItemModelImportFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Shared

import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Where facet owner is a DataModel and it imports a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.ModelImportController
 */
@Integration
@Slf4j
@Ignore('No longer relevant')
class DataClassImportingDataClassModelImportFunctionalSpec extends CatalogueItemModelImportFunctionalSpec {

    @Shared
    DataModel importedDataModel

    @Shared
    DataClass importedParentDataClass

    @Shared
    DataClass importedChildDataClass

    @Shared  
    DataModel importingDataModel

    @Shared
    DataClass importingDataClass

    @Shared
    DataModel destinationDataModel    

    String getCatalogueItemCopyPath() {
        "dataModels/${destinationDataModelId}/dataClasses/${sourceDataModelId}/${sourceDataClassId}"
    }

    @Transactional
    String getImportedChildDataClassId() {
        DataClass.findByLabel('Imported Child DataClass').id.toString()
    }    

    @Transactional
    String getImportingDataClassId() {
        DataClass.findByLabel('Importing DataClass').id.toString()
    }    

    @Transactional
    String getImportingDataModelId() {
        DataModel.findByLabel('Importing DataModel').id.toString()
    }       

    @Transactional
    String getDestinationDataModelId() {
        DataModel.findByLabel('Destination DataModel').id.toString()
    }    

    @Transactional
    String getSourceDataModelId() {
        getImportingDataModelId()
    }    

    @Transactional
    String getSourceDataClassId() {
        getImportingDataClassId()
    }       

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        importedDataModel = new DataModel(label: 'Imported DataModel', createdBy: 'functionalTest@test.com',
                                          folder: folder, authority: testAuthority).save(flush: true)

        importedDataModel.finalised = true
        importedDataModel.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        importedDataModel.breadcrumbTree.finalised = true
        importedDataModel.modelVersion = Version.from('1.0.0')
        importedDataModel.save(flush: true)                                           

        importedParentDataClass = new DataClass(label: 'Imported Parent DataClass', createdBy: 'functionalTest@test.com',
                                                dataModel: importedDataModel).save(flush: true)

        importedChildDataClass = new DataClass(label: 'Imported Child DataClass', createdBy: 'functionalTest@test.com',
                                               dataModel: importedDataModel, dataClass: importedParentDataClass).save(flush: true)                                                

        importingDataModel = new DataModel(label: 'Importing DataModel', createdBy: 'functionalTest@test.com',
                                           folder: folder, authority: testAuthority).save(flush: true)

        importingDataClass = new DataClass(label: 'Importing DataClass', createdBy: 'functionalTest@test.com',
                                           dataModel: importingDataModel, authority: testAuthority).save(flush: true)  

        destinationDataModel = new DataModel(label: 'Destination DataModel', createdBy: 'functionalTest@test.com',
                                             folder: folder, authority: testAuthority).save(flush: true)
                                                                                                                                                                
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(DataModel, Folder, DataClass, ModelImport)
    }

    @Override
    UUID getCatalogueItemId() {
        importingDataClass.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'dataClasses'
    }

    @Override
    Map getValidJson() {
        [
            importedCatalogueItemDomainType : "DataClass",
            importedCatalogueItemId         : getImportedChildDataClassId()
        ]
    }

    //Invalid because the DomainType is wrong, so the imported catalogue item does not exist
    @Override
    Map getInvalidJson() {
        [
            importedCatalogueItemDomainType : "DataElement",
            importedCatalogueItemId         : getImportedChildDataClassId()
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            importedCatalogueItemDomainType : "DataClass"
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "catalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Importing DataClass",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Importing DataModel",
        "domainType": "DataModel",
        "finalised": false
      }
    ]
  },
  "importedCatalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Imported Child DataClass",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Imported DataModel",
        "domainType": "DataModel",
        "finalised": true
      }
    ]
  }
}'''
    }    

}
