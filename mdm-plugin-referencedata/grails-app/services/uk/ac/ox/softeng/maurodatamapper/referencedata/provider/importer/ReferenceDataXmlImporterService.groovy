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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.XmlImportMapping
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult

import java.nio.charset.Charset

@Slf4j
class ReferenceDataXmlImporterService
    extends DataBindReferenceDataModelImporterProviderService<ReferenceDataModelFileImporterProviderServiceParameters> implements XmlImportMapping {

    @Override
    String getDisplayName() {
        'XML Reference Data Importer'
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
    ReferenceDataModel importReferenceDataModel(User currentUser, byte[] content, boolean forceDefaultAuthority = true) {
        if (!currentUser) throw new ApiUnauthorizedException('XIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('XIS02', 'Cannot import empty content')

        String xml = new String(content, Charset.defaultCharset())

        log.debug('Parsing in file content using XmlSlurper')
        GPathResult result = new XmlSlurper().parseText(xml)

        Map map = convertToMap(result)

        log.debug('Importing ReferenceDataModel map')
        bindMapToReferenceDataModel currentUser, new HashMap(map.referenceDataModel), forceDefaultAuthority
    }
}
