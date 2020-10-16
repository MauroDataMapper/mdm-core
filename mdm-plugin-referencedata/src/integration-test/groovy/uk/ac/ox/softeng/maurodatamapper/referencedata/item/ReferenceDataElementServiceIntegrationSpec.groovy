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

import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.BaseReferenceDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Integration
@Rollback
@Slf4j
class ReferenceDataElementServiceIntegrationSpec extends BaseReferenceDataModelIntegrationSpec {

    UUID simpleId
    UUID contentId
    UUID childId
    UUID referenceDataModelId
    UUID elementId
    UUID element2Id

    ReferenceDataElementService dataElementService

    Boolean buildComplex = false

    ReferenceDataModel complexReferenceDataModel

    UserSecurityPolicyManager userSecurityPolicyManager

    void setupDomainData() {
        /*log.debug('Setting up DataElementServiceSpec')
        referenceDataModel = new ReferenceDataModel(createdByUser: admin, label: 'Integration test model', folder: testFolder, authority: testAuthority)
        checkAndSave(referenceDataModel)

        referenceDataModel.addToDataTypes(new ReferencePrimitiveType(createdByUser: admin, label: 'string'))
        referenceDataModel.addToDataTypes(new ReferencePrimitiveType(createdByUser: editor, label: 'integer'))

        DataClass simple = new DataClass(createdByUser: admin, label: 'dc1')
        ReferenceDataElement element = new ReferenceDataElement(createdByUser: admin, label: 'ele1', referenceDataType: referenceDataModel.findDataTypeByLabel('string'))
        simple.addToDataElements(element)
        referenceDataModel.addToDataClasses(simple)

        DataClass content = new DataClass(createdByUser: editor, label: 'content', description: 'A dataclass with elements')
        content.addToDataElements(createdByUser: editor, label: 'ele1', dataType: referenceDataModel.findDataTypeByLabel('string'))
        content.addToDataElements(createdByUser: reader1, label: 'element2', description: 'another',
                                  dataType: referenceDataModel.findDataTypeByLabel('integer'))
        content.addToDataElements(createdByUser: reader1, label: 'element3', dataType: referenceDataModel.findDataTypeByLabel('integer'),
                                  maxMultiplicity: 1, minMultiplicity: 0)
        DataClass child = new DataClass(createdByUser: editor, label: 'child')

        ReferenceDataElement el2 = new ReferenceDataElement(createdByUser: editor, label: 'another', minMultiplicity: 1, maxMultiplicity: 1,
                                          referenceDataType: referenceDataModel.findDataTypeByLabel('integer'))

        child.addToDataElements(el2)
        content.addToDataClasses(child)
        referenceDataModel.addToDataClasses(content)

        checkAndSave(referenceDataModel)
        elementId = element.id
        childId = child.id
        contentId = content.id
        simpleId = simple.id
        referenceDataModelId = referenceDataModel.id
        element2Id = el2.id

        if (buildComplex) complexReferenceDataModel = buildComplexReferenceDataModel()

        id = element.id*/
    }

