package uk.ac.ox.softeng.maurodatamapper.core.provider


import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService

import org.springframework.beans.factory.annotation.Autowired

class MauroDataMapperServiceProviderService extends MauroDataMapperProviderService {

    @Autowired(required = false)
    Set<DataLoaderProviderService> dataLoaderProviderServices

    @Autowired(required = false)
    Set<EmailProviderService> emailProviderServices

    @Autowired(required = false)
    Set<ImporterProviderService> importerProviderServices

    @Autowired(required = false)
    Set<ExporterProviderService> exporterProviderServices

    Set<DataLoaderProviderService> getDataLoaderProviderServices() {
        dataLoaderProviderServices ?: [] as HashSet
    }

    Set<ImporterProviderService> getImporterProviderServices() {
        importerProviderServices ?: [] as HashSet
    }

    Set<ExporterProviderService> getExporterProviderServices() {
        exporterProviderServices ?: [] as HashSet
    }

    DataLoaderProviderService findDataLoaderProvider(String namespace, String name, String version) {
        findService(dataLoaderProviderServices ?: [] as HashSet, namespace, name, version)
    }

    EmailProviderService findEmailProvider(String namespace, String name, String version) {
        emailProviderServices.find {
            it.namespace.equalsIgnoreCase(namespace) &&
            it.name.equalsIgnoreCase(name) &&
            it.version.equalsIgnoreCase(version)
        }
    }

    EmailProviderService getEmailProvider() {
        emailProviderServices.sort {it.order}.first()
    }

    Set<String> getProviderTypes() {
        ProviderType.getProviderTypeNames()
    }

    ImporterProviderService findImporterProvider(String namespace, String name, String version) {
        findService(importerProviderServices, namespace, name, version)
    }

    Map<String, Set<MauroDataMapperService>> getProviderServicesMap() {
        Map<String, Set<MauroDataMapperService>> map = [:]
        map.put("${ProviderType.EMAIL.typeName}".toString(), emailProviderServices)
        map.put("${ProviderType.IMPORTER.typeName}".toString(), importerProviderServices ?: [] as HashSet)
        map.put("${ProviderType.DATALOADER.typeName}".toString(), dataLoaderProviderServices ?: [] as HashSet)
        map.put("${ProviderType.EXPORTER.typeName}".toString(), exporterProviderServices ?: [] as HashSet)
        map.sort()
    }

    Set<MauroDataMapperService> getProviderServices() {
        final Set<MauroDataMapperService> providers = [] as HashSet
        providers.addAll(emailProviderServices)
        providers.addAll(importerProviderServices ?: [] as HashSet)
        providers.addAll(exporterProviderServices ?: [] as HashSet)
        providers
    }

    ExporterProviderService findExporterProvider(String namespace, String name, String version) {
        findService(exporterProviderServices, namespace, name, version)
    }

    MauroDataMapperService findProviderByNamespace(String namespace) {
        providerServices.find {it.namespace.equalsIgnoreCase(namespace)}
    }

    Set<MauroDataMapperService> findProvidersIlikeNamespace(String namespacePrefix) {
        providerServices.findAll {it.namespace.toLowerCase().startsWith(namespacePrefix.toLowerCase())}
    }

    static <T extends MauroDataMapperService, P extends T> T findService(Set<T> beans, String namespace,
                                                                         String name, String version) {
        beans.find {
            it.namespace.equalsIgnoreCase(namespace) &&
            it.name.equalsIgnoreCase(name) &&
            it.version.equalsIgnoreCase(version)
        }
    }

    static <T extends MauroDataMapperService, P extends T> T findLatestService(Set<T> beans, String namespace, String name, String version = null) {
        if (version) {
            beans.find {
                it.namespace.equalsIgnoreCase(namespace) &&
                it.name.equalsIgnoreCase(name) &&
                it.version.equalsIgnoreCase(version)
            }
        } else {
            def plugins = beans.findAll {
                it.namespace.equalsIgnoreCase(namespace) &&
                it.name.equalsIgnoreCase(name)
            }
            if (plugins && plugins.size() > 0) {
                return plugins.sort {it.version}.last()
            } else {
                return null
            }
        }
    }
}
