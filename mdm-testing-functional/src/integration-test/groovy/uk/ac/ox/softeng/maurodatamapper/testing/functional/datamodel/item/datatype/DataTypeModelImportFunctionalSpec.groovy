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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item.datatype


import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
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
class DataTypeModelImportFunctionalSpec extends ModelImportFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getImportingDataModelId()}/dataTypes"
    }

    @Transactional
    @Override
    String getImportedCatalogueItemId() {
        DataType.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id, 'gnirts on finalised example data model').get().id.toString()
    }

    @Override
    String getImportedCatalogueItemDomainType() {
        DataType.simpleName
    } 
    
    @Override
    String getModelImportPath() {
        "dataModels/${getImportingDataModelId()}/modelImports"
    }

    @Override
    List getAdditionalModelImportPaths() {
        []
    }     

    @Transactional
    String getImportingDataModelId() {
        DataModel.findByLabel(BootstrapModels.IMPORTING_DATAMODEL_NAME_1).id.toString()
    }

    @Transactional
    String getImportedDataClassId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id, BootstrapModels.FIRST_CLASS_LABEL_ON_FINALISED_EXAMPLE_DATAMODEL).get().id.toString()
    }   

    @Transactional
    String getImportedDataModelId() {
        DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id.toString()
    }     

    String getResourcePathForFinalisedSimpleDataModel() {
        "dataModels/${getImportedDataModelId()}/dataTypes"
    }     

    @Override
    String getExpectedModelImportJson() {
      '''{
  "id": "${json-unit.matches:id}",
  "catalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataModel",
    "label": "First Importing DataModel"
  },
  "importedCatalogueItem": {
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

    void "E03c: Test the save action correctly persists an instance for reference type when the DataClass is imported as editor"() {
        given:
        loginEditor()
        
        //TODO Currently this test fails because the ReferenceType is created. Add a check so that ReferenceType can only
        //be created on a DataClass which is directly owned by or imported into the DataModel
        //when: "The save action is executed using valid data with the imported DataClass which has not yet been imported"
        //POST('', [
        //    domainType    : 'ReferenceType',
        //    label         : 'Functional Reference Type on Imported DataClass',
        //    referenceClass: getImportedDataClassId()
        //])

        //then: "The response is correct"
        //verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response

        when: "A DataClass is imported into the DataModel"
        POST(getModelImportPath(), [
            importedCatalogueItemDomainType: "DataClass",
            importedCatalogueItemId: getImportedDataClassId()
        ], MAP_ARG, true)

        then: "The response is correct"
        verifyResponse HttpStatus.CREATED, response
        String modelImportId = responseBody().id

        when: "The save action is executed using valid data with the imported DataClass"
        POST('', [
            domainType    : 'ReferenceType',
            label         : 'Functional Reference Type on Imported DataClass',
            referenceClass: getImportedDataClassId()
        ])

        then: "The response is correct"
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        assert responseBody().domainType == 'ReferenceType'
        assert responseBody().label == 'Functional Reference Type on Imported DataClass'
        assert responseBody().referenceClass.id == getImportedDataClassId()

        when:
        GET(id, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "ReferenceType",
  "label": "Functional Reference Type on Imported DataClass",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "First Importing DataModel",
      "domainType": "DataModel",
      "finalised": false
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
  "referenceClass": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "first class on example finalised model",
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
}'''

        when: "List the DataTypes on the importing DataModel"
        GET("${getResourcePath()}", STRING_ARG, true)

        then: "The ReferenceType is included in the list as the imported primitive type"
        verifyJsonResponse HttpStatus.OK, '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceType",
      "label": "Functional Reference Type on Imported DataClass",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "First Importing DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "referenceClass": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataClass",
        "label": "first class on example finalised model",
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
    },
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

        when: "List the DataTypes on the DataModel from which the DataClass was imported"
        GET(getResourcePathForFinalisedSimpleDataModel(), STRING_ARG, true)

        then: "The ReferenceType is not unintentionally included in the list"
        verifyJsonResponse HttpStatus.OK, '''{
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
        cleanup:     
        //Delete the Reference Type
        DELETE(id, MAP_ARG)
        verifyResponse HttpStatus.NO_CONTENT, response

        //Delete the ModelImport
        DELETE("${getModelImportPath()}/${modelImportId}", MAP_ARG, true)
        verifyResponse HttpStatus.NO_CONTENT, response        
    }    

}
