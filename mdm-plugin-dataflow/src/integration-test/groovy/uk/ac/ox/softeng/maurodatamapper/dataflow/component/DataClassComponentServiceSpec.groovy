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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.dataflow.test.BaseDataFlowIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Integration
@Rollback
class DataClassComponentServiceSpec extends BaseDataFlowIntegrationSpec {

    DataClassComponentService dataClassComponentService

    @Override
    void setupDomainData() {
        id = dataClassComponentService.findAllByDataFlowId(dataFlow.id, [sort: 'label']).first().id
    }

    void "test get"() {
        given:
        setupData()

        expect:
        dataClassComponentService.get(id) != null
    }

    void "test list"() {
        given:
        setupData()

        when:
        List<DataClassComponent> dataClassComponents = dataClassComponentService.list(max: 2, offset: 0)

        then:
        dataClassComponents.size() == 2

        and:
        dataClassComponents[0].label == 'aToD'
        dataClassComponents[0].definition == '''SELECT * 
INTO TargetFlowDataModel.tableD 
FROM SourceFlowDataModel.tableA'''

        and:
        dataClassComponents[1].label == 'bAndCToE'
        dataClassComponents[1].definition == '''INSERT INTO TargetFlowDataModel.tableE
SELECT  
    b.columnE1                                      AS columnE,
    b.columnF                                       AS columnR,
    CONCAT(b.columnG,'_',c.columnJ)                 AS columnS,
    CASE
        WHEN b.columnH IS NULL THEN b.columnI
        ELSE b.columnH
    END                                             AS columnT,
    TRIM(c.columnJ)                                 AS columnU,
    CONCAT(c.columnL,' ',c.columnM,'--',b.columnG)  AS columnV
FROM SourceFlowDataModel.tableB b
INNER JOIN SourceFlowDataModel.tableC c ON b.columnE1 = c.columnE2'''

    }

    void "test count"() {
        given:
        setupData()

        expect:
        dataClassComponentService.count() == 2
    }

    void "test delete"() {
        given:
        setupData()

        expect:
        dataClassComponentService.count() == 2
        DataClassComponent dm = dataClassComponentService.get(id)

        when:
        dataClassComponentService.delete(dm)
        sessionFactory.currentSession.flush()

        then:
        dataClassComponentService.count() == 1
    }
}
