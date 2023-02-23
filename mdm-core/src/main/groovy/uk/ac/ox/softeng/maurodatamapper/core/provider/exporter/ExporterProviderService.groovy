/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.async.DomainExport
import uk.ac.ox.softeng.maurodatamapper.core.async.DomainExportService
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.web.mapping.LinkGenerator
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 16/11/2017
 */
@Slf4j
@CompileStatic
abstract class ExporterProviderService extends MauroDataMapperService {

    @Autowired
    AsyncJobService asyncJobService

    @Autowired
    DomainExportService domainExportService

    @Autowired
    LinkGenerator linkGenerator

    @Deprecated
    ByteArrayOutputStream exportDomain(User currentUser, UUID domainId) throws ApiException {
        exportDomain(currentUser, domainId, [:])
    }

    @Deprecated
    ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds) throws ApiException {
        exportDomains(currentUser, domainIds, [:])
    }

    abstract ByteArrayOutputStream exportDomain(User currentUser, UUID domainId, Map<String, Object> parameters) throws ApiException

    abstract ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds, Map<String, Object> parameters) throws ApiException

    abstract Boolean canExportMultipleDomains()

    abstract String getFileExtension()

    /**
     * MIME type produced by ExporterProviderService
     * @return MIME type
     */
    abstract String getContentType()

    @Override
    String getProviderType() {
        ProviderType.EXPORTER.name
    }

    @Override
    int compareTo(MauroDataMapperService that) {
        if (that instanceof ExporterProviderService) {
            int result = this.order <=> that.order
            if (result == 0) result = super.compareTo(that)
            return result
        } else {
            return super.compareTo(that)
        }
    }

    String getFileName(MdmDomain domain) {
        "${domain.id}.${this.fileExtension}"
    }

    AsyncJob asyncExportDomain(User currentUser, MdmDomain domain, Map<String, Object> parameters) {

        asyncJobService.createAndSaveAsyncJob("Export ${domain.path} using ${this.displayName}",
                                              currentUser.emailAddress) {asyncJobId ->
            domain.attach()
            ByteArrayOutputStream exportByteArray = exportDomain(currentUser, domain.id, parameters)
            DomainExport domainExport = domainExportService.createAndSaveNewDomainExport(this, domain, getFileName(domain), exportByteArray, currentUser)

            Map linkParams = parameters.baseUrl ? domainExport.getAbsoluteDownloadLinkParams(parameters.baseUrl as String) : domainExport.downloadLinkParams
            AsyncJob asyncJob = asyncJobService.get(asyncJobId)
            asyncJob.message = "Download at ${linkGenerator.link(linkParams)}"
            asyncJobService.save(asyncJob)
        }
    }

    AsyncJob asyncExportDomains(User currentUser, List<MdmDomain> domains, Map<String, Object> parameters) {
        asyncJobService.createAndSaveAsyncJob("Export multiple ${domains*.path} using ${this.displayName}",
                                              currentUser.emailAddress) {asyncJobId ->
            domains.each {domain ->
                domain.attach()
            }

            ByteArrayOutputStream exportByteArray = exportDomains(currentUser, domains*.id, parameters)
            DomainExport domainExport = domainExportService.createAndSaveNewDomainExport(this, domains, "${UUID.randomUUID()}.${fileExtension}", exportByteArray, currentUser)
            Map linkParams = parameters.baseUrl ? domainExport.getAbsoluteDownloadLinkParams(parameters.baseUrl as String) : domainExport.downloadLinkParams
            AsyncJob asyncJob = asyncJobService.get(asyncJobId)
            asyncJob.message = "Download ${domains*.id} at ${linkGenerator.link(linkParams)}"
            asyncJobService.save(asyncJob)
        }
    }
}