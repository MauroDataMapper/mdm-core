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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponent
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.CREATED

/**
 * <pre>
 * Controller: dataElementComponent
 *  |   POST   | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents | Action:
 *  save
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents | Action:
 *  index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id} |
 *  Action: delete
 *  |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id} |
 *  Action: update
 *  |   GET    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/${id} |
 *  Action: show
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/$
 *{dataElementComponentId}/${type}/${dataElementId} | Action: alterDataElements
 *  |   PUT    | /api/dataModels/${dataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents/$
 *{dataElementComponentId}/${type}/${dataElementId} | Action: alterDataElements
 * </pre>
 *
 * @see uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataElementComponentController
 */
@Integration
@Slf4j
class DataElementComponentFunctionalSpec extends UserAccessFunctionalSpec {

    @Override
    String getResourcePath() {
        "dataModels/${targetDataModelId}/dataFlows/${dataFlowId}/dataClassComponents/${dataClassComponentId}/dataElementComponents"
    }

    @Override
    String getEditsPath() {
        'dataElementComponents'
    }

    @Transactional
    String getTargetDataModelId() {
        DataModel.findByLabel(BootstrapModels.TARGET_DATAMODEL_NAME).id.toString()
    }

    @Transactional
    String getSourceDataModelId() {
        DataModel.findByLabel(BootstrapModels.SOURCE_DATAMODEL_NAME).id.toString()
    }

    @Transactional
    String getDataFlowId() {
        DataFlow.findByLabel(BootstrapModels.DATAFLOW_NAME).id.toString()
    }

    @Transactional
    String getDataClassComponentId() {
        DataClassComponent.findByLabel('bAndCToE').id.toString()
    }

    @Transactional
    String getTargetDataElementId() {
        DataElement.findByLabel('columnG').id.toString()
    }

    @Transactional
    String getSourceDataElementId() {
        DataElement.findByLabel('columnI').id.toString()
    }

    @Transactional
    String getTargetDataElementId2() {
        DataElement.findByLabel('columnO').id.toString()
    }

    @Transactional
    String getSourceDataElementId2() {
        DataElement.findByLabel('columnT').id.toString()
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withInheritedAccessPermissions()
            .whereTestingUnsecuredResource()
            .whereContainerAdminsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'update')
            .whereEditorsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'update')
            .whereAuthorsCanAction('comment', 'editDescription', 'show',)
            .whereReviewersCanAction('comment', 'show')
            .whereReadersCanAction('show')
    }

    @Override
    void verify03ValidResponseBody(HttpResponse<Map> response) {
        assert responseBody().id
        assert responseBody().label == 'Functional Test DataElementComponent'
        assert responseBody().sourceDataElements.size() == 1
        assert responseBody().sourceDataElements.first().id == sourceDataElementId
        assert responseBody().targetDataElements.size() == 1
        assert responseBody().targetDataElements.first().id == targetDataElementId
        assert responseBody().dataClassComponent == dataClassComponentId
    }

    @Override
    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
        verify03ValidResponseBody(response)
    }

    @Override
    Map getValidJson() {
        [
            label             : 'Functional Test DataElementComponent',
            sourceDataElements: [sourceDataElementId],
            targetDataElements: [targetDataElementId]
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label             : 'Functional Test DataElementComponent',
            sourceDataElements: [],
            targetDataElements: []
        ]
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 6,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElementComponent",
      "label": "JOIN KEY",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "TargetFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "Sample DataFlow",
          "domainType": "DataFlow"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "bAndCToE",
          "domainType": "DataClassComponent"
        }
      ],
      "dataClassComponent": "${json-unit.matches:id}",
      "sourceDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnE2",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableC",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnE1",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableB",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ],
      "targetDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnE",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "TargetFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableE",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElementComponent",
      "label": "Direct Copy",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "TargetFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "Sample DataFlow",
          "domainType": "DataFlow"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "bAndCToE",
          "domainType": "DataClassComponent"
        }
      ],
      "dataClassComponent": "${json-unit.matches:id}",
      "sourceDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnF",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableB",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ],
      "targetDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnR",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "TargetFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableE",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElementComponent",
      "label": "CONCAT",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "TargetFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "Sample DataFlow",
          "domainType": "DataFlow"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "bAndCToE",
          "domainType": "DataClassComponent"
        }
      ],
      "dataClassComponent": "${json-unit.matches:id}",
      "sourceDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnG",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableB",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnJ",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableC",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ],
      "targetDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnS",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "TargetFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableE",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElementComponent",
      "label": "CASE",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "TargetFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "Sample DataFlow",
          "domainType": "DataFlow"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "bAndCToE",
          "domainType": "DataClassComponent"
        }
      ],
      "dataClassComponent": "${json-unit.matches:id}",
      "sourceDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnH",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableB",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnI",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableB",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ],
      "targetDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnT",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "TargetFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableE",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElementComponent",
      "label": "TRIM",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "TargetFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "Sample DataFlow",
          "domainType": "DataFlow"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "bAndCToE",
          "domainType": "DataClassComponent"
        }
      ],
      "dataClassComponent": "${json-unit.matches:id}",
      "sourceDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnJ",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableC",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ],
      "targetDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnU",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "TargetFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableE",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElementComponent",
      "label": "CONCAT",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "TargetFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "Sample DataFlow",
          "domainType": "DataFlow"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "bAndCToE",
          "domainType": "DataClassComponent"
        }
      ],
      "dataClassComponent": "${json-unit.matches:id}",
      "sourceDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnM",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableC",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnG",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableB",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnL",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "SourceFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableC",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ],
      "targetDataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "columnV",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
            {
              "id": "${json-unit.matches:id}",
              "label": "TargetFlowDataModel",
              "domainType": "DataModel",
              "finalised": false
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "tableE",
              "domainType": "DataClass"
            }
          ],
          "dataClass": "${json-unit.matches:id}",
          "dataType": "${json-unit.matches:id}"
        }
      ]
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataElementComponent",
  "label": "Functional Test DataElementComponent",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "TargetFlowDataModel",
      "domainType": "DataModel",
      "finalised": false
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "Sample DataFlow",
      "domainType": "DataFlow"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "bAndCToE",
      "domainType": "DataClassComponent"
    }
  ],
  "availableActions": [
    "show"
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "definition": null,
  "dataClassComponent": "${json-unit.matches:id}",
  "sourceDataElements": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "columnI",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "SourceFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "tableB",
          "domainType": "DataClass"
        }
      ],
      "dataClass": "${json-unit.matches:id}",
      "dataType": "${json-unit.matches:id}"
    }
  ],
  "targetDataElements": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "columnG",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "SourceFlowDataModel",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "tableB",
          "domainType": "DataClass"
        }
      ],
      "dataClass": "${json-unit.matches:id}",
      "dataType": "${json-unit.matches:id}"
    }
  ]
}'''
    }

}