/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.tree

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import net.javacrumbs.jsonunit.core.Option

/**
 * @since 26/03/2021
 */
@Slf4j
@Integration
class DataModelTreeItemFunctionalSpec extends FunctionalSpec{

    @Override
    String getResourcePath() {
        'tree/full/dataModels'
    }

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    void 'L01 - Test render full model tree (not logged in)'(){
        given:
        logout()

        when:
        GET(getComplexDataModelId())

        then:
        verifyNotFound(response, getComplexDataModelId())

    }

    void 'N01 - Test render full model tree (as authenticated/no access)'(){
        given:
        loginAuthenticated()

        when:
        GET(getComplexDataModelId())

        then:
        verifyNotFound(response, getComplexDataModelId())

    }

    void 'R01 - Test render full model tree (as reader)'(){
        given:
        loginReader()

        when:
        GET(getComplexDataModelId(), STRING_ARG)

        then:
        verifyJsonResponse(HttpStatus.OK, getFullTreeRender('[]'), Option.IGNORING_EXTRA_FIELDS)

    }

    void 'E01 - Test render full model tree (as editor)'(){
        given:
        loginEditor()

        when:
        GET(getComplexDataModelId(), STRING_ARG)

        then:
        verifyJsonResponse(HttpStatus.OK, getFullTreeRender(), Option.IGNORING_EXTRA_FIELDS)

    }

    void 'A01 - Test render full model tree (as admin)'(){
        given:
        loginAdmin()

        when:
        GET(getComplexDataModelId(), STRING_ARG)

        then:
        verifyJsonResponse(HttpStatus.OK, getFullTreeRender('''[
    "createModelItem",
    "delete",
    "moveToContainer",
    "moveToFolder",
    "moveToVersionedFolder",
    "softDelete"
  ]'''), Option.IGNORING_EXTRA_FIELDS)

    }


    String getFullTreeRender(String actions = '''[
    "createModelItem",
    "moveToContainer",
    "moveToFolder",
    "moveToVersionedFolder",
    "softDelete"
  ]''') {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Complex Test DataModel",
  "hasChildren": true,
  "availableActions": ''' + actions + ''',
  "deleted": false,
  "finalised": false,
  "superseded": false,
  "documentationVersion": "1.0.0",
  "folder": "${json-unit.matches:id}",
  "type": "Data Standard",
  "branchName": "main",
  "children": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "emptyclass",
      "hasChildren": false,
      "availableActions": [],
      "modelId": "${json-unit.matches:id}"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "integer",
      "hasChildren": false,
      "availableActions": [],
      "modelId": "${json-unit.matches:id}"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "PrimitiveType",
      "label": "string",
      "hasChildren": false,
      "availableActions": [],
      "modelId": "${json-unit.matches:id}"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "ReferenceType",
      "label": "child",
      "hasChildren": false,
      "availableActions": [],
      "modelId": "${json-unit.matches:id}"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "EnumerationType",
      "label": "yesnounknown",
      "hasChildren": true,
      "availableActions": [],
      "modelId": "${json-unit.matches:id}",
      "children": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "EnumerationValue",
          "label": "Y",
          "hasChildren": false,
          "availableActions": [],
          "modelId": "${json-unit.matches:id}",
          "parentId": "${json-unit.matches:id}"
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "EnumerationValue",
          "label": "N",
          "hasChildren": false,
          "availableActions": [],
          "modelId": "${json-unit.matches:id}",
          "parentId": "${json-unit.matches:id}"
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "EnumerationValue",
          "label": "U",
          "hasChildren": false,
          "availableActions": [],
          "modelId": "${json-unit.matches:id}",
          "parentId": "${json-unit.matches:id}"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "parent",
      "hasChildren": true,
      "availableActions": [],
      "modelId": "${json-unit.matches:id}",
      "children": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "child",
          "hasChildren": false,
          "availableActions": [],
          "modelId": "${json-unit.matches:id}",
          "parentId": "${json-unit.matches:id}"
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataClass",
          "label": "child",
          "hasChildren": false,
          "availableActions": [],
          "modelId": "${json-unit.matches:id}",
          "parentId": "${json-unit.matches:id}"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "content",
      "hasChildren": true,
      "availableActions": [],
      "modelId": "${json-unit.matches:id}",
      "children": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "ele1",
          "hasChildren": false,
          "availableActions": [],
          "modelId": "${json-unit.matches:id}",
          "parentId": "${json-unit.matches:id}"
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "element2",
          "hasChildren": false,
          "availableActions": [],
          "modelId": "${json-unit.matches:id}",
          "parentId": "${json-unit.matches:id}"
        }
      ]
    }
  ]
}'''
    }
}
