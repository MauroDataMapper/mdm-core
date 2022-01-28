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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 14/09/2020
 */
@Slf4j
abstract class CodeSetExporterProviderService extends ExporterProviderService {

    @Autowired
    CodeSetService codeSetService

    abstract ByteArrayOutputStream exportCodeSet(User currentUser, CodeSet codeSet) throws ApiException

    abstract ByteArrayOutputStream exportCodeSets(User currentUser, List<CodeSet> codeSets) throws ApiException

    @Override
    ByteArrayOutputStream exportDomain(User currentUser, UUID domainId) throws ApiException {
        CodeSet codeSet = codeSetService.get(domainId)
        if (!codeSet) {
            log.error('Cannot find codeSet id [{}] to export', domainId)
            throw new ApiInternalException('CSEP01', "Cannot find codeSet id [${domainId}] to export")
        }
        exportCodeSet(currentUser, codeSet)
    }

    @Override
    ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds) throws ApiException {
        List<CodeSet> codeSets = []
        List<UUID> cannotExport = []
        domainIds?.unique()?.each {
            CodeSet codeSet = codeSetService.get(it)
            if (codeSet) codeSets << codeSet
            else cannotExport << it
        }
        if (!codeSets) throw new ApiBadRequestException('CSEP01', "Cannot find CodeSet IDs [${cannotExport}] to export")
        if (cannotExport) log.warn('Cannot find CodeSet IDs [{}] to export', cannotExport)
        exportCodeSets(currentUser, codeSets)
    }

    @Override
    Boolean canExportMultipleDomains() {
        false
    }

    @Override
    String getProviderType() {
        "CodeSet${ProviderType.EXPORTER.name}"
    }
}
