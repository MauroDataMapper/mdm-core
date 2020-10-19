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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig


/**
 * Created by james on 01/06/2017.
 */
class ReferenceDataModelFileImporterProviderServiceParameters extends ReferenceDataModelImporterProviderServiceParameters {

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
        displayName = 'Model name',
        description = ['Label of Model, this will override any existing name provided in the imported data.'],
        order = 0,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    String modelName    

}
