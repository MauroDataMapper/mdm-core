/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.test.functional.facet.CatalogueItemRuleFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

/**
 * Where facet owner is a CodeSet
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.RuleController
 */
@Integration
@Slf4j
class CodeSetRuleFunctionalSpec extends CatalogueItemRuleFunctionalSpec {

    @Shared
    CodeSet codeSet

    String getCatalogueItemCopyPath() {
        "codeSets/${sourceCatalogueItemId}/newForkModel"
    }

    @Transactional
    String getSourceCatalogueItemId() {
        CodeSet.findByLabel('Functional Test CodeSet').id.toString()
    }

    @Transactional
    String getDestinationCatalogueItemId() {
        // newForkModel doesn't require a destination CodeSet
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        codeSet = new CodeSet(label: 'Functional Test CodeSet', createdBy: StandardEmailAddress.FUNCTIONAL_TEST,
                              folder: folder, authority: testAuthority).save(flush: true)
        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec')
        cleanUpResources(CodeSet)
    }

    @Override
    UUID getCatalogueItemId() {
        codeSet.id
    }

    @Override
    String getCatalogueItemDomainResourcePath() {
        'codeSets'
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