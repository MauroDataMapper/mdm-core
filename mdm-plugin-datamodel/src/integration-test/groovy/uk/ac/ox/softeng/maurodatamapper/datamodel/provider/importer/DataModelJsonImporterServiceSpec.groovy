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
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider.DataBindDataModelImporterProviderServiceSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * @since 15/11/2017
 */
@Integration
@Rollback
@Slf4j
class DataModelJsonImporterServiceSpec extends DataBindDataModelImporterProviderServiceSpec<DataModelJsonImporterService> {

    DataModelJsonImporterService dataModelJsonImporterService

    @Override
    DataModelJsonImporterService getImporterService() {
        dataModelJsonImporterService
    }

    @Override
    String getImportType() {
        'json'
    }

    void 'test multiple DataModel import fails'() {
        given:
        setupData()

        expect:
        !importerService.canImportMultipleDomains()

        when:
        importerService.importDataModels(admin, loadTestFile('simple'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('cannot import multiple DataModels')
    }

    void 'test parameters for federation'() {
        when:
        ModelImporterProviderServiceParameters parameters = dataModelJsonImporterService.createNewImporterProviderServiceParameters()

        then:
        parameters.hasProperty('importFile')
        parameters.hasProperty('importFile').type == FileParameter
    }

    void 'F01 : test import as finalised'() {
        given:
        setupData()
        basicParameters.finalised = true

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('simple'))

        then:
        dm
        dm.finalised
        dm.dateFinalised
        dm.modelVersion == Version.from('1')

        cleanup:
        basicParameters.finalised = false

    }

    void 'F02 : test import as finalised when already imported as finalised'() {
        given:
        setupData()
        basicParameters.finalised = true
        importAndConfirm(loadTestFile('simple'))

        when:
        importModel(loadTestFile('simple'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message == 'Request to finalise import without creating newBranchModelVersion to existing models'

        cleanup:
        basicParameters.finalised = false

    }

    void 'F03 : test import as finalised when already imported as not finalised'() {
        given:
        setupData()
        importAndConfirm(loadTestFile('simple'))

        when:
        basicParameters.finalised = true
        importModel(loadTestFile('simple'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message == 'Request to finalise import without creating newBranchModelVersion to existing models'

        cleanup:
        basicParameters.finalised = false
    }


    void 'MV01 : test import as newBranchModelVersion with no existing model'() {
        given:
        setupData()
        basicParameters.importAsNewBranchModelVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        !dm.finalised
        !dm.dateFinalised
        !dm.modelVersion
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false

    }

    void 'MV02 : test import as newBranchModelVersion with existing finalised model'() {
        given:
        setupData()
        basicParameters.finalised = true
        DataModel v1 = importAndConfirm(loadTestFile('simple'))
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        !dm.finalised
        !dm.dateFinalised
        !dm.modelVersion
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        dm.versionLinks.size() == 1
        dm.versionLinks.find {it.targetModelId == v1.id}

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false

    }

    void 'MV02B : test import as newBranchModelVersion with existing finalised model'() {
        given:

        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simpleDataModel'))

        then:
        true
    }

    void 'MV03 : test import as newBranchModelVersion with existing non-finalised model'() {
        given:
        setupData()
        DataModel v1 = importAndConfirm(loadTestFile('simple'))
        basicParameters.importAsNewBranchModelVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        !dm.finalised
        !dm.dateFinalised
        !dm.modelVersion
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        dm.versionLinks.size() == 1
        dm.versionLinks.find {it.targetModelId == v1.id}

        and:
        v1.finalised
        v1.modelVersion == Version.from('1')

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false

    }

    void 'MV04 : test import as finalised and newBranchModelVersion with no existing model'() {
        given:
        setupData()
        basicParameters.finalised = true
        basicParameters.importAsNewBranchModelVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        dm.finalised
        dm.dateFinalised
        dm.modelVersion == Version.from('1')
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME

        cleanup:
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = false

    }

    void 'MV05 : test import as finalised and newBranchModelVersion with existing finalised model'() {
        given:
        setupData()
        basicParameters.finalised = true
        DataModel v1 = importAndConfirm(loadTestFile('simple'))
        basicParameters.importAsNewBranchModelVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        dm.finalised
        dm.dateFinalised
        dm.modelVersion == Version.from('2')
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        dm.versionLinks.size() == 1
        dm.versionLinks.find {it.targetModelId == v1.id}

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
        basicParameters.finalised = false

    }

    void 'MV06 : test import as finalised and newBranchModelVersion with existing non-finalised model'() {
        given:
        setupData()
        DataModel v1 = importAndConfirm(loadTestFile('simple'))
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.finalised = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        dm.finalised
        dm.dateFinalised
        dm.modelVersion == Version.from('2')
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        dm.versionLinks.size() == 1
        dm.versionLinks.find {it.targetModelId == v1.id}

        and:
        v1.modelVersion == Version.from('1')
        v1.finalised

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
        basicParameters.finalised = false

    }
}
