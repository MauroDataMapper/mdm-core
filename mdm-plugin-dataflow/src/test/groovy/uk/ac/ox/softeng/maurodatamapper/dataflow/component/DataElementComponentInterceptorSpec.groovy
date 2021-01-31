/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponent
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataElementComponent
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ContainedResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class DataElementComponentInterceptorSpec extends ContainedResourceInterceptorUnitSpec
    implements InterceptorUnitTest<DataElementComponentInterceptor> {

    def setup() {
        log.debug('Setting up DataElementComponentInterceptorSpec')
        mockDomains(DataModel, DataClass, DataClassComponent, DataElementComponent, DataFlow)
    }

    @Override
    String getControllerName() {
        'dataElementComponent'
    }

    @Override
    void setAnyInitialParams() {
        params.dataModelId = UUID.randomUUID().toString()
        params.dataFlowId = UUID.randomUUID().toString()
        params.dataClassComponentId = UUID.randomUUID().toString()
    }

    @Override
    String getExpectedExceptionCodeForNoContainingItem() {
        'DMSI01'
    }

    @Override
    void setContainingResourceParameters(String id) {
        params.dataModelId = id
    }
}
