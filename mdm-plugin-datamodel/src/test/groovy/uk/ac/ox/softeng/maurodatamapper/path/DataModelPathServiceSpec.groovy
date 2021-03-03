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
package uk.ac.ox.softeng.maurodatamapper.path


import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTreeService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.Stepwise

@Slf4j
@Stepwise
class DataModelPathServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<PathService> {

    DataModel dataModel1
    DataClass dataClass1_1
    DataClass dataClass1_2
    DataClass dataClass1_3
    DataClass dataClass1_1_1

    DataModel dataModel2
    DataClass dataClass2_1
    DataElement dataElement2_1
    DataClass dataClass2_2
    DataClass dataClass2_3
    DataClass dataClass2_4

    /*
    Set up test data like:

    "data model 1"
           ->     "data class 1"
                        -> data class 1_1
           ->     "data class 2"
           ->     "data class 3"

     "data model 2"
           ->     "data class 1"
                        -> data element 1
           ->     "data class 2"
           ->     "data class 3"
           ->     "data class 4"
     */
    def setup() {
        log.debug('Setting up PathServiceSpec Unit')
        mockArtefact(BreadcrumbTreeService)
        mockArtefact(DataTypeService)
        mockArtefact(DataClassService)
        mockArtefact(DataElementService)
        //The Metadata is required
        mockDomains(DataModel, Metadata, DataClass, DataType, DataElement, PrimitiveType)
        mockArtefact(DataModelService)

        //        service.breadcrumbTreeService = Stub(BreadcrumbTreeService){
        //            finalise(_) >> {
        //                BreadcrumbTree bt ->
        //                    bt.finalised = true
        //                    bt.buildTree()
        //
        //            }
        //        }

        dataModel1 = new DataModel(createdByUser: admin, label: 'data model 1', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel1)

        dataClass1_1 = new DataClass(createdByUser: admin, label: 'data class 1')

        dataClass1_1_1 = new DataClass(createdByUser: admin, label: 'data class 1_1')
        dataClass1_1.addToDataClasses(dataClass1_1_1)
        dataModel1.addToDataClasses(dataClass1_1)

        dataClass1_2 = new DataClass(createdByUser: admin, label: 'data class 2')
        dataModel1.addToDataClasses(dataClass1_2)

        dataClass1_3 = new DataClass(createdByUser: admin, label: 'data class 3')
        dataModel1.addToDataClasses(dataClass1_3)
        checkAndSave(dataModel1)


        dataModel2 = new DataModel(createdByUser: admin, label: 'data model 2', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel2)

        dataClass2_1 = new DataClass(createdByUser: admin, label: 'data class 1')
        DataType dt = new PrimitiveType(createdByUser: admin, label: 'path service test data type')
        dataModel2.addToDataTypes(dt)
        dataElement2_1 = new DataElement(createdByUser: admin, label: 'data element 1', dataType: dt)
        dataClass2_1.addToDataElements(dataElement2_1)
        dataModel2.addToDataClasses(dataClass2_1)

        dataClass2_2 = new DataClass(createdByUser: admin, label: 'data class 2')
        dataModel2.addToDataClasses(dataClass2_2)

        dataClass2_3 = new DataClass(createdByUser: admin, label: 'data class 3')
        dataModel2.addToDataClasses(dataClass2_3)

        dataClass2_4 = new DataClass(createdByUser: admin, label: 'data class 4')
        dataModel2.addToDataClasses(dataClass2_4)
        checkAndSave(dataModel2)

        [dataModel1, dataClass1_1, dataClass1_2, dataClass1_3, dataModel2, dataClass2_1, dataClass2_2, dataClass2_3, dataClass2_4, dataElement2_1].each{
            log.debug("${it.label}  ${it.id}")
        }
    }


