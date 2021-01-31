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
package uk.ac.ox.softeng.maurodatamapper.core.exporter

import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional

@Transactional
class ExporterService {

    ByteArrayOutputStream exportDomain(User currentUser, ExporterProviderService exporterProviderService, UUID domainId) {
        exporterProviderService.exportDomain(currentUser, domainId)
    }

    ByteArrayOutputStream exportDomain(User currentUser, ExporterProviderService exporterProviderService, String domainId) {
        exporterProviderService.exportDomain(currentUser, Utils.toUuid(domainId))
    }

    ByteArrayOutputStream exportDomains(User currentUser, ExporterProviderService exporterProviderService, List<Serializable> domainIds) {
        exporterProviderService.exportDomains(currentUser, domainIds.collect {Utils.toUuid(it)})
    }
}