/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.async.DomainExport
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.DataModelPluginMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.TestMergeData
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelUserAccessPermissionChangingAndVersioningFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.web.mime.MimeType
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.spockframework.util.Assert

import java.util.concurrent.CancellationException
import java.util.concurrent.Future

import static io.micronaut.http.HttpStatus.ACCEPTED
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * <pre>
 * Controller: dataModel
 *
 *  |  GET     | /api/dataModels        | Action: index
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
 *  |  GET     | /api/dataModels/${dataModelId}/diff/${otherModelId}  | Action: diff
 *
 *  |  POST    | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}                 | Action: importDataModels
 *  |  POST    | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                 | Action: exportDataModels
 *  |  GET     | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportDataModel
 *
 *  |   GET    | /api/dataModels/${dataModelId}/search  | Action: search
 *  |   POST   | /api/dataModels/${dataModelId}/search  | Action: search
 *
 *  |   POST   | /api/dataModels/${dataModelId}/subset/${otherDataModelId}      | Action: subset
 *  |   GET    | /api/dataModels/${dataModelId}/intersects/${otherDataModelId}  | Action: search
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelController
 */
@Integration
@Slf4j
class DataModelFunctionalSpec extends ModelUserAccessPermissionChangingAndVersioningFunctionalSpec {

    DataModelPluginMergeBuilder builder

    AsyncJobService asyncJobService

    def setup() {
        builder = new DataModelPluginMergeBuilder(this)
    }

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Transactional
    String getSimpleDataModelId() {
        DataModel.findByLabel('Simple Test DataModel').id.toString()
    }

    @Transactional
    String getLeftHandDiffModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Transactional
    String getRightHandDiffModelId() {
        DataModel.findByLabel('Simple Test DataModel').id.toString()
    }

    @Transactional
    @Override
    String getModelFolderId(String id) {
        DataModel.get(id).folder.id.toString()
    }

    @Override
    String getResourcePath() {
        'dataModels'
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
    String getModelType() {
        'DataModel'
    }

    @Override
    String getModelUrlType() {
        'dataModels'
    }

    @Override
    String getModelPrefix() {
        'dm'
    }

    @Override
    String getEditorIndexJson() {
        '''{
  "count": 5,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "Finalised Example Test DataModel",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "modelVersion": "1.0.0"
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "TargetFlowDataModel",
      "type": "Data Asset",
      "branchName": "main",
      "documentationVersion": "1.0.0"
    },
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
          "label": "test classifier",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier2",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ],
      "author": "admin person",
      "organisation": "brc"
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
    }
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Functional Test DataModel",
  "finalised": false,
  "type": "Data Standard",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "documentationVersion": "1.0.0",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper",
    "defaultAuthority": true
  },
  "availableActions": [
    "show"
  ],
  "branchName":"main"
}'''
    }

    void waitForAysncToComplete(String id) {
        log.debug('Waiting to complete {}', id)
        Future p = asyncJobService.getAsyncJobFuture(id)
        try {
            p.get()
        } catch (CancellationException ignored) {
        }
        log.debug('Async Job Completed')
    }

    @Transactional
    void cleanupDomainExport(String id) {
        if (id) DomainExport.get(id).delete(flush: true)
    }

    void 'DM01 : Test getting DataModel types'() {
        when: 'not logged in'
        GET('types')

        then:
        verifyForbidden response

        when: 'logged in'
        loginAuthenticated()
        GET('types')

        then:
        verifyResponse OK, response
        response.getBody(String).get() == '["Data Asset","Data Standard"]'
    }

    void 'DM02 : Test getting available DataModel default datatype providers'() {
        when: 'not logged in'
        GET('providers/defaultDataTypeProviders')

        then:
        verifyForbidden response

        when: 'logged in as authenticated user'
        loginAuthenticated()
        GET('providers/defaultDataTypeProviders', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "dataTypes": [
      {
        "domainType": "PrimitiveType",
        "description": "A piece of text",
        "label": "Text"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A whole number",
        "label": "Number"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A decimal number",
        "label": "Decimal"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A date",
        "label": "Date"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A date with a timestamp",
        "label": "DateTime"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A timestamp",
        "label": "Timestamp"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A true or false value",
        "label": "Boolean"
      },
      {
        "domainType": "PrimitiveType",
        "description": "A time period in arbitrary units",
        "label": "Duration"
      }
    ],
    "displayName": "Basic Default DataTypes",
    "name": "DataTypeService",
    "version": "1.0.0"
  },
  {
    "name":"ProfileSpecificationDataTypeProvider",
    "version":"1.0.0",
    "displayName":"Profile Specification DataTypes",
    "dataTypes":
    [
        {
            "domainType":"PrimitiveType",
            "label":"boolean",
            "description":"logical Boolean [true/false)"
        },
        {
            "domainType":"PrimitiveType",
            "label":"string",
            "description":"short variable-length character string (plain-text)"
        },
        {
            "domainType":"PrimitiveType",
            "label":"text",
            "description":"long variable-length character string (may include html / markdown)"
        },
        {
            "domainType":"PrimitiveType",
            "label":"int",
            "description":"integer"
        },
        {
            "domainType":"PrimitiveType",
            "label":"decimal",
            "description":"decimal"
        },
        {
            "domainType":"PrimitiveType",
            "label":"date",
            "description":"calendar date [year, month, day)"
        },
        {
            "domainType":"PrimitiveType",
            "label":"datetime",
            "description":"date and time, excluding time zone"
        },
        {
            "domainType":"PrimitiveType",
            "label":"time",
            "description":"time of day [no time zone)"
        },
        {
            "domainType":"PrimitiveType",
            "label":"folder",
            "description":"pointer to a folder in this Mauro instance"
        },
        {
            "domainType":"PrimitiveType",
            "label":"model",
            "description":"pointer to a model in this Mauro instance"
        },
        {
            "domainType":"PrimitiveType",
            "label":"json",
            "description":"a text field containing valid json syntax"
        }
    ]
  }
]'''
    }

    void 'DM03 : Test getting available DataModel exporters'() {

        when: 'not logged in then accessible'
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "name": "DataModelJsonExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON DataModel Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [],
    "providerType": "DataModelExporter",
    "fileExtension": "json",
    "contentType": "application/mauro.datamodel+json",
    "canExportMultipleDomains": true
  },
  {
    "name": "DataModelXmlExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML DataModel Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [],
    "providerType": "DataModelExporter",
    "fileExtension": "xml",
    "contentType": "application/mauro.datamodel+xml",
    "canExportMultipleDomains": true
  }
]'''
    }

