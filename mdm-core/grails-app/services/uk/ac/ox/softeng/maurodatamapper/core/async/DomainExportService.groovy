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
package uk.ac.ox.softeng.maurodatamapper.core.async

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 18/05/2022
 */
@Slf4j
@Transactional
class DomainExportService implements MdmDomainService<DomainExport> {

    @Autowired(required = false)
    List<MdmDomainService> mdmDomainServices

    @Override
    DomainExport get(Serializable id) {
        DomainExport.get(id)
    }

    @Override
    List<DomainExport> getAll(Collection<UUID> resourceIds) {
        DomainExport.getAll(resourceIds)
    }

    @Override
    List<DomainExport> list(Map args) {
        DomainExport.list()
    }

    @Override
    Long count() {
        DomainExport.count()
    }

    @Override
    void delete(DomainExport domain) {
        domain.delete(flush: true)
    }

    @Override
    DomainExport findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        throw new ApiNotYetImplementedException('DES', 'findByParentIdAndPathIdentifier')
    }

    List<DomainExport> findAllReadableByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination) {

        if (!mdmDomainServices) return []
        Class[] classes = mdmDomainServices
            .findAll {SecurableResourceService.isAssignableFrom(it.class)}
            .collect {it.domainClass}.toArray() as Class[]
        List<UUID> readableModelIds = userSecurityPolicyManager.listReadableSecuredResourceIds(classes)

        DomainExport.findAllByExportedDomainIdInList(readableModelIds, pagination)
    }

    DomainExport findByDomainAndExporterProviderService(MdmDomain domain, ExporterProviderService exporterProviderService) {
        DomainExport.byExportedDomainAndExporterProviderService(domain, exporterProviderService).get()
    }

    DomainExport createAndSaveNewDomainExport(ExporterProviderService exporterProviderService, MdmDomain domain, String filename, ByteArrayOutputStream exportByteArray,
                                              User exporter) {

        // Check for existing export which will be updated if it exists
        DomainExport domainExport
        // = findByDomainAndExporterProviderService(domain, exporterProviderService)

        // If no existing export then create a new object to store the export
        //if (!domainExport) {
        domainExport = new DomainExport().tap {
            exportedDomain = domain
            exporterService = exporterProviderService
            exportFileName = filename
            multiDomainExport = false
        }
        //}

        domainExport.tap {
            exportData = exportByteArray.toByteArray()
            createdBy = exporter.emailAddress
        }

        if (!domainExport.validate()) throw new ApiInvalidModelException('DES', 'DomainExport model is not valid', domainExport.errors)

        save(domainExport, validate: false, flush: true)
    }

    DomainExport createAndSaveNewDomainExport(ExporterProviderService exporterProviderService, List<MdmDomain> domains, String filename, ByteArrayOutputStream exportByteArray,
                                              User exporter) {

        // Check for existing export which will be updated if it exists
        DomainExport domainExport
        // = findByDomainAndExporterProviderService(domain, exporterProviderService)

        // If no existing export then create a new object to store the export
        //if (!domainExport) {
        domainExport = new DomainExport().tap {
            exportedDomainIds = domains.collect {it.id.toString()}.join(',')
            exportedDomainType = domains.first().domainType
            exporterService = exporterProviderService
            exportFileName = filename
            multiDomainExport = true
        }
        //}

        domainExport.tap {
            exportData = exportByteArray.toByteArray()
            createdBy = exporter.emailAddress
        }

        if (!domainExport.validate()) throw new ApiInvalidModelException('DES', 'DomainExport model is not valid', domainExport.errors)

        save(domainExport, validate: false, flush: true)
    }
}
