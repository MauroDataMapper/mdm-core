/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.core.importer

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer.ImportParameterGroup
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest

class ImporterServiceSpec extends BaseUnitSpec implements ServiceUnitTest<ImporterService> {

    void 'test import param describer'() {
        given:
        def importer = new TestImporterProviderService()

        when:
        def groups = service.describeImporterParams(importer)

        then:
        groups.size() == 2

        when:
        ImportParameterGroup dmGroup = groups[0]
        ImportParameterGroup sGroup = groups[1]

        then:
        dmGroup.size() == 4
        sGroup.size() == 1

        when:
        def dataModelName = dmGroup.find {it.name == 'dataModelName'}
        def finalised = dmGroup.find {it.name == 'finalised'}
        def importFile = sGroup.find {it.name == 'importFile'}
        def newDocVersion = dmGroup.find {it.name == 'importAsNewDocumentationVersion'}
        def folder = dmGroup.find {it.name == 'folderId'}

        then:
        dataModelName.displayName == 'DataModel name'
        dataModelName.description == '''Label of DataModel, this will override any existing name provided in the imported data.
Note that if importing multiple models this will be ignored.'''
        dataModelName.type == 'String'
        dataModelName.optional

        and:
        finalised.displayName == 'Finalised'
        finalised.description == '''Whether the new model is to be marked as finalised.
Note that if the model is already finalised this will not be overridden.'''
        finalised.type == 'Boolean'
        !finalised.optional

        and:
        newDocVersion.displayName == 'Import as New Documentation Version'
        newDocVersion.description == '''Should the DataModel/s be imported as new Documentation Version/s.
If selected then any models with the same name will be superseded and the imported models will be given the latest documentation version of the 
existing DataModels.
If not selected then the 'DataModel Name' field should be used to ensure the imported DataModel is uniquely named, 
otherwise you could get an error.'''
        newDocVersion.type == 'Boolean'
        !newDocVersion.optional

        and:
        importFile.displayName == 'File'
        importFile.description == 'The file containing the data to be imported'
        importFile.type == 'File'
        !importFile.optional

        and:
        folder.displayName == 'Folder'
        folder.description == 'The folder into which the DataModel/s should be imported.'
        folder.type == 'Folder'
        !folder.optional

    }

    void 'test parameter validation'() {
        given:
        def importer = new TestImporterProviderService()
        TestFileImporterProviderServiceParameters params = service.createNewImporterProviderServiceParameters(importer)

        when: 'no params set'
        def result = service.validateParameters(params, importer.importerProviderServiceParametersClass)

        then:
        result.hasErrors()
        result.errorCount == 4
        result.getFieldError('finalised')
        result.getFieldError('importFile')
        result.getFieldError('importAsNewDocumentationVersion')
        result.getFieldError('folderId')

        when: 'set required params'
        params.importFile = new FileParameter('test', 'test', 'test'.bytes)
        params.finalised = false
        params.importAsNewDocumentationVersion = false
        params.folderId = UUID.randomUUID()

        result = service.validateParameters(params, importer.importerProviderServiceParametersClass)

        then:
        !result.hasErrors()
    }

    class TestFileImporterProviderServiceParameters implements ImporterProviderServiceParameters {

        @ImportParameterConfig(
            displayName = 'File',
            description = 'The file containing the data to be imported',
            order = -1,
            group = @ImportGroupConfig(
                name = 'Source',
                order = 1
            )
        )
        FileParameter importFile

        @ImportParameterConfig(
            optional = true,
            displayName = 'DataModel name',
            description = '''Label of DataModel, this will override any existing name provided in the imported data.
Note that if importing multiple models this will be ignored.''',
            order = 0,
            group = @ImportGroupConfig(
                name = 'DataModel',
                order = 0
            ))
        String dataModelName

        @ImportParameterConfig(
            displayName = 'Finalised',
            description = '''Whether the new model is to be marked as finalised.
Note that if the model is already finalised this will not be overridden.''',
            order = 0,
            group = @ImportGroupConfig(
                name = 'DataModel',
                order = 0
            ))
        Boolean finalised

        @ImportParameterConfig(
            displayName = 'Import as New Documentation Version',
            description = '''Should the DataModel/s be imported as new Documentation Version/s.
If selected then any models with the same name will be superseded and the imported models will be given the latest documentation version of the 
existing DataModels.
If not selected then the 'DataModel Name' field should be used to ensure the imported DataModel is uniquely named, 
otherwise you could get an error.''',
            order = 0,
            group = @ImportGroupConfig(
                name = 'DataModel',
                order = 0
            ))
        Boolean importAsNewDocumentationVersion

        @ImportParameterConfig(
            displayName = 'Folder',
            description = 'The folder into which the DataModel/s should be imported.',
            order = 0,
            group = @ImportGroupConfig(
                name = 'DataModel',
                order = 0
            ))
        UUID folderId

    }

    class TestImporterProviderService extends ImporterProviderService<Folder, TestFileImporterProviderServiceParameters> {

        @Override
        Folder importDomain(User currentUser, TestFileImporterProviderServiceParameters params) {
            return null
        }

        @Override
        List<Folder> importDomains(User currentUser, TestFileImporterProviderServiceParameters params) {
            return null
        }

        @Override
        Boolean canImportMultipleDomains() {
            null
        }

        @Override
        String getDisplayName() {
            'Test Importer'
        }

        @Override
        String getVersion() {
            '1.0'
        }
    }
}