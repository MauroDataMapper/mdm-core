/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.semanticlink

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemSemanticLinkFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkController
 */
@Integration
@Slf4j
class DataElementSemanticLinkFunctionalSpec extends CatalogueItemSemanticLinkFunctionalSpec {

    @Shared
    DataModel dataModel
    @Shared
    DataModel destinationDataModel
    @Shared
    DataClass dataClass
    @Shared
    DataClass destinationDataClass
    @Shared
    DataElement dataElement
    @Shared
    DataType dataType

    String getCatalogueItemCopyPath() {
        """dataModels/${destinationDataModelId}/dataClasses/${destinationDataClassId}/${catalogueItemDomainResourcePath}/${sourceDataModelId}\
/${sourceDataClassId}/${catalogueItemId}"""
    }

    @Transactional
    String getSourceDataModelId() {
        DataModel.findByLabel('Functional Test DataModel').id.toString()
    }

    @Transactional
    String getDestinationDataModelId() {
        DataModel.findByLabel('Destination Test DataModel').id.toString()
    }

    @Transactional
    String getSourceDataClassId() {
        DataClass.findByLabel('Functional Test DataClass').id.toString()
    }

    @Transactional
    String getDestinationDataClassId() {
        DataClass.findByLabel('Destination Test DataClass').id.toString()
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: 'functionalTest@test.com',
                                  folder: folder, authority: testAuthority).save(flush: true)
        destinationDataModel = new DataModel(label: 'Destination Test DataModel', createdBy: 'functionalTest@test.com',
                                             folder: folder, authority: testAuthority).save(flush: true)
        dataClass = new DataClass(label: 'Functional Test DataClass', createdBy: 'functionalTest@test.com',
                                  dataModel: dataModel).save(flush: true)
        destinationDataClass = new DataClass(label: 'Destination Test DataClass', createdBy: 'functionalTest@test.com',
                                             dataModel: destinationDataModel).save(flush: true)
        dataType = new PrimitiveType(label: 'string', createdBy: 'functionalTest@test.com',
                                     dataModel: dataModel).save(flush: true)
        dataElement = new DataElement(label: 'Functional Test DataElement', createdBy: 'functionalTest@test.com',
                                      dataModel: dataModel, dataClass: dataClass, dataType: dataType).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PluginCatalogueItemFunctionalSpec')
        cleanUpResources(DataModel, Folder, DataClass, DataElement, DataType, SemanticLink)
    }

    @Override
    UUID getCatalogueItemId() {
        dataElement.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'dataElements'
    }

    @Override
    String getTargetCatalogueItemId() {
        dataModel.id.toString()
    }

    @Override
    String getTargetCatalogueItemDomainType() {
        'DataModel'
    }

    @Override
    String getCatalogueItemDomainType() {
        'DataElement'
    }

    @Override
    String getTargetCatalogueItemJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Functional Test DataModel"
  }'''
    }

    @Override
    String getSourceCatalogueItemJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "DataElement",
    "label": "Functional Test DataElement",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel",
        "domainType": "DataModel",
        "finalised": false
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataClass",
        "domainType": "DataClass"
      }
    ]
  }'''
    }
}