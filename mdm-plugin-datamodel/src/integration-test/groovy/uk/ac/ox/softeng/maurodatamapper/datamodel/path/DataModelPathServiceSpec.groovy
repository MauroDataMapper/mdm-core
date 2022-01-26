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
package uk.ac.ox.softeng.maurodatamapper.datamodel.path

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.path.Path

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Integration
@Rollback
class DataModelPathServiceSpec extends BaseDataModelIntegrationSpec {

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

    DataModel dataModel3
    DataClass dataClass3_1
    DataClass dataClass3_core
    DataClass dataClass3_core_1
    DataClass dataClass3_core_2

    PathService pathService

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

     "data model 3"
           ->     "data class 1"
           ->     "data class core"
                      -> data class 1
                      -> data class 2
     */

    @Override
    void setupDomainData() {
        dataModel1 = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data model 1', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel1)

        dataClass1_1 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 1')

        dataClass1_1_1 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 1_1')
        dataClass1_1.addToDataClasses(dataClass1_1_1)
        dataModel1.addToDataClasses(dataClass1_1)

        dataClass1_2 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 2')
        dataModel1.addToDataClasses(dataClass1_2)

        dataClass1_3 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 3')
        dataModel1.addToDataClasses(dataClass1_3)
        checkAndSave(dataModel1)


        dataModel2 = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data model 2', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel2)

        dataClass2_1 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 1')
        DataType dt = new PrimitiveType(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'path service test data type')
        dataModel2.addToDataTypes(dt)
        dataElement2_1 = new DataElement(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data element 1', dataType: dt)
        dataClass2_1.addToDataElements(dataElement2_1)
        dataModel2.addToDataClasses(dataClass2_1)

        dataClass2_2 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 2')
        dataModel2.addToDataClasses(dataClass2_2)

        dataClass2_3 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 3')
        dataModel2.addToDataClasses(dataClass2_3)

        dataClass2_4 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 4')
        dataModel2.addToDataClasses(dataClass2_4)
        checkAndSave(dataModel2)

        /*
        "data model 3"
            ->     "data class 1"
            ->     "data class core"
                        -> data class 1
                        -> data class 2
        */
        //This is to test that when we have duplicate labels ("data class 1") at different levels then the correct
        //class is retrieved. We intentionally add the core class and its children first.

        dataModel3 = new DataModel(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data model 3', folder: testFolder, authority: testAuthority)
        checkAndSave(dataModel3)

        dataClass3_core = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class core')
        dataClass3_core_1 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 1')
        dataClass3_core_2 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 2')
        dataClass3_core.addToDataClasses(dataClass3_core_1)
        dataClass3_core.addToDataClasses(dataClass3_core_2)
        dataModel3.addToDataClasses(dataClass3_core)

        dataClass3_1 = new DataClass(createdBy: StandardEmailAddress.INTEGRATION_TEST, label: 'data class 1')
        dataModel3.addToDataClasses(dataClass3_1)

        checkAndSave(dataModel3)

        [dataModel1, dataClass1_1, dataClass1_2, dataClass1_3, dataModel2, dataClass2_1, dataClass2_2, dataClass2_3, dataClass2_4, dataElement2_1,
        dataModel3, dataClass3_core, dataClass3_core_1, dataClass3_core_2, dataClass3_1].each {
            log.debug("${it.label}  ${it.id}")
        }
    }

    /*
    Get each of the three DataClasses in data model 1, by specifying the data class ID and label
     */

    void "test get of data classes in data model 1 by data class ID"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        when:
        Path path = Path.from(dataModel1, dataClass1_1)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        //This one (data class 1_1) is nested inside data class 1
        when:
        path = Path.from(dataModel1, dataClass1_1, dataClass1_1_1)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 1_1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        when:
        path = Path.from(dataModel1, dataClass1_2)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 2"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        when:
        path = Path.from(dataModel1, dataClass1_3)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 3"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id
    }

    /*
    Get each of the four DataClasses in data model 2, by specifying the data class ID and label
     */

    void "test get of data classes in data model 2 by data class ID"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        when:
        Path path = Path.from(dataModel2, dataClass2_1)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id

        when:
        path = Path.from(dataModel2, dataClass2_2)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 2"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id

        when:
        path = Path.from(dataModel2, dataClass2_3)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 3"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id

        when:
        path = Path.from(dataModel2, dataClass2_4)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 4"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id
    }


