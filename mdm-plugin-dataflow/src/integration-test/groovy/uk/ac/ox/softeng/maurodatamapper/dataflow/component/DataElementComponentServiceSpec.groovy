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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.dataflow.test.BaseDataFlowIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Integration
@Rollback
class DataElementComponentServiceSpec extends BaseDataFlowIntegrationSpec {

    DataElementComponentService dataElementComponentService
    DataClassComponentService dataClassComponentService

    @Override
    void setupDomainData() {
        DataClassComponent dcc = dataClassComponentService.findAllByDataFlowId(dataFlow.id, [sort: 'label']).first()
        id = dataElementComponentService.findAllByDataClassComponentId(dcc.id, [sort: 'label']).first().id
    }

    void "test get"() {
        given:
        setupData()

        expect:
        dataElementComponentService.get(id) != null
    }

    void "test list"() {
        given:
        setupData()

        when:
        List<DataElementComponent> dataElementComponents = dataElementComponentService.list(max: 2, offset: 4)

        then:
        dataElementComponents.size() == 2

        and:
        dataElementComponents[0].label == 'JOIN KEY'
        dataElementComponents[0].sourceDataElements.size() == 2
        dataElementComponents[0].targetDataElements.size() == 1

        and:
        dataElementComponents[1].label == 'Direct Copy'
        dataElementComponents[1].sourceDataElements.size() == 1
        dataElementComponents[1].targetDataElements.size() == 1
    }

    void "test count"() {
        given:
        setupData()

        expect:
        dataElementComponentService.count() == 10
    }

    void "test delete"() {
        given:
        setupData()

        expect:
        dataElementComponentService.count() == 10
        DataElementComponent dm = dataElementComponentService.get(id)

        when:
        dataElementComponentService.delete(dm)
        sessionFactory.currentSession.flush()

        then:
        dataElementComponentService.count() == 9
    }
}
