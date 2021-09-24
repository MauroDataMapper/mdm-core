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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Integration
@Rollback
class DataClassServiceIntegrationSpec extends BaseDataModelIntegrationSpec {

    DataClassService dataClassService
    UserSecurityPolicyManager userSecurityPolicyManager
    DataModelService dataModelService

    @Override
    void setupDomainData() {
        log.debug('Setting up DataClassServiceIntegrationSpec')
        dataModel = new DataModel(createdByUser: admin, label: 'Integration test model', folder: testFolder,
                                  authority: testAuthority)
        checkAndSave(dataModel)

        dataModel.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'string'))
        dataModel.addToDataTypes(new PrimitiveType(createdByUser: editor, label: 'integer'))

        DataClass dataClass = new DataClass(createdByUser: admin, label: 'dc1')
        dataModel.addToDataClasses(dataClass)
        DataClass parent = new DataClass(createdByUser: editor, label: 'Integration parent', dataModel: dataModel, minMultiplicity: 0,
                                         maxMultiplicity: 1)
        DataClass child = new DataClass(createdByUser: reader1, label: 'Integration child', minMultiplicity: 1, maxMultiplicity: -1)
        parent.addToDataClasses(child)
        DataClass added = new DataClass(createdByUser: reader1, label: 'added', description: 'a desc')
        dataModel.addToDataClasses(added)
        DataClass grandParent = new DataClass(createdByUser: editor, label: 'Integration grandparent')
        grandParent.addToDataClasses(parent)
        dataModel.addToDataClasses(grandParent)

        checkAndSave(dataModel)

        ReferenceType refType = new ReferenceType(createdByUser: editor, label: 'Integration parent')
        parent.addToReferenceTypes(refType)
        dataModel.addToDataTypes(refType)

        DataElement el1 = new DataElement(createdByUser: editor, label: 'parentel', minMultiplicity: 1, maxMultiplicity: 1, dataType: refType)
        parent.addToDataElements(el1)

        ReferenceType refType2 = new ReferenceType(createdByUser: editor, label: 'dataclass')
        dataClass.addToReferenceTypes(refType2)
        dataModel.addToDataTypes(refType2)

        DataElement el2 = new DataElement(createdByUser: editor, label: 'childEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType2.addToDataElements(el2)
        child.addToDataElements(el2)

        DataElement el3 = new DataElement(createdByUser: editor, label: 'anotherParentEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType.addToDataElements(el3)
        added.addToDataElements(el3)

        checkAndSave(dataModel)

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: editor, targetMultiFacetAwareItem: dataClass)
        parent.addToSemanticLinks(link)

        checkAndSave(link)

        id = parent.id
    }

    void "test get"() {
        given:
        setupData()

        expect:
        dataClassService.get(id) != null
    }

    void "test list"() {
        given:
        setupData()

        when:
        List<DataClass> dataClassList = dataClassService.list(max: 2, offset: 2)

        then:
        dataClassList.size() == 2

        and:
        dataClassList[0].label == 'Integration grandparent'
        dataClassList[0].dataClasses.size() == 1
        dataClassList[0].dataClasses.find {it.label == 'Integration parent'}

        and:
        dataClassList[1].label == 'Integration parent'
        dataClassList[1].minMultiplicity == 0
        dataClassList[1].maxMultiplicity == 1
        dataClassList[1].dataClasses.size() == 1
        dataClassList[1].dataClasses.find {it.label == 'Integration child'}
    }

    void "test count"() {
        given:
        setupData()

        expect:
        dataClassService.count() == 5
    }

    void "test delete child dataclass"() {
        given:
        setupData()
        DataClass dataClass = DataClass.findByLabel('Integration child')

        expect:
        SemanticLink.count() == 1
        DataElement.count() == 3
        ReferenceType.count() == 2
        dataClassService.count() == 5

        when:
        log.info('Attempting to delete id {}', dataClass.id)
        dataClassService.delete(dataClass, true)
        sessionFactory.currentSession.flush()

        then:
        dataClassService.count() == 4
        SemanticLink.count() == 1
        DataElement.count() == 2
        ReferenceType.count() == 2
    }

    void 'test findByParentDataClassIdAndId'() {
        given:
        setupData()

        when:
        def grandParentId = DataClass.findByLabel('Integration grandparent').id
        def parentId = DataClass.findByLabel('Integration parent').id
        def dataClassId = DataClass.findByLabel('Integration child').id

        then:
        assert grandParentId
        assert parentId
        assert dataClassId

        and:
        dataClassService.findByDataModelIdAndParentDataClassIdAndId(dataModel.id, grandParentId, parentId)
        dataClassService.findByDataModelIdAndParentDataClassIdAndId(dataModel.id, parentId, dataClassId)
        !dataClassService.findByDataModelIdAndParentDataClassIdAndId(dataModel.id, grandParentId, dataClassId)
    }

    void 'test findWhereRootDataClassOfDataModelIdAndId'() {
        given:
        setupData()

        when:
        def grandParentId = DataClass.findByLabel('Integration grandparent').id
        def dataModelId = DataModel.findByLabel('Integration test model').id
        def dataClassId = DataClass.findByLabel('Integration child').id

        then:
        assert grandParentId
        assert dataModelId
        assert dataClassId

        and:
        dataClassService.findWhereRootDataClassOfDataModelIdAndId(dataModelId, grandParentId)
        !dataClassService.findWhereRootDataClassOfDataModelIdAndId(dataModelId, dataClassId)
        !dataClassService.findWhereRootDataClassOfDataModelIdAndId(dataModelId, UUID.randomUUID())
    }

    void 'test findAllByParentDataClassId'() {
        given:
        setupData()

        def grandParentId = DataClass.findByLabel('Integration grandparent').id
        def parentId = DataClass.findByLabel('Integration parent').id
        def dataClassId = DataClass.findByLabel('Integration child').id

        expect:
        dataClassService.findAllByDataModelIdAndParentDataClassId(dataModel.id, grandParentId).size() == 1
        dataClassService.findAllByDataModelIdAndParentDataClassId(dataModel.id, parentId).size() == 1
        dataClassService.findAllByDataModelIdAndParentDataClassId(dataModel.id, dataClassId).isEmpty()
    }

    void 'test findAllWhereRootDataClassOfDataModelId'() {
        given:
        setupData()

        def dataModelId = DataModel.findByLabel('Integration test model').id

        expect:
        dataClassService.findAllWhereRootDataClassOfDataModelId(dataModelId).size() == 3
        dataClassService.findAllWhereRootDataClassOfDataModelId(UUID.randomUUID()).isEmpty()
    }

    void 'test findAllContentOfDataClassId'() {
        given:
        setupData()

        def grandParentId = DataClass.findByLabel('Integration grandparent').id
        def parentId = DataClass.findByLabel('Integration parent').id

        expect:
        dataClassService.findAllContentOfDataClassIdInDataModelId(dataModel.id, grandParentId).size() == 1
        dataClassService.findAllContentOfDataClassIdInDataModelId(dataModel.id, parentId).size() == 2
    }

    void 'test findAllByDataModelId'() {
        given:
        setupData()

        def dataModelId = DataModel.findByLabel('Integration test model').id

        expect:
        dataClassService.findAllByDataModelId(dataModelId).size() == 5
        dataClassService.findAllByDataModelId(UUID.randomUUID()).isEmpty()
    }

    void 'test findAllByDataModelIdIncludingImported'() {
        given:
        setupData()
        setupImportingData()

        def dataModelId = DataModel.findByLabel('Integration test model').id

        expect:
        dataClassService.findAllByDataModelIdIncludingImported(dataModelId).size() == 9
        dataClassService.findAllByDataModelIdIncludingImported(UUID.randomUUID()).isEmpty()
    }

    void 'test findAllByDataModelIdAndParentDataClassIdIncludingImported'() {
        given:
        setupData()
        setupImportingData()

        def dataModelId = DataModel.findByLabel('Integration test model').id
        def importingParentId = DataClass.findByLabel('Integration Test Importing Parent DataClass').id
        def imported1Id = DataClass.findByLabel('Integration Test Imported DataClass 1').id
        def imported2Id = DataClass.findByLabel('Integration Test Imported DataClass 2').id

        expect:
        dataClassService.findAllByDataModelIdAndParentDataClassIdIncludingImported(dataModelId, importingParentId).size() == 2
        dataClassService.findAllByDataModelIdAndParentDataClassIdIncludingImported(dataModelId, imported1Id).size() == 0
        dataClassService.findAllByDataModelIdAndParentDataClassIdIncludingImported(dataModelId, imported2Id).size() == 1
    }

    void 'test findAllByDataModelIdAndLabelIlikeOrDescriptionIlike'() {
        given:
        setupData()

        def dataModelId = DataModel.findByLabel('Integration test model').id

        expect:
        dataClassService.findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(dataModelId, '%parent').size() == 2
        dataClassService.findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(dataModelId, 'desc').size() == 1
        dataClassService.findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(dataModelId, 'dc').size() == 1
        dataClassService.findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(UUID.randomUUID(), 'Integration parent').isEmpty()
    }

    void 'test copying very simple DataClass'() {
        given:
        setupData()

        when:
        DataModel copyModel = new DataModel(label: 'copy', createdByUser: editor, folder: testFolder, authority: testAuthority)
        DataClass vsimple = new DataClass(label: 'vsimple', createdByUser: editor)
        dataModel.addToDataClasses(vsimple)

        then:
        checkAndSave(dataModel)
        checkAndSave(copyModel)

        when:
        DataClass original = dataClassService.get(vsimple.id)
        DataClass copy = dataClassService.copyDataClass(copyModel, original, editor, userSecurityPolicyManager)

        then:
        checkAndSave(copyModel)

        when:
        original = dataClassService.get(vsimple.id)
        copy = dataClassService.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        copy.createdBy == editor.emailAddress
        copy.metadata?.size() == original.metadata?.size()
        copy.classifiers == original.classifiers

        and:
        !copy.dataClasses
        !copy.dataElements
        !copy.referenceTypes

        and:
        copy.semanticLinks.any {it.targetMultiFacetAwareItemId == original.id && it.linkType == SemanticLinkType.REFINES}
    }

    void 'test copying simple DataClass with simple data elements'() {
        given:
        setupData()

        DataClass content = new DataClass(createdByUser: admin, label: 'content', minMultiplicity: 1, maxMultiplicity: 1)
        content.addToDataClasses(label: 'vsimple', createdByUser: editor, minMultiplicity: 0, maxMultiplicity: 1)
        DataElement element = new DataElement(createdByUser: admin, label: 'ele1', dataType: dataModel.findDataTypeByLabel('string'))
        content.addToDataElements(element)
        dataModel.addToDataClasses(content)
        DataModel copyModel = new DataModel(label: 'copy', createdByUser: editor, folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(dataModel)
        checkAndSave(copyModel)

        when:
        copyModel.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'string'))
        copyModel.addToDataTypes(new PrimitiveType(createdByUser: editor, label: 'integer'))

        then:
        checkAndSave(copyModel)

        when:
        DataClass original = dataClassService.get(content.id)
        DataClass copy = dataClassService.copyDataClass(copyModel, original, editor, userSecurityPolicyManager)

        then:
        checkAndSave(copyModel)

        when:
        original = dataClassService.get(content.id)
        copy = dataClassService.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        copy.createdBy == editor.emailAddress
        copy.metadata?.size() == original.metadata?.size()
        copy.classifiers == original.classifiers

        and:
        copy.dataClasses.size() == 1
        copy.dataClasses.size() == original.dataClasses.size()
        copy.dataElements.size() == 1
        copy.dataElements.size() == original.dataElements.size()

        and:
        copy.semanticLinks.any {it.targetMultiFacetAwareItemId == original.id && it.linkType == SemanticLinkType.REFINES}
    }

    void 'test metadata saved at parent'()
    {
        given:
        setupData()

        DataModel copyModel = new DataModel(label: 'test', createdByUser: editor, folder: testFolder, authority: testAuthority)


        DataClass top = new DataClass(createdByUser: reader1, label: 'Top', minMultiplicity: 1, maxMultiplicity: 1)

        Metadata metadata = new Metadata(namespace: 'Test', key: 'key', value: '1')

        copyModel.addToDataClasses(top)
        top.addToMetadata(metadata)

        DataClass middle = new DataClass(createdByUser: reader1, label: 'Middle', minMultiplicity: 1, maxMultiplicity: 1)
        top.addToDataClasses(middle)
        copyModel.addToDataClasses(middle)

        when:
        dataModelService.validate(copyModel)
        dataModelService.saveModelWithContent(copyModel)

        then:
        DataModel dm = DataModel.get(copyModel.id)
        dm.dataClasses[0].metadata.size() == 1
    }

    void 'test copying complex dataclass'() {
        given:
        setupData()
        DataClass complex = dataModel.dataClasses.find {it.label == 'Integration grandparent'}
        DataModel copyModel = new DataModel(label: 'copy', createdByUser: editor, folder: testFolder, authority: testAuthority)
        checkAndSave(copyModel)
        dataModelService.updateFacetsAfterInsertingCatalogueItem(copyModel)
        sessionFactory.currentSession.flush()
        dataClassService.copyDataClass(copyModel, dataModel.childDataClasses.find {it.label == 'dc1'}, editor, userSecurityPolicyManager)

        expect:
        check(copyModel)
        dataModelService.saveModelNewContentOnly(copyModel)

        when:
        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()
        copyModel = dataModelService.get(copyModel.id)
        DataClass original = dataClassService.get(complex.id)
        DataClass copy = dataClassService.copyDataClass(copyModel, original, editor, userSecurityPolicyManager)

        then:
        check(copyModel)
        dataModelService.saveModelNewContentOnly(copyModel)

        when:
        original = dataClassService.get(complex.id)
        copy = dataClassService.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.minMultiplicity == original.minMultiplicity
        copy.maxMultiplicity == original.maxMultiplicity

        copy.createdBy == editor.emailAddress
        copy.metadata?.size() == original.metadata?.size()
        copy.classifiers == original.classifiers

        and:
        copy.dataClasses
        copy.dataClasses.size() == 1
        !copy.dataElements
        !copy.referenceTypes

        and:
        SemanticLink.findByTargetMultiFacetAwareItemIdAndMultiFacetAwareItemIdAndLinkType(original.id, copy.id, SemanticLinkType.REFINES)

        when:
        DataClass copiedParent = copy.dataClasses.first()

        then:
        copiedParent
        copiedParent.label == 'Integration parent'
        copiedParent.dataClasses.size() == 1
        copiedParent.dataClasses.find {it.label == 'Integration child'}
        copiedParent.dataElements.size() == 1
        copiedParent.referenceTypes.size() == 1
        copiedParent.referenceTypes.find {it.label == 'Integration parent'}

        when:
        ReferenceType referenceType = copyModel.dataTypes.find {it.label == 'Integration parent'} as ReferenceType

        then:
        referenceType
        referenceType.referenceClass == copiedParent

        when:
        DataElement copiedElement = copiedParent.dataElements.find {it.label == 'parentel'}

        then:
        copiedElement
        copiedElement.dataType == referenceType
    }

    private void setupImportingData() {
        // DataModel: Integration test model
        //      DataClass: Integration Test Importing Parent DataClass (directly owned)
        //          DataClass: Integration Test Imported DataClass 1 (imported)
        //          DataClass: Integration Test Imported DataClass 2 (imported)
        //              DataClass: Integration Test Imported Child DataClass 1 (imported)

        DataClass importedChild1 = new DataClass(createdByUser: reader1, label: 'Integration Test Imported Child DataClass 1', dataModel: dataModel)
        checkAndSave(importedChild1)

        DataClass imported1 = new DataClass(createdByUser: reader1, label: 'Integration Test Imported DataClass 1', dataModel: dataModel)
        DataClass imported2 = new DataClass(createdByUser: reader1, label: 'Integration Test Imported DataClass 2', dataModel: dataModel)
        imported2.addToImportedDataClasses(importedChild1)
        [imported1, imported2].each {checkAndSave(it)}

        DataClass importingParent = new DataClass(createdByUser: editor, label: 'Integration Test Importing Parent DataClass', dataModel: dataModel)
        [imported1, imported2].each {importingParent.addToImportedDataClasses(it)}
        checkAndSave(importingParent)
        dataModel.addToDataClasses(importingParent)
        checkAndSave(dataModel)
    }
}
