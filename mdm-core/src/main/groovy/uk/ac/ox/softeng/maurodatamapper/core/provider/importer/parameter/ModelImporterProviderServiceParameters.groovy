/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig

import groovy.transform.AutoClone
import groovy.transform.CompileStatic

@AutoClone
@CompileStatic
class ModelImporterProviderServiceParameters extends ImporterProviderServiceParameters {

    @ImportParameterConfig(
        displayName = 'Folder',
        description = 'The folder into which the Model/s should be imported.',
        order = 4,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    UUID folderId

    @ImportParameterConfig(
        optional = true,
        displayName = 'Model name',
        description = ['Label of Model, this will override any existing name provided in the imported data.',
            'Note that if importing multiple models this will be ignored.'],
        descriptionJoinDelimiter = ' ',
        order = 3,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    String modelName

    @ImportParameterConfig(
        displayName = 'Finalised',
        description = ['Whether the new model is to be marked as finalised.',
            'Note that if the model is already finalised this will not be overridden.'],
        descriptionJoinDelimiter = ' ',
        order = 2,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    Boolean finalised = false

    @ImportParameterConfig(
        displayName = 'Import as New Documentation Version',
        description = [
            'Should the Model/s be imported as new Documentation Version/s.',
            'If selected then any models with the same name will be superseded and the imported models will be given the latest',
            'documentation version of the existing Models.',
            'If not selected then the \'Model Name\' field should be used to ensure the imported Model is uniquely named,',
            'otherwise you could get an error.'],
        descriptionJoinDelimiter = ' ',
        order = 1,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    Boolean importAsNewDocumentationVersion = false

    @ImportParameterConfig(
        displayName = 'Import as New Branch Model Version',
        description = [
            'Should the Model/s be imported as new Branch Version/s.',
            'If selected then the latest finalised model with the same name will be chosen as the base.',
            'If not selected then the \'Model Name\' field should be used to ensure the imported Model is uniquely named,',
            'otherwise you could get an error.'],
        descriptionJoinDelimiter = ' ',
        order = 1,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    Boolean importAsNewBranchModelVersion = false

    @ImportParameterConfig(
        displayName = 'Propagate From Previous Version',
        description = 'Propagate descriptions and facets from the last version. Default: false.',
        order = 1,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        )
    )
    Boolean propagateFromPreviousVersion = false

    @ImportParameterConfig(
        optional = true,
        displayName = 'New Branch Name',
        description = [
            'Name for the branch if importing as new branch model version. Default if not provided is "main".',
            'Each branch from a finalised model must have a unique name.',
            'If the branch name already exists then the model will not be imported.'],
        descriptionJoinDelimiter = ' ',
        order = 0,
        group = @ImportGroupConfig(
            name = 'Model Branching',
            order = 1
        ))
    String newBranchName

    @ImportParameterConfig(
        displayName = 'Model Description',
        description = 'The description of the Model being imported',
        optional = true,
        order = 2,
        group = @ImportGroupConfig(
            name = 'Model Information',
            order = 2
        )
    )
    String description

    @ImportParameterConfig(
        displayName = 'Author',
        description = 'The author of the file, can be the same as the organisation',
        optional = true,
        order = 1,
        group = @ImportGroupConfig(
            name = 'Model Information',
            order = 2
        )
    )
    String author

    @ImportParameterConfig(
        displayName = 'Organisation',
        description = 'The organisation which created the Model',
        optional = true,
        order = 0,
        group = @ImportGroupConfig(
            name = 'Model Information',
            order = 2
        )
    )
    String organisation

    @ImportParameterConfig(
        hidden = true
    )
    Boolean useDefaultAuthority = true


}
