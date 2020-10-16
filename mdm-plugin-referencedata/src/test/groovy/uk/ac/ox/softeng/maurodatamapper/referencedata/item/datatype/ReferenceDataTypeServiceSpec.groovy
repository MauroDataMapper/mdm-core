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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype

import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElementService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError
import spock.lang.Stepwise

@Slf4j
@Stepwise
class ReferenceDataTypeServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<ReferenceDataTypeService> {

    ReferenceDataModel referenceDataModel
    UUID id

    def setup() {
        log.debug('Setting up DataTypeServiceSpec Unit')
        mockArtefact(ReferenceDataElementService)
        mockArtefact(ReferenceTypeService)
        mockArtefact(ReferencePrimitiveTypeService)
        mockArtefact(ReferenceEnumerationTypeService)
        mockArtefact(ReferenceSummaryMetadataService)
        mockDomains(ReferenceDataModel, ReferenceDataType, ReferencePrimitiveType, ReferenceType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement)

        referenceDataModel = new ReferenceDataModel(createdByUser: admin, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(referenceDataModel)

        ReferencePrimitiveType primitiveType = new ReferencePrimitiveType(createdByUser: editor, label: 'varchar')

        referenceDataModel.addToDataTypes(primitiveType)
        referenceDataModel.addToDataTypes(new ReferencePrimitiveType(createdByUser: admin, label: 'string'))
        referenceDataModel.addToDataTypes(new ReferencePrimitiveType(createdByUser: editor, label: 'integer'))

        ReferenceEnumerationType et1 = new ReferenceEnumerationType(createdByUser: editor, label: 'et1')
            .addToReferenceEnumerationValues(createdByUser: admin, key: 'key1', value: 'val1')
            .addToReferenceEnumerationValues(new ReferenceEnumerationValue(createdByUser: admin, key: 'key2', value: 'val2')
            )
        referenceDataModel.addToDataTypes(et1)
        referenceDataModel.addToDataTypes(new ReferenceEnumerationType(createdByUser: editor, label: 'moreet')
                                     .addToReferenceEnumerationValues(createdByUser: admin, key: 'key1', value: 'val1')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key2', value: 'val2')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key3', value: 'val3')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key4', value: 'val4')
        )
        referenceDataModel.addToDataTypes(new ReferenceEnumerationType(createdByUser: admin, label: 'yesnounknown')
                                     .addToReferenceEnumerationValues(key: 'Y', value: 'Yes')
                                     .addToEnumerationValues(key: 'N', value: 'No')
                                     .addToEnumerationValues(key: 'U', value: 'Unknown'))

        

        /*ReferenceType refType = new ReferenceType(createdByUser: editor, label: 'Unit parent')
        parent.addToReferenceTypes(refType)
        referenceDataModel.addToDataTypes(refType)

        ReferenceDataElement el1 = new ReferenceDataElement(createdByUser: editor, label: 'parentel', minMultiplicity: 1, maxMultiplicity: 1, referenceDataType: refType)
        parent.addToDataElements(el1)

        ReferenceType refType2 = new ReferenceType(createdByUser: editor, label: 'dataclass')
        dataClass.addToReferenceTypes(refType2)
        referenceDataModel.addToDataTypes(refType2)

        ReferenceDataElement el2 = new ReferenceDataElement(createdByUser: editor, label: 'childEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType2.addToDataElements(el2)
        child.addToDataElements(el2)

        ReferenceDataElement el3 = new ReferenceDataElement(createdByUser: editor, label: 'anotherParentEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType.addToDataElements(el3)
        added.addToDataElements(el3)

        added.addToDataElements(new ReferenceDataElement(createdByUser: editor, label: 'varcharel', minMultiplicity: 1, maxMultiplicity: 1,
                                                referenceDataType: primitiveType))

        checkAndSave(referenceDataModel)

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: editor, targetCatalogueItem: dataClass)
        parent.addToSemanticLinks(link)

        checkAndSave(link)

        verifyBreadcrumbTrees()*/

        id = primitiveType.id

    }

    void "test get"() {

        expect:
        service.get(id) != null
    }

    void "test list"() {

        when:
        List<ReferenceDataType> dataTypeList = service.list(max: 2, offset: 2, sort: 'label')

        then:
        dataTypeList.size() == 2

        and:
        dataTypeList[0].label == 'et1'
        dataTypeList[0].domainType == 'EnumerationType'

        and:
        dataTypeList[1].label == 'integer'
        dataTypeList[1].domainType == 'PrimitiveType'
    }

    void "test count"() {
        expect:
        service.count() == 8
    }

    void "test delete"() {
        given:
        ReferenceDataType pt = service.get(id)

        expect:
        service.count() == 8
        ReferenceDataElement.findByLabel('varcharel')

        when:
        service.delete(pt)

        then:
        ReferenceDataType.count() == 7

        and:
        !ReferenceDataElement.findByLabel('varcharel')
    }

    void "test save"() {

        when:
        ReferenceDataType dataType = new ReferencePrimitiveType(createdByUser: reader2, label: 'saving test', referenceDataModel: referenceDataModel)
        service.save(dataType)

        then:
        dataType.id != null
    }

    void 'test copying primitive datatype'() {
        given:
        ReferenceDataType original = ReferencePrimitiveType.findByLabel('string')
        ReferenceDataModel copyModel = new ReferenceDataModel(createdByUser: admin, label: 'copy model', folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(copyModel)

        when:
        ReferenceDataType copy = service.copyDataType(copyModel, original, editor, userSecurityPolicyManager)

        then:
        checkAndSave(copyModel)

        when:
        original = ReferencePrimitiveType.findByLabel('string')
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.units == original.units

        and:
        copy.createdBy == editor.emailAddress
        copy.metadata?.size() == original.metadata?.size()
        copy.classifiers == original.classifiers

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }

    void 'test copying enumeration datatype'() {
        given:
        ReferenceDataType original = ReferenceEnumerationType.findByLabel('yesnounknown')
        ReferenceDataModel copyModel = new ReferenceDataModel(createdByUser: admin, label: 'copy model', folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(copyModel)

        when:
        ReferenceDataType copy = service.copyDataType(copyModel, original, editor, userSecurityPolicyManager)

        then:
        checkAndSave(copyModel)

        when:
        original = ReferenceEnumerationType.findByLabel('yesnounknown')
        copy = service.get(copy.id)

        then:
        copy.label == original.label
        copy.description == original.description
        copy.enumerationValues.size() == original.referenceEnumerationValues.size()

        and:
        original.referenceEnumerationValues.every { o -> copy.enumerationValues.any { c -> c.key == o.key && c.value == o.value } }

        and:
        copy.createdBy == editor.emailAddress
        copy.metadata?.size() == original.metadata?.size()
        copy.classifiers == original.classifiers

        and:
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }

    void 'test copying reference datatype'() {
        given:
        ReferenceDataType original = ReferenceType.findByLabel('dataclass')
        ReferenceDataModel copyModel = new ReferenceDataModel(createdByUser: admin, label: 'copy model', folder: testFolder, authority: testAuthority)

        expect:
        checkAndSave(copyModel)

        when:
        ReferenceDataType copy = service.copyDataType(copyModel, original, editor, userSecurityPolicyManager)
        checkAndSave(copyModel)

        then:
        thrown(InternalSpockError)

        and:
        copyModel.errors.getFieldError('dataTypes[0].referenceClass').code == 'nullable'

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
        copy.semanticLinks.any { it.targetCatalogueItemId == original.id && it.linkType == SemanticLinkType.REFINES }
    }
}
