/*
 * Copyright 2020 University of Oxford
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

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import grails.testing.mixin.integration.Integration
import org.springframework.context.MessageSource

/**
 * @since 13/10/2017
 * TODO xmlnamespacesandversions & dm importers
 */
@Integration
class MauroDataMapperServiceProviderServiceSpec extends MdmSpecification {

    MessageSource messageSource

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService

    void 'test getting plugin map'() {
        when:
        def map = mauroDataMapperServiceProviderService.providerServicesMap

        then: 'should be same size as the types of plugins'
        map.size() == ProviderType.providerTypeNames.size()

        and:
        map.values().flatten().size() == mauroDataMapperServiceProviderService.providerServices.size()

        and:
        map.values().flatten().each {p ->
            mauroDataMapperServiceProviderService.providerServices.any {it == p}
        }
    }

    void 'test email plugins'() {
        when:
        def results = mauroDataMapperServiceProviderService.emailProviderServices

        then: 'we have 1 default email plugin'
        results.size() == 1

        and:
        results[0] == mauroDataMapperServiceProviderService.emailProvider

        and:
        mauroDataMapperServiceProviderService.findEmailProvider('uk.ac.ox.softeng.maurodatamapper.core.provider.email',
                                                                'BasicEmailProviderService',
                                                                '1.0')
    }

    void 'test dataLoaders'() {
        expect: 'no default dataLoaders'
        mauroDataMapperServiceProviderService.dataLoaderProviderServices.size() == 0
    }

    void 'test importers'() {
        expect: 'no default importers'
        mauroDataMapperServiceProviderService.dataLoaderProviderServices.size() == 0
        /*
        expect:
        mauroDataMapperServiceProvidingService.importerPlugins.size() == 3

        and:
        mauroDataMapperServiceProvidingService.findImporter('ox.softeng.maurodatamapper.core.spi.json',
                                                    'JsonImporterService',
                                                    '1.1')

        and:
        mauroDataMapperServiceProvidingService.findImporter('ox.softeng.maurodatamapper.core.spi.xml',
                                                    'XmlImporterService',
                                                    dataModelNamespaceVersion)
                                                    *
         */
    }

    void 'test exporters'() {
        expect: 'no default exporters'
        mauroDataMapperServiceProviderService.dataLoaderProviderServices.size() == 0
        /*
        expect:
        mauroDataMapperServiceProvidingService.exporterPlugins.size() == 3

        and:
        mauroDataMapperServiceProvidingService.findExporter('ox.softeng.maurodatamapper.core.spi.json',
                                                    'JsonExporterService',
                                                    '1.1')

        and:
        mauroDataMapperServiceProvidingService.findExporter('ox.softeng.maurodatamapper.core.spi.xml',
                                                    'XmlExporterService',
                                                    exportNamespaceVersion)

        //        and:
        //        mauroDataMapperServiceProvidingService.findExporter('ox.softeng.maurodatamapper.spi.msword',
        //                                                          'MicrosoftWordExporter',
        //                                                          '0.1')
         */
    }

    /*
        void 'test DataModel importers'() {
            expect:
            mauroDataMapperServiceProvidingService.dataModelImporterPlugins.size() == 2

            and:
            mauroDataMapperServiceProvidingService.findDataModelImporter('ox.softeng.maurodatamapper.core.spi.json',
                                                                 'JsonImporterService',
                                                                 '1.1')

            and:
            mauroDataMapperServiceProvidingService.findDataModelImporter('ox.softeng.maurodatamapper.core.spi.xml',
                                                                 'XmlImporterService',
                                                                 dataModelNamespaceVersion)
        }

        void 'test DataModel exporters'() {
            expect:
            mauroDataMapperServiceProvidingService.dataModelExporterPlugins.size() == 2

            and:
            mauroDataMapperServiceProvidingService.findDataModelExporter('ox.softeng.maurodatamapper.core.spi.json',
                                                                 'JsonExporterService',
                                                                 '1.1')

            and:
            mauroDataMapperServiceProvidingService.findDataModelExporter('ox.softeng.maurodatamapper.core.spi.xml',
                                                                 'XmlExporterService',
                                                                 exportNamespaceVersion)
        }
     */
}
