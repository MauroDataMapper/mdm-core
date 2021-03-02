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
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImport
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemModelImportFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Where facet owner is a DataModel and it imports a DataType
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImportController
 */
@Integration
@Slf4j
class DataModelImportingDataTypeModelImportFunctionalSpec extends CatalogueItemModelImportFunctionalSpec {

    @Shared
    DataModel importingDataModel   
    @Shared
    DataModel importedDataModel 
    @Shared
    DataType importedDataType


    String getCatalogueItemCopyPath() {
        "dataModels/${importingDataModelId}/newForkModel"
    }

    @Transactional
    String getImportedDataModelId() {
        DataModel.findByLabel('Imported DataModel').id.toString()
    }

    @Transactional
    String getImportedDataTypeId() {
        DataType.findByLabel('Imported DataType').id.toString()
    }    

    @Transactional
    String getImportingDataModelId() {
        DataModel.findByLabel('Importing DataModel').id.toString()
    }    

    @Transactional
    String getDestinationDataModelId() {
        // newForkModel doesn't require a destination data model
    }    

    @Transactional
    String getSourceDataModelId() {
        getImportingDataModelId()
    }     

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        importedDataModel = new DataModel(label: 'Imported DataModel', createdBy: 'functionalTest@test.com',
                                          folder: folder, authority: testAuthority).save(flush: true)

        importedDataModel.finalised = true
        importedDataModel.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        importedDataModel.breadcrumbTree.finalise()
        importedDataModel.modelVersion = Version.from('1.0.0')
        importedDataModel.save(flush: true)                                           

        importedDataType = new PrimitiveType(label: 'Imported DataType', createdBy: 'functionalTest@test.com',
                                             dataModel: importedDataModel).save(flush: true)

        importingDataModel = new DataModel(label: 'Importing DataModel', createdBy: 'functionalTest@test.com',
                                           folder: folder, authority: testAuthority).save(flush: true)                                       
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(DataModel, Folder, DataType, PrimitiveType, ModelImport)
    }

    @Override
    UUID getCatalogueItemId() {
        importingDataModel.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'dataModels'
    }

    @Override
    Map getValidJson() {
        [
            importedCatalogueItemDomainType : "DataType",
            importedCatalogueItemId         : getImportedDataTypeId()
        ]
    }

    //Invalid because the DomainType is wrong, so the imported catalogue item does not exist
    @Override
    Map getInvalidJson() {
        [
            importedCatalogueItemDomainType : "DataElement",
            importedCatalogueItemId         : getImportedDataTypeId()
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            importedCatalogueItemDomainType : "DataType"
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "catalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Importing DataModel"
  },
  "importedCatalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "PrimitiveType",
    "label": "Imported DataType",
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

    @Override
    void verifyCIF01SuccessfulCatalogueItemCopy(HttpResponse response) {
        // Only copied for new doc version
    }

    @Override
    HttpResponse requestCIF01CopiedCatalogueItemFacet(HttpResponse response) {
        /// Only copied for new doc version
    }

    @Override
    void verifyCIF01CopiedFacetSuccessfully(HttpResponse response) {
        // Only copied for new doc version
    }

}
