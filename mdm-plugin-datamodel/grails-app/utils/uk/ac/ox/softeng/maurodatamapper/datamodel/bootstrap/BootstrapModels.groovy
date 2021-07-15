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
package uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.version.Version

import asset.pipeline.grails.AssetResourceLocator
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.context.MessageSource
import org.springframework.core.io.Resource

import java.time.OffsetDateTime
import java.time.ZoneOffset

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.DEVELOPMENT
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.check
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.outputDomainErrors

@Slf4j
class BootstrapModels {


    public static final String COMPLEX_DATAMODEL_NAME = 'Complex Test DataModel'
    public static final String SIMPLE_DATAMODEL_NAME = 'Simple Test DataModel'
    public static final String FINALISED_EXAMPLE_DATAMODEL_NAME = 'Finalised Example Test DataModel'

    public static final String MODEL_VERSION_TREE_DATAMODEL_NAME = 'Model Version Tree DataModel'

    static DataModel buildAndSaveSimpleDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        DataModel simpleDataModel = DataModel.findByLabel(SIMPLE_DATAMODEL_NAME)

        if (!simpleDataModel) {
            log.debug("Creating simple datamodel")
            simpleDataModel = new DataModel(createdBy: DEVELOPMENT, label: SIMPLE_DATAMODEL_NAME, folder: folder, authority: authority)

            Classifier classifier

            classifier = Classifier.findByLabel('test classifier simple')

            if (!classifier) {
                log.debug("creating test classifier simple")
                classifier = new Classifier(createdBy: DEVELOPMENT, label: 'test classifier simple', readableByAuthenticatedUsers: true)
                checkAndSave(messageSource, classifier)
            } else {
                log.debug("test classifier simple already exists")
            }
            simpleDataModel.addToClassifiers(classifier)
            checkAndSave(messageSource, simpleDataModel)

            DataClass dataClass = new DataClass(createdBy: DEVELOPMENT, label: 'simple')

            simpleDataModel
                .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com/simple', key: 'mdk1', value: 'mdv1')
                .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com', key: 'mdk2', value: 'mdv2')
                .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com/simple', key: 'mdk2', value: 'mdv2')
                .addToDataClasses(dataClass)

            checkAndSave(messageSource, simpleDataModel)

            dataClass.addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com/simple', key: 'mdk1', value: 'mdv1')

            checkAndSave(messageSource, simpleDataModel)
        }

