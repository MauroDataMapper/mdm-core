/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class EnumerationValueServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<EnumerationValueService> {

    DataModel dataModel
    UUID id

    def setup() {
        log.debug('Setting up EnumerationValueServiceSpec Unit')
        mockArtefact(SummaryMetadataService)
        mockDomains(DataModel, DataClass, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue, DataElement)

        dataModel = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel)

        dataModel.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'string'))
        dataModel.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'integer'))
        EnumerationValue ev1 = new EnumerationValue(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key2', value: 'val2')
        EnumerationType et1 = new EnumerationType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'et1')
            .addToEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key1', value: 'val1')
            .addToEnumerationValues(ev1)
        dataModel.addToDataTypes(et1)
        dataModel.addToDataTypes(new EnumerationType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'et2')
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
        DataClass child = new DataClass
            (createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit child', minMultiplicity: 1, maxMultiplicity: -1)
        parent.addToDataClasses(child)
        DataClass added = new DataClass
            (createdBy: StandardEmailAddress.UNIT_TEST, label: 'added', description: 'a desc')
        dataModel.addToDataClasses(added)
        DataClass grandParent = new DataClass
            (createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit grandparent')
        grandParent.addToDataClasses(parent)
        dataModel.addToDataClasses(grandParent)

        ReferenceType refType = new ReferenceType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit parent')
        parent.addToReferenceTypes(refType)
        dataModel.addToDataTypes(refType)

        DataElement el1 = new DataElement
            (createdBy: StandardEmailAddress.UNIT_TEST, label: 'parentel', minMultiplicity: 1, maxMultiplicity: 1, dataType: refType)
        parent.addToDataElements(el1)

        ReferenceType refType2 = new ReferenceType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'dataclass')
        dataClass.addToReferenceTypes(refType2)
        dataModel.addToDataTypes(refType2)

        DataElement el2 = new DataElement
            (createdBy: StandardEmailAddress.UNIT_TEST, label: 'childEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType2.addToDataElements(el2)
        child.addToDataElements(el2)

        DataElement el3 = new DataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'anotherParentEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType.addToDataElements(el3)
        added.addToDataElements(el3)

        checkAndSave(dataModel)

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdBy: StandardEmailAddress.UNIT_TEST, targetMultiFacetAwareItem: dataClass)
        parent.addToSemanticLinks(link)

        checkAndSave(link)

        verifyBreadcrumbTrees()

        id = ev1.id

    }

    void 'test get'() {

        expect:
        service.get(id) != null
    }

    void 'test list'() {

        when:
        List<EnumerationValue> enumerationValueList = service.list(max: 2, offset: 3)

        then:
        enumerationValueList.size() == 2

        and:
        enumerationValueList[0].key == 'key2'
        enumerationValueList[0].value == 'val2'

        and:
        enumerationValueList[1].key == 'key3'
        enumerationValueList[1].value == 'val3'
    }

    void 'test count'() {
        expect:
        service.count() == 9
    }

    void 'test delete'() {

        expect:
        service.count() == 9
        EnumerationValue ev = service.get(id)

        when:
        service.delete(ev)

        then:
        EnumerationValue.count() == 8
    }

    void 'test save'() {

        when:
        EnumerationValue enumerationValue = new EnumerationValue(createdBy: StandardEmailAddress.UNIT_TEST, key: 'st', value: 'saving test',
                                                                 enumerationType: EnumerationType.findByLabel('yesnounknown'))
        service.save(enumerationValue)

        then:
        enumerationValue.id != null
    }
}
