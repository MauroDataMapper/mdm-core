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
package uk.ac.ox.softeng.maurodatamapper.datamodel.test

import uk.ac.ox.softeng.maurodatamapper.core.admin.AdminService
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
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import groovy.util.logging.Slf4j

/**
 * @since 03/03/2020
 */
@Slf4j
abstract class BaseDataModelIntegrationSpec extends BaseIntegrationSpec {


    AdminService adminService

    DataModel dataModel

    Folder getTestFolder() {
        folder
    }

    @Override
    void preDomainDataSetup() {
        folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)
    }

    DataModel buildSimpleDataModel() {
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost").save(flush: true)
        DataModel simpleDataModel = new DataModel(createdByUser: editor, label: 'Simple Test DataModel', folder: testFolder, authority: testAuthority)

        Classifier classifier = Classifier.findOrCreateWhere(createdBy: editor.emailAddress, label: 'test classifier simple')
        checkAndSave(classifier)

        simpleDataModel.addToClassifiers(classifier)

        checkAndSave(simpleDataModel)
        DataClass dataClass = new DataClass(createdByUser: editor, label: 'simple')

        simpleDataModel
            .addToMetadata(createdByUser: editor, namespace: 'test.com/simple', key: 'mdk1', value: 'mdv1')
            .addToMetadata(createdByUser: editor, namespace: 'test.com', key: 'mdk2', value: 'mdv2')

            .addToMetadata(createdByUser: editor, namespace: 'test.com/simple', key: 'mdk2', value: 'mdv2')

            .addToDataClasses(dataClass)

        checkAndSave(simpleDataModel)

        dataClass.addToMetadata(createdByUser: editor, namespace: 'test.com/simple', key: 'mdk1', value: 'mdv1')

        checkAndSave(simpleDataModel)

        simpleDataModel
    }

    DataModel buildComplexDataModel() {
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost").save(flush: true)
        DataModel dataModel = new DataModel(createdByUser: admin, label: 'Complex Test DataModel', organisation: 'brc', author: 'admin person',
                                            folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel)
        Classifier classifier = Classifier.findOrCreateWhere(createdBy: editor.emailAddress, label: 'test classifier')
        Classifier classifier1 = Classifier.findOrCreateWhere(createdBy: editor.emailAddress, label: 'test classifier2')
        checkAndSave(classifier)
        checkAndSave(classifier1)

        dataModel.addToClassifiers(classifier)
            .addToClassifiers(classifier1)

            .addToMetadata(createdByUser: admin, namespace: 'test.com', key: 'mdk1', value: 'mdv1')
            .addToMetadata(createdByUser: admin, namespace: 'test.com', key: 'mdk2', value: 'mdv2')

            .addToMetadata(createdByUser: editor, namespace: 'test.com/test', key: 'mdk1', value: 'mdv2')

            .addToAnnotations(createdByUser: admin, label: 'test annotation 1')

            .addToAnnotations(createdByUser: editor, label: 'test annotation 2', description: 'with description')

            .addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'string'))

            .addToDataTypes(new PrimitiveType(createdByUser: editor, label: 'integer'))

            .addToDataClasses(createdByUser: admin, label: 'emptyclass', description: 'dataclass with desc')
            .addToDataTypes(new EnumerationType(createdByUser: admin, label: 'yesnounknown')
                                .addToEnumerationValues(key: 'Y', value: 'Yes')
                                .addToEnumerationValues(key: 'N', value: 'No')
                                .addToEnumerationValues(key: 'U', value: 'Unknown'))

        DataClass parent =
            new DataClass(createdByUser: admin, label: 'parent', minMultiplicity: 1, maxMultiplicity: -1, dataModel: dataModel)
        DataClass child = new DataClass(createdByUser: admin, label: 'child')
        parent.addToDataClasses(child)
        dataModel.addToDataClasses(child)
        dataModel.addToDataClasses(parent)

        checkAndSave(dataModel)

        ReferenceType refType = new ReferenceType(createdByUser: editor, label: 'child')
        child.addToReferenceTypes(refType)
        dataModel.addToDataTypes(refType)

        DataElement el1 = new DataElement(createdByUser: editor, label: 'child', minMultiplicity: 1, maxMultiplicity: 1)
        refType.addToDataElements(el1)
        parent.addToDataElements(el1)

        DataClass content = new DataClass(createdByUser: editor, label: 'content', description: 'A dataclass with elements',
                                          minMultiplicity: 0, maxMultiplicity: 1)
        DataElement el2 = new DataElement(createdByUser: editor, label: 'ele1',
                                          minMultiplicity: 0, maxMultiplicity: 20)
        dataModel.findDataTypeByLabel('string').addToDataElements(el2)
        content.addToDataElements(el2)
        DataElement el3 = new DataElement(createdByUser: reader1, label: 'element2',
                                          minMultiplicity: 1, maxMultiplicity: 1)
        dataModel.findDataTypeByLabel('integer').addToDataElements(el3)
        content.addToDataElements(el3)
        dataModel.addToDataClasses(content)

        checkAndSave(dataModel)

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: editor,
                                             targetCatalogueItem: DataClass.findByLabel('parent'))
        DataClass.findByLabel('content').addToSemanticLinks(link)

        checkAndSave(dataModel)
        checkAndSave(link)

        dataModel
    }
}
