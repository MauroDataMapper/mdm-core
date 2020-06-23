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

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.CatalogueItemSummaryMetadataFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 * Controller: summaryMetadata
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata        | Action: save
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: delete
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: update
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/summaryMetadata/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataController
 */
@Integration
@Slf4j
class DataElementSummaryMetadataFunctionalSpec extends CatalogueItemSummaryMetadataFunctionalSpec {

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