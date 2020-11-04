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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelUserAccessAndPermissionChangingFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: referenceDataModel
 *
 *  |  GET     | /api/referenceDataModels        | Action: index
 *  |  DELETE  | /api/dataModels/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${id}  | Action: update
 *  |  GET     | /api/dataModels/${id}  | Action: show
 *  |  POST    | /api/folders/${folderId}/dataModels                 | Action: save
 *
 *  |  DELETE  | /api/dataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  PUT     | /api/dataModels/${dataModelId}/readByAuthenticated    | Action: readByAuthenticated
 *  |  DELETE  | /api/dataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 *  |  PUT     | /api/dataModels/${dataModelId}/readByEveryone         | Action: readByEveryone
 *
 *  |  GET     | /api/dataModels/types  | Action: types
 *  |  GET     | /api/dataModels/${dataModelId}/hierarchy  | Action: hierarchy
 *  |  PUT     | /api/dataModels/${dataModelId}/finalise   | Action: finalise
 *
 *  |  PUT     | /api/dataModels/${dataModelId}/newForkModel          | Action: newForkModel
 *  |  PUT     | /api/dataModels/${dataModelId}/newDocumentationVersion  | Action: newDocumentationVersion
 *
 *  |  PUT     | /api/folders/${folderId}/dataModels/${dataModelId}      | Action: changeFolder
 *  |  PUT     | /api/dataModels/${dataModelId}/folder/${folderId}       | Action: changeFolder
 *
 *  |  GET     | /api/dataModels/providers/defaultDataTypeProviders       | Action: defaultDataTypeProviders
 *  |  GET     | /api/dataModels/providers/importers                      | Action: importerProviders
 *  |  GET     | /api/dataModels/providers/exporters                      | Action: exporterProviders
 *  |  GET     | /api/dataModels/${dataModelId}/diff/${otherDataModelId}  | Action: diff
 *
 *  |  POST    | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}                 | Action: importDataModels
 *  |  POST    | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                 | Action: exportDataModels
 *  |  GET     | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModel
 *
 *  |   GET    | /api/dataModels/${dataModelId}/search  | Action: search
 *  |   POST   | /api/dataModels/${dataModelId}/search  | Action: search
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelController
 */
@Integration
@Slf4j
class ReferenceDataModelFunctionalSpec extends ModelUserAccessAndPermissionChangingFunctionalSpec {

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Transactional
    String getTestFolder2Id() {
        Folder.findByLabel('Functional Test Folder 2').id.toString()
    }

    @Transactional
    String getComplexDataModelId() {
        ReferenceDataModel.findByLabel('Second Simple Reference Data Model').id.toString()
    }

    @Transactional
    String getSimpleDataModelId() {
        ReferenceDataModel.findByLabel('Simple Reference Data Model').id.toString()
    }

    @Transactional
    String getDataModelFolderId(String id) {
        ReferenceDataModel.get(id).folder.id.toString()
    }

    @Override
    String getResourcePath() {
        'referenceDataModels'
    }

    @Override
    String getSavePath() {
        "folders/${getTestFolderId()}/${getResourcePath()}"
    }

    @Override
    Map getValidJson() {
        [
            label: 'Functional Test DataModel'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label: 'Complex Test DataModel'
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            description: 'This is a new testing DataModel'
        ]
    }

    @Override
    String getEditorGroupRoleName() {
        GroupRole.CONTAINER_ADMIN_ROLE_NAME
    }

