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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 14/09/2020
 */
@Slf4j
abstract class TerminologyExporterProviderService extends ExporterProviderService {

    @Autowired
    TerminologyService terminologyService

    abstract ByteArrayOutputStream exportTerminology(User currentUser, Terminology terminology) throws ApiException

    abstract ByteArrayOutputStream exportTerminologies(User currentUser, List<Terminology> terminologies) throws ApiException

    @Override
    ByteArrayOutputStream exportDomain(User currentUser, UUID domainId) throws ApiException {
        Terminology terminology = terminologyService.get(domainId)
        if (!terminology) {
            log.error('Cannot find terminology id [{}] to export', domainId)
            throw new ApiInternalException('TEEP01', "Cannot find terminology id [${domainId}] to export")
        }
        exportTerminology(currentUser, terminology)
    }

    @Override
    ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds) throws ApiException {
        List<Terminology> terminologies = []
        domainIds.each {
            Terminology terminology = terminologyService.get(it)
            if (!terminology) {
                getLogger().warn('Cannot find terminology id [{}] to export', it)
            } else terminologies += terminology
        }
        exportTerminologies(currentUser, terminologies)
    }

    @Override
    Boolean canExportMultipleDomains() {
        false
    }

    @Override
    String getProviderType() {
        "Terminology${ProviderType.EXPORTER.name}"
    }
}
