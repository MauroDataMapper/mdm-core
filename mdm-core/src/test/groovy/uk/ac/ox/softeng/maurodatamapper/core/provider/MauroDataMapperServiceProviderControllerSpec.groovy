/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.provider.email.BasicEmailProviderService
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec
import uk.ac.ox.softeng.maurodatamapper.test.unit.JsonWebUnitSpec

import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j
import io.micronaut.core.order.Ordered

import static io.micronaut.http.HttpStatus.OK

@Slf4j
class MauroDataMapperServiceProviderControllerSpec extends BaseUnitSpec
    implements ControllerUnitTest<MauroDataMapperServiceProviderController>, JsonWebUnitSpec {

    static Map<String, Set<MauroDataMapperService>> serviceProviderMap

    def setupSpec() {
        serviceProviderMap = [:]
        serviceProviderMap["${ProviderType.EMAIL.typeName}".toString()] = [new BasicEmailProviderService()] as Set
        serviceProviderMap["${ProviderType.IMPORTER.typeName}".toString()] = [] as Set
        serviceProviderMap["${ProviderType.DATALOADER.typeName}".toString()] = [] as Set
        serviceProviderMap["${ProviderType.EXPORTER.typeName}".toString()] = [] as Set
        serviceProviderMap = serviceProviderMap.sort()
    }

    def setup() {
        request.contentType = JSON_CONTENT_TYPE
        controller.mauroDataMapperServiceProviderService = Mock(MauroDataMapperServiceProviderService) {
            getProviderServicesMap() >> serviceProviderMap
            getProviderTypes() >> ProviderType.getProviderTypeNames()
            getImporterProviderServices() >> serviceProviderMap[ProviderType.IMPORTER.typeName]
            getEmailProviderServices() >> serviceProviderMap[ProviderType.EMAIL.typeName]
            getDataLoaderProviderServices() >> serviceProviderMap[ProviderType.DATALOADER.typeName]
            getExporterProviderServices() >> serviceProviderMap[ProviderType.EXPORTER.typeName]
        }
    }

    void 'test importerProviders'() {
        when:
        controller.importerProviders()

        then:
        verifyJsonResponse(OK, '[]')
    }

    void 'test dataLoaderProviders'() {
        when:
        controller.dataLoaderProviders()

        then:
        verifyJsonResponse(OK, '[]')
    }

    void 'test emailProviders'() {
        when:
        controller.emailProviders()

        then:
        verifyJsonResponse(OK, "[${emailJson}]".toString())
    }

    void 'test exporterProviders'() {
        when:
        controller.exporterProviders()

        then:
        verifyJsonResponse(OK, '[]')
    }

    String emailJson = '{' +
                       '"order": ' + Ordered.LOWEST_PRECEDENCE + ',' +
                       '"knownMetadataKeys": [],' +
                       '"displayName": "Basic Email Provider",' +
                       '"name": "BasicEmailProviderService",' +
                       '"namespace": "uk.ac.ox.softeng.maurodatamapper.core.provider.email",' +
                       '"allowsExtraMetadataKeys": true,' +
                       '"providerType": "Email",' +
                       '"version": "${json-unit.matches:version}"' +
                       '}'

}