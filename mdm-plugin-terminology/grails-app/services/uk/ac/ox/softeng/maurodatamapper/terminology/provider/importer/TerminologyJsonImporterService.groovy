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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.JsonImportMapping
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.TerminologyJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters

import groovy.util.logging.Slf4j

@Slf4j
class TerminologyJsonImporterService extends DataBindTerminologyImporterProviderService<TerminologyFileImporterProviderServiceParameters> implements JsonImportMapping {

    @Override
    String getDisplayName() {
        'JSON Terminology Importer'
    }

    @Override
    String getVersion() {
        '4.0'
    }

    @Override
    Boolean canImportMultipleDomains() {
        true
    }

    @Override
    Boolean handlesContentType(String contentType) {
        contentType.equalsIgnoreCase(TerminologyJsonExporterService.CONTENT_TYPE)
    }

    @Override
    Terminology importTerminology(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('JIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('JIS02', 'Cannot import empty content')

        def result = slurpAndClean(content)

        Map terminology = result.terminology
        if (!terminology) throw new ApiBadRequestException('JIS03', 'Cannot import JSON as terminology is not present')

        log.debug('Importing Terminology map')
        bindMapToTerminology(currentUser, new HashMap(terminology))
    }

    @Override
    List<Terminology> importTerminologies(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('JIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('JIS02', 'Cannot import empty content')

        log.debug('Parsing in file content using JsonSlurper')
        Object jsonContent = slurpAndClean(content)
        List<Map> jsonMaps = jsonContent.terminologies?.unique() ?: [jsonContent.terminology]

        List<Map> terminologyMaps = jsonMaps.findAll { it }
        if (!terminologyMaps) throw new ApiBadRequestException('JIS03', 'Cannot import JSON as terminology/ies is not present')
        if (terminologyMaps.size() < jsonMaps.size()) log.warn('Cannot import certain JSON as terminology/ies is not present')

        log.debug('Importing list of Terminology maps')
        terminologyMaps.collect { bindMapToTerminology(currentUser, new HashMap(it)) }
    }
}
