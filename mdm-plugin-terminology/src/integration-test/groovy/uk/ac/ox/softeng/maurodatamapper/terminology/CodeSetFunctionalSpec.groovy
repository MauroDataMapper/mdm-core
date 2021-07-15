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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.BAD_REQUEST
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * <pre>
 * Controller: codeSet
 *  |   POST   | /api/folders/${folderId}/codeSets  | Action: save
 *  |   GET    | /api/folders/${folderId}/codeSets  | Action: index
 *
 *  |  DELETE  | /api/codeSets/${id}  | Action: delete
 *  |   PUT    | /api/codeSets/${id}  | Action: update
 *  |   GET    | /api/codeSets/${id}  | Action: show
 *  |   GET    | /api/codeSets        | Action: index
 *
 *  |  DELETE  | /api/codeSets/${codeSetId}/terms/${termId}  | Action: alterTerms
 *  |   PUT    | /api/codeSets/${codeSetId}/terms/${termId}  | Action: alterTerms
 *
 *  |   GET    | /api/codeSets/providers/importers  | Action: importerProviders
 *  |   GET    | /api/codeSets/providers/exporters  | Action: exporterProviders
 *
 *  |  DELETE  | /api/codeSets/${codeSetId}/readByAuthenticated  | Action: readByAuthenticated
 *  |   PUT    | /api/codeSets/${codeSetId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  DELETE  | /api/codeSets/${codeSetId}/readByEveryone       | Action: readByEveryone
 *  |   PUT    | /api/codeSets/${codeSetId}/readByEveryone       | Action: readByEveryone
 *
 *  |   PUT    | /api/codeSets/${codeSetId}/newForkModel          | Action: newForkModel
 *  |   PUT    | /api/codeSets/${codeSetId}/newDocumentationVersion  | Action: newDocumentationVersion
 *  |   PUT    | /api/codeSets/${codeSetId}/finalise                 | Action: finalise
 *
 *  |   PUT    | /api/codeSets/${codeSetId}/folder/${folderId}   | Action: changeFolder
 *  |   PUT    | /api/folders/${folderId}/codeSets/${codeSetId}  | Action: changeFolder
 *
 *  |   GET    | /api/codeSets/${codeSetId}/diff/${otherCodeSetId}  | Action: diff
 *
 *  |  DELETE  | /api/codeSets  | Action: deleteAll
 *
 *  |   GET    | /api/codeSets/${codeSetId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportModel
 *  |   POST   | /api/codeSets/import/${importerNamespace}/${importerName}/${importerVersion}  | Action: importModels
 *  |   POST   | /api/codeSets/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportModels
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetController
 */
@Integration
@Transactional
@Slf4j
class CodeSetFunctionalSpec extends ResourceFunctionalSpec<CodeSet> {

    @Shared
    UUID folderId

    @Shared
    UUID movingFolderId

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        assert Folder.count() == 0
        assert CodeSet.count() == 0
        folderId = new Folder(label: 'Functional Test Folder', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id
        assert folderId
        movingFolderId = new Folder(label: 'Functional Test Folder 2', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id
        assert movingFolderId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec CodeSetFunctionalSpec')
        cleanUpResources(Terminology, Folder, Classifier)
    }

    @Override
    String getResourcePath() {
        'codeSets'
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
  "id": "${json-unit.matches:id}",
  "domainType": "CodeSet",
  "label": "Functional Test Model",
  "availableActions": [
    "delete",
    "show",
    "update"
  ],
  "branchName": "main",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "type": "CodeSet",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Test Authority"
  }
}'''
    }

    String getExpectedMergeDiffJson() {
        '''{
  "leftId": "${json-unit.matches:id}",
  "rightId": "${json-unit.matches:id}",
  "label": "Functional Test Model",
  "count": 9,
  "diffs": [
    {
      "branchName": {
        "left": "main",
        "right": "source",
        "isMergeConflict": false
      }
    },
    {
      "description": {
        "left": "DescriptionRight",
        "right": "DescriptionLeft",
        "isMergeConflict": true,
        "commonAncestorValue": null
      }
    },
    {
      "terms": {
        "deleted": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "DAM: deleteAndModify",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Functional Test Model",
                  "domainType": "Terminology",
                  "finalised": true
                }
              ]
            },
            "isMergeConflict": true,
            "commonAncestorValue": {
              "id": "${json-unit.matches:id}",
              "label": "DAM: deleteAndModify",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Functional Test Model",
                  "domainType": "Terminology",
                  "finalised": true
                }
              ]
            }
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "DLO: deleteLeftOnly",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Functional Test Model",
                  "domainType": "Terminology",
                  "finalised": true
                }
              ]
            },
            "isMergeConflict": false
          }
        ],
        "created": [
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "ALO: addLeftOnly",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Functional Test Model",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            },
            "isMergeConflict": false
          },
          {
            "value": {
              "id": "${json-unit.matches:id}",
              "label": "MAD: modifyAndDelete",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Functional Test Model",
                  "domainType": "Terminology",
                  "finalised": false
                }
              ]
            },
            "isMergeConflict": true,
            "commonAncestorValue": {
              "id": "${json-unit.matches:id}",
              "label": "MAD: modifyAndDelete",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Functional Test Model",
                  "domainType": "Terminology",
                  "finalised": true
                }
              ]
            }
          }
        ],
        "modified": [
          {
            "leftId": "${json-unit.matches:id}",
            "rightId": "${json-unit.matches:id}",
            "label": "MLO: modifyLeftOnly",
            "leftBreadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Model",
                "domainType": "Terminology",
                "finalised": true
              }
            ],
            "rightBreadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Model",
                "domainType": "Terminology",
                "finalised": true
              }
            ],
            "count": 1,
            "diffs": [
              {
                "description": {
                  "left": null,
                  "right": "Description",
                  "isMergeConflict": false
                }
              }
            ]
          },
          {
            "leftId": "${json-unit.matches:id}",
            "rightId": "${json-unit.matches:id}",
            "label": "MAMRD: modifyAndModifyReturningDifference",
            "leftBreadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Model",
                "domainType": "Terminology",
                "finalised": false
              }
            ],
            "rightBreadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Model",
                "domainType": "Terminology",
                "finalised": false
              }
            ],
            "count": 1,
            "diffs": [
              {
                "description": {
                  "left": "DescriptionRight",
                  "right": "DescriptionLeft",
                  "isMergeConflict": true,
                  "commonAncestorValue": null
                }
              }
            ]
          },
          {
            "leftId": "${json-unit.matches:id}",
            "rightId": "${json-unit.matches:id}",
            "label": "AAARD: addAndAddReturningDifference",
            "leftBreadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Model",
                "domainType": "Terminology",
                "finalised": false
              }
            ],
            "rightBreadcrumbs": [
              {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Model",
                "domainType": "Terminology",
                "finalised": false
              }
            ],
            "count": 1,
            "diffs": [
              {
                "description": {
                  "left": "DescriptionRight",
                  "right": "DescriptionLeft",
                  "isMergeConflict": true,
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
    }

    void 'test finalising CodeSet'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/finalise", [versionChangeType: "Major"])

        then:
        verifyResponse OK, response

        and:
        response.body().availableActions == ['delete', 'show', 'update',]
        response.body().finalised
        response.body().dateFinalised

        when: 'List edits for the CodeSet'
        GET("$id/edits", MAP_ARG)

        then: 'The response is OK'
        verifyResponse OK, response

        and: 'There is not a CHANGENOTICE edit'
        !response.body().items.find {
            it.description == "Functional Test Change Notice"
        }

        cleanup:
        cleanUpData(id)
    }

    void 'test finalising CodeSet with a changeNotice'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/finalise", [versionChangeType: "Major", changeNotice: "Functional Test Change Notice"])

        then:
        verifyResponse OK, response

        and:
        response.body().availableActions == ['delete', 'show', 'update',]
        response.body().finalised
        response.body().dateFinalised

        when: 'List edits for the CodeSet'
        GET("$id/edits", MAP_ARG)

        then: 'The response is OK'
        verifyResponse OK, response

        and: 'There is a CHANGENOTICE edit'
        response.body().items.find {
            it.title == "CHANGENOTICE" && it.description == "Functional Test Change Notice"
        }

        cleanup:
        cleanUpData(id)
    }

    void 'Test undoing a soft delete using the admin endpoint'() {
        given: 'model is deleted'
        String id = createNewItem(validJson)
        DELETE(id)
        verifyResponse(OK, response)
        assert responseBody().deleted

        when:
        PUT("admin/$resourcePath/$id/undoSoftDelete", [:], MAP_ARG, true)

        then:
        verifyResponse(OK, response)
        responseBody().deleted == null

        cleanup:
        cleanUpData(id)
    }

    void 'Test undoing a soft delete via update'() {
        given: 'model is deleted'
        String id = createNewItem(validJson)
        DELETE(id)
        verifyResponse(OK, response)
        assert responseBody().deleted

        when:
        PUT(id, [deleted: false])

        then:
        verifyResponse(FORBIDDEN, response)
        responseBody().additional == 'Cannot update a deleted Model'

        cleanup:
        cleanUpData(id)
    }

    void 'VF01 : test creating a new fork model of a CodeSet'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response

        when: 'adding one new model'
        PUT("$id/newForkModel", [label: 'Functional Test CodeSet reader'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
            .replaceFirst(/"label": "Functional Test Model",/, '"label": "Functional Test CodeSet reader",')


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
      "sourceMultiFacetAwareItem": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test CodeSet reader"
      },
      "targetMultiFacetAwareItem": {
        "domainType": "CodeSet",
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
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test CodeSet reader"
      },
      "targetModel": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when: 'adding another'
        PUT("$id/newForkModel", [label: 'Functional Test CodeSet editor'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
            .replaceFirst(/"label": "Functional Test Model",/, '"label": "Functional Test CodeSet editor",')

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
      "sourceMultiFacetAwareItem": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test CodeSet reader"
      },
      "targetMultiFacetAwareItem": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    },
    {
      "domainType": "SemanticLink",
      "linkType": "Refines",
      "id": "${json-unit.matches:id}",
      "unconfirmed":false,
      "sourceMultiFacetAwareItem": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test CodeSet editor"
      },
      "targetMultiFacetAwareItem": {
        "domainType": "CodeSet",
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
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test CodeSet reader"
      },
      "targetModel": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    },
     {
      "domainType": "VersionLink",
      "linkType": "New Fork Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test CodeSet editor"
      },
      "targetModel": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        cleanup:
        cleanUpData()
    }

    void 'VD01 : test creating a new documentation version of a CodeSet'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response

        when:
        PUT("$id/newDocumentationVersion", [:], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
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
      "sourceMultiFacetAwareItem": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetMultiFacetAwareItem": {
        "domainType": "CodeSet",
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
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "domainType": "CodeSet",
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

    void 'VB01 : test creating a new main branch model version of a CodeSet'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
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
      "sourceMultiFacetAwareItem": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetMultiFacetAwareItem": {
        "domainType": "CodeSet",
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
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "domainType": "CodeSet",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''
        cleanup:
        cleanUpData()
    }

    void 'VB02 : test creating a main branch model version finalising and then creating another main branch of a CodeSet'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response

        when: 'create second model'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response

        when: 'finalising second model'
        String secondId = responseBody().id
        PUT("$secondId/finalise", [versionChangeType: "Major"])

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
            it.targetMultiFacetAwareItem.id == id &&
            it.sourceMultiFacetAwareItem.id == secondId
        }
        // This is unconfirmed as its copied
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
            it.targetMultiFacetAwareItem.id == id &&
            it.sourceMultiFacetAwareItem.id == thirdId &&
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
            it.targetMultiFacetAwareItem.id == id &&
            it.sourceMultiFacetAwareItem.id == secondId
        }
        responseBody().items.any {
            it.linkType == SemanticLinkType.REFINES.label &&
            it.targetMultiFacetAwareItem.id == secondId &&
            it.sourceMultiFacetAwareItem.id == thirdId
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
        PUT("$id/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response

        when: 'create default main'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse CREATED, response

        when: 'another main branch created'
        PUT("$id/newBranchModelVersion", [:])

        then:
        verifyResponse UNPROCESSABLE_ENTITY, response
        responseBody().errors.first().message == 'Property [branchName] of class [class uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet] ' +
        'with value [main] already exists for label [Functional Test Model]'

        cleanup:
        cleanUpData()
    }

    void 'VB04 : test creating a non-main branch model version without main existing'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
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

    void 'VB05 : test finding common ancestor of two Model<T> (as editor)'() {
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: 'left'])
        verifyResponse CREATED, response
        String leftId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'right'])
        verifyResponse CREATED, response
        String rightId = responseBody().id

        when:
        GET("$leftId/commonAncestor/$rightId")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().label == 'Functional Test Model'

        cleanup:
        cleanUpData(leftId)
        cleanUpData(rightId)
        cleanUpData(id)
    }

    void 'VB06 : test finding latest finalised version of a Model<T> (as editor)'() {
        /*
        id (finalised) -- expectedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String expectedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$expectedId/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response
        PUT("$expectedId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
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


    void 'VB07 : test finding latest model version of a Model<T> (as editor)'() {
        /*
        id (finalised) -- expectedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String expectedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$expectedId/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response
        PUT("$expectedId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
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

    void 'VB08 : test finding merge difference of two Model<T> (as editor)'() {
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
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

    void 'VB09a : test merging diff with no patch data'() {
        given:
        String id = createNewItem(validJson)

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        PUT("$source/mergeInto/$target", [:])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().total == 1
        responseBody().errors[0].message.contains('cannot be null')

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'VB09b : test merging diff with URI id not matching body id'() {
        given:
        String id = createNewItem(validJson)

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        PUT("$source/mergeInto/$target", [patch:
                                              [
                                                  leftId : "$target" as String,
                                                  rightId: "${UUID.randomUUID().toString()}" as String,
                                                  label  : "Functional Test Model",
                                                  count  : 0,
                                                  diffs  : []
                                              ]
        ])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().message == 'Source model id passed in request body does not match source model id in URI.'

        when:
        PUT("$source/mergeInto/$target", [patch:
                                              [
                                                  leftId : "${UUID.randomUUID().toString()}" as String,
                                                  rightId: "$source" as String,
                                                  label  : "Functional Test Model",
                                                  count  : 0,
                                                  diffs  : []
                                              ]
        ])

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().message == 'Target model id passed in request body does not match target model id in URI.'

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'VB09c : test merging diff into draft model'() {
        //TODO This test currently uses the ModelService mergeInto method which doesn't cater for modifications to codesets - update/override required
        given:
        POST("folders/${folderId}/terminologies", validJson, MAP_ARG, true)
        verifyResponse(CREATED, response)
        String baseTerminology = responseBody().id

        POST("terminologies/$baseTerminology/terms", [code: 'DLO', definition: 'deleteLeftOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        String deleteLeftOnly = responseBody().id
        POST("terminologies/$baseTerminology/terms", [code: 'MLO', definition: 'modifyLeftOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        String modifyLeftOnly = responseBody().id
        POST("terminologies/$baseTerminology/terms", [code: 'DAM', definition: 'deleteAndModify'], MAP_ARG, true)
        verifyResponse CREATED, response
        String deleteAndModify = responseBody().id
        POST("terminologies/$baseTerminology/terms", [code: 'MAD', definition: 'modifyAndDelete'], MAP_ARG, true)
        verifyResponse CREATED, response
        String modifyAndDelete = responseBody().id
        POST("terminologies/$baseTerminology/terms", [code: 'MAMRD', definition: 'modifyAndModifyReturningDifference'], MAP_ARG, true)
        verifyResponse CREATED, response
        String modifyAndModifyReturningDifference = responseBody().id

        PUT("terminologies/$baseTerminology/finalise", [versionChangeType: 'Major'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$baseTerminology/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME], MAP_ARG, true)
        verifyResponse CREATED, response
        String targetTerminology = responseBody().id
        PUT("terminologies/$baseTerminology/newBranchModelVersion", [branchName: 'source'], MAP_ARG, true)
        verifyResponse CREATED, response
        String sourceTerminology = responseBody().id

        String baseCodeSet = createNewItem(validJson)

        PUT("$baseCodeSet", [terms: [
            [id: deleteLeftOnly],
            [id: modifyLeftOnly],
            [id: deleteAndModify],
            [id: modifyAndDelete],
            [id: modifyAndModifyReturningDifference]
        ]])
        verifyResponse OK, response

        PUT("$baseCodeSet/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$baseCodeSet/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$baseCodeSet/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        //dataModel description
        PUT("$source", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        //to modify
        GET("terminologies/$sourceTerminology/path/te%3A%7Ctm%3AMLO:%20modifyLeftOnly", MAP_ARG, true)
        verifyResponse OK, response
        String sourceModifyLeftOnly = responseBody().id
        GET("terminologies/$sourceTerminology/path/te%3A%7Ctm%3AMAD:%20modifyAndDelete", MAP_ARG, true)
        verifyResponse OK, response
        String sourceModifyAndDelete = responseBody().id
        GET("terminologies/$sourceTerminology/path/te%3A%7Ctm%3AMAMRD:%20modifyAndModifyReturningDifference", MAP_ARG, true)
        verifyResponse OK, response
        String sourceModifyAndModifyReturningDifference = responseBody().id

        PUT("terminologies/$sourceTerminology/terms/$sourceModifyLeftOnly", [description: 'Description'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$sourceTerminology/terms/$sourceModifyAndDelete", [description: 'Description'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$sourceTerminology/terms/$sourceModifyAndModifyReturningDifference", [description: 'DescriptionLeft'],
            MAP_ARG, true)
        verifyResponse OK, response

        POST("terminologies/$sourceTerminology/terms/", [code: 'ALO', definition: 'addLeftOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        String addLeftOnly = responseBody().id
        POST("terminologies/$sourceTerminology/terms/", [code: 'AAARD', definition: 'addAndAddReturningDifference', description:
            'DescriptionLeft'], MAP_ARG, true)
        verifyResponse CREATED, response
        String sourceAddAndAddReturningDifference = responseBody().id

        //terms
        PUT("$source", [terms: [
            [id: sourceModifyLeftOnly],
            [id: sourceModifyAndDelete],
            [id: sourceModifyAndModifyReturningDifference],
            [id: addLeftOnly],
            [id: sourceAddAndAddReturningDifference]
        ]])
        verifyResponse OK, response

        //dataModel description
        PUT("$target", [description: 'DescriptionRight'])
        verifyResponse OK, response


        GET("terminologies/$targetTerminology/path/te%3A%7Ctm%3ADAM:%20deleteAndModify", MAP_ARG, true)
        verifyResponse OK, response
        deleteAndModify = responseBody().id
        GET("terminologies/$targetTerminology/path/te%3A%7Ctm%3AMAMRD:%20modifyAndModifyReturningDifference", MAP_ARG, true)
        verifyResponse OK, response
        modifyAndModifyReturningDifference = responseBody().id

        GET("terminologies/$targetTerminology//path/te%3A%7Ctm%3ADLO:%20deleteLeftOnly", MAP_ARG, true)
        verifyResponse OK, response
        deleteLeftOnly = responseBody().id
        GET("terminologies/$targetTerminology/path/te%3A%7Ctm%3AMLO:%20modifyLeftOnly", MAP_ARG, true)
        verifyResponse OK, response
        modifyLeftOnly = responseBody().id

        PUT("terminologies/$targetTerminology/terms/$deleteAndModify", [description: 'Description'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$targetTerminology/terms/$modifyAndModifyReturningDifference", [description: 'DescriptionRight'],
            MAP_ARG, true)
        verifyResponse OK, response

        POST("terminologies/$targetTerminology/terms/", [code: 'AAARD', definition: 'addAndAddReturningDifference', description:
            'DescriptionRight'], MAP_ARG, true)
        verifyResponse CREATED, response
        String addAndAddReturningDifference = responseBody().id

        //terms
        PUT("$target", [terms: [
            [id: deleteAndModify],
            [id: deleteLeftOnly],
            [id: modifyLeftOnly],
            [id: modifyAndModifyReturningDifference],
            [id: addAndAddReturningDifference]
        ]])
        verifyResponse OK, response

        when:
        GET("$source/mergeDiff/$target", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedMergeDiffJson

        when:
        def requestBody = [
            changeNotice: 'Functional Test Merge Change Notice',
            patch       : [
                leftId : target,
                rightId: source,
                label  : "Functional Test Model",
                count  : 7,
                diffs  : [
                    [
                        fieldName: "description",
                        value    : "DescriptionLeft"
                    ],
                    [
                        fieldName: "terms",
                        deleted  : [
                            [
                                id: deleteAndModify,
                            ],
                            [
                                id: deleteLeftOnly,
                            ]
                        ],
                        created  : [
                            [
                                id: addLeftOnly,
                            ],
                            [
                                id: sourceModifyAndDelete,
                            ]
                        ],
                        modified : [
                            [
                                leftId : addAndAddReturningDifference,
                                rightId: sourceAddAndAddReturningDifference,
                            ],
                            [
                                leftId : modifyAndModifyReturningDifference,
                                rightId: sourceModifyAndModifyReturningDifference,
                            ],
                            [
                                leftId : modifyLeftOnly,
                                rightId: sourceModifyLeftOnly,
                            ]
                        ]
                    ]
                ]
            ]
        ]


        PUT("$source/mergeInto/$target", requestBody)

        then:
        verifyResponse OK, response
        responseBody().id == target
        responseBody().description == 'DescriptionLeft'

        when:
        GET("$target/terms")

        then:
        responseBody().items.label as Set == ['AAARD: addAndAddReturningDifference', 'ALO: addLeftOnly', 'MAD: modifyAndDelete',
                                              'MAMRD: modifyAndModifyReturningDifference', 'MLO: modifyLeftOnly'] as Set
        responseBody().items.find { dataClass -> dataClass.label == 'MAD: modifyAndDelete' }.description == 'Description'
        responseBody().items.find { dataClass -> dataClass.label == 'AAARD: addAndAddReturningDifference' }.description == 'DescriptionLeft'
        responseBody().items.find { dataClass -> dataClass.label == 'MAMRD: modifyAndModifyReturningDifference' }.description ==
        'DescriptionLeft'
        responseBody().items.find { dataClass -> dataClass.label == 'MLO: modifyLeftOnly' }.description == 'Description'

        when: 'List edits for the Target CodeSet'
        GET("$target/edits", MAP_ARG)

        then: 'The response is OK'
        verifyResponse OK, response

        and: 'There is a CHANGENOTICE edit'
        response.body().items.find {
            it.title == "CHANGENOTICE" && it.description == "Functional Test Merge Change Notice"
        }

        cleanup:
        cleanUpData(source)
        cleanUpData(target)

        DELETE("terminologies/$baseTerminology?permanent=true", MAP_ARG, true)
        response.status() == NO_CONTENT
        DELETE("terminologies/$sourceTerminology?permanent=true", MAP_ARG, true)
        response.status() == NO_CONTENT
        DELETE("terminologies/$targetTerminology?permanent=true", MAP_ARG, true)
        response.status() == NO_CONTENT

        cleanUpData(baseCodeSet)
    }

    void 'cd  folder from CodeSet context'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/folder/${movingFolderId}", [:])

        then:
        verifyResponse OK, response

        when:
        GET("folders/$movingFolderId/codeSets", MAP_ARG, true)

        then:
        response.body().count == 1
        response.body().items[0].id == id

        when:
        GET("folders/$folderId/codeSets", MAP_ARG, true)

        then:
        response.body().count == 0

        cleanup:
        cleanUpData(id)
    }

    void 'test changing folder from Folder context'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        response = PUT("folders/${movingFolderId}/codeSets/$id", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        when:
        GET("folders/$movingFolderId/codeSets", MAP_ARG, true)

        then:
        response.body().count == 1
        response.body().items[0].id == id

        when:
        GET("folders/$folderId/codeSets", MAP_ARG, true)

        then:
        response.body().count == 0

        cleanup:
        cleanUpData(id)
    }

    void 'test diffing 2 CodeSets'() {
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

    @PendingFeature(reason = 'no importers/exporters')
    void 'test export a single CodeSet'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
  "terminology": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test Model",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false
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

    @PendingFeature(reason = 'no importers/exporters')
    void 'test export multiple CodeSets (json only exports first id)'() {
        given:
        String id = createNewItem(validJson)
        String id2 = createNewItem([label: 'Functional Test Model 2'])

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0',
             [codeSetIds: [id, id2]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
  "terminology": {
    "id": "${json-unit.matches:id}",
    "label": "Functional Test Model",
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "type": "Data Standard",
    "documentationVersion": "1.0.0",
    "finalised": false
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

    @PendingFeature(reason = 'no importers/exporters')
    void 'test import basic CodeSet'() {
        given:
        String id = createNewItem(validJson)

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/CodeSetJsonImporterService/2.0', [
            finalised                      : false,
            terminologyName                : 'Functional Test Import',
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
                      "domainType": "CodeSet",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Import",
                      "type": "Data Standard"
                    }
                  ]
                }'''

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'no importers/exporters')
    void 'test import basic CodeSet as new documentation version'() {
        given:
        String id = createNewItem([
            label    : 'Functional Test Model',
            finalised: true
        ])

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/CodeSetJsonImporterService/2.0', [
            finalised                      : true,
            terminologyName                : 'Functional Test Model',
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
                      "domainType": "CodeSet",
                      "id": "${json-unit.matches:id}",
                      "label": "Functional Test Model",
                      "type": "Data Standard"
                    }
                  ]
                }'''

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'not yet implemented')
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
                      "deleted": true,
                      "domainType": "CodeSet",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
                    },
                    {
                      "deleted": true,
                      "domainType": "CodeSet",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
                    },
                    {
                      "deleted": true,
                      "domainType": "CodeSet",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
                    },
                    {
                      "deleted": true,
                      "domainType": "CodeSet",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
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

    @PendingFeature(reason = 'no importers/exporters')
    void 'test importing simple test CodeSet'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/CodeSetJsonImporterService/2.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleCodeSet').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'no importers/exporters')
    void 'test importing complex test CodeSet'() {
        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/CodeSetJsonImporterService/2.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexCodeSet').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'no importers/exporters')
    void 'test export simple CodeSet'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/CodeSetJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleCodeSet').toList()
            ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('simpleCodeSet')).replaceFirst('"exportedBy": "Admin User",',
                                                                                 '"exportedBy": "Unlogged User",')

        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    @PendingFeature(reason = 'no importers/exporters')
    void 'test export complex CodeSet'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer/CodeSetJsonImporterService/2.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexCodeSet').toList()
            ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('complexCodeSet')).replaceFirst('"exportedBy": "Admin User",',
                                                                                  '"exportedBy": "Unlogged User",')

        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter/JsonExporterService/2.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    void 'EX01: test getting CodeSet exporters'() {
        when:
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
            {
                "name": "CodeSetJsonExporterService",
                "version": "3.0",
                "displayName": "JSON CodeSet Exporter",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [],
                "providerType": "CodeSetExporter",
                "fileExtension": "json",
                "fileType": "text/json",
                "canExportMultipleDomains": false
            },
            {
                "name": "CodeSetXmlExporterService",
                "version": "3.1",
                "displayName": "XML CodeSet Exporter",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [],
                "providerType": "CodeSetExporter",
                "fileExtension": "xml",
                "fileType": "text/xml",
                "canExportMultipleDomains": false
            }
        ]'''
    }

    void 'IM01: test getting CodeSet importers'() {
        when:
        GET('providers/importers', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
            {
                "name": "CodeSetXmlImporterService",
                "version": "3.0",
                "displayName": "XML CodeSet Importer",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [
      
                ],
                "providerType": "CodeSetImporter",
                "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters",
                "canImportMultipleDomains": false
            },
            {
                "name": "CodeSetJsonImporterService",
                "version": "3.0",
                "displayName": "JSON CodeSet Importer",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [
      
                ],
                "providerType": "CodeSetImporter",
                "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters",
                "canImportMultipleDomains": false
            }
        ]'''
    }

    void 'EX02: test export a single CodeSet'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/3.0", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
            "codeSet": {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Model",
                "lastUpdated": "${json-unit.matches:offsetDateTime}",
                "documentationVersion": "1.0.0",
                "finalised": false,
                "authority": {
                    "id": "${json-unit.matches:id}",
                    "url": "http://localhost",
                    "label": "Test Authority"
                }
            },
           "exportMetadata": {
                "exportedBy": "Unlogged User",
                "exportedOn": "${json-unit.matches:offsetDateTime}",
                "exporter": {
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                    "name": "CodeSetJsonExporterService",
                    "version": "3.0"
                }
            }
        }'''

        cleanup:
        cleanUpData(id)
    }

    void 'IM02: test import single a CodeSet that was just exported'() {
        given:
        String id = createNewItem(validJson)

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/3.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/3.0', [
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
                    "domainType": "CodeSet",
                    "id": "${json-unit.matches:id}",
                    "label": "Functional Test Import",
                    "branchName": "main",
                    "documentationVersion": "1.0.0",
                    "authority": {
                        "id": "${json-unit.matches:id}",
                        "url": "http://localhost",
                        "label": "Test Authority"
                    }
                }
            ]
        }'''

        cleanup:
        cleanUpData(id)
    }

    void 'EX03: test export multiple CodeSets (json only exports first id)'() {
        given:
        String id = createNewItem(validJson)
        String id2 = createNewItem([label: 'Functional Test Model 2'])

        when:
        POST('export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/3.0',
             [codeSetIds: [id, id2]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
            "codeSet": {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Model",
                "lastUpdated": "${json-unit.matches:offsetDateTime}",
                "documentationVersion": "1.0.0",
                "finalised": false,
                "authority": {
                    "id": "${json-unit.matches:id}",
                    "url": "http://localhost",
                    "label": "Test Authority"
                }
            },
            "exportMetadata": {
                "exportedBy": "Unlogged User",
                "exportedOn": "${json-unit.matches:offsetDateTime}",
                "exporter": {
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                    "name": "CodeSetJsonExporterService",
                    "version": "3.0"
                }
            }
        }'''

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'IM03: test import basic CodeSet as new documentation version'() {
        given:
        String id = createNewItem([
            label       : 'Functional Test Model',
            finalised   : true,
            modelVersion: Version.from('1.0.0')
        ])

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/3.0", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/3.0', [
            finalised                      : true,
            terminologyName                : 'Functional Test Model',
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
                    "domainType": "CodeSet",
                    "id": "${json-unit.matches:id}",
                    "label": "Functional Test Model",
                    "branchName": "main",
                    "documentationVersion": "2.0.0",
                    "modelVersion": "1.0.0",
                    "authority": {
                        "id": "${json-unit.matches:id}",
                        "url": "http://localhost",
                        "label": "Test Authority"
                    }
                }
            ]
        }'''

        cleanup:
        cleanUpData(id)
    }

    void 'EX04: test export simple CodeSet'() {
        given:
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/3.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleCodeSet').toList()
            ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('simpleCodeSet')).replaceFirst('"exportedBy": "Admin User",',
                                                                                 '"exportedBy": "Unlogged User",')


        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/3.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    void 'IM04: test importing and exporting a CodeSet with unknown terms'() {

        when: 'importing a codeset which references unknown'
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/3.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('codeSetFunctionalTest').toList()
            ]
        ])

        then:
        verifyResponse BAD_REQUEST, response
    }

    void 'IM05: test importing and exporting a CodeSet with terms'() {
        given: 'The Simple Test Terminology is imported as a pre-requisite'
        POST('terminologies/import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/3.0', [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleTerminologyForCodeSet').toList()
            ]
        ], MAP_ARG, true)
        verifyResponse CREATED, response


        when: 'importing a codeset which references the terms already imported'
        POST('import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/CodeSetJsonImporterService/3.0', [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('codeSetFunctionalTest').toList()
            ]
        ])

        then:
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        String expected = new String(loadTestFile('codeSetFunctionalTest')).replaceFirst('"exportedBy": "Admin User",',
                                                                                         '"exportedBy": "Unlogged User",')


        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/CodeSetJsonExporterService/3.0", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

}