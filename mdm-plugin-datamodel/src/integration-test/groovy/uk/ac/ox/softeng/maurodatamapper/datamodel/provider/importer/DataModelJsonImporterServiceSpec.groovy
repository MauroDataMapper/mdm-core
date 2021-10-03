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
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.provider.DataBindDataModelImporterProviderServiceSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration

import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getDEVELOPMENT

/**
 * @since 15/11/2017
 */
@Integration
@Rollback
@Slf4j
class DataModelJsonImporterServiceSpec extends DataBindDataModelImporterProviderServiceSpec<DataModelJsonImporterService> {

    DataModelJsonImporterService dataModelJsonImporterService

    @Override
    DataModelJsonImporterService getImporterService() {
        dataModelJsonImporterService
    }

    @Override
    String getImportType() {
        'json'
    }

    void 'test multiple DataModel import fails'() {
        given:
        setupData()

        expect:
        !importerService.canImportMultipleDomains()

        when:
        importerService.importDataModels(admin, loadTestFile('simple'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('cannot import multiple DataModels')
    }

    void 'test parameters for federation'() {
        when:
        ModelImporterProviderServiceParameters parameters = dataModelJsonImporterService.createNewImporterProviderServiceParameters()

        then:
        parameters.hasProperty('importFile')
        parameters.hasProperty('importFile').type == FileParameter
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

        cleanup:
        basicParameters.finalised = false

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

        cleanup:
        basicParameters.finalised = false

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

        cleanup:
        basicParameters.finalised = false
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

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false

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

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false

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

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false

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

        cleanup:
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = false

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

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
        basicParameters.finalised = false

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

        cleanup:
        basicParameters.importAsNewBranchModelVersion = false
        basicParameters.finalised = false

    }

    void 'PG01 test propagatingCatalogueItemElements'() {

        given:
        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        dataModel = DataModel.findById(complexDataModelId)

        Annotation testAnnotation = new Annotation(label: 'propagationTest', description: 'propagationTest', createdBy: admin.emailAddress)
        Classifier testClassifier = new Classifier(label: 'propagationTest', createdBy: admin.emailAddress)
        Metadata testMetadata = new Metadata(namespace: 'propagationTest', key: 'key', value: 'value', createdBy: admin.emailAddress)
        Rule testRule = new Rule(name: 'propagationTest', createdBy: admin.emailAddress).addToRuleRepresentations(language: 'e', representation:
            'a+b', createdBy: admin.emailAddress)
        SemanticLink testSemanticLink = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: admin,
                                                         targetMultiFacetAwareItem: DataClass.findByLabel('parent'))
        ReferenceFile testReferenceFile = new ReferenceFile(fileName: 'propagationTest', fileType: 'text', fileContents: 'hello'.bytes, fileSize:
            'hello'.bytes.size(), createdBy: admin.emailAddress)

        dataModel.addToAnnotations(testAnnotation)
        dataModel.addToClassifiers(testClassifier)
        dataModel.addToMetadata(testMetadata)
        dataModel.addToRules(testRule)
        dataModel.addToSemanticLinks(testSemanticLink)
        dataModel.addToReferenceFiles(testReferenceFile)

        dataModelService.saveModelNewContentOnly(dataModel)

        when:
        DataModel dm = importModel(loadTestFile('complexDataModel'))

        then:
        dm.annotations.find { it.label == testAnnotation.label }
        dm.classifiers.find { it.label == testClassifier.label }
        dm.metadata.find { it.namespace == testMetadata.namespace }
        dm.rules.find { it.name == testRule.name }
        dm.semanticLinks.find { it.targetMultiFacetAwareItemId == testSemanticLink.targetMultiFacetAwareItemId }
        dm.semanticLinks.find { it.multiFacetAwareItemDomainType == testSemanticLink.multiFacetAwareItemDomainType }
        dm.referenceFiles.find { it.fileName == testReferenceFile.fileName }


    }

    void 'PG02 : test importing a dataModel and propagating existing information from the already present version'() {
        /*
        imported data is a copy of the complexDataModel with the following alterations:
        Expect it to propagate information from complex data model where it has been altered or removed.

        expected propagated elements:
        dataClass: "dataClass with elements"
        DataType: "child"
        DataElement "child"
        Enumeration key: U value: unknown

        expected new imported elements:
        dataClass "propagated DataClass with elements"
        dataElement: propagated child dataElement
        DataType: Propagated child dataType
        Enumeration value key: M value: Maybe
         */
        given:

        setupData()
        basicParameters.finalised = false
        basicParameters.importAsNewBranchModelVersion = true
        basicParameters.propagateFromPreviousVersion = true

        DataModel dataModel = DataModel.get(simpleDataModelId)
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
        dm.dataClasses.find{it.label == dataClass.label}

    }
}
