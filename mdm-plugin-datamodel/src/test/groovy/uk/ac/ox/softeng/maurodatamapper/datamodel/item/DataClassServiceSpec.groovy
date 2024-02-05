/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class DataClassServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<DataClassService> {

    DataModel dataModel
    DataModel targetDataModel
    DataElement el4
    UUID id

    def setup() {
        log.debug('Setting up DataClassServiceSpec Unit')
        mockArtefact(DataTypeService)
        mockArtefact(DataElementService)
        mockArtefact(SummaryMetadataService)
        mockArtefact(ApiPropertyService)
        mockDomains(DataModel, DataClass, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue, DataElement, ApiProperty)

        dataModel = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel)

        targetDataModel = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit test target model', folder: testFolder, authority:
            testAuthority)
        checkAndSave(targetDataModel)

        dataModel.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'string'))
        dataModel.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'integer'))

        DataClass dataClass = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'dc1')
        dataModel.addToDataClasses(dataClass)
        DataClass parent = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit parent', dataModel: dataModel, minMultiplicity: 0,
                                         maxMultiplicity: 1)
        DataClass child = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit child', minMultiplicity: 1, maxMultiplicity: -1)
        parent.addToDataClasses(child)
        DataClass added = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'added', description: 'a desc')
        dataModel.addToDataClasses(added)
        DataClass grandParent = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit grandparent')
        grandParent.addToDataClasses(parent)
        dataModel.addToDataClasses(grandParent)

        ReferenceType refType = new ReferenceType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit parent')
        parent.addToReferenceTypes(refType)
        dataModel.addToDataTypes(refType)

        DataElement el1 = new DataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'parentel', minMultiplicity: 1, maxMultiplicity: 1, dataType: refType)
        parent.addToDataElements(el1)

        ReferenceType refType2 = new ReferenceType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'dataclass')
        dataClass.addToReferenceTypes(refType2)
        dataModel.addToDataTypes(refType2)

        DataElement el2 = new DataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'childEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType2.addToDataElements(el2)
        child.addToDataElements(el2)

        DataElement el3 = new DataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'anotherParentEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType.addToDataElements(el3)
        added.addToDataElements(el3)

        ReferenceType refType3 = new ReferenceType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'reference to child')
        child.addToReferenceTypes(refType3)
        dataModel.addToDataTypes(refType3)

        el4 = new DataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'element with ref type', minMultiplicity: 1,
                                          maxMultiplicity: 1)
        refType3.addToDataElements(el4)
        child.addToDataElements(el4)

        checkAndSave(dataModel)

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdBy: StandardEmailAddress.UNIT_TEST, targetMultiFacetAwareItem: dataClass)
        parent.addToSemanticLinks(link)

        checkAndSave(link)

        service.dataElementService.dataTypeService = service.dataTypeService
        service.dataTypeService.dataElementService = service.dataElementService

        verifyBreadcrumbTrees()

        id = parent.id
    }

    void 'test get'() {

        expect:
        service.get(id) != null
    }

    void 'test list'() {

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


    void 'test count'() {
        expect:
        service.count() == 5
    }

    void 'test save'() {

        when:
        DataClass dataClass = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'saving test', dataModel: dataModel)
        service.save(dataClass)

        then:
        dataClass.id != null

        when:
        DataClass saved = service.get(dataClass.id)

        then:
        saved.breadcrumbTree
        saved.breadcrumbTree.domainId == saved.id
    }

    void 'test copy a data element which has a reference type'() {
        given:
        UnloggedUser user = UnloggedUser.instance

        when: 'Create a data class in the target model'
        DataClass targetDataClass = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'target class', dataModel: targetDataModel)
        service.save(targetDataClass)

        and: 'Copy the data element which has a reference type to the target data class, and add the target data class to target data model'
        DataElement copiedDataElement = service.dataElementService.copyDataElement(targetDataModel, el4, user, userSecurityPolicyManager)
        targetDataClass.addToDataElements(copiedDataElement)
        targetDataModel.addToDataClasses(targetDataClass)
        service.save(targetDataClass)

        then: 'The target model contains exactly one class (the target class)'
        targetDataModel.dataClasses.size() == 1
        targetDataModel.dataClasses.find{it.label == 'target class'}

        and: 'exactly one reference type (ref to child)'
        targetDataModel.referenceTypes.size() == 1
        targetDataModel.referenceTypes.find{it.label == 'reference to child'}

        and: 'exactly one data element'
        targetDataModel.getAllDataElements().size() == 1
        targetDataModel.getAllDataElements().find{it.label == 'element with ref type'}

        when:
        service.matchUpAndAddMissingReferenceTypeClasses(targetDataModel, dataModel, user, userSecurityPolicyManager)

        then:
        targetDataModel.getChildDataClasses().size() == 2
        targetDataModel.getChildDataClasses().find{it.label == 'target class'}
        targetDataModel.getChildDataClasses().find{it.label == 'Unit grandparent'}

        when:
        DataClass grandparent = targetDataModel.getChildDataClasses().find{it.label == 'Unit grandparent'}

        then:
        grandparent.dataClasses.size() == 1
        grandparent.dataClasses.find{it.label == 'Unit parent'}

        when:
        DataClass parent = grandparent.dataClasses.find{it.label == 'Unit parent'}

        then:
        parent.dataClasses.size() == 1
        parent.dataClasses.find{it.label == 'Unit child'}
    }
}
