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

import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndCopyingInDataModelsFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: dataClass
 *  |  POST    | /api/dataModels/${dataModelId}/dataClasses        | Action: save
 *  |  GET     | /api/dataModels/${dataModelId}/dataClasses        | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: update
 *  |  GET     | /api/dataModels/${dataModelId}/dataClasses/${id}  | Action: show
 *
 *  |  POST    | /api/dataModels/${dataModelId}/dataClasses/${otherDataModelId}/${otherDataClassId}  | Action: copyDataClass
 *
 *  |   GET    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search  | Action: search
 *  |   POST   | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/search  | Action: search
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassController
 */
@Integration
@Slf4j
class DataClassFunctionalSpec extends UserAccessAndCopyingInDataModelsFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getComplexDataModelId()}/dataClasses"
    }

    @Override
    String getEditsPath() {
        'dataClasses'
    }

    @Transactional
    @Override
    String getExpectedTargetId() {
        getDataClassIdByLabel 'parent'
    }

    @Transactional
    String getDataClassIdByLabel(String label) {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(complexDataModelId), label).get().id.toString()
    }

    @Override
    Map getValidJson() {
        [
            label: 'A new DataClass'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label: 'parent'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Adding a description to the DataClass'
        ]
    }

    @Override
    String getShowJson() {
        '''{
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "DataClass",
  "model": "${json-unit.matches:id}",
  "id": "${json-unit.matches:id}",
  "label": "A new DataClass",
  "breadcrumbs": [
    {
      "domainType": "DataModel",
      "finalised": false,
      "id": "${json-unit.matches:id}",
      "label": "Complex Test DataModel"
    }
  ],
  "availableActions": [
    "delete",
    "update",
    "save",
    "show",
    "comment",
    "editDescription"
  ]
}'''
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 3,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "emptyclass",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "description": "dataclass with desc"
    },
    {
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
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "content",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "description": "A dataclass with elements",
      "maxMultiplicity": 1,
      "minMultiplicity": 0
    }
  ]
}'''
    }

    @Override
    void verifyCopiedResponseBody(HttpResponse<Map> response) {
        Map body = response.body()

        assert body.id
        assert body.domainType == 'DataClass'
        assert body.label == 'parent'
        assert body.model == getSimpleDataModelId()
        assert body.breadcrumbs
        assert body.breadcrumbs.size() == 1
        assert body.breadcrumbs.first().id == getSimpleDataModelId()
        assert body.breadcrumbs.first().label == 'Simple Test DataModel'
        assert body.breadcrumbs.first().domainType == 'DataModel'
        assert body.breadcrumbs.first().finalised == false

        assert body.availableActions == [
            'show', 'comment', 'editDescription', 'update', 'save', 'delete'
        ]
        assert body.lastUpdated
        assert body.maxMultiplicity == -1
        assert body.minMultiplicity == 1
    }

    void 'S01 : test searching for metadata "mdk1" in empty dataclass'() {
        given:
        def term = 'mdk1'
        def id = getDataClassIdByLabel('emptyclass')

        when: 'not logged in'
        GET("$id/search?searchTerm=$term")

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("$id/search?searchTerm=$term")

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'S02 : test searching for metadata "mdk1" in content dataclass'() {
        given:
        def term = 'mdk1'
        def id = getDataClassIdByLabel('content')

        when: 'not logged in'
        GET("$id/search?searchTerm=$term")

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("$id/search?searchTerm=$term")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.first().domainType == 'DataElement'
        responseBody().items.first().label == 'ele1'
        responseBody().items.first().breadcrumbs.size() == 2
    }

    void 'S03 : test searching for label "ele*" in content dataclass'() {
        given:
        def term = 'ele*'
        def id = getDataClassIdByLabel('content')

        when: 'not logged in'
        GET("$id/search?searchTerm=$term")

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("$id/search?searchTerm=$term&sort=label")

        then:
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items[0].domainType == 'DataElement'
        responseBody().items[0].label == 'ele1'
        responseBody().items[0].breadcrumbs.size() == 2
        responseBody().items[1].domainType == 'DataElement'
        responseBody().items[1].label == 'element2'
        responseBody().items[1].breadcrumbs.size() == 2
    }

    void 'S04 : test searching for label "ele*" in content dataclass using post'() {
        given:
        def term = 'ele*'
        def id = getDataClassIdByLabel('content')

        when: 'not logged in'
        POST("$id/search", [searchTerm: term])

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        POST("$id/search", [searchTerm: term, sort: 'label'])

        then:
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items[0].domainType == 'DataElement'
        responseBody().items[0].label == 'ele1'
        responseBody().items[0].breadcrumbs.size() == 2
        responseBody().items[1].domainType == 'DataElement'
        responseBody().items[1].label == 'element2'
        responseBody().items[1].breadcrumbs.size() == 2
    }

    /*
    void 'Test getting all DataClasses of a DataModel'() {
        when: 'not logged in'
        def response = restGet("${apiPath}/dataModels/${testDataModel.id}/allDataClasses")

        then:
        verifyUnauthorised response

        when: 'logged in'
        loginEditor()
        response = restGet("${apiPath}/dataModels/${testDataModel.id}/allDataClasses")

        then:
        verifyResponse OK, response, '''{
  "count": 4,
  "items": [
    {
      "domainType": "DataClass",
      "dataModel": "${json-unit.matches:id}",
      "description": "dataclass with desc",
      "id": "${json-unit.matches:id}",
      "label": "emptyclass",
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel"
        }
      ]
    },
    {
      "domainType": "DataClass",
      "dataModel": "${json-unit.matches:id}",
      "maxMultiplicity": -1,
      "id": "${json-unit.matches:id}",
      "label": "parent",
      "minMultiplicity": 1,
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel"
        }
      ]
    },
    {
      "domainType": "DataClass",
      "dataModel": "${json-unit.matches:id}",
      "parentDataClass": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "child",
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel"
        },
        {
          "domainType": "DataClass",
          "id": "${json-unit.matches:id}",
          "label": "parent"
        }
      ]
    },
    {
      "domainType": "DataClass",
      "semanticLinks": [
        {
          "domainType": "CatalogueSemanticLink",
          "linkType": "Does Not Refine",
          "id": "${json-unit.matches:id}",
          "source": {
            "domainType": "DataClass",
            "dataModel": "${json-unit.matches:id}",
            "id": "${json-unit.matches:id}",
            "label": "content",
            "breadcrumbs": [
              {
                "domainType": "DataModel",
                "finalised": false,
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel"
              }
            ]
          },
          "target": {
            "domainType": "DataClass",
            "dataModel": "${json-unit.matches:id}",
            "id": "${json-unit.matches:id}",
            "label": "parent",
            "breadcrumbs": [
              {
                "domainType": "DataModel",
                "finalised": false,
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel"
              }
            ]
          }
        }
      ],
      "dataModel": "${json-unit.matches:id}",
      "description": "A dataclass with elements",
      "maxMultiplicity": 1,
      "id": "${json-unit.matches:id}",
      "label": "content",
      "minMultiplicity": 0,
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel"
        }
      ]
    }
  ]
}'''
    }

    void 'Test getting all content of a DataClass'() {
        when: 'not logged in'
        def response = restGet("${DataClass.findByDataModelAndLabel(testDataModel, 'parent').id}/content")

        then:
        verifyUnauthorised response

        when: 'logged in'
        loginEditor()
        response = restGet("${DataClass.findByDataModelAndLabel(testDataModel, 'parent').id}/content")

        then:
        verifyResponse OK, response, '''{
  "count": 2,
  "items": [
    {
      "domainType": "DataClass",
      "dataModel": "${json-unit.matches:id}",
      "parentDataClass": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "child",
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel"
        },
        {
          "domainType": "DataClass",
          "id": "${json-unit.matches:id}",
          "label": "parent"
        }
      ]
    },
    {
      "domainType": "DataElement",
      "dataClass": "${json-unit.matches:id}",
      "dataType": {
        "domainType": "ReferenceType",
        "dataModel": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "child",
        "breadcrumbs": [
          {
            "domainType": "DataModel",
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel"
          }
        ],
        "referenceClass": {
          "domainType": "DataClass",
          "dataModel": "${json-unit.matches:id}",
          "parentDataClass": "${json-unit.matches:id}",
          "id": "${json-unit.matches:id}",
          "label": "child",
          "breadcrumbs": [
            {
              "domainType": "DataModel",
              "finalised": false,
              "id": "${json-unit.matches:id}",
              "label": "Complex Test DataModel"
            },
            {
              "domainType": "DataClass",
              "id": "${json-unit.matches:id}",
              "label": "parent"
            }
          ]
        }
      },
      "dataModel": "${json-unit.matches:id}",
      "maxMultiplicity": 1,
      "id": "${json-unit.matches:id}",
      "label": "child",
      "minMultiplicity": 1,
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Complex Test DataModel"
        },
        {
          "domainType": "DataClass",
          "id": "${json-unit.matches:id}",
          "label": "parent"
        }
      ]
    }
  ]
}
'''
    }


    */
}