    /*void "test get"() {
        setupData()

        expect:
        dataElementService.get(id) != null
    }

    void "test list"() {
        setupData()

        when:
        List<ReferenceDataElement> dataElementList = dataElementService.list(max: 2, offset: 2)

        then:
        dataElementList.size() == 2

        and:
        dataElementList[0].label == 'element2'
        dataElementList[0].dataClassId == DataClass.findByLabel('content').id
        dataElementList[0].minMultiplicity == null
        dataElementList[0].maxMultiplicity == null
        dataElementList[0].dataTypeId == ReferenceDataType.findByLabel('integer').id

        and:
        dataElementList[1].label == 'element3'
        dataElementList[1].dataClassId == DataClass.findByLabel('content').id
        dataElementList[1].minMultiplicity == 0
        dataElementList[1].maxMultiplicity == 1
        dataElementList[1].dataTypeId == ReferenceDataType.findByLabel('integer').id
    }

    void "test count"() {
        setupData()

        expect:
        dataElementService.count() == 5
    }

    void "test delete"() {
        setupData()

        expect:
        dataElementService.count() == 5
        BreadcrumbTree.findByDomainType('DataElement').any { it.domainId == id }

        when:
        dataElementService.delete(id)
        sessionFactory.currentSession.flush()

        then:
        dataElementService.count() == 4
        BreadcrumbTree.findByDomainType('DataElement').every { it.domainId != id }
    }

    void 'test findByDataClassIdAndId'() {
        given:
        setupData()

        expect:
        dataElementService.findByDataClassIdAndId(UUID.randomUUID(), UUID.randomUUID())?.id == null
        dataElementService.findByDataClassIdAndId(simpleId, UUID.randomUUID())?.id == null
        dataElementService.findByDataClassIdAndId(simpleId, elementId)?.id == elementId
        dataElementService.findByDataClassIdAndId(contentId, elementId)?.id == null
        dataElementService.findByDataClassIdAndId(contentId, element2Id)?.id == null
        dataElementService.findByDataClassIdAndId(childId, element2Id)?.id == element2Id
    }

    void 'test findByDataTypeIdAndId'() {
        given:
        setupData()

        expect:
        dataElementService.findByDataTypeIdAndId(UUID.randomUUID(), UUID.randomUUID())?.id == null
        dataElementService.findByDataTypeIdAndId(referenceDataModel.findDataTypeByLabel('string').id, UUID.randomUUID())?.id == null
        dataElementService.findByDataTypeIdAndId(referenceDataModel.findDataTypeByLabel('string').id, elementId)?.id == elementId
        dataElementService.findByDataTypeIdAndId(referenceDataModel.findDataTypeByLabel('integer').id, elementId)?.id == null
        dataElementService.findByDataTypeIdAndId(referenceDataModel.findDataTypeByLabel('integer').id, element2Id)?.id == element2Id
    }

    void 'test findAllByDataClassId'() {
        given:
        setupData()

        expect:
        dataElementService.findAllByDataClassId(UUID.randomUUID()).isEmpty()
        dataElementService.findAllByDataClassId(simpleId).size() == 1
        dataElementService.findAllByDataClassId(elementId).isEmpty()
        dataElementService.findAllByDataClassId(contentId).size() == 3
        dataElementService.findAllByDataClassId(childId).size() == 1
    }

    void 'test findAllByDataTypeId'() {
        given:
        setupData()

        expect:
        dataElementService.findAllByDataTypeId(UUID.randomUUID()).isEmpty()
        dataElementService.findAllByDataTypeId(referenceDataModel.findDataTypeByLabel('string').id).size() == 2
        dataElementService.findAllByDataTypeId(elementId).isEmpty()
        dataElementService.findAllByDataTypeId(referenceDataModel.findDataTypeByLabel('integer').id).size() == 3
        dataElementService.findAllByDataTypeId(childId).isEmpty()
    }

    void 'test findAllByReferenceDataModelId'() {
        given:
        setupData()
        ReferenceDataModel other = new ReferenceDataModel(createdByUser: admin, label: 'anotherModel', folder: testFolder, authority: testAuthority)
        other.addToDataTypes(new ReferencePrimitiveType(createdByUser: admin, label: 'string'))
        other.addToDataTypes(new ReferencePrimitiveType(createdByUser: editor, label: 'integer'))
        DataClass simple = new DataClass(createdByUser: admin, label: 'dc1')
        other.addToDataClasses(simple)

        expect:
        checkAndSave(other)

        when:
        simple.addToDataElements(createdByUser: admin, label: 'ele1', dataType: other.findDataTypeByLabel('string'))

        then:
        checkAndSave(other)
        dataElementService.findAllByReferenceDataModelId(UUID.randomUUID()).isEmpty()
        dataElementService.findAllByReferenceDataModelId(referenceDataModel.id).size() == 5
        dataElementService.findAllByReferenceDataModelId(other.id).size() == 1
        dataElementService.findAllByReferenceDataModelId(elementId).isEmpty()
    }

    void 'test findAllByReferenceDataModelIdAndLabelIlike'() {
        given:
        setupData()
        ReferenceDataModel other = new ReferenceDataModel(createdByUser: admin, label: 'anotherModel', folder: testFolder, authority: testAuthority)
        other.addToDataTypes(new ReferencePrimitiveType(createdByUser: admin, label: 'string'))
        other.addToDataTypes(new ReferencePrimitiveType(createdByUser: editor, label: 'integer'))

        DataClass simple = new DataClass(createdByUser: admin, label: 'dc1')
        ReferenceDataElement element = new ReferenceDataElement(createdByUser: admin, label: 'element', referenceDataType: other.findDataTypeByLabel('string'))
        simple.addToDataElements(element)
        other.addToDataClasses(simple)
        checkAndSave(other)

        expect:
        dataElementService.findAllByReferenceDataModelIdAndLabelIlike(UUID.randomUUID(), '').isEmpty()
        dataElementService.findAllByReferenceDataModelIdAndLabelIlike(referenceDataModel.id, '').size() == 5
        dataElementService.findAllByReferenceDataModelIdAndLabelIlike(other.id, '').size() == 1
        dataElementService.findAllByReferenceDataModelIdAndLabelIlike(elementId, '').isEmpty()

        dataElementService.findAllByReferenceDataModelIdAndLabelIlike(UUID.randomUUID(), 'element').isEmpty()
        dataElementService.findAllByReferenceDataModelIdAndLabelIlike(referenceDataModel.id, 'element').size() == 2
        dataElementService.findAllByReferenceDataModelIdAndLabelIlike(other.id, 'element').size() == 1
        dataElementService.findAllByReferenceDataModelIdAndLabelIlike(elementId, 'element').isEmpty()
    }

    void 'test copying DataElement'() {
        given:
        setupData()
        ReferenceDataElement original = dataElementService.get(id)
        DataClass copyClass = new DataClass(label: 'copy', createdByUser: editor)
        referenceDataModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(referenceDataModel)

        when:
        ReferenceDataElement copy = dataElementService.copyDataElement(referenceDataModel, original, editor, userSecurityPolicyManager)
        copyClass.addToDataElements(copy)

        then:
        checkAndSave(referenceDataModel)

        when:
        original = dataElementService.get(id)
        copy = dataElementService.get(copy.id)

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
        setupData()
        ReferenceDataElement original = dataElementService.get(id)
        DataClass copyClass = new DataClass(label: 'copy', createdByUser: editor)
        referenceDataModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(referenceDataModel)

        when:
        ReferenceDataElement copy = dataElementService.copyDataElement(referenceDataModel, original, admin, userSecurityPolicyManager)
        copyClass.addToDataElements(copy)

        then:
        checkAndSave(copy)

        when:
        original = dataElementService.get(id)
        copy = dataElementService.get(copy.id)

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
        copy.classifiers == original.classifiers

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }

    }

    void 'test copying DataElement with datatype not present'() {
        given:
        setupData()
        ReferenceDataElement original = dataElementService.get(id)
        ReferenceDataModel copyModel = new ReferenceDataModel(createdByUser: admin, label: 'copy model', folder: testFolder, authority: testAuthority)
        DataClass copyClass = new DataClass(label: 'copy', createdByUser: editor)
        copyModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(copyModel)

        when:
        ReferenceDataElement copy = dataElementService.copyDataElement(copyModel, original, editor, userSecurityPolicyManager)
        copyClass.addToDataElements(copy)

        then:
        checkAndSave(copyModel)

        when:
        original = dataElementService.get(id)
        copy = dataElementService.get(copy.id)

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

    void 'test finding all similar DataElements in another model'() {
        given:
        buildComplex = true
        setupData()
        ReferenceDataElement original = dataElementService.get(id)

        when:
        DataElementSimilarityResult result = dataElementService.findAllSimilarDataElementsInReferenceDataModel(complexReferenceDataModel, original)

        then:
        result.size() == 1
        result.first().item.label == 'ele1'
        result.first().item.id != elementId
        result.first().similarity > 0
    }*/
}