    @Override
    void verifyL01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        response.body().count == 0
    }

    @Override
    void verifyN01Response(HttpResponse<Map> response) {
        verifyResponse OK, response
        response.body().count == 0
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getTestFolderId()
    }

    @Override
    List<String> getEditorAvailableActions() {
        ['show', 'comment', 'editDescription', 'update', 'save', 'softDelete', 'finalise', 'delete']
    }

    List<String> getReaderAvailableActions() {
        ['show', 'comment']
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[Data Standard:Functional Test DataModel] created/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[Data Standard:Functional Test DataModel] changed properties \[description]/
    }

    @Override
    Boolean isDisabledNotDeleted() {
        true
    }

    @Override
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    String getModelType() {
        'ReferenceDataModel'
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 4,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Complex Test DataModel",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier2",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ],
      "author": "admin person",
      "organisation": "brc"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Simple Test DataModel",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier simple",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "SourceFlowDataModel",
      "type": "Data Asset",
      "branchName": "main",
      "documentationVersion": "1.0.0"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "TargetFlowDataModel",
      "type": "Data Asset",
      "branchName": "main",
      "documentationVersion": "1.0.0"
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "domainType": "ReferenceDataModel",
  "availableActions": ['delete', 'show', 'update'],
  "branchName": "main",
  "finalised": false,
  "label": "Reference Data Functional Test Model",
  "type": "ReferenceDataModel",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "documentationVersion": "1.0.0",
  "id": "${json-unit.matches:id}",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  }
}'''
    }

    void 'Test getting available ReferenceDataModel exporters'() {

        when: 'not logged in then accessible'
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "providerType": "ReferenceDataModelExporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "XML Reference Data Exporter",
    "fileExtension": "xml",
    "name": "XmlExporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "canExportMultipleDomains": false,
    "version": "${json-unit.matches:version}",
    "fileType": "text/xml"
  },
  {
    "providerType": "ReferenceDataModelExporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "JSON Reference Data Exporter",
    "fileExtension": "json",
    "name": "JsonExporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "canExportMultipleDomains": false,
    "version": "${json-unit.matches:version}",
    "fileType": "text/json"
  }
]'''
    }

    void 'Test getting available ReferenceDataModel importers'() {

        when: 'not logged in then inaccessible'
        GET('providers/importers')

        then:
        verifyForbidden response

        when: 'logged in'
        loginAuthenticated()
        GET('providers/importers', STRING_ARG)

        then: 'The response is Unauth'
        verifyJsonResponse OK, '''[
  {
    "name": "XmlImporterService",
    "version": "3.0",
    "displayName": "XML Reference Data Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "ReferenceDataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "CsvImporterService",
    "version": "3.0",
    "displayName": "CSV Reference Data Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      
    ],
    "providerType": "ReferenceDataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "JsonImporterService",
    "version": "3.0",
    "displayName": "JSON Reference Data Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "ReferenceDataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  }
]'''

    }

    /*void 'L30 : test getting DataModel hierarchy (as not logged in)'() {
        given:
        String id = getSimpleDataModelId()

        when: 'not logged in'
        GET("${id}/hierarchy")

        then:
        verifyNotFound response, id
    }

    void 'N30 : test getting DataModel hierarchy (as authenticated/no access)'() {
        when: 'authenticated user'
        String id = getSimpleDataModelId()
        loginAuthenticated()
        GET("${id}/hierarchy")

        then:
        verifyNotFound response, id
    }

    void 'R30 : test getting DataModel hierarchy (as reader)'() {
        when: 'logged in as reader'
        String id = getSimpleDataModelId()
        loginReader()
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Simple Test DataModel",
  "availableActions": [
    "show",
    "comment"
  ],
  "branchName": "main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  },
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "classifiers": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier simple",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ],
  "type": "Data Standard",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "dataTypes": [
    
  ],
  "childDataClasses": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "simple",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel",
          "domainType": "DataModel",
          "finalised": false
        }
      ],
      "availableActions": [
        "show",
        "comment"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "dataClasses": [
        
      ],
      "dataElements": [
        
      ]
    }
  ]
}'''
    }

    void 'E30a : test getting DataModel hierarchy (as editor)'() {
        when: 'logged in as editor'
        String id = getSimpleDataModelId()
        loginEditor()
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Simple Test DataModel",
  "availableActions": [
        "delete",
        "softDelete",
        "update",
        "save",
        "show",
        "comment",
        "editDescription",
        "finalise"
      ],
      "branchName": "main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  },
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "classifiers": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier simple",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ],
  "type": "Data Standard",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "dataTypes": [
    
  ],
  "childDataClasses": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataClass",
      "label": "simple",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel",
          "domainType": "DataModel",
          "finalised": false
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
      "dataClasses": [
        
      ],
      "dataElements": [
        
      ]
    }
  ]
}'''
    }

    void 'E30b : test getting complex DataModel hierarchy (as editor)'() {
        when: 'logged in as editor'
        String id = getComplexDataModelId()
        loginEditor()
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Complex Test DataModel",
  "availableActions": [
    "show",
    "comment",
    "editDescription",
    "update",
    "save",
    "softDelete",
    "delete",
    "finalise"
  ],
  "branchName": "main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  },
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "classifiers": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier2",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ],
  "type": "Data Standard",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "author": "admin person",
  "organisation": "brc",
  "dataTypes": [
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
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "enumerationValues": [
        {
          "index": 0,
          "id": "${json-unit.matches:id}",
          "key": "Y",
          "value": "Yes",
          "category": null
        },
        {
          "index": 1,
          "id": "${json-unit.matches:id}",
          "key": "N",
          "value": "No",
          "category": null
        },
        {
          "index": 2,
          "id": "${json-unit.matches:id}",
          "key": "U",
          "value": "Unknown",
          "category": null
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
      ],
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    },
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
      ],
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
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
  ],
  "childDataClasses": [
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
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "maxMultiplicity": 1,
      "minMultiplicity": 0,
      "dataClasses": [
        
      ],
      "dataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "ele1",
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
              "label": "content",
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
          "maxMultiplicity": 20,
          "minMultiplicity": 0
        },
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
          "label": "element2",
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
              "label": "content",
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
          "maxMultiplicity": 1,
          "minMultiplicity": 1
        }
      ]
    },
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
      "description": "dataclass with desc",
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "dataClasses": [
        
      ],
      "dataElements": [
        
      ]
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
      "availableActions": [
        "show",
        "comment",
        "editDescription",
        "update",
        "save",
        "delete"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "maxMultiplicity": -1,
      "minMultiplicity": 1,
      "dataClasses": [
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
          "availableActions": [
            "show",
            "comment",
            "editDescription",
            "update",
            "save",
            "delete"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "parentDataClass": "${json-unit.matches:id}",
          "dataClasses": [
            
          ],
          "dataElements": [
            
          ],
          "parentDataClass": "${json-unit.matches:id}"
        }
      ],
      "dataElements": [
        {
          "id": "${json-unit.matches:id}",
          "domainType": "DataElement",
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
          },
          "maxMultiplicity": 1,
          "minMultiplicity": 1
        }
      ]
    }
  ]
}'''
    }

    void 'L31 : test changing folder from DataModel context (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N31 : test changing folder from DataModel context (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R31 : test changing folder from DataModel context (as reader)'() {
        given:
        String id = getValidId()

        when: 'logged in as reader'
        loginReader()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E31 : test changing folder from DataModel context (as editor)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor of the datamodel but not the folder 2'
        loginEditor()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyNotFound response, getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }

    void 'A31 : test changing folder from DataModel context (as admin)'() {
        given:
        String id = getValidId()

        when: 'logged in as admin'
        loginAdmin()
        PUT("$id/folder/${getTestFolder2Id()}", [:])

        then:
        verifyResponse OK, response

        and:
        getDataModelFolderId(id) == getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }

    void 'L32 : test changing folder from Folder context (as not logged in)'() {
        given:
        String id = getValidId()

        when: 'not logged in'
        PUT("folders/${getTestFolder2Id()}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N32 : test changing folder from Folder context (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        PUT("folders/${getTestFolder2Id()}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R32 : test changing folder from Folder context (as reader)'() {
        given:
        String id = getValidId()

        when: 'logged in as reader'
        loginReader()
        PUT("folders/${getTestFolder2Id()}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E32 : test changing folder from Folder context (as editor)'() {
        given:
        String id = getValidId()

        when: 'logged in as editor of the datamodel but not the folder 2'
        loginEditor()
        PUT("folders/${getTestFolder2Id()}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyNotFound response, getTestFolder2Id()

        cleanup:
        removeValidIdObject(id)
    }

    void 'A32 : test changing folder from Folder context (as admin)'() {
        given:
        String id = getValidId()

        when: 'logged in as admin'
        loginAdmin()
        PUT("folders/${getTestFolder2Id()}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        and:
        getDataModelFolderId(id) == getTestFolder2Id()

        when: 'logged in as reader as no access to folder 2 or reader share'
        loginReader()
        GET(id)

        then:
        verifyNotFound response, id

        when: 'logged in as editor no access to folder 2 but has direct DM access'
        loginEditor()
        GET(id)

        then:
        verifyResponse OK, response

        cleanup:
        removeValidIdObject(id)
    }


    void 'L33 : test diffing 2 DataModels (as not logged in)'() {

        when: 'not logged in'
        GET("${getComplexDataModelId()}/diff/${getSimpleDataModelId()}")

        then:
        verifyNotFound response, getComplexDataModelId()
    }

    void 'N33 : test diffing 2 DataModels (as authenticated/no access)'() {
        when:
        loginAuthenticated()
        GET("${getComplexDataModelId()}/diff/${getSimpleDataModelId()}")

        then:
        verifyNotFound response, getComplexDataModelId()
    }

    void 'R33A : test diffing 2 DataModels (as reader of LH model)'() {
        given:
        String id = getValidId()
        loginAdmin()
        PUT("$id/folder/${getTestFolder2Id()}", [:])
        logout()

        when: 'able to read right model only'
        loginReader()
        GET("${getComplexDataModelId()}/diff/${id}")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R33B : test diffing 2 DataModels (as reader of RH model)'() {
        given:
        String id = getValidId()
        loginAdmin()
        PUT("$id/folder/${getTestFolder2Id()}", [:])
        logout()

        when:
        loginReader()
        GET("${id}/diff/${getComplexDataModelId()}")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R33C : test diffing 2 DataModels (as reader of both models)'() {
        when:
        loginReader()
        GET("${getComplexDataModelId()}/diff/${getSimpleDataModelId()}", STRING_ARG)

        then:
        verifyJsonResponse OK, getExpectedDiffJson()
    }

    void 'L34 : test export a single DataModel (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N34 : test export a single DataModel (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        loginAuthenticated()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0")

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R34 : test export a single DataModel (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "dataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataModel",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false,
    "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
    }
  },
  "exportMetadata": {
    "exportedBy": "reader User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "JsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L35 : test export multiple DataModels (json only exports first id) (as not logged in)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0',
             [dataModelIds: [id, getSimpleDataModelId()]]
        )

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'N35 : test export multiple DataModels (json only exports first id) (as authenticated/no access)'() {
        given:
        String id = getValidId()

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0',
             [dataModelIds: [id, getSimpleDataModelId()]]
        )

        then:
        verifyNotFound response, id

        cleanup:
        removeValidIdObject(id)
    }

    void 'R35 : test export multiple DataModels (json only exports first id) (as reader)'() {
        given:
        String id = getValidId()

        when:
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0',
             [dataModelIds: [id, getSimpleDataModelId()]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
  "dataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test DataModel",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false,
    "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
    }
  },
  "exportMetadata": {
    "exportedBy": "reader User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "JsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        removeValidIdObject(id)
    }

    void 'L36 : test import basic DataModel (as not logged in)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'N36 : test import basic DataModel (as authenticated/no access)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginAuthenticated()
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyNotFound response, testFolderId

        cleanup:
        removeValidIdObject(id)
    }

    void 'R36 : test import basic DataModel (as reader)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginReader()
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyForbidden response

        cleanup:
        removeValidIdObject(id)
    }

    void 'E36A : test import basic DataModel (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        response.body().count == 1
        response.body().items.first().label == 'Functional Test Import'
        response.body().items.first().id != id
        String id2 = response.body().items.first().id

        cleanup:
        removeValidIdObjectUsingTransaction(id2)
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObject(id2, NOT_FOUND)
        removeValidIdObject(id, NOT_FOUND)
    }

    void 'E36B : test import basic DataModel as new documentation version (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: true,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        response.body().count == 1
        response.body().items.first().label == 'Functional Test DataModel'
        response.body().items.first().id != id

        when:
        String newId = response.body().items.first().id
        GET("$newId/versionLinks")

        then:
        verifyResponse OK, response
        response.body().count == 1
        response.body().items.first().domainType == 'VersionLink'
        response.body().items.first().linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label
        response.body().items.first().sourceModel.id == newId
        response.body().items.first().targetModel.id == id
        response.body().items.first().sourceModel.domainType == response.body().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(newId)
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObject(newId, NOT_FOUND)
        removeValidIdObject(id, NOT_FOUND)
    }*/
}