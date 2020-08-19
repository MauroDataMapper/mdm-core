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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item


import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.Stepwise

@Slf4j
@Stepwise
class DataClassServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<DataClassService> {

    DataModel dataModel
    UUID id

    def setup() {
        log.debug('Setting up DataClassServiceSpec Unit')
        mockArtefact(SemanticLinkService)
        mockArtefact(DataTypeService)
        mockArtefact(DataElementService)
        mockDomains(DataModel, DataClass, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue, DataElement)

        dataModel = new DataModel(createdByUser: admin, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel)

        dataModel.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'string'))
        dataModel.addToDataTypes(new PrimitiveType(createdByUser: editor, label: 'integer'))

        DataClass dataClass = new DataClass(createdByUser: admin, label: 'dc1')
        dataModel.addToDataClasses(dataClass)
        DataClass parent = new DataClass(createdByUser: editor, label: 'Unit parent', dataModel: dataModel, minMultiplicity: 0,
                                         maxMultiplicity: 1)
        DataClass child = new DataClass(createdByUser: reader1, label: 'Unit child', minMultiplicity: 1, maxMultiplicity: -1)
        parent.addToDataClasses(child)
        DataClass added = new DataClass(createdByUser: reader1, label: 'added', description: 'a desc')
        dataModel.addToDataClasses(added)
        DataClass grandParent = new DataClass(createdByUser: editor, label: 'Unit grandparent')
        grandParent.addToDataClasses(parent)
        dataModel.addToDataClasses(grandParent)

        ReferenceType refType = new ReferenceType(createdByUser: editor, label: 'Unit parent')
        parent.addToReferenceTypes(refType)
        dataModel.addToDataTypes(refType)

        DataElement el1 = new DataElement(createdByUser: editor, label: 'parentel', minMultiplicity: 1, maxMultiplicity: 1, dataType: refType)
        parent.addToDataElements(el1)

        ReferenceType refType2 = new ReferenceType(createdByUser: editor, label: 'dataclass')
        dataClass.addToReferenceTypes(refType2)
        dataModel.addToDataTypes(refType2)

        DataElement el2 = new DataElement(createdByUser: editor, label: 'childEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType2.addToDataElements(el2)
        child.addToDataElements(el2)

        DataElement el3 = new DataElement(createdByUser: editor, label: 'anotherParentEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType.addToDataElements(el3)
        added.addToDataElements(el3)

        checkAndSave(dataModel)

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: editor, targetCatalogueItem: dataClass)
        parent.addToSemanticLinks(link)

        checkAndSave(link)

        service.dataElementService.dataTypeService = service.dataTypeService
        service.dataTypeService.dataElementService = service.dataElementService

        verifyBreadcrumbTrees()

        id = parent.id
    }

    void "test get"() {

        expect:
        service.get(id) != null
    }

    void "test list"() {

        when:
        List<DataClass> dataClassList = service.list(max: 2, offset: 3)

        then:
        dataClassList.size() == 2

        and:
        dataClassList[0].label == 'Unit parent'
        dataClassList[0].minMultiplicity == 0
        dataClassList[0].maxMultiplicity == 1
        dataClassList[0].dataClasses.size() == 1
        dataClassList[0].dataClasses.find { it.label == 'Unit child' }

        and:
        dataClassList[1].label == 'Unit grandparent'
        dataClassList[1].dataClasses.size() == 1
        dataClassList[1].dataClasses.find { it.label == 'Unit parent' }
    }


    void "test count"() {
        expect:
        service.count() == 5
    }

    void "test delete child dataclass"() {
        given:
        def dataClass = DataClass.findByLabel('Unit child')

        expect:
        SemanticLink.count() == 1
        DataElement.count() == 3
        ReferenceType.count() == 2
        service.count() == 5

        when:
        log.info('Attempting to delete id {}', dataClass.id)
        service.delete(dataClass)

        then:
        service.count() == 4
        SemanticLink.count() == 1
        DataElement.count() == 2
        ReferenceType.count() == 2
    }

    void "test save"() {

        when:
        DataClass dataClass = new DataClass(createdByUser: reader2, label: 'saving test', dataModel: dataModel)
        service.save(dataClass)

        then:
        dataClass.id != null

        when:
        DataClass saved = service.get(dataClass.id)

        then:
        saved.breadcrumbTree
        saved.breadcrumbTree.domainId == saved.id
    }

    void 'test copying very simple DataClass'() {
        when:
        DataClass vsimple = new DataClass(label: 'vsimple', createdByUser: editor)
        dataModel.addToDataClasses(vsimple)
        DataModel copyModel = new DataModel(label: 'copy', createdByUser: editor, folder: testFolder, authority: testAuthority)

        then:
        checkAndSave(dataModel)
        checkAndSave(copyModel)

        when:
        DataClass original = service.get(vsimple.id)
        DataClass copy = service.copyDataClass(copyModel, original, editor)

        then:
        checkAndSave(copyModel)

        when:
        original = service.get(vsimple.id)
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        copy.createdBy == editor.emailAddress
        !copy.metadata
        !original.metadata
        !copy.classifiers
        !original.classifiers

        and:
        !copy.dataClasses
        !copy.dataElements
        !copy.referenceTypes

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }

    void 'test copying simple DataClass with simple data elements'() {
        given:
        DataClass content = new DataClass(createdByUser: admin, label: 'content', minMultiplicity: 1, maxMultiplicity: 1)
        content.addToDataClasses(label: 'vsimple', createdByUser: editor, minMultiplicity: 0, maxMultiplicity: 1)
        DataElement element = new DataElement(createdByUser: admin, label: 'ele1', dataType: dataModel.findDataTypeByLabel('string'))
        content.addToDataElements(element)
        dataModel.addToDataClasses(content)
        DataModel copyModel = new DataModel(label: 'copy', createdByUser: editor, folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(dataModel)
        checkAndSave(copyModel)

        when:
        copyModel.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'string'))
        copyModel.addToDataTypes(new PrimitiveType(createdByUser: editor, label: 'integer'))

        then:
        checkAndSave(copyModel)

        when:
        DataClass original = service.get(content.id)
        DataClass copy = service.copyDataClass(copyModel, original, editor)

        then:
        checkAndSave(copyModel)

        when:
        original = service.get(content.id)
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        copy.createdBy == editor.emailAddress
        !copy.metadata
        !original.metadata
        !copy.classifiers
        !original.classifiers

        and:
        copy.dataClasses.size() == 1
        copy.dataClasses.size() == original.dataClasses.size()
        copy.dataElements.size() == 1
        copy.dataElements.size() == original.dataElements.size()

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }

    void 'test copying complex dataclass'() {
        given:
        DataClass complex = dataModel.dataClasses.find { it.label == 'Unit grandparent' }

        DataModel copyModel = new DataModel(label: 'copy', createdByUser: editor, folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(copyModel)

        when:
        service.copyDataClass(copyModel, dataModel.childDataClasses.find { it.label == 'dc1' }, editor)

        then:
        checkAndSave(copyModel)

        when:
        DataClass original = service.get(complex.id)
        DataClass copy = service.copyDataClassMatchingAllReferenceTypes(copyModel, original, editor, null, null)

        then:
        checkAndSave(copyModel)

        when:
        original = service.get(complex.id)
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        copy.createdBy == editor.emailAddress
        !copy.metadata
        !original.metadata
        !copy.classifiers
        !original.classifiers

        and:
        copy.dataClasses
        copy.dataClasses.size() == 1
        !copy.dataElements
        !copy.referenceTypes

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }

        when:
        DataClass copiedParent = copy.dataClasses.first()

        then:
        copiedParent
        copiedParent.label == 'Unit parent'
        copiedParent.dataClasses.size() == 1
        copiedParent.dataClasses.find { it.label == 'Unit child' }
        copiedParent.dataElements.size() == 1
        copiedParent.referenceTypes.size() == 1
        copiedParent.referenceTypes.find { it.label == 'Unit parent' }

        when:
        ReferenceType referenceType = copyModel.dataTypes.find { it.label == 'Unit parent' } as ReferenceType

        then:
        referenceType
        referenceType.referenceClass == copiedParent

        when:
        DataElement copiedElement = copiedParent.dataElements.find { it.label == 'parentel' }

        then:
        copiedElement
        copiedElement.dataType == referenceType
    }
}
