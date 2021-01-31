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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.facet.rule

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.rule.CatalogueItemRuleRepresentationFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 * Controller: ruleRepresentation
 *  |  POST    | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations        | Action: save
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations        | Action: index
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations/${id}  | Action: delete
 *  |  PUT     | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations/${id}  | Action: update
 *  |  GET     | /api/${catalogueItemDomainType}/${catalogueItemId}/rules/${ruleId}/representations/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.item.RuleRepresentationController
 */
@Integration
@Slf4j
class DataTypeRuleRepresentationFunctionalSpec extends CatalogueItemRuleRepresentationFunctionalSpec {

    @Transactional
    @Override
    CatalogueItem getModel() {
        DataModel.findByLabel(BootstrapModels.COMPLEX_DATAMODEL_NAME)
    }

    @Transactional
    @Override
    CatalogueItem getCatalogueItem() {
        DataType.byDataModelIdAndLabel(Utils.toUuid(getModelId()), 'string').get()
    }

    @Override
    String getCatalogueItemDomainType() {
        'dataTypes'
    }

    @Override
    String getEditsFullPath(String id) {
        "primitiveTypes/${getCatalogueItemId()}"
    }    
}