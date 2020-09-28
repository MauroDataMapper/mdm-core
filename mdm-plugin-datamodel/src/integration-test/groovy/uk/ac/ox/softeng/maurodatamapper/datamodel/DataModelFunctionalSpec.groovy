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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @see DataModelController* Controller: dataModel
 *  | POST   | /api/dataModels       | Action: save   |
 *  | GET    | /api/dataModels       | Action: index  |
 *  | DELETE | /api/dataModels/${id} | Action: delete |
 *  | PUT    | /api/dataModels/${id} | Action: update |
 *  | GET    | /api/dataModels/${id} | Action: show   |
 *
 *  | GET    | /api/dataModels/types                                   | Action: types                   |
 *  | GET    | /api/dataModels/${dataModelId}/hierarchy                | Action: hierarchy               |
 *  | PUT    | /api/dataModels/${dataModelId}/newVersion               | Action: newVersion              |
 *  | PUT    | /api/dataModels/${dataModelId}/newBranchModelVersion  | Action: newBranchModelVersion |
 *  | PUT    | /api/dataModels/${dataModelId}/newDocumentationVersion  | Action: newDocumentationVersion |
 *  | PUT    | /api/dataModels/${dataModelId}/finalise                  | Action: finalise                 |
 *  | GET    | /api/dataModels/${dataModelId}/diff/${otherDataModelId} | Action: diff                    |
 *
 *  | POST   | /api/dataModels/export/${exporterNamespace}/${exporterName}/${exporterVersion}                | Action: exportDataModels |
 *  | POST   | /api/dataModels/import/${importerNamespace}/${importerName}/${importerVersion}                | Action: importDataModels |
 *  | GET    | /api/dataModels/${dataModelId}/export/${exporterNamespace}/${exporterName}/${exporterVersion} | Action: exportDataModel  |
 *
 *  | PUT    | /api/dataModels/${dataModelId}/folder/${folderId}  | Action: changeFolder |
 *  | PUT    | /api/folders/${folderId}/dataModels/${dataModelId} | Action: changeFolder |
 *
 *  | GET    | /api/dataModels/${dataModelId}/suggestLinks/${otherDataModelId}" | Action: suggestLinksModel | TODO
 *
 *  | GET    | /api/dataModels/${dataModelId}/search | Action: search |
 *  | POST   | /api/dataModels/${dataModelId}/search | Action: search |
 */
@Integration
@Slf4j
//@Stepwise
class DataModelFunctionalSpec extends ResourceFunctionalSpec<DataModel> {

    @Shared
    UUID folderId

