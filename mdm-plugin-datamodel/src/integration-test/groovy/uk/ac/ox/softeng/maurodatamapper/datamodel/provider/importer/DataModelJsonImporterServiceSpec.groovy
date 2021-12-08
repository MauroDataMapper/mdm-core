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
package uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.summarymetadata.SummaryMetadataReport
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider.DataBindDataModelImporterProviderServiceSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j

import java.time.OffsetDateTime

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getDEVELOPMENT

/**
 * @since 15/11/2017
 */
@Integration
@Rollback
@Slf4j
class DataModelJsonImporterServiceSpec extends DataBindDataModelImporterProviderServiceSpec<DataModelJsonImporterService> {

    private static final String CANNOT_IMPORT_JSON_CODE = 'JIS03'

    DataModelJsonImporterService dataModelJsonImporterService

    @Override
    DataModelJsonImporterService getImporterService() {
        dataModelJsonImporterService
    }

    @Override
    String getImportType() {
        'json'
    }

    void 'test parameters for federation'() {
        when:
        ModelImporterProviderServiceParameters parameters = dataModelJsonImporterService.createNewImporterProviderServiceParameters()

        then:
        parameters.hasProperty('importFile')
        parameters.hasProperty('importFile').type == FileParameter
    }

    void 'IXX : test import with grandchild DC with facet'() {
        setupData()

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importModel(loadTestFile('incDataClassWithChildrenAndMetadata'))

        then:
        dm.dataClasses.find { it.label == 'parent' }.metadata.size() == 1
        dm.dataClasses.find { it.label == 'content' }.metadata.size() == 1

        when:
        Metadata md = dm.dataClasses.find { it.label == 'parent' }.metadata[0]

        then:
        md.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits'
        md.key == 'SCTSImport'
        md.value == '0.1'
        md.multiFacetAwareItemId == dm.dataClasses.find { it.label == 'parent' }.id

        when:
        md = dm.dataClasses.find { it.label == 'content' }.metadata[0]

        then:
        md.namespace == 'ox.softeng.maurodatamapper.dataloaders.cancer.audits'
        md.key == 'SCTSImport'
        md.value == '0.1'
        md.multiFacetAwareItemId == dm.dataClasses.find { it.label == 'content' }.id
    }

    void 'F01 : test import as finalised'() {
        given:
        setupData()
        basicParameters.finalised = true

        expect:
        DataModel.count() == 2

        when:
        DataModel dm = importAndConfirm(loadTestFile('simple'))

        then:
        dm
        dm.finalised
        dm.dateFinalised
        dm.modelVersion == Version.from('1')
    }

