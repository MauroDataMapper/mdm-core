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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 14/09/2020
 */
@Slf4j
abstract class TerminologyExporterProviderService extends ExporterProviderService {

    @Autowired
    TerminologyService terminologyService

    abstract ByteArrayOutputStream exportTerminology(User currentUser, Terminology terminology, Map<String, Object> parameters) throws ApiException

    abstract ByteArrayOutputStream exportTerminologies(User currentUser, List<Terminology> terminologies, Map<String, Object> parameters) throws ApiException

    @Override
    ByteArrayOutputStream exportDomain(User currentUser, UUID domainId, Map<String, Object> parameters) throws ApiException {
        Terminology terminology = terminologyService.get(domainId)
        if (!terminology) {
            log.error('Cannot find terminology id [{}] to export', domainId)
            throw new ApiInternalException('TEEP01', "Cannot find terminology id [${domainId}] to export")
        }
        exportTerminology(currentUser, terminology, parameters)
    }

    @Override
    ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds, Map<String, Object> parameters) throws ApiException {
        List<Terminology> terminologies = []
        List<UUID> cannotExport = []
        domainIds?.unique()?.each {
            Terminology terminology = terminologyService.get(it)
            if (terminology) terminologies << terminology
            else cannotExport << it
        }
        if (!terminologies) throw new ApiBadRequestException('TEEP01', "Cannot find Terminology IDs [${cannotExport}] to export")
        if (cannotExport) log.warn('Cannot find Terminology IDs [{}] to export', cannotExport)
        exportTerminologies(currentUser, terminologies, parameters)
    }

    @Override
    Boolean canExportMultipleDomains() {
        false
    }

    @Override
    String getProviderType() {
        "Terminology${ProviderType.EXPORTER.name}"
    }

    @Override
    String getFileName(MdmDomain domain) {
        "${(domain as Model).label}.${getFileExtension()}"
    }
}
