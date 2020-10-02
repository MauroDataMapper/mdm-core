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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration


import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.Stepwise

@Slf4j
@Stepwise
class ReferenceEnumerationValueServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<ReferenceEnumerationValueService> {

    DataModel dataModel
    UUID id

    def setup() {
        log.debug('Setting up EnumerationValueServiceSpec Unit')
        mockArtefact(ReferenceSummaryMetadataService)
        mockDomains(DataModel, DataClass, ReferenceDataType, ReferencePrimitiveType, ReferenceType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement)

        dataModel = new DataModel(createdByUser: admin, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel)

        dataModel.addToDataTypes(new ReferencePrimitiveType(createdByUser: admin, label: 'string'))
        dataModel.addToDataTypes(new ReferencePrimitiveType(createdByUser: editor, label: 'integer'))
        ReferenceEnumerationValue ev1 = new ReferenceEnumerationValue(createdByUser: admin, key: 'key2', value: 'val2')
        ReferenceEnumerationType et1 = new ReferenceEnumerationType(createdByUser: editor, label: 'et1')
            .addToReferenceEnumerationValues(createdByUser: admin, key: 'key1', value: 'val1')
            .addToReferenceEnumerationValues(ev1)
        dataModel.addToDataTypes(et1)
        dataModel.addToDataTypes(new ReferenceEnumerationType(createdByUser: editor, label: 'et2')
                                     .addToReferenceEnumerationValues(createdByUser: admin, key: 'key1', value: 'val1')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key2', value: 'val2')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key3', value: 'val3')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key4', value: 'val4')
        )
        dataModel.addToDataTypes(new ReferenceEnumerationType(createdByUser: admin, label: 'yesnounknown')
                                     .addToReferenceEnumerationValues(key: 'Y', value: 'Yes')
                                     .addToEnumerationValues(key: 'N', value: 'No')
                                     .addToEnumerationValues(key: 'U', value: 'Unknown'))

        DataClass dataClass = new DataClass(createdByUser: admin, label: 'dc1')
        dataModel.addToDataClasses(dataClass)
        DataClass parent = new DataClass(createdByUser: editor, label: 'Unit parent', dataModel: dataModel, minMultiplicity: 0,
                                         maxMultiplicity: 1)
        DataClass child = new DataClass
            (createdByUser: reader1, label: 'Unit child', minMultiplicity: 1, maxMultiplicity: -1)
        parent.addToDataClasses(child)
        DataClass added = new DataClass
            (createdByUser: reader1, label: 'added', description: 'a desc')
        dataModel.addToDataClasses(added)
        DataClass grandParent = new DataClass
            (createdByUser: editor, label: 'Unit grandparent')
        grandParent.addToDataClasses(parent)
        dataModel.addToDataClasses(grandParent)

        ReferenceType refType = new ReferenceType(createdByUser: editor, label: 'Unit parent')
        parent.addToReferenceTypes(refType)
        dataModel.addToDataTypes(refType)

        ReferenceDataElement el1 = new ReferenceDataElement
            (createdByUser: editor, label: 'parentel', minMultiplicity: 1, maxMultiplicity: 1, referenceDataType: refType)
        parent.addToDataElements(el1)

        ReferenceType refType2 = new ReferenceType(createdByUser: editor, label: 'dataclass')
        dataClass.addToReferenceTypes(refType2)
        dataModel.addToDataTypes(refType2)

        ReferenceDataElement el2 = new ReferenceDataElement
            (createdByUser: editor, label: 'childEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType2.addToDataElements(el2)
        child.addToDataElements(el2)

        ReferenceDataElement el3 = new ReferenceDataElement(createdByUser: editor, label: 'anotherParentEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType.addToDataElements(el3)
        added.addToDataElements(el3)

        checkAndSave(dataModel)

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: editor, targetCatalogueItem: dataClass)
        parent.addToSemanticLinks(link)

        checkAndSave(link)

        verifyBreadcrumbTrees()

        id = ev1.id

    }

    void "test get"() {

        expect:
        service.get(id) != null
    }

    void "test list"() {

        when:
        List<ReferenceEnumerationValue> enumerationValueList = service.list(max: 2, offset: 3)

        then:
        enumerationValueList.size() == 2

        and:
        enumerationValueList[0].key == 'key2'
        enumerationValueList[0].value == 'val2'

        and:
        enumerationValueList[1].key == 'key3'
        enumerationValueList[1].value == 'val3'
    }

    void "test count"() {
        expect:
        service.count() == 9
    }

    void "test delete"() {

        expect:
        service.count() == 9
        ReferenceEnumerationValue ev = service.get(id)

        when:
        service.delete(ev)

        then:
        ReferenceEnumerationValue.count() == 8
    }

    void "test save"() {

        when:
        ReferenceEnumerationValue enumerationValue = new ReferenceEnumerationValue(createdByUser: reader2, key: 'st', value: 'saving test',
                                                                 enumerationType: ReferenceEnumerationType.findByLabel('yesnounknown'))
        service.save(enumerationValue)

        then:
        enumerationValue.id != null
    }
}
