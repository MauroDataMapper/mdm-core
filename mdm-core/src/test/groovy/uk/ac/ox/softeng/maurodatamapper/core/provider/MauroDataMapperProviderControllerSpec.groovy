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
package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.provider.plugin.MauroDataMapperPlugin
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.web.controllers.ControllerUnitTest

class MauroDataMapperProviderControllerSpec extends BaseUnitSpec implements ControllerUnitTest<MauroDataMapperProviderController> {

    void 'test get modules'() {
        given:
        controller.mauroDataMapperProviderService = Mock(MauroDataMapperProviderService) {
            getModulesList() >> [[getName: { -> 'test'}, getVersion: { -> '1.0'}] as MauroDataMapperPlugin,
                                 [getName: { -> 'test2'}, getVersion: { -> '0.2'}] as MauroDataMapperPlugin]
        }

        when:
        controller.modules()

        then:
        model
        model.modules[0].name == 'test'
        model.modules[0].version == '1.0'
        model.modules[1].name == 'test2'
        model.modules[1].version == '0.2'
    }

}