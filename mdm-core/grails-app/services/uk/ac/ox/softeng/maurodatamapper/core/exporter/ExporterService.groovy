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