        log.debug("Simple Test DataModel id = {}", simpleDataModel.id.toString())
        log.debug("test classifier simple id = {}", simpleDataModel.classifiers[0].id.toString())
        simpleDataModel
    }

    static DataModel buildAndSaveComplexDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        DataModel dataModel = DataModel.findByLabel(COMPLEX_DATAMODEL_NAME)

        if (!dataModel) {
            dataModel = new DataModel(createdBy: DEVELOPMENT, label: COMPLEX_DATAMODEL_NAME, organisation: 'brc', author: 'admin person',
                                      folder: folder, authority: authority)
            checkAndSave(messageSource, dataModel)
            Classifier classifier = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier',
                                                                 readableByAuthenticatedUsers: true)
            Classifier classifier1 = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier2',
                                                                  readableByAuthenticatedUsers: true)
            checkAndSave(messageSource, classifier)
            checkAndSave(messageSource, classifier1)

            PrimitiveType stringPrimitive = new PrimitiveType(createdBy: DEVELOPMENT, label: 'string')

            dataModel.addToClassifiers(classifier)
                .addToClassifiers(classifier1)

                .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com', key: 'mdk1', value: 'mdv1')
                .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com', key: 'mdk2', value: 'mdv2')

                .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com/test', key: 'mdk1', value: 'mdv2')

                .addToAnnotations(createdBy: DEVELOPMENT, label: 'test annotation 1')

                .addToAnnotations(createdBy: DEVELOPMENT, label: 'test annotation 2', description: 'with description')

                .addToDataTypes(stringPrimitive)

                .addToDataTypes(new PrimitiveType(createdBy: DEVELOPMENT, label: 'integer'))

                .addToDataClasses(createdBy: DEVELOPMENT, label: 'emptyclass', description: 'dataclass with desc')
                .addToDataTypes(new EnumerationType(createdBy: DEVELOPMENT, label: 'yesnounknown')
                                    .addToEnumerationValues(key: 'Y', value: 'Yes', idx: 0)
                                    .addToEnumerationValues(key: 'N', value: 'No', idx: 1)
                                    .addToEnumerationValues(key: 'U', value: 'Unknown', idx: 2))

            DataClass parent =
                new DataClass(createdBy: DEVELOPMENT, label: 'parent', minMultiplicity: 1, maxMultiplicity: -1, dataModel: dataModel)
            DataClass child = new DataClass(createdBy: DEVELOPMENT, label: 'child')
            parent.addToDataClasses(child)
            dataModel.addToDataClasses(child)
            dataModel.addToDataClasses(parent)
            checkAndSave(messageSource, dataModel)

            parent.addToRules(name: "Bootstrapped Functional Test Rule",
                              description: 'Functional Test Description',
                              createdBy: DEVELOPMENT)

            dataModel.addToRules(name: "Bootstrapped Functional Test Rule",
                                 description: 'Functional Test Description',
                                 createdBy: DEVELOPMENT)

            stringPrimitive.addToRules(name: "Bootstrapped Functional Test Rule",
                                       description: 'Functional Test Description',
                                       createdBy: DEVELOPMENT)

            checkAndSave(messageSource, dataModel)

            ReferenceType refType = new ReferenceType(createdBy: DEVELOPMENT, label: 'child')
            child.addToReferenceTypes(refType)
            dataModel.addToDataTypes(refType)

            DataElement el1 = new DataElement(createdBy: DEVELOPMENT, label: 'child', minMultiplicity: 1, maxMultiplicity: 1)
            refType.addToDataElements(el1)
            parent.addToDataElements(el1)

            DataClass content = new DataClass(createdBy: DEVELOPMENT, label: 'content', description: 'A dataclass with elements',
                                              minMultiplicity: 0, maxMultiplicity: 1)
            DataElement el2 = new DataElement(createdBy: DEVELOPMENT, label: 'ele1',
                                              minMultiplicity: 0, maxMultiplicity: 20)
            dataModel.findDataTypeByLabel('string').addToDataElements(el2)
            content.addToDataElements(el2)
            DataElement el3 = new DataElement(createdBy: DEVELOPMENT, label: 'element2',
                                              minMultiplicity: 1, maxMultiplicity: 1)
            dataModel.findDataTypeByLabel('integer').addToDataElements(el3)
            content.addToDataElements(el3)
            dataModel.addToDataClasses(content)

            checkAndSave(messageSource, dataModel)

            el2.addToRules(name: "Bootstrapped Functional Test Rule",
                           description: 'Functional Test Description',
                           createdBy: DEVELOPMENT)

            checkAndSave(messageSource, dataModel)

            SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdBy: DEVELOPMENT,
                                                 targetMultiFacetAwareItem: DataClass.findByLabel('parent'))
            DataClass.findByLabel('content').addToSemanticLinks(link)

            checkAndSave(messageSource, dataModel)
            checkAndSave(messageSource, link)
        }

        dataModel
    }

    static DataModel buildAndSaveFinalisedSimpleDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        DataModel simpleDataModel = DataModel.findByLabel(FINALISED_EXAMPLE_DATAMODEL_NAME)

        if (!simpleDataModel) {
            simpleDataModel = new DataModel(createdBy: DEVELOPMENT,
                                            label: FINALISED_EXAMPLE_DATAMODEL_NAME,
                                            folder: folder,
                                            authority: authority)


            checkAndSave(messageSource, simpleDataModel)

            PrimitiveType primitiveType1 = new PrimitiveType(createdBy: DEVELOPMENT,
                                                             label: 'Finalised Data Type')
            DataElement dataElement1 = new DataElement(createdBy: DEVELOPMENT,
                                                       label: 'Finalised Data Element',
                                                       minMultiplicity: 1,
                                                       maxMultiplicity: 1,
                                                       dataType: primitiveType1)
            DataElement dataElement2 = new DataElement(createdBy: DEVELOPMENT,
                                                       label: 'Another DataElement',
                                                       minMultiplicity: 1,
                                                       maxMultiplicity: 1,
                                                       dataType: primitiveType1)
            DataClass dataClass = new DataClass(createdBy: DEVELOPMENT,
                                                label: 'Finalised Data Class')

            dataClass.addToDataElements(dataElement1).addToDataElements(dataElement2)

            simpleDataModel
                .addToDataClasses(dataClass)
                .addToDataClasses(createdBy: DEVELOPMENT, label: 'Another Data Class')
                .addToDataTypes(primitiveType1)

            checkAndSave(messageSource, simpleDataModel)

            simpleDataModel.finalised = true
            simpleDataModel.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
            simpleDataModel.breadcrumbTree.finalised = true
            simpleDataModel.breadcrumbTree.updateTree()
            simpleDataModel.modelVersion = Version.from('1.0.0')

            checkAndSave(messageSource, simpleDataModel)
        }

        simpleDataModel
    }

    static void buildAndSaveModelVersionTree(MessageSource messageSource, Folder folder, Authority authority, DataModelService dataModelService,
                                             DataClassService dataClassService, DataElementService dataElementService,
                                             AssetResourceLocator assetResourceLocator) {
        /*
                                                 /- anotherFork
    v1 --------------------------- v2 -- v3  -- v4 --------------- v5 --- main
      \\_ newBranch (v1)                  \_ testBranch (v3)          \__ anotherBranch (v5)
       \_ fork ---- main                                               \_ interestingBranch (v5)
    */
        log.debug("Creating model version tree")
        User dev = [emailAddress: DEVELOPMENT] as User
        UserSecurityPolicyManager policyManager = PublicAccessSecurityPolicyManager.instance

        // V1
        DataModel v1 = new DataModel(createdBy: DEVELOPMENT,
                                     label: MODEL_VERSION_TREE_DATAMODEL_NAME,
                                     organisation: 'bootStrap',
                                     author: 'bill',
                                     folder: folder,
                                     authority: authority)

        checkAndSave(messageSource, v1)
        v1 = populateV1(v1, messageSource)
        v1 = dataModelService.finaliseModel(v1, dev, Version.from('1'), null, null)
        checkAndSave(messageSource, v1)

        // Fork and finalise fork
        DataModel fork = dataModelService.createNewForkModel("$MODEL_VERSION_TREE_DATAMODEL_NAME fork", v1, dev, false, policyManager)
        checkAndSave(messageSource, fork)
        fork = dataModelService.finaliseModel(fork, dev, Version.from('1'), null, null)
        checkAndSave(messageSource, fork)

        // Fork main branch
        DataModel forkMain = dataModelService.createNewBranchModelVersion('main', fork, dev, false, policyManager)
        checkAndSave(messageSource, forkMain)

        // V2 main branch
        DataModel v2 = dataModelService.createNewBranchModelVersion('main', v1, dev, false, policyManager)
        checkAndSave(messageSource, v2)
        v2 = modifyV2(v2, messageSource, assetResourceLocator, dataClassService, dataElementService)
        checkAndSave(messageSource, v2)

        // newBranch from v1 (do this after is it creates the main branch if done before and then we have to hassle getting the id)
        DataModel newBranch = dataModelService.createNewBranchModelVersion('newBranch', v1, dev, false, policyManager)
        checkAndSave(messageSource, newBranch)
        modifyNewBranch(newBranch, messageSource)
        checkAndSave(messageSource, newBranch)

        // Finalise the main branch to v2
        v2 = dataModelService.finaliseModel(v2, dev, Version.from('2'), null, null)
        checkAndSave(messageSource, v2)

        // V3 main branch
        DataModel v3 = dataModelService.createNewBranchModelVersion('main', v2, dev, false, policyManager)
        checkAndSave(messageSource, v3)
        // Finalise the main branch to v3
        v3 = dataModelService.finaliseModel(v3, dev, Version.from('3'), null, null)
        checkAndSave(messageSource, v3)

        // V4 main branch
        DataModel v4 = dataModelService.createNewBranchModelVersion('main', v3, dev, false, policyManager)
        checkAndSave(messageSource, v4)

        // testBranch from v3 (do this after is it creates the main branch if done before and then we have to hassle getting the id)
        DataModel testBranch = dataModelService.createNewBranchModelVersion('testBranch', v3, dev, false, policyManager)
        checkAndSave(messageSource, testBranch)
        testBranch = modifyTestBranch(testBranch, messageSource)
        checkAndSave(messageSource, testBranch)

        // Finalise main branch to v4
        v4 = dataModelService.finaliseModel(v4, dev, Version.from('4'), null, null)
        checkAndSave(messageSource, v4)

        // Fork from v4
        DataModel anotherFork = dataModelService.createNewForkModel("$MODEL_VERSION_TREE_DATAMODEL_NAME another fork", v4, dev, false, policyManager)
        checkAndSave(messageSource, anotherFork)

        // V5 and finalise
        DataModel v5 = dataModelService.createNewBranchModelVersion('main', v4, dev, false, policyManager)
        checkAndSave(messageSource, v5)
        v5 = dataModelService.finaliseModel(v5, dev, Version.from('5'), null, null)
        checkAndSave(messageSource, v5)

        // Main branch
        DataModel main = dataModelService.createNewBranchModelVersion('main', v5, dev, false, policyManager)
        checkAndSave(messageSource, main)

        // Another branch
        DataModel anotherBranch = dataModelService.createNewBranchModelVersion('anotherBranch', v5, dev, false, policyManager)
        checkAndSave(messageSource, anotherBranch)
        anotherBranch = modifyAnotherBranch(anotherBranch, messageSource)
        checkAndSave(messageSource, anotherBranch)

        // Interesting branch
        DataModel interestingBranch = dataModelService.createNewBranchModelVersion('interestingBranch', v5, dev, false, policyManager)
        checkAndSave(messageSource, interestingBranch)
    }

    static DataModel populateV1(DataModel v1DataModel, MessageSource messageSource) {

        v1DataModel.addToMetadata(createdBy: DEVELOPMENT, namespace: 'v1Versioning.com', key: 'jun1', value: 'jun2')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'v1Versioning.com', key: 'mdk1', value: 'mdv1')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'v1Versioning.com', key: 'mdk2', value: 'mdv2')

        PrimitiveType v1PrimitiveType1 = new PrimitiveType(createdBy: DEVELOPMENT,
                                                           label: 'V1 Data Type')
        v1DataModel.addToDataTypes(v1PrimitiveType1)
        DataElement v1DataElement1 = new DataElement(createdBy: DEVELOPMENT,
                                                     label: 'V1 Data Element',
                                                     minMultiplicity: 1,
                                                     maxMultiplicity: 1,
                                                     dataType: v1PrimitiveType1)
        DataElement v1DataElement2 = new DataElement(createdBy: DEVELOPMENT,
                                                     label: 'V1 Second DataElement',
                                                     minMultiplicity: 1,
                                                     maxMultiplicity: 1,
                                                     dataType: v1PrimitiveType1)
        DataElement v1ModifyDataElement3 = new DataElement(createdBy: DEVELOPMENT,
                                                           label: 'V1 Modify DataElement',
                                                           minMultiplicity: 1,
                                                           maxMultiplicity: 1,
                                                           dataType: v1PrimitiveType1)
        DataElement v1ModifyDataElement4 = new DataElement(createdBy: DEVELOPMENT,
                                                           label: 'V1 Modify DataElement 2',
                                                           minMultiplicity: 1,
                                                           maxMultiplicity: 1,
                                                           dataType: v1PrimitiveType1)
        DataClass v1DataClass = new DataClass(createdBy: DEVELOPMENT,
                                              label: 'V1 Data Class')
        DataClass v1InternalDataClass = new DataClass(createdBy: DEVELOPMENT,
                                                      label: 'V1 Internal Data Class')
        DataClass v1ModifyDataClass = new DataClass(createdBy: DEVELOPMENT,
                                                    label: 'V1 Modify Data Class')

        v1InternalDataClass.addToDataElements(v1DataElement1)
        v1ModifyDataClass.addToDataElements(v1ModifyDataElement3).addToDataElements(v1ModifyDataElement4)
        v1DataClass.addToDataElements(v1DataElement2)
            .addToDataClasses(v1InternalDataClass)

        v1DataModel
            .addToDataClasses(v1DataClass)
            .addToDataClasses(v1ModifyDataClass)
            .addToDataClasses(createdBy: DEVELOPMENT, label: 'V1 Another Data Class')
            .addToDataTypes(v1PrimitiveType1)

        checkAndSave(messageSource, v1DataModel)

        v1DataModel.addToRules(name: "Bootstrapped versioning Test Rule",
                               description: "versioning Model rule Description",
                               createdBy: DEVELOPMENT)

        v1DataModel.addToRules(name: "Bootstrapped modify rule",
                               description: "Bootstrapped rule for modification",
                               createdBy: DEVELOPMENT)


        v1DataModel.addToMetadata(namespace: 'versioning.com', key: 'map', value: '''
                                             /- anotherFork
v1 --------------------------- v2 -- v3  -- v4 --------------- v5 --- main
  \\\\_ newBranch (v1)                  \\_ testBranch (v3)          \\__ anotherBranch (v5)
   \\_ fork ---- main                                               \\_ interestingBranch (v5)
 ''')


        checkAndSave(messageSource, v1DataModel)

        return v1DataModel

    }

    static DataModel modifyV2(DataModel v2DataModel, MessageSource messageSource, AssetResourceLocator assetResourceLocator, DataClassService dataClassService,
                              DataElementService dataElementService) {
        v2DataModel.addToMetadata(createdBy: DEVELOPMENT, namespace: 'JRDev.com/versioning', key: 'mkv1', value: 'mkv2')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'JRDev.com/versioning', key: 'abc2', value: 'abc3')

        v2DataModel.setAuthor('Dante')
        v2DataModel.setOrganisation('Baal')

        Resource resource = assetResourceLocator.findAssetForURI('versioningModelDescription.txt')

        try {v2DataModel.setDescription(resource.getInputStream().getText())}
        catch (NullPointerException e) {
            v2DataModel.setDescription('default description due to error reading file. see log')
            log.debug(
                'error reading the description file, please check the asset pipeline and ensure the versioningModelDescription.txt file is in the' +
                'available assets')
        }

        PrimitiveType v2PrimitiveType1 = new PrimitiveType(createdBy: DEVELOPMENT,
                                                           label: 'V2 Data Type')
        PrimitiveType v2PrimitiveType2 = new PrimitiveType(createdBy: DEVELOPMENT,
                                                           label: 'V2 Data Type 2')
        PrimitiveType v2PrimitiveType3 = new PrimitiveType(createdBy: DEVELOPMENT,
                                                           label: 'V2 Data Type 3')
        DataElement v2DataElement1 = new DataElement(createdBy: DEVELOPMENT,
                                                     label: 'V2 Data Element',
                                                     minMultiplicity: 1,
                                                     maxMultiplicity: 1,
                                                     dataType: v2PrimitiveType1)
        DataElement v2DataElement2 = new DataElement(createdBy: DEVELOPMENT,
                                                     label: 'V2 Second DataElement',
                                                     minMultiplicity: 1,
                                                     maxMultiplicity: 1,
                                                     dataType: v2PrimitiveType2)
        DataElement v2DataElement3 = new DataElement(createdBy: DEVELOPMENT,
                                                     label: 'V2 Third DataElement',
                                                     minMultiplicity: 1,
                                                     maxMultiplicity: 1,
                                                     dataType: v2PrimitiveType3)
        DataClass v2DataClass = new DataClass(createdBy: DEVELOPMENT,
                                              label: 'V2 Data Class')

        v2DataClass.addToDataElements(v2DataElement1).addToDataElements(v2DataElement2).addToDataElements(v2DataElement3)

        v2DataModel
            .addToDataClasses(v2DataClass)
            .addToDataTypes(v2PrimitiveType1)
            .addToDataTypes(v2PrimitiveType2)
            .addToDataTypes(v2PrimitiveType3)

        checkAndSave(messageSource, v2DataModel)

        v2DataModel.addToRules(name: "Bootstrapped versioning V2Model Rule",
                               description: "versioning V2Model model Description",
                               createdBy: DEVELOPMENT)

        v2DataModel.addToRules(name: "Bootstrapped versioning Deletion Rule",
                               description: "versioning V2Model model for Deletion",
                               createdBy: DEVELOPMENT)

        checkAndSave(messageSource, v2DataModel)

        manipulateV2Rules(v2DataModel, messageSource)
        manipulateV2DataElements(v2DataModel, messageSource, dataElementService)
        manipulateV2DataClasses(v2DataModel, messageSource, dataClassService)
        manipulateV2MetaData(v2DataModel, messageSource)

        v2DataModel

    }

    static void manipulateV2DataClasses(DataModel v2DataModel, MessageSource messageSource, DataClassService dataClassService) {

        DataClass v1DataClass = v2DataModel.getDataClasses().find {it.label == 'V1 Data Class'}
        DataClass v1InternalDataClass = v1DataClass.getDataClasses().find {it.label == 'V1 Internal Data Class'}
        DataClass v1ModifyDataClass = v2DataModel.dataClasses.find {it.label == 'V1 Modify Data Class'}

        //modify dataClass
        v1DataClass.description = 'Modified this description for V2'
        checkAndSave(messageSource, v1DataClass)

        dataClassService.delete(v1InternalDataClass)

        //Move internal DC, this counts as a deletion and an addition
        DataClass moved = new DataClass(createdBy: DEVELOPMENT,
                                        label: 'V1 Internal Data Class')
        v1ModifyDataClass.addToDataClasses(moved)
        v2DataModel.addToDataClasses(moved)
        checkAndSave(messageSource, v1ModifyDataClass)
        checkAndSave(messageSource, v2DataModel)
    }


    static void manipulateV2DataElements(DataModel v2DataModel, MessageSource messageSource, DataElementService dataElementService) {

        DataClass v1ModifyDataClass = v2DataModel.dataClasses.find {it.label == 'V1 Modify Data Class'}

        //Rename DataElement this counts as an addition and deletion
        DataElement modifyDataElement1 = v1ModifyDataClass.dataElements.find {it.label == 'V1 Modify DataElement'}
        modifyDataElement1.label = 'Modified Label On this element'
        checkAndSave(messageSource, modifyDataElement1)

        //remove other data element
        DataElement modifyDataElement2 = v1ModifyDataClass.dataElements.find {it.label == 'V1 Modify DataElement 2'}
        dataElementService.delete(modifyDataElement2)
    }

    static void manipulateV2MetaData(DataModel v2DataModel, MessageSource messageSource) {

        //modify metaData
        Metadata md1 = v2DataModel.metadata.find {it.key == 'jun1'}
        md1.value = 'mod1'
        checkAndSave(messageSource, md1)

        //remove metaData
        Metadata md2 = v2DataModel.metadata.find {it.key == 'mdk2'}
        v2DataModel.metadata.remove(md2)
        md2.delete()
        checkAndSave(messageSource, v2DataModel)
    }

    static void manipulateV2Rules(DataModel v2DataModel, MessageSource messageSource) {

        Rule modifyRule = v2DataModel.rules.find {it.name == 'Bootstrapped versioning V2Model Rule'}
        modifyRule.description = 'Modified this description'
        checkAndSave(messageSource, modifyRule)

        Rule deleteRule = v2DataModel.rules.find {it.name == 'Bootstrapped versioning Deletion Rule'}
        v2DataModel.rules.remove(deleteRule)
        checkAndSave(messageSource, v2DataModel)
    }


    static DataModel modifyNewBranch(DataModel newBranch, MessageSource messageSource) {

        newBranch
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'versioning.com', key: 'mdk1', value: 'mdv1')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'versioning.com', key: 'mdk2', value: 'mdv2')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'versioning.com/bootstrap', key: 'mdk1', value: 'mdv2')

            .addToAnnotations(createdBy: DEVELOPMENT, label: 'versioning annotation 1')
            .addToAnnotations(createdBy: DEVELOPMENT, label: 'versioning annotation 2', description: 'with description')

            .addToDataTypes(new PrimitiveType(createdBy: DEVELOPMENT, label: 'string'))
            .addToDataTypes(new PrimitiveType(createdBy: DEVELOPMENT, label: 'integer'))

            .addToDataClasses(createdBy: DEVELOPMENT, label: 'emptyVersioningClass', description: 'dataclass with desc')
            .addToDataTypes(new EnumerationType(createdBy: DEVELOPMENT, label: 'catdogfish')
                                .addToEnumerationValues(key: 'C', value: 'Cat', idx: 0)
                                .addToEnumerationValues(key: 'D', value: 'Dog', idx: 1)
                                .addToEnumerationValues(key: 'F', value: 'Fish', idx: 2))
        checkAndSave(messageSource, newBranch)
        newBranch

    }

    static DataModel modifyTestBranch(DataModel testBranch, MessageSource messageSource) {
        DataClass v1DataClass = testBranch.getDataClasses().find {it.label == 'V1 Data Class'}

        v1DataClass.description = 'Modified this description for test branch'
        checkAndSave(messageSource, v1DataClass)


        DataElement de = v1DataClass.dataElements.find {it.label == 'V1 Second DataElement'}
        de.description = 'Adding a description'
        de.minMultiplicity = 0
        checkAndSave(messageSource, de)

        Metadata md1 = testBranch.metadata.find {it.key == 'jun1'}
        md1.value = 'modtest'
        checkAndSave(messageSource, md1)


        testBranch

    }

    static DataModel modifyAnotherBranch(DataModel anotherBranch, MessageSource messageSource) {
        DataClass v1DataClass = anotherBranch.getDataClasses().find {it.label == 'V1 Data Class'}
        v1DataClass.description = 'Modified this description for test branch'
        v1DataClass.addToMetadata(createdBy: DEVELOPMENT, namespace: 'versioning.com', key: 'mdk1', value: 'mdv1')
        checkAndSave(messageSource, v1DataClass)

        DataElement de = v1DataClass.dataElements.find {it.label == 'V1 Second DataElement'}
        de.maxMultiplicity = -1
        checkAndSave(messageSource, de)

        PrimitiveType string = new PrimitiveType(createdBy: DEVELOPMENT, label: 'string')
        anotherBranch.addToDataTypes(string)
        checkAndSave(messageSource, anotherBranch)

        DataClass v2DataClass = anotherBranch.getDataClasses().find {it.label == 'V2 Data Class'}
        de = v2DataClass.dataElements.find {it.label == 'V2 Data Element'}
        de.dataType = string
        checkAndSave(messageSource, de)

        Metadata md1 = anotherBranch.metadata.find {it.key == 'jun1'}
        md1.value = 'modanother'
        checkAndSave(messageSource, md1)

        anotherBranch

    }


    static Map<String, UUID> buildMergeModelsForTestingOnly(UUID id, User creator, DataModelService dataModelService, DataClassService dataClassService,
                                                            MetadataService metadataService, SessionFactory sessionFactory, MessageSource messageSource) {
        // generate common ancestor
        UserSecurityPolicyManager policyManager = PublicAccessSecurityPolicyManager.instance
        DataModel commonAncestor = dataModelService.get(id)
        commonAncestor.author = 'john'
        commonAncestor.addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'deleteSourceOnly'))
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'deleteTargetOnly'))
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'modifySourceOnly', description: 'common'))
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'modifyTargetOnly'))
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'deleteBoth'))
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'deleteSourceAndModifyTarget'))
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'modifySourceAndDeleteTarget'))
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'modifyBothReturningNoDifference', description: 'common'))
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'modifyBothReturningDifference', description: 'common'))
        commonAncestor.addToDataClasses(
            new DataClass(createdBy: creator.emailAddress, label: 'existingClass')
                .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'deleteSourceOnlyFromExistingClass'))
                .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'deleteTargetOnlyFromExistingClass'))
        ).addToMetadata(namespace: 'test', key: 'deleteSourceOnly', value: 'deleteSourceOnly')
            .addToMetadata(namespace: 'test', key: 'modifySourceOnly', value: 'modifySourceOnly')
        dataModelService.finaliseModel(commonAncestor, creator, null, null, null)
        checkAndSave(messageSource, commonAncestor)

        assert commonAncestor.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME


        // Generate main/target branch
        UUID targetModelId = createAndSaveNewBranchModel(VersionAwareConstraints.DEFAULT_BRANCH_NAME, commonAncestor, creator, dataModelService,
                                                         messageSource, policyManager)

        dataClassService.delete(dataClassService.findByDataModelIdAndLabel(targetModelId, 'deleteTargetOnlyFromExistingClass'))
        dataClassService.delete(dataClassService.findByDataModelIdAndLabel(targetModelId, 'deleteTargetOnly'))
        dataClassService.delete(dataClassService.findByDataModelIdAndLabel(targetModelId, 'deleteBoth'))
        dataClassService.delete(dataClassService.findByDataModelIdAndLabel(targetModelId, 'modifySourceAndDeleteTarget'))

        checkAndSave messageSource, dataClassService.findByDataModelIdAndLabel(targetModelId, 'modifyTargetOnly').tap {
            description = 'Description'
        }
        checkAndSave messageSource, dataClassService.findByDataModelIdAndLabel(targetModelId, 'deleteSourceAndModifyTarget').tap {
            description = 'Description'
        }
        checkAndSave messageSource, dataClassService.findByDataModelIdAndLabel(targetModelId, 'modifyBothReturningNoDifference').tap {
            description = 'Description'
        }
        checkAndSave messageSource, dataClassService.findByDataModelIdAndLabel(targetModelId, 'modifyBothReturningDifference').tap {
            description = 'DescriptionTarget'
        }

        checkAndSave messageSource, dataClassService.findByDataModelIdAndLabel(targetModelId, 'existingClass')
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'addTargetToExistingClass'))

        DataModel draftModel = dataModelService.get(targetModelId)
        draftModel.author = 'dick'
        checkAndSave messageSource, new DataClass(createdBy: creator.emailAddress, label: 'addTargetWithNestedChild', dataModel: draftModel)
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'addTargetNestedChild', dataModel: draftModel))
        checkAndSave messageSource, new DataClass(createdBy: creator.emailAddress, label: 'addTargetOnly', dataModel: draftModel)
        checkAndSave messageSource, new DataClass(createdBy: creator.emailAddress, label: 'addBothReturningNoDifference', dataModel: draftModel)
        checkAndSave messageSource, new DataClass(createdBy: creator.emailAddress, label: 'addBothReturningDifference', description: 'target', dataModel: draftModel)

        checkAndSave messageSource, dataModelService.get(targetModelId).tap {
            description = 'DescriptionTarget'
        }

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()


        // Generate test/source branch
        UUID sourceModelId = createAndSaveNewBranchModel('test', commonAncestor, creator, dataModelService, messageSource, policyManager)

        dataClassService.delete(dataClassService.findByDataModelIdAndLabel(sourceModelId, 'deleteSourceOnlyFromExistingClass'))
        dataClassService.delete(dataClassService.findByDataModelIdAndLabel(sourceModelId, 'deleteSourceOnly'))
        dataClassService.delete(dataClassService.findByDataModelIdAndLabel(sourceModelId, 'deleteBoth'))
        dataClassService.delete(dataClassService.findByDataModelIdAndLabel(sourceModelId, 'deleteSourceAndModifyTarget'))
        metadataService.delete(metadataService.findAllByMultiFacetAwareItemId(sourceModelId).find {it.key == 'deleteSourceOnly'})

        checkAndSave messageSource, dataClassService.findByDataModelIdAndLabel(sourceModelId, 'modifySourceOnly').tap {
            description = 'Description'
        }
        checkAndSave messageSource, dataClassService.findByDataModelIdAndLabel(sourceModelId, 'modifySourceAndDeleteTarget').tap {
            description = 'Description'
        }
        checkAndSave messageSource, dataClassService.findByDataModelIdAndLabel(sourceModelId, 'modifyBothReturningNoDifference').tap {
            description = 'Description'
        }
        checkAndSave messageSource, dataClassService.findByDataModelIdAndLabel(sourceModelId, 'modifyBothReturningDifference').tap {
            description = 'DescriptionSource'
        }

        checkAndSave messageSource, dataClassService.findByDataModelIdAndLabel(sourceModelId, 'existingClass')
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'addSourceToExistingClass'))

        DataModel testModel = dataModelService.get(sourceModelId)
        testModel.organisation = 'under test'
        testModel.author = 'harry'
        checkAndSave messageSource, new DataClass(createdBy: creator.emailAddress, label: 'addSourceWithNestedChild', dataModel: testModel)
            .addToDataClasses(new DataClass(createdBy: creator.emailAddress, label: 'addSourceNestedChild', dataModel: testModel))

        checkAndSave messageSource, new DataClass(createdBy: creator.emailAddress, label: 'addSourceOnly', dataModel: testModel)
        checkAndSave messageSource, new DataClass(createdBy: creator.emailAddress, label: 'addBothReturningNoDifference', dataModel: testModel)
        checkAndSave messageSource, new DataClass(createdBy: creator.emailAddress, label: 'addBothReturningDifference', description: 'source', dataModel: testModel)
        checkAndSave messageSource, new PrimitiveType(createdBy: StandardEmailAddress.ADMIN, label: 'addSourceOnlyOnlyChangeInArray', dataModel: testModel)


        checkAndSave messageSource, metadataService.findAllByMultiFacetAwareItemId(sourceModelId).find {it.key == 'modifySourceOnly'}.tap {
            value = 'altered'
        }

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        [commonAncestorId: commonAncestor.id,
         sourceId        : sourceModelId,
         targetId        : targetModelId]
    }

    static UUID createAndSaveNewBranchModel(String branchName, DataModel base, User creator, DataModelService dataModelService, MessageSource messageSource,
                                            UserSecurityPolicyManager userSecurityPolicyManager) {
        DataModel dataModel = dataModelService.createNewBranchModelVersion(branchName, base, creator, false, userSecurityPolicyManager)
        if (dataModel.hasErrors()) {
            outputDomainErrors(messageSource, dataModel)
            throw new ApiInvalidModelException('BM01', 'Could not create new branch version', dataModel.errors)
        }
        check(messageSource, dataModel)
        dataModelService.saveModelWithContent(dataModel)
        dataModel.id
    }
}