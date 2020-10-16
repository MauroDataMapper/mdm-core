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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration.ReferenceEnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.Stepwise

@Slf4j
@Stepwise
class ReferenceEnumerationTypeServiceSpec extends BaseUnitSpec implements ServiceUnitTest<ReferenceEnumerationTypeService> {

    ReferenceDataModel referenceDataModel
    UUID id

    Folder getTestFolder() {
        Folder.findByLabel('catalogue')
    }

    Authority getTestAuthority() {
        Authority.findByLabel('Test Authority')
    }

    def setup() {
        log.debug('Setting up DataClassServiceSpec Unit')
        mockArtefact(ClassifierService)
        mockArtefact(VersionLinkService)
        mockArtefact(SemanticLinkService)
        mockArtefact(EditService)
        mockArtefact(MetadataService)
        mockArtefact(ReferenceSummaryMetadataService)
        mockDomains(Classifier, Folder, Annotation, BreadcrumbTree, Edit, Metadata, ReferenceFile, SemanticLink,
                    ReferenceDataModel, DataClass, ReferenceDataType, ReferencePrimitiveType, ReferenceDataType, ReferenceEnumerationType, ReferenceEnumerationValue, ReferenceDataElement, Authority)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        checkAndSave(new Authority(label: 'Test Authority', url: 'http:localhost', createdBy: StandardEmailAddress.UNIT_TEST))
        referenceDataModel = new ReferenceDataModel(createdByUser: admin, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(referenceDataModel)

        referenceDataModel.addToDataTypes(new ReferencePrimitiveType(createdByUser: admin, label: 'string'))
        referenceDataModel.addToDataTypes(new ReferencePrimitiveType(createdByUser: editor, label: 'integer'))

        ReferenceEnumerationType et1 = new ReferenceEnumerationType(createdByUser: editor, label: 'et1')
            .addToReferenceEnumerationValues(createdByUser: admin, key: 'key1', value: 'val1')
            .addToReferenceEnumerationValues(new ReferenceEnumerationValue(createdByUser: admin, key: 'key2', value: 'val2')
            )
        referenceDataModel.addToDataTypes(et1)
        referenceDataModel.addToDataTypes(new ReferenceEnumerationType(createdByUser: editor, label: 'et2')
                                     .addToReferenceEnumerationValues(createdByUser: admin, key: 'key1', value: 'val1')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key2', value: 'val2')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key3', value: 'val3')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key4', value: 'val4')
        )
        referenceDataModel.addToDataTypes(new ReferenceEnumerationType(createdByUser: admin, label: 'yesnounknown')
                                     .addToReferenceEnumerationValues(key: 'Y', value: 'Yes')
                                     .addToEnumerationValues(key: 'N', value: 'No')
                                     .addToEnumerationValues(key: 'U', value: 'Unknown'))

        
        /*ReferenceDataType refType = new ReferenceDataType(createdByUser: editor, label: 'Unit parent')
        parent.addToReferenceDataTypes(refType)
        referenceDataModel.addToDataTypes(refType)

        ReferenceDataElement el1 = new ReferenceDataElement(createdByUser: editor, label: 'parentel', minMultiplicity: 1, maxMultiplicity: 1, referenceDataType: refType)
        parent.addToDataElements(el1)

        ReferenceDataType refType2 = new ReferenceDataType(createdByUser: editor, label: 'dataclass')
        dataClass.addToReferenceDataTypes(refType2)
        referenceDataModel.addToDataTypes(refType2)

        ReferenceDataElement el2 = new ReferenceDataElement(createdByUser: editor, label: 'childEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType2.addToDataElements(el2)
        child.addToDataElements(el2)

        ReferenceDataElement el3 = new ReferenceDataElement(createdByUser: editor, label: 'anotherParentEl', minMultiplicity: 1, maxMultiplicity: 1)
        refType.addToDataElements(el3)
        added.addToDataElements(el3)

        checkAndSave(referenceDataModel)

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: editor, targetCatalogueItem: dataClass)
        parent.addToSemanticLinks(link)

        checkAndSave(link)*/

        id = et1.id

    }

    void "test get"() {

        expect:
        service.get(id) != null
    }

    void "test list"() {

        when:
        List<ReferenceEnumerationType> enumerationTypeList = service.list(max: 2, offset: 1)

        then:
        enumerationTypeList.size() == 2

        and:
        enumerationTypeList[0].label == 'et2'
        enumerationTypeList[0].referenceEnumerationValues.size() == 4

        and:
        enumerationTypeList[1].label == 'yesnounknown'
        enumerationTypeList[1].referenceEnumerationValues.size() == 3
    }

    void "test count"() {
        expect:
        service.count() == 3
    }

    void "test delete"() {

        expect:
        service.count() == 3
        ReferenceEnumerationType et = service.get(id)

        when:
        service.delete(et)

        then:
        ReferenceEnumerationType.count() == 2
    }
}
