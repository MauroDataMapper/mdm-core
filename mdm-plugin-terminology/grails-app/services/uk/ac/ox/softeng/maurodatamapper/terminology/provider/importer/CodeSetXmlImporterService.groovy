/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.CodeSetXmlExporterService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters

import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChild

import java.nio.charset.Charset

@Slf4j
class CodeSetXmlImporterService extends DataBindCodeSetImporterProviderService<CodeSetFileImporterProviderServiceParameters> implements XmlImportMapping {

    @Override
    String getDisplayName() {
        'XML CodeSet Importer'
    }

    @Override
    String getVersion() {
        '5.0'
    }

    @Override
    Boolean canImportMultipleDomains() {
        true
    }

    @Override
    Boolean handlesContentType(String contentType) {
        contentType.equalsIgnoreCase(CodeSetXmlExporterService.CONTENT_TYPE)
    }

    @Override
    CodeSet importCodeSet(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('XTIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('XTIS02', 'Cannot import empty content')

        String xml = new String(content, Charset.defaultCharset())

        log.debug('Parsing in file content using XmlSlurper')
        GPathResult result = new XmlSlurper().parseText(xml)

        log.debug('Converting result to Map')
        Map map = convertToMap(result)

        log.debug('Importing CodeSet map')
        bindMapToCodeSet(currentUser, map.codeSet as Map)
    }

    @Override
    List<CodeSet> importCodeSets(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('XTIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('XTIS02', 'Cannot import empty content')

        String xml = new String(content, Charset.defaultCharset())

        log.debug('Parsing in file content using XmlSlurper')
        GPathResult result = new XmlSlurper().parseText(xml)
        result = result.children()[0].name() == 'codeSets' ? result.children()[0] : result

        if (result.name() == 'codeSets') {
            log.debug('Importing CodeSets list')
            List<Map> xmlMaps = convertToList(result as NodeChild)
            List<Map> codeSetMaps = xmlMaps.findAll {it}
            if (!codeSetMaps) throw new ApiBadRequestException('XIS03', 'Cannot import XML as codeset/s is not present')
            if (codeSetMaps.size() < xmlMaps.size()) log.warn('Cannot import certain XML as codeset/s is not present')

            log.debug('Importing list of CodeSet maps')
            return codeSetMaps.collect {bindMapToCodeSet(currentUser, new HashMap(it))}
        }

        // Handle single CodeSet map or exportModel passed to this method, for backwards compatibility

        log.debug('Converting result to Map')
        Map map = convertToMap(result)

        log.debug('Importing CodeSet map')
        [bindMapToCodeSet(currentUser, backwardsCompatibleExtractCodeSetMap(result, map))]
    }

    private Map backwardsCompatibleExtractCodeSetMap(GPathResult result, Map map) {
        if (result.name() == 'exportModel' && map.codeSet && map.codeSet instanceof Map) return map.codeSet as Map
        if (result.name() == 'codeSet') return map
        throw new ApiBadRequestException('XIS03', 'Cannot import XML as codeSet is not present')
    }
}
