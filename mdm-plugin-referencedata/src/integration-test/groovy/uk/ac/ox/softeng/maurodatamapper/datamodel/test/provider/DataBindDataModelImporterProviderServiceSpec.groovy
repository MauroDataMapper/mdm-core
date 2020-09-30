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


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.DataModelService
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.referencedatamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.DataBindDataModelImporterProviderService

import grails.gorm.transactions.Rollback
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Stepwise

/**
 * @since 15/11/2017
 */
@Rollback
@Slf4j
@SuppressWarnings("DuplicatedCode")
@Stepwise
abstract class DataBindDataModelImporterProviderServiceSpec<K extends DataBindDataModelImporterProviderService> extends BaseImportExportSpec {

    abstract K getImporterService()

    @Autowired
    DataModelService dataModelService

    DataModel importAndConfirm(byte[] bytes) {

        log.trace('Importing:\n {}', new String(bytes))

        DataModel imported = importerService.importDataModel(admin, bytes)
        assert imported
        imported.folder = testFolder
        log.debug('Check and save imported model')
        check(imported)
        dataModelService.saveWithBatching(imported)
        sessionFactory.currentSession.flush()
        assert dataModelService.count() == 3
        log.debug('DataModel saved')
        DataModel dm = dataModelService.get(imported.id)
        confirmDataModel(dm)
        dm
    }

    void 'test empty data import'() {
        given:
        setupData()

        when:
        String data = ''
        importerService.importDataModel(admin, data.bytes)

        then:
        thrown(ApiBadRequestException)

    }

    void 'test simple data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('simple'))

