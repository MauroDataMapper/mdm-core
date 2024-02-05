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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValueService
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

@Slf4j
class DataTypeServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<DataTypeService> {

    DataModel dataModel
    UUID id

    def setup() {
        log.debug('Setting up DataTypeServiceSpec Unit')
        mockArtefact(DataElementService)
        mockArtefact(ReferenceTypeService)
        mockArtefact(PrimitiveTypeService)
        mockArtefact(EnumerationTypeService)
        mockArtefact(EnumerationValueService)
        mockArtefact(SummaryMetadataService)
        mockArtefact(ApiPropertyService)
        mockDomains(DataModel, DataClass, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue, DataElement, ApiProperty)

        dataModel = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel)

        PrimitiveType primitiveType = new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'varchar')

        dataModel.addToDataTypes(primitiveType)
        dataModel.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'string'))
        dataModel.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'integer'))

        EnumerationType et1 = new EnumerationType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'et1')
            .addToEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key1', value: 'val1')
            .addToEnumerationValues(new EnumerationValue(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key2', value: 'val2')
            )
        dataModel.addToDataTypes(et1)
        dataModel.addToDataTypes(new EnumerationType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'moreet')
                                     .addToEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key1', value: 'val1')
                                     .addToEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key2', value: 'val2')
                                     .addToEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key3', value: 'val3')
                                     .addToEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key4', value: 'val4')
        )
        dataModel.addToDataTypes(new EnumerationType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'yesnounknown')
                                     .addToEnumerationValues(key: 'Y', value: 'Yes')
                                     .addToEnumerationValues(key: 'N', value: 'No')
                                     .addToEnumerationValues(key: 'U', value: 'Unknown'))

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

        DataElement el1 = new DataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'parentel', minMultiplicity: 1, maxMultiplicity: 1)
        refType.addToDataElements(el1)
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

        DataElement el4 = new DataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'varcharel', minMultiplicity: 1, maxMultiplicity: 1)
        primitiveType.addToDataElements(el4)
        added.addToDataElements(el4)

        checkAndSave(dataModel)

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdBy: StandardEmailAddress.UNIT_TEST, targetMultiFacetAwareItem: dataClass)
        parent.addToSemanticLinks(link)

        checkAndSave(link)

        verifyBreadcrumbTrees()

        id = primitiveType.id

    }

    void 'test get'() {

        expect:
        service.get(id) != null
    }

    void 'test list'() {

        when:
        List<DataType> dataTypeList = service.list(max: 2, offset: 2, sort: 'label')

        then:
        dataTypeList.size() == 2

        and:
        dataTypeList[0].label == 'et1'
        dataTypeList[0].domainType == 'EnumerationType'

        and:
        dataTypeList[1].label == 'integer'
        dataTypeList[1].domainType == 'PrimitiveType'
    }

    void 'test count'() {
        expect:
        service.count() == 8
    }

    void 'test delete'() {
        given:
        DataType pt = service.get(id)

        expect:
        service.count() == 8
        DataElement.findByLabel('varcharel')

        when:
        service.delete(pt, true)

        then:
        DataType.count() == 7

        and:
        !DataElement.findByLabel('varcharel')
    }

    void 'test save'() {

        when:
        DataType dataType = new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'saving test', dataModel: dataModel)
        service.save(dataType)

        then:
        dataType.id != null
    }

    void 'test copying primitive datatype'() {
        given:
        DataType original = PrimitiveType.findByLabel('string')
        DataModel copyModel = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'copy model', folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(copyModel)

        when:
        DataType copy = service.copyDataType(copyModel, original, editor, userSecurityPolicyManager)

        then:
        checkAndSave(copyModel)

        when:
        original = PrimitiveType.findByLabel('string')
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.units == original.units

        and:
        copy.createdBy == editor.emailAddress
        !copy.metadata
        !original.metadata
        !copy.classifiers
        !original.classifiers

        and:
        copy.semanticLinks.any {it.targetMultiFacetAwareItemId == original.id && it.linkType == SemanticLinkType.REFINES}
    }

    void 'test copying enumeration datatype'() {
        given:
        DataType original = EnumerationType.findByLabel('yesnounknown')
        DataModel copyModel = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'copy model', folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(copyModel)

        when:
        DataType copy = service.copyDataType(copyModel, original, editor, userSecurityPolicyManager)

        then:
        checkAndSave(copyModel)

        when:
        original = EnumerationType.findByLabel('yesnounknown')
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.enumerationValues.size() == original.enumerationValues.size()

        and:
        original.enumerationValues.every { o -> copy.enumerationValues.any { c -> c.key == o.key && c.value == o.value } }

        and:
        copy.createdBy == editor.emailAddress
        !copy.metadata
        !original.metadata
        !copy.classifiers
        !original.classifiers

        and:
        copy.semanticLinks.any {it.targetMultiFacetAwareItemId == original.id && it.linkType == SemanticLinkType.REFINES}
    }

    void 'test copying reference datatype'() {
        given:
        DataType original = ReferenceType.findByLabel('dataclass')
        DataModel copyModel = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'copy model', folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(copyModel)

        when:
        DataType copy = service.copyDataType(copyModel, original, editor, userSecurityPolicyManager)
        checkAndSave(copyModel)

        then:
        thrown(InternalSpockError)

        and:
        copyModel.errors.getFieldError('referenceTypes[0].referenceClass').code == 'nullable'

        and:
        copy.label == original.label
        copy.description == original.description

        and:
        copy.createdBy == editor.emailAddress
        !copy.metadata
        !original.metadata
        !copy.classifiers
        !original.classifiers

        and:
        copy.semanticLinks.any {it.targetMultiFacetAwareItemId == original.id && it.linkType == SemanticLinkType.REFINES}
    }
}
