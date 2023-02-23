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
package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import grails.core.GrailsApplication
import grails.testing.mixin.integration.Integration
import org.springframework.context.MessageSource

/**
 * @since 13/10/2017
 */
@Integration
class MauroDataMapperProviderServiceSpec extends MdmSpecification {

    MessageSource messageSource
    GrailsApplication grailsApplication
    MauroDataMapperProviderService mauroDataMapperProviderService

    void 'test modules'() {
        expect:
        mauroDataMapperProviderService.modulesList.size() == 85
        mauroDataMapperProviderService.javaModules.size() == 62
        mauroDataMapperProviderService.allGrailsPluginModules.size() == 22
        mauroDataMapperProviderService.grailsPluginModules.size() == 21
        mauroDataMapperProviderService.mdmGrailsPluginModules.size() == 1
        mauroDataMapperProviderService.otherModules.size() == 1

        and:
        mauroDataMapperProviderService.findModule('mdm.core', grailsApplication.metadata.getApplicationVersion())
        mauroDataMapperProviderService.findModule('mdm.common', grailsApplication.metadata.getApplicationVersion())
    }
}
