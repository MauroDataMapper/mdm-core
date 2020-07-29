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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item


import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessAndCopyingInDataModelsFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

/**
 * <pre>
 * Controller: dataClass
 *  |  POST    | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses        | Action: save
 *  |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses        | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}  | Action: update
 *  |  GET     | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${id}  | Action: show
 *
 *  |  POST  | /api/dataModels/${dataModelId}/dataClasses/${dataClassId}/dataClasses/${otherDataModelId}/${otherDataClassId}  | Action: copyDataClass
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassController
 */
@Integration
@Slf4j
class NestedDataClassFunctionalSpec extends UserAccessAndCopyingInDataModelsFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${getComplexDataModelId()}/dataClasses/${getParentDataClassId()}/dataClasses"
    }

    String getCopyPath(String fromId) {
        "dataModels/${getSimpleDataModelId()}/dataClasses/${getSimpleDataClassId()}/dataClasses/${getComplexDataModelId()}/${fromId}"
    }

    @Override
    String getAlternativePath(String id) {
        "dataModels/${getSimpleDataModelId()}/dataClasses/${getSimpleDataClassId()}/dataClasses/$id"
    }

    @Override
    String getEditsPath() {
        'dataClasses'
    }

    @Override
    String getCatalogueItemDomainType() {
        'dataClasses'
    }

    @Transactional
    @Override
    String getExpectedTargetId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(complexDataModelId), 'parent').get().id.toString()
    }

    @Transactional
    String getParentDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(complexDataModelId), 'parent').get().id.toString()
    }

    @Transactional
    String getSimpleDataClassId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(simpleDataModelId), 'simple').get().id.toString()
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
            label: 'child'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'Adding a description to the DataClass'
        ]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
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
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataClass",
  "label": "A new DataClass",
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
  "availableActions": [
    "delete",
    "update",
    "save",
    "show",
    "comment",
    "editDescription"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "parentDataClass": "${json-unit.matches:id}"
}'''
    }

    @Override
    void verifyCopiedResponseBody(HttpResponse<Map> response) {
        Map body = response.body()

        assert body.domainType == 'DataClass'
        assert body.domainType == 'DataClass'
        assert body.label == 'parent'
        assert body.model == getSimpleDataModelId()
        assert body.breadcrumbs
        assert body.breadcrumbs.size() == 2
        assert body.breadcrumbs[0].id == getSimpleDataModelId()
        assert body.breadcrumbs[0].label == 'Simple Test DataModel'
        assert body.breadcrumbs[0].domainType == 'DataModel'
        assert body.breadcrumbs[0].finalised == false
        assert body.breadcrumbs[1].id == getSimpleDataClassId()
        assert body.breadcrumbs[1].label == 'simple'
        assert body.breadcrumbs[1].domainType == 'DataClass'

        assert body.availableActions == [
            'show', 'comment', 'editDescription', 'update', 'save', 'delete'
        ]
        assert body.lastUpdated
        assert body.maxMultiplicity == -1
        assert body.minMultiplicity == 1
    }
}