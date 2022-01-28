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
package uk.ac.ox.softeng.maurodatamapper.terminology.facet.rule

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemRuleFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

/**
 * Where facet owner is a TermRelationshipType
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.RuleController
 */
@Integration
@Slf4j
class TermRelationshipTypeRuleFunctionalSpec extends CatalogueItemRuleFunctionalSpec {

    @Shared
    Terminology terminology

    @Shared
    TermRelationshipType termRelationshipType

    String getCatalogueItemCopyPath() {
        "termRelationshipTypes/${sourceCatalogueItemId}/newForkModel"
    }

    @Transactional
    String getSourceCatalogueItemId() {
        termRelationshipType.id.toString()
    }

    @Transactional
    String getDestinationCatalogueItemId() {
        // newForkModel doesn't require a destination terminology
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        terminology = new Terminology(label: 'Functional Test Terminology', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                                      folder: folder, authority: testAuthority).save(flush: true)

        Term term1 = new Term(code: 'Functional Test Code Source', definition: 'Functional Test Definition',
                              terminology: terminology, createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true)

        Term term2 = new Term(code: 'Functional Test Code Target', definition: 'Functional Test Definition',
                              terminology: terminology, createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true)

        termRelationshipType = new TermRelationshipType(label: 'Functional Test TermRelationshipType',
                                                        terminology: terminology, createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true)

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(Terminology, Term, TermRelationship, TermRelationshipType)
    }

    @Override
    UUID getCatalogueItemId() {
        termRelationshipType.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        "termRelationshipTypes"
    }

    @Override
    void verifyCIF01SuccessfulCatalogueItemCopy(HttpResponse response) {
        // Rule only copied for new doc version
    }

    @Override
    HttpResponse requestCIF01CopiedCatalogueItemFacet(HttpResponse response) {
        /// Rule only copied for new doc version
    }

    @Override
    void verifyCIF01CopiedFacetSuccessfully(HttpResponse response) {
        // Rule only copied for new doc version
    }

}