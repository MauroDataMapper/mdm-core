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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.modelextend

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.ModelExtend
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.functional.CatalogueItemModelExtendFunctionalSpec
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
 * Where facet owner is a DataClass and it extends a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.ModelExtendController
 */
@Integration
@Slf4j
@Ignore('No longer relevant')
class DataClassExtendingDataClassModelExtendFunctionalSpec extends CatalogueItemModelExtendFunctionalSpec {

    @Shared
    DataModel extendedDataModel

    @Shared
    DataClass extendedParentDataClass

    @Shared
    DataClass extendedChildDataClass

    @Shared  
    DataModel extendingDataModel

    @Shared
    DataClass extendingDataClass

    @Shared
    DataModel destinationDataModel    

    String getCatalogueItemCopyPath() {
        "dataModels/${destinationDataModelId}/dataClasses/${sourceDataModelId}/${sourceDataClassId}"
    }

    @Transactional
    String getExtendedChildDataClassId() {
        DataClass.findByLabel('Extended Child DataClass').id.toString()
    }    

    @Transactional
    String getExtendingDataClassId() {
        DataClass.findByLabel('Extending DataClass').id.toString()
    }    

    @Transactional
    String getExtendingDataModelId() {
        DataModel.findByLabel('Extending DataModel').id.toString()
    }       

    @Transactional
    String getDestinationDataModelId() {
        DataModel.findByLabel('Destination DataModel for Extend Test').id.toString()
    }    

    @Transactional
    String getSourceDataModelId() {
        getExtendingDataModelId()
    }    

    @Transactional
    String getSourceDataClassId() {
        getExtendingDataClassId()
    }       

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        extendedDataModel = new DataModel(label: 'Extended DataModel', createdBy: 'functionalTest@test.com',
                                          folder: folder, authority: testAuthority).save(flush: true)

        extendedDataModel.finalised = true
        extendedDataModel.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        extendedDataModel.breadcrumbTree.finalised = true
        extendedDataModel.modelVersion = Version.from('1.0.0')
        extendedDataModel.save(flush: true)                                           

        extendedParentDataClass = new DataClass(label: 'Extended Parent DataClass', createdBy: 'functionalTest@test.com',
                                                dataModel: extendedDataModel).save(flush: true)

        extendedChildDataClass = new DataClass(label: 'Extended Child DataClass', createdBy: 'functionalTest@test.com',
                                               dataModel: extendedDataModel, dataClass: extendedParentDataClass).save(flush: true)                                                

        extendingDataModel = new DataModel(label: 'Extending DataModel', createdBy: 'functionalTest@test.com',
                                           folder: folder, authority: testAuthority).save(flush: true)

        extendingDataClass = new DataClass(label: 'Extending DataClass', createdBy: 'functionalTest@test.com',
                                           dataModel: extendingDataModel, authority: testAuthority).save(flush: true)  

        destinationDataModel = new DataModel(label: 'Destination DataModel for Extend Test', createdBy: 'functionalTest@test.com',
                                             folder: folder, authority: testAuthority).save(flush: true)
                                                                                                                                                                
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(DataModel, Folder, DataClass, ModelExtend)
    }

    @Override
    UUID getCatalogueItemId() {
        extendingDataClass.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'dataClasses'
    }

    @Override
    Map getValidJson() {
        [
            extendedCatalogueItemDomainType : "DataClass",
            extendedCatalogueItemId         : getExtendedChildDataClassId()
        ]
    }

    //Invalid because the DomainType is wrong, so the extended catalogue item does not exist
    @Override
    Map getInvalidJson() {
        [
            extendedCatalogueItemDomainType : "DataElement",
            extendedCatalogueItemId         : getExtendedChildDataClassId()
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            extendedCatalogueItemDomainType : "DataClass"
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "catalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Extending DataClass",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Extending DataModel",
        "domainType": "DataModel",
        "finalised": false
      }
    ]
  },
  "extendedCatalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Extended Child DataClass",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Extended DataModel",
        "domainType": "DataModel",
        "finalised": true
      }
    ]
  }
}'''
    }    

}
