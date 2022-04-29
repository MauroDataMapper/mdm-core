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
package uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataBindDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters

import grails.gorm.transactions.Rollback
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared

/**
 * @since 15/11/2017
 */
@Rollback
@Slf4j
@SuppressWarnings('DuplicatedCode')
abstract class DataBindDataModelImporterProviderServiceSpec<K extends DataBindDataModelImporterProviderService> extends BaseImportExportSpec {

    @Autowired
    DataModelService dataModelService

    @Shared
    DataModelFileImporterProviderServiceParameters basicParameters

    def setupSpec() {
        basicParameters = new DataModelFileImporterProviderServiceParameters().tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
        }
    }

    abstract K getImporterService()

    void cleanupParameters() {
        basicParameters.tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
            propagateFromPreviousVersion = false
        }
    }

    DataModel importModel(byte[] bytes) {
        log.trace('Importing:\n {}', new String(bytes))
        basicParameters.importFile = new FileParameter(fileContents: bytes)

        DataModel imported = importerService.importDomain(admin, basicParameters) as DataModel
        imported.folder = testFolder
        check(imported)
        dataModelService.saveModelWithContent(imported)
        sessionFactory.currentSession.flush()
        log.debug('DataModel saved')
        dataModelService.get(imported.id)
    }

    List<DataModel> importModels(byte[] bytes) {
        log.trace('Importing:\n {}', new String(bytes))
        basicParameters.importFile = new FileParameter(fileContents: bytes)

        List<DataModel> imported = importerService.importDomains(admin, basicParameters) as List<DataModel>
        imported.each {
            it.folder = testFolder
            check(it)
            dataModelService.saveModelWithContent(it)
        }
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        log.debug('DataModels saved')
        imported.collect { dataModelService.get(it.id) }
    }

    DataModel importAndConfirm(byte[] bytes) {
        DataModel dm = importModel(bytes)
        assert dataModelService.count() == 3
        confirmDataModel(dm)
        dm
    }

    DataModel importNoConfirm(byte[] bytes) {
        DataModel dm = importModel(bytes)
        //assert dataModelService.count() == 3
        //confirmDataModel(dm)
        dm
    }

    List<DataModel> clearExpectedDiffsFromModels(List<UUID> modelIds) {
        // Rules are not imported/exported and will therefore exist as diffs
        Closure<Boolean> removeRule = {it.rules?.removeIf {rule -> rule.name == 'Bootstrapped Functional Test Rule'}}
        List<DataModel> dataModels = modelIds.collect {
            DataModel dataModel = dataModelService.get(it)
            removeRule(dataModel)
            ['dataClasses', 'allDataElements', 'dataTypes'].each {
                dataModel.getProperty(it)?.each(removeRule)
            }
            dataModel
        }
        sessionFactory.currentSession.clear()
        dataModels
    }

    void 'I01 : test empty data import'() {
        given:
        setupData()

        when:
        String data = ''
        importerService.importDataModel(admin, data.bytes)

        then:
        thrown(ApiBadRequestException)
    }

    void 'I02 : test simple data import'() {
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
        !dm.summaryMetadata
        !dm.dataTypes
        !dm.dataClasses
    }

    void 'I03 : test inc classifiers import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incClassifiers'))

        then:
        !dm.annotations
        !dm.metadata
        dm.classifiers.size() == 2
        !dm.summaryMetadata
        !dm.dataTypes
        !dm.dataClasses
    }

    void 'I04 : test importing aliases'() {
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

    void 'I05 : test inc metadata data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incMetadata'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
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
        md.multiFacetAwareItemId == dm.id
    }

    void 'I06 : test inc annotation data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incAnnotation'))

        then:
        !dm.metadata
        !dm.classifiers
        !dm.summaryMetadata
        !dm.dataTypes
        !dm.dataClasses

        and:
        dm.annotations.size() == 1

        when:
        Annotation ann = dm.annotations[0]

        then:
        ann.description == 'http://www.datadictionary.nhs.uk/data_dictionary/attributes/a/at/attended_or_did_not_attend_de.asp?shownav=1'
        ann.label == 'Link to NHS Data Dictionary element'
        ann.multiFacetAwareItemId == dm.id
    }

    void 'I07 : test inc single primitive type data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSinglePrimitiveType'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
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
        !dataType.summaryMetadata
    }

    void 'I07a : test inc single primitive type with newline data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSinglePrimitiveTypeWithNewline'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
        !dm.dataClasses

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then: 'the \n is replaced with a space'
        dataType.instanceOf(PrimitiveType)
        dataType.label == 'openworld tick'
        (dataType as PrimitiveType).units == 'mg'
        !dataType.annotations
        !dataType.metadata
        !dataType.summaryMetadata
    }

    void 'I08 : test inc single primitive type with metadata data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSinglePrimitiveTypeAndMetadata'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
        !dm.dataClasses

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(PrimitiveType)
        dataType.label == 'openworld_tick'
        !dataType.annotations
        !dataType.summaryMetadata

        and:
        dataType.metadata.size() == 1

        when:
        Metadata md = dataType.metadata[0]

        then:
        md.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits'
        md.key == 'SCTSImport'
        md.value == '0.1'
        md.multiFacetAwareItemId == dataType.id
    }

    void 'I09 : test inc single primitive type with annotation data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSinglePrimitiveTypeAndAnnotation'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
        !dm.dataClasses

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(PrimitiveType)
        dataType.label == 'openworld_tick'
        !dataType.metadata
        !dataType.summaryMetadata

        and:
        dataType.annotations.size() == 1

        when:
        Annotation ann = dataType.annotations[0]

        then:
        ann.description == 'http://www.datadictionary.nhs.uk/data_dictionary/attributes/a/at/attended_or_did_not_attend_de.asp?shownav=1'
        ann.label == 'Link to NHS Data Dictionary element'
        ann.multiFacetAwareItemId == dataType.id
    }

    void 'I10 : test inc single enumeration type data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSingleEnumerationType'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
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
        !dataType.summaryMetadata

        when:
        EnumerationValue val1 = ((EnumerationType) dataType).enumerationValues.find { it.key == 'M' }
        EnumerationValue val2 = ((EnumerationType) dataType).enumerationValues.find { it.key == 'F' }

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
        val1.breadcrumbTree.domainId
        val2.breadcrumbTree.domainId

    }

    void 'I11 : test inc single enumeration type with metadata data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSingleEnumerationTypeAndMetadata'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
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
        md.multiFacetAwareItemId == dataType.id

        and:
        !dataType.annotations
        !dataType.summaryMetadata

        when:
        EnumerationValue val1 = ((EnumerationType) dataType).enumerationValues.find { it.key == 'M' }
        EnumerationValue val2 = ((EnumerationType) dataType).enumerationValues.find { it.key == 'F' }

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

    void 'I12 : test inc single empty dataclass data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incEmptyDataClass'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
        !dm.dataTypes

        and:
        dm.dataClasses.size() == 1

        when:
        DataClass dataClass = dm.dataClasses[0]

        then:
        dataClass.label == 'Core'
        !dataClass.annotations
        !dataClass.metadata
        !dataClass.summaryMetadata
        !dataClass.dataElements
        !dataClass.dataClasses

        and:
        dataClass.breadcrumbTree.domainId == dataClass.id
        dataClass.breadcrumbTree.parent.domainId == dm.id
    }

    void 'I13 : test inc single empty dataclass with metadata data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incEmptyDataClassAndMetadata'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
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
        !dataClass.summaryMetadata
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
        md.multiFacetAwareItemId == dataClass.id
    }

    void 'I14 : test inc single empty dataclass with annotation data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incEmptyDataClassAndAnnotation'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
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
        !dataClass.summaryMetadata
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
        ann.multiFacetAwareItemId == dataClass.id
    }

    void 'I15 : test inc single dataclass with empty child dataclass data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incDataClassWithChild'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
        !dm.dataTypes

        when:
        DataClass dataClass = dm.dataClasses.find { it.label == 'Core' }

        then:
        dm.childDataClasses.size() == 1
        dm.dataClasses.size() == 2

        and:
        dataClass
        !dataClass.maxMultiplicity
        !dataClass.minMultiplicity
        !dataClass.annotations
        !dataClass.metadata
        !dataClass.summaryMetadata
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
        !child.summaryMetadata
        !child.dataElements
        !child.dataClasses

        and:
        child.breadcrumbTree.domainId == child.id
        child.breadcrumbTree.parent.domainId == dataClass.id
        child.breadcrumbTree.parent.parent.domainId == dm.id
    }

    void 'I16 : test inc single dataclass with data element type data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incDataClassWithDataElement'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(PrimitiveType)
        dataType.label == 'openworld_tick'
        !dataType.annotations
        !dataType.metadata
        !dataType.summaryMetadata

        when:
        DataClass dataClass = dm.dataClasses.find { it.label == 'Core' }

        then:
        dm.dataClasses.size() == 1

        and:
        dataClass
        !dataClass.maxMultiplicity
        !dataClass.minMultiplicity
        !dataClass.annotations
        !dataClass.metadata
        !dataClass.summaryMetadata

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
        !dataElement.summaryMetadata

        and:
        dataElement.metadata.size() == 2
        dataElement.metadata.every { it.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits' }
        dataElement.metadata.every { it.multiFacetAwareItemId == dataElement.id }
        dataElement.metadata.any { it.key == 'Number' && it.value == '93' }
        dataElement.metadata.any { it.key == 'Notes' }

        and:
        dataElement.dataType
        dataElement.dataType.label == 'openworld_tick'

        and:
        dataElement.breadcrumbTree.domainId == dataElement.id
        dataElement.breadcrumbTree.parent.domainId == dataClass.id
        dataElement.breadcrumbTree.parent.parent.domainId == dm.id
    }

    void 'I17 : test inc dataclass with data element and reference datatype data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incDataClassWithChildAndSingleReferenceDataType'))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata

        and:
        dm.dataTypes.size() == 1

        when:
        DataType dataType = dm.dataTypes[0]

        then:
        dataType.instanceOf(ReferenceType)
        dataType.label == 'child'
        !dataType.annotations
        !dataType.metadata
        !dataType.summaryMetadata
        dataType.referenceClass.label == 'child'

        when:
        DataClass dataClass = dm.dataClasses.find { it.label == 'parent' }

        then:
        dm.dataClasses.size() == 2
        dm.childDataClasses.size() == 1

        and:
        dataClass
        dataClass.maxMultiplicity == -1
        dataClass.minMultiplicity == 1
        !dataClass.annotations
        !dataClass.metadata
        !dataClass.summaryMetadata

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

    void 'I18 : test inc summary metadata without summary metadata reports data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSummaryMetadataWithoutReports'))

        then:
        !dm.metadata
        !dm.annotations
        !dm.classifiers

        and:
        dm.summaryMetadata.size() == 1

        when:
        SummaryMetadata dmSm = dm.summaryMetadata[0]

        then:
        dmSm.label == 'Heads and Tails'
        dmSm.description == 'Instance Count'
        dmSm.summaryMetadataType == SummaryMetadataType.NUMBER

        and:
        !dmSm.summaryMetadataReports

        when:
        DataClass dataClass = dm.dataClasses.find {it.label == 'Heads and Tails'}

        then:
        dm.dataClasses.size() == 1
        dm.childDataClasses.size() == 1

        and:
        dataClass
        !dataClass.maxMultiplicity
        !dataClass.minMultiplicity
        !dataClass.annotations
        !dataClass.metadata

        and:
        dataClass.summaryMetadata.size() == 1

        when:
        SummaryMetadata dcSm = dataClass.summaryMetadata[0]

        then:
        dcSm.label == 'Coin'
        dcSm.description == 'Value Distribution'
        dcSm.summaryMetadataType == SummaryMetadataType.MAP

        and:
        !dcSm.summaryMetadataReports

        when:
        DataElement dataElement = dataClass.dataElements.find {it.label == 'Coin'}

        then:
        dataClass.dataElements.size() == 1

        and:
        dataElement
        dataElement.maxMultiplicity == 1
        dataElement.minMultiplicity == 1
        !dataElement.annotations
        !dataElement.metadata
        dataElement.dataType

        and:
        dataElement.summaryMetadata.size() == 1

        when:
        SummaryMetadata deSm = dataElement.summaryMetadata[0]

        then:
        deSm.label == 'Coin'
        deSm.description == 'Value Distribution'
        deSm.summaryMetadataType == SummaryMetadataType.MAP

        and:
        !deSm.summaryMetadataReports

        when:
        DataType dataType = dataElement.dataType

        then:
        dm.dataTypes.size() == 1
        dataType.id == dm.dataTypes.find {it.label == 'Coin'}.id

        and:
        !dataType.annotations
        !dataType.metadata
        dataType.instanceOf(EnumerationType)
        dataType.enumerationValues.size() == 2
        dataType.summaryMetadata.size() == 1

        when:
        SummaryMetadata dtSm = dataType.summaryMetadata[0]

        then:
        dtSm.label == 'Coin'
        dtSm.description == 'Value Distribution'
        dtSm.summaryMetadataType == SummaryMetadataType.MAP

        and:
        !dtSm.summaryMetadataReports
    }

    void 'I19 : test inc summary metadata with summary metadata reports data import'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('incSummaryMetadataWithReports'))

        then:
        !dm.annotations
        !dm.classifiers

        and:
        dm.summaryMetadata.size() == 1

        when:
        SummaryMetadata dmSm = dm.summaryMetadata[0]

        then:
        dmSm.label == 'Heads and Tails'
        dmSm.description == 'Instance Count'
        dmSm.summaryMetadataType == SummaryMetadataType.NUMBER

        and:
        dmSm.summaryMetadataReports.size() == 1
        dmSm.summaryMetadataReports[0].reportDate
        dmSm.summaryMetadataReports[0].reportValue == '27'

        when:
        DataClass dataClass = dm.dataClasses.find {it.label == 'Heads and Tails'}

        then:
        dm.dataClasses.size() == 1
        dm.childDataClasses.size() == 1

        and:
        dataClass
        !dataClass.maxMultiplicity
        !dataClass.minMultiplicity
        !dataClass.annotations
        !dataClass.metadata

        and:
        dataClass.summaryMetadata.size() == 1

        when:
        SummaryMetadata dcSm = dataClass.summaryMetadata[0]

        then:
        dcSm.label == 'Coin'
        dcSm.description == 'Value Distribution'
        dcSm.summaryMetadataType == SummaryMetadataType.MAP

        and:
        dcSm.summaryMetadataReports.size() == 1
        dcSm.summaryMetadataReports[0].reportDate
        dcSm.summaryMetadataReports[0].reportValue == '{"False":17,"True":10}'

        when:
        DataElement dataElement = dataClass.dataElements.find {it.label == 'Coin'}

        then:
        dataClass.dataElements.size() == 1

        and:
        dataElement
        dataElement.maxMultiplicity == 1
        dataElement.minMultiplicity == 1
        !dataElement.annotations
        !dataElement.metadata
        dataElement.dataType

        and:
        dataElement.summaryMetadata.size() == 1

        when:
        SummaryMetadata deSm = dataElement.summaryMetadata[0]

        then:
        deSm.label == 'Coin'
        deSm.description == 'Value Distribution'
        deSm.summaryMetadataType == SummaryMetadataType.MAP

        and:
        deSm.summaryMetadataReports.size() == 1
        deSm.summaryMetadataReports[0].reportDate
        deSm.summaryMetadataReports[0].reportValue == '{"False":17,"True":10}'

        when:
        DataType dataType = dataElement.dataType

        then:
        dm.dataTypes.size() == 1
        dataType.id == dm.dataTypes.find {it.label == 'Coin'}.id

        and:
        !dataType.annotations
        !dataType.metadata
        dataType.instanceOf(EnumerationType)
        dataType.enumerationValues.size() == 2
        dataType.summaryMetadata.size() == 1

        when:
        SummaryMetadata dtSm = dataType.summaryMetadata[0]

        then:
        dtSm.label == 'Coin'
        dtSm.description == 'Value Distribution'
        dtSm.summaryMetadataType == SummaryMetadataType.MAP

        and:
        dtSm.summaryMetadataReports.size() == 1
        dtSm.summaryMetadataReports[0].reportDate
        dtSm.summaryMetadataReports[0].reportValue == '{"False":17,"True":10}'
    }

    void 'I20 : test load datamodel with datatypes'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile("${DATAMODEL_WITH_DATATYPES_FILENAME}.${importerService.version}"))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata
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
        dm.dataTypes.findAll { it.instanceOf(PrimitiveType) }.size() == 6
        dm.dataTypes.findAll { it.instanceOf(EnumerationType) }.size() == 4

        and:
        EnumerationType.count() == 5
        PrimitiveType.count() == 8
        EnumerationValue.count() == 20
    }

    void 'I21 : test load complete exported datamodel from cancer audit dataloader'() {
        given:
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile("${COMPLETE_DATAMODEL_EXPORT_FILENAME}.${importerService.version}"))

        then:
        !dm.annotations
        !dm.classifiers
        !dm.summaryMetadata

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
        dm.dataTypes.findAll { it.instanceOf(PrimitiveType) }.size() == 6
        dm.dataTypes.findAll { it.instanceOf(EnumerationType) }.size() == 4

        and:
        EnumerationType.count() == 5
        PrimitiveType.count() == 8
        EnumerationValue.count() == 20

        when:
        DataClass dataClass = dm.dataClasses.find { it.label == 'Core' }

        then:
        dm.childDataClasses.size() == 1
        dm.dataClasses.size() == 14

        and:
        dataClass
        !dataClass.maxMultiplicity
        !dataClass.minMultiplicity
        !dataClass.annotations
        !dataClass.metadata
        !dataClass.summaryMetadata

        and:
        dataClass.dataClasses.size() == 12
        dataClass.dataElements.size() == 14

        when:
        DataClass child = dataClass.dataClasses.find { it.label == 'Pre-Operative primary lung cancer diagnostic staging tests' }

        then:
        child
        child.dataClasses.size() == 1

        and:
        Metadata.count() == 298
        DataClass.count() == 19
        DataElement.count() == 143
        Annotation.count() == 2
        Classifier.count() == 3
    }
}
