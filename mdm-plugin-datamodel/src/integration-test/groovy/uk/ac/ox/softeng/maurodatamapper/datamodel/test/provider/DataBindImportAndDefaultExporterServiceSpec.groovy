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
package uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataBindDataModelImporterProviderService

import grails.gorm.transactions.Rollback
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Stepwise
import spock.lang.Unroll

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path

/**
 * @since 15/11/2017
 */
@Rollback
@Slf4j
@Stepwise
abstract class DataBindImportAndDefaultExporterServiceSpec<I extends DataBindDataModelImporterProviderService, E extends ExporterProviderService>
    extends BaseImportExportSpec {

    @Autowired
    DataModelService dataModelService

    abstract I getImporterService()

    abstract E getExporterService()

    abstract void validateExportedModel(String testName, String exportedModel)

    DataModel importAndConfirm(byte[] bytes) {
        def imported = importerService.importDataModel(admin, bytes)

        assert imported
        imported.folder = testFolder
        log.info('Checking imported model')
        check(imported)
        log.info('Saving imported model')
        assert dataModelService.saveWithBatching(imported)
        sessionFactory.currentSession.flush()
        assert dataModelService.count() == 3

        DataModel dm = DataModel.listOrderByDateCreated().last()

        log.info('Confirming imported model')

        confirmDataModel(dm)
        dm
    }

    String exportModel(UUID dataModelId) {
        ByteArrayOutputStream byteArrayOutputStream = exporterService.exportDomain(admin, dataModelId)
        new String(byteArrayOutputStream.toByteArray(), Charset.defaultCharset())
    }

    String importAndExport(byte[] bytes) {
        DataModel dm = importAndConfirm(bytes)
        assert dm, 'Must have a datamodel imported to be able to export'
        exportModel(dm.id)
    }

    void 'test empty data import export'() {
        given:
        setupData()

        when:
        exportModel(null)

        then:
        ApiInternalException exception = thrown(ApiInternalException)
        exception.errorCode == 'DMEP01'

    }

    @Unroll
    void 'test "#testName" data export'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        String exported = importAndExport(loadTestFile(testName))

        then:
        validateExportedModel(testName, exported.replace(/Mauro Data Mapper/, 'Test Authority'))

        where:
        testName << [
            'simple',
            'incAliases',
            'incMetadata',
            'incAnnotation',
            'incSinglePrimitiveType',
            'incSinglePrimitiveTypeAndMetadata',
            'incSinglePrimitiveTypeAndAnnotation',
            'incSingleEnumerationType',
            'incSingleEnumerationTypeAndMetadata',
            'incEmptyDataClass',
            'incEmptyDataClassAndMetadata',
            'incEmptyDataClassAndAnnotation',
            'incDataClassWithChild',
            'incDataClassWithDataElement',
            'incDataClassWithChildAndSingleReferenceDataType'
        ]
    }

    @Unroll
    void 'test load datamodel with datatypes - #version'() {
        given:
        setupData()
        String exported

        expect:

        DataModel.count() == 2

        when:
        Path testFilePath = resourcesPath.resolve("${DATAMODEL_WITH_DATATYPES_FILENAME}.${version}.$importType")
        boolean test = Files.exists(testFilePath)

        if (test) {
            String xml = Files.readString(testFilePath)
                .replaceAll(/"lastUpdated": "\$\{json-unit\.matches:offsetDateTime}"/, '')
                .replaceAll(/"id": "\$\{json-unit.ignore}",/, '')
            exported = importAndExport(xml.bytes)
        } else log.info('{} does not exist, skipping', testFilePath)

        then:
        if (test) {
            validateExportedModel("${DATAMODEL_WITH_DATATYPES_FILENAME}.${exporterService.version}",
                                  exported.replace(/Mauro Data Mapper/, 'Test Authority'))
        } else true

        where:
        version << [
            '2.0',
            '3.0',
            '3.1'
        ]

    }

    @Unroll
    void 'test load complete exported datamodel from cancer audit dataloader - #version'() {
        given:
        setupData()
        String exported

        expect:
        DataModel.count() == 2

        when:
        Path testFilePath = resourcesPath.resolve("${COMPLETE_DATAMODEL_EXPORT_FILENAME}.${version}.$importType")
        boolean test = Files.exists(testFilePath)

        if (test) {
            String xml = Files.readString(testFilePath)
                .replaceAll(/"lastUpdated": "\$\{json-unit\.matches:offsetDateTime}"/, '')
                .replaceAll(/"id": "\$\{json-unit.ignore}",/, '')
            exported = importAndExport(xml.bytes)
        } else log.info('{} does not exist, skipping', testFilePath)

        then:
        if (test) {
            validateExportedModel("${COMPLETE_DATAMODEL_EXPORT_FILENAME}.${exporterService.version}",
                                  exported.replace(/Mauro Data Mapper/, 'Test Authority'))
        } else true

        where:
        version << [
            '2.0',
            '3.0'
        ]
    }

    void 'test export and import complex DataModel'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        String exported = exportModel(complexDataModelId)

        then:
        validateExportedModel('complexDataModel', exported)

        when:
        DataModel imported = importerService.importDataModel(admin, exported.bytes)

        then:
        assert imported

        when:
        imported.folder = testFolder
        ObjectDiff diff = dataModelService.diff(dataModelService.get(complexDataModelId), imported)

        then:
        if (!diff.objectsAreIdentical()) {
            log.error('{}', diff.toString())
        }
        diff.objectsAreIdentical()
    }

    void 'test export and import simple DataModel'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        String exported = exportModel(simpleDataModelId)

        then:
        validateExportedModel('simpleDataModel', exported)

        when:
        DataModel imported = importerService.importDataModel(admin, exported.bytes)

        then:
        assert imported

        and:
        imported.classifiers
        imported.classifiers.size() == 1
        imported.classifiers[0].label == 'test classifier simple'

        when:
        imported.folder = testFolder
        ObjectDiff diff = dataModelService.diff(dataModelService.get(simpleDataModelId), imported)

        then:
        diff.objectsAreIdentical()
    }

}
