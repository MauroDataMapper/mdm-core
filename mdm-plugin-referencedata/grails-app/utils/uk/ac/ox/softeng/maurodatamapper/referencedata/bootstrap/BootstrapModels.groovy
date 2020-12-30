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
package uk.ac.ox.softeng.maurodatamapper.referencedata.bootstrap

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType

import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.DEVELOPMENT
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

@Slf4j
class BootstrapModels {

    static String SIMPLE_REFERENCE_MODEL_NAME = "Simple Reference Data Model"
    static String SECOND_SIMPLE_REFERENCE_MODEL_NAME = "Second Simple Reference Data Model"

    static ReferenceDataModel buildAndSaveExampleReferenceDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        ReferenceDataModel referenceDataModel = ReferenceDataModel.findByLabel(SIMPLE_REFERENCE_MODEL_NAME)
        if(!referenceDataModel) {
            referenceDataModel = new ReferenceDataModel(createdBy: DEVELOPMENT, label: SIMPLE_REFERENCE_MODEL_NAME,
                                   folder: folder, authority: authority)

            Classifier classifier = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier simple',
                    readableByAuthenticatedUsers: true)
            checkAndSave(messageSource, classifier)
            referenceDataModel.addToClassifiers(classifier)
            checkAndSave(messageSource, referenceDataModel)


            ReferenceDataType stringDataType = new ReferencePrimitiveType(createdBy: DEVELOPMENT, label: 'string')
            ReferenceDataType integerDataType = new ReferencePrimitiveType(createdBy: DEVELOPMENT, label: 'integer')
            referenceDataModel
                    .addToMetadata(createdBy: DEVELOPMENT, namespace: 'referencedata.com', key: 'mdk1', value: 'mdv1')
                    .addToMetadata(createdBy: DEVELOPMENT, namespace: 'referencedata.com', key: 'mdk2', value: 'mdv2')
                    .addToMetadata(createdBy: DEVELOPMENT, namespace: 'referencedata.com', key: 'mdk3', value: 'mdv3')
                    .addToReferenceDataTypes(stringDataType)
                    .addToReferenceDataTypes(integerDataType)

            checkAndSave(messageSource, referenceDataModel)

            ReferenceDataElement organisationName = new ReferenceDataElement(referenceDataType: stringDataType, label: "Organisation name", createdBy: DEVELOPMENT)
            ReferenceDataElement organisationCode = new ReferenceDataElement(referenceDataType: stringDataType, label: "Organisation code", createdBy: DEVELOPMENT)
            referenceDataModel.addToReferenceDataElements(organisationName)
            referenceDataModel.addToReferenceDataElements(organisationCode)

            checkAndSave(messageSource, referenceDataModel)

            (1..100).each {
                referenceDataModel.addToReferenceDataValues(new ReferenceDataValue(referenceDataElement: organisationName, value: "Organisation ${it}", rowNumber: it, createdBy: DEVELOPMENT))
                referenceDataModel.addToReferenceDataValues(new ReferenceDataValue(referenceDataElement: organisationCode, value: "ORG${it}", rowNumber: it, createdBy: DEVELOPMENT))
            }

            checkAndSave(messageSource, referenceDataModel)

            referenceDataModel.addToRules(name: "Bootstrapped Functional Test Rule",
                                          description: 'Functional Test Description',
                                          createdBy: DEVELOPMENT)

            organisationName.addToRules(name: "Bootstrapped Functional Test Rule",
                                        description: 'Functional Test Description',
                                        createdBy: DEVELOPMENT)

            stringDataType.addToRules(name: "Bootstrapped Functional Test Rule",
                                      description: 'Functional Test Description',
                                      createdBy: DEVELOPMENT)

            checkAndSave(messageSource, referenceDataModel)

        }
        referenceDataModel
    }

    static ReferenceDataModel buildAndSaveSecondExampleReferenceDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        ReferenceDataModel referenceDataModel = ReferenceDataModel.findByLabel(SECOND_SIMPLE_REFERENCE_MODEL_NAME)
        if(!referenceDataModel) {
            referenceDataModel = new ReferenceDataModel(createdBy: DEVELOPMENT, label: SECOND_SIMPLE_REFERENCE_MODEL_NAME, folder: folder, authority: authority)

            Classifier classifier = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier simple',
                    readableByAuthenticatedUsers: true)
            checkAndSave(messageSource, classifier)
            referenceDataModel.addToClassifiers(classifier)
            checkAndSave(messageSource, referenceDataModel)


            ReferenceDataType stringDataType = new ReferencePrimitiveType(createdBy: DEVELOPMENT, label: 'string')
            ReferenceDataType integerDataType = new ReferencePrimitiveType(createdBy: DEVELOPMENT, label: 'integer')
            referenceDataModel
                    .addToMetadata(createdBy: DEVELOPMENT, namespace: 'referencedata.com', key: 'mdk1', value: 'mdv1')
                    .addToMetadata(createdBy: DEVELOPMENT, namespace: 'referencedata.com', key: 'mdk2', value: 'mdv2')
                    .addToMetadata(createdBy: DEVELOPMENT, namespace: 'referencedata.com', key: 'mdk3', value: 'mdv3')
                    .addToReferenceDataTypes(stringDataType)
                    .addToReferenceDataTypes(integerDataType)

            checkAndSave(messageSource, referenceDataModel)

            ReferenceDataElement a = new ReferenceDataElement(referenceDataType: stringDataType, label: "Column A", createdBy: DEVELOPMENT)
            ReferenceDataElement b = new ReferenceDataElement(referenceDataType: stringDataType, label: "Column B", createdBy: DEVELOPMENT)
            ReferenceDataElement c = new ReferenceDataElement(referenceDataType: stringDataType, label: "Column C", createdBy: DEVELOPMENT)
            referenceDataModel.addToReferenceDataElements(a)
            referenceDataModel.addToReferenceDataElements(b)
            referenceDataModel.addToReferenceDataElements(c)

            checkAndSave(messageSource, referenceDataModel)

            (1..25).each {
                referenceDataModel.addToReferenceDataValues(new ReferenceDataValue(referenceDataElement: a, value: "A ${it}", rowNumber: it, createdBy: DEVELOPMENT))
                referenceDataModel.addToReferenceDataValues(new ReferenceDataValue(referenceDataElement: b, value: "B ${it}", rowNumber: it, createdBy: DEVELOPMENT))
                referenceDataModel.addToReferenceDataValues(new ReferenceDataValue(referenceDataElement: c, value: "C ${it}", rowNumber: it, createdBy: DEVELOPMENT))
            }

            checkAndSave(messageSource, referenceDataModel)

        }
        referenceDataModel
    }

}
