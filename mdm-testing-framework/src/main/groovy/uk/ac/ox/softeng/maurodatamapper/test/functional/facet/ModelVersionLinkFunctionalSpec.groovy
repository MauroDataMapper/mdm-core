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
package uk.ac.ox.softeng.maurodatamapper.test.functional.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType

import groovy.util.logging.Slf4j

/**
 * Where facet owner is a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkController* Controller: versionLink
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/versionLinks       | Action: save                                 |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/versionLinks       | Action: index                                |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/versionLinks/${id} | Action: delete                               |
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/versionLinks/${id} | Action: update                               |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/versionLinks/${id} | Action: show                                 |
 */
@Slf4j
abstract class ModelVersionLinkFunctionalSpec extends CatalogueItemFacetFunctionalSpec<VersionLink> {

    abstract String getTargetModelId()

    abstract String getTargetModelDomainType()

    abstract String getModelDomainType()

    abstract String getTargetModelJsonString()

    abstract String getSourceModelJsonString()

    String getModelId() {
        getCatalogueItemId().toString()
    }

    @Override
    String getFacetResourcePath() {
        'versionLinks'
    }

    @Override
    Map getValidJson() {
        [
            linkType             : VersionLinkType.NEW_MODEL_VERSION_OF.label,
            targetModelId        : getTargetModelId(),
            targetModelDomainType: getTargetModelDomainType()
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            linkType   : VersionLinkType.NEW_MODEL_VERSION_OF.label,
            targetModel: getModelId(),

        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "linkType": "''' + VersionLinkType.NEW_MODEL_VERSION_OF.label + '''",
  "domainType": "VersionLink",
  "sourceModel": ''' + getSourceModelJsonString() + ''',
  "targetModel": ''' + getTargetModelJsonString() + '''
}'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        response.body().linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label
    }
}