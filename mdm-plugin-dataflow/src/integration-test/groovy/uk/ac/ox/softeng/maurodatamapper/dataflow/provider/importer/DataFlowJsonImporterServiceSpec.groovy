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
package uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.dataflow.test.provider.DataBindDataFlowImporterProviderServiceSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

/**
 * @since 18/01/2021
 */
@Integration
@Rollback
@Slf4j
class DataFlowJsonImporterServiceSpec extends DataBindDataFlowImporterProviderServiceSpec<DataFlowJsonImporterService> {

    DataFlowJsonImporterService dataFlowJsonImporterService

    @Override
    DataFlowJsonImporterService getDataFlowImporterService() {
        dataFlowJsonImporterService
    }

    @Override
    String getImportType() {
        'json'
    }

    void 'test multiple DataFlow import fails'() {
        given:
        setupData()

        expect:
        !dataFlowImporterService.canImportMultipleDomains()

        when:
        dataFlowImporterService.importDataFlows(admin, loadTestFile('simple'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('cannot import multiple DataFlows')
    }
}