    @Shared
    UUID movingFolderId

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        folderId = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert folderId
        movingFolderId = new Folder(label: 'Functional Test Folder 2', createdBy: FUNCTIONAL_TEST).save(flush: true).id
        assert movingFolderId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataModelFunctionalSpec')
        cleanUpResources(Folder, Classifier)
    }

    @Override
    String getResourcePath() {
        'dataModels'
    }

    @Override
    String getSavePath() {
        "folders/${folderId}/${getResourcePath()}"
    }

    @Override
    Map getValidJson() {
        [
                label: 'Functional Test Model'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
                label: null
        ]
    }

    @Override
    String getDeleteEndpoint(String id) {
        "${super.getDeleteEndpoint(id)}?permanent=true"
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "domainType": "DataModel",
  "availableActions": ['delete', 'show', 'update'],
  "branchName": "main",
  "finalised": false,
  "label": "Functional Test Model",
  "type": "Data Standard",
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

    void 'test getting DataModel types'() {
        when:
        GET('types', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
          "Data Asset",
          "Data Standard"
        ]'''
    }

    void 'test getting DataModel exporters'() {
        when:
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "providerType": "DataModelExporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "XML DataModel Exporter",
    "fileExtension": "xml",
    "name": "XmlExporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "canExportMultipleDomains": false,
    "version": "${json-unit.matches:version}",
    "fileType": "text/xml"
  },
  {
    "providerType": "DataModelExporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "JSON DataModel Exporter",
    "fileExtension": "json",
    "name": "JsonExporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "canExportMultipleDomains": false,
    "version": "${json-unit.matches:version}",
    "fileType": "text/json"
  }
]'''
    }

    void 'test getting DataModel importers'() {
        when:
        GET('providers/importers', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
  {
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters",
    "providerType": "DataModelImporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "XML DataModel Importer",
    "name": "XmlImporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
    "allowsExtraMetadataKeys": true,
    "canImportMultipleDomains": true,
    "version": "${json-unit.matches:version}"
  },
  {
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters",
    "providerType": "DataModelImporter",
    "knownMetadataKeys": [
      
    ],
    "displayName": "JSON DataModel Importer",
    "name": "JsonImporterService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
    "allowsExtraMetadataKeys": true,
    "canImportMultipleDomains": false,
    "version": "${json-unit.matches:version}"
  }
]'''
    }

    void 'test getting DataModel default datatype providers'() {
        when:
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
  }
]'''
    }

    void 'test finalising DataModel'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse OK, response

        and:
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().finalised
        response.body().dateFinalised

        cleanup:
        cleanUpData(id)
    }

    void 'VF01 : test creating a new fork model of a DataModel'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'adding one new model'
        PUT("$id/newForkModel", [label: 'Functional Test DataModel reader'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
                .replaceFirst(/"label": "Functional Test Model",/, '"label": "Functional Test DataModel reader",')


        when:
        GET("$id/semanticLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel reader"
      },
      "targetCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when:
        GET("$id/versionLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "VersionLink",
      "linkType": "New Fork Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel reader"
      },
      "targetModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when: 'adding another'
        PUT("$id/newForkModel", [label: 'Functional Test DataModel editor'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
                .replaceFirst(/"label": "Functional Test Model",/, '"label": "Functional Test DataModel editor",')

        when:
        GET("$id/semanticLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel reader"
      },
      "targetCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    },
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel editor"
      },
      "targetCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when:
        GET("$id/versionLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 2,
  "items": [
    {
      "domainType": "VersionLink",
      "linkType": "New Fork Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel reader"
      },
      "targetModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    },
     {
      "domainType": "VersionLink",
      "linkType": "New Fork Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test DataModel editor"
      },
      "targetModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        cleanup:
        cleanUpData()
    }

    void 'VD01 : test creating a new documentation version of a DataModel'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when:
        PUT("$id/newDocumentationVersion", [:], STRING_ARG)

        then:
        verifyJsonResponse CREATED, expectedShowJson
                .replaceFirst(/"documentationVersion": "1\.0\.0",/, '"documentationVersion": "2.0.0",')

        when:
        GET("$id/semanticLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when:
        GET("$id/versionLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "VersionLink",
      "linkType": "New Documentation Version Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when: 'adding another'
        PUT("$id/newDocumentationVersion", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        response.body().total == 1
        response.body().errors.size() == 1
        response.body().errors[0].message.contains('cannot have a new version as it has been superseded by [Functional Test Model')

        cleanup:
        cleanUpData()
    }

    void 'VB01 : test creating a new main branch model version of a DataModel'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when:
        PUT("$id/newBranchModelVersion", [:], STRING_ARG)

        then:
        verifyJsonResponse CREATED, expectedShowJson

        when:
        GET("$id/semanticLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetCatalogueItem": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when:
        GET("$id/versionLinks", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 1,
  "items": [
    {
      "domainType": "VersionLink",
      "linkType": "New Model Version Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "domainType": "DataModel",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''
        cleanup:
        cleanUpData()
    }

    void 'VB02 : test creating a main branch model version finalising and then creating another main branch of a DataModel'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'create second model'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response

        when: 'finalising second model'
        String secondId = responseBody().id
        PUT("$secondId/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse OK, response

        when: 'create new branch from second model'
        PUT("$secondId/newBranchModelVersion", [:])

        then:
        String thirdId = responseBody().id
        verifyResponse CREATED, response

        when: 'get first model SLs'
        GET("$id/semanticLinks")

        then: 'first model is the target of refines for both second and third model'
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
                    it.targetCatalogueItem.id == id &&
                    it.sourceCatalogueItem.id == secondId
        }
        // This is unconfirmed as its copied
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
                    it.targetCatalogueItem.id == id &&
                    it.sourceCatalogueItem.id == thirdId &&
                    it.unconfirmed
        }

        when: 'getting the first model VLs'
        GET("$id/versionLinks")

        then: 'first model is the target of new model version of for second model only'
        verifyResponse OK, response
        responseBody().count == 1
        responseBody().items.any {
            it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label &&
                    it.targetModel.id == id &&
                    it.sourceModel.id == secondId
        }

        when: 'get second model SLs'
        GET("$secondId/semanticLinks")

        then: 'second model is the target of refines for third model and source for first model'
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
                    it.targetCatalogueItem.id == id &&
                    it.sourceCatalogueItem.id == secondId
        }
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
                    it.targetCatalogueItem.id == secondId &&
                    it.sourceCatalogueItem.id == thirdId
        }

        when: 'getting the second model VLs'
        GET("$secondId/versionLinks")

        then: 'second model is the target of new model version of for third model and source for first model'
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.any {
            it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
            it.targetModel.id == secondId &&
                    it.sourceModel.id == thirdId
        }
        responseBody().items.any {
            it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
            it.targetModel.id == id &&
                    it.sourceModel.id == secondId
        }

        cleanup:
        cleanUpData()
    }

    void 'VB03 : test creating a main branch model version when one already exists'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'create default main'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response

        when: 'another main branch created'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == 'Property [branchName] of class [class uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel] ' +
                'with value [main] already exists for label [Functional Test Model]'

        cleanup:
        cleanUpData()
    }

    void 'VB04 : test creating a non-main branch model version without main existing'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when: 'create default main'
        PUT("$id/newBranchModelVersion", [branchName: 'functionalTest'])

        then:
        verifyResponse CREATED, response

        when:
        GET("$id/versionLinks")

        then:
        verifyResponse OK, response
        responseBody().count == 2
        responseBody().items.every {
            it.linkType == 'New Model Version Of'
            it.targetModel.id == id
        }

        cleanup:
        cleanUpData()
    }

    void 'VB05 : test finding common ancestor of two datamodels'() {
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'right'])
        verifyResponse CREATED, response
        String rightId = responseBody().id

        when:
        GET('')

        then:
        verifyResponse OK, response
        String mainId = responseBody().items.find {
            it.label == 'Functional Test Model' &&
                    !(it.id in [id, leftId, rightId])
        }?.id
        mainId

        when: 'check CA between L and R'
        GET("$leftId/commonAncestor/$rightId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        when: 'check CA between R and L'
        GET("$rightId/commonAncestor/$leftId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        when: 'check CA between L and M'
        GET("$leftId/commonAncestor/$mainId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        when: 'check CA between M and L'
        GET("$mainId/commonAncestor/$leftId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        when: 'check CA between M and R'
        GET("$mainId/commonAncestor/$rightId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        when: 'check CA between R and M'
        GET("$rightId/commonAncestor/$mainId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        cleanup:
        cleanUpData(leftId)
        cleanUpData(rightId)
        cleanUpData(mainId)
        cleanUpData(id)
    }

    void 'VB06 : test finding latest finalised model of a datamodel'() {
        /*
        id (finalised) -- expectedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: 'main'])
        verifyResponse CREATED, response
        String expectedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$expectedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$expectedId/newBranchModelVersion", [branchName: 'main'])
        verifyResponse CREATED, response
        String latestDraftId = responseBody().id

        when:
        GET("$newBranchId/latestFinalisedModel")

        then:
        verifyResponse OK, response
        responseBody().id == expectedId
        responseBody().label == 'Functional Test Model'
        responseBody().modelVersion == '2.0.0'

        when:
        GET("$latestDraftId/latestFinalisedModel")

        then:
        verifyResponse OK, response
        responseBody().id == expectedId
        responseBody().label == 'Functional Test Model'
        responseBody().modelVersion == '2.0.0'

        cleanup:
        cleanUpData(newBranchId)
        cleanUpData(expectedId)
        cleanUpData(latestDraftId)
        cleanUpData(id)
    }

    void 'VB07 : test finding latest model version of a datamodel'() {
        /*
        id (finalised) -- expectedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: 'main'])
        verifyResponse CREATED, response
        String expectedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$expectedId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$expectedId/newBranchModelVersion", [branchName: 'main'])
        verifyResponse CREATED, response
        String latestDraftId = responseBody().id

        when:
        GET("$newBranchId/latestModelVersion")

        then:
        verifyResponse OK, response
        responseBody().modelVersion == '2.0.0'

        when:
        GET("$latestDraftId/latestModelVersion")

        then:
        verifyResponse OK, response
        responseBody().modelVersion == '2.0.0'

        cleanup:
        cleanUpData(newBranchId)
        cleanUpData(expectedId)
        cleanUpData(latestDraftId)
        cleanUpData(id)
    }

    void 'VB08a : test finding merge difference of two datamodels'() {
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String mainId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'right'])
        verifyResponse CREATED, response
        String rightId = responseBody().id

        when:
        GET("$leftId/mergeDiff/$rightId")

        then:
        verifyResponse OK, response
        responseBody().leftId == rightId
        responseBody().rightId == leftId

        when:
        GET("$leftId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().leftId == mainId
        responseBody().rightId == leftId

        when:
        GET("$rightId/mergeDiff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().leftId == mainId
        responseBody().rightId == rightId

        cleanup:
        cleanUpData(mainId)
        cleanUpData(leftId)
        cleanUpData(rightId)
        cleanUpData(id)
    }

    @PendingFeature
    void 'VB08b : test finding merge difference of two complex datamodels'() {
        given:
        String id = createNewItem(validJson)

        POST("$id/dataClasses", [label: 'deleteLeftOnly'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'deleteRightOnly'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyLeftOnly'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyRightOnly'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'deleteAndDelete'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'deleteAndModify'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyAndDelete'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyAndModifyReturningNoDifference'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'modifyAndModifyReturningDifference'])
        verifyResponse CREATED, response
        POST("$id/dataClasses", [label: 'existingClass'])
        verifyResponse CREATED, response
        String existingClass = responseBody().id
        POST("$id/dataClasses/$existingClass/dataClasses", [label: 'deleteLeftOnlyFromExistingClass'])
        verifyResponse CREATED, response
        POST("$id/dataClasses/$existingClass/dataClasses", [label: 'deleteRightOnlyFromExistingClass'])
        verifyResponse CREATED, response

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: 'main'])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        GET("$source/path/dm%3A%7Cdc%3AexistingClass")
        verifyResponse OK, response
        existingClass = responseBody().id
        GET("dataClasses/$existingClass/path/dc%3A%7Cdc%3AdeleteLeftOnlyFromExistingClass", MAP_ARG, true)
        verifyResponse OK, response
        String deleteLeftOnlyFromExistingClass = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AdeleteLeftOnly")
        verifyResponse OK, response
        String deleteLeftOnly = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AdeleteAndDelete")
        verifyResponse OK, response
        String deleteAndDelete = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AdeleteAndModify")
        verifyResponse OK, response
        String deleteAndModify = responseBody().id

        GET("$source/path/dm%3A%7Cdc%3AmodifyLeftOnly")
        verifyResponse OK, response
        String modifyLeftOnly = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AmodifyAndDelete")
        verifyResponse OK, response
        String modifyAndDelete = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AmodifyAndModifyReturningNoDifference")
        verifyResponse OK, response
        String modifyAndModifyReturningNoDifference = responseBody().id
        GET("$source/path/dm%3A%7Cdc%3AmodifyAndModifyReturningDifference")
        verifyResponse OK, response
        String modifyAndModifyReturningDifference = responseBody().id

        then:
        DELETE("$source/dataClasses/$existingClass/dataClasses/$deleteLeftOnlyFromExistingClass")
        verifyResponse NO_CONTENT, response
        DELETE("$source/dataClasses/$deleteLeftOnly")
        verifyResponse NO_CONTENT, response
        DELETE("$source/dataClasses/$deleteAndDelete")
        verifyResponse NO_CONTENT, response
        DELETE("$source/dataClasses/$deleteAndModify")
        verifyResponse NO_CONTENT, response

        PUT("$source/dataClasses/$modifyLeftOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("$source/dataClasses/$modifyAndDelete", [description: 'Description'])
        verifyResponse OK, response
        PUT("$source/dataClasses/$modifyAndModifyReturningNoDifference", [description: 'Description'])
        verifyResponse OK, response
        PUT("$source/dataClasses/$modifyAndModifyReturningDifference", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("$source/dataClasses/$existingClass/dataClasses", [label: 'addLeftToExistingClass'])
        verifyResponse CREATED, response
        POST("$source/dataClasses", [label: 'addLeftOnly'])
        verifyResponse CREATED, response
        POST("$source/dataClasses", [label: 'addAndAddReturningNoDifference'])
        verifyResponse CREATED, response
        POST("$source/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionLeft'])
        verifyResponse CREATED, response

        PUT("$source", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        when:
        GET("$target/path/dm%3A%7Cdc%3AexistingClass")
        verifyResponse OK, response
        existingClass = responseBody().id
        GET("dataClasses/$existingClass/path/dc%3A%7Cdc%3AdeleteRightOnlyFromExistingClass", MAP_ARG, true)
        verifyResponse OK, response
        String deleteRightOnlyFromExistingClass = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AdeleteRightOnly")
        verifyResponse OK, response
        String deleteRightOnly = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AdeleteAndDelete")
        verifyResponse OK, response
        deleteAndDelete = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AmodifyAndDelete")
        verifyResponse OK, response
        modifyAndDelete = responseBody().id

        GET("$target/path/dm%3A%7Cdc%3AmodifyRightOnly")
        verifyResponse OK, response
        String modifyRightOnly = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AdeleteAndModify")
        verifyResponse OK, response
        deleteAndModify = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AmodifyAndModifyReturningNoDifference")
        verifyResponse OK, response
        modifyAndModifyReturningNoDifference = responseBody().id
        GET("$target/path/dm%3A%7Cdc%3AmodifyAndModifyReturningDifference")
        verifyResponse OK, response
        modifyAndModifyReturningDifference = responseBody().id

        then:
        DELETE("$target/dataClasses/$existingClass/dataClasses/$deleteRightOnlyFromExistingClass")
        verifyResponse NO_CONTENT, response
        DELETE("$target/dataClasses/$deleteRightOnly")
        verifyResponse NO_CONTENT, response
        DELETE("$target/dataClasses/$deleteAndDelete")
        verifyResponse NO_CONTENT, response
        DELETE("$target/dataClasses/$modifyAndDelete")
        verifyResponse NO_CONTENT, response

        PUT("$target/dataClasses/$modifyRightOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("$target/dataClasses/$deleteAndModify", [description: 'Description'])
        verifyResponse OK, response
        PUT("$target/dataClasses/$modifyAndModifyReturningNoDifference", [description: 'Description'])
        verifyResponse OK, response
        PUT("$target/dataClasses/$modifyAndModifyReturningDifference", [description: 'DescriptionRight'])
        verifyResponse OK, response

        POST("$target/dataClasses/$existingClass/dataClasses", [label: 'addRightToExistingClass'])
        verifyResponse CREATED, response
        POST("$target/dataClasses", [label: 'addRightOnly'])
        verifyResponse CREATED, response
        POST("$target/dataClasses", [label: 'addAndAddReturningNoDifference'])
        verifyResponse CREATED, response
        POST("$target/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionRight'])
        verifyResponse CREATED, response

        PUT("$target", [description: 'DescriptionRight'])
        verifyResponse OK, response

        when:
        GET("$source/mergeDiff/$target", STRING_ARG)
//        GET("$source/mergeDiff/$target")

        then:
        verifyJsonResponse OK, '''{
"leftId": "${json-unit.matches:id}",
"rightId": "${json-unit.matches:id}",
"label": "Functional Test Model",
"count": 16,
"diffs": [
        {
            "description": {
            "left": "DescriptionRight",
            "right": "DescriptionLeft",
            "isMergeConflict": true,
            "commonAncestorValue": null
        }
        },
        {
            "branchName": {
            "left": "main",
            "right": "source",
            "isMergeConflict": false,
            "commonAncestorValue": null
        }
        },
        {
            "dataClasses": {
            "deleted": [
                    {
                        "id": "${json-unit.matches:id}",
                        "label": "deleteAndModify",
                        "breadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ]
                    },
                    {
                        "id": "${json-unit.matches:id}",
                        "label": "deleteLeftOnly",
                        "breadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ]
                    }
            ],
            "created": [
                    {
                        "id": "${json-unit.matches:id}",
                        "label": "addLeftOnly",
                        "breadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ]
                    },
                    {
                        "id": "${json-unit.matches:id}",
                        "label": "modifyAndDelete",
                        "breadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ]
                    }
            ],
            "modified": [
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "addAndAddReturningDifference",
                        "leftBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ],
                        "rightBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ],
                        "count": 1,
                        "diffs": [
                            {
                                "description": {
                                "left": "DescriptionRight",
                                "right": "DescriptionLeft",
                                "isMergeConflict": null,
                                "commonAncestorValue": null
                            }
                            }
                    ]
                    },
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "existingClass",
                        "leftBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ],
                        "rightBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ],
                        "count": 4,
                        "diffs": [
                            {
                                "dataClasses": {
                                "deleted": [
                                        {
                                            "id": "${json-unit.matches:id}",
                                            "label": "deleteLeftOnlyFromExistingClass",
                                            "breadcrumbs": [
                                                {
                                                    "id": "${json-unit.matches:id}",
                                                    "label": "Functional Test Model",
                                                    "domainType": "DataModel",
                                                    "finalised": false
                                                },
                                                {
                                                    "id": "${json-unit.matches:id}",
                                                    "label": "existingClass",
                                                    "domainType": "DataClass"
                                                }
                                        ]
                                        }
                                ],
                                "created": [
                                        {
                                            "id": "${json-unit.matches:id}",
                                            "label": "addLeftToExistingClass",
                                            "breadcrumbs": [
                                                {
                                                    "id": "${json-unit.matches:id}",
                                                    "label": "Functional Test Model",
                                                    "domainType": "DataModel",
                                                    "finalised": false
                                                },
                                                {
                                                    "id": "${json-unit.matches:id}",
                                                    "label": "existingClass",
                                                    "domainType": "DataClass"
                                                }
                                        ]
                                        }
                                ]
                            }
                            }
                    ]
                    },
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "modifyAndModifyReturningDifference",
                        "leftBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ],
                        "rightBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ],
                        "count": 1,
                        "diffs": [
                            {
                                "description": {
                                "left": "DescriptionRight",
                                "right": "DescriptionLeft",
                                "isMergeConflict": null,
                                "commonAncestorValue": null
                            }
                            }
                    ]
                    },
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "modifyLeftOnly",
                        "leftBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ],
                        "rightBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "DataModel",
                                "finalised": false
                            }
                    ],
                        "count": 1,
                        "diffs": [
                            {
                                "description": {
                                "left": null,
                                "right": "Description",
                                "isMergeConflict": null,
                                "commonAncestorValue": null
                            }
                            }
                    ]
                    }
            ]
        }
        }
]
}'''

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'test changing folder from DataModel context'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/folder/${movingFolderId}", [:])

        then:
        verifyResponse OK, response

        when:
        GET("folders/$movingFolderId/dataModels", MAP_ARG, true)

        then:
        response.body().count == 1
        response.body().items[0].id == id

        when:
        GET("folders/$folderId/dataModels", MAP_ARG, true)

        then:
        response.body().count == 0

        cleanup:
        cleanUpData(id)
    }

    void 'test changing folder from Folder context'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        response = PUT("folders/${movingFolderId}/dataModels/$id", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        when:
        GET("folders/$movingFolderId/dataModels", MAP_ARG, true)

        then:
        response.body().count == 1
        response.body().items[0].id == id

        when:
        GET("folders/$folderId/dataModels", MAP_ARG, true)

        then:
        response.body().count == 0

        cleanup:
        cleanUpData(id)
    }

    void 'D01 : test diffing 2 DataModels'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)
        String otherId = createNewItem([label: 'Functional Test Model 2'])

        when:
        GET("${id}/diff/${otherId}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "count": 1,
  "label": "Functional Test Model",
  "diffs": [
    {
      "label": {
        "left": "Functional Test Model",
        "right": "Functional Test Model 2"
      }
    }
  ]
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'D02 : test diffing branches'() {
        given:
        // Create base model and finalise
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        // Create a new branch main
        PUT("$id/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String mainId = responseBody().id

        // Create a new branch test
        PUT("$id/newBranchModelVersion", [branchName: 'test'])
        verifyResponse CREATED, response
        String testId = responseBody().id

        // Add dataclass test2 to main branch
        POST("$mainId/dataClasses", [label: 'test2'])
        verifyResponse CREATED, response

        // Add dataclass test2 to test branch
        POST("$testId/dataClasses", [label: 'test2'])
        verifyResponse CREATED, response

        when: 'performing diff'
        GET("$testId/diff/$mainId")

        then:
        verifyResponse OK, response
        responseBody()

        cleanup:
        cleanUpData(id)
        cleanUpData(testId)
        cleanUpData(mainId)
    }

    void 'D03 : test diffing branches on modifications'() {
        given:
        // Create base model
        String id = createNewItem(validJson)
        // Create content
        POST("$id/dataClasses", [label: 'parent'])
        verifyResponse(CREATED, response)
        String parentId = responseBody().id
        POST("$id/dataClasses/${parentId}/dataClasses", [label: 'child'])
        verifyResponse(CREATED, response)
        POST("$id/dataClasses", [label: 'content', description: 'some interesting content'])
        verifyResponse(CREATED, response)

        //Finalise
        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        // Create a new branch main
        PUT("$id/newBranchModelVersion", [:])
        verifyResponse CREATED, response
        String mainId = responseBody().id

        // Create a new branch test
        PUT("$id/newBranchModelVersion", [branchName: 'test'])
        verifyResponse CREATED, response
        String testId = responseBody().id

        //Change child DC label
        GET("$testId/dataClasses")
        verifyResponse(OK, response)
        parentId = responseBody().items.find { it.label == 'parent' }.id
        String contentId = responseBody().items.find { it.label == 'content' }.id
        GET("$testId/dataClasses/${parentId}/dataClasses")
        verifyResponse(OK, response)
        assert responseBody().items.first().id
        assert responseBody().items.first().label == 'child'
        String childId = responseBody().items.first().id
        PUT("$testId/dataClasses/${parentId}/dataClasses/$childId", [label: 'child edit'])
        verifyResponse(OK, response)
        // change description of the content
        PUT("$testId/dataClasses/$contentId", [description: 'a change to the description'])

        when: 'performing diff'
        GET("$testId/diff/$mainId")

        then:
        verifyResponse OK, response
        responseBody().count == 4
        responseBody().diffs.size() == 2
        responseBody().diffs.first().branchName.left == 'test'
        responseBody().diffs.first().branchName.right == 'main'

        and:
        Map dataClassesDiffs = responseBody().diffs[1].dataClasses
        dataClassesDiffs.modified.size() == 2

        and:
        Map contentDiff = dataClassesDiffs.modified.find { it.label == 'content' }
        contentDiff.diffs.size() == 1
        contentDiff.diffs.first().description.left == 'a change to the description'
        contentDiff.diffs.first().description.right == 'some interesting content'

        and:
        Map parentDiff = dataClassesDiffs.modified.find { it.label == 'parent' }
        parentDiff.diffs.size() == 1
        parentDiff.diffs.first().dataClasses.deleted.size() == 1
        parentDiff.diffs.first().dataClasses.created.size() == 1
        parentDiff.diffs.first().dataClasses.deleted.first().label == 'child edit'
        parentDiff.diffs.first().dataClasses.created.first().label == 'child'

        cleanup:
        cleanUpData(id)
        cleanUpData(testId)
        cleanUpData(mainId)
    }

    void 'test export a single DataModel'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "dataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test Model",
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
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "JsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'test export multiple DataModels (json only exports first id)'() {
        given:
        String id = createNewItem(validJson)
        String id2 = createNewItem([label: 'Functional Test Model 2'])

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0',
                [dataModelIds: [id, id2]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
  "dataModel": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test Model",
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
    "exportedBy": "Unlogged User",
    "exportedOn": "${json-unit.matches:offsetDateTime}",
    "exporter": {
      "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
      "name": "JsonExporterService",
      "version": "2.0"
    }
  }
}'''

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'test import basic DataModel'() {
        given:
        String id = createNewItem(validJson)

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                modelName                      : 'Functional Test Import',
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: exportedJsonString.bytes.toList()
                ]
        ], STRING_ARG)

        then:
        verifyJsonResponse CREATED, '''{
                  "count": 1,
                  "items": [
                    {
                      "domainType": "DataModel",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Import",
                      "type": "Data Standard",
                      "branchName": "main",
                      "documentationVersion": "1.0.0"
                    }
                  ]
                }'''

        cleanup:
        cleanUpData(id)
    }

    void 'test import basic DataModel as new documentation version'() {
        given:
        String id = createNewItem([
                label       : 'Functional Test Model',
                finalised   : true,
                modelVersion: Version.from('1.0.0')
        ])

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : true,
                modelName                      : 'Functional Test Model',
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: true,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: exportedJsonString.bytes.toList()
                ]
        ], STRING_ARG)

        then:
        verifyJsonResponse CREATED, '''{
                  "count": 1,
                  "items": [
                    {
                      "domainType": "DataModel",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Model",
                      "type": "Data Standard",
                      "branchName": "main",
                      "documentationVersion": "2.0.0",
                      "modelVersion": "1.0.0"
                    }
                  ]
                }'''

        cleanup:
        cleanUpData(id)
    }

    void 'test delete multiple models'() {
        given:
        def idstoDelete = []
        (1..4).each { n ->
            idstoDelete << createNewItem([
                    folder: folderId,
                    label : UUID.randomUUID().toString()
            ])
        }

        when:
        DELETE('', [
                ids      : idstoDelete,
                permanent: false
        ], STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "count": 4,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "${json-unit.matches:id}",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "${json-unit.matches:id}",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "${json-unit.matches:id}",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataModel",
      "label": "${json-unit.matches:id}",
      "type": "Data Standard",
      "branchName": "main",
      "documentationVersion": "1.0.0",
      "deleted": true
    }
  ]
}'''

        when:
        DELETE('', [
                ids      : idstoDelete,
                permanent: true
        ])

        then:
        verifyResponse NO_CONTENT, response
    }

    void 'test importing simple test DataModel'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : true,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('simpleDataModel').toList()
                ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'test importing complex test DataModel'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : true,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('complexDataModel').toList()
                ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'test importing DataModel with classifiers'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : true,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('fullModelWithClassifiers').toList()
                ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'test export simple DataModel'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('simpleDataModel').toList()
                ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('simpleDataModel'))
                .replaceFirst('"exportedBy": "Admin User",', '"exportedBy": "Unlogged User",')
                .replace(/Test Authority/, 'Mauro Data Mapper')

        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    void 'test export complex DataModel'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('complexDataModel').toList()
                ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('complexDataModel'))
                .replaceFirst('"exportedBy": "Admin User",', '"exportedBy": "Unlogged User",')
                .replace(/Test Authority/, 'Mauro Data Mapper')

        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    void 'test getting simple DataModel hierarchy'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('simpleDataModel').toList()
                ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        expect:
        id

        when:
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "childDataClasses": [
    {
      "dataClasses": [],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "dataElements": [],
      "domainType": "DataClass",
      "availableActions": ["delete","show","update"],
      "model": "${json-unit.matches:id}",
      "id": "${json-unit.matches:id}",
      "label": "simple",
      "breadcrumbs": [
        {
          "domainType": "DataModel",
          "finalised": false,
          "id": "${json-unit.matches:id}",
          "label": "Simple Test DataModel"
        }
      ]
    }
  ],
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "dataTypes": [],
  "domainType": "DataModel",
  "documentationVersion": "1.0.0",
  "availableActions": ["delete","show","update"],
  "branchName":"main",
  "finalised": false,
  "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
    },
  "id": "${json-unit.matches:id}",
  "label": "Simple Test DataModel",
  "type": "Data Standard",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "classifiers": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test classifier simple",
      "lastUpdated": "${json-unit.matches:offsetDateTime}"
    }
  ]
}'''

        cleanup:
        cleanUpData(id)
    }

    void 'test getting complex DataModel hierarchy'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('complexDataModel').toList()
                ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        expect:
        id

        when:
        GET("${id}/hierarchy", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "id": "${json-unit.matches:id}",
  "domainType": "DataModel",
  "label": "Complex Test DataModel",
  "availableActions": [
    "delete",
    "show",
    "update"
  ],
  "branchName":"main",
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
  "authority": {
      "id": "${json-unit.matches:id}",
      "url": "http://localhost",
      "label": "Mauro Data Mapper"
  },
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
        "delete",
        "show",
        "update"
      ],
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "enumerationValues": [
        {
          "index": 2,
          "id": "${json-unit.matches:id}",
          "key": "U",
          "value": "Unknown",
          "category": null
        },
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
        "delete",
        "show",
        "update"
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
        "delete",
        "show",
        "update"
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
        "delete",
        "show",
        "update"
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
        "delete",
        "show",
        "update"
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
            "delete",
            "show",
            "update"
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
        },
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
            "delete",
            "show",
            "update"
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
        "delete",
        "show",
        "update"
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
        "delete",
        "show",
        "update"
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
            "delete",
            "show",
            "update"
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
            "delete",
            "show",
            "update"
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

        cleanup:
        cleanUpData(id)
    }

    void 'test diffing 2 complex and simple DataModels'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('complexDataModel').toList()
                ]
        ])
        verifyResponse CREATED, response
        String complexDataModelId = response.body().items[0].id

        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('simpleDataModel').toList()
                ]
        ])
        verifyResponse CREATED, response
        String simpleDataModelId = response.body().items[0].id

        expect:
        complexDataModelId
        simpleDataModelId

        when:
        GET("${complexDataModelId}/diff/${simpleDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "label": "Complex Test DataModel",
  "count": 20,
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
            "id": "${json-unit.matches:id}",
            "namespace": "test.com",
            "key": "mdk1",
            "value": "mdv1"
          },
          {
            "id": "${json-unit.matches:id}",
            "namespace": "test.com/test",
            "key": "mdk1",
            "value": "mdv2"
          }
        ],
        "created": [
          {
            "id": "${json-unit.matches:id}",
            "namespace": "test.com/simple",
            "key": "mdk1",
            "value": "mdv1"
          },
          {
            "id": "${json-unit.matches:id}",
            "namespace": "test.com/simple",
            "key": "mdk2",
            "value": "mdv2"
          }
        ]
      }
    },
    {
      "annotations": {
        "deleted": [
          {
            "id": "${json-unit.matches:id}",
            "label": "test annotation 2"
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "test annotation 1"
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
          },
          {
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
          },
          {
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
          },
          {
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
        ]
      }
    },
    {
      "dataClasses": {
        "deleted": [
          {
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
          },
          {
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
          },
          {
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
        ],
        "created": [
          {
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
        ]
      }
    },
    {
      "dataElements": {
        "deleted": [
          {
            "id": "${json-unit.matches:id}",
            "label": "child",
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
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "element2",
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
            ]
          },
          {
            "id": "${json-unit.matches:id}",
            "label": "ele1",
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
            ]
          }
        ]
      }
    }
  ]
}'''

        cleanup:
        cleanUpData(complexDataModelId)
        cleanUpData(simpleDataModelId)
    }


    void 'test searching for label "emptyclass" in complex model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('complexDataModel').toList()
                ]
        ])

        verifyResponse CREATED, response
        String id = response.body().items[0].id
        String term = 'emptyclass'

        expect:
        id

        when:
        GET("${id}/search?search=${term}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
                      "count": 1,
                      "items": [
                        {
                          "domainType": "DataClass",
                          "description": "dataclass with desc",
                          "id": "${json-unit.matches:id}",
                          "label": "emptyclass",
                          "model": "${json-unit.matches:id}",
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
        cleanup:
        cleanUpData(id)
    }

    void 'test searching for label "emptyclass" in simple model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('simpleDataModel').toList()
                ]
        ])

        verifyResponse CREATED, response
        String id = response.body().items[0].id
        String term = 'emptyclass'

        expect:
        id

        when:
        GET("${id}/search?search=${term}",)

        then:
        verifyResponse OK, response
        response.body().count == 0
        response.body().items.size() == 0

        cleanup:
        cleanUpData(id)
    }

    @Transactional
    void setupForLinkSuggestions(String simpleDataModelId) {
        DataClass dataClass = DataClass.byDataModelId(Utils.toUuid(simpleDataModelId)).eq('label', 'simple').find()
        assert dataClass


        POST("${simpleDataModelId}/dataTypes", [
                domainType: 'PrimitiveType',
                label     : 'string'
        ])
        verifyResponse(CREATED, response)
        String dataTypeId = response.body().id

        assert dataTypeId

        POST("${simpleDataModelId}/dataClasses/${dataClass.id}/dataElements", [
                domainType : 'DataElement',
                label      : 'ele1',
                description: 'most obvious match',
                dataType   : [
                        id: dataTypeId
                ]
        ])
        verifyResponse CREATED, response

        POST("${simpleDataModelId}/dataClasses/${dataClass.id}/dataElements", [
                domainType : 'DataElement',
                label      : 'ele2',
                description: 'least obvious match',
                dataType   : [
                        id: dataTypeId
                ]
        ])
        verifyResponse CREATED, response
    }


    void 'test get link suggestions for a model with no data elements in the target'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('complexDataModel').toList()
                ]
        ])
        verifyResponse CREATED, response
        String complexDataModelId = response.body().items[0].id

        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('simpleDataModel').toList()
                ]
        ])
        verifyResponse CREATED, response
        String simpleDataModelId = response.body().items[0].id


        expect:
        complexDataModelId
        simpleDataModelId

        when:
        GET("${complexDataModelId}/suggestLinks/${simpleDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedLinkSuggestions(['', '', ''])

        cleanup:
        cleanUpData(complexDataModelId)
        cleanUpData(simpleDataModelId)
    }

    void 'test get link suggestions for a model'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('complexDataModel').toList()
                ]
        ])
        verifyResponse CREATED, response
        String complexDataModelId = response.body().items[0].id

        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/JsonImporterService/2.0', [
                finalised                      : false,
                folderId                       : folderId.toString(),
                importAsNewDocumentationVersion: false,
                importFile                     : [
                        fileName    : 'FT Import',
                        fileType    : MimeType.JSON_API.name,
                        fileContents: loadTestFile('simpleDataModel').toList()
                ]
        ])
        verifyResponse CREATED, response
        String simpleDataModelId = response.body().items[0].id

        expect:
        complexDataModelId
        simpleDataModelId

        when:
        setupForLinkSuggestions(simpleDataModelId)

        GET("${complexDataModelId}/suggestLinks/${simpleDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedLinkSuggestions(expectedLinkSuggestionResults())

        cleanup:
        cleanUpData(complexDataModelId)
        cleanUpData(simpleDataModelId)
    }

    String expectedLinkSuggestions(List<String> results) {
        '''{
                      "links": [
                        {
                          "sourceDataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                              "domainType": "ReferenceType",
                              "model": "${json-unit.matches:id}",
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
                                "model": "${json-unit.matches:id}",
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
                            "model": "${json-unit.matches:id}",
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
                          },
                          "results": [''' + results[2] + '''

                          ]
                        },
                        {
                          "sourceDataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                              "domainType": "PrimitiveType",
                              "model": "${json-unit.matches:id}",
                              "id": "${json-unit.matches:id}",
                              "label": "string",
                              "breadcrumbs": [
                                {
                                  "domainType": "DataModel",
                                  "finalised": false,
                                  "id": "${json-unit.matches:id}",
                                  "label": "Complex Test DataModel"
                                }
                              ]
                            },
                            "model": "${json-unit.matches:id}",
                            "maxMultiplicity": 20,
                            "id": "${json-unit.matches:id}",
                            "label": "ele1",
                            "minMultiplicity": 0,
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
                                "label": "content"
                              }
                            ]
                          },
                          "results": [''' + results[0] + '''

                          ]
                        },
                        {
                          "sourceDataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                              "domainType": "PrimitiveType",
                              "model": "${json-unit.matches:id}",
                              "id": "${json-unit.matches:id}",
                              "label": "integer",
                              "breadcrumbs": [
                                {
                                  "domainType": "DataModel",
                                  "finalised": false,
                                  "id": "${json-unit.matches:id}",
                                  "label": "Complex Test DataModel"
                                }
                              ]
                            },
                            "model": "${json-unit.matches:id}",
                            "maxMultiplicity": 1,
                            "id": "${json-unit.matches:id}",
                            "label": "element2",
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
                                "label": "content"
                              }
                            ]
                          },
                          "results": [''' + results[1] + '''

                          ]
                        }
                      ]
                    }'''
    }

    List<String> expectedLinkSuggestionResults() {
        ['''
                    {
                        "score": "${json-unit.any-number}",
                        "dataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                                "domainType": "PrimitiveType",
                                "model": "${json-unit.matches:id}",
                                "id": "${json-unit.matches:id}",
                                "label": "string",
                                "breadcrumbs": [
                                    {
                                        "domainType": "DataModel",
                                        "finalised": false,
                                        "id": "${json-unit.matches:id}",
                                        "label": "Simple Test DataModel"
                                    }
                                ]
                            },
                            "model": "${json-unit.matches:id}",
                            "description": "most obvious match",
                            "id": "${json-unit.matches:id}",
                            "label": "ele1",
                            "breadcrumbs": [
                                {
                                    "domainType": "DataModel",
                                    "finalised": false,
                                    "id": "${json-unit.matches:id}",
                                    "label": "Simple Test DataModel"
                                },
                                {
                                    "domainType": "DataClass",
                                    "id": "${json-unit.matches:id}",
                                    "label": "simple"
                                }
                            ]
                        }
                    },
                    {
                        "score": "${json-unit.any-number}",
                        "dataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                                "domainType": "PrimitiveType",
                                "model": "${json-unit.matches:id}",
                                "id": "${json-unit.matches:id}",
                                "label": "string",
                                "breadcrumbs": [
                                    {
                                        "domainType": "DataModel",
                                        "finalised": false,
                                        "id": "${json-unit.matches:id}",
                                        "label": "Simple Test DataModel"
                                    }
                                ]
                            },
                            "model": "${json-unit.matches:id}",
                            "description": "least obvious match",
                            "id": "${json-unit.matches:id}",
                            "label": "ele2",
                            "breadcrumbs": [
                                {
                                    "domainType": "DataModel",
                                    "finalised": false,
                                    "id": "${json-unit.matches:id}",
                                    "label": "Simple Test DataModel"
                                },
                                {
                                    "domainType": "DataClass",
                                    "id": "${json-unit.matches:id}",
                                    "label": "simple"
                                }
                            ]
                        }
                    }''', '''
                    {
                        "score": "${json-unit.any-number}",
                        "dataElement": {
                            "domainType": "DataElement",
                            "dataClass": "${json-unit.matches:id}",
                            "dataType": {
                                "domainType": "PrimitiveType",
                                "model": "${json-unit.matches:id}",
                                "id": "${json-unit.matches:id}",
                                "label": "string",
                                "breadcrumbs": [
                                    {
                                        "domainType": "DataModel",
                                        "finalised": false,
                                        "id": "${json-unit.matches:id}",
                                        "label": "Simple Test DataModel"
                                    }
                                ]
                            },
                            "model": "${json-unit.matches:id}",
                            "description": "least obvious match",
                            "id": "${json-unit.matches:id}",
                            "label": "ele2",
                            "breadcrumbs": [
                                {
                                    "domainType": "DataModel",
                                    "finalised": false,
                                    "id": "${json-unit.matches:id}",
                                    "label": "Simple Test DataModel"
                                },
                                {
                                    "domainType": "DataClass",
                                    "id": "${json-unit.matches:id}",
                                    "label": "simple"
                                }
                            ]
                        }
                    }''', '''''']
    }
}