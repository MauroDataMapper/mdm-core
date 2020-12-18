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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.facet

import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.CatalogueItemModelImportFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j

import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.NOT_FOUND

/**
 * <pre>
 * Controller: modelImport
 * The Simple Test DataModel imports the string PrimitiveType from the Complex Test DataModel.
 *
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports        | Action: save
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports/${id}  | Action: delete
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports/${id}  | Action: update
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelImports/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.ModelImportController
 */
@Integration
@Slf4j
class DataModelImportingDataTypeModelImportFunctionalSpec extends CatalogueItemModelImportFunctionalSpec {

    @Transactional
    String getImportedDataModelId() {
        DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id.toString()
    }

    @Transactional
    String getImportedDataTypeId() {
        DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).findDataTypeByLabel('string on finalised simple data model').id.toString()
    }   

    @Transactional
    String getImportingDataModelId() {
        DataModel.findByLabel(BootstrapModels.SIMPLE_DATAMODEL_NAME).id.toString()
    }     

    @Override
    String getModelId() {
        getImportingDataModelId()
    }

    @Override
    String getCatalogueItemDomainType() {
        'dataModels'
    }

    @Override
    String getCatalogueItemId() {
        getImportingDataModelId()
    }

    @Override
    Map getValidJson() {
        [
            importedCatalogueItemDomainType      : 'DataType',
            importedCatalogueItemId              : getImportedDataTypeId()
        ]
    }   

    void verifyInvalidUpdateResponse(HttpResponse response) {
        verifyResponse NOT_FOUND, response
    }     

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "catalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Simple Test DataModel"
  },
  "importedCatalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "PrimitiveType",
    "label": "string on finalised simple data model",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Finalised Example Test DataModel",
        "domainType": "DataModel",
        "finalised": true
      }
    ]
  }
}'''
    }    

    @Override
    void verifyE03ValidResponseBody(HttpResponse<Map> response) {
        assert responseBody().id
        assert responseBody().catalogueItem
        assert responseBody().importedCatalogueItem
    }
}