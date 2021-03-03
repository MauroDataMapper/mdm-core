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
class DataClassModelImportFunctionalSpec extends ModelImportFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getImportingDataModelId()}/dataClasses"
    }

    @Transactional
    @Override
    String getImportedCatalogueItemId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id, BootstrapModels.FIRST_CLASS_LABEL_ON_FINALISED_EXAMPLE_DATAMODEL).get().id.toString()
    }

    @Override
    String getImportedCatalogueItemDomainType() {
        DataClass.simpleName
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
        DataModel.findByLabel(BootstrapModels.IMPORTING_DATAMODEL_NAME_2).id.toString()
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
    "label": "Second Importing DataModel"
  },
  "importedCatalogueItem": {
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
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "importing class 2",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Second Importing DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    }
  ]
}'''
    }

    //Same as getEditorIndexJson but with one extra imported DataType
    @Override
    String getEditorIndexJsonWithImported() {
        '''{
  "count": 2,
  "items": [
    {
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
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "importing class 2",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Second Importing DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    }
  ]
}'''
    }    

}
