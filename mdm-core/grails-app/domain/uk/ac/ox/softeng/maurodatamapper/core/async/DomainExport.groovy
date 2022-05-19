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
    String exportFileType
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
        exportFileType blank: false
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
        this.exportFileType = exporterProviderService.fileType
        this.exportContentType = exporterProviderService.producesContentType
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

    static DetachedCriteria<DomainExport> byExportedDomainAndExporterProviderService(MdmDomain domain, ExporterProviderService exporterProviderService) {
        new DetachedCriteria<DomainExport>(DomainExport)
            .eq('exportedDomainId', domain.id)
            .eq('exportedDomainType', domain.domainType)
            .eq('exporterNamespace', exporterProviderService.namespace)
            .eq('exporterName', exporterProviderService.name)
            .eq('exporterVersion', exporterProviderService.sortableVersion())
    }
}
