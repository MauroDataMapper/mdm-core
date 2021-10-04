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

import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.test.provider.DataBindTerminologyImporterProviderServiceSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * @since 18/09/2020
 */
@Integration
@Rollback
@Slf4j
class TerminologyXmlImporterServiceSpec extends DataBindTerminologyImporterProviderServiceSpec<TerminologyXmlImporterService> {

    TerminologyXmlImporterService terminologyXmlImporterService

    @Override
    TerminologyXmlImporterService getImporterService() {
        terminologyXmlImporterService
    }

    String getImportType() {
        'xml'
    }

    void 'test import multiple Terminologies'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2
        importerService.canImportMultipleDomains()

        when:
        List<Terminology> terminologies = importerService.importTerminologies(admin, loadTestFile('simpleAndComplexTerminologies'))

        then:
        terminologies
        terminologies.size() == 2

        when:
        ObjectDiff simpleDiff = terminologyService.getDiffForModels(simpleTerminology, terminologies[0])
        ObjectDiff complexDiff = terminologyService.getDiffForModels(complexTerminology, terminologies[1])

        then:
        simpleDiff.objectsAreIdentical()
        !complexDiff.objectsAreIdentical() // Expected

        // Rules are not imported/exported and will therefore exist as diffs
        complexDiff.numberOfDiffs == 4
        complexDiff.diffs.find {it.fieldName == 'rule'}.deleted.size() == 1
        complexDiff.diffs.find {it.fieldName == 'terms'}.modified[0].diffs.deleted.size() == 1
        complexDiff.diffs.find {it.fieldName == 'termRelationships'}.modified[0].diffs.deleted.size() == 1
        complexDiff.diffs.find {it.fieldName == 'termRelationshipTypes'}.modified[0].diffs.deleted.size() == 1
    }
}
