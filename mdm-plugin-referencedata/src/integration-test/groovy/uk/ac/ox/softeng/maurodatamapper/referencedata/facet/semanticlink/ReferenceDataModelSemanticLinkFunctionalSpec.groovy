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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.semanticlink

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemSemanticLinkFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Shared

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkController
 */
@Integration
@Slf4j
class ReferenceDataModelSemanticLinkFunctionalSpec extends CatalogueItemSemanticLinkFunctionalSpec {

    @Shared
    ReferenceDataModel referenceDataModel
    @Shared
    ReferenceDataElement referenceDataElement
    @Shared
    ReferenceDataType referenceDataType
    @Shared
    ReferenceDataModel targetReferenceDataModel

    String getCatalogueItemCopyPath() {
        "referenceDataModels/${sourceDataModelId}/newForkModel"
    }

    @Transactional
    String getSourceDataModelId() {
        ReferenceDataModel.findByLabel('Functional Test ReferenceDataModel').id.toString()
    }

    @Transactional
    String getDestinationDataModelId() {
        // newForkModel doesn't require a destination data model
    }

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        referenceDataModel = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel', createdBy: 'functionalTest@test.com',
                                  folder: folder, authority: testAuthority).save(flush: true)
        targetReferenceDataModel = new ReferenceDataModel(label: 'Functional Test Target ReferenceDataModel', createdBy: 'functionalTest@test.com',
                                        folder: folder, authority: testAuthority).save(flush: true)
        referenceDataType = new ReferencePrimitiveType(label: 'string', createdBy: 'functionalTest@test.com',
                                     referenceDataModel: referenceDataModel).save(flush: true)
        referenceDataElement = new ReferenceDataElement(label: 'Functional Test ReferenceDataElement', createdBy: 'functionalTest@test.com',
                                      referenceDataModel: referenceDataModel, referenceDataType: referenceDataType).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PluginCatalogueItemFunctionalSpec')
        cleanUpResources(ReferenceDataModel, Folder, ReferenceDataElement, ReferenceDataType)
    }

    @Override
    UUID getCatalogueItemId() {
        referenceDataModel.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'referenceDataModels'
    }

    @Override
    String getTargetCatalogueItemId() {
        referenceDataElement.id.toString()
    }

    @Override
    String getTargetCatalogueItemDomainType() {
        'ReferenceDataElement'
    }

    @Override
    String getCatalogueItemDomainType() {
        'ReferenceDataModel'
    }

    @Override
    String getSourceCatalogueItemJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "ReferenceDataModel",
    "label": "Functional Test ReferenceDataModel"
  }'''
    }

    @Override
    String getTargetCatalogueItemJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "ReferenceDataElement",
    "label": "Functional Test ReferenceDataElement",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test ReferenceDataModel",
        "domainType": "ReferenceDataModel",
        "finalised": false
      }
    ]
  }'''
    }

    @Override
    void verifyCIF01SuccessfulCatalogueItemCopy(HttpResponse response) {
        // Semantic link only copied for new doc version
    }

    @Override
    HttpResponse requestCIF01CopiedCatalogueItemFacet(HttpResponse response) {
        // Semantic link only copied for new doc version
    }

    @Override
    void verifyCIF01CopiedFacetSuccessfully(HttpResponse response) {
        // Semantic link only copied for new doc version
    }

    def 'test confirm semantic link'() {
        given: 'Create Semantic Link'
        String id = createNewItem(validJson)

        when: 'finalise and create a new copy of the finalised model'
        PUT("referenceDataModels/${referenceDataModel.id}/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        PUT("referenceDataModels/${referenceDataModel.id}/newForkModel", ['label': 'Functional Test Fork'], MAP_ARG, true)

        then:
        verifyResponse(HttpStatus.CREATED, response)

        when: 'Get the forked models SLs'
        String forkId = responseBody().get("id")
        GET("referenceDataModels/${forkId}/semanticLinks", MAP_ARG, true)

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().count == 2
        responseBody().items.any { Map m ->
            m.linkType == SemanticLinkType.REFINES.label &&
            m.targetCatalogueItem.id == targetCatalogueItemId &&
            m.unconfirmed
        }
        responseBody().items.any { Map m ->
            m.linkType == SemanticLinkType.REFINES.label &&
            m.targetCatalogueItem.id == referenceDataModel.id.toString() &&
            !m.unconfirmed
        }

        when:
        String semanticLinkId = responseBody().items.find { Map m ->
            m.linkType == SemanticLinkType.REFINES.label &&
            m.targetCatalogueItem.id == targetCatalogueItemId &&
            m.unconfirmed
        }.id
        PUT("referenceDataModels/${forkId}/semanticLinks/${semanticLinkId}/confirm", [:], MAP_ARG, true)

        then:
        verifyResponse(HttpStatus.OK, response)

        cleanup:
        DELETE("referenceDataModels/$forkId?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
        cleanUpData(id)
    }
}