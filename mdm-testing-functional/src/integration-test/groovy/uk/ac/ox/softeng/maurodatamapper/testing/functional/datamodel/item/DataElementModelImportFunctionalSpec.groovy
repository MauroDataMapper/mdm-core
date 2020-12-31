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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item.datatype


import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelImportFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

/**
 * <pre>
 * Controller: dataType
 *  |  POST    | /api/dataModels/${dataModelId}/dataTypes        | Action: save
 *  |  GET     | /api/dataModels/${dataModelId}/dataTypes        | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: update
 *  |  GET     | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: show
 *
 *  |  POST    | /api/dataModels/${dataModelId}/dataTypes/${otherDataModelId}/${dataTypeId}  | Action: copyDataType
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeController
 */
@Integration
@Slf4j
class DataElementModelImportFunctionalSpec extends ModelImportFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getImportingDataModelId()}/dataClasses/${getContentDataClassId()}/dataElements"
    }

    String getDataTypeResourcePath() {
        "dataModels/${getImportingDataModelId()}/dataTypes"
    }      

    @Transactional
    @Override
    String getImportedCatalogueItemId() {
        String dataClassId = DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id, BootstrapModels.FIRST_CLASS_LABEL_ON_FINALISED_EXAMPLE_DATAMODEL).get().id.toString()
        DataElement.byDataClassIdAndLabel(dataClassId, 'data element 1').get().id.toString()
    }

    @Override
    String getImportedCatalogueItemDomainType() {
        DataElement.simpleName
    } 
    
    @Override
    String getModelImportPath() {
        "dataClasses/${getContentDataClassId()}/modelImports"
    }

    @Override
    List getAdditionalModelImportPaths() {
        ["dataModels/${getImportingDataModelId()}/modelImports"]
    }   

    @Transactional
    String getImportingDataModelId() {
        DataModel.findByLabel(BootstrapModels.IMPORTING_DATAMODEL_NAME_3).id.toString()
    }

    @Transactional
    String getImportedDataClassId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id, BootstrapModels.FIRST_CLASS_LABEL_ON_FINALISED_EXAMPLE_DATAMODEL).get().id.toString()
    }   

    @Transactional
    String getImportedDataModelId() {
        DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id.toString()
    }     

    @Transactional
    String getFinalisedSimpleDataModelId() {
        DataModel.byLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).get().id.toString()
    }

    @Transactional
    String getImportedStringDataTypeId() {
        PrimitiveType.byDataModelIdAndLabel(Utils.toUuid(getFinalisedSimpleDataModelId()), 'gnirts on finalised example data model').get().id.toString()
    }  

    @Transactional
    String getContentDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(importingDataModelId), 'importing class 3').get().id.toString()
    }        

    String getResourcePathForFinalisedSimpleDataModel() {
        "dataModels/${getImportedDataModelId()}/dataTypes"
    }  

    Map getValidJsonWithImportedDataType() {
        [
            label          : 'Functional Test DataElement With Imported DataType',
            maxMultiplicity: 1,
            minMultiplicity: 1,
            dataType       : [id: getImportedStringDataTypeId()]
        ]
    }        

    @Override
    String getExpectedModelImportJson() {
      '''{
  "id": "${json-unit.matches:id}",
  "catalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "importing class 3",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Third Importing DataModel",
        "domainType": "DataModel",
        "finalised": false
      }
    ]
  },
  "importedCatalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataElement",
    "label": "data element 1",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Finalised Example Test DataModel",
        "domainType": "DataModel",
        "finalised": true
      },
      {
        "id": "${json-unit.matches:id}",
        "label": "first class on example finalised model",
        "domainType": "DataClass"
      }
    ]
  }
}'''
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 0,
  "items": []
}'''
    }

    //Same as getEditorIndexJson but with one extra imported DataType
    @Override
    String getEditorIndexJsonWithImported() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "data element 1",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Finalised Example Test DataModel",
          "domainType": "DataModel",
          "finalised": true
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "first class on example finalised model",
          "domainType": "DataClass"
        }
      ],
      "dataClass": "${json-unit.matches:id}",
      "dataType": {
        "id": "${json-unit.matches:id}",
        "domainType": "PrimitiveType",
        "label": "gnirts on finalised example data model",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Finalised Example Test DataModel",
            "domainType": "DataModel",
            "finalised": true
          }
        ]
      },
      "maxMultiplicity": 1,
      "minMultiplicity": 1
    }
  ]
}'''
    }    

    String getExpectedDataElementWithImportedDataTypeJson() {
      '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataElement",
  "label": "Functional Test DataElement With Imported DataType",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Third Importing DataModel",
      "domainType": "DataModel",
      "finalised": false
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "importing class 3",
      "domainType": "DataClass"
    }
  ],
  "availableActions": [
    "show",
    "comment",
    "editDescription",
    "update",
    "save",
    "delete"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "dataClass": "${json-unit.matches:id}",
  "dataType": {
    "id": "${json-unit.matches:id}",
    "domainType": "PrimitiveType",
    "label": "gnirts on finalised example data model",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Finalised Example Test DataModel",
        "domainType": "DataModel",
        "finalised": true
      }
    ]
  },
  "maxMultiplicity": 1,
  "minMultiplicity": 1
}'''
    }

    /**
     * Import a DataElement into a DataClass and check that a DataType import is also created.
     * Note that we are not testing here that ModelImports are done/not done for different logins -
     * that is done by separate facet tests.
     * Check that the imported item appears in relevant endpoints when the ?imported query parameter is used.
     * Check that the imported item does not appear when the ?imported query parameter is not used.
     */
    void "MI02: import DataElement and check that its DataType is listed in the DataType Endpoint"() {
        given:
        loginEditor()

        when: "List the resources on the endpoint"
        GET(getResourcePath(), STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getEditorIndexJson()

        //TODO Prevent a DataElement being created with a DataType that is neither imported into or directly 
        //owned by the DataModel to which the DataElement is being saved.
        //when: "Create a new DataElement with the DataType from another model which has not been imported"    
        //POST(getResourcePath(), getValidJsonWithImportedDataType(), MAP_ARG, true)

        //then: "DataElement is created correctly"
        //verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response          
    
        when: "The save action is executed with valid data"
        POST(getModelImportPath(), getModelImportJson(), MAP_ARG, true)

        then: "The response is correct"
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        assert responseBody().catalogueItem
        assert responseBody().importedCatalogueItem

        when: "The ModelImport is requested"
        GET("${getModelImportPath()}/${id}", STRING_ARG, true)        

        then: "The response is correct"
        verifyJsonResponse HttpStatus.OK, getExpectedModelImportJson()

        when: "List the resources on the DataType endpoint without showing imported resources"
        GET("${getDataTypeResourcePath()}?imported=false", STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getExpectedDataTypeIndexJson()        

        when: "List the resources on the DataType endpoint showing imported resources"
        GET(getDataTypeResourcePath(), STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getExpectedDataTypeIndexJsonWithImported()       

        when: "Create a new DataElement with the imported DataType"    
        POST(getResourcePath(), getValidJsonWithImportedDataType(), MAP_ARG, true)

        then: "DataElement is created correctly"
        verifyResponse HttpStatus.CREATED, response
        String newDataElementId = responseBody().id

        when: "Get the DataElement created with the imported DataType"
        GET("${getResourcePath()}/${newDataElementId}", STRING_ARG, true)

        then: "The DataElement is shown correctly"
        verifyJsonResponse HttpStatus.OK, getExpectedDataElementWithImportedDataTypeJson()  

        cleanup:
        DELETE("${getModelImportPath()}/${id}", MAP_ARG, true) 
        verifyResponse HttpStatus.NO_CONTENT, response
    }    

    String getExpectedDataTypeIndexJson() {
      '''{
  "count": 0,
  "items": []
}'''
    }

    String getExpectedDataTypeIndexJsonWithImported() {
      '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "gnirts on finalised example data model",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Finalised Example Test DataModel",
          "domainType": "DataModel",
          "finalised": true
        }
      ]
    }
  ]
}'''
    }    


}
