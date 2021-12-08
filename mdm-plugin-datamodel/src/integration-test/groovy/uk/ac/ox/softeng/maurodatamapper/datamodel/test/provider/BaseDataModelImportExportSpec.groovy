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
package uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataBindDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec

import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 20/11/2017
 */
@Slf4j
abstract class BaseDataModelImportExportSpec extends BaseDataModelIntegrationSpec {

    static final String COMPLETE_DATAMODEL_EXPORT_FILENAME = 'export_cancer_audits'
    static final String DATAMODEL_WITH_DATATYPES_FILENAME = 'export_with_datatypes_only'

    @Autowired
    DataModelService dataModelService

    @Shared
    Path resourcesPath

    @Shared
    UUID simpleDataModelId

    @Shared
    UUID complexDataModelId

    @Shared
    DataModelFileImporterProviderServiceParameters basicParameters

    Object setupSpec() {
        basicParameters = new DataModelFileImporterProviderServiceParameters()
    }

    Object setup() {
        basicParameters.tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
            propagateFromPreviousVersion = false
            importFile = null
        }
    }

    abstract DataBindDataModelImporterProviderService getImporterService()

    abstract String getImportType()

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
        assert getImporterService()
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up DataModelServiceSpec unit')

        simpleDataModelId = buildSimpleDataModel().id
        complexDataModelId = buildComplexDataModel().id
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    DataModel importModel(byte[] bytes) {
        log.trace('Importing:\n {}', new String(bytes))
        basicParameters.importFile = new FileParameter(fileContents: bytes)

        DataModel imported = importerService.importDomain(admin, basicParameters) as DataModel
        assert imported
        imported.folder = testFolder

        log.info('Checking imported model')
        check(imported)

        log.info('Saving imported model')
        assert dataModelService.saveModelWithContent(imported)
        sessionFactory.currentSession.flush()
        // assert dataModelService.count() == 3
        log.debug('DataModel saved')

        dataModelService.get(imported.id)
    }

    DataModel confirmDataModel(DataModel dataModel) {
        assert dataModel
        assert dataModel.label == 'National Minimum Data Set for Thoracic Surgery and Lung Cancer Surgery'
        assert dataModel.modelType == DataModelType.DATA_STANDARD.label
        assert dataModel.createdBy == admin.emailAddress
        assert !dataModel.description
        assert !dataModel.author
        assert !dataModel.organisation
        assert dataModel.breadcrumbTree
        assert dataModel.breadcrumbTree.domainId == dataModel.id
        assert dataModel.breadcrumbTree.label == dataModel.label
        dataModel
    }

    DataModel importAndConfirm(byte[] bytes) {
        importModel(bytes)
        DataModel dm = DataModel.listOrderByDateCreated().last()
        log.info('Confirming imported model')
        confirmDataModel(dm)
    }
}
