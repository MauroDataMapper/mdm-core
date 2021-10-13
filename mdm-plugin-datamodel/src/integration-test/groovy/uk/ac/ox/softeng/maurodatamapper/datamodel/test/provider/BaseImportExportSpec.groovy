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

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec

import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 20/11/2017
 */
@Slf4j
abstract class BaseImportExportSpec extends BaseDataModelIntegrationSpec {

    @Shared
    Path resourcesPath

    @Shared
    UUID complexDataModelId

    @Shared
    UUID simpleDataModelId

    public static final String DATAMODEL_WITH_DATATYPES_FILENAME = 'export_with_datatypes_only'
    public static final String COMPLETE_DATAMODEL_EXPORT_FILENAME = 'export_cancer_audits'

    abstract ImporterProviderService getImporterService()

    abstract String getImportType()

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', importType)
        assert getImporterService()
    }

    @Override
    void setupDomainData() {
        log.debug('Setting up DataModelServiceSpec unit')

        complexDataModelId = buildComplexDataModel().id
        simpleDataModelId = buildSimpleDataModel().id
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}.${importType}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    void confirmDataModel(DataModel dataModel) {
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
    }
}
