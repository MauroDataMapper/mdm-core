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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class ReferenceEnumerationValueServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<ReferenceEnumerationValueService> {

    ReferenceDataModel referenceDataModel
    UUID id

    def setup() {
        log.debug('Setting up EnumerationValueServiceSpec Unit')
        mockArtefact(ReferenceSummaryMetadataService)
        mockDomains(ReferenceDataModel, ReferenceDataType, ReferencePrimitiveType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement)

        referenceDataModel = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(referenceDataModel)

        referenceDataModel.addToReferenceDataTypes(new ReferencePrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'string'))
        referenceDataModel.addToReferenceDataTypes(new ReferencePrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'integer'))
        ReferenceEnumerationValue ev1 = new ReferenceEnumerationValue(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key2', value: 'val2')
        ReferenceEnumerationType et1 = new ReferenceEnumerationType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'et1')
            .addToReferenceEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key1', value: 'val1')
            .addToReferenceEnumerationValues(ev1)
        referenceDataModel.addToReferenceDataTypes(et1)
        referenceDataModel.addToReferenceDataTypes(new ReferenceEnumerationType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'et2')
                                                       .addToReferenceEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key1', value: 'val1')
                                                       .addToReferenceEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key2', value: 'val2')
                                                       .addToReferenceEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key3', value: 'val3')
                                                       .addToReferenceEnumerationValues(createdBy: StandardEmailAddress.UNIT_TEST, key: 'key4', value: 'val4')
        )
        referenceDataModel.addToReferenceDataTypes(new ReferenceEnumerationType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'yesnounknown')
                                                       .addToReferenceEnumerationValues(key: 'Y', value: 'Yes')
                                                       .addToReferenceEnumerationValues(key: 'N', value: 'No')
                                                       .addToReferenceEnumerationValues(key: 'U', value: 'Unknown'))


        checkAndSave(referenceDataModel)

        id = ev1.id
    }

    void 'test get'() {

        expect:
        service.get(id) != null
    }

    void 'test list'() {

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

    void 'test count'() {
        expect:
        service.count() == 9
    }

    void 'test delete'() {

        expect:
        service.count() == 9
        ReferenceEnumerationValue ev = service.get(id)

        when:
        service.delete(ev)

        then:
        ReferenceEnumerationValue.count() == 8
    }

    void 'test save'() {

        when:
        ReferenceEnumerationValue referenceEnumerationValue = new ReferenceEnumerationValue(createdBy: StandardEmailAddress.UNIT_TEST, key: 'st', value: 'saving test',
                                                                                            referenceEnumerationType: ReferenceEnumerationType.findByLabel('yesnounknown'))
        service.save(referenceEnumerationValue)

        then:
        referenceEnumerationValue.id != null
    }
}
