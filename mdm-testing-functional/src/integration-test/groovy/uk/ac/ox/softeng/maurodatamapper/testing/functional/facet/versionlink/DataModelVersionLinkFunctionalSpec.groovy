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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.versionlink

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.testing.functional.facet.ModelVersionLinkFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * <pre>
 * Controller: versionLink
 *  |  POST    | /api/${modelDomainType}/${modelId}/versionLinks        | Action: save
 *  |  GET     | /api/${modelDomainType}/${modelId}/versionLinks        | Action: index
 *  |  DELETE  | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: delete
 *  |  PUT     | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: update
 *  |  GET     | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkController
 */
@Integration
@Slf4j
class DataModelVersionLinkFunctionalSpec extends ModelVersionLinkFunctionalSpec {

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Override
    String getModelDomainType() {
        'dataModels'
    }

    @Override
    String getModelId() {
        getComplexDataModelId()
    }

    @Transactional
    String getTargetModelId() {
        DataModel.findByLabel('Simple Test DataModel').id.toString()
    }

    String getTargetModelDomainType() {
        'DataModel'
    }

    @Override
    String getModelJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Complex Test DataModel"
  }'''
    }

    @Override
    String getTargetModelJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Simple Test DataModel"
  }'''
    }
}