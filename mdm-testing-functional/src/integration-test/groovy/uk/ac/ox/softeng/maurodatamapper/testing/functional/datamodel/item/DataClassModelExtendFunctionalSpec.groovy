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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Ignore

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
@Ignore('No longer relevant')
class DataClassModelExtendFunctionalSpec extends FunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getExtendingDataModelId()}/dataClasses"
    }

    String getDataElementsResourcePath() {
        "${getResourcePath()}/${getExtendingDataClassId()}/dataElements"
    }    

    String getChildDataClassesResourcePath() {
        "${getResourcePath()}/${getExtendingDataClassId()}/dataClasses"
    }     

    @Transactional
    String getExtendedCatalogueItemId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXTENDABLE_DATAMODEL_NAME).id, BootstrapModels.FIRST_CLASS_LABEL_ON_FINALISED_EXTENDABLE_DATAMODEL).get().id.toString()
    }

    String getExtendedCatalogueItemDomainType() {
        DataClass.simpleName
    } 
    
    String getModelExtendPath() {
        "dataClasses/${getExtendingDataClassId()}/modelExtends"
    }

    List getAdditionalModelExtendPaths() {
        []
    }

    @Transactional
    String getExtendingDataModelId() {
        DataModel.findByLabel(BootstrapModels.EXTENDING_DATAMODEL_NAME_1).id.toString()
    }

    @Transactional
    String getExtendingDataClassId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.EXTENDING_DATAMODEL_NAME_1).id, 'extending class 1').get().id.toString()
    }    

    @Transactional
    String getExtendedDataClassId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXTENDABLE_DATAMODEL_NAME).id, BootstrapModels.FIRST_CLASS_LABEL_ON_FINALISED_EXTENDABLE_DATAMODEL).get().id.toString()
    }   

    @Transactional
    String getExtendedDataModelId() {
        DataModel.findByLabel(BootstrapModels.FINALISED_EXTENDABLE_DATAMODEL_NAME).id.toString()
    }
  
   /**
     * Extend a DataClass.
     * Note that we are not testing here that ModelExtends are done/not done for different logins -
     * that is done by separate facet tests.
     * Check that the extended item appears in relevant endpoints when the ?extended query parameter is used.
     * Check that the extended item does not appear when the ?extended query parameter is not used.
     */
    void "ME01: Extend DataClass and check that its DataElements are listed in the extending class"() {
        given:
        loginEditor()

        when: "List the resources on the endpoint"
        GET(getResourcePath(), STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getInitialEditorIndexJson()

        when: "The save action is executed with valid data"
        POST(getModelExtendPath(), getModelExtendJson(), MAP_ARG, true)

        then: "The response is correct"
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        assert responseBody().catalogueItem
        assert responseBody().extendedCatalogueItem

        when: "The ModelExtend is requested"
        GET("${getModelExtendPath()}/${id}" , STRING_ARG, true)        

        then: "The response is correct"
        verifyJsonResponse HttpStatus.OK, getExpectedModelExtendJson()

        when: "List the DataElements on the DataElement endpoint without showing extended resources"
        GET("${getDataElementsResourcePath()}?extended=false", STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getDataElementIndexJson()        

        when: "List the resources on the endpoint showing extended resources"
        GET(getDataElementsResourcePath(), STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getDataElementIndexJsonWithExtended()    

        when: "List the child DataClasses on the DataClass endpoint without showing extended resources"
        GET("${getChildDataClassesResourcePath()}?extended=false", STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getDataClassIndexJson()      

        when: "List the child DataClasses on the DataClass endpoint showing extended resources"
        GET("${getChildDataClassesResourcePath()}", STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getDataClassIndexJsonWithExtended()                     

        cleanup:
        DELETE("${getModelExtendPath()}/${id}", MAP_ARG, true)  
        verifyResponse HttpStatus.NO_CONTENT, response
    }    

    //The data class before it extends anything
    String getInitialEditorIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "extending class 1",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Xtending DataM0del 1",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    }
  ]
}'''
    }

    Map getModelExtendJson() {
        [
            extendedCatalogueItemDomainType       : "DataClass",
            extendedCatalogueItemId               : getExtendedDataClassId()
        ]
    }  

    String getExpectedModelExtendJson() {
      '''{
  "id": "${json-unit.matches:id}",
  "catalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "extending class 1",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Xtending DataM0del 1",
        "domainType": "DataModel",
        "finalised": false
      }
    ]
  },
  "extendedCatalogueItem": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "extendable class on extendable data m0del",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Extendable DataModel",
        "domainType": "DataModel",
        "finalised": true
      }
    ]
  }
}'''
    }    

    //DataElements directly belonging to the DataClass
    String getDataElementIndexJson() {
      '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "on extending class 1",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Xtending DataM0del 1",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "extending class 1",
          "domainType": "DataClass"
        }
      ],
      "dataClass": "${json-unit.matches:id}",
      "dataType": {
        "id": "${json-unit.matches:id}",
        "domainType": "PrimitiveType",
        "label": "an example primitive type",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Xtending DataM0del 1",
            "domainType": "DataModel",
            "finalised": false
          }
        ]
      },
      "maxMultiplicity": 1,
      "minMultiplicity": 1
    }
  ]
}'''
    }

    //DataElements directly belonging to the DataClass plus those from the class which has been extended
    String getDataElementIndexJsonWithExtended() {
      '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "on extending class 1",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Xtending DataM0del 1",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "extending class 1",
          "domainType": "DataClass"
        }
      ],
      "dataClass": "${json-unit.matches:id}",
      "dataType": {
        "id": "${json-unit.matches:id}",
        "domainType": "PrimitiveType",
        "label": "an example primitive type",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Xtending DataM0del 1",
            "domainType": "DataModel",
            "finalised": false
          }
        ]
      },
      "maxMultiplicity": 1,
      "minMultiplicity": 1
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "data element 1",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Extendable DataModel",
          "domainType": "DataModel",
          "finalised": true
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "extendable class on extendable data m0del",
          "domainType": "DataClass"
        }
      ],
      "dataClass": "${json-unit.matches:id}",
      "dataType": {
        "id": "${json-unit.matches:id}",
        "domainType": "PrimitiveType",
        "label": "primitive type on extendable data m0del",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Extendable DataModel",
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

    //DataClasses directly belonging to the DataClass
    String getDataClassIndexJson() {
      '''{
  "count": 0,
  "items": [
    
  ]
}'''
    }    

    //DataClasses directly belonging to the DataClass plus extended classes
    String getDataClassIndexJsonWithExtended() {
      '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "i am a child class",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Extendable DataModel",
          "domainType": "DataModel",
          "finalised": true
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "extendable class on extendable data m0del",
          "domainType": "DataClass"
        }
      ],
      "parentDataClass": "${json-unit.matches:id}"
    }
  ]
}'''
    }     

}
