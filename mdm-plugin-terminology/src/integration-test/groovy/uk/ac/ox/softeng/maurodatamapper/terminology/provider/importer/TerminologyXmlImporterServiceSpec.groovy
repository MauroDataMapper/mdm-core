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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
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

    private static final String CANNOT_IMPORT_EMPTY_CONTENT_CODE = 'XTIS02'
    private static final String NO_TERMINOLOGY_TO_IMPORT_CODE = 'FBIP03'

    TerminologyXmlImporterService terminologyXmlImporterService

    @Override
    TerminologyXmlImporterService getImporterService() {
        terminologyXmlImporterService
    }

    String getImportType() {
        'xml'
    }

    void 'test multi-import invalid Terminology content'() {
        expect:
        importerService.canImportMultipleDomains()

        when: 'given empty content'
        importModels(''.bytes)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_EMPTY_CONTENT_CODE

        when: 'given neither models list or model map (backwards compatibility)'
        importModels(loadTestFile('exportMetadataOnly'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        when: 'given an empty model map (backwards compatibility)'
        importModels(loadTestFile('emptyTerminology'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        // when: 'given an empty models list'
        // importModels(loadTestFile('emptyTerminologyList'))
        //
        // then:
        // exception = thrown(ApiBadRequestException)
        // exception.errorCode == 'TODO'
    }

    void 'test multi-import invalid Terminologies'() {
        given:
        setupData()

        expect:
        importerService.canImportMultipleDomains()

        when: 'given an invalid model map (backwards compatibility)'
        importModels(loadTestFile('invalidTerminology'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        when: 'given a single invalid model'
        importModels(loadTestFile('invalidTerminologyInList'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        when: 'given multiple invalid models'
        importModels(loadTestFile('invalidTerminologies'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        // when: 'not given export metadata'
        // importModels(loadTestFile('noExportMetadata'))
        //
        // then:
        // exception = thrown(ApiBadRequestException)
        // exception.errorCode == 'TODO'
    }

    void 'test multi-import single Terminology (backwards compatibility)'() {
        given:
        setupData()
        Terminology.count() == 2
        List<Terminology> terminologies = clearExpectedDiffsFromModels([simpleTerminologyId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<Terminology> imported = importModels(loadTestFile('simpleTerminology'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = terminologyService.getDiffForModels(terminologies.pop(), imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import single Terminology'() {
        given:
        setupData()
        Terminology.count() == 2
        List<Terminology> terminologies = clearExpectedDiffsFromModels([simpleTerminologyId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<Terminology> imported = importModels(loadTestFile('simpleTerminologyInList'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = terminologyService.getDiffForModels(terminologies.pop(), imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import multiple Terminologies'() {
        given:
        setupData()
        Terminology.count() == 2
        List<Terminology> terminologies = clearExpectedDiffsFromModels([simpleTerminologyId, complexTerminologyId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<Terminology> imported = importModels(loadTestFile('simpleAndComplexTerminologies'))

        then:
        imported
        imported.size() == 2

        when:
        ObjectDiff simpleDiff = terminologyService.getDiffForModels(terminologies[0], imported[0])
        ObjectDiff complexDiff = terminologyService.getDiffForModels(terminologies[1], imported[1])

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import Terminologies with invalid models'() {
        given:
        setupData()
        Terminology.count() == 2
        List<Terminology> terminologies = clearExpectedDiffsFromModels([simpleTerminologyId, complexTerminologyId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<Terminology> imported = importModels(loadTestFile('simpleAndInvalidTerminologies'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = terminologyService.getDiffForModels(terminologies[0], imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        when:
        imported = importModels(loadTestFile('simpleComplexAndInvalidTerminologies'))

        then:
        imported
        imported.size() == 2

        when:
        simpleDiff = terminologyService.getDiffForModels(terminologies[0], imported[0])
        ObjectDiff complexDiff = terminologyService.getDiffForModels(terminologies[1], imported[1])

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }
}