    void 'DM04 : Test getting available DataModel importers'() {

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
    "name": "DataModelJsonImporterService",
    "version": "3.1",
    "displayName": "JSON DataModel Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "DataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": true
  },
  {
    "name": "DataModelXmlImporterService",
    "version": "5.1",
    "displayName": "XML DataModel Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [

    ],
    "providerType": "DataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": true
  }
]'''
    }

    void 'DM-#prefix-01 : test getting DataModel hierarchy [not allowed] (as #name)'() {
        given:
        String id = getSimpleDataModelId()

        when:
        login(name)
        GET("${id}/hierarchy")

        then:
        verifyNotFound response, id

        where:
        prefix | name
        'LO'   | null
        'NA'   | 'Authenticated'
    }

    void 'DM-#prefix-01 : test getting DataModel hierarchy [allowed] (as #name)'() {
        given:
        def miActions = (actions - ResourceActions.DISALLOWED_MODELITEM_ACTIONS)
        if (name == 'Editor') miActions << ResourceActions.DELETE_ACTION
        String expectedJson = '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Simple Test DataModel",
  "availableActions": ''' + JsonOutput.toJson(actions) + ''',
  "branchName": "main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper",
    "defaultAuthority": true
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
      "availableActions":''' + JsonOutput.toJson(miActions) + ''',
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "dataClasses": [

      ],
      "dataElements": [

      ]
    }
  ]
}'''

        when: 'logged in'
        String id = getSimpleDataModelId()
        login(name)
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedJson

        where:
        prefix | name             | actions
        'RE'   | 'Reader'         | expectations.readerAvailableActions
        'RV'   | 'Reviewer'       | expectations.reviewerAvailableActions
        'AU'   | 'Author'         | expectations.authorAvailableActions
        'ED'   | 'Editor'         | expectations.editorAvailableActions
        'CA'   | 'ContainerAdmin' | expectations.containerAdminAvailableActions
        'AD'   | 'Admin'          | expectations.containerAdminAvailableActions
    }

    void 'DM-#prefix-02 : test getting complex DataModel hierarchy [allowed] (as #name)'() {
        given:
        def miActions = (actions - ResourceActions.DISALLOWED_MODELITEM_ACTIONS)
        if (name == 'Editor') miActions << ResourceActions.DELETE_ACTION
        String expectedJson = '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Complex Test DataModel",
  "availableActions": ''' + JsonOutput.toJson(actions) +
                              ''',
  "branchName": "main",
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper",
    "defaultAuthority": true
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
      "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
      "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
      "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
      "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
      "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
          "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
          "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
      "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
      "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
          "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
          "availableActions": ''' + JsonOutput.toJson(miActions) + ''',
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
        when: 'logged in as editor'
        String id = getComplexDataModelId()
        login(name)
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedJson

        where:
        prefix | name             | actions
        'RE'   | 'Reader'         | expectations.readerAvailableActions
        'RV'   | 'Reviewer'       | expectations.reviewerAvailableActions
        'AU'   | 'Author'         | expectations.authorAvailableActions
        'ED'   | 'Editor'         | expectations.editorAvailableActions
        'CA'   | 'ContainerAdmin' | expectations.containerAdminAvailableActions
        'AD'   | 'Admin'          | expectations.containerAdminAvailableActions
    }


    void 'DM-#prefix-06 : test export a single DataModel (as #name)'() {
        given:
        String id = getValidId()

        when:
        login(name)
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", canRead ? STRING_ARG : MAP_ARG)

        then:
        if (!canRead) verifyNotFound response, id
        else {
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
    "exportedBy": "${json-unit.any-string}",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "DataModelJsonExporterService",
      "version": "3.1"
    }
  }
}'''
        }

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name             | canRead
        'LO'   | null             | false
        'NA'   | 'Authenticated'  | false
        'RE'   | 'Reader'         | true
        'RV'   | 'Reviewer'       | true
        'AU'   | 'Author'         | true
        'ED'   | 'Editor'         | true
        'CA'   | 'ContainerAdmin' | true
        'AD'   | 'Admin'          | true
    }

    void 'DM-#prefix-06a : test async export a single DataModel (as #name)'() {
        given:
        String deId
        String id = getValidId()

        when:
        login(name)
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1?asynchronous=true")

        then:
        if (!canRead) verifyNotFound response, id
        else {
            verifyResponse ACCEPTED, response

            when:
            String jobId = responseBody().id
            waitForAysncToComplete(jobId)
            GET("asyncJobs/$jobId", MAP_ARG, true)

            then:
            verifyResponse(OK, response)
            responseBody().status == 'COMPLETED'
            responseBody().message ==~ /Download at ${baseUrl}domainExports\/.+?\/download/

            when:
            GET('domainExports', STRING_ARG, true)

            then:
            verifyJsonResponse(OK, '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "exported": {
        "domainType": "DataModel",
        "domainId": "${json-unit.matches:id}"
      },
      "exporter": {
        "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
        "name": "DataModelJsonExporterService",
        "version": "${json-unit.matches:version}"
      },
      "export": {
        "fileName": "Functional Test DataModel.json",
        "contentType": "application/mauro.datamodel+json",
        "fileSize": "${json-unit.any-number}"
      },
      "exportedOn": "${json-unit.matches:offsetDateTime}",
      "exportedBy": "${json-unit.regex}.+?@.+?",
      "links": {
        "relative": "${json-unit.regex}/api/domainExports/[\\\\w-]+?/download",
        "absolute": "${json-unit.regex}http://localhost:\\\\d+/api/domainExports/.+?/download"
      }
    }
  ]
}''')

            when:
            GET('domainExports', MAP_ARG, true)

            then:
            verifyResponse(OK, response)

            when:
            deId = responseBody().items.first().id
            GET(responseBody().items.first().links.absolute, STRING_ARG, true)

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
    "exportedBy": "${json-unit.any-string}",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "DataModelJsonExporterService",
      "version": "3.1"
    }
  }
}'''
            when:
            GET("$id/domainExports")

            then:
            verifyResponse(OK, response)
            responseBody().count == 1
            responseBody().items.first().id == deId
        }

        cleanup:
        cleanupDomainExport(deId)
        removeValidIdObject(id)

        where:
        prefix | name             | canRead
        'LO'   | null             | false
        'NA'   | 'Authenticated'  | false
        'RE'   | 'Reader'         | true
        'RV'   | 'Reviewer'       | true
        'AU'   | 'Author'         | true
        'ED'   | 'Editor'         | true
        'CA'   | 'ContainerAdmin' | true
        'AD'   | 'Admin'          | true
    }

    void 'DM-#prefix-07 : test export multiple DataModels (as reader)'() {
        given:
        String id = getValidId()

        when:
        login(name)
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1',
             [dataModelIds: [id, getSimpleDataModelId()]], canRead ? STRING_ARG : MAP_ARG)

        then:
        if (!canRead) verifyNotFound response, id
        else {
            verifyJsonResponse OK, '''{
  "dataModels": [
    {
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
    {
      "id": "${json-unit.matches:id}",
      "label": "Simple Test DataModel",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "type": "Data Standard",
      "documentationVersion": "1.0.0",
      "finalised": false,
      "authority": {
        "id": "${json-unit.matches:id}",
        "url": "http://localhost",
        "label": "Mauro Data Mapper"
      },
      "childDataClasses": [
        {
          "id": "${json-unit.matches:id}",
          "label": "simple",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "metadata": [
            {
              "id": "${json-unit.matches:id}",
              "lastUpdated": "${json-unit.matches:offsetDateTime}",
              "namespace": "test.com/simple",
              "key": "mdk1",
              "value": "mdv1"
            }
          ]
        }
      ],
      "classifiers": [
        {
          "id": "${json-unit.matches:id}",
          "label": "test classifier simple",
          "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }
      ],
      "metadata": [
        {
          "id": "${json-unit.matches:id}",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "namespace": "test.com",
          "key": "mdk2",
          "value": "mdv2"
        },
        {
          "id": "${json-unit.matches:id}",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "namespace": "test.com/simple",
          "key": "mdk1",
          "value": "mdv1"
        },
        {
          "id": "${json-unit.matches:id}",
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "namespace": "test.com/simple",
          "key": "mdk2",
          "value": "mdv2"
        }
      ]
    }
  ],
  "exportMetadata": {
    "exportedBy": "${json-unit.any-string}",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "DataModelJsonExporterService",
      "version": "3.1"
    }
  }
}'''
        }

        cleanup:
        removeValidIdObject(id)

        where:
        prefix | name             | canRead
        'LO'   | null             | false
        'NA'   | 'Authenticated'  | false
        'RE'   | 'Reader'         | true
        'RV'   | 'Reviewer'       | true
        'AU'   | 'Author'         | true
        'ED'   | 'Editor'         | true
        'CA'   | 'ContainerAdmin' | true
        'AD'   | 'Admin'          | true
    }

    void 'DM-#prefix-08 : test import basic DataModel (as #name)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()
        String id2

        expect:
        exportedJsonString

        when:
        login(name)
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            modelName                      : 'Functional Test Import',
            folderId                       : testFolderId,
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString.bytes.toList()
            ]
        ])

        then:
        if (name && !canRead) verifyNotFound(response, testFolderId)
        else if (canImport) {
            verifyResponse CREATED, response
            id2 = response.body().items.first().id
            response.body().count == 1
            response.body().items.first().label == 'Functional Test Import'
            response.body().items.first().id != id
        } else {
            verifyForbidden response
        }

        cleanup:
        removeValidIdObjectUsingTransaction(id)
        if (canImport) {
            removeValidIdObjectUsingTransaction(id2)
            removeValidIdObject(id2, NOT_FOUND)
        }
        removeValidIdObject(id, NOT_FOUND)

        where:
        prefix | name             | canRead | canImport
        'LO'   | null             | false   | false
        'NA'   | 'Authenticated'  | false   | false
        'RE'   | 'Reader'         | true    | false
        'RV'   | 'Reviewer'       | true    | false
        'AU'   | 'Author'         | true    | false
        'ED'   | 'Editor'         | true    | true
        'CA'   | 'ContainerAdmin' | true    | true
        'AD'   | 'Admin'          | true    | true
    }

    void 'DM-ED-09 : test import basic DataModel as new documentation version (as editor)'() {
        given:
        String id = getValidId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
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
    }

    void 'DM-ED-10 : test import basic DataModel as new branch model version (as editor)'() {
        given:
        String id = getValidFinalisedId()
        loginReader()
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        expect:
        exportedJsonString

        when:
        loginEditor()
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importAsNewBranchModelVersion  : true,
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
        response.body().items.first().branchName == 'main'
        !response.body().items.first().modelVersion
        response.body().items.first().id != id

        when:
        String newId = response.body().items.first().id
        GET("$newId/versionLinks")

        then:
        verifyResponse OK, response
        response.body().count == 1
        response.body().items.first().domainType == 'VersionLink'
        response.body().items.first().linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
        response.body().items.first().sourceModel.id == newId
        response.body().items.first().targetModel.id == id
        response.body().items.first().sourceModel.domainType == response.body().items.first().targetModel.domainType

        cleanup:
        removeValidIdObjectUsingTransaction(newId)
        removeValidIdObjectUsingTransaction(id)
        removeValidIdObject(newId, NOT_FOUND)
        removeValidIdObject(id, NOT_FOUND)
    }

    void 'DM-#prefix-11 : test import multiple DataModels (as #name)'() {
        given:
        String id
        String id2
        loginReader()
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/DataModelJsonExporterService/3.1',
             [dataModelIds: [getSimpleDataModelId(), getComplexDataModelId()]], STRING_ARG)

        expect:
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()
        logout()

        and:
        exportedJsonString

        when:
        login(name)
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/DataModelJsonImporterService/3.1', [
            finalised                      : false,
            folderId                       : testFolderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileType    : MimeType.JSON_API.name,
                fileContents: exportedJsonString
                    .replace(/${BootstrapModels.SIMPLE_DATAMODEL_NAME}/, 'Simple Test DataModel 2')
                    .replace(/${BootstrapModels.COMPLEX_DATAMODEL_NAME}/, 'Complex Test DataModel 2').bytes.toList()
            ]
        ])

        then:
        if (name && !canRead) verifyNotFound(response, testFolderId)
        else if (canImport) {
            verifyResponse CREATED, response
            response.body().count == 2

            Object object = response.body().items[0]
            Object object2 = response.body().items[1]
            id = object.id
            id2 = object2.id

            object.label == 'Simple Test DataModel 2'
            object2.label == 'Complex Test DataModel 2'
            object.id != object2.id
        } else verifyForbidden response

        cleanup:
        if (canImport) {
            removeValidIdObjectUsingTransaction(id)
            removeValidIdObjectUsingTransaction(id2)
            removeValidIdObject(id2, NOT_FOUND)
            removeValidIdObject(id, NOT_FOUND)
        }

        where:
        prefix | name             | canRead | canImport
        'LO'   | null             | false   | false
        'NA'   | 'Authenticated'  | false   | false
        'RE'   | 'Reader'         | true    | false
        'RV'   | 'Reviewer'       | true    | false
        'AU'   | 'Author'         | true    | false
        'ED'   | 'Editor'         | true    | true
        'CA'   | 'ContainerAdmin' | true    | true
        'AD'   | 'Admin'          | true    | true
    }

    void 'S01 : test searching for label "emptyclass" in complex model'() {
        given:
        def term = 'emptyclass'

        when: 'not logged in'
        GET("${getComplexDataModelId()}/search?searchTerm=${term}")

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("${getComplexDataModelId()}/search?searchTerm=${term}")

        then:
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.size() == 1
        responseBody().items.first().label == 'emptyclass'
        responseBody().items.first().domainType == 'DataClass'
        responseBody().items.first().breadcrumbs.size() == 1
    }

    void 'S02 : test searching for label "emptyclass" in simple model'() {
        given:
        def term = 'emptyclass'

        when: 'not logged in'
        GET("${getSimpleDataModelId()}/search?searchTerm=${term}")

        then:
        verifyNotFound(response, getSimpleDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("${getSimpleDataModelId()}/search?searchTerm=${term}")

        then:
        verifyResponse OK, response
        responseBody().count == 0
        responseBody().items.isEmpty()
    }

    void 'S03 : test searching for label "c*" in complex model'() {
        given:
        def term = 'c*'

        when: 'not logged in'
        GET("${getComplexDataModelId()}/search?searchTerm=${term}")

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        GET("${getComplexDataModelId()}/search?searchTerm=${term}&sort=domainType")

        then:
        verifyResponse OK, response
        responseBody().count == 4
        responseBody().items.size() == 4
        responseBody().items.any {
            it.label == 'child' &&
            it.domainType == 'DataClass'
        }
        responseBody().items.any {
            it.label == 'content' &&
            it.domainType == 'DataClass'
        }
        responseBody().items.any {
            it.label == 'child' &&
            it.domainType == 'DataElement'
        }
        responseBody().items.any {
            it.label == 'child' &&
            it.domainType == 'ReferenceType'
        }
    }

    void 'S04 : test searching for label "c*" in complex model using post'() {
        given:
        def term = 'c*'

        when: 'not logged in'
        POST("${getComplexDataModelId()}/search", [searchTerm: term, sort: 'label'])

        then:
        verifyNotFound(response, getComplexDataModelId())

        when: 'logged in as reader user'
        loginReader()
        POST("${getComplexDataModelId()}/search", [
            searchTerm : term,
            sort       : 'label',
            domainTypes: ['DataClass'],
        ])

        then:
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.size() == 2
        responseBody().items[0].label == 'child'
        responseBody().items[0].domainType == 'DataClass'
        responseBody().items[1].label == 'content'
        responseBody().items[1].domainType == 'DataClass'
    }

    void 'DM-#prefix-12 : test importing DataType [not allowed] (as #name)'() {
        given:
        String mergeFolderId = getTestFolderId()
        Assert.notNull mergeFolderId
        Map data = configureImportDataType()
        login(name)

        when: 'importing non-existent'
        PUT("$data.id/dataTypes/$data.finalisedId/${data.nonImportableId}", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'importing non importable id'
        PUT("$data.id/dataTypes/$data.otherId/$data.nonImportableId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'importing internal id'
        PUT("$data.id/dataTypes/$data.id/$data.internalId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'importing with same label id'
        PUT("$data.id/dataTypes/$data.finalisedId/$data.sameLabelId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'importing importable id'
        PUT("$data.id/dataTypes/$data.finalisedId/$data.importableId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'getting list of datatypes'
        GET("$data.id/dataTypes")

        then:
        if (canRead) {
            verifyResponse OK, response
            responseBody().items.size() == 1
            responseBody().items.any {it.id == data.internalId && !it.imported}
        } else verifyNotFound(response, data.id)

        cleanup:
        cleanupImportData(data)

        where:
        prefix | name            | canRead
        'LO'   | null            | false
        'NA'   | 'Authenticated' | false
        'RE'   | 'Reader'        | true
        'RV'   | 'Reviewer'      | true
        'AU'   | 'Author'        | true
    }

    void 'DM-#prefix-12 : test importing DataType [allowed] (as #name)'() {
        given:
        Map data = configureImportDataType()
        login(name)

        when: 'importing non-existent'
        PUT("$data.id/dataTypes/$data.finalisedId/${data.nonImportableId}", [:])

        then:
        verifyNotFound(response, data.nonImportableId)

        when: 'importing non importable id'
        PUT("$data.id/dataTypes/$data.otherId/$data.nonImportableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "PrimitiveType [${data.nonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing internal id'
        PUT("$data.id/dataTypes/$data.id/$data.internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "PrimitiveType [${data.internalId}] to be imported belongs to the DataModel already"

        when: 'importing with same label id'
        PUT("$data.id/dataTypes/$data.finalisedId/$data.sameLabelId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "Property [importedDataTypes] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel." +
        "DataModel] has non-unique values [Functional Test DataType] on property [label]"

        when: 'importing importable id'
        PUT("$data.id/dataTypes/$data.finalisedId/$data.importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of datatypes'
        GET("$data.id/dataTypes")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 2
        responseBody().items.any {it.id == data.internalId && !it.imported}
        responseBody().items.any {it.id == data.importableId && it.imported}

        cleanup:
        cleanupImportData(data)

        where:
        prefix | name
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'DM-#prefix-13 : test importing DataType and removing [not allowed] (as #name)'() {
        given:
        Map data = configureImportDataType()
        loginEditor()
        PUT("$data.id/dataTypes/$data.finalisedId/$data.importableId", [:])
        verifyResponse OK, response
        logout()
        login(name)

        when: 'removing non-existent'
        DELETE("$data.id/dataTypes/$data.finalisedId/${UUID.randomUUID()}", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'removing internal id'
        DELETE("$data.id/dataTypes/$data.id/$data.internalId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'removing importable id'
        DELETE("$data.id/dataTypes/$data.finalisedId/$data.importableId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        cleanup:
        cleanupImportData(data)

        where:
        prefix | name            | canRead
        'LO'   | null            | false
        'NA'   | 'Authenticated' | false
        'RE'   | 'Reader'        | true
        'RV'   | 'Reviewer'      | true
        'AU'   | 'Author'        | true
    }

    void 'DM-#prefix-13 : test importing DataType and removing [allowed] (as #name)'() {
        given:
        Map data = configureImportDataType()
        loginEditor()
        PUT("$data.id/dataTypes/$data.finalisedId/$data.importableId", [:])
        verifyResponse OK, response
        logout()
        login(name)
        String randomId = UUID.randomUUID().toString()

        when: 'removing non-existent'
        DELETE("$data.id/dataTypes/$data.finalisedId/$randomId", [:])

        then:
        verifyNotFound(response, data.randomId)

        when: 'removing internal id'
        DELETE("$data.id/dataTypes/$data.id/$data.internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "PrimitiveType [${data.internalId}] belongs to the DataModel and cannot be removed as an import"

        when: 'removing importable id'
        DELETE("$data.id/dataTypes/$data.finalisedId/$data.importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of datatypes'
        GET("$data.id/dataTypes")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 1
        responseBody().items.any {it.id == data.internalId}

        cleanup:
        cleanupImportData(data)

        where:
        prefix | name
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'DM-#prefix-14 : test importing DataClasses [not allowed] (as #name)'() {
        given:
        Map data = configureImportDataClass()
        login(name)

        when: 'importing non-existent'
        PUT("$data.id/dataClasses/$data.finalisedId/${data.nonImportableId}", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'importing non importable id'
        PUT("$data.id/dataClasses/$data.otherId/$data.nonImportableId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'importing internal id'
        PUT("$data.id/dataClasses/$data.id/$data.internalId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'importing with same label id'
        PUT("$data.id/dataClasses/$data.finalisedId/$data.sameLabelId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'importing importable id'
        PUT("$data.id/dataClasses/$data.finalisedId/$data.importableId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'getting list of dataclasses'
        GET("$data.id/dataClasses")

        then:
        if (canRead) {
            verifyResponse OK, response
            responseBody().items.size() == 1
            responseBody().items.any {it.id == data.internalId && !it.imported}
        } else verifyNotFound(response, data.id)

        cleanup:
        cleanupImportData(data)

        where:
        prefix | name            | canRead
        'LO'   | null            | false
        'NA'   | 'Authenticated' | false
        'RE'   | 'Reader'        | true
        'RV'   | 'Reviewer'      | true
        'AU'   | 'Author'        | true
    }

    void 'DM-#prefix-14 : test importing DataClasses [allowed] (as #name)'() {
        given:
        Map data = configureImportDataClass()
        login(name)

        when: 'importing non-existent'
        PUT("$data.id/dataClasses/$data.finalisedId/${data.nonImportableId}", [:])

        then:
        verifyNotFound(response, data.nonImportableId)

        when: 'importing non importable id'
        PUT("$data.id/dataClasses/$data.otherId/$data.nonImportableId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message ==
        "DataClass [${data.nonImportableId}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder"

        when: 'importing internal id'
        PUT("$data.id/dataClasses/$data.id/$data.internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataClass [${data.internalId}] to be imported belongs to the DataModel already"

        when: 'importing with same label id'
        PUT("$data.id/dataClasses/$data.finalisedId/$data.sameLabelId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "Property [importedDataClasses] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel." +
        "DataModel] has non-unique values [Functional Test DataClass] on property [label]"

        when: 'importing importable id'
        PUT("$data.id/dataClasses/$data.finalisedId/$data.importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataclasses'
        GET("$data.id/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 2
        responseBody().items.any {it.id == data.internalId && !it.imported}
        responseBody().items.any {it.id == data.importableId && it.imported}

        cleanup:
        cleanupImportData(data)

        where:
        prefix | name
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void 'DM-#prefix-15 : test importing DataClass and removing [not allowed] (as #name)'() {
        given:
        Map data = configureImportDataClass()
        loginEditor()
        PUT("$data.id/dataClasses/$data.finalisedId/$data.importableId", [:])
        verifyResponse OK, response
        logout()
        login(name)

        when: 'removing non-existent'
        DELETE("$data.id/dataClasses/$data.finalisedId/${UUID.randomUUID()}", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'removing internal id'
        DELETE("$data.id/dataClasses/$data.id/$data.internalId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'removing importable id'
        DELETE("$data.id/dataClasses/$data.finalisedId/$data.importableId", [:])

        then:
        if (canRead) verifyForbidden(response) else verifyNotFound(response, data.id)

        when: 'getting list of dataClasses'
        GET("$data.id/dataClasses")

        then:
        if (canRead) {
            verifyResponse OK, response
            responseBody().items.size() == 2
            responseBody().items.any {it.id == data.internalId}
            responseBody().items.any {it.id == data.importableId}
        } else verifyNotFound(response, data.id)

        cleanup:
        cleanupImportData(data)

        where:
        prefix | name            | canRead
        'LO'   | null            | false
        'NA'   | 'Authenticated' | false
        'RE'   | 'Reader'        | true
        'RV'   | 'Reviewer'      | true
        'AU'   | 'Author'        | true
    }

    void 'DM-#prefix-15 : test importing DataClass and removing [allowed] (as #name)'() {
        given:
        Map data = configureImportDataClass()
        loginEditor()
        PUT("$data.id/dataClasses/$data.finalisedId/$data.importableId", [:])
        verifyResponse OK, response
        logout()
        login(name)

        when: 'removing non-existent'
        DELETE("$data.id/dataClasses/$data.finalisedId/${UUID.randomUUID()}", [:])

        then:
        verifyResponse NOT_FOUND, response

        when: 'removing internal id'
        DELETE("$data.id/dataClasses/$data.id/$data.internalId", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == "DataClass [${data.internalId}] belongs to the DataModel and cannot be removed as an import"

        when: 'removing importable id'
        DELETE("$data.id/dataClasses/$data.finalisedId/$data.importableId", [:])

        then:
        verifyResponse OK, response

        when: 'getting list of dataClasses'
        GET("$data.id/dataClasses")

        then:
        verifyResponse OK, response
        responseBody().items.size() == 1
        responseBody().items.any {it.id == data.internalId}

        cleanup:
        cleanupImportData(data)

        where:
        prefix | name
        'ED'   | 'Editor'
        'CA'   | 'ContainerAdmin'
        'AD'   | 'Admin'
    }

    void cleanupImportData(Map data) {
        removeValidIdObjectUsingTransaction(data.id)
        removeValidIdObjectUsingTransaction(data.otherId)
        removeValidIdObjectUsingTransaction(data.finalisedId)
        cleanUpRoles(data.id, data.otherId, data.finalisedId)
    }

    Map configureImportDataType() {
        Map data = configureImportDataModels()

        // Get internal DT
        POST("$data.id/dataTypes", [
            label     : 'Functional Test DataType',
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        data.internalId = responseBody().id

        POST("$data.finalisedId/dataTypes", [
            label     : 'Functional Test DataType 2',
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        data.importableId = responseBody().id

        POST("$data.finalisedId/dataTypes", [
            label     : 'Functional Test DataType',
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        data.sameLabelId = responseBody().id

        POST("$data.otherId/dataTypes", [
            label     : 'Functional Test DataType 3',
            domainType: 'PrimitiveType',])
        verifyResponse CREATED, response
        data.nonImportableId = responseBody().id

        // Finalise DM
        PUT("$data.finalisedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        logout()
        data
    }

    Map configureImportDataClass() {

        Map data = configureImportDataModels()
        // Get internal DT
        POST("$data.id/dataClasses", [
            label: 'Functional Test DataClass',])
        verifyResponse CREATED, response
        data.internalId = responseBody().id

        POST("$data.finalisedId/dataClasses", [
            label: 'Functional Test DataClass 2',])
        verifyResponse CREATED, response
        data.importableId = responseBody().id
        POST("$data.finalisedId/dataClasses", [
            label: 'Functional Test DataClass',])
        verifyResponse CREATED, response
        data.sameLabelId = responseBody().id

        POST("$data.otherId/dataClasses", [
            label: 'Functional Test DataClass 3',])
        verifyResponse CREATED, response
        data.nonImportableId = responseBody().id

        // Finalise DM
        PUT("$data.finalisedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        logout()
        data
    }

    Map configureImportDataModels() {
        Map data = [:]
        // Get DataModel
        data.id = getValidId()
        loginCreator()

        // Get second DataModel
        POST(getSavePath(), [
            label: 'Functional Test Model 1'
        ], MAP_ARG, true)
        verifyResponse CREATED, response
        data.otherId = response.body().id
        addAccessShares(data.otherId)

        // Get finalised DataModel
        POST(getSavePath(), [
            label: 'Functional Test Model 2'
        ], MAP_ARG, true)
        verifyResponse CREATED, response
        data.finalisedId = response.body().id
        addAccessShares(data.finalisedId)
        data
    }

    String getExpectedDiffJson() {
        '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "label": "Complex Test DataModel",
  "count": 18,
  "diffs": [
    {
      "label": {
        "left": "Complex Test DataModel",
        "right": "Simple Test DataModel"
      }
    },
    {
      "metadata": {
        "deleted": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "namespace": "test.com",
              "key": "mdk1",
              "value": "mdv1"
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "namespace": "test.com/test",
              "key": "mdk1",
              "value": "mdv2"
            }
          }
        ],
        "created": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "namespace": "test.com/simple",
              "key": "mdk2",
              "value": "mdv2"
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "namespace": "test.com/simple",
              "key": "mdk1",
              "value": "mdv1"
            }
          }
        ]
      }
    },
    {
      "annotations": {
        "deleted": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "test annotation 1"
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "test annotation 2"
            }
          }
        ]
      }
    },
    {
      "rules": {
        "deleted": [
          {
            "value": {
              "id": "${json-unit.matches:id}"
            }
          }
        ]
      }
    },
    {
      "author": {
        "left": "admin person",
        "right": null
      }
    },
    {
      "organisation": {
        "left": "brc",
        "right": null
      }
    },
    {
      "dataTypes": {
        "deleted": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "integer",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "string",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "yesnounknown",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "child",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
              ]
            }
          }
        ]
      }
    },
    {
      "dataClasses": {
        "deleted": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "emptyclass",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "parent",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "content",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
              ]
            }
          }
        ],
        "created": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "simple",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Simple Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
              ]
            }
          }
        ]
      }
    }
  ]
}'''
    }

    void 'MI07 : test merge into of two DMs with a Model Data Type'() {
        given:
        String mergeFolderId = getTestFolderId()
        loginEditor()

        when: 'Get the Complex Test Terminology ID for checking later'
        GET("terminologies/path/te:Complex%20Test%20Terminology", MAP_ARG, true)

        then: 'The response is OK'
        verifyResponse OK, response
        String complexTerminologyId = responseBody().id

        when: 'Get the Simple Test Terminology ID to use in the Model Data Type'
        GET("terminologies/path/te:Simple%20Test%20Terminology", MAP_ARG, true)

        then: 'The response is OK'
        verifyResponse OK, response
        String simpleTerminologyId = responseBody().id

        when:
        TestMergeData mergeData = builder.buildComplexModelsForMerging(mergeFolderId, simpleTerminologyId)

        then:
        mergeData.sourceMap.externallyPointingModelDataTypeId

        when: 'get the mergeDiff between source and target'
        GET("$mergeData.source/mergeDiff/$mergeData.target?isLegacy=false")

        then: 'there are no modification diffs for dataTypes'
        verifyResponse OK, response
        !responseBody().diffs.find{it.fieldName == 'modelResourcePath'}

        when: 'get the data types on the source data model'
        GET("dataModels/$mergeData.source/dataTypes", MAP_ARG, true)

        then:
        verifyResponse OK, response
        responseBody().count == 6
        responseBody().items.label as Set == ['addLeftOnly', 'Functional Test Model Data Type Pointing Externally', 'existingDataType1',
                                              'existingDataType2', 'Functional Test DataType Importable', 'Functional Test DataType Importable Add'] as Set
        def mdt = responseBody().items.find {it.label == 'Functional Test Model Data Type Pointing Externally' }

        and:
        mdt.modelResourceDomainType == 'Terminology'
        mdt.modelResourceId == simpleTerminologyId
        mdt.id == mergeData.sourceMap.externallyPointingModelDataTypeId

        when: 'Update the MDT to point at the Complex Test Terminology'
        PUT("dataModels/$mergeData.source/dataTypes/$mdt.id", [modelResourceDomainType: 'Terminology', modelResourceId: complexTerminologyId], MAP_ARG, true)

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'get the mergeDiff between source and target'
        GET("$mergeData.source/mergeDiff/$mergeData.target?isLegacy=false")

        then: 'the diffs include a modification to the Model Data Type'
        verifyResponse OK, response
        def postDiffs = responseBody().diffs
        def modelResourcePath = responseBody().diffs.find {it.fieldName == 'modelResourcePath'}
        modelResourcePath.sourceValue.contains('Complex Test Terminology')
        modelResourcePath.targetValue.contains('Simple Test Terminology')
        modelResourcePath.type == 'modification'

        when: 'Merge the diffs from source to target'
        loginEditor()
        PUT("$mergeData.source/mergeInto/$mergeData.target?isLegacy=false", [
            patch:
                [
                    targetId: mergeData.target,
                    sourceId: mergeData.source,
                    label   : 'Functional Test Model',
                    count   : postDiffs.size(),
                    patches : postDiffs
                ]
        ])

        then: 'the response is OK'
        verifyResponse OK, response

        when: 'get the data types on the target data model'
        GET("dataModels/$mergeData.target/dataTypes", MAP_ARG, true)

        then: 'there is a Model Data Type'
        verifyResponse OK, response
        def mergedMdt = responseBody().items.find {it.label == 'Functional Test Model Data Type Pointing Externally' }

        and: 'the Model Data Type now points to the Complex Test Terminology'
        mergedMdt.modelResourceDomainType == 'Terminology'
        mergedMdt.modelResourceId == complexTerminologyId

        cleanup:
        builder.cleanupTestMergeData(mergeData)
    }

    void 'SUBSET01 : test subsetting of Complex Data Model'() {
        def source = [:]
        def target = [:]
        def target2 = [:]

        given:
        source.dataModelId = getComplexDataModelId()

        and:
        loginAdmin()
        POST(getSavePath(), ['label': 'Target of Subset'], MAP_ARG, true)
        verifyResponse CREATED, response
        target.dataModelId = response.body().id
        POST(getSavePath(), ['label': 'Second Target of Subset'], MAP_ARG, true)
        verifyResponse CREATED, response
        target2.dataModelId = response.body().id

        and:
        loginAdmin()
        GET("/${source.dataModelId}/dataClasses")
        verifyResponse OK, response
        source.emptyClass = response.body().items.find { it.label == 'emptyclass' }
        source.contentClass = response.body().items.find { it.label == 'content' }
        source.parentClass = response.body().items.find { it.label == 'parent' }

        and:
        GET("/${source.dataModelId}/dataClasses/${source.parentClass.id}/dataClasses")
        verifyResponse OK, response
        source.parentClass.childClass = response.body().items.find { it.label == 'child' }

        and:
        GET("/${source.dataModelId}/dataClasses/${source.contentClass.id}/dataElements")
        verifyResponse OK, response
        source.contentClass.ele1 = response.body().items.find { it.label == 'ele1' }
        source.contentClass.element2 = response.body().items.find { it.label == 'element2' }

        and:
        GET("/${source.dataModelId}/dataClasses/${source.parentClass.id}/dataClasses")
        verifyResponse OK, response
        source.parentClass.childClass = response.body().items.find {it.label == 'child'}

        and:
        GET("/${source.dataModelId}/dataTypes")
        verifyResponse OK, response
        def dataTypeId = responseBody().items.find {it.label == 'string'}.id

        and: 'there is a Data Element called grandchild on the child Data Class'
        POST("/${source.dataModelId}/dataClasses/${source.parentClass.childClass.id}/dataElements", [
            'label'   : 'grandchild',
            'dataType': dataTypeId
        ])
        verifyResponse CREATED, response
        source.parentClass.childClass.grandchild = response.body()
        logout()

        when: 'Get the intersection between complex and target (not logged in)'
        GET("/${source.dataModelId}/intersects/${target.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse NOT_FOUND, response

        when: 'Get the intersection between complex and target (logged in as reader)'
        loginReader()
        GET("/${source.dataModelId}/intersects/${target.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse OK, response
        response.body().intersects.size() == 0

        /**
         * Subset DataElement 'ele1' which belongs to the 'content' Data Class. This should:
         * 1. Create the content Data Class and ele1 Data Element on targetDataModel
         * 2. Appear in the /intersects response
         */
        when: 'subset ele1 (not logged in)'
        logout()
        PUT("/${source.dataModelId}/subset/${target.dataModelId}", ['additions': [source.contentClass.ele1.id], 'deletions': []])

        then: 'The response is NOT_FOUND'
        verifyResponse NOT_FOUND, response

        when: 'subset ele1 (logged in as reader)'
        loginReader()
        PUT("/${source.dataModelId}/subset/${target.dataModelId}", ['additions': [source.contentClass.ele1.id], 'deletions': []])

        then: 'The response is FORBIDDEN'
        verifyForbidden response

        when: 'subset ele1 (logged in as editor)'
        loginEditor()
        PUT("/${source.dataModelId}/subset/${target.dataModelId}", ['additions': [source.contentClass.ele1.id], 'deletions': []])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get Data Classes on targetDataModel'
        GET("/${target.dataModelId}/dataClasses")
        target.contentClass = response.body().items[0]

        then: 'There is the content Data Class'
        verifyResponse OK, response
        response.body().items.size() == 1
        response.body().items[0].label == 'content'
        target.contentClass.id

        when: 'Get Data Elements on targetDataModel'
        GET("/${target.dataModelId}/dataElements")

        then: 'There is the ele1 Data Element in the content Data Class'
        verifyResponse OK, response
        response.body().items.size() == 1
        response.body().items[0].label == 'ele1'
        response.body().items[0].dataClass == target.contentClass.id

        when: 'Get the intersection between complex and target'
        GET("/${source.dataModelId}/intersects/${target.dataModelId}")

        then: 'The response is OK with one results'
        verifyResponse OK, response
        response.body().intersects.size() == 1
        response.body().intersects.contains(source.contentClass.ele1.id)

        when: 'Get the intersection between complex and target2'
        GET("/${source.dataModelId}/intersects/${target2.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse OK, response
        response.body().intersects.size() == 0

        when: 'Get the intersectionMany between complex and [target, target2]'
        POST("/${source.dataModelId}/intersectsMany", [
            targetDataModelIds: [target.dataModelId, target2.dataModelId],
            dataElementIds: [source.contentClass.ele1.id, source.contentClass.element2.id, source.parentClass.childClass.grandchild.id]
        ])

        then: 'The response is OK with two results'
        verifyResponse OK, response
        response.body().items.size() == 2

        and: 'The result for target contains one intersection'
        response.body().items.find{ it.targetDataModelId == target.dataModelId }.intersects.size() == 1

        and: 'The result for target2 contains no intersections'
        response.body().items.find{ it.targetDataModelId == target2.dataModelId }.intersects.size() == 0


        /**
         * Subset delete DataElement 'ele1' which belongs to the 'content' Data Class. This should:
         * 1. Remove ele1 Data Element from targetDataModel
         * 2. Remove ele1 from the /intersects response
         */
        when: 'subset ele1'
        PUT("/${source.dataModelId}/subset/${target.dataModelId}", ['additions': [], 'deletions': [source.contentClass.ele1.id]])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get Data Classes on targetDataModel'
        GET("/${target.dataModelId}/dataClasses")

        then: 'There are no Data Classes'
        verifyResponse OK, response
        response.body().items.size() == 0

        when: 'Get Data Elements on targetDataModel'
        GET("/${target.dataModelId}/dataElements")

        then: 'There are no Data Elements'
        verifyResponse OK, response
        response.body().items.size() == 0

        when: 'Get the intersection between complex and target'
        GET("/${source.dataModelId}/intersects/${target.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse OK, response
        response.body().intersects.size() == 0

        when: 'Get the intersection between complex and target2'
        GET("/${source.dataModelId}/intersects/${target2.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse OK, response
        response.body().intersects.size() == 0

        when: 'Get the intersectionMany between complex and [target, target2]'
        POST("/${source.dataModelId}/intersectsMany", [
            targetDataModelIds: [target.dataModelId, target2.dataModelId],
            dataElementIds: [source.contentClass.ele1.id, source.contentClass.element2.id, source.parentClass.childClass.grandchild.id]
        ])

        then: 'The response is OK with two results'
        verifyResponse OK, response
        response.body().items.size() == 2

        and: 'The result for target contains no intersections'
        response.body().items.find{ it.targetDataModelId == target.dataModelId }.intersects.size() == 0

        and: 'The result for target2 contains no intersections'
        response.body().items.find{ it.targetDataModelId == target2.dataModelId }.intersects.size() == 0


        /**
         * Subset delete DataElement 'ele1' (again) and also 'element2', and 'childde' which belongs to the parent | child Data Class.
         * This should leave us with:
         * 1. The content Data Class with both ele1 and element2
         * 2. A parent Data Class containing a child Data Class containing a child Data Element
         * 3. All three Data Elements listed in the /intersection response
         */
        when: 'subset ele1, element2 and child'
        PUT("/${source.dataModelId}/subset/${target.dataModelId}", [
            'additions': [
                source.contentClass.ele1.id,
                source.contentClass.element2.id,
                source.parentClass.childClass.grandchild.id
            ],
            'deletions': []
        ])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get Data Classes on targetDataModel'
        GET("/${target.dataModelId}/dataClasses")
        target.contentClass = response.body().items.find { it.label == 'content' }
        target.parentClass = response.body().items.find { it.label == 'parent' }

        then: 'There is the content Data Class and parent Data Class and child Data Class'
        verifyResponse OK, response
        response.body().items.size() == 2
        target.contentClass.id
        target.parentClass.id

        when: 'Get the Data Classes of the parent Data Class'
        GET("/${target.dataModelId}/dataClasses/${target.parentClass.id}/dataClasses")
        target.parentClass.childClass = response.body().items.find { it.label == 'child' }

        then: 'The response is OK and includes the child Data Class'
        verifyResponse OK, response
        response.body().items.size() == 1
        target.parentClass.childClass.id

        when: 'Get Data Elements on targetDataModel'
        GET("/${target.dataModelId}/dataElements")
        target.contentClass.ele1 = response.body().items.find { it.label == 'ele1' }
        target.contentClass.element2 = response.body().items.find { it.label == 'element2' }
        target.parentClass.childClass.grandchild = response.body().items.find { it.label == 'grandchild' }

        then: 'There are the ele1 and element2 Data Elements in the content Data Class'
        verifyResponse OK, response
        response.body().items.size() == 3
        target.contentClass.ele1.id
        target.contentClass.element2.id
        target.parentClass.childClass.grandchild.id

        and: 'The grandchild belongs to child'
        target.parentClass.childClass.grandchild.dataClass == target.parentClass.childClass.id

        when: 'Get the intersection between complex and target'
        GET("/${source.dataModelId}/intersects/${target.dataModelId}")

        then: 'The response is OK with three results'
        verifyResponse OK, response
        response.body().intersects.size() == 3
        response.body().intersects.contains(source.contentClass.ele1.id)
        response.body().intersects.contains(source.contentClass.element2.id)
        response.body().intersects.contains(source.parentClass.childClass.grandchild.id)

        when: 'Get the intersection between complex and target2'
        GET("/${source.dataModelId}/intersects/${target2.dataModelId}")

        then: 'The response is OK with no results'
        verifyResponse OK, response
        response.body().intersects.size() == 0

        when: 'Get the intersectionMany between complex and [target, target2]'
        POST("/${source.dataModelId}/intersectsMany", [
            targetDataModelIds: [target.dataModelId, target2.dataModelId],
            dataElementIds: [source.contentClass.ele1.id, source.contentClass.element2.id, source.parentClass.childClass.grandchild.id]
        ])

        then: 'The response is OK with two results'
        verifyResponse OK, response
        response.body().items.size() == 2

        and: 'The result for target contains three intersections'
        response.body().items.find{ it.targetDataModelId == target.dataModelId }.intersects.size() == 3

        and: 'The result for target2 contains no intersections'
        response.body().items.find{ it.targetDataModelId == target2.dataModelId }.intersects.size() == 0

        when: 'subset ele1, element2 and child onto target2'
        PUT("/${source.dataModelId}/subset/${target2.dataModelId}", [
            'additions': [
                source.contentClass.ele1.id,
                source.contentClass.element2.id,
                source.parentClass.childClass.grandchild.id
            ],
            'deletions': []
        ])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get the intersectionMany between complex and [target, target2]'
        POST("/${source.dataModelId}/intersectsMany", [
            targetDataModelIds: [target.dataModelId, target2.dataModelId],
            dataElementIds: [source.contentClass.ele1.id, source.contentClass.element2.id, source.parentClass.childClass.grandchild.id]
        ])

        then: 'The response is OK with two results'
        verifyResponse OK, response
        response.body().items.size() == 2

        and: 'The result for target contains three intersections'
        response.body().items.find{ it.targetDataModelId == target.dataModelId }.intersects.size() == 3

        and: 'The result for target2 now has intersections'
        response.body().items.find{ it.targetDataModelId == target2.dataModelId }.intersects.size() == 3


        when: 'Delete the grandchild from the subset'
        PUT("/${source.dataModelId}/subset/${target.dataModelId}", [
            'additions': [],
            'deletions': [source.parentClass.childClass.grandchild.id]
        ])

        then: 'The response is OK'
        verifyResponse OK, response

        when: 'Get Data Classes on targetDataModel'
        GET("/${target.dataModelId}/dataClasses")

        then: 'The parent Data Classes has been deleted from the targetDataModel'
        response.body().items.find { it.label == 'content' }
        !response.body().items.find { it.label == 'parent' }

        cleanup:
        loginAdmin()
        DELETE("/${target.dataModelId}?permanent=true")
        verifyResponse NO_CONTENT, response
        DELETE("/${target2.dataModelId}?permanent=true")
        verifyResponse NO_CONTENT, response
        DELETE("/${source.dataModelId}/dataClasses/${source.parentClass.childClass.id}/dataElements/${source.parentClass.childClass.grandchild.id}")
        verifyResponse NO_CONTENT, response
    }
}
