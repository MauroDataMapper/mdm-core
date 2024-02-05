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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.metadata

import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.CatalogueItemMetadataFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 * Controller: metadata
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}  | Action: delete
 *  |  PUT     | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}  | Action: update
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/metadata/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController
 */
@Integration
@Slf4j
class TerminologyMetadataFunctionalSpec extends CatalogueItemMetadataFunctionalSpec {

    @Transactional
    @Override
    String getModelId() {
        Terminology.findByLabel(BootstrapModels.COMPLEX_TERMINOLOGY_NAME).id.toString()
    }

    @Override
    String getCatalogueItemDomainType() {
        'terminologies'
    }

    @Override
    String getCatalogueItemId() {
        getModelId()
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 3,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "namespace": "terminology.test.com/simple",
      "key": "mdk1",
      "value": "mdv1",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "namespace": "terminology.test.com",
      "key": "mdk2",
      "value": "mdv2",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "namespace": "terminology.test.com/simple",
      "key": "mdk2",
      "value": "mdv2",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ]
}'''
    }
}