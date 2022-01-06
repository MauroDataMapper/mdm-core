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
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet.semanticlink

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemSemanticLinkFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Shared

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkController
 */
@Integration
@Slf4j
class DataModelSemanticLinkFunctionalSpec extends CatalogueItemSemanticLinkFunctionalSpec {

    @Shared
    DataModel dataModel
    @Shared
    DataClass dataClass
    @Shared
    DataElement dataElement
    @Shared
    DataType dataType
    @Shared
    DataModel targetDataModel

    String getCatalogueItemCopyPath() {
        "dataModels/${sourceDataModelId}/newForkModel"
    }

    @Transactional
    String getSourceDataModelId() {
        DataModel.findByLabel('Functional Test DataModel').id.toString()
    }

    @Transactional
    String getDestinationDataModelId() {
        // newForkModel doesn't require a destination data model
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        dataModel = new DataModel(label: 'Functional Test DataModel', createdBy: 'functionalTest@test.com',
                                  folder: folder, authority: testAuthority).save(flush: true)
        targetDataModel = new DataModel(label: 'Functional Test Target DataModel', createdBy: 'functionalTest@test.com',
                                        folder: folder, authority: testAuthority).save(flush: true)
        dataClass = new DataClass(label: 'Functional Test DataClass', createdBy: 'functionalTest@test.com',
                                  dataModel: dataModel).save(flush: true)
        dataType = new PrimitiveType(label: 'string', createdBy: 'functionalTest@test.com',
                                     dataModel: dataModel).save(flush: true)
        dataElement = new DataElement(label: 'Functional Test DataElement', createdBy: 'functionalTest@test.com',
                                      dataModel: dataModel, dataClass: dataClass, dataType: dataType).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PluginCatalogueItemFunctionalSpec')
        cleanUpResources(DataModel, Folder, DataClass, DataElement, DataType, SemanticLink)
    }

    @Override
    UUID getCatalogueItemId() {
        dataModel.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'dataModels'
    }

    @Override
    String getTargetCatalogueItemId() {
        dataClass.id.toString()
    }

    @Override
    String getTargetCatalogueItemDomainType() {
        'DataClass'
    }

    @Override
    String getCatalogueItemDomainType() {
        'DataModel'
    }

    @Override
    String getSourceCatalogueItemJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "Functional Test DataModel"
  }'''
    }

    @Override
    String getTargetCatalogueItemJsonString() {
        '''{
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "Functional Test DataClass",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel",
        "domainType": "DataModel",
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
        PUT("dataModels/${dataModel.id}/finalise", [versionChangeType: "Major"], MAP_ARG, true)
        PUT("dataModels/${dataModel.id}/newForkModel", ['label': 'Functional Test Fork'], MAP_ARG, true)

        then:
        verifyResponse(HttpStatus.CREATED, response)

        when: 'Get the forked models SLs'
        String forkId = responseBody().get("id")
        GET("dataModels/${forkId}/semanticLinks", MAP_ARG, true)

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().count == 2
        responseBody().items.any { Map m ->
            m.linkType == SemanticLinkType.REFINES.label &&
            m.targetMultiFacetAwareItem.id == targetCatalogueItemId &&
            m.unconfirmed
        }
        responseBody().items.any { Map m ->
            m.linkType == SemanticLinkType.REFINES.label &&
            m.targetMultiFacetAwareItem.id == dataModel.id.toString() &&
            !m.unconfirmed
        }
        String semanticLinkId = responseBody().items.find { Map m ->
            m.linkType == SemanticLinkType.REFINES.label &&
            m.targetMultiFacetAwareItem.id == targetCatalogueItemId &&
            m.unconfirmed
        }.id
        assert semanticLinkId

        when:
        PUT("dataModels/${forkId}/semanticLinks/${semanticLinkId}/confirm", [:], MAP_ARG, true)

        then:
        verifyResponse(HttpStatus.OK, response)

        cleanup:
        DELETE("dataModels/$forkId?permanent=true", MAP_ARG, true)
        assert response.status() == HttpStatus.NO_CONTENT
        cleanUpData(id)
    }
}