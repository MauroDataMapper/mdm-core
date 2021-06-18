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

import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource

import java.time.OffsetDateTime
import java.time.ZoneOffset

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.DEVELOPMENT
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

@Slf4j
class BootstrapModels {

    public static final String COMPLEX_DATAMODEL_NAME = 'Complex Test DataModel'
    public static final String SIMPLE_DATAMODEL_NAME = 'Simple Test DataModel'
    public static final String FINALISED_EXAMPLE_DATAMODEL_NAME = 'Finalised Example Test DataModel'
    public static final String MODEL_VERSION_TREE_DATAMODEL_V1 = 'Versioning Tree Example DataModel - Version 1'
    public static final String MODEL_VERSION_TREE_DATAMODEL_V2 = 'Versioning Tree Example DataModel - Version 2'
    public static final String MODEL_VERSION_TREE_DATAMODEL_NAME = 'Versioning Tree Branch'

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

    static void buildAndSaveModelVersionTree(MessageSource messageSource, Folder folder, Authority authority, DataModelService dataModelService) {
        /*
                -Development Folder
                 -Example model v1 (finalised)
                     +Stuff
                 +Example model v2 (draft)
                 +Example model v2 Branch (draft)

                        DEV (finalised)
                            |
                            |
                        Dev (main)  ---------------- dev (main Fork)
                            |      \
                            |        \
                            |         Dev (branch)

        */
        User dev = [emailAddress: DEVELOPMENT] as User
        UserSecurityPolicyManager policyManager = PublicAccessSecurityPolicyManager.instance

        buildVersionTreeV1Model(messageSource, folder, authority)
        DataModel v2DataModel = buildVersionTreeV2Model(messageSource, folder, authority, dataModelService)
        buildVersionTreeV2Branches(messageSource, dataModelService, v2DataModel, dev, policyManager)
    }


    static void buildVersionTreeV1Model(MessageSource messageSource, Folder folder, Authority authority) {

        DataModel v1DataModel = DataModel.findByLabel(MODEL_VERSION_TREE_DATAMODEL_V1)

        if (!v1DataModel) {
            v1DataModel = new DataModel(createdBy: DEVELOPMENT,
                                        label: MODEL_VERSION_TREE_DATAMODEL_V1,
                                        folder: folder,
                                        authority: authority)
            checkAndSave(messageSource, v1DataModel)

            PrimitiveType V1PrimitiveType1 = new PrimitiveType(createdBy: DEVELOPMENT,
                                                               label: 'V1 Finalised Data Type')
            DataElement V1DataElement1 = new DataElement(createdBy: DEVELOPMENT,
                                                         label: 'V1 Finalised Data Element',
                                                         minMultiplicity: 1,
                                                         maxMultiplicity: 1,
                                                         dataType: V1PrimitiveType1)
            DataElement V1DataElement2 = new DataElement(createdBy: DEVELOPMENT,
                                                         label: 'V1 Second DataElement',
                                                         minMultiplicity: 1,
                                                         maxMultiplicity: 1,
                                                         dataType: V1PrimitiveType1)
            DataClass V1DataClass = new DataClass(createdBy: DEVELOPMENT,
                                                  label: 'V1 Finalised Data Class')

            V1DataClass.addToDataElements(V1DataElement1).addToDataElements(V1DataElement2)

            v1DataModel
                .addToDataClasses(V1DataClass)
                .addToDataClasses(createdBy: DEVELOPMENT, label: 'V1 Another Data Class')
                .addToDataTypes(V1PrimitiveType1)

            checkAndSave(messageSource, v1DataModel)

            v1DataModel.finalised = true
            v1DataModel.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
            v1DataModel.breadcrumbTree.finalised = true
            v1DataModel.breadcrumbTree.updateTree()
            v1DataModel.modelVersion = Version.from('1.0.0')

            checkAndSave(messageSource, v1DataModel)
        }
    }

    static DataModel buildVersionTreeV2Model(MessageSource messageSource, Folder folder, Authority authority, DataModelService dataModelService) {
        DataModel v2DataModel = DataModel.findByLabel(MODEL_VERSION_TREE_DATAMODEL_V2)

        if (!v2DataModel) {
            v2DataModel = new DataModel(createdBy: DEVELOPMENT,
                                        label: MODEL_VERSION_TREE_DATAMODEL_V2,
                                        folder: folder,
                                        authority: authority)


            checkAndSave(messageSource, v2DataModel)

            PrimitiveType V2PrimitiveType = new PrimitiveType(createdBy: DEVELOPMENT,
                                                              label: 'V2 Data Type')
            DataElement V2DataElement1 = new DataElement(createdBy: DEVELOPMENT,
                                                         label: 'V2 Data Element',
                                                         minMultiplicity: 1,
                                                         maxMultiplicity: 1,
                                                         dataType: V2PrimitiveType)
            DataElement V2DataElement2 = new DataElement(createdBy: DEVELOPMENT,
                                                         label: 'V2 Second DataElement',
                                                         minMultiplicity: 1,
                                                         maxMultiplicity: 1,
                                                         dataType: V2PrimitiveType)
            DataClass V2DataClass = new DataClass(createdBy: DEVELOPMENT,
                                                  label: 'V2 Data Class')

            V2DataClass.addToDataElements(V2DataElement1).addToDataElements(V2DataElement2)

            v2DataModel
                .addToDataClasses(V2DataClass)
                .addToDataClasses(createdBy: DEVELOPMENT, label: 'V2 Another Data Class')
                .addToDataTypes(V2PrimitiveType)

            checkAndSave(messageSource, v2DataModel)

            v2DataModel.finalised = true
            v2DataModel.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
            v2DataModel.breadcrumbTree.finalised = true
            v2DataModel.breadcrumbTree.updateTree()
            v2DataModel.modelVersion = Version.from('2.0.0')

            checkAndSave(messageSource, v2DataModel)

            return v2DataModel
        }
    }

    static void buildVersionTreeV2Branches(MessageSource messageSource, DataModelService dataModelService,
                                           DataModel v2DataModel, User dev, UserSecurityPolicyManager policyManager) {

        DataModel v2MainBranch = dataModelService.createNewBranchModelVersion('main', v2DataModel, dev, false, policyManager)
        checkAndSave(messageSource, v2MainBranch)

        DataModel anotherFork = dataModelService.createNewForkModel("V2 Main Fork", v2MainBranch, dev, false, policyManager)
        checkAndSave(messageSource, anotherFork)

        DataModel v2WorkingBranch = dataModelService.createNewBranchModelVersion('V2 Working Branch', v2DataModel, dev, false, policyManager)
        checkAndSave(messageSource, v2WorkingBranch)
    }
}
