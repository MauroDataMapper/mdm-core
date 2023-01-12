/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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


import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndCopyingInDataModelsFunctionalSpec
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
class DataTypeFunctionalSpec extends UserAccessAndCopyingInDataModelsFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getComplexDataModelId()}/dataTypes"
    }

    @Override
    String getEditsPath() {
        'primitiveTypes'
    }

    String getCatalogueItemDomainType() {
        'dataTypes'
    }

    @Transactional
    @Override
    String getExpectedTargetId() {
        DataType.byDataModelIdAndLabel(Utils.toUuid(complexDataModelId), 'string').get().id.toString()
    }

    @Transactional
    String getReferenceDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(getComplexDataModelId()), 'parent').get().id.toString()
    }

    @Override
    Map getValidJson() {
        [
            domainType: 'PrimitiveType',
            label: 'Functional Data Type'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            domainType: 'PrimitiveType'
        ]
    }

    @Override
    Map getInvalidUpdateJson() {
        [
            label: 'string'
        ]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 4,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "string",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "integer",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "EnumerationType",
      "label": "yesnounknown",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "enumerationValues": [
        {
          "index": 0,
          "id": "${json-unit.matches:id}",
          "key": "Y",
          "value": "Yes",
          "category": null
        },
        {
          "index": 2,
          "id": "${json-unit.matches:id}",
          "key": "U",
          "value": "Unknown",
          "category": null
        },
        {
          "index": 1,
          "id": "${json-unit.matches:id}",
          "key": "N",
          "value": "No",
          "category": null
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceType",
      "label": "child",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "referenceClass": {
        "id": "${json-unit.matches:id}",
        "domainType": "DataClass",
        "label": "child",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel",
            "domainType": "DataModel",
            "finalised": false
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "parent",
            "domainType": "DataClass"
          }
        ],
        "parentDataClass": "${json-unit.matches:id}"
      }
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "PrimitiveType",
  "label": "Functional Data Type",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Complex Test DataModel",
      "domainType": "DataModel",
      "finalised": false
    }
  ],
  "availableActions": [
    "show"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}"
}'''
    }

    @Override
    void verifyCopiedResponseBody(HttpResponse<Map> response) {
        Map body = responseBody()

        assert body.id
        assert body.domainType == 'PrimitiveType'
        assert body.label == 'string'
        assert body.model == getSimpleDataModelId()
        assert body.breadcrumbs
        assert body.breadcrumbs.size() == 1
        assert body.breadcrumbs.first().id == getSimpleDataModelId()
        assert body.breadcrumbs.first().label == 'Simple Test DataModel'
        assert body.breadcrumbs.first().domainType == 'DataModel'
        assert body.breadcrumbs.first().finalised == false

        assert body.availableActions == getEditorModelItemAvailableActions().sort()
        assert body.lastUpdated
    }

    void 'DT01: Test the save action correctly persists an instance for enumeration type'() {
        given:
        loginCreator()
        Map validJson = [
            domainType       : 'EnumerationType',
            label            : 'Functional Enumeration Type',
            enumerationValues: [
                [key: 'a', value: 'wibble'],
                [key: 'b', value: 'wobble']
            ]
        ]

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        assert responseBody().domainType == 'EnumerationType'
        assert responseBody().label == 'Functional Enumeration Type'
        assert responseBody().enumerationValues.size() == 2

        when:
        GET(id, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "EnumerationType",
  "label": "Functional Enumeration Type",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Complex Test DataModel",
      "domainType": "DataModel",
      "finalised": false
    }
  ],
  "availableActions": [
    "show","comment","editDescription","update","save","delete"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "enumerationValues": [
    {
      "index": 0,
      "id": "${json-unit.matches:id}",
      "key": "a",
      "value": "wibble",
      "category": null
    },
    {
      "index": 1,
      "id": "${json-unit.matches:id}",
      "key": "b",
      "value": "wobble",
      "category": null
    }
  ]
}
'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'DT02: Test the save action correctly persists an instance for reference type'() {
        given:
        loginCreator()


        when: 'The save action is executed with valid data'
        POST('', [
            domainType    : 'ReferenceType',
            label         : 'Functional Reference Type',
            referenceClass: getReferenceDataClassId()
        ])


        then: 'The response is correct'
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        assert responseBody().domainType == 'ReferenceType'
        assert responseBody().label == 'Functional Reference Type'
        assert responseBody().referenceClass.id == getReferenceDataClassId()

        when:
        GET(id, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "ReferenceType",
  "label": "Functional Reference Type",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Complex Test DataModel",
      "domainType": "DataModel",
      "finalised": false
    }
  ],
  "availableActions": [
   "show","comment","editDescription","update","save","delete"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "referenceClass": {
    "id": "${json-unit.matches:id}",
    "domainType": "DataClass",
    "label": "parent",
    "model": "${json-unit.matches:id}",
    "breadcrumbs": [
      {
        "id": "${json-unit.matches:id}",
        "label": "Complex Test DataModel",
        "domainType": "DataModel",
        "finalised": false
      }
    ],
    "maxMultiplicity": -1,
    "minMultiplicity": 1
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'DT03: Test the save action correctly persists an instance for model data type'() {
        given:
        UUID modelId = UUID.randomUUID()
        loginCreator()
        Map validJson = [
            domainType             : 'ModelDataType',
            label                  : 'Functional ModelData Type',
            modelResourceId        : modelId,
            modelResourceDomainType: 'Terminology'
        ]

        when: 'The save action is executed with valid data'
        POST('', validJson)

        then: 'The response is correct'
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        responseBody().domainType == 'ModelDataType'
        responseBody().label == 'Functional ModelData Type'
        responseBody().modelResourceId == modelId.toString()
        responseBody().modelResourceDomainType == 'Terminology'

        when:
        GET(id, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "ModelDataType",
  "label": "Functional ModelData Type",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "Complex Test DataModel",
      "domainType": "DataModel",
      "finalised": false
    }
  ],
  "availableActions": [
    "show","comment","editDescription","update","save","delete"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "modelResourceId": "${json-unit.matches:id}",
  "modelResourceDomainType": "Terminology"
}
'''

        cleanup:
        removeValidIdObject(id)
    }

}