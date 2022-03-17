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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED

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
abstract class CatalogueItemSemanticLinkFunctionalSpec extends UserAccessFunctionalSpec {

    abstract String getModelId()

    abstract String getCatalogueItemDomainType()

    abstract String getCatalogueItemId()

    abstract String getCatalogueItemJsonString()

    @Override
    String getResourcePath() {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}/semanticLinks"
    }

    @Override
    String getEditsFullPath(String id) {
        "${getCatalogueItemDomainType()}/${getCatalogueItemId()}"
    }

    @Transactional
    String getTargetCatalogueItemId() {
        DataModel.findByLabel('Simple Test DataModel').id.toString()
    }

    String getTargetCatalogueItemDomainType() {
        'DataModel'
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereTestingUnsecuredResource()
            .withoutAvailableActions()
            .whereAuthors {
                cannotEditDescription()
                cannotUpdate()
            }
    }

    @Override
    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
    }

    @Override
    void verify03ValidResponseBody(HttpResponse<Map> response) {
        assert response.body().id
        assert response.body().linkType == 'Refines'
        assert response.body().domainType == 'SemanticLink'
        assert response.body().sourceMultiFacetAwareItem.id == getCatalogueItemId()
        assert response.body().targetMultiFacetAwareItem.id == getTargetCatalogueItemId()
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[SemanticLink:REFINES:.+?] added to component \[.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[SemanticLink:DOES_NOT_REFINE:.+?] changed properties \[path, linkType]/
    }

    @Override
    Map getValidJson() {
        [
            linkType                           : SemanticLinkType.REFINES.label,
            targetMultiFacetAwareItemId        : getTargetCatalogueItemId(),
            targetMultiFacetAwareItemDomainType: getTargetCatalogueItemDomainType()
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            linkType                 : SemanticLinkType.REFINES.label,
            targetMultiFacetAwareItem: getCatalogueItemId().toString(),

        ]
    }

    @Override
    Map getValidNonDescriptionUpdateJson() {
        [
            linkType: SemanticLinkType.DOES_NOT_REFINE.label
        ]
    }

    @Override
    Map getValidDescriptionOnlyUpdateJson() {
        getValidNonDescriptionUpdateJson()
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 0,
  "items": [

  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "linkType": "Refines",
  "domainType": "SemanticLink",
  "unconfirmed": false,
  "sourceMultiFacetAwareItem": ''' + getCatalogueItemJsonString() + ''',
  "targetMultiFacetAwareItem": ''' + getTargetCatalogueItemJsonString() + '''
}'''
    }

    String getTargetCatalogueItemJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Simple Test DataModel"
  }'''
    }

}