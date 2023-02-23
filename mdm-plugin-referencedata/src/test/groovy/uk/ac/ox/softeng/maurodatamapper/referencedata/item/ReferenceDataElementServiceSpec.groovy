/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataTypeService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class ReferenceDataElementServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<ReferenceDataElementService> {

    UUID simpleId
    UUID contentId
    UUID childId
    UUID referenceDataModelId
    UUID elementId
    UUID element2Id
    UUID id

    ReferenceDataModel referenceDataModel

    def setup() {
        log.debug('Setting up DataElementServiceSpec Unit')
        mockArtefact(ReferenceDataTypeService)
        mockArtefact(ReferenceSummaryMetadataService)
        mockDomains(ReferenceDataModel, ReferenceDataType, ReferencePrimitiveType, ReferenceDataType, ReferenceEnumerationType, ReferenceEnumerationValue,
                    ReferenceDataElement)

        referenceDataModel = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(referenceDataModel)

        referenceDataModel.addToReferenceDataTypes(new ReferencePrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'string'))
        referenceDataModel.addToReferenceDataTypes(new ReferencePrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'integer'))

        ReferenceDataElement element = new ReferenceDataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'ele1',
                                                                referenceDataType: referenceDataModel.findReferenceDataTypeByLabel('string'))
        referenceDataModel.addToReferenceDataElements(element)

        referenceDataModel
            .addToReferenceDataElements(createdBy: StandardEmailAddress.UNIT_TEST, label: 'elex', referenceDataType: referenceDataModel.findReferenceDataTypeByLabel('string'))
        referenceDataModel.addToReferenceDataElements(createdBy: StandardEmailAddress.UNIT_TEST, label: 'element2', description: 'another',
                                                      referenceDataType: referenceDataModel.findReferenceDataTypeByLabel('integer'))
        referenceDataModel.addToReferenceDataElements(createdBy: StandardEmailAddress.UNIT_TEST, label: 'element3',
                                                      referenceDataType: referenceDataModel.findReferenceDataTypeByLabel('integer'),
                                                      maxMultiplicity: 1, minMultiplicity: 0)

        ReferenceDataElement el2 = new ReferenceDataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'another', minMultiplicity: 1, maxMultiplicity: 1,
                                                            referenceDataType: referenceDataModel.findReferenceDataTypeByLabel('integer'))

        referenceDataModel.addToReferenceDataElements(el2)

        checkAndSave(referenceDataModel)

        verifyBreadcrumbTrees()

        elementId = element.id
        referenceDataModelId = referenceDataModel.id
        element2Id = el2.id
        id = element.id
    }

    void 'test get'() {
        expect:
        service.get(id) != null
    }

    void 'test list'() {

        when:
        List<ReferenceDataElement> dataElementList = service.list(max: 2, offset: 2)

        then:
        dataElementList.size() == 2

        and:
        dataElementList[0].label == 'element2'
        dataElementList[0].minMultiplicity == null
        dataElementList[0].maxMultiplicity == null
        dataElementList[0].referenceDataTypeId == ReferenceDataType.findByLabel('integer').id

        and:
        dataElementList[1].label == 'element3'
        dataElementList[1].minMultiplicity == 0
        dataElementList[1].maxMultiplicity == 1
        dataElementList[1].referenceDataTypeId == ReferenceDataType.findByLabel('integer').id
    }

    void 'test count'() {

        expect:
        service.count() == 5
    }

    void 'test delete'() {

        expect:
        service.count() == 5

        when:
        service.delete(id)

        then:
        service.count() == 4
    }

    void 'test save'() {

        when:
        ReferenceDataElement dataElement = new ReferenceDataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'saving test', referenceDataModel: referenceDataModelId,
                                                                    referenceDataType: referenceDataModel.findReferenceDataTypeByLabel('string'))
        service.save(dataElement)

        then:
        dataElement.id != null

        when:
        ReferenceDataElement saved = service.get(dataElement.id)

        then:
        saved.breadcrumbTree
        saved.breadcrumbTree.domainId == saved.id
        saved.breadcrumbTree.parent.domainId == referenceDataModelId
    }


    void 'test copying ReferenceDataElement from one ReferenceDataModel to another'() {
        given:
        ReferenceDataModel copyModel = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'copy', folder: testFolder, authority: testAuthority)
        ReferenceDataElement original = service.get(id)

        expect:
        checkAndSave(copyModel)

        when:
        ReferenceDataElement copy = service.copyReferenceDataElement(referenceDataModel, original, editor, userSecurityPolicyManager)
        copyModel.addToReferenceDataElements(copy)

        then:
        checkAndSave(copyModel)

        when:
        original = service.get(id)
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        and:
        copy.createdBy == editor.emailAddress
        copy.metadata?.size() == original.metadata?.size()
        copy.classifiers == original.classifiers

        and:
        copy.referenceDataType.label == original.referenceDataType.label

        and:
        copy.semanticLinks.any {it.targetMultiFacetAwareItemId == original.id && it.linkType == SemanticLinkType.REFINES}
    }

    void 'test copying ReferenceDataElement with metadata and classifiers'() {
        given:
        ReferenceDataModel copyModel = new ReferenceDataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'copy', folder: testFolder, authority: testAuthority)
        ReferenceDataElement original = service.get(id)

        expect:
        checkAndSave(copyModel)

        when:
        ReferenceDataElement copy = service.copyReferenceDataElement(referenceDataModel, original, editor, userSecurityPolicyManager)
        copyModel.addToReferenceDataElements(copy)

        then:
        checkAndSave(copyModel)

        when:
        original = service.get(id)
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        and:
        copy.metadata.every { md ->
            original.metadata.any { md.namespace == it.namespace && md.key == it.key && md.value == it.value }
        }

        and:
        !copy.classifiers
        !original.classifiers

        and:
        copy.semanticLinks.any {it.targetMultiFacetAwareItemId == original.id && it.linkType == SemanticLinkType.REFINES}

    }
}
