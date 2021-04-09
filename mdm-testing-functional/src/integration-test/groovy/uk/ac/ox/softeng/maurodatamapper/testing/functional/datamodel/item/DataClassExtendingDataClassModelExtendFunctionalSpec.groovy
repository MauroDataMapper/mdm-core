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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.facet

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.CatalogueItemModelExtendFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Ignore

import static io.micronaut.http.HttpStatus.NOT_FOUND

/**
 * <pre>
 * Controller: modelExtend
 *
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/modelExtends        | Action: save
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelExtends        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/modelExtends/${id}  | Action: delete
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelExtends/${id}  | Action: update
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/modelExtends/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.ModelExtendController
 */
@Integration
@Slf4j
@Ignore('No longer relevant')
class DataClassExtendingDataClassModelExtendFunctionalSpec extends CatalogueItemModelExtendFunctionalSpec {

    @Transactional
    String getExtendedDataModelId() {
        DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id.toString()
    }

    @Transactional
    String getExtendedDataClassId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXTENDABLE_DATAMODEL_NAME).id, BootstrapModels.FIRST_CLASS_LABEL_ON_FINALISED_EXTENDABLE_DATAMODEL).get().id.toString()
    }   

    @Transactional
    String getExtendingDataModelId() {
        DataModel.findByLabel(BootstrapModels.EXTENDING_DATAMODEL_NAME_1).id.toString()
    }     

    @Transactional
    String getExtendingDataClassId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.EXTENDING_DATAMODEL_NAME_1).id, 'extending class 1').get().id.toString()
    }     

    @Override
    String getModelId() {
        getExtendingDataModelId()
    }

    @Override
    String getCatalogueItemDomainType() {
        'dataClasses'
    }

    @Override
    String getCatalogueItemId() {
        getExtendingDataClassId()
    }

    @Override
    Map getValidJson() {
        [
            extendedCatalogueItemDomainType      : 'DataClass',
            extendedCatalogueItemId              : getExtendedDataClassId()
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
    "domainType": "DataClass",
    "label": "extending class 1",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Xtending DataM0del 1",
        "domainType": "DataModel",
        "finalised": false
      }
    ]
  },
  "extendedCatalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "extendable class on extendable data m0del",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Extendable DataModel",
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
        assert responseBody().extendedCatalogueItem
    }
}
