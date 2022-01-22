/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.facet.annotation

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.CatalogueItemAnnotationFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 *  Controller: annotation
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}  | Action: delete
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/annotations/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationController
 */
@Integration
@Slf4j
class DataModelAnnotationFunctionalSpec extends CatalogueItemAnnotationFunctionalSpec {

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Override
    String getModelId() {
        getComplexDataModelId()
    }

    @Override
    String getCatalogueItemDomainType() {
        'dataModels'
    }

    @Override
    String getCatalogueItemId() {
        getComplexDataModelId()
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test annotation 2",
      "createdBy": "development@test.com",
      "createdByUser": {
        "name": "Anonymous User",
        "id": "${json-unit.matches:id}"
      },
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "description": "with description"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "test annotation 1",
      "createdBy": "development@test.com",
      "createdByUser": {
        "name": "Anonymous User",
        "id": "${json-unit.matches:id}"
      },
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ]
}
'''
    }
}