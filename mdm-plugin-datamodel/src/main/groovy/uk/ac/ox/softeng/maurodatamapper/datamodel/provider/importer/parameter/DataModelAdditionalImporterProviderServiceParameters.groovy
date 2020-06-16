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
package uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig

/**
 * @since 11/07/2018
 */
class DataModelAdditionalImporterProviderServiceParameters extends DataModelFileImporterProviderServiceParameters {

    @ImportParameterConfig(
        displayName = 'Author',
        description = 'The author of the file, can be the same as the organisation',
        optional = true,
        order = 12,
        group = @ImportGroupConfig(
            name = 'DataModel',
            order = 0
        )
    )
    String author

    @ImportParameterConfig(
        displayName = 'DataModel Description',
        description = 'The description of the DataModel being imported',
        optional = true,
        order = 10,
        group = @ImportGroupConfig(
            name = 'DataModel',
            order = 0
        )
    )
    String description

    @ImportParameterConfig(
        displayName = 'Organisation',
        description = 'The organisation which created the XSD',
        optional = true,
        order = 13,
        group = @ImportGroupConfig(
            name = 'DataModel',
            order = 0
        )
    )
    String organisation
}
