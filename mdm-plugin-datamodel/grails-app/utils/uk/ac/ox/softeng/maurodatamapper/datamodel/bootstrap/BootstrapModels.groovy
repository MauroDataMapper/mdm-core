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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Version

import asset.pipeline.grails.AssetResourceLocator
import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource
import org.springframework.core.io.Resource

import java.time.OffsetDateTime
import java.time.ZoneOffset

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.DEVELOPMENT
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

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
        v2 = populateV2(v2, messageSource, assetResourceLocator)
        checkAndSave(messageSource, v2)
        v2 = modifyV1(v2, messageSource, assetResourceLocator)
        checkAndSave(messageSource, v2)

        // newBranch from v1 (do this after is it creates the main branch if done before and then we have to hassle getting the id)
        DataModel newBranch = dataModelService.createNewBranchModelVersion('newBranch', v1, dev, false, policyManager)
        checkAndSave(messageSource, newBranch)
        populateNewBranch(newBranch, messageSource)

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

        // Interesting branch
        DataModel interestingBranch = dataModelService.createNewBranchModelVersion('interestingBranch', v5, dev, false, policyManager)
        checkAndSave(messageSource, interestingBranch)
    }

    static DataModel populateV1(DataModel v1DataModel, MessageSource messageSource) {

        v1DataModel.addToMetadata(createdBy: DEVELOPMENT, namespace: 'v1Versioning.com', key: 'jun1', value: 'jun2')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'v1Versioning.com', key: 'mdk1', value: 'mdv1')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'v1Versioning.com', key: 'mdk2', value: 'mdv2')

        PrimitiveType v1PrimitiveType1 = new PrimitiveType(createdBy: DEVELOPMENT,
                                                           label: 'V1 Finalised Data Type')
        DataElement v1DataElement1 = new DataElement(createdBy: DEVELOPMENT,
                                                     label: 'V1 Finalised Data Element',
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
                                              label: 'V1 Finalised Data Class')
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
        return v1DataModel

    }

    static DataModel modifyV1(DataModel v2DataModel, MessageSource messageSource) {

        manipulateDataElements(v2DataModel, messageSource)
        manipulateDataClasses(v2DataModel, messageSource)
        manipulateMetaData(v2DataModel, messageSource)

        v2DataModel = DataModel.findById(v2DataModel.id)

        return v2DataModel
    }

    static void manipulateDataClasses(DataModel v2DataModel, MessageSource messageSource) {

        v2DataModel = DataModel.findById(v2DataModel.id)

        DataClass v1DataClass = v2DataModel.getDataClasses().find { it.label = 'V1 Finalised Data Class' }
        DataClass v1InternalDataClass = v1DataClass.getDataClasses().find { it.label = '1 Finalised Data Class' }

        //modify dataClass and internal DataClass
        v1InternalDataClass.label = 'Modified this internal label name'
        v1DataClass.label = 'Modified this label name'
        checkAndSave(messageSource, v1InternalDataClass)
        checkAndSave(messageSource, v1DataClass)

        //remove internal Dataclass, then DataClass
        v1DataClass.removeFromDataClasses(v1InternalDataClass)
        checkAndSave(messageSource, v1DataClass)
        v2DataModel.removeFromDataClasses(v1DataClass)
        checkAndSave(messageSource, v2DataModel)


    }

    static void manipulateDataElements(DataModel v2DataModel, MessageSource messageSource) {

        v2DataModel = DataModel.findById(v2DataModel.id)

        DataClass v1ModifyDataClass = v2DataModel.getDataClasses().find { it.label = 'V1 Modify Data Class' }

        //Modify DataElement
        DataElement modifyDataElement1 = v1ModifyDataClass.getDataElements().find { it.label = 'V1 Modify DataElement' }
        modifyDataElement1.label = 'Modified Label On this element'
        checkAndSave(messageSource, modifyDataElement1)

        //Add modified and remove other data element
        DataElement modifyDataElement2 = v1ModifyDataClass.getDataElements().find { it.label = 'V1 Modify DataElement 2' }
        v1ModifyDataClass.removeFromDataElements(modifyDataElement2)
        checkAndSave(messageSource, v1ModifyDataClass)
        v1ModifyDataClass.addToDataElements(modifyDataElement1)
        checkAndSave(messageSource, v1ModifyDataClass)


    }

    static void manipulateMetaData(DataModel v2DataModel, MessageSource messageSource) {

        v2DataModel = DataModel.findById(v2DataModel.id)
        //modify metaData
        Metadata md1 = v2DataModel.getMetadata().find { it.key = 'jun1' }
        md1.value = 'mod1'
        v2DataModel.metadata.add(md1)
        checkAndSave(messageSource, v2DataModel)

        //remove metaData
        Metadata md2 = v2DataModel.getMetadata().find { it.key = 'mdk2' }
        v2DataModel.removeFromMetadata(md2)
        checkAndSave(messageSource, v2DataModel)
    }

    static DataModel populateV2(DataModel v2DataModel, MessageSource messageSource,
                                AssetResourceLocator assetResourceLocator) {
        v2DataModel.addToMetadata(createdBy: DEVELOPMENT, namespace: 'JRDev.com/versioning', key: 'mkv1', value: 'mkv2')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'JRDev.com/versioning', key: 'abc2', value: 'abc3')

        v2DataModel.setAuthor('Dante')
        v2DataModel.setOrganisation('Baal')

        v2DataModel.setDescription(getDescriptionFromFile(assetResourceLocator, 'modelDescription.txt'))

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
        v2DataModel

    }

    static DataModel populateNewBranch(DataModel newBranch, MessageSource messageSource) {


        PrimitiveType stringPrimitive = new PrimitiveType(createdBy: DEVELOPMENT, label: 'string')

        newBranch
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'versioning.com', key: 'mdk1', value: 'mdv1')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'versioning.com', key: 'mdk2', value: 'mdv2')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'versioning.com/bootstrap', key: 'mdk1', value: 'mdv2')

            .addToAnnotations(createdBy: DEVELOPMENT, label: 'versioning annotation 1')
            .addToAnnotations(createdBy: DEVELOPMENT, label: 'versioning annotation 2', description: 'with description')

            .addToDataTypes(stringPrimitive)
            .addToDataTypes(new PrimitiveType(createdBy: DEVELOPMENT, label: 'integer'))

            .addToDataClasses(createdBy: DEVELOPMENT, label: 'emptyVersioningClass', description: 'dataclass with desc')
            .addToDataTypes(new EnumerationType(createdBy: DEVELOPMENT, label: 'catdogfish')
                                .addToEnumerationValues(key: 'C', value: 'Cat', idx: 0)
                                .addToEnumerationValues(key: 'D', value: 'Dog', idx: 1)
                                .addToEnumerationValues(key: 'F', value: 'Fish', idx: 2))
        checkAndSave(messageSource, newBranch)
        newBranch

    }

    static String getDescriptionFromFile(AssetResourceLocator assetResourceLocator, fileName) {
        try {
            Resource resource = assetResourceLocator.findResourceForURI(fileName)
            resource.getInputStream().getText()
        } catch (FileNotFoundException e) {
            log.error("Cannot locate file " + fileName + "---" + e)
            'File Path not located: default description'
        }
    }

}