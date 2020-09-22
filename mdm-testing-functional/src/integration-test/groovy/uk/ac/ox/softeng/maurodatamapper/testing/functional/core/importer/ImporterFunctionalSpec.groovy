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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.importer


import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.JsonImporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.JsonImporterService
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: importer
 *  |  GET     | /api/importer/parameters/${ns}?/${name}?/${version}?  | Action: parameters
 * </pre>
 */
@Integration
@Slf4j
class ImporterFunctionalSpec extends FunctionalSpec {

    @Override
    String getResourcePath() {
        'importer'
    }

    void 'test datamodel importer parameters'() {
        given:
        uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.JsonImporterService jsonImporterService = new uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.JsonImporterService()
        String endpoint = "parameters/${jsonImporterService.class.packageName}/${jsonImporterService.class.simpleName}/${jsonImporterService.version}"
        when: 'Unlogged in call to check'
        GET(endpoint)

        then:
        verifyForbidden response

        when: 'logged in as user'
        loginAuthenticated()
        GET(endpoint, STRING_ARG)

        then:
        verifyJsonResponse OK, getDataModelExpectedJson()
    }

    String getDataModelExpectedJson() {
        '''{
            "importer": {
                "name": "JsonImporterService",
                "version": "2.0",
                "displayName": "JSON DataModel Importer",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [],
                "providerType": "DataModelImporter",
                "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter''' +
        '''.DataModelFileImporterProviderServiceParameters",
                "canImportMultipleDomains": false
            },
            "parameterGroups": [
                {
                    "name": "Model",
                    "parameters": [
                    {
                        "name": "folderId",
                        "type": "Folder",
                        "optional": false,
                        "displayName": "Folder",
                        "description": "The folder into which the Model/s should be imported."
                    },
                    {
                        "name": "modelName",
                        "type": "String",
                        "optional": true,
                        "displayName": "Model name",
                        "description": "Label of Model, this will override any existing name provided in the imported data.\\n''' +
        '''Note that if importing multiple models this will be ignored."
        },
        {
          "name": "finalised",
          "type": "Boolean",
          "optional": false,
          "displayName": "Finalised",
          "description": "Whether the new model is to be marked as finalised.\\n''' +
        '''Note that if the model is already finalised this will not be overridden."
        },
        {
          "name": "importAsNewDocumentationVersion",
          "type": "Boolean",
          "optional": false,
          "displayName": "Import as New Documentation Version",
          "description": "Should the Model/s be imported as new Documentation Version/s.\\n''' +
        '''If selected then any models with the same name will be superseded and the imported models will be given the''' +
        ''' latest documentation version of the existing Models.\\n''' +
        '''If not selected then the \'Model Name\' field should be used to ensure the imported Model is uniquely''' +
        ''' named, otherwise you could get an error."
        }
      ]
    },
    {
      "name": "Source",
      "parameters": [
        {
          "name": "importFile",
          "type": "File",
          "optional": false,
          "displayName": "File",
          "description": "The file containing the data to be imported"
        }
      ]
    }
  ]
}'''
    }

    void 'test terminology importer parameters'() {
        given:
        uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.JsonImporterService jsonImporterService = new uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.JsonImporterService()
        String endpoint = "parameters/${jsonImporterService.class.packageName}/${jsonImporterService.class.simpleName}/${jsonImporterService.version}"
        when: 'Unlogged in call to check'
        GET(endpoint)

        then:
        verifyForbidden response

        when: 'logged in as user'
        loginAuthenticated()
        GET(endpoint, STRING_ARG)

        then:
        verifyJsonResponse OK, getTerminologyExpectedJson()
    }

    String getTerminologyExpectedJson() {
        '''{
  "importer": {
    "name": "JsonImporterService",
    "version": "3.0",
    "displayName": "JSON Terminology Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "TerminologyImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  "parameterGroups": [
    {
      "name": "Model",
      "parameters": [
        {
          "name": "modelName",
          "type": "String",
          "optional": true,
          "displayName": "Model name",
          "description": "Label of Model, this will override any existing name provided in the imported data.\\n''' + 
          '''Note that if importing multiple models this will be ignored."
        }
      ]
    },
    {
      "name": "Terminology",
      "parameters": [
        {
          "name": "folderId",
          "type": "Folder",
          "optional": false,
          "displayName": "Folder",
          "description": "The folder into which the Terminology should be imported."
        },
        {
          "name": "terminologyName",
          "type": "String",
          "optional": true,
          "displayName": "Terminology name",
          "description": "Label of Terminology, this will override any existing name provided in the imported data."
        },
        {
          "name": "finalised",
          "type": "Boolean",
          "optional": false,
          "displayName": "Finalised",
          "description": "Whether the new model is to be marked as finalised. Note that if the model is already finalised this will not be overridden."
        },
        {
          "name": "importAsNewDocumentationVersion",
          "type": "Boolean",
          "optional": false,
          "displayName": "Import as New Documentation Version",
          "description": "Should the Terminology be imported as a new Documentation Version.\\n''' +
          '''If selected then any models with the same name will be superseded and the imported models will be given the latest documentation version of the \\n''' +
          '''existing Terminology.\\n''' + 
          '''If not selected then the 'Terminology Name' field should be used to ensure the imported Terminology is uniquely named, \\n''' +
          '''otherwise you could get an error."
        }
      ]
    },
    {
      "name": "Source",
      "parameters": [
        {
          "name": "importFile",
          "type": "File",
          "optional": false,
          "displayName": "File",
          "description": "The file containing the data to be imported"
        }
      ]
    }
  ]
}'''
    }


}
