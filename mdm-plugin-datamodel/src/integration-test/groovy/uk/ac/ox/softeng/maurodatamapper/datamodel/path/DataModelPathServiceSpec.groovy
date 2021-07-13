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
package uk.ac.ox.softeng.maurodatamapper.datamodel.path


import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.test.BaseDataModelIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.util.Path

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
     */

    @Override
    void setupDomainData() {
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

        [dataModel1, dataClass1_1, dataClass1_2, dataClass1_3, dataModel2, dataClass2_1, dataClass2_2, dataClass2_3, dataClass2_4, dataElement2_1].each {
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
        Path path = Path.from('dm:|dc:data class 1')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        //This is the nested data class
        when:
        path = Path.from('dm:|dc:data class 1|dc:data class 1_1')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 1_1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        when:
        path = Path.from('dm:|dc:data class 2')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 2"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        when:
        path = Path.from('dm:|dc:data class 3')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 3"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel1.id

        when:
        path = Path.from('dm:|dc:data class 4')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel1, path) as CatalogueItem

        then:
        catalogueItem == null
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
        Path path = Path.from('dm:|dc:data class 1')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel2, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 1"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id

        when:
        path = Path.from('dm:|dc:data class 2')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel2, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 2"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id

        when:
        path = Path.from('dm:|dc:data class 3')
        catalogueItem = pathService.findResourceByPathFromRootResource(dataModel2, path) as CatalogueItem

        then:
        catalogueItem.label == "data class 3"
        catalogueItem.domainType == "DataClass"
        catalogueItem.model.id == dataModel2.id

        when:
        path = Path.from('dm:|dc:data class 4')
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
        path = Path.from('dm:|dc:data class 1|de:data element 1')
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
        Path path = Path.from('dm:|dt:path service test data type')
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


}
