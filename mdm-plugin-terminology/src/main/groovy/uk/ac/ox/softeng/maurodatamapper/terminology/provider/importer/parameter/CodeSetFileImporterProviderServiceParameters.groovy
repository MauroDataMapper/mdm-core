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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig


/**
 * @since 14/09/2020
 */
class CodeSetFileImporterProviderServiceParameters extends CodeSetImporterProviderServiceParameters {

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
        displayName = 'CodeSet name',
        description = '''Label of CodeSet, this will override any existing name provided in the imported data.''',
        group = @ImportGroupConfig(
            name = 'CodeSet',
            order = 0
        )
    )
    String codeSetName

    @ImportParameterConfig(
        displayName = 'Finalised',
        description = '''Whether the new model is to be marked as finalised. Note that if the model is already finalised this will not be overridden.''',
        group = @ImportGroupConfig(
            name = 'CodeSet',
            order = 0
        )
    )
    Boolean finalised

    @ImportParameterConfig(
        displayName = 'Import as New Documentation Version',
        description = '''Should the CodeSet be imported as a new Documentation Version.
If selected then any models with the same name will be superseded and the imported models will be given the latest documentation version of the 
existing CodeSet.
If not selected then the 'CodeSet Name' field should be used to ensure the imported CodeSet is uniquely named, 
otherwise you could get an error.''',
        group = @ImportGroupConfig(
            name = 'CodeSet',
            order = 0
        )
    )
    Boolean importAsNewDocumentationVersion

    @ImportParameterConfig(
        displayName = 'Folder',
        description = 'The folder into which the CodeSet should be imported.',
        group = @ImportGroupConfig(
            name = 'CodeSet',
            order = 0
        )
    )
    UUID folderId

}
