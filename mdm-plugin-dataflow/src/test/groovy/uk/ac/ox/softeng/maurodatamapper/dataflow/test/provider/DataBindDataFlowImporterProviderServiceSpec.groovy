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
package uk.ac.ox.softeng.maurodatamapper.dataflow.test.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlowService
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponent
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataElementComponent
import uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.DataBindDataFlowImporterProviderService

import grails.gorm.transactions.Rollback
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 11/01/2021
 */
@Rollback
@Slf4j
@SuppressWarnings("DuplicatedCode")
abstract class DataBindDataFlowImporterProviderServiceSpec<K extends DataBindDataFlowImporterProviderService> extends BaseImportExportSpec {

    abstract K getDataFlowImporterService()

    @Autowired
    DataFlowService dataFlowService

    DataFlow importAndConfirm(byte[] bytes) {

        log.trace('Importing:\n {}', new String(bytes))

        DataFlow imported = dataFlowImporterService.importDataFlow(admin, bytes)
        assert imported

        dataFlowImporterService.checkImport(admin, imported)
        check(imported)
        
        dataFlowService.save(imported)
        sessionFactory.currentSession.flush()
        assert dataFlowService.count() == 2
        log.debug('DataFlow saved')
        DataFlow df = dataFlowService.get(imported.id)
        confirmDataFlow(df)
        df
    }

    void 'I01 : test empty data import'() {
        given:
        setupData()

        when:
        String data = ''
        dataFlowImporterService.importDataFlow(admin, data.bytes)

        then:
        thrown(ApiBadRequestException)

    }

    void 'I02 : test simple data import'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('simple'))

        then:
        DataFlow.count() == 2
        !df.annotations
        !df.metadata
        !df.classifiers
        !df.dataClassComponents
    }

    void 'I03 : test inc classifiers import'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('incClassifiers'))

        then:
        DataFlow.count() == 2
        !df.annotations
        !df.metadata
        df.classifiers.size() == 2
        !df.dataClassComponents
    }

    void 'I04 : test importing aliases'() {

        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('incAliases'))

        then:
        DataFlow.count() == 2
        df.aliases.size() == 2
        'wibble' in df.aliases
        'wobble' in df.aliases
    }

    void 'I05 : test inc metadata data import'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('incMetadata'))

        then:
        DataFlow.count() == 2
        !df.annotations
        !df.classifiers
        !df.dataClassComponents

        and:
        df.metadata.size() == 1

        when:
        Metadata md = df.metadata[0]

        then:
        md.namespace == 'Metadata namespace'
        md.key == 'Metadata key'
        md.value == 'Metadata value'
        md.multiFacetAwareItemId == df.id
    }

    void 'I06 : test inc annotation data import'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('incAnnotation'))

        then:
        DataFlow.count() == 2
        df.annotations.size() == 1
        !df.metadata
        !df.classifiers

        when:
        Annotation ann = df.annotations[0]

        then:
        ann.description == 'Annotation description'
        ann.label == 'Annotation label'
        ann.multiFacetAwareItemId == df.id
    }

    void 'I07 : test inc data class components and classifiers import'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('incDataClassComponentsAndClassifiers'))

        then:
        DataFlow.count() == 2
        !df.annotations
        df.classifiers.size() == 2
        df.dataClassComponents.size() == 1

        when:
        DataClassComponent dataClassComponent = df.dataClassComponents[0]

        then:
        dataClassComponent.label == 'Label of data class component'
        dataClassComponent.definition == 'Definition of data class component'
        !dataClassComponent.annotations
        !dataClassComponent.metadata
        dataClassComponent.classifiers.size() == 2
        dataClassComponent.dataElementComponents.size() == 1

        when:
        DataElementComponent dataElementComponent = dataClassComponent.dataElementComponents[0]

        then:
        dataElementComponent.label == 'First data element component label'
        dataElementComponent.definition == 'First data element component definition'
        !dataElementComponent.annotations
        !dataElementComponent.metadata
        dataElementComponent.classifiers.size() == 2

    }

    void 'I08 : test import with failed pathing on source model'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('invalidSource'))

        then:
        DataFlow.count() == 1
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('Source DataModel retrieval for dm:Invalid SourceFlowDataModel$main failed')
    }

    void 'I09 : test import with failed pathing on target model'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('invalidTarget'))

        then:
        DataFlow.count() == 1
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('Target DataModel retrieval for dm:Invalid TargetFlowDataModel$main failed')
    }    

    void 'I10 : test import with failed pathing on a source dataclass'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('invalidSourceDataClass'))

        then:
        DataFlow.count() == 1
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('Source DataClass retrieval for dm:SourceFlowDataModel$main|dc:Invalid tableB failed')
    }

    void 'I11 : test import with failed pathing on a target dataclass'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('invalidTargetDataClass'))

        then:
        DataFlow.count() == 1
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('Target DataClass retrieval for dm:TargetFlowDataModel$main|dc:Invalid tableD failed')
    }

    void 'I12 : test import with failed pathing on a source dataelement'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('invalidSourceDataElement1'))

        then:
        DataFlow.count() == 1
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('Source DataElement retrieval for dm:SourceFlowDataModel$main|dc:tableA|de:Invalid columnA failed')
    }     

    void 'I13 : test import with failed pathing on a source dataelement when the dataclass label is wrong'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('invalidSourceDataElement2'))

        then:
        DataFlow.count() == 1
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('Source DataElement retrieval for dm:SourceFlowDataModel$main|dc:Invalid tableA|de:columnA failed')
    }   

    void 'I14 : test import with failed pathing on a target dataelement'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('invalidTargetDataElement1'))

        then:
        DataFlow.count() == 1
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('Target DataElement retrieval for dm:TargetFlowDataModel$main|dc:tableD|de:Invalid columnN failed')
    }     

    void 'I15 : test import with failed pathing on a target dataelement when the dataclass label is wrong'() {
        given:
        setupData()

        expect:
        DataFlow.count() == 1

        when:
        DataFlow df = importAndConfirm(loadTestFile('invalidTargetDataElement2'))

        then:
        DataFlow.count() == 1
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('Target DataElement retrieval for dm:TargetFlowDataModel$main|dc:Invalid tableD|de:columnN failed')
    }          

}
