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
package uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider.DataBindDataModelImporterProviderServiceSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature

/**
 * @since 04/08/2017
 */
@Integration
@Rollback
@Slf4j
class DataModelXmlImporterServiceSpec extends DataBindDataModelImporterProviderServiceSpec<DataModelXmlImporterService> {

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

    void 'test multi-import invalid DataModel content'() {
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
        exception.errorCode == NO_DATAMODEL_TO_IMPORT_CODE

        when: 'given an empty model map (backwards compatibility)'
        importModels(loadTestFile('emptyDataModel'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_DATAMODEL_TO_IMPORT_CODE

        // when: 'given an empty models list'
        // importModels(loadTestFile('emptyDataModelList'))
        //
        // then:
        // exception = thrown(ApiBadRequestException)
        // exception.errorCode == 'TODO'
    }

    @PendingFeature
    void 'test multi-import invalid DataModels'() {
        given:
        setupData()

        expect:
        importerService.canImportMultipleDomains()

        when: 'given an invalid model map (backwards compatibility)'
        importModels(loadTestFile('invalidDataModel'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_DATAMODEL_TO_IMPORT_CODE

        when: 'given a single invalid model'
        importModels(loadTestFile('invalidDataModelInList'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_DATAMODEL_TO_IMPORT_CODE

        when: 'given multiple invalid models'
        importModels(loadTestFile('invalidDataModels'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == NO_DATAMODEL_TO_IMPORT_CODE

        // when: 'not given export metadata'
        // importModels(loadTestFile('noExportMetadata'))
        //
        // then:
        // exception = thrown(ApiBadRequestException)
        // exception.errorCode == 'TODO'
    }

    void 'test multi-import single DataModel (backwards compatibility)'() {
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
        ObjectDiff simpleDiff = dataModelService.getDiffForModels(dataModels.pop(), imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import single DataModel'() {
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
        ObjectDiff simpleDiff = dataModelService.getDiffForModels(dataModels.pop(), imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    void 'test multi-import multiple DataModels'() {
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
        ObjectDiff simpleDiff = dataModelService.getDiffForModels(dataModels[0], imported[0])
        ObjectDiff complexDiff = dataModelService.getDiffForModels(dataModels[1], imported[1])

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }

    @PendingFeature
    void 'test multi-import DataModels with invalid models'() {
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
        ObjectDiff simpleDiff = dataModelService.getDiffForModels(dataModels[0], imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        when:
        imported = importModels(loadTestFile('simpleComplexAndInvalidDataModels'))

        then:
        imported
        imported.size() == 2

        when:
        simpleDiff = dataModelService.getDiffForModels(dataModels[0], imported[0])
        ObjectDiff complexDiff = dataModelService.getDiffForModels(dataModels[1], imported[1])

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
    }
}
