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
package uk.ac.ox.softeng.maurodatamapper.referencedata.bootstrap

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import org.springframework.context.MessageSource
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.DEVELOPMENT
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

class BootstrapModels {

    static String SIMPLE_REFERENCE_MODEL_NAME = "Simple Reference Data Model"

    static ReferenceDataModel buildAndSaveExampleReferenceDataModel(MessageSource messageSource, Folder folder, Authority authority) {
        ReferenceDataModel referenceDataModel = new ReferenceDataModel(createdBy: DEVELOPMENT, label: SIMPLE_REFERENCE_MODEL_NAME, folder: folder, authority: authority)



        Classifier classifier = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier simple',
                readableByAuthenticatedUsers: true)
        checkAndSave(messageSource, classifier)
        referenceDataModel.addToClassifiers(classifier)
        checkAndSave(messageSource, referenceDataModel)


        ReferenceDataType stringDataType = new ReferencePrimitiveType(createdBy: DEVELOPMENT, label: 'string')
        ReferenceDataType integerDataType = new ReferencePrimitiveType(createdBy: DEVELOPMENT, label: 'integer')
        referenceDataModel
                .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com/simple', key: 'mdk1', value: 'mdv1')
                .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com', key: 'mdk2', value: 'mdv2')
                .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com/simple', key: 'mdk2', value: 'mdv2')
                .addToReferenceDataTypes(stringDataType)
                .addToReferenceDataTypes(integerDataType)

        checkAndSave(messageSource, referenceDataModel)

        ReferenceDataElement organisationName = new ReferenceDataElement(referenceDataType: stringDataType, label: "Organisation name", createdBy: DEVELOPMENT)
        ReferenceDataElement organisationCode = new ReferenceDataElement(referenceDataType: stringDataType, label: "Organisation code", createdBy: DEVELOPMENT)
        referenceDataModel.addToReferenceDataElements(organisationName)
        referenceDataModel.addToReferenceDataElements(organisationCode)

        checkAndSave(messageSource, referenceDataModel)

        referenceDataModel
    }


}
