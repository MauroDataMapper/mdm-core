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
package uk.ac.ox.softeng.maurodatamapper.referencedata.facet.rule

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemRuleFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

/**
 * Where facet owner is a ReferenceDataModel
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.RuleController
 */
@Integration
@Slf4j
class ReferenceDataModelRuleFunctionalSpec extends CatalogueItemRuleFunctionalSpec {

    @Shared
    ReferenceDataModel referenceDataModel
    @Shared
    ReferenceDataElement referenceDataElement
    @Shared
    ReferenceDataType referenceDataType


    String getCatalogueItemCopyPath() {
        "referenceDataModels/${sourceCatalogueItemId}/newForkModel"
    }

    @Transactional
    String getSourceCatalogueItemId() {
        ReferenceDataModel.findByLabel('Functional Test ReferenceDataModel').id.toString()
    }

    @Transactional
    String getDestinationCatalogueItemId() {
        // newForkModel doesn't require a destination data model
    }

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        referenceDataModel = new ReferenceDataModel(label: 'Functional Test ReferenceDataModel', createdBy: 'functionalTest@test.com',
                                  folder: folder, authority: testAuthority).save(flush: true)
        referenceDataType = new ReferencePrimitiveType(label: 'string', createdBy: 'functionalTest@test.com',
                                     referenceDataModel: referenceDataModel).save(flush: true)
        referenceDataElement = new ReferenceDataElement(label: 'Functional Test ReferenceDataElement', createdBy: 'functionalTest@test.com',
                                      referenceDataModel: referenceDataModel, referenceDataType: referenceDataType).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
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
    void verifyCIF01SuccessfulCatalogueItemCopy(HttpResponse response) {
        // Rules only copied for new doc version
    }

    @Override
    HttpResponse requestCIF01CopiedCatalogueItemFacet(HttpResponse response) {
        // Rules only copied for new doc version
    }

    @Override
    void verifyCIF01CopiedFacetSuccessfully(HttpResponse response) {
        // Rules only copied for new doc version
    }

}