    /*
    Get each of the three DataClasses in data model 1, by specifying the data class ID and label
     */
    void "test get of data classes in data model 1 by data class ID"() {
        Map params
        CatalogueItem catalogueItem

        when:
        params = ['catalogueItemDomainType': 'dataClasses', 'catalogueItemId': dataClass1_1.id.toString(), 'path': "dc:data class 1"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel1.id)

        //This one (data class 1_1) is nested inside data class 1
        when:
        params = ['catalogueItemDomainType': 'dataClasses', 'catalogueItemId': dataClass1_1_1.id.toString(), 'path': "dc:data class 1_1"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 1_1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel1.id)

        when:
        params = ['catalogueItemDomainType': 'dataClasses', 'catalogueItemId': dataClass1_2.id.toString(), 'path': "dc:data class 2"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 2"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel1.id)

        when:
        params = ['catalogueItemDomainType': 'dataClasses', 'catalogueItemId': dataClass1_3.id.toString(), 'path': "dc:data class 3"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 3"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel1.id)
    }

    /*
    Get each of the four DataClasses in data model 2, by specifying the data class ID and label
     */
    void "test get of data classes in data model 2 by data class ID"() {
        Map params
        CatalogueItem catalogueItem

        when:
        params = ['catalogueItemDomainType': 'dataClasses', 'catalogueItemId': dataClass2_1.id.toString(), 'path': "dc:data class 1"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel2.id)

        when:
        params = ['catalogueItemDomainType': 'dataClasses', 'catalogueItemId': dataClass2_2.id.toString(), 'path': "dc:data class 2"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 2"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel2.id)

        when:
        params = ['catalogueItemDomainType': 'dataClasses', 'catalogueItemId': dataClass2_3.id.toString(), 'path': "dc:data class 3"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 3"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel2.id)

        when:
        params = ['catalogueItemDomainType': 'dataClasses', 'catalogueItemId': dataClass2_4.id.toString(), 'path': "dc:data class 4"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 4"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel2.id)
    }


    /*
    Get data model 1 by specifying its ID and label
     */
    void "test get of data model 1"() {
        when:
        Map params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel1.id.toString(), 'path': "dm:data model 1"]
        CatalogueItem catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data model 1"
        catalogueItem.domainType == "DataModel"
    }

    /*
    Get data model 2 by specifying its ID and label
    */
    void "test get of data model 2"() {
        when:
        Map params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel2.id.toString(), 'path': "dm:data model 2"]
        CatalogueItem catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data model 2"
        catalogueItem.domainType == "DataModel"
    }

    /*
    Get the data classes in data model 1 by specifying the ID of the data model and the label of the data class
     */
    void "test get of data classes by label in data model 1"() {
        Map params
        CatalogueItem catalogueItem

        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel1.id.toString(), 'path': "dm:|dc:data class 1"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel1.id)

        //This is the nested data class
        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel1.id.toString(), 'path': "dm:|dc:data class 1|dc:data class 1_1"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 1_1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel1.id)

        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel1.id.toString(), 'path': "dm:|dc:data class 2"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 2"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel1.id)

        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel1.id.toString(), 'path': "dm:|dc:data class 3"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 3"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel1.id)

        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel1.id.toString(), 'path': "dm:|dc:data class 4"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem == null
    }

    /*
    Get the data classes in data model 2 by specifying the ID of the data model and the label of the data class
    */
    void "test get of data classes by label in data model 2"() {
        Map params
        CatalogueItem catalogueItem

        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel2.id.toString(), 'path': "dm:|dc:data class 1"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel2.id)

        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel2.id.toString(), 'path': "dm:|dc:data class 2"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 2"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel2.id)

        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel2.id.toString(), 'path': "dm:|dc:data class 3"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 3"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel2.id)

        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel2.id.toString(), 'path': "dm:|dc:data class 4"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data class 4"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id.equals(dataModel2.id)
    }

    /*
    Get the data element 1 in data model 2
    */
    void "test get of data element"() {
        Map params
        CatalogueItem catalogueItem

        //When the data class is root
        when:
        params = ['catalogueItemDomainType': 'dataClasses', 'catalogueItemId': dataClass2_1.id.toString(), 'path': "dc:|de:data element 1"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data element 1"
        catalogueItem.domainType == "DataElement"
        catalogueItem.model.id.equals(dataModel2.id)

        //When the data model is root
        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel2.id.toString(), 'path': "dm:|dc:data class 1|de:data element 1"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "data element 1"
        catalogueItem.domainType == "DataElement"
        catalogueItem.model.id.equals(dataModel2.id)
    }

    /*
    Get the data type in data model 2
    */
    void "test get of data type"() {
        Map params
        CatalogueItem catalogueItem

        //When the data model ID is provided
        when:
        params = ['catalogueItemDomainType': 'dataModels', 'catalogueItemId': dataModel2.id.toString(), 'path': "dm:|dt: path service test data type"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "path service test data type"
        catalogueItem.domainType == "PrimitiveType"
        catalogueItem.model.id.equals(dataModel2.id)

        //When the data model label is provided
        when:
        params = ['catalogueItemDomainType': 'dataModels', 'path': "dm:data model 2|dt: path service test data type"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == "path service test data type"
        catalogueItem.domainType == "PrimitiveType"
        catalogueItem.model.id.equals(dataModel2.id)
    }
}
