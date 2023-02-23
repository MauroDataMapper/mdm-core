/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec
import uk.ac.ox.softeng.maurodatamapper.test.unit.JsonWebUnitSpec

import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

@Slf4j
class ImporterControllerSpec extends BaseUnitSpec implements ControllerUnitTest<ImporterController>, JsonWebUnitSpec {

    void 'test parameters response for no params'() {
        when:
        controller.parameters()

        then:
        verifyResponse(HttpStatus.BAD_REQUEST)
        model.message == 'Namespace, name and version must be provided to identify individual importers'
    }

    void 'test the importer parameters are returned correctly'() {

        given:
        controller.mauroDataMapperServiceProviderService = Mock(MauroDataMapperServiceProviderService) {
            findImporterProvider(_, _, _) >> new TestImporterProviderService()
        }
        controller.importerService = new ImporterService()

        String expectedJson = '''{
  "importer": {
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterControllerSpec.TestFileImporterProviderServiceParameters",
    "knownMetadataKeys": [
      
    ],
    "displayName": "Test Importer",
    "name": "TestImporterProviderService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.core.importer",
    "allowsExtraMetadataKeys": true,
    "canImportMultipleDomains": true,
    "version": "1.0",
    "providerType": "Importer"
  },
  "parameterGroups": [
    {
      "name": "DataModel",
      "parameters": [
        {
          "displayName": "Folder",
          "name": "folderId",
          "description": "The folder into which the DataModel/s should be imported.",
          "optional": false,
          "type": "Folder"
        },
        {
          "displayName": "DataModel name",
          "name": "dataModelName",
          "description": "Label of DataModel, this will override any existing name provided in the imported data.\\nNote that if importing multiple models this will be''' +
                              ''' ignored.",
          "optional": true,
          "type": "String"
        },
        {
          "displayName": "Finalised",
          "name": "finalised",
          "description": "Whether the new model is to be marked as finalised.\\nNote that if the model is already finalised this will not be overridden.",
          "optional": false,
          "type": "Boolean"
        },
        {
          "displayName": "Import as New Documentation Version",
          "name": "importAsNewDocumentationVersion",
          "description": "Should the DataModel/s be imported as new Documentation Version/s.\\nIf selected then any models with the same name will be superseded and the''' +
                              ' imported models will be given the latest documentation version of the\\nexisting DataModels.\\nIf not selected then the \'DataModel Name\' ' +
                              'field should be used to ' +
                              '''ensure the imported DataModel is uniquely named,\\notherwise you could get an error.",
          "optional": false,
          "type": "Boolean"
        }
      ]
    },
    {
      "name": "Source",
      "parameters": [
        {
          "displayName": "File",
          "name": "importFile",
          "description": "The file containing the data to be imported",
          "optional": false,
          "type": "File"
        }
      ]
    },
    {
      "name": "Import Process",
      "parameters": [
        {
          "displayName": "Import Asynchronously",
          "name": "asynchronous",
          "description": "Choose to start the import process asynchronously. The import process will need to checked via the returned AsyncJob to see when its completed.''' +
                              ''' Any errors which occur whilst importing can also be seen here. Default is false.",
          "optional": false,
          "type": "Boolean"
        }
      ]
    }
  ]
}'''

        when:
        params.ns = 'namespace'
        params.name = 'name'
        params.version = '1'
        controller.parameters()

        then:
        verifyJsonResponse HttpStatus.OK, expectedJson
    }

    class TestFileImporterProviderServiceParameters extends ImporterProviderServiceParameters {

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
            true
        }

        @Override
        String getDisplayName() {
            'Test Importer'
        }

        @Override
        String getVersion() {
            '1.0'
        }

        @Override
        Boolean handlesContentType(String contentType) {
            contentType.equalsIgnoreCase('application/mauro.test')
        }
    }
}