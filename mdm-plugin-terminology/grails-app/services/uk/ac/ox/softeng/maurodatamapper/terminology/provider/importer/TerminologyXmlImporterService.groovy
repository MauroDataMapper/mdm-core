/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.XmlImportMapping
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild

import java.nio.charset.Charset

@Slf4j
class TerminologyXmlImporterService extends DataBindTerminologyImporterProviderService<TerminologyFileImporterProviderServiceParameters> implements XmlImportMapping {

    @Override
    String getDisplayName() {
        'XML Terminology Importer'
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
    Terminology importTerminology(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('XTIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('XTIS02', 'Cannot import empty content')

        String xml = new String(content, Charset.defaultCharset())

        log.debug('Parsing in file content using XmlSlurper')
        GPathResult result = new XmlSlurper().parseText(xml)

        log.debug('Converting result to Map')
        Map map = convertToMap(result)

        log.debug('Importing Terminology map')
        bindMapToTerminology(currentUser, backwardsCompatibleExtractTerminologyMap(result, map))
    }

    @Override
    List<Terminology> importTerminologies(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('XTIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('XTIS02', 'Cannot import empty content')

        String xml = new String(content, Charset.defaultCharset())

        log.debug('Parsing in file content using XmlSlurper')
        GPathResult result = new XmlSlurper().parseText(xml)
        result = result.children()[0].name() == 'terminologies' ? result.children()[0] : result

        if (result.name() == 'terminologies') {
            log.debug('Importing Terminology list')
            return convertToList(result as NodeChild).collect { bindMapToTerminology(currentUser, it) }
        }

        // Handle single Terminology map or exportModel passed to this method, for backwards compatibility

        log.debug('Converting result to Map')
        Map map = convertToMap(result)

        log.debug('Importing Terminology map')
        [bindMapToTerminology(currentUser, backwardsCompatibleExtractTerminologyMap(result, map))]
    }

    private Map backwardsCompatibleExtractTerminologyMap(GPathResult result, Map map) {
        if (result.name() == 'exportModel') return map.terminology as Map
        if (result.name() == 'terminology') return map
        throw new ApiBadRequestException('XIS03', 'Cannot import XML as terminology is not present')
    }
}
