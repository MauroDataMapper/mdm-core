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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataTypeService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.Stepwise

@Slf4j
@Stepwise
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
        mockDomains(ReferenceDataModel, ReferenceDataType, ReferencePrimitiveType, ReferenceDataType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement)

        referenceDataModel = new ReferenceDataModel(createdByUser: admin, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(referenceDataModel)

        referenceDataModel.addToDataTypes(new ReferencePrimitiveType(createdByUser: admin, label: 'string'))
        referenceDataModel.addToDataTypes(new ReferencePrimitiveType(createdByUser: editor, label: 'integer'))

        ReferenceDataElement element = new ReferenceDataElement(createdByUser: admin, label: 'ele1', referenceDataType: referenceDataModel.findDataTypeByLabel('string'))
        simple.addToDataElements(element)

        content.addToDataElements(createdByUser: editor, label: 'ele1', dataType: referenceDataModel.findDataTypeByLabel('string'))
        content.addToDataElements(createdByUser: reader1, label: 'element2', description: 'another',
                                  dataType: referenceDataModel.findDataTypeByLabel('integer'))
        content.addToDataElements(createdByUser: reader1, label: 'element3', dataType: referenceDataModel.findDataTypeByLabel('integer'),
                                  maxMultiplicity: 1, minMultiplicity: 0)

        ReferenceDataElement el2 = new ReferenceDataElement(createdByUser: editor, label: 'another', minMultiplicity: 1, maxMultiplicity: 1,
                                          referenceDataType: referenceDataModel.findDataTypeByLabel('integer'))

        child.addToDataElements(el2)

        checkAndSave(referenceDataModel)

        verifyBreadcrumbTrees()

        elementId = element.id
        childId = child.id
        contentId = content.id
        simpleId = simple.id
        referenceDataModelId = referenceDataModel.id
        element2Id = el2.id
        id = element.id
    }

    void "test get"() {


        expect:
        service.get(id) != null
    }

    void "test list"() {

        when:
        List<ReferenceDataElement> dataElementList = service.list(max: 2, offset: 2)

        then:
        dataElementList.size() == 2

        and:
        dataElementList[0].label == 'element2'
        dataElementList[0].minMultiplicity == null
        dataElementList[0].maxMultiplicity == null
        dataElementList[0].dataTypeId == ReferenceDataType.findByLabel('integer').id

        and:
        dataElementList[1].label == 'element3'
        dataElementList[1].minMultiplicity == 0
        dataElementList[1].maxMultiplicity == 1
        dataElementList[1].dataTypeId == ReferenceDataType.findByLabel('integer').id
    }

    void "test count"() {

        expect:
        service.count() == 5
    }

    void "test delete"() {

        expect:
        service.count() == 5

        when:
        service.delete(id)

        then:
        service.count() == 4
    }

   /* void "test save"() {

        when:
        ReferenceDataElement dataElement = new ReferenceDataElement(createdByUser: reader2, label: 'saving test', 
                                                  referenceDataType: referenceDataModel.findDataTypeByLabel('string'))
        service.save(dataElement)

        then:
        dataElement.id != null

        when:
        ReferenceDataElement saved = service.get(dataElement.id)

        then:
        saved.breadcrumbTree
        saved.breadcrumbTree.domainId == saved.id
        saved.breadcrumbTree.parent.domainId == contentId
    }


    void 'test copying DataElement'() {
        given:
        ReferenceDataElement original = service.get(id)

        expect:
        checkAndSave(referenceDataModel)

        when:
        ReferenceDataElement copy = service.copyDataElement(referenceDataModel, original, editor, userSecurityPolicyManager)
        copyClass.addToDataElements(copy)

        then:
        checkAndSave(referenceDataModel)

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
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }

    void 'test copying DataElement with metadata and classifiers'() {
        given:
        ReferenceDataElement original = service.get(id)
        DataClass copyClass = new DataClass(label: 'copy', createdByUser: editor)
        referenceDataModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(referenceDataModel)

        when:
        ReferenceDataElement copy = service.copyDataElement(referenceDataModel, original, editor, userSecurityPolicyManager)
        copyClass.addToDataElements(copy)

        then:
        checkAndSave(copy)

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
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }

    }

    void 'test copying DataElement with datatype not present'() {
        given:
        ReferenceDataElement original = service.get(id)
        ReferenceDataModel copyModel = new ReferenceDataModel(createdByUser: admin, label: 'copy model', folder: testFolder, authority: testAuthority)
        DataClass copyClass = new DataClass(label: 'copy', createdByUser: editor)
        copyModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(copyModel)

        when:
        ReferenceDataElement copy = service.copyDataElement(copyModel, original, editor, userSecurityPolicyManager)
        copyClass.addToDataElements(copy)

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
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }*/
}