        then:
        !dm.annotations
        !dm.metadata
        !dm.classifiers
        !dm.dataTypes
        !dm.dataClasses

    }

    void 'test importing aliases'() {

        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incAliases'))

        then:
        dm.aliases.size() == 2
        'wibble' in dm.aliases
        'wobble' in dm.aliases
    }

    void 'test inc metadata data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incMetadata'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataTypes
        !dm.dataClasses

        and:
        dm.metadata.size() == 1

        when:
        Metadata md = dm.metadata[0]

        then:
        md.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits'
        md.key == 'SCTSImport'
        md.value == '0.1'
        md.catalogueItemId == dm.id
    }

    void 'test inc annotation data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incAnnotation'))

        then:
        !dm.metadata
        !dm.classifiers
        !dm.dataTypes
        !dm.dataClasses

        and:
        dm.annotations.size() == 1

        when:
        Annotation ann = dm.annotations[0]

        then:
        ann.description == 'http://www.datadictionary.nhs.uk/data_dictionary/attributes/a/at/attended_or_did_not_attend_de.asp?shownav=1'
        ann.label == 'Link to NHS Data Dictionary element'
        ann.catalogueItemId == dm.id
    }

    void 'test inc single primitive type data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSinglePrimitiveType'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataClasses

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(PrimitiveType)
        dataType.label == 'openworld_tick'
        (dataType as PrimitiveType).units == 'mg'
        !dataType.annotations
        !dataType.metadata

    }

    void 'test inc single primitive type with metadata data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSinglePrimitiveTypeAndMetadata'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataClasses

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(PrimitiveType)
        dataType.label == 'openworld_tick'
        !dataType.annotations

        and:
        dataType.metadata.size() == 1

        when:
        Metadata md = dataType.metadata[0]

        then:
        md.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits'
        md.key == 'SCTSImport'
        md.value == '0.1'
        md.catalogueItemId == dataType.id

    }

    void 'test inc single primitive type with annotation data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSinglePrimitiveTypeAndAnnotation'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataClasses

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(PrimitiveType)
        dataType.label == 'openworld_tick'
        !dataType.metadata

        and:
        dataType.annotations.size() == 1

        when:
        Annotation ann = dataType.annotations[0]

        then:
        ann.description == 'http://www.datadictionary.nhs.uk/data_dictionary/attributes/a/at/attended_or_did_not_attend_de.asp?shownav=1'
        ann.label == 'Link to NHS Data Dictionary element'
        ann.catalogueItemId == dataType.id

    }

    void 'test inc single enumeration type data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSingleEnumerationType'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataClasses

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(EnumerationType)
        dataType.label == 'Sex'
        ((EnumerationType) dataType).enumerationValues.size() == 2

        and:
        !dataType.annotations
        !dataType.metadata

        when:
        EnumerationValue val1 = ((EnumerationType) dataType).enumerationValues.find {it.key == 'M'}
        EnumerationValue val2 = ((EnumerationType) dataType).enumerationValues.find {it.key == 'F'}

        then:
        val1
        val2

        and:
        val1.value == 'male'
        val2.value == 'female'
        !val1.metadata
        !val2.metadata
        !val1.annotations
        !val2.annotations

    }

    void 'test inc single enumeration type with metadata data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSingleEnumerationTypeAndMetadata'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataClasses

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(EnumerationType)
        dataType.label == 'Sex'
        ((EnumerationType) dataType).enumerationValues.size() == 2

        and:
        dataType.metadata.size() == 1

        when:
        Metadata md = dataType.metadata[0]

        then:
        md.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits'
        md.key == 'SCTSImport'
        md.value == '0.1'
        md.catalogueItemId == dataType.id

        and:
        !dataType.annotations

        when:
        EnumerationValue val1 = ((EnumerationType) dataType).enumerationValues.find {it.key == 'M'}
        EnumerationValue val2 = ((EnumerationType) dataType).enumerationValues.find {it.key == 'F'}

        then:
        val1
        val2

        and:
        val1.value == 'male'
        val2.value == 'female'
        !val2.metadata
        !val1.annotations
        !val2.annotations
    }

    void 'test inc single empty dataclass data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incEmptyDataClass'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataTypes

        and:
        dm.dataClasses.size() == 1

        when:
        DataClass dataClass = dm.dataClasses[0]

        then:
        dataClass.label == 'Core'
        !dataClass.annotations
        !dataClass.metadata
        !dataClass.dataElements
        !dataClass.dataClasses

        and:
        dataClass.breadcrumbTree.domainId == dataClass.id
        dataClass.breadcrumbTree.parent.domainId == dm.id

    }

    void 'test inc single empty dataclass with metadata data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incEmptyDataClassAndMetadata'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataTypes

        and:
        dm.dataClasses.size() == 1

        when:
        DataClass dataClass = dm.dataClasses[0]

        then:
        dataClass.label == 'Core'
        !dataClass.maxMultiplicity
        !dataClass.minMultiplicity
        !dataClass.annotations
        !dataClass.dataElements
        !dataClass.dataClasses

        and:
        dataClass.metadata.size() == 1

        and:
        dataClass.breadcrumbTree.domainId == dataClass.id
        dataClass.breadcrumbTree.parent.domainId == dm.id

        when:
        Metadata md = dataClass.metadata[0]

        then:
        md.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits'
        md.key == 'SCTSImport'
        md.value == '0.1'
        md.catalogueItemId == dataClass.id

    }

    void 'test inc single empty dataclass with annotation data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incEmptyDataClassAndAnnotation'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataTypes

        and:
        dm.dataClasses.size() == 1

        when:
        DataClass dataClass = dm.dataClasses[0]

        then:
        dataClass.label == 'Core'
        !dataClass.maxMultiplicity
        !dataClass.minMultiplicity
        !dataClass.metadata
        !dataClass.dataElements
        !dataClass.dataClasses

        and:
        dataClass.breadcrumbTree.domainId == dataClass.id
        dataClass.breadcrumbTree.parent.domainId == dm.id

        and:
        dataClass.annotations.size() == 1

        when:
        Annotation ann = dataClass.annotations[0]

        then:
        ann.description == 'http://www.datadictionary.nhs.uk/data_dictionary/attributes/a/at/attended_or_did_not_attend_de.asp?shownav=1'
        ann.label == 'Link to NHS Data Dictionary element'
        ann.catalogueItemId == dataClass.id

    }

    void 'test inc single dataclass with empty child dataclass data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incDataClassWithChild'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataTypes

        when:
        DataClass dataClass = dm.dataClasses.find {it.label == 'Core'}

        then:
        dm.childDataClasses.size() == 1
        dm.dataClasses.size() == 2

        and:
        dataClass
        !dataClass.maxMultiplicity
        !dataClass.minMultiplicity
        !dataClass.annotations
        !dataClass.metadata
        !dataClass.dataElements

        and:
        dataClass.breadcrumbTree.domainId == dataClass.id
        dataClass.breadcrumbTree.parent.domainId == dm.id

        and:
        dataClass.dataClasses.size() == 1

        when:
        DataClass child = dataClass.dataClasses[0]

        then:
        child.label == 'Primary lung cancer pathological (post-op) TNM staging'
        !child.annotations
        !child.dataElements
        !child.dataClasses

        and:
        child.breadcrumbTree.domainId == child.id
        child.breadcrumbTree.parent.domainId == dataClass.id
        child.breadcrumbTree.parent.parent.domainId == dm.id

    }

    void 'test inc single dataclass with data element type data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incDataClassWithDataElement'))

        then:
        !dm.annotations
        !dm.classifiers

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(PrimitiveType)
        dataType.label == 'openworld_tick'
        !dataType.annotations
        !dataType.metadata

        when:
        DataClass dataClass = dm.dataClasses.find {it.label == 'Core'}

        then:
        dm.dataClasses.size() == 1

        and:
        dataClass
        !dataClass.maxMultiplicity
        !dataClass.minMultiplicity
        !dataClass.annotations
        !dataClass.metadata

        and:
        dataClass.breadcrumbTree.domainId == dataClass.id
        dataClass.breadcrumbTree.parent.domainId == dm.id

        and:
        dataClass.dataElements.size() == 1

        when:
        DataElement dataElement = dataClass.dataElements[0]

        then:
        dataElement.label == 'Lung Cancer Surgery'
        dataElement.description == 'Is the patient undergoing'
        dataElement.maxMultiplicity == 1
        dataElement.minMultiplicity == 1

        and:
        dataElement.metadata.size() == 2
        dataElement.metadata.every {it.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits'}
        dataElement.metadata.every {it.catalogueItemId == dataElement.id}
        dataElement.metadata.any {it.key == 'Number' && it.value == '93'}
        dataElement.metadata.any {it.key == 'Notes'}

        and:
        dataElement.dataType
        dataElement.dataType.label == 'openworld_tick'

        and:
        dataElement.breadcrumbTree.domainId == dataElement.id
        dataElement.breadcrumbTree.parent.domainId == dataClass.id
        dataElement.breadcrumbTree.parent.parent.domainId == dm.id

    }

    void 'test inc dataclass with data element and reference datatype data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incDataClassWithChildAndSingleReferenceDataType'))

        then:
        !dm.annotations
        !dm.classifiers

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(ReferenceType)
        dataType.label == 'child'
        !dataType.annotations
        !dataType.metadata
        dataType.referenceClass.label == 'child'

        when:
        DataClass dataClass = dm.dataClasses.find {it.label == 'parent'}

        then:
        dm.dataClasses.size() == 2
        dm.childDataClasses.size() == 1

        and:
        dataClass
        dataClass.maxMultiplicity == -1
        dataClass.minMultiplicity == 1
        !dataClass.annotations
        !dataClass.metadata

        and:
        dataClass.breadcrumbTree.domainId == dataClass.id
        dataClass.breadcrumbTree.parent.domainId == dm.id

        and:
        dataClass.dataClasses.size() == 1

        and:
        dataClass.dataElements.size() == 1

        when:
        DataClass child = dataClass.dataClasses[0]

        then:
        child.label == 'child'

        and:
        child.id == dataType.referenceClass.id

        and:
        child.breadcrumbTree.domainId == child.id
        child.breadcrumbTree.parent.domainId == dataClass.id
        child.breadcrumbTree.parent.parent.domainId == dm.id

        when:
        DataElement dataElement = dataClass.dataElements[0]

        then:
        dataElement.label == 'child'
        dataElement.maxMultiplicity == 1
        dataElement.minMultiplicity == 1

        and:
        dataElement.dataType
        dataElement.dataType.label == 'child'

        and:
        dataElement.breadcrumbTree.domainId == dataElement.id
        dataElement.breadcrumbTree.parent.domainId == dataClass.id
        dataElement.breadcrumbTree.parent.parent.domainId == dm.id

    }

    void 'test load datamodel with datatypes'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile("${DATAMODEL_WITH_DATATYPES_FILENAME}.${importerService.version}"))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.dataClasses

        and:
        dm.metadata.size() == 1

        when:
        Metadata md = dm.metadata[0]

        then:
        md.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits'
        md.key == 'SCTSImport'
        md.value == '0.1'

        and:
        dm.dataTypes.size() == 10
        dm.dataTypes.findAll {it.instanceOf(PrimitiveType)}.size() == 6
        dm.dataTypes.findAll {it.instanceOf(EnumerationType)}.size() == 4

        and:
        EnumerationType.count() == 5
        PrimitiveType.count() == 8
        EnumerationValue.count() == 20
    }

    void 'test load complete exported datamodel from cancer audit dataloader'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile("${COMPLETE_DATAMODEL_EXPORT_FILENAME}.${importerService.version}"))

        then:
        !dm.annotations
        !dm.classifiers

        and:
        dm.metadata.size() == 1

        when:
        Metadata md = dm.metadata[0]

        then:
        md.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits'
        md.key == 'SCTSImport'
        md.value == '0.1'

        and:
        dm.dataTypes.size() == 10
        dm.dataTypes.findAll {it.instanceOf(PrimitiveType)}.size() == 6
        dm.dataTypes.findAll {it.instanceOf(EnumerationType)}.size() == 4

        and:
        EnumerationType.count() == 5
        PrimitiveType.count() == 8
        EnumerationValue.count() == 20

        when:
        DataClass dataClass = dm.dataClasses.find {it.label == 'Core'}

        then:
        dm.childDataClasses.size() == 1
        dm.dataClasses.size() == 14

        and:
        dataClass
        !dataClass.maxMultiplicity
        !dataClass.minMultiplicity
        !dataClass.annotations
        !dataClass.metadata

        and:
        dataClass.dataClasses.size() == 12
        dataClass.dataElements.size() == 14

        when:
        DataClass child = dataClass.dataClasses.find {it.label == 'Pre-Operative primary lung cancer diagnostic staging tests'}

        then:
        child
        child.dataClasses.size() == 1

        and:
        Metadata.count() == 296
        DataClass.count() == 19
        DataElement.count() == 143
        Annotation.count() == 2
        Classifier.count() == 3
    }
}
