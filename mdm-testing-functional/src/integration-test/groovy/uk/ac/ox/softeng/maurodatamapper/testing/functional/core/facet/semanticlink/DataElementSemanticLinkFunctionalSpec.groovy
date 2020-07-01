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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.facet.semanticlink

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.test.junit.TroubleTest
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.CatalogueItemSemanticLinkFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.experimental.categories.Category

/**
 * <pre>
 * Controller: semanticLink
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}  | Action: delete
 *  |  PUT     | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}  | Action: update
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkController
 */
@Integration
@Slf4j
@Category(TroubleTest)
class DataElementSemanticLinkFunctionalSpec extends CatalogueItemSemanticLinkFunctionalSpec {

    @Transactional
    @Override
    String getModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Override
    String getCatalogueItemDomainType() {
        'dataElements'
    }

    @Transactional
    String getContentDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(getModelId()), 'content').get().id.toString()
    }

    @Transactional
    @Override
    String getCatalogueItemId() {
        DataElement.byDataClassIdAndLabel(Utils.toUuid(getContentDataClassId()), 'ele1').get().id.toString()
    }

    @Override
    String getCatalogueItemJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "DataElement",
    "label": "ele1",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Complex Test DataModel",
        "domainType": "DataModel",
        "finalised": false
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "content",
        "domainType": "DataClass"
      }
    ]
  }'''
    }
}