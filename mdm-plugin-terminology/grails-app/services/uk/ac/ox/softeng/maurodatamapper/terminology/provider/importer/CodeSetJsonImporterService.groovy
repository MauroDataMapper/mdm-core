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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.JsonImportMapping
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j

@Slf4j
class CodeSetJsonImporterService extends DataBindCodeSetImporterProviderService<CodeSetFileImporterProviderServiceParameters> 
    implements JsonImportMapping {

    @Override
    String getDisplayName() {
        'JSON CodeSet Importer'
    }

    @Override
    String getVersion() {
        '3.0'
    }

    @Override
    CodeSet importCodeSet(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('JIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('JIS02', 'Cannot import empty content')

        log.debug('Parsing in file content using JsonSlurper')
        def result = slurpAndClean(content)
        Map codeSet = result.codeSet
        if (!codeSet) throw new ApiBadRequestException('JIS03', 'Cannot import JSON as codeSet is not present')

        log.debug('Importing CodeSet map')
        bindMapToCodeSet currentUser, new HashMap(codeSet)
    }
}
