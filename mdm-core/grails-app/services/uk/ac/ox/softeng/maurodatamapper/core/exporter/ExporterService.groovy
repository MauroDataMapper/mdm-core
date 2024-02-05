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
package uk.ac.ox.softeng.maurodatamapper.core.exporter

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional

@Transactional
class ExporterService {

    ByteArrayOutputStream exportDomain(User currentUser, ExporterProviderService exporterProviderService, UUID domainId, Map<String, Object> parameters) {
        exporterProviderService.exportDomain(currentUser, domainId, parameters)
    }

    @Deprecated
    ByteArrayOutputStream exportDomain(User currentUser, ExporterProviderService exporterProviderService, String domainId, Map<String, Object> parameters) {
        exportDomain(currentUser, exporterProviderService, Utils.toUuid(domainId), parameters)
    }

    ByteArrayOutputStream exportDomains(User currentUser, ExporterProviderService exporterProviderService, List<Serializable> domainIds,
                                        Map<String, Object> parameters = [:]) {
        exporterProviderService.exportDomains(currentUser, domainIds.collect {Utils.toUuid(it)}, parameters)
    }

    AsyncJob asyncExportDomain(User currentUser, ExporterProviderService exporterProviderService, MdmDomain domain, Map<String, Object> parameters) {
        exporterProviderService.asyncExportDomain(currentUser, domain, parameters)
    }

    AsyncJob asyncExportDomains(User currentUser, ExporterProviderService exporterProviderService, List<MdmDomain> domains, Map<String, Object> parameters) {
        exporterProviderService.asyncExportDomains(currentUser, domains, parameters)
    }
}