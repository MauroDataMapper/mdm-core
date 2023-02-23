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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.referencedata

import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * <pre>
 * Controller: referenceDataValue
 * | GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues                            | Action: index
 * | POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues                            | Action: save
 * | GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${referenceDataValueId}    | Action: show
 * | PUT    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${referenceDataValueId}    | Action: update
 * | DELETE | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${referenceDataValueId}    | Action: delete
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValueController
 */
@Integration
@Slf4j
class ReferenceDataValueFunctionalSpec extends UserAccessFunctionalSpec {

    @Shared
    int rowNumber = 100

    @Shared
    Path resourcesPath

    @RunOnce
    def setup() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'referencedata').toAbsolutePath()
    }

    private byte[] loadJsonFile(String filename) {
        Path jsonFilePath = resourcesPath.resolve("${filename}.json").toAbsolutePath()
        assert Files.exists(jsonFilePath)
        Files.readAllBytes(jsonFilePath)
    }

    @Override
    String getResourcePath() {
        "referenceDataModels/$simpleReferenceDataModelId/referenceDataValues"
    }

    @Override
    String getEditsFullPath(String id) {
        "referenceDataElements/$referenceDataElementId"
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereTestingUnsecuredResource()
            .withoutAvailableActions()
            .whereAuthors {
                cannotEditDescription()
                cannotUpdate()
            }
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[ReferenceDataValue:.+?] added to component \[ReferenceDataElement:.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[ReferenceDataValue:.+?] changed properties \[value]/
    }

    @Transactional
    String getSimpleReferenceDataModelId() {
        ReferenceDataModel.findByLabel(BootstrapModels.SIMPLE_REFERENCE_MODEL_NAME).id.toString()
    }

    @Transactional
    String getReferenceDataElementId() {
        ReferenceDataElement.findByLabel('Organisation code').id.toString()
    }

    @Override
    Map getValidJson() {
        // increment the rowNumber so that we don't try to break the unique constraint
        rowNumber = rowNumber + 1
        [
            rowNumber           : rowNumber,
            value               : 'Functional Test ReferenceDataValue',
            referenceDataElement: referenceDataElementId
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            rowNumber           : -1,
            value               : null,
            referenceDataElement: null,
        ]
    }

    @Override
    Map getValidNonDescriptionUpdateJson() {
        [
            value: 'Updated ReferenceDataValue'
        ]
    }

    @Override
    Map getValidDescriptionOnlyUpdateJson() {
        getValidNonDescriptionUpdateJson()
    }

    @Override
    void verify03ValidResponseBody(HttpResponse<Map> response) {
        assert response.body().id
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 200,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "rowNumber": 1,
      "value": "Organisation 1",
      "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Organisation name",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ],
        "columnNumber": 1,
        "referenceDataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "ReferencePrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Simple Reference Data Model",
              "domainType": "ReferenceDataModel",
              "finalised": false
            }
          ]
        }
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "rowNumber": 1,
      "value": "ORG1",
      "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Organisation code",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ],
        "columnNumber": 0,
        "referenceDataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "ReferencePrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Simple Reference Data Model",
              "domainType": "ReferenceDataModel",
              "finalised": false
            }
          ]
        }
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "rowNumber": 2,
      "value": "Organisation 2",
      "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Organisation name",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ],
        "columnNumber": 1,
        "referenceDataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "ReferencePrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Simple Reference Data Model",
              "domainType": "ReferenceDataModel",
              "finalised": false
            }
          ]
        }
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "rowNumber": 2,
      "value": "ORG2",
      "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Organisation code",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ],
        "columnNumber": 0,
        "referenceDataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "ReferencePrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Simple Reference Data Model",
              "domainType": "ReferenceDataModel",
              "finalised": false
            }
          ]
        }
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "rowNumber": 3,
      "value": "Organisation 3",
      "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Organisation name",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ],
        "columnNumber": 1,
        "referenceDataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "ReferencePrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Simple Reference Data Model",
              "domainType": "ReferenceDataModel",
              "finalised": false
            }
          ]
        }
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "rowNumber": 3,
      "value": "ORG3",
      "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Organisation code",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ],
        "columnNumber": 0,
        "referenceDataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "ReferencePrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Simple Reference Data Model",
              "domainType": "ReferenceDataModel",
              "finalised": false
            }
          ]
        }
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "rowNumber": 4,
      "value": "Organisation 4",
      "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Organisation name",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ],
        "columnNumber": 1,
        "referenceDataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "ReferencePrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Simple Reference Data Model",
              "domainType": "ReferenceDataModel",
              "finalised": false
            }
          ]
        }
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "rowNumber": 4,
      "value": "ORG4",
      "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Organisation code",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ],
        "columnNumber": 0,
        "referenceDataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "ReferencePrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Simple Reference Data Model",
              "domainType": "ReferenceDataModel",
              "finalised": false
            }
          ]
        }
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "rowNumber": 5,
      "value": "Organisation 5",
      "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Organisation name",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ],
        "columnNumber": 1,
        "referenceDataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "ReferencePrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Simple Reference Data Model",
              "domainType": "ReferenceDataModel",
              "finalised": false
            }
          ]
        }
      }
    },
    {
      "id": "${json-unit.matches:id}",
      "rowNumber": 5,
      "value": "ORG5",
      "referenceDataElement": {
        "id": "${json-unit.matches:id}",
        "domainType": "ReferenceDataElement",
        "label": "Organisation code",
        "model": "${json-unit.matches:id}",
        "breadcrumbs": [
          {
            "id": "${json-unit.matches:id}",
            "label": "Simple Reference Data Model",
            "domainType": "ReferenceDataModel",
            "finalised": false
          }
        ],
        "columnNumber": 0,
        "referenceDataType": {
          "id": "${json-unit.matches:id}",
          "domainType": "ReferencePrimitiveType",
          "label": "string",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "Simple Reference Data Model",
              "domainType": "ReferenceDataModel",
              "finalised": false
            }
          ]
        }
      }
    }
  ]
}
'''
    }
}
