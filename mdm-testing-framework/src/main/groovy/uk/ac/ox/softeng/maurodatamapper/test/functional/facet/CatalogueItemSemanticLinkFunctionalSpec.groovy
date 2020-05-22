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


import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType

import groovy.util.logging.Slf4j

/**
 * Where facet owner is a DataClass
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkController* Controller: semanticLink
 *  |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks       | Action: save                                 |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks       | Action: index                                |
 *  |  DELETE  | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id} | Action: delete                               |
 *  |   PUT    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id} | Action: update                               |
 *  |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/semanticLinks/${id} | Action: show                                 |
 */
@Slf4j
abstract class CatalogueItemSemanticLinkFunctionalSpec extends CatalogueItemFacetFunctionalSpec<SemanticLink> {

    abstract String getTargetCatalogueItemId()

    abstract String getTargetCatalogueItemDomainType()

    abstract String getCatalogueItemDomainType()

    abstract String getTargetCatalogueItemJsonString()

    abstract String getSourceCatalogueItemJsonString()

    @Override
    String getFacetResourcePath() {
        'semanticLinks'
    }

    @Override
    Map getValidJson() {
        [
            linkType                     : SemanticLinkType.REFINES.label,
            targetCatalogueItemId        : getTargetCatalogueItemId(),
            targetCatalogueItemDomainType: getTargetCatalogueItemDomainType()
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            linkType           : SemanticLinkType.REFINES.label,
            targetCatalogueItem: getCatalogueItemId().toString(),

        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            linkType: SemanticLinkType.DOES_NOT_REFINE.label
        ]
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "linkType": "Refines",
  "domainType": "SemanticLink",
  "sourceCatalogueItem": ''' + getSourceCatalogueItemJsonString() + ''',
  "targetCatalogueItem": ''' + getTargetCatalogueItemJsonString() + '''
}'''
    }

    @Override
    void verifyR4UpdateResponse() {
        super.verifyR4UpdateResponse()
        response.body().linkType == SemanticLinkType.DOES_NOT_REFINE.label
    }
}