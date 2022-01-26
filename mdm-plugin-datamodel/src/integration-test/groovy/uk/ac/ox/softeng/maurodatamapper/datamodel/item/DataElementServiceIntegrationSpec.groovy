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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.Tag

@Integration
@Rollback
@Slf4j
class DataElementServiceIntegrationSpec extends BaseDataModelIntegrationSpec {

    UUID simpleId
    UUID contentId
    UUID childId
    UUID dataModelId
    UUID elementId
    UUID element2Id

    DataElementService dataElementService

    Boolean buildComplex = false

    DataModel complexDataModel

    UserSecurityPolicyManager userSecurityPolicyManager

    void setupDomainData() {
        log.debug('Setting up DataElementServiceSpec')
        dataModel = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'Integration test model', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel)

        dataModel.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'string'))
        dataModel.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'integer'))
        checkAndSave(dataModel)

        DataClass simple = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'dc1')
        DataElement element = new DataElement(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'ele1', dataType: dataModel.findDataTypeByLabel('string'))
        simple.addToDataElements(element)
        dataModel.addToDataClasses(simple)
        checkAndSave(dataModel)

        DataClass content = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'content', description: 'A dataclass with elements')
        content.addToDataElements(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'ele1', dataType: dataModel.findDataTypeByLabel('string'))
        content.addToDataElements(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'element2', description: 'another',
                                  dataType: dataModel.findDataTypeByLabel('integer'))
        content.addToDataElements(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'element3', dataType: dataModel.findDataTypeByLabel('integer'),
                                  maxMultiplicity: 1, minMultiplicity: 0)
        DataClass child = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'child')

        DataElement el2 = new DataElement(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'another', minMultiplicity: 1, maxMultiplicity: 1,
                                          dataType: dataModel.findDataTypeByLabel('integer'))

        child.addToDataElements(el2)
        content.addToDataClasses(child)
        dataModel.addToDataClasses(content)

        checkAndSave(dataModel)
        elementId = element.id
        childId = child.id
        contentId = content.id
        simpleId = simple.id
        dataModelId = dataModel.id
        element2Id = el2.id

        if (buildComplex) complexDataModel = buildComplexDataModel()

        id = element.id
    }

    void "test get"() {
        setupData()

        expect:
        dataElementService.get(id) != null
    }

    void "test list"() {
        setupData()

        when:
        List<DataElement> dataElementList = dataElementService.list(max: 2, offset: 2)

        then:
        dataElementList.size() == 2

        and:
        dataElementList[0].label == 'element2'
        dataElementList[0].dataClassId == DataClass.findByLabel('content').id
        dataElementList[0].minMultiplicity == null
        dataElementList[0].maxMultiplicity == null
        dataElementList[0].dataTypeId == DataType.findByLabel('integer').id

        and:
        dataElementList[1].label == 'element3'
        dataElementList[1].dataClassId == DataClass.findByLabel('content').id
        dataElementList[1].minMultiplicity == 0
        dataElementList[1].maxMultiplicity == 1
        dataElementList[1].dataTypeId == DataType.findByLabel('integer').id
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
        dataElementService.findByDataTypeIdAndId(dataModel.findDataTypeByLabel('string').id, UUID.randomUUID())?.id == null
        dataElementService.findByDataTypeIdAndId(dataModel.findDataTypeByLabel('string').id, elementId)?.id == elementId
        dataElementService.findByDataTypeIdAndId(dataModel.findDataTypeByLabel('integer').id, elementId)?.id == null
        dataElementService.findByDataTypeIdAndId(dataModel.findDataTypeByLabel('integer').id, element2Id)?.id == element2Id
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
        dataElementService.findAllByDataTypeId(dataModel.findDataTypeByLabel('string').id).size() == 2
        dataElementService.findAllByDataTypeId(elementId).isEmpty()
        dataElementService.findAllByDataTypeId(dataModel.findDataTypeByLabel('integer').id).size() == 3
        dataElementService.findAllByDataTypeId(childId).isEmpty()
    }

    void 'test findAllByDataModelId'() {
        given:
        setupData()
        DataModel other = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'anotherModel', folder: testFolder, authority: testAuthority)
        other.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'string'))
        other.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'integer'))
        DataClass simple = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'dc1')
        other.addToDataClasses(simple)

        expect:
        checkAndSave(other)

        when:
        simple.addToDataElements(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'ele1', dataType: other.findDataTypeByLabel('string'))

        then:
        checkAndSave(other)
        dataElementService.findAllByDataModelId(UUID.randomUUID()).isEmpty()
        dataElementService.findAllByDataModelId(dataModel.id).size() == 5
        dataElementService.findAllByDataModelId(other.id).size() == 1
        dataElementService.findAllByDataModelId(elementId).isEmpty()
    }

    void 'test findAllByDataModelIdAndLabelIlike'() {
        given:
        setupData()
        DataModel other = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'anotherModel', folder: testFolder, authority: testAuthority)
        other.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'string'))
        other.addToDataTypes(new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'integer'))

        DataClass simple = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'dc1')
        DataElement element = new DataElement(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'element', dataType: other.findDataTypeByLabel('string'))
        simple.addToDataElements(element)
        other.addToDataClasses(simple)
        checkAndSave(other)

        expect:
        dataElementService.findAllByDataModelIdAndLabelIlike(UUID.randomUUID(), '').isEmpty()
        dataElementService.findAllByDataModelIdAndLabelIlike(dataModel.id, '').size() == 5
        dataElementService.findAllByDataModelIdAndLabelIlike(other.id, '').size() == 1
        dataElementService.findAllByDataModelIdAndLabelIlike(elementId, '').isEmpty()

        dataElementService.findAllByDataModelIdAndLabelIlike(UUID.randomUUID(), 'element').isEmpty()
        dataElementService.findAllByDataModelIdAndLabelIlike(dataModel.id, 'element').size() == 2
        dataElementService.findAllByDataModelIdAndLabelIlike(other.id, 'element').size() == 1
        dataElementService.findAllByDataModelIdAndLabelIlike(elementId, 'element').isEmpty()
    }

    void 'test copying DataElement'() {
        given:
        setupData()
        DataElement original = dataElementService.get(id)
        DataClass copyClass = new DataClass(label: 'copy', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        dataModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(dataModel)

        when:
        DataElement copy = dataElementService.copyDataElement(dataModel, original, editor, userSecurityPolicyManager)
        copyClass.addToDataElements(copy)

        then:
        checkAndSave(dataModel)

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
        copy.dataType.label == original.dataType.label

        and:
        copy.semanticLinks.any {it.targetMultiFacetAwareItemId == original.id && it.linkType == SemanticLinkType.REFINES}
    }

    void 'test copying DataElement with metadata and classifiers'() {
        given:
        setupData()
        DataElement original = dataElementService.get(id)
        DataClass copyClass = new DataClass(label: 'copy', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        dataModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(dataModel)

        when:
        DataElement copy = dataElementService.copyDataElement(dataModel, original, admin, userSecurityPolicyManager)
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
        copy.semanticLinks.any {it.targetMultiFacetAwareItemId == original.id && it.linkType == SemanticLinkType.REFINES}

    }

    void 'test copying DataElement with datatype not present'() {
        given:
        setupData()
        DataElement original = dataElementService.get(id)
        DataModel copyModel = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'copy model', folder: testFolder, authority: testAuthority)
        DataClass copyClass = new DataClass(label: 'copy', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        copyModel.addToDataClasses(copyClass)

        expect:
        checkAndSave(copyModel)

        when:
        DataElement copy = dataElementService.copyDataElement(copyModel, original, editor, userSecurityPolicyManager)
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
        copy.dataType.label == original.dataType.label

        and:
        copy.semanticLinks.any {it.targetMultiFacetAwareItemId == original.id && it.linkType == SemanticLinkType.REFINES}
    }

    @Tag('non-parallel')
    void 'test finding all similar DataElements in another model'() {
        given:
        buildComplex = true
        hibernateSearchIndexingService.purgeAllIndexes()
        setupData()
        hibernateSearchIndexingService.flushIndexes()
        DataElement original = dataElementService.get(id)

        when:
        DataElementSimilarityResult result = dataElementService.findAllSimilarDataElementsInDataModel(complexDataModel, original)
        log.debug('{}', result)

        then:
        result.totalSimilar() == 1
        result.first().item.label == 'ele1'
        result.first().item.id != elementId
        result.first().score > 0
    }
}
