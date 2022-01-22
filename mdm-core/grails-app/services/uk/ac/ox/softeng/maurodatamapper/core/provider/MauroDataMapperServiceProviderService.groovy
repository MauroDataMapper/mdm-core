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
        List<EmailProviderService> enabled = emailProviderServices.findAll {it.enabled}.sort {it.order}
        enabled ? enabled.first() : null
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
        if (!version) {
            // return the latest version of the service if theres more than 1
            return beans.findAll {
                it.namespace.equalsIgnoreCase(namespace) &&
                it.name.equalsIgnoreCase(name)
            }
                .sort()
                .last()
        }
        beans.find {
            it.namespace.equalsIgnoreCase(namespace) &&
            it.name.equalsIgnoreCase(name) &&
            it.version.equalsIgnoreCase(version)
        }
    }

    static <T extends MauroDataMapperService, P extends T> T findLatestService(Set<T> beans, String namespace, String name, String version = null) {
        if (version) {
            findService(beans, namespace, name, version)
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