    /*
    Get data model 1 by specifying its ID and label
     */

    void "test get of data model 1"() {
        given:
        setupData()

        when:
        Path path = Path.from(dataModel1)
        CatalogueItem catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data model 1"
        catalogueItem.domainType == "DataModel"

        when: 'providing the ID and using absolute path from the id'
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem.label == "data model 1"
        catalogueItem.domainType == "DataModel"
    }

    /*
    Get data model 2 by specifying its ID and label
    */

    void "test get of data model 2"() {
        given:
        setupData()

        when:
        Path path = Path.from(dataModel2)
        CatalogueItem catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data model 2"
        catalogueItem.domainType == "DataModel"
    }

    /*
    Get the data classes in data model 1 by specifying the ID of the data model and the label of the data class
     */

    void "test get of data classes by label in data model 1"() {
        given:
        setupData()
        Map params
        CatalogueItem catalogueItem

        when:
        Path path = Path.from('dc:data class 1')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        //This is the nested data class
        when:
        path = Path.from('dc:data class 1|dc:data class 1_1')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 1_1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        when:
        path = Path.from('dc:data class 2')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 2"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        when:
        path = Path.from('dc:data class 3')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 3"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        when:
        path = Path.from('dc:data class 4')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem == null

        when: 'absolute path of DC with the correct DM'
        path = Path.from(dataModel1, dataClass1_1)
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id
    }

    /*
    Get the data classes in data model 2 by specifying the ID of the data model and the label of the data class
    */

    void "test get of data classes by label in data model 2"() {
        given:
        setupData()
        Map params
        CatalogueItem catalogueItem

        when:
        Path path = Path.from('dc:data class 1')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel2, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id

        when:
        path = Path.from('dc:data class 2')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel2, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 2"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id

        when:
        path = Path.from('dc:data class 3')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel2, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 3"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id

        when:
        path = Path.from('dc:data class 4')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel2, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 4"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id
    }

    /*
    Get the data element 1 in data model 2
    */

    void "test get of data element"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        //When the data class is root
        when:
        Path path = Path.from(dataModel2, dataClass2_1, dataElement2_1)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "data element 1"
        catalogueItem.domainType == "DataElement"
        catalogueItem.model.id == dataModel2.id

        //When the data model is root
        when:
        path = Path.from('dc:data class 1|de:data element 1')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel2, path) as CatalogueItem

        then:
        catalogueItem.label == "data element 1"
        catalogueItem.domainType == "DataElement"
        catalogueItem.model.id == dataModel2.id
    }

    /*
    Get the data type in data model 2
    */

    void "test get of data type"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        //When the data model ID is provided
        when:
        Path path = Path.from('dt:path service test data type')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel2, path) as CatalogueItem

        then:
        catalogueItem.label == "path service test data type"
        catalogueItem.domainType == "PrimitiveType"
        catalogueItem.model.id == dataModel2.id

        //When the data model label is provided
        when:
        path = Path.from('dm:data model 2|dt:path service test data type')
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then:
        catalogueItem.label == "path service test data type"
        catalogueItem.domainType == "PrimitiveType"
        catalogueItem.model.id == dataModel2.id
    }

    void "test get of data classes in data model 3 by data class label, where the label is duplicated at different levels"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        when: 'look for data class 1 belonging directly to data model 3'
        Path path = Path.from(dataModel3, dataClass3_1)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then: 'we find the class called data class 1 which does not have a parent data class'
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel3.id
        !catalogueItem.parentDataClass

        when: 'look for data class 1 belonging to data class core'
        path = Path.from(dataModel3, dataClass3_core, dataClass3_core_1)
        catalogueItem = pathService.findResourceByPathFromRootClass(DataModel, path) as CatalogueItem

        then: 'we find the class called data class 1 which does have a parent data class called data class core'
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel3.id
        catalogueItem.parentDataClass.label == "data class core"
    }
}
