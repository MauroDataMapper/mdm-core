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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

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
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration.EnumerationValue
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.Stepwise

@Slf4j
@Stepwise
class PrimitiveTypeServiceSpec extends BaseUnitSpec implements ServiceUnitTest<PrimitiveTypeService> {

    DataModel dataModel
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
        mockArtefact(SummaryMetadataService)
        mockDomains(Classifier, Folder, Annotation, BreadcrumbTree, Edit, Metadata, ReferenceFile, SemanticLink,
                    DataModel, DataClass, DataType, PrimitiveType, ReferenceType, EnumerationType, EnumerationValue, DataElement, Authority)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        checkAndSave(new Authority(label: 'Test Authority', url: 'http:localhost', createdBy: StandardEmailAddress.UNIT_TEST))
        dataModel = new DataModel(createdByUser: admin, label: 'Unit test model', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel)

        PrimitiveType primitiveType = new PrimitiveType(createdByUser: editor, label: 'varchar')

        dataModel.addToDataTypes(primitiveType)
        dataModel.addToDataTypes(new PrimitiveType(createdByUser: admin, label: 'string'))
        dataModel.addToDataTypes(new PrimitiveType(createdByUser: editor, label: 'integer'))

        EnumerationType et1 = new EnumerationType(createdByUser: editor, label: 'et1')
            .addToEnumerationValues(createdByUser: admin, key: 'key1', value: 'val1')
            .addToEnumerationValues(new EnumerationValue(createdByUser: admin, key: 'key2', value: 'val2')
            )
        dataModel.addToDataTypes(et1)
        dataModel.addToDataTypes(new EnumerationType(createdByUser: editor, label: 'et2')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key1', value: 'val1')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key2', value: 'val2')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key3', value: 'val3')
                                     .addToEnumerationValues(createdByUser: admin, key: 'key4', value: 'val4')
        )
        dataModel.addToDataTypes(new EnumerationType(createdByUser: admin, label: 'yesnounknown')
                                     .addToEnumerationValues(key: 'Y', value: 'Yes')
                                     .addToEnumerationValues(key: 'N', value: 'No')
                                     .addToEnumerationValues(key: 'U', value: 'Unknown'))

        DataClass dataClass = new DataClass(createdByUser: admin, label: 'dc1')
        dataModel.addToDataClasses(dataClass)
        DataClass parent = new DataClass(createdByUser: editor, label: 'Unit parent', dataModel: dataModel, minMultiplicity: 0,
                                         maxMultiplicity: 1)
        DataClass child = new DataClass(createdByUser: reader1, label: 'Unit child', minMultiplicity: 1, maxMultiplicity: -1)
        parent.addToDataClasses(child)
        DataClass added = new DataClass(createdByUser: reader1, label: 'added', description: 'a desc')
        dataModel.addToDataClasses(added)
        DataClass grandParent = new DataClass(createdByUser: editor, label: 'Unit grandparent')
        grandParent.addToDataClasses(parent)
        dataModel.addToDataClasses(grandParent)

        ReferenceType refType = new ReferenceType(createdByUser: editor, label: 'Unit parent')
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

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdByUser: editor, targetCatalogueItem: dataClass)
        parent.addToSemanticLinks(link)

        checkAndSave(link)

        id = primitiveType.id

    }

    void "test get"() {

        expect:
        service.get(id) != null
    }

    void "test list"() {

        when:
        List<PrimitiveType> primitiveTypeList = service.list(max: 2, offset: 1, sort: 'label')

        then:
        primitiveTypeList.size() == 2

        and:
        primitiveTypeList[0].label == 'string'

        and:
        primitiveTypeList[1].label == 'varchar'
    }

    void "test count"() {
        expect:
        service.count() == 3
    }

    void "test delete"() {

        expect:
        service.count() == 3
        PrimitiveType pt = service.get(id)

        when:
        service.delete(pt)

        then:
        PrimitiveType.count() == 2
    }
}
