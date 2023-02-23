/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.referencefile

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.CatalogueItemReferenceFileFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 * Controller: referenceFile
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: delete
 *  |  PUT     | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: update
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/referenceFiles/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileController
 */
@Integration
@Slf4j
class DataElementReferenceFileFunctionalSpec extends CatalogueItemReferenceFileFunctionalSpec {

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
}