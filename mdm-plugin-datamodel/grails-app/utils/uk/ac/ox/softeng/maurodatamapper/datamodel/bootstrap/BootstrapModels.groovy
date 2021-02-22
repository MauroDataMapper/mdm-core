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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.util.Version

import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.DEVELOPMENT
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

import java.time.OffsetDateTime
import java.time.ZoneOffset

class BootstrapModels {

    public static final String COMPLEX_DATAMODEL_NAME = 'Complex Test DataModel'
    public static final String SIMPLE_DATAMODEL_NAME = 'Simple Test DataModel'
    public static final String IMPORTING_DATAMODEL_NAME_1 = 'First Importing DataModel'
    public static final String IMPORTING_DATAMODEL_NAME_2 = 'Second Importing DataModel'
    public static final String IMPORTING_DATAMODEL_NAME_3 = 'Third Importing DataModel'
    public static final String FINALISED_EXAMPLE_DATAMODEL_NAME = 'Finalised Example Test DataModel'
    public static final String FINALISED_EXTENDABLE_DATAMODEL_NAME = 'Extendable DataM0del'
    public static final String EXTENDING_DATAMODEL_NAME_1 = 'Xtending DataM0del 1'
    public static final String EXTENDING_DATAMODEL_NAME_2 = 'Xtending DataM0del 2'
    public static final String FIRST_CLASS_LABEL_ON_FINALISED_EXAMPLE_DATAMODEL = 'first class on example finalised model'
    public static final String FIRST_CLASS_LABEL_ON_FINALISED_EXTENDABLE_DATAMODEL = 'extendable class on extendable data m0del'

    static DataModel buildAndSaveSimpleDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        DataModel simpleDataModel = DataModel.findByLabel(SIMPLE_DATAMODEL_NAME)

        if (!simpleDataModel) {
            simpleDataModel = new DataModel(createdBy: DEVELOPMENT, label: SIMPLE_DATAMODEL_NAME, folder: folder, authority: authority)

            Classifier classifier

            classifier = Classifier.findByLabel('test classifier simple')

            if (!classifier) {
                classifier = new Classifier(createdBy: DEVELOPMENT, label: 'test classifier simple', readableByAuthenticatedUsers: true)
                checkAndSave(messageSource, classifier)
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

        simpleDataModel
    }

    static DataModel buildAndSaveFirstImportingDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        DataModel dataModel = DataModel.findByLabel(IMPORTING_DATAMODEL_NAME_1)

        if (!dataModel) {
            dataModel = new DataModel(createdBy: DEVELOPMENT, label: IMPORTING_DATAMODEL_NAME_1, folder: folder, authority: authority)

            checkAndSave(messageSource, dataModel)

            DataClass dataClass = new DataClass(createdBy: DEVELOPMENT, label: 'importing class 1')

            dataModel.addToDataClasses(dataClass)

            checkAndSave(messageSource, dataModel)
        }

        dataModel
    }    

    static DataModel buildAndSaveSecondImportingDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        DataModel dataModel = DataModel.findByLabel(IMPORTING_DATAMODEL_NAME_2)
        
        if (!dataModel) {
            dataModel = new DataModel(createdBy: DEVELOPMENT, label: IMPORTING_DATAMODEL_NAME_2, folder: folder, authority: authority)

            checkAndSave(messageSource, dataModel)

            DataClass dataClass = new DataClass(createdBy: DEVELOPMENT, label: 'importing class 2')

            dataModel.addToDataClasses(dataClass)

            checkAndSave(messageSource, dataModel)
        }

        dataModel
    }   

    static DataModel buildAndSaveThirdImportingDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        DataModel dataModel = DataModel.findByLabel(IMPORTING_DATAMODEL_NAME_3)

        if (!dataModel) {
            dataModel = new DataModel(createdBy: DEVELOPMENT, label: IMPORTING_DATAMODEL_NAME_3, folder: folder, authority: authority)

            checkAndSave(messageSource, dataModel)

            DataClass dataClass = new DataClass(createdBy: DEVELOPMENT, label: 'importing class 3')

            dataModel.addToDataClasses(dataClass)

            checkAndSave(messageSource, dataModel)
        }

        dataModel
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
                                                 targetCatalogueItem: DataClass.findByLabel('parent'))
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
                                                             label: 'gnirts on finalised example data model')
            DataElement dataElement1 = new DataElement(createdBy: DEVELOPMENT,
                                                       label: 'data element 1',
                                                       minMultiplicity: 1,
                                                       maxMultiplicity: 1,
                                                       dataType: primitiveType1)         
            DataClass dataClass = new DataClass(createdBy: DEVELOPMENT,
                                                label: FIRST_CLASS_LABEL_ON_FINALISED_EXAMPLE_DATAMODEL)

            dataClass.addToDataElements(dataElement1)

            simpleDataModel
            .addToDataClasses(dataClass)
            .addToDataTypes(primitiveType1)
    
            checkAndSave(messageSource, simpleDataModel)

            simpleDataModel.finalised = true
            simpleDataModel.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
            simpleDataModel.breadcrumbTree.finalise()
            simpleDataModel.modelVersion = Version.from('1.0.0')     

            checkAndSave(messageSource, simpleDataModel)   
        }

        simpleDataModel
    }    

    static DataModel buildAndSaveFinalisedExtendableDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        DataModel dataModel = DataModel.findByLabel(FINALISED_EXTENDABLE_DATAMODEL_NAME)
        
        if (!dataModel) {
            dataModel = new DataModel(createdBy: DEVELOPMENT, label: FINALISED_EXTENDABLE_DATAMODEL_NAME, folder: folder, authority: authority)

            checkAndSave(messageSource, dataModel)

            PrimitiveType primitiveType1 = new PrimitiveType(createdBy: DEVELOPMENT, label: 'primitive type on extendable data m0del')
            DataElement dataElement1 = new DataElement(createdBy: DEVELOPMENT, label: 'data element 1', minMultiplicity: 1, maxMultiplicity: 1, dataType: primitiveType1)         
            DataClass dataClass = new DataClass(createdBy: DEVELOPMENT, label: FIRST_CLASS_LABEL_ON_FINALISED_EXTENDABLE_DATAMODEL)

            DataClass childDataClass = new DataClass(createdBy: DEVELOPMENT, label: 'i am a child class')

            dataClass
            .addToDataElements(dataElement1)
            .addToDataClasses(childDataClass)

            dataModel
            .addToDataClasses(dataClass)
            .addToDataTypes(primitiveType1)
        

            checkAndSave(messageSource, dataModel)

            dataModel.finalised = true
            dataModel.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
            dataModel.breadcrumbTree.finalise()
            dataModel.modelVersion = Version.from('1.0.0')     

            checkAndSave(messageSource, dataModel)   
        }

        dataModel
    }     

    static DataModel buildAndSaveFirstExtendingDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        DataModel dataModel = DataModel.findByLabel(EXTENDING_DATAMODEL_NAME_1)

        if (!dataModel) {
            dataModel = new DataModel(createdBy: DEVELOPMENT, label: EXTENDING_DATAMODEL_NAME_1, folder: folder, authority: authority)

            checkAndSave(messageSource, dataModel)

            PrimitiveType primitiveType = new PrimitiveType(createdBy: DEVELOPMENT, label: 'an example primitive type')
            dataModel.addToDataTypes(primitiveType)
            checkAndSave(messageSource, dataModel)

            DataClass dataClass = new DataClass(createdBy: DEVELOPMENT, label: 'extending class 1')

            dataModel.addToDataClasses(dataClass)

            DataElement dataElement = new DataElement(createdBy: DEVELOPMENT, label: 'on extending class 1', minMultiplicity: 1, maxMultiplicity: 1, dataType: primitiveType)
            dataClass.addToDataElements(dataElement)

            checkAndSave(messageSource, dataModel)
        }

        dataModel
    }      

}
