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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig

import groovy.transform.CompileStatic

/**
 * @since 06/03/2018
 */
@CompileStatic
abstract class ImporterProviderServiceParameters {

    @ImportParameterConfig(
        displayName = 'Import Asynchronously',
        description = ['Choose to start the import process asynchronously.',
            'The import process will need to checked via the returned AsyncJob to see when its completed.',
            'Any errors which occur whilst importing can also be seen here.',
            'Default is false.'],
        descriptionJoinDelimiter = ' ',
        order = 0,
        group = @ImportGroupConfig(
            name = 'Import Process',
            order = Integer.MAX_VALUE
        ))
    Boolean asynchronous = false

    boolean providerHasSavedModels = false

    // Optional Authority parameter which may be used by importers if they set an authority themselves
    Authority authority
}
