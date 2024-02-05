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
package uk.ac.ox.softeng.maurodatamapper.dataflow.test.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlowService
import uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.DataBindDataFlowImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel

import grails.gorm.transactions.Rollback
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll

import java.nio.charset.Charset

/**
 * @since 11/01/2021
 */
@Rollback
@Slf4j
abstract class DataBindImportAndDefaultExporterServiceSpec<I extends DataBindDataFlowImporterProviderService, E extends ExporterProviderService>
    extends BaseImportExportSpec {

    @Autowired
    DataFlowService dataFlowService

    abstract I getDataFlowImporterService()

    abstract E getDataFlowExporterService()

    abstract void validateExportedModel(String testName, String exportedModel)

    DataFlow importAndConfirm(byte[] bytes) {
        def imported = dataFlowImporterService.importDataFlow(admin, bytes)

        assert imported
        log.info('Checking imported DataFlow')
        dataFlowImporterService.checkImport(admin, imported)
        check(imported)
        log.info('Saving imported DataFlow')
        assert dataFlowService.save(imported)
        sessionFactory.currentSession.flush()
        assert dataFlowService.count() == 2

        DataFlow df = DataFlow.listOrderByDateCreated().last()

        log.info('Confirming imported DataFlow')

        confirmDataFlow(df)
        df
    }

    String exportModel(UUID dataFlowId) {
        ByteArrayOutputStream byteArrayOutputStream = dataFlowExporterService.exportDomain(admin, dataFlowId)
        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset())
    }

    String importAndExport(byte[] bytes) {
        DataFlow df = importAndConfirm(bytes)
        assert df, 'Must have a dataflow imported to be able to export'
        exportModel(df.id)
    }

    void 'test empty data import export'() {
        given:
        setupData()

        when:
        exportModel(null)

        then:
        ApiInternalException exception = thrown(ApiInternalException)
        exception.errorCode == 'DFEP01'

    }

    @Unroll
    void 'test "#testName" data export'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2
        DataFlow.count() == 1

        when:
        String exported = importAndExport(loadTestFile(testName))

        then:
        validateExportedModel(testName, exported)

        where:
        testName << [
            'incSourceAndTarget',
//            'incDataClassComponents',
//            'incDataClassComponentsAndClassifiers',
//            'incMetadata',
//            'incAnnotation',
//            'sampleDataFlow'
        ]
    }

    void 'Y01: test export and import the bootstrapped DataFlow'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2
        DataFlow.count() == 1

        when:
        String exported = exportModel(dataFlowId)

        then:
        //The bootstrapped DataFlow intentionally uses non-unique labels for DataElementComponent and DataClassComponent
        //Consequently the order in which output varies from test to test. The XML validator which compares actual to expected
        //XML cannot deal with this, so we avoid making this comparison for XML.
        if (getImportType() == 'json') {
            validateExportedModel('bootstrappedDataFlow', exported)
        } else {
            true
        }

        when:
        DataFlow imported = dataFlowImporterService.importDataFlow(admin, exported.bytes)

        then:
        assert imported

        when:
        ObjectDiff diff = dataFlowService.diff(dataFlowService.get(dataFlowId), imported)

        then:
        if (!diff.objectsAreIdentical()) {
            log.error('{}', diff.toString())
        }
        diff.objectsAreIdentical()
    }

}
