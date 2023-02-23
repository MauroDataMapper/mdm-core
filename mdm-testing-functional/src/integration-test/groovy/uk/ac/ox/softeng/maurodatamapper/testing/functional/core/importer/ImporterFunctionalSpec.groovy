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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.importer

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelJsonImporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.TerminologyJsonImporterService
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
        DataModelJsonImporterService jsonImporterService = new DataModelJsonImporterService()
        String endpoint = 'parameters/' +
                          "${jsonImporterService.class.packageName}/" +
                          "${jsonImporterService.class.simpleName}/" +
                          "${jsonImporterService.version}"
        when: 'Unlogged in call to check'
        GET(endpoint)

        then:
        verifyForbidden response

        when: 'logged in as user'
        loginAuthenticated()
        GET(endpoint)

        then:
        verifyResponse OK, response
        confirmModelImporter(jsonImporterService)
    }

    void 'test terminology importer parameters'() {
        given:
        TerminologyJsonImporterService jsonImporterService = new TerminologyJsonImporterService()
        String endpoint = 'parameters/' +
                          "${jsonImporterService.class.packageName}/" +
                          "${jsonImporterService.class.simpleName}/" +
                          "${jsonImporterService.version}"
        when: 'Unlogged in call to check'
        GET(endpoint)

        then:
        verifyForbidden response

        when: 'logged in as user'
        loginAuthenticated()
        GET(endpoint)

        then:
        verifyResponse OK, response
        confirmModelImporter(jsonImporterService)
    }

    void confirmModelImporter(ModelImporterProviderService modelImporterProviderService) {
        confirmImporter(responseBody().importer as Map, modelImporterProviderService)
        assert responseBody().parameterGroups
        assert responseBody().parameterGroups.size() == 5
        confirmSource(responseBody().parameterGroups.find {it.name == 'Source'} as Map)
        confirmModel(responseBody().parameterGroups.find {it.name == 'Model'} as Map)
        confirmModelBranching(responseBody().parameterGroups.find {it.name == 'Model Branching'} as Map)
        confirmModelInformation(responseBody().parameterGroups.find {it.name == 'Model Information'} as Map)
        Map importProcess = responseBody().parameterGroups.find {it.name == 'Import Process'} as Map
        assert importProcess
        assert importProcess.parameters.first().displayName == 'Import Asynchronously'
    }

    void confirmImporter(Map importer, ModelImporterProviderService modelImporterProviderService) {
        assert importer
        assert importer.name == modelImporterProviderService.name
        assert importer.version == modelImporterProviderService.version
        assert importer.displayName == modelImporterProviderService.displayName
        assert importer.namespace == modelImporterProviderService.namespace
        assert importer.allowsExtraMetadataKeys == modelImporterProviderService.allowsExtraMetadataKeys()
        assert importer.knownMetadataKeys == modelImporterProviderService.knownMetadataKeys.toList()
        assert importer.providerType == modelImporterProviderService.providerType - 'Provider'
        assert importer.paramClassType == modelImporterProviderService.importerProviderServiceParametersClass.name
        assert importer.canImportMultipleDomains == modelImporterProviderService.canImportMultipleDomains()
    }

    void confirmSource(Map source) {
        assert source
        assert source.parameters
        assert source.parameters.size() == 1
        confirmParameter(source.parameters.find {
            it.name == 'importFile'
        } as Map,
                         'File',
                         false,
                         'File',
                         'The file containing the data to be imported'
        )
    }

    void confirmModel(Map model) {
        assert model
        assert model.parameters
        assert model.parameters.size() == 6
        confirmParameter(model.parameters.find {
            it.name == 'folderId'
        } as Map,
                         'Folder',
                         false,
                         'Folder',
                         'The folder into which the Model/s should be imported.'
        )
        confirmParameter(model.parameters.find {
            it.name == 'modelName'
        } as Map,
                         'String',
                         true,
                         'Model name',
                         'Label of Model, this will override any existing name provided in the imported data. ' +
                         'Note that if importing multiple models this will be ignored.'
        )
        confirmParameter(model.parameters.find {
            it.name == 'importAsNewBranchModelVersion'
        } as Map,
                         'Boolean',
                         false,
                         'Import as New Branch Model Version',
                         'Should the Model/s be imported as new Branch Version/s. If selected then the latest finalised model with the same ' +
                         'name will be chosen as the base. If not selected then the \'Model Name\' field should be used to ensure the imported ' +
                         'Model is uniquely' +
                         ' named, otherwise you could get an error.'
        )
        confirmParameter(model.parameters.find {
            it.name == 'importAsNewDocumentationVersion'
        } as Map,
                         'Boolean',
                         false,
                         'Import as New Documentation Version',
                         'Should the Model/s be imported as new Documentation Version/s. If selected then any models with the same name will ' +
                         'be superseded and the imported models will be given the latest documentation version of the existing Models. If not ' +
                         'selected then the ' +
                         '\'Model Name\' field should be used to ensure the imported Model is uniquely named, otherwise you could get an error.'
        )
        confirmParameter(model.parameters.find {
            it.name == 'finalised'
        } as Map,
                         'Boolean',
                         false,
                         'Finalised',
                         'Whether the new model is to be marked as finalised. ' +
                         'Note that if the model is already finalised this will not be overridden.'
        )
    }

    void confirmModelBranching(Map modelBranching) {
        assert modelBranching
        assert modelBranching.parameters
        assert modelBranching.parameters.size() == 1
        confirmParameter(modelBranching.parameters.find {
            it.name == 'newBranchName'
        } as Map,
                         'String',
                         true,
                         'New Branch Name',
                         'Name for the branch if importing as new branch model version. Default if not provided is "main". Each branch ' +
                         'from a finalised model must have a unique name. If the branch name already exists then the model will not be imported.'
        )
    }

    void confirmModelInformation(Map modelInformation) {
        assert modelInformation
        assert modelInformation.parameters
        assert modelInformation.parameters.size() == 3
        confirmParameter(modelInformation.parameters.find {
            it.name == 'organisation'
        } as Map,
                         'String',
                         true,
                         'Organisation',
                         'The organisation which created the Model'
        )
        confirmParameter(modelInformation.parameters.find {
            it.name == 'author'
        } as Map,
                         'String',
                         true,
                         'Author',
                         'The author of the file, can be the same as the organisation'
        )
        confirmParameter(modelInformation.parameters.find {
            it.name == 'description'
        } as Map,
                         'String',
                         true,
                         'Model Description',
                         'The description of the Model being imported')

    }

    void confirmParameter(Map parameter, String type, boolean optional, String displayName, String description) {
        assert parameter
        assert parameter.size() == 5
        assert parameter.type == type
        assert parameter.optional == optional
        assert parameter.displayName == displayName
        assert parameter.description == description
    }
}
