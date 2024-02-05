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
package uk.ac.ox.softeng.maurodatamapper.core.async

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.version.Version

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
        DomainExport.list(args)
    }

    List<DomainExport> listWithFilter(Map filter, Map pagination) {
        DomainExport.withFilter(DomainExport.by(), filter).list(pagination)
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
    DomainExport findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        throw new ApiNotYetImplementedException('DES', 'findByParentIdAndPathIdentifier')
    }

    MdmDomain getExportedDomain(UUID domainExportId) {
        if (!domainExportId) return null
        DomainExport domainExport = get(domainExportId)
        if (!domainExport) return null
        getExportedDomain(domainExport)
    }

    MdmDomain getExportedDomain(DomainExport domainExport) {
        if (!domainExport) return null
        getExportedDomain(domainExport.exportedDomainType, domainExport.exportedDomainId)
    }

    MdmDomain getExportedDomain(String exportedDomainType, UUID exportedDomainId) {
        MdmDomainService domainService = mdmDomainServices.find {it.handles(exportedDomainType)}
        if (!domainService) {
            throw new ApiInternalException('DES', "No domain service exists to load DomainExport for exported type ${exportedDomainType}")
        }
        domainService.get(exportedDomainId)
    }

    List<MdmDomain> getExportedDomains(DomainExport domainExport) {
        if (!domainExport) return null

        if (!domainExport.multiDomainExport) return [getExportedDomain(domainExport)]

        domainExport.exportedDomainIds.split(',').collect {exportedDomainId ->
            MdmDomainService domainService = mdmDomainServices.find {it.handles(domainExport.exportedDomainType)}
            if (!domainService) {
                throw new ApiInternalException('DES', "No domain service exists to load DomainExport for exported type ${domainExport.exportedDomainType}")
            }
            domainService.get(exportedDomainId)
        }
    }

    List<DomainExport> findAllReadableByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination) {

        if (!mdmDomainServices) return []
        Class[] classes = mdmDomainServices
            .findAll {SecurableResourceService.isAssignableFrom(it.class)}
            .collect {it.domainClass}.toArray() as Class[]
        List<UUID> readableModelIds = userSecurityPolicyManager.listReadableSecuredResourceIds(classes)

        DomainExport.withFilter(DomainExport.byExportedDomainIdInList(readableModelIds), pagination).list(pagination)
    }

    List<DomainExport> findAllByExportedDomainAndExporterProviderService(UUID domainId, String domainType, String namespace, String name, Version version, Map pagination) {
        DomainExport.withFilter(DomainExport.byExportedDomainAndExporterProviderService(domainId, domainType, namespace, name, version), pagination).list()
    }

    List<DomainExport> findAllByExportedDomain(UUID domainId, String domainType, Map pagination) {
        DomainExport.withFilter(DomainExport.byExportedDomain(domainId, domainType), pagination).list(pagination)
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

    Map updatePaginationAndFilterParameters(Map params) {
        // Remap sorting
        if (params.sort) {
            switch (params.sort) {
                case 'exportedOn':
                    params.sort = 'lastUpdated'
                    break
                case 'exportedBy':
                    params.sort = 'createdBy'
                    break
                case 'export.fileName':
                    params.sort = 'exportFileName'
                    break
                case 'export.contentType':
                    params.sort = 'exportContentType'
                    break
                case 'export.fileSize':
                    params.sort = 'exportfileSize'
                    break
                case 'exporter.namespace':
                    params.sort = 'exporterNamespace'
                    break
                case 'exporter.name':
                    params.sort = 'exporterName'
                    break
                case 'exporter.version':
                    params.sort = 'exporterVersion'
                    break
                case 'exported.domainType':
                    params.sort = 'exportedDomainType'
                    break
            }
        }
        // Remap filtering
        if (params.exportedOn) params.lastUpdated = params.exportedOn
        if (params.exportedBy) params.createdBy = params.exportedBy
        if (params['export.fileName']) params.exportFileName = params['export.fileName']
        if (params['export.contentType']) params.exportContentType = params['export.contentType']
        if (params['export.fileSize']) params.exportFileSize = params['export.fileSize']
        if (params['exporter.namespace']) params.exporterNamespace = params['exporter.namespace']
        if (params['exporter.name']) params.exporterName = params['exporter.name']
        if (params['exporter.version']) params.exporterVersion = params['exporter.version']
        if (params['exported.domainType']) params.exportedDomainType = params['exported.domainType']
        params
    }
}