    void 'F02 : test import as finalised when already imported as finalised'() {
        given:
        setupData()
        basicParameters.finalised = true
        importAndConfirm(loadTestFile('simple'))

        when:
        importModel(loadTestFile('simple'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message == 'Request to finalise import without creating newBranchModelVersion to existing models'
    }

    void 'F03 : test import as finalised when already imported as not finalised'() {
        given:
        setupData()
        importAndConfirm(loadTestFile('simple'))

        when:
        basicParameters.finalised = true
        importModel(loadTestFile('simple'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message == 'Request to finalise import without creating newBranchModelVersion to existing models'
    }

    void 'F04 : test import as finalised with content and check BreadcrumbTree'() {
        given:
        setupData()
        basicParameters.finalised = true

        when:
        DataModel dm = importModel(loadTestFile('incDataClassWithChild'))

        then:
        dm
        dm.finalised
        dm.dateFinalised
        dm.breadcrumbTree.finalised

        and:
        dm.dataClasses
        dm.dataClasses.every { it.breadcrumbs.first().finalised }
    }

    void 'F05 : test import finalised with content and check BreadcrumbTree and date'() {
        given:
        setupData()
        basicParameters.useDefaultAuthority = false

        when:
        DataModel dm = importModel(loadTestFile('simpleFinalised'))

        then:
        dm
        dm.finalised
        dm.dateFinalised
        dm.dateFinalised == OffsetDateTime.parse('2021-01-11T14:56:15.600Z')
        dm.breadcrumbTree.finalised

        and:
        dm.dataClasses
        dm.dataClasses.every { it.breadcrumbs.first().finalised }

        and:
        dm.authority.url == 'http://localhost/other'
        dm.authority.label == 'Another Test Authority'
    }

    void 'MV01 : test import as newBranchModelVersion with no existing model'() {
        given:
        setupData()
        basicParameters.importAsNewBranchModelVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        !dm.finalised
        !dm.dateFinalised
        !dm.modelVersion
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
    }

    void 'MV02 : test import as newBranchModelVersion with existing finalised model'() {
        given:
        setupData()
        basicParameters.finalised = true
        DataModel v1 = importAndConfirm(loadTestFile('simple'))
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        !dm.finalised
        !dm.dateFinalised
        !dm.modelVersion
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        dm.versionLinks.size() == 1
        dm.versionLinks.find { it.targetModelId == v1.id }
    }

    void 'MV03 : test import as newBranchModelVersion with existing non-finalised model'() {
        given:
        setupData()
        DataModel v1 = importAndConfirm(loadTestFile('simple'))
        basicParameters.importAsNewBranchModelVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        !dm.finalised
        !dm.dateFinalised
        !dm.modelVersion
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        dm.versionLinks.size() == 1
        dm.versionLinks.find { it.targetModelId == v1.id }

        and:
        v1.finalised
        v1.modelVersion == Version.from('1')
    }

    void 'MV04 : test import as finalised and newBranchModelVersion with no existing model'() {
        given:
        setupData()
        basicParameters.finalised = true
        basicParameters.importAsNewBranchModelVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        dm.finalised
        dm.dateFinalised
        dm.modelVersion == Version.from('1')
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
    }

    void 'MV05 : test import as finalised and newBranchModelVersion with existing finalised model'() {
        given:
        setupData()
        basicParameters.finalised = true
        DataModel v1 = importAndConfirm(loadTestFile('simple'))
        basicParameters.importAsNewBranchModelVersion = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        dm.finalised
        dm.dateFinalised
        dm.modelVersion == Version.from('2')
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        dm.versionLinks.size() == 1
        dm.versionLinks.find { it.targetModelId == v1.id }
    }

    void 'MV06 : test import as finalised and newBranchModelVersion with existing non-finalised model'() {
        given:
        setupData()
        DataModel v1 = importAndConfirm(loadTestFile('simple'))
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.finalised = true

        when:
        DataModel dm = importModel(loadTestFile('simple'))

        then:
        dm
        dm.finalised
        dm.dateFinalised
        dm.modelVersion == Version.from('2')
        dm.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
        dm.versionLinks.size() == 1
        dm.versionLinks.find { it.targetModelId == v1.id }

        and:
        v1.modelVersion == Version.from('1')
        v1.finalised
    }

    void 'PG01 : test propagatingCatalogueItemElements'() {
        given:
        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        dataModel = DataModel.findById(complexDataModelId)

        Annotation testAnnotation = new Annotation(label: 'propagationTest', description: 'propagationTest', createdBy: admin.emailAddress)
        Classifier testClassifier = new Classifier(label: 'propagationTest', createdBy: admin.emailAddress).save()
        Metadata testMetadata = new Metadata(namespace: 'propagationTest', key: 'key', value: 'value', createdBy: admin.emailAddress)
        Rule testRule = new Rule(name: 'propagationTest', createdBy: admin.emailAddress).addToRuleRepresentations(language: 'e', representation:
            'a+b', createdBy: admin.emailAddress)
        SemanticLink testSemanticLink = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: admin,
                                                         targetMultiFacetAwareItem: DataClass.findByLabel('parent'))
        ReferenceFile testReferenceFile = new ReferenceFile(fileName: 'propagationTest', fileType: 'text', fileContents: 'hello'.bytes, fileSize:
            'hello'.bytes.size(), createdBy: admin.emailAddress)

        // Add summary metadata with a report to the Data Model
        SummaryMetadata dataModelSummaryMetadata = new SummaryMetadata(label: 'PropagationTest: Data Model Summary Metadata', createdBy: admin.emailAddress,
            summaryMetadataType: SummaryMetadataType.MAP)
        SummaryMetadataReport dataModelSummaryMetadataReport = new SummaryMetadataReport(
            reportDate: OffsetDateTime.now(),
            reportValue: new JsonBuilder([A: 1, B: 2]).toString(),
            createdBy: admin.emailAddress
        )
        dataModelSummaryMetadata.addToSummaryMetadataReports(dataModelSummaryMetadataReport)

        dataModel.addToAnnotations(testAnnotation)
        dataModel.addToClassifiers(testClassifier)
        dataModel.addToMetadata(testMetadata)
        dataModel.addToRules(testRule)
        dataModel.addToSemanticLinks(testSemanticLink)
        dataModel.addToReferenceFiles(testReferenceFile)
        dataModel.addToSummaryMetadata(dataModelSummaryMetadata)

        // Add summary metadata with a report to a Data Class
        DataClass parentDataClass = dataModel.childDataClasses.find {
            it.label == 'parent'
        }

        SummaryMetadata parentDataClassSummaryMetadata = new SummaryMetadata(label: 'PropagationTest: Parent Data Class Summary Metadata', createdBy: admin.emailAddress,
            summaryMetadataType: SummaryMetadataType.MAP)
        SummaryMetadataReport parentDataClassSummaryMetadataReport = new SummaryMetadataReport(
            reportDate: OffsetDateTime.now(),
            reportValue: new JsonBuilder([C: 3, D: 4]).toString(),
            createdBy: admin.emailAddress
        )
        parentDataClassSummaryMetadata.addToSummaryMetadataReports(parentDataClassSummaryMetadataReport)
        parentDataClass.addToSummaryMetadata(parentDataClassSummaryMetadata)

        // Add summary metadata with a report to a Data Element
        DataElement dataElement = parentDataClass.dataElements.find {
            it.label == 'child'
        }

        SummaryMetadata dataElementSummaryMetadata = new SummaryMetadata(label: 'PropagationTest: Data Element Summary Metadata', createdBy: admin.emailAddress,
            summaryMetadataType: SummaryMetadataType.MAP)
        SummaryMetadataReport dataElementSummaryMetadataReport = new SummaryMetadataReport(
            reportDate: OffsetDateTime.now(),
            reportValue: new JsonBuilder([E: 5, F: 6]).toString(),
            createdBy: admin.emailAddress
        )
        dataElementSummaryMetadata.addToSummaryMetadataReports(dataElementSummaryMetadataReport)
        dataElement.addToSummaryMetadata(dataElementSummaryMetadata)

        // Add summary metadata with a report to a Data Type
        PrimitiveType dataType = dataModel.dataTypes.find {
            it.label == 'string'
        }

        SummaryMetadata dataTypeSummaryMetadata = new SummaryMetadata(label: 'PropagationTest: Data Type Summary Metadata', createdBy: admin.emailAddress,
            summaryMetadataType: SummaryMetadataType.MAP)
        SummaryMetadataReport dataTypeSummaryMetadataReport = new SummaryMetadataReport(
            reportDate: OffsetDateTime.now(),
            reportValue: new JsonBuilder([G: 7, H: 8]).toString(),
            createdBy: admin.emailAddress
        )
        dataTypeSummaryMetadata.addToSummaryMetadataReports(dataTypeSummaryMetadataReport)
        dataType.addToSummaryMetadata(dataTypeSummaryMetadata)

        checkAndSave(dataModel)

        when:
        DataModel dm = importModel(loadTestFile('complexDataModel'))

        then:
        dm.annotations.find {it.label == testAnnotation.label}
        dm.classifiers.find {it.label == testClassifier.label}
        dm.metadata.find {it.namespace == testMetadata.namespace}
        dm.rules.find {it.name == testRule.name}
        dm.semanticLinks.find {it.targetMultiFacetAwareItemId == testSemanticLink.targetMultiFacetAwareItemId}
        dm.semanticLinks.find {it.multiFacetAwareItemDomainType == testSemanticLink.multiFacetAwareItemDomainType}
        dm.referenceFiles.find {it.fileName == testReferenceFile.fileName}
        dm.summaryMetadata.find {it.label == dataModelSummaryMetadata.label}
        SummaryMetadata foundDataModelSummaryMetadata = dm.summaryMetadata.find {it.label == dataModelSummaryMetadata.label}
        foundDataModelSummaryMetadata.summaryMetadataReports.size() == 1

        DataClass pdc = dm.childDataClasses.find{it.label == 'parent'}
        pdc
        pdc.summaryMetadata.find {it.label == parentDataClassSummaryMetadata.label}
        SummaryMetadata foundParentDataClassSummaryMetadata = pdc.summaryMetadata.find {it.label == parentDataClassSummaryMetadata.label}
        foundParentDataClassSummaryMetadata.summaryMetadataReports.size() == 1

        DataElement de = pdc.dataElements.find {it.label == 'child'}
        de
        de.summaryMetadata.find {it.label == dataElementSummaryMetadata.label}
        SummaryMetadata foundDataElementSummaryMetadata = de.summaryMetadata.find {it.label == dataElementSummaryMetadata.label}
        foundDataElementSummaryMetadata.summaryMetadataReports.size() == 1

        PrimitiveType dt = dm.dataTypes.find {it.label == 'string'}
        dt
        dt.summaryMetadata.find {it.label == dataTypeSummaryMetadata.label}
        SummaryMetadata foundDataTypeSummaryMetadata = dt.summaryMetadata.find {it.label == dataTypeSummaryMetadata.label}
        foundDataTypeSummaryMetadata.summaryMetadataReports.size() == 1
    }

    void 'PG02 : test importing a dataModel making sure model items arent propagated'() {
        /*
        imported data is a copy of the complexDataModel with the following alterations:
       Expect it to not propagate content
         */
        given:
        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        DataModel dataModel = DataModel.get(simpleDataModelId)
        dataModel.description = 'Some interesting thing we should preserve'
        DataClass dataClass = new DataClass(createdByUser: editor, label: 'propagation parent', dataModel: dataModel, minMultiplicity: 0,
                                            maxMultiplicity: 1)
        DataClass dataClassChild = new DataClass(createdByUser: editor, label: 'propagation child', dataModel: dataModel, minMultiplicity: 0,
                                                 maxMultiplicity: 1)
        dataModel.addToDataClasses(dataClass)
        dataClass.addToDataClasses(dataClassChild)

        DataType dataType = new PrimitiveType(createdBy: DEVELOPMENT, label: 'integer')
        dataModel.addToDataTypes(dataType)
        EnumerationType enumType = new EnumerationType(createdBy: DEVELOPMENT, label: 'yesnounknown')
            .addToEnumerationValues(key: 'Y', value: 'Yes', idx: 0)
            .addToEnumerationValues(key: 'N', value: 'No', idx: 1)
            .addToEnumerationValues(key: 'U', value: 'Unknown', idx: 2)
        dataModel.addToEnumerationTypes(enumType)
        ReferenceType refType = new ReferenceType(createdBy: DEVELOPMENT, label: 'child', referenceClass: dataClass)
        dataModel.addToReferenceTypes(refType)

        dataClass.addToDataElements(label: 'Propagation Test DataElement', createdBy: editor.emailAddress,
                                    dataModel: dataModel, dataClass: dataClass, dataType: dataType)

        checkAndSave(dataModel)

        when:
        DataModel dm = importModel(loadTestFile('simpleDataModel'))

        then:
        !dm.dataClasses.find { it.label == dataClass.label }
        dm.description == 'Some interesting thing we should preserve'
    }

    void 'PG03 : test propagating child content'() {
        given:
        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        dataModel = DataModel.findById(complexDataModelId)
        DataClass dataClass = dataModel.dataClasses.find { it.label == 'parent' }

        Annotation testAnnotation = new Annotation(label: 'propagationTest', description: 'propagationTest', createdBy: admin.emailAddress)
        Classifier testClassifier = new Classifier(label: 'propagationTest', createdBy: admin.emailAddress).save()
        Metadata testMetadata = new Metadata(namespace: 'propagationTest', key: 'key', value: 'value', createdBy: admin.emailAddress)
        Rule testRule = new Rule(name: 'propagationTest', createdBy: admin.emailAddress).addToRuleRepresentations(language: 'e', representation:
            'a+b', createdBy: admin.emailAddress)
        SemanticLink testSemanticLink = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: admin,
                                                         targetMultiFacetAwareItem: DataClass.findByLabel('child'))
        ReferenceFile testReferenceFile = new ReferenceFile(fileName: 'propagationTest', fileType: 'text', fileContents: 'hello'.bytes, fileSize:
            'hello'.bytes.size(), createdBy: admin.emailAddress)

        dataClass.addToAnnotations(testAnnotation)
        dataClass.addToClassifiers(testClassifier)
        dataClass.addToMetadata(testMetadata)
        dataClass.addToRules(testRule)
        dataClass.addToSemanticLinks(testSemanticLink)
        dataClass.addToReferenceFiles(testReferenceFile)

        checkAndSave(dataClass)

        dataClass = dataModel.dataClasses.find { it.label == 'child' }
        dataClass.description = 'Some interesting thing we should preserve'

        checkAndSave(dataClass)

        dataClass = dataModel.dataClasses.find { it.label == 'content' }
        dataClass.description = 'Some interesting thing we should lose'

        checkAndSave(dataClass)

        when:
        DataModel dm = importModel(loadTestFile('complexDataModel'))
        dataClass = dm.dataClasses.find { it.label == 'parent' }

        then:
        dataClass.metadata.find { it.namespace == testMetadata.namespace }
        dataClass.annotations.find { it.label == testAnnotation.label }
        dataClass.classifiers.find { it.label == testClassifier.label }
        dataClass.rules.find { it.name == testRule.name }
        dataClass.semanticLinks.find { it.targetMultiFacetAwareItemId == testSemanticLink.targetMultiFacetAwareItemId }
        dataClass.semanticLinks.find { it.multiFacetAwareItemDomainType == testSemanticLink.multiFacetAwareItemDomainType }
        dataClass.referenceFiles.find { it.fileName == testReferenceFile.fileName }

        when:
        dataClass = dm.dataClasses.find { it.label == 'child' }

        then:
        dataClass.description == 'Some interesting thing we should preserve'

        when:
        dataClass = dm.dataClasses.find { it.label == 'content' }

        then: 'description is not overwritten as it was included in the import'
        dataClass.description == 'A dataclass with elements'
    }

    void 'test multi-import invalid DataModel content'() {
        expect:
        importerService.canImportMultipleDomains()

        when: 'given empty content'
        importModels(''.bytes)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_EMPTY_FILE_CODE

        when: 'given an empty JSON map'
        importModels('{}'.bytes)

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        when: 'given neither models list or model map (backwards compatibility)'
        importModels(loadTestFile('exportMetadataOnly'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        when: 'given an empty model map (backwards compatibility)'
        importModels(loadTestFile('emptyDataModel'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        when: 'given an empty models list'
        importModels(loadTestFile('emptyDataModelList'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE
    }

    void 'test multi-import invalid DataModels'() {
        given:
        setupData()

        expect:
        importerService.canImportMultipleDomains()

        when: 'given an invalid model map (backwards compatibility)'
        importModels(loadTestFile('invalidDataModel'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        when: 'given a single invalid model'
        importModels(loadTestFile('invalidDataModelInList'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        when: 'given multiple invalid models'
        importModels(loadTestFile('invalidDataModels'))

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE

        // when: 'not given export metadata'
        // importModels(loadTestFile('noExportMetadata'))
        //
        // then:
        // exception = thrown(ApiBadRequestException)
        // exception.errorCode == 'TODO'
    }

    void 'test multi-import single DataModel (backwards compatibility)'() {
        given:
        setupData()
        DataModel.count() == 2
        List<DataModel> dataModels = clearExpectedDiffsFromModels([simpleDataModelId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<DataModel> imported = importModels(loadTestFile('simpleDataModel'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = dataModelService.getDiffForModels(dataModels.pop(), imported.pop())

        then:
        simpleDiff.objectsAreIdentical()
    }

    void 'test multi-import single DataModel'() {
        given:
        setupData()
        DataModel.count() == 2
        List<DataModel> dataModels = clearExpectedDiffsFromModels([simpleDataModelId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<DataModel> imported = importModels(loadTestFile('simpleDataModelInList'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = dataModelService.getDiffForModels(dataModels.pop(), imported.pop())

        then:
        simpleDiff.objectsAreIdentical()
    }

    void 'test multi-import multiple DataModels'() {
        given:
        setupData()
        DataModel.count() == 2
        List<DataModel> dataModels = clearExpectedDiffsFromModels([simpleDataModelId, complexDataModelId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<DataModel> imported = importModels(loadTestFile('simpleAndComplexDataModels'))

        then:
        imported
        imported.size() == 2

        when:
        ObjectDiff simpleDiff = dataModelService.getDiffForModels(dataModels[0], imported[0])
        ObjectDiff complexDiff = dataModelService.getDiffForModels(dataModels[1], imported[1])

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()
    }

    void 'test multi-import DataModels with invalid models'() {
        given:
        setupData()
        DataModel.count() == 2
        List<DataModel> dataModels = clearExpectedDiffsFromModels([simpleDataModelId, complexDataModelId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<DataModel> imported = importModels(loadTestFile('simpleAndInvalidDataModels'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = dataModelService.getDiffForModels(dataModels[0], imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        when:
        imported = importModels(loadTestFile('simpleComplexAndInvalidDataModels'))

        then:
        imported
        imported.size() == 2

        when:
        simpleDiff = dataModelService.getDiffForModels(dataModels[0], imported[0])
        ObjectDiff complexDiff = dataModelService.getDiffForModels(dataModels[1], imported[1])

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()
    }

    void 'test multi-import DataModels with duplicates'() {
        given:
        setupData()
        DataModel.count() == 2
        List<DataModel> dataModels = clearExpectedDiffsFromModels([simpleDataModelId, complexDataModelId])
        basicParameters.importAsNewBranchModelVersion = true // Needed to import the same models

        expect:
        importerService.canImportMultipleDomains()

        when:
        List<DataModel> imported = importModels(loadTestFile('simpleDuplicateDataModels'))

        then:
        imported
        imported.size() == 1

        when:
        ObjectDiff simpleDiff = dataModelService.getDiffForModels(dataModels[0], imported.pop())

        then:
        simpleDiff.objectsAreIdentical()

        when:
        imported = importModels(loadTestFile('simpleAndComplexDuplicateDataModels'))

        then:
        imported
        imported.size() == 2

        when:
        simpleDiff = dataModelService.getDiffForModels(dataModels[0], imported[0])
        ObjectDiff complexDiff = dataModelService.getDiffForModels(dataModels[1], imported[1])

        then:
        simpleDiff.objectsAreIdentical()
        complexDiff.objectsAreIdentical()
    }
}
