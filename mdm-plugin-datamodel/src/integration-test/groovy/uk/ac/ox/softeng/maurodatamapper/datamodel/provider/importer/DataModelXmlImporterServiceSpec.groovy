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
package uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider.DataBindDataModelImporterProviderServiceSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.Tag

/**
 * @since 04/08/2017
 */
@Integration
@Rollback
@Slf4j
@Tag('non-parallel')
class DataModelXmlImporterServiceSpec extends DataBindDataModelImporterProviderServiceSpec<DataModelXmlImporterService> {

    private static final String CANNOT_IMPORT_EMPTY_FILE_CODE = 'FBIP02'
    private static final String NO_DATAMODEL_TO_IMPORT_CODE = 'FBIP03'

    DataModelXmlImporterService dataModelXmlImporterService

    @Override
    DataModelXmlImporterService getImporterService() {
        dataModelXmlImporterService
    }

    @Override
    String getImportType() {
        'xml'
    }

    void 'M01 : test multi-import invalid DataModel content'() {
        expect:
        importerService.canImportMultipleDomains()

        when: 'given empty content'
        importModels(''.bytes)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_EMPTY_FILE_CODE

        when: 'given neither models list or model map (backwards compatibility)'
        importModels(loadTestFile('exportMetadataOnly'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        when: 'given an empty model map (backwards compatibility)'
        importModels(loadTestFile('emptyDataModel'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        // when: 'given an empty models list'
        // importModels(loadTestFile('emptyDataModelList'))
        //
        // then:
        // exception = thrown(ApiBadRequestException)
        // exception.errorCode == 'TODO'
    }

    void 'M02 : test multi-import invalid DataModels'() {
        given:
        setupData()

        expect:
        importerService.canImportMultipleDomains()

        when: 'given an invalid model map (backwards compatibility)'
        importModels(loadTestFile('invalidDataModel'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        when: 'given a single invalid model'
        importModels(loadTestFile('invalidDataModelInList'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == 'XIS03'

        when: 'given multiple invalid models'
        importModels(loadTestFile('invalidDataModels'))

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

    void 'M03 : test multi-import single DataModel (backwards compatibility)'() {
        given:
        setupData()
        DataModel.count() == 2
        List<DataModel> dataModels = clearExpectedDiffsFromModels([simpleDataModelId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<DataModel> imported = importModels(loadTestFile('simpleDataModel'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = dataModels.pop().diff(imported.pop(), 'none', null, null)

        then:
        simpleDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'M04 : test multi-import single DataModel'() {
        given:
        setupData()
        DataModel.count() == 2
        List<DataModel> dataModels = clearExpectedDiffsFromModels([simpleDataModelId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<DataModel> imported = importModels(loadTestFile('simpleDataModelInList'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = dataModels.pop().diff(imported.pop(), 'none', null, null)

        then:
        simpleDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'M05 : test multi-import multiple DataModels'() {
        given:
        setupData()
        DataModel.count() == 2
        List<DataModel> dataModels = clearExpectedDiffsFromModels([simpleDataModelId, complexDataModelId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<DataModel> imported = importModels(loadTestFile('simpleAndComplexDataModels'))

        then:
        imported
        imported.size() == 2

        when:
        ObjectDiff simpleDiff = dataModels[0].diff(imported[0], 'none', null, null)
        ObjectDiff complexDiff = dataModels[1].diff(imported[1], 'none', null, null)

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'M06 : test multi-import DataModels with invalid models'() {
        given:
        setupData()
        DataModel.count() == 2
        List<DataModel> dataModels = clearExpectedDiffsFromModels([simpleDataModelId, complexDataModelId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<DataModel> imported = importModels(loadTestFile('simpleAndInvalidDataModels'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = dataModels[0].diff(imported.pop(), 'none', null, null)

        then:
        simpleDiff.objectsAreIdentical()

        when:
        imported = importModels(loadTestFile('simpleComplexAndInvalidDataModels'))

        then:
        imported
        imported.size() == 2

        when:
        simpleDiff = dataModels[0].diff(imported[0], 'none', null, null)
        ObjectDiff complexDiff = dataModels[1].diff(imported[1], 'none', null, null)

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }
}
