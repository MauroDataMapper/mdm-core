/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.referencedata.similarity.ReferenceDataElementSimilarityResult
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

    ReferenceDataElementService referenceDataElementService

    Boolean buildComplex = false

    ReferenceDataModel referenceDataModel
    ReferenceDataModel complexReferenceDataModel

    UserSecurityPolicyManager userSecurityPolicyManager

    void setupDomainData() {
        log.debug('Setting up ReferenceDataElementServiceSpec')
        referenceDataModel = new ReferenceDataModel(createdByUser: admin, label: 'Integration test model', folder: testFolder, authority: testAuthority)
        checkAndSave(referenceDataModel)

        referenceDataModel.addToReferenceDataTypes(new ReferencePrimitiveType(createdByUser: admin, label: 'string'))
        referenceDataModel.addToReferenceDataTypes(new ReferencePrimitiveType(createdByUser: editor, label: 'integer'))

        ReferenceDataElement element = new ReferenceDataElement(createdByUser: admin, label: 'ele1', referenceDataType: referenceDataModel.findReferenceDataTypeByLabel('string'))
        referenceDataModel.addToReferenceDataElements(element)

        ReferenceDataElement element2 = new ReferenceDataElement(createdByUser: admin, label: 'ele2', referenceDataType: referenceDataModel.findReferenceDataTypeByLabel('integer'),
                                                                 minMultiplicity: 0, maxMultiplicity: 1)
        referenceDataModel.addToReferenceDataElements(element2)        

        checkAndSave(referenceDataModel)
        elementId = element.id
        element2Id = element2.id
        referenceDataModelId = referenceDataModel.id

        if (buildComplex) complexReferenceDataModel = buildSecondExampleReferenceDataModel()

        id = element.id
    }

    void "test get"() {
        setupData()

        expect:
        referenceDataElementService.get(id) != null
    }

    void "test list"() {
        setupData()

        when:
        List<ReferenceDataElement> dataElementList = referenceDataElementService.list(max: 2, offset: 0)

        then:
        dataElementList.size() == 2

        and:
        dataElementList[0].label == 'ele1'
        dataElementList[0].minMultiplicity == null
        dataElementList[0].maxMultiplicity == null
        dataElementList[0].referenceDataTypeId == referenceDataModel.findReferenceDataTypeByLabel('string').id

        and:
        dataElementList[1].label == 'ele2'
        dataElementList[1].minMultiplicity == 0
        dataElementList[1].maxMultiplicity == 1
        dataElementList[1].referenceDataTypeId == referenceDataModel.findReferenceDataTypeByLabel('integer').id
    }

    void "test count"() {
        setupData()

        expect:
        referenceDataElementService.count() ==2
    }

    void "test delete"() {
        setupData()

        expect:
        referenceDataElementService.count() == 2

        when:
        referenceDataElementService.delete(id)
        sessionFactory.currentSession.flush()

        then:
        referenceDataElementService.count() == 1
    }

    void 'test findByReferenceDataTypeIdAndId'() {
        given:
        setupData()

        expect:
        referenceDataElementService.findByReferenceDataTypeIdAndId(UUID.randomUUID(), UUID.randomUUID())?.id == null
        referenceDataElementService.findByReferenceDataTypeIdAndId(referenceDataModel.findReferenceDataTypeByLabel('string').id, UUID.randomUUID())?.id == null
        referenceDataElementService.findByReferenceDataTypeIdAndId(referenceDataModel.findReferenceDataTypeByLabel('string').id, elementId)?.id == elementId
        referenceDataElementService.findByReferenceDataTypeIdAndId(referenceDataModel.findReferenceDataTypeByLabel('integer').id, elementId)?.id == null
        referenceDataElementService.findByReferenceDataTypeIdAndId(referenceDataModel.findReferenceDataTypeByLabel('integer').id, element2Id)?.id == element2Id
    }

    void 'test findAllByReferenceDataTypeId'() {
        given:
        setupData()

        expect:
        referenceDataElementService.findAllByReferenceDataTypeId(UUID.randomUUID()).isEmpty()
        referenceDataElementService.findAllByReferenceDataTypeId(referenceDataModel.findReferenceDataTypeByLabel('string').id).size() == 1
        referenceDataElementService.findAllByReferenceDataTypeId(elementId).isEmpty()
        referenceDataElementService.findAllByReferenceDataTypeId(referenceDataModel.findReferenceDataTypeByLabel('integer').id).size() == 1
        referenceDataElementService.findAllByReferenceDataTypeId(childId).isEmpty()
    }

    void 'test findAllByReferenceDataModelIdAndLabelIlike'() {
        given:
        setupData()
        ReferenceDataModel other = new ReferenceDataModel(createdByUser: admin, label: 'anotherModel', folder: testFolder, authority: testAuthority)
        other.addToReferenceDataTypes(new ReferencePrimitiveType(createdByUser: admin, label: 'string'))
        other.addToReferenceDataTypes(new ReferencePrimitiveType(createdByUser: editor, label: 'integer'))
        checkAndSave(other)
        ReferenceDataElement element = new ReferenceDataElement(createdByUser: admin, label: 'other element', referenceDataType: other.findReferenceDataTypeByLabel('string'))
        other.addToReferenceDataElements(element)        
        checkAndSave(other)

        expect:
        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(UUID.randomUUID(), '').isEmpty()
        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(referenceDataModel.id, '').size() == 2
        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(other.id, '').size() == 1
        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(elementId, '').isEmpty()

        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(UUID.randomUUID(), 'element').isEmpty()
        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(referenceDataModel.id, 'ele').size() == 2
        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(referenceDataModel.id, 'ele1').size() == 1       
        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(referenceDataModel.id, 'aele1').size() == 0
        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(other.id, 'element').size() == 1
        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(other.id, 'other eleme').size() == 1        
        referenceDataElementService.findAllByReferenceDataModelIdAndLabelIlike(elementId, 'element').isEmpty()
    }

    void 'test copying ReferenceDataElement'() {
        given:
        setupData()
        ReferenceDataElement original = referenceDataElementService.get(id)
        ReferenceDataModel destination = new ReferenceDataModel(createdByUser: admin, label: 'Destination integration test model', folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(destination)

        when:
        ReferenceDataElement copy = referenceDataElementService.copyReferenceDataElement(destination, original, editor, userSecurityPolicyManager)

        then:
        checkAndSave(destination)

        when:
        original = referenceDataElementService.get(id)
        copy = referenceDataElementService.get(copy.id)

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
        ReferenceDataElement original = referenceDataElementService.get(id)
        ReferenceDataModel destination = new ReferenceDataModel(createdByUser: admin, label: 'Destination integration test model', folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(destination)

        when:
        ReferenceDataElement copy = referenceDataElementService.copyReferenceDataElement(destination, original, editor, userSecurityPolicyManager)

        then:
        checkAndSave(destination)

        when:
        original = referenceDataElementService.get(id)
        copy = referenceDataElementService.get(copy.id)

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

    void 'test finding all similar DataElements in another model'() {
        given:
        buildComplex = true
        setupData()
        ReferenceDataElement original = referenceDataElementService.get(id)

        when:
        ReferenceDataElementSimilarityResult result =
            referenceDataElementService.findAllSimilarReferenceDataElementsInReferenceDataModel(complexReferenceDataModel, original)

        then:
        result.size() == 3
        result.first().item.label == 'Column A'
        result.first().item.id != elementId
        result.first().similarity > 0
    }
}
