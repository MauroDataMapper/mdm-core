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
package uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.parameter

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig

/**
 * @since 11/01/2021
 */
class DataFlowImporterProviderServiceParameters extends ImporterProviderServiceParameters {
    @ImportParameterConfig(
        optional = true,
        displayName = 'DataFlow name',
        description = ['Label of the DataFlow. This will override any existing name provided in the imported data.',
            'Note that if importing multiple DataFlows this will be ignored.'],
        order = 0,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    String modelName
}
