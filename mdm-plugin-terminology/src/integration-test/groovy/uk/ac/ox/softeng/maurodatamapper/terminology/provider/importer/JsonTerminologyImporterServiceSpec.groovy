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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer


import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.TerminologyJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.test.provider.DataBindTerminologyImporterProviderServiceSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * @since 17/09/2020
 */
@Integration
@Rollback
@Slf4j
class JsonTerminologyImporterServiceSpec extends DataBindTerminologyImporterProviderServiceSpec<TerminologyJsonImporterService> implements JsonComparer {

    TerminologyJsonImporterService terminologyJsonImporterService

    @Override
    TerminologyJsonImporterService getImporterService() {
        terminologyJsonImporterService
    }

    @Override
    String getImportType() {
        'json'
    }

    void 'GH77 : test trimming issue'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        Terminology terminology = importAndSave(loadTestFile('trimmingIssue'))

        then:
        terminology.termRelationshipTypes.size() == 1
        terminology.terms.size() == 2

        when:
        Term a = terminology.terms.find { it.code == 'A' }
        Term b = terminology.terms.find { it.code == 'B' }

        then:
        a
        a.definition == 'Alpha'
        a.depth == 1

        and:
        b
        b.definition == 'Beta'
        b.depth == 2

        and:
        a.sourceTermRelationships.size() == 0
        a.targetTermRelationships.size() == 1

        and:
        b.sourceTermRelationships.size() == 1
        b.targetTermRelationships.size() == 0
    }

    void 'PG01 test importing a Terminology and propagating existing information'() {
        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        when:
        Terminology terminology =  importerService.importTerminology(admin, loadTestFile('propagationImportTerminology'))

        then:
        true
    }
}
