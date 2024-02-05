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

import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.VersionUserType
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.DetachedCriteria

/**
 * @since 17/05/2022
 */
class DomainExport implements MdmDomain {

    UUID id
    UUID exportedDomainId
    String exportedDomainIds
    String exportedDomainType
    MdmDomain exportedDomain
    byte[] exportData
    String exportFileName
    String exportContentType

    String exporterNamespace
    String exporterName
    Version exporterVersion
    Boolean multiDomainExport

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        exportedDomainId nullable: true
        exportedDomainIds nullable: true, blank: false
        exportedDomainType blank: false
        exportData minSize: 1, maxSize: 1e+9.toInteger() // Limit the size to 1GB, more than this is overkill for an export. TBH even 1GB is insane overkill for an export
        exporterNamespace blank: false
        exporterName blank: false
        exportFileName blank: false
        exportContentType nullable: true, blank: false
    }

    static mapping = {
        exporterVersion type: VersionUserType
    }

    static transients = ['exportedDomain']

    @Override
    String getDomainType() {
        DomainExport.simpleName
    }

    @Override
    String getPathPrefix() {
        return null
    }

    @Override
    String getPathIdentifier() {
        return null
    }

    int getExportFileSize() {
        exportData.size()
    }

    void setExportedDomain(MdmDomain exportedDomain) {
        this.exportedDomain = exportedDomain
        this.exportedDomainId = exportedDomain.id
        this.exportedDomainType = exportedDomain.domainType
    }

    void setExporterService(ExporterProviderService exporterProviderService) {
        this.exporterNamespace = exporterProviderService.namespace
        this.exporterName = exporterProviderService.name
        this.exporterVersion = exporterProviderService.sortableVersion()
        this.exportContentType = exporterProviderService.contentType
    }

    Map getDownloadLinkParams() {
        [
            resource: 'domainExport',
            action  : 'download',
            params  : [domainExportId: id,],
        ]
    }

    Map getAbsoluteDownloadLinkParams(String baseUrl = null) {
        getDownloadLinkParams().tap {
            if (baseUrl) put('base', baseUrl)
            put('absolute', true)
        }
    }

    static DetachedCriteria<DomainExport> by() {
        new DetachedCriteria<DomainExport>(DomainExport)
    }

    static DetachedCriteria<DomainExport> byExportedDomain(UUID domainId, String domainType) {
        by()
            .eq('exportedDomainId', domainId)
            .eq('exportedDomainType', domainType)
    }

    static DetachedCriteria<DomainExport> byExportedDomainIdInList(List<UUID> domainIds) {
        by().inList('exportedDomainId', domainIds)
    }

    static DetachedCriteria<DomainExport> byExportedDomainAndExporterProviderService(UUID domainId, String domainType, String namespace, String name, Version version) {
        DetachedCriteria<DomainExport> criteria = byExportedDomain(domainId, domainType)
            .eq('exporterNamespace', namespace)
            .eq('exporterName', name)
        if (version) criteria.eq('exporterVersion', version)
        criteria
    }

    static DetachedCriteria<DomainExport> withFilter(DetachedCriteria<DomainExport> criteria, Map filters) {
        if (filters.exportFileName) criteria = criteria.ilike('exportFileName', "%${filters.exportFileName}%")
        if (filters.exportContentType) criteria = criteria.ilike('exportContentType', "%${filters.exportContentType}%")
        if (filters.createdBy) criteria = criteria.ilike('createdBy', "%${filters.createdBy}%")
        if (filters.exportedDomainType) criteria = criteria.ilike('exportedDomainType', "%${filters.exportedDomainType}%")
        if (filters.exporterNamespace) criteria = criteria.ilike('exporterNamespace', "%${filters.exporterNamespace}%")
        if (filters.exporterName) criteria = criteria.ilike('exporterName', "%${filters.exporterName}%")
        if (filters.exporterVersion) criteria = criteria.ilike('exporterVersion', "%${filters.exporterVersion}%")
        criteria
    }
}
