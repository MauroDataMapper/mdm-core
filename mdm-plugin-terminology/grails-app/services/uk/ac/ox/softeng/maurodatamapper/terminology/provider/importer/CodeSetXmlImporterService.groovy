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
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.XmlImportMapping
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import org.springframework.core.io.Resource

import java.nio.charset.Charset
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

@Slf4j
class CodeSetXmlImporterService extends DataBindCodeSetImporterProviderService<CodeSetFileImporterProviderServiceParameters> implements XmlImportMapping {

    @Override
    String getDisplayName() {
        'XML CodeSet Importer'
    }

    @Override
    String getVersion() {
        '3.0'
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
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
        bindMapToCodeSet currentUser, map.codeSet as Map
    }
}