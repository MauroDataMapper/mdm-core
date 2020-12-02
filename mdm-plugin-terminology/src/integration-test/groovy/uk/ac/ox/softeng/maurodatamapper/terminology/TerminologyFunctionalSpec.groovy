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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * <pre>
 * Controller: terminology
 *  |   POST   | /api/folders/${folderId}/terminologies  | Action: save
 *  |   GET    | /api/folders/${folderId}/terminologies  | Action: index
 *
 *  |  DELETE  | /api/terminologies/${id}                | Action: delete
 *  |   PUT    | /api/terminologies/${id}                | Action: update
 *  |   GET    | /api/terminologies/${id}                | Action: show
 *  |   GET    | /api/terminologies                      | Action: index
 *
 *  |   GET    | /api/terminologies/providers/importers  | Action: importerProviders
 *  |   GET    | /api/terminologies/providers/exporters  | Action: exporterProviders

 *  |  DELETE  | /api/terminologies/${terminologyId}/readByAuthenticated  | Action: readByAuthenticated
 *  |   PUT    | /api/terminologies/${terminologyId}/readByAuthenticated  | Action: readByAuthenticated
 *  |  DELETE  | /api/terminologies/${terminologyId}/readByEveryone       | Action: readByEveryone
 *  |   PUT    | /api/terminologies/${terminologyId}/readByEveryone       | Action: readByEveryone
 *
 *  |   PUT    | /api/terminologies/${terminologyId}/newForkModel          | Action: newForkModel
 *  |   PUT    | /api/terminologies/${terminologyId}/newDocumentationVersion  | Action: newDocumentationVersion
 *  |   PUT    | /api/terminologies/${terminologyId}/finalise                 | Action: finalise

 *  |   PUT    | /api/terminologies/${terminologyId}/folder/${folderId}   | Action: changeFolder
 *  |   PUT    | /api/folders/${folderId}/terminologies/${terminologyId}  | Action: changeFolder
 *
 *  |   GET    | /api/terminologies/${terminologyId}/diff/${otherTerminologyId}  | Action: diff
 *
 *  |  DELETE  | /api/terminologies  | Action: deleteAll
 *
 *  |   GET    | /api/terminologies/${terminologyId}/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportModel
 *  |   POST   | /api/terminologies/import/${importerNamespace}/${importerName}/${importerVersion}  | Action: importModels
 *  |   POST   | /api/terminologies/export/${exporterNamespace}/${exporterName}/${exporterVersion}  | Action: exportModels
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyController
 */
@Integration
@Transactional
@Slf4j
class TerminologyFunctionalSpec extends ResourceFunctionalSpec<Terminology> {

    String importerVersion = "3.1"
    String exporterVersion = "3.1"

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
        assert Terminology.count() == 0
        folderId = new Folder(label: 'Functional Test Folder', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id
        assert folderId
        movingFolderId = new Folder(label: 'Functional Test Folder 2', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id
        assert movingFolderId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec TerminologyFunctionalSpec')
        cleanUpResources(Folder, Classifier)
    }

    @Override
    String getResourcePath() {
        'terminologies'
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
  "domainType": "Terminology",
  "label": "Functional Test Model",
  "availableActions": ["delete","show","update"],
  "branchName": "main",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "type": "Terminology",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper"
  }
}'''
    }

    String getExpectedMergeDiffJson() {
        '''{
    "leftId": "${json-unit.matches:id}",
    "rightId": "${json-unit.matches:id}",
    "label": "Functional Test Model",
    "count": 17,
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
                "isMergeConflict": false
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
                                    "finalised": false
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
                                    "finalised": false
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
                            "label": "SALO: secondAddLeftOnly",
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
                    },
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "MLO: modifyLeftOnly",
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
                    }
                ]
            }
        },
        {
            "termRelationshipTypes": {
                "deleted": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "oppositeActionTo",
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
                    }
                ],
                "created": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "sameActionAs",
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
                    }
                ],
                "modified": [
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "inverseOf",
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
                                    "left": null,
                                    "right": "inverseOf(Modified)",
                                    "isMergeConflict": false
                                }
                            }
                        ]
                    }
                ]
            }
        },
        {
            "termRelationships": {
                "deleted": [
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "similarSourceAction",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Functional Test Model",
                                    "domainType": "Terminology",
                                    "finalised": false
                                },
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "MLO: modifyLeftOnly",
                                    "domainType": "Term"
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
                            "label": "similarSourceAction",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Functional Test Model",
                                    "domainType": "Terminology",
                                    "finalised": false
                                },
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "ALO: addLeftOnly",
                                    "domainType": "Term"
                                }
                            ]
                        },
                        "isMergeConflict": false
                    },
                    {
                        "value": {
                            "id": "${json-unit.matches:id}",
                            "label": "sameSourceActionType",
                            "breadcrumbs": [
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "Functional Test Model",
                                    "domainType": "Terminology",
                                    "finalised": false
                                },
                                {
                                    "id": "${json-unit.matches:id}",
                                    "label": "ALO: addLeftOnly",
                                    "domainType": "Term"
                                }
                            ]
                        },
                        "isMergeConflict": false
                    }
                ],
                "modified": [
                    {
                        "leftId": "${json-unit.matches:id}",
                        "rightId": "${json-unit.matches:id}",
                        "label": "sameSourceActionType",
                        "leftBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "Terminology",
                                "finalised": false
                            },
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "SMLO: secondModifyLeftOnly",
                                "domainType": "Term"
                            }
                        ],
                        "rightBreadcrumbs": [
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "Functional Test Model",
                                "domainType": "Terminology",
                                "finalised": false
                            },
                            {
                                "id": "${json-unit.matches:id}",
                                "label": "SMLO: secondModifyLeftOnly",
                                "domainType": "Term"
                            }
                        ],
                        "count": 1,
                        "diffs": [
                            {
                                "description": {
                                    "left": null,
                                    "right": "NewDescription",
                                    "isMergeConflict": false
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

    void 'test finalising Terminology'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/finalise", [versionChangeType: "Major"])

        then:
        verifyResponse OK, response

        and:
        response.body().availableActions == ['delete', 'show', 'update']
        response.body().finalised
        response.body().dateFinalised

        cleanup:
        cleanUpData(id)
    }

    void 'VF01 : test creating a new fork model of a Terminology'() {
        given: 'finalised model is created'
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response

        when: 'adding one new model'
        PUT("$id/newForkModel", [label: 'Functional Test Terminology reader'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
            .replaceFirst(/"label": "Functional Test Model",/, '"label": "Functional Test Terminology reader",')


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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology reader"
      },
      "targetCatalogueItem": {
        "domainType": "Terminology",
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology reader"
      },
      "targetModel": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        when: 'adding another'
        PUT("$id/newForkModel", [label: 'Functional Test Terminology editor'], STRING_ARG)

        then:
        verifyJsonResponse CREATED, getExpectedShowJson()
            .replaceFirst(/"label": "Functional Test Model",/, '"label": "Functional Test Terminology editor",')

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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology reader"
      },
      "targetCatalogueItem": {
        "domainType": "Terminology",
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology editor"
      },
      "targetCatalogueItem": {
        "domainType": "Terminology",
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology reader"
      },
      "targetModel": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    },
     {
      "domainType": "VersionLink",
      "linkType": "New Fork Of",
      "id": "${json-unit.matches:id}",
      "sourceModel": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Terminology editor"
      },
      "targetModel": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      }
    }
  ]
}'''

        cleanup:
        cleanUpData()
    }

    void 'VD01 : test creating a new documentation version of a Terminology'() {
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
      "sourceCatalogueItem": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetCatalogueItem": {
        "domainType": "Terminology",
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "domainType": "Terminology",
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

    void 'VB01 : test creating a new main branch model version of a Terminology'() {
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
      "sourceCatalogueItem": {
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetCatalogueItem": {
        "domainType": "Terminology",
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
        "domainType": "Terminology",
        "id": "${json-unit.matches:id}",
        "label": "Functional Test Model"
      },
      "targetModel": {
        "domainType": "Terminology",
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
        responseBody().errors.first().message == 'Property [branchName] of class [class uk.ac.ox.softeng.maurodatamapper.terminology.Terminology] ' +
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
        PUT("$id/newBranchModelVersion", [branchName: 'main'])
        verifyResponse CREATED, response
        String expectedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$expectedId/finalise", [versionChangeType: "Major"])
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

    void 'VB07 : test finding latest model version of a Model<T> (as editor)'() {
        /*
        id (finalised) -- expectedId (finalised) -- latestDraftId (draft)
          \_ newBranchId (draft)
        */
        given:
        String id = createNewItem(validJson)
        PUT("$id/finalise", [versionChangeType: "Major"])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: 'main'])
        verifyResponse CREATED, response
        String expectedId = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'newBranch'])
        verifyResponse CREATED, response
        String newBranchId = responseBody().id
        PUT("$expectedId/finalise", [versionChangeType: "Major"])
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
        PUT("$id/newBranchModelVersion", [branchName: 'main'])
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
        PUT("$id/newBranchModelVersion", [branchName: 'main'])
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
        given:
        String id = createNewItem(validJson)

        POST("$id/terms", [code: 'DLO', definition: 'deleteLeftOnly'])
        verifyResponse CREATED, response
        POST("$id/terms", [code: 'MLO', definition: 'modifyLeftOnly'])
        verifyResponse CREATED, response
        String modifyLeftOnly = responseBody().id
        POST("$id/terms", [code: 'SMLO', definition: 'secondModifyLeftOnly'])
        verifyResponse CREATED, response
        String secondModifyLeftOnly = responseBody().id
        POST("$id/terms", [code: 'DAM', definition: 'deleteAndModify'])
        verifyResponse CREATED, response
        String deleteAndModify = responseBody().id
        POST("$id/terms", [code: 'MAD', definition: 'modifyAndDelete'])
        verifyResponse CREATED, response
        String modifyAndDelete = responseBody().id
        POST("$id/terms", [code: 'MAMRD', definition: 'modifyAndModifyReturningDifference'])
        verifyResponse CREATED, response
        String modifyAndModifyReturningDifference = responseBody().id

        POST("$id/termRelationshipTypes", [label: 'inverseOf'])
        verifyResponse CREATED, response
        String inverseOf = responseBody().id
        POST("$id/termRelationshipTypes", [label: 'oppositeActionTo'])
        verifyResponse CREATED, response
        POST("$id/termRelationshipTypes", [label: 'similarSourceAction'])
        verifyResponse CREATED, response
        String similarSourceAction = responseBody().id
        POST("$id/termRelationshipTypes", [label: 'sameSourceActionType'])
        verifyResponse CREATED, response
        String sameSourceActionType = responseBody().id

        POST("$id/terms/$deleteAndModify/termRelationships", [
            targetTerm      : modifyAndDelete,
            relationshipType: inverseOf,
            sourceTerm      : deleteAndModify
        ])
        verifyResponse CREATED, response
        POST("$id/terms/$modifyLeftOnly/termRelationships", [
            targetTerm      : modifyAndModifyReturningDifference,
            relationshipType: similarSourceAction,
            sourceTerm      : modifyLeftOnly
        ])
        verifyResponse CREATED, response
        POST("$id/terms/$secondModifyLeftOnly/termRelationships", [
            targetTerm      : modifyLeftOnly,
            relationshipType: sameSourceActionType,
            sourceTerm      : secondModifyLeftOnly
        ])
        verifyResponse CREATED, response

        //        POST("$id/metadata", [namespace: 'functional.test.namespace', key: 'deleteMetadataSource', value: 'original'])
        //        verifyResponse CREATED, response
        //        POST("$id/metadata", [namespace: 'functional.test.namespace', key: 'modifyMetadataSource', value: 'original'])
        //        verifyResponse CREATED, response

        PUT("$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("$id/newBranchModelVersion", [branchName: 'main'])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("$id/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        when:
        //to delete
        GET("$source/path/te%3A%7Ctm%3ADLO:%20deleteLeftOnly")
        verifyResponse OK, response
        String deleteLeftOnly = responseBody().id
        GET("$source/path/te%3A%7Ctm%3ADAM:%20deleteAndModify")
        verifyResponse OK, response
        deleteAndModify = responseBody().id
        //to modify
        GET("$source/path/te%3A%7Ctm%3AMLO:%20modifyLeftOnly")
        verifyResponse OK, response
        modifyLeftOnly = responseBody().id
        GET("$source/path/te%3A%7Ctm%3ASMLO:%20secondModifyLeftOnly")
        verifyResponse OK, response
        secondModifyLeftOnly = responseBody().id
        GET("$source/path/te%3A%7Ctm%3AMAD:%20modifyAndDelete")
        verifyResponse OK, response
        String sourceModifyAndDelete = responseBody().id
        GET("$source/path/te%3A%7Ctm%3AMAMRD:%20modifyAndModifyReturningDifference")
        verifyResponse OK, response
        modifyAndModifyReturningDifference = responseBody().id

        GET("$source/termRelationshipTypes")
        verifyResponse OK, response
        String oppositeActionTo = responseBody().items.find { it.label == 'oppositeActionTo' }.id
        inverseOf = responseBody().items.find { it.label == 'inverseOf' }.id
        sameSourceActionType = responseBody().items.find { it.label == 'sameSourceActionType' }.id

        GET("$source/terms/$modifyLeftOnly/termRelationships")
        verifyResponse OK, response
        String similarSourceActionOnModifyLeftOnly = responseBody().items.find { it.label == 'similarSourceAction' }.id
        String sameSourceActionTypeOnSecondModifyLeftOnly = responseBody().items.find { it.label == 'sameSourceActionType' }.id

        //        GET("$source/metadata")
        //        verifyResponse OK, response
        //        String deleteMetadataSource = responseBody().items.find { it.key == 'deleteMetadataSource' }.id
        //        String modifyMetadataSource = responseBody().items.find { it.key == 'modifyMetadataSource' }.id

        then:
        //dataModel description
        PUT("$source", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        //terms
        DELETE("$source/terms/$deleteLeftOnly")
        verifyResponse NO_CONTENT, response
        DELETE("$source/terms/$deleteAndModify")
        verifyResponse NO_CONTENT, response

        PUT("$source/terms/$modifyLeftOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("$source/terms/$sourceModifyAndDelete", [description: 'Description'])
        verifyResponse OK, response
        PUT("$source/terms/$modifyAndModifyReturningDifference", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("$source/terms", [code: 'ALO', definition: 'addLeftOnly'])
        verifyResponse CREATED, response
        String addLeftOnly = responseBody().id
        POST("$source/terms", [code: 'SALO', definition: 'secondAddLeftOnly'])
        verifyResponse CREATED, response
        String secondAddLeftOnly = responseBody().id
        POST("$source/terms", [code: 'AAARD', definition: 'addAndAddReturningDifference', description: 'DescriptionLeft'])
        verifyResponse CREATED, response
        String sourceAddAndAddReturningDifference = responseBody().id

        //termRelationshipTypes
        DELETE("$source/termRelationshipTypes/$oppositeActionTo")
        verifyResponse NO_CONTENT, response

        PUT("$source/termRelationshipTypes/$inverseOf", [description: 'inverseOf(Modified)'])
        verifyResponse OK, response

        POST("$source/termRelationshipTypes", [label: 'sameActionAs'])
        verifyResponse CREATED, response

        //termRelationships
        DELETE("$source/terms/$modifyLeftOnly/termRelationships/$similarSourceActionOnModifyLeftOnly")
        verifyResponse NO_CONTENT, response

        PUT("$source/terms/$secondModifyLeftOnly/termRelationships/$sameSourceActionTypeOnSecondModifyLeftOnly", [description: 'NewDescription'])
        verifyResponse OK, response

        POST("$source/terms/$addLeftOnly/termRelationships", [
            targetTerm      : sourceAddAndAddReturningDifference,
            relationshipType: similarSourceAction,
            sourceTerm      : addLeftOnly
        ])
        verifyResponse CREATED, response
        String similarSourceActionOnAddLeftOnly = responseBody().id

        POST("$source/terms/$addLeftOnly/termRelationships", [
            targetTerm      : secondAddLeftOnly,
            relationshipType: sameSourceActionType,
            sourceTerm      : addLeftOnly
        ])
        verifyResponse CREATED, response
        String sameSourceActionTypeOnAddLeftOnly = responseBody().id

        //metadata
        //        DELETE("$source/metadata/$deleteMetadataSource")
        //        verifyResponse NO_CONTENT, response
        //
        //        PUT("$source/metadata/$modifyMetadataSource", [value: 'Modified Description'])
        //        verifyResponse OK, response
        //
        //        POST("$source/metadata", [namespace: 'functional.test.namespace', key: 'addMetadataSource', value: 'original'])
        //        verifyResponse CREATED, response
        //        String addMetadataSource = responseBody().id

        when:
        // for mergeInto json
        GET("$target/path/te%3A%7Ctm%3AMAD:%20modifyAndDelete")
        verifyResponse OK, response
        String targetModifyAndDelete = responseBody().id

        GET("$target/path/te%3A%7Ctm%3ADAM:%20deleteAndModify")
        verifyResponse OK, response
        deleteAndModify = responseBody().id
        GET("$target/path/te%3A%7Ctm%3AMAMRD:%20modifyAndModifyReturningDifference")
        verifyResponse OK, response
        modifyAndModifyReturningDifference = responseBody().id

        then:
        //dataModel description
        PUT("$target", [description: 'DescriptionRight'])
        verifyResponse OK, response

        //terms
        DELETE("$target/terms/$targetModifyAndDelete")
        verifyResponse NO_CONTENT, response

        PUT("$target/terms/$deleteAndModify", [description: 'Description'])
        verifyResponse OK, response
        PUT("$target/terms/$modifyAndModifyReturningDifference", [description: 'DescriptionRight'])
        verifyResponse OK, response

        POST("$target/terms", [code: 'AAARD', definition: 'addAndAddReturningDifference', description: 'DescriptionRight'])
        verifyResponse CREATED, response
        String addAndAddReturningDifference = responseBody().id

        when:
        // for mergeInto json
        GET("$target/path/te%3A%7Ctm%3ADLO:%20deleteLeftOnly")
        verifyResponse OK, response
        deleteLeftOnly = responseBody().id
        GET("$target/path/te%3A%7Ctm%3AMLO:%20modifyLeftOnly")
        verifyResponse OK, response
        modifyLeftOnly = responseBody().id
        GET("$target/path/te%3A%7Ctm%3ASMLO:%20secondModifyLeftOnly")
        verifyResponse OK, response
        secondModifyLeftOnly = responseBody().id

        GET("$target/termRelationshipTypes")
        verifyResponse OK, response
        oppositeActionTo = responseBody().items.find { it.label == 'oppositeActionTo' }.id
        inverseOf = responseBody().items.find { it.label == 'inverseOf' }.id

        GET("$source/termRelationshipTypes")
        verifyResponse OK, response
        String sameActionAs = responseBody().items.find { it.label == 'sameActionAs' }.id

        GET("$target/terms/$modifyLeftOnly/termRelationships")
        verifyResponse OK, response
        similarSourceActionOnModifyLeftOnly = responseBody().items.find { it.label == 'similarSourceAction' }.id
        sameSourceActionTypeOnSecondModifyLeftOnly = responseBody().items.find { it.label == 'sameSourceActionType' }.id

        GET("$source/mergeDiff/$target", STRING_ARG)

        then:
        verifyJsonResponse OK, expectedMergeDiffJson

        when:
        String modifiedDescriptionSource = 'modifiedDescriptionSource'

        def requestBody = [
            patch: [
                leftId : target,
                rightId: source,
                label  : "Functional Test Model",
                diffs  : [
                    [
                        fieldName: "description",
                        value    : modifiedDescriptionSource
                    ],
                    [
                        fieldName: "terms",
                        deleted  : [
                            [
                                id   : deleteAndModify,
                                label: "DAM: deleteAndModify"
                            ],
                            [
                                id   : deleteLeftOnly,
                                label: "DLO: deleteLeftOnly"
                            ]
                        ],
                        created  : [
                            [
                                id   : addLeftOnly,
                                label: "ALO: addLeftOnly"
                            ],
                            [
                                id   : sourceModifyAndDelete,
                                label: "MAD: modifyAndDelete"
                            ],
                            [
                                id   : secondAddLeftOnly,
                                label: "SALO: secondAddLeftOnly"
                            ]
                        ],
                        modified : [
                            [
                                leftId: addAndAddReturningDifference,
                                label : "AAARD: addAndAddReturningDifference",
                                count : 1,
                                diffs : [
                                    [
                                        fieldName: "description",
                                        value    : "addedDescriptionSource"
                                    ]
                                ]
                            ],
                            [
                                leftId: modifyAndModifyReturningDifference,
                                label : "modifyAndModifyReturningDifference",
                                count : 1,
                                diffs : [
                                    [
                                        fieldName: "description",
                                        value    : modifiedDescriptionSource
                                    ]
                                ]
                            ],
                            [
                                leftId: modifyLeftOnly,
                                label : "modifyLeftOnly",
                                diffs : [
                                    [
                                        fieldName: "description",
                                        value    : "modifiedDescriptionSourceOnly"
                                    ]
                                ]
                            ]
                        ]
                    ],
                    [
                        fieldName: "termRelationshipTypes",
                        deleted  : [
                            [
                                id   : oppositeActionTo,
                                label: "oppositeActionTo"
                            ]
                        ],
                        created  : [
                            [
                                id   : sameActionAs,
                                label: "sameActionAs"
                            ]
                        ],
                        modified : [
                            [
                                leftId: inverseOf,
                                label : "inverseOf",
                                diffs : [
                                    [
                                        fieldName: "description",
                                        value    : "inverseOf(Modified)"
                                    ]
                                ]
                            ]
                        ]
                    ],
                    [
                        fieldName: "termRelationships",
                        deleted  : [
                            [
                                id   : similarSourceActionOnModifyLeftOnly,
                                label: "similarSourceAction"
                            ]
                        ],
                        created  : [
                            [
                                id   : similarSourceActionOnAddLeftOnly,
                                label: "similarSourceAction"
                            ],
                            [
                                id   : sameSourceActionTypeOnAddLeftOnly,
                                label: "sameSourceActionType"
                            ]
                        ],
                        modified : [
                            [
                                leftId: sameSourceActionTypeOnSecondModifyLeftOnly,
                                label : "sameSourceActionType",
                                diffs : [
                                    fieldName: "description",
                                    value    : "NewDescription"
                                ]
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
        responseBody().description == modifiedDescriptionSource

        when:
        GET("$target/terms")

        then:
        verifyResponse OK, response
        responseBody().items.label as Set == ['AAARD: addAndAddReturningDifference', 'ALO: addLeftOnly', 'SALO: secondAddLeftOnly',
                                              'MAD: modifyAndDelete', 'MAMRD: modifyAndModifyReturningDifference', 'MLO: modifyLeftOnly',
                                              'SMLO: secondModifyLeftOnly'] as Set
        responseBody().items.find { term -> term.label == 'MAD: modifyAndDelete' }.description == 'Description'
        responseBody().items.find { term -> term.label == 'AAARD: addAndAddReturningDifference' }.description == 'addedDescriptionSource'
        responseBody().items.find { term -> term.label == 'MAMRD: modifyAndModifyReturningDifference' }.description == modifiedDescriptionSource
        responseBody().items.find { term -> term.label == 'MLO: modifyLeftOnly' }.description == 'modifiedDescriptionSourceOnly'

        when:
        addLeftOnly = responseBody().items.find { it.label == 'ALO: addLeftOnly' }.id
        secondAddLeftOnly = responseBody().items.find { it.label == 'SALO: secondAddLeftOnly' }.id
        GET("$target/termRelationshipTypes")

        then:
        verifyResponse OK, response
        responseBody().items.label as Set == ['inverseOf', 'sameSourceActionType', 'similarSourceAction', 'sameActionAs'] as Set
        responseBody().items.find { term -> term.label == 'inverseOf' }.description == 'inverseOf(Modified)'

        when:
        GET("$target/terms/$modifyLeftOnly/termRelationships")

        then:
        verifyResponse OK, response
        responseBody().items.size == 1
        responseBody().items.label as Set == ['sameSourceActionType'] as Set

        when:
        sameSourceActionType = responseBody().items.find { it.label == 'sameSourceActionType' }.id
        GET("$target/terms/$modifyLeftOnly/termRelationships/$sameSourceActionType")

        then:
        verifyResponse OK, response
        responseBody().sourceTerm.id == secondModifyLeftOnly
        responseBody().targetTerm.id == modifyLeftOnly

        when:
        GET("$target/terms/$addLeftOnly/termRelationships")

        then:
        verifyResponse OK, response
        responseBody().items.size == 2
        responseBody().items.label as Set == ['similarSourceAction', 'sameSourceActionType'] as Set

        when:
        sameSourceActionType = responseBody().items.find { it.label == 'sameSourceActionType' }.id
        similarSourceAction = responseBody().items.find { it.label == 'similarSourceAction' }.id
        GET("$target/terms/$addLeftOnly/termRelationships/$sameSourceActionType")

        then:
        verifyResponse OK, response
        responseBody().sourceTerm.id == addLeftOnly
        responseBody().targetTerm.id == secondAddLeftOnly

        when:
        GET("$target/terms/$addLeftOnly/termRelationships/$similarSourceAction")

        then:
        verifyResponse OK, response
        responseBody().sourceTerm.id == addLeftOnly
        responseBody().targetTerm.id == addAndAddReturningDifference

        cleanup:
        cleanUpData(source)
        cleanUpData(target)
        cleanUpData(id)
    }

    void 'test changing folder from Terminology context'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        PUT("$id/folder/${movingFolderId}", [:])

        then:
        verifyResponse OK, response

        when:
        GET("folders/$movingFolderId/terminologies", MAP_ARG, true)

        then:
        response.body().count == 1
        response.body().items[0].id == id

        when:
        GET("folders/$folderId/terminologies", MAP_ARG, true)

        then:
        response.body().count == 0

        cleanup:
        cleanUpData(id)
    }

    void 'test changing folder from Folder context'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem(validJson)

        when:
        response = PUT("folders/${movingFolderId}/terminologies/$id", [:], MAP_ARG, true)

        then:
        verifyResponse OK, response

        when:
        GET("folders/$movingFolderId/terminologies", MAP_ARG, true)

        then:
        response.body().count == 1
        response.body().items[0].id == id

        when:
        GET("folders/$folderId/terminologies", MAP_ARG, true)

        then:
        response.body().count == 0

        cleanup:
        cleanUpData(id)
    }

    void 'test diffing 2 Terminologys'() {
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
                      "domainType": "Terminology",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
                    },
                    {
                      "deleted": true,
                      "domainType": "Terminology",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
                    },
                    {
                      "deleted": true,
                      "domainType": "Terminology",
                      "id": "${json-unit.matches:id}",
                      "label": "${json-unit.matches:id}",
                      "type": "Data Standard"
                    },
                    {
                      "deleted": true,
                      "domainType": "Terminology",
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

    void 'EX01: test getting Terminology exporters'() {
        when:
        GET('providers/exporters', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
            {
                "name": "TerminologyJsonExporterService",
                "version": "${json-unit.matches:version}",
                "displayName": "JSON Terminology Exporter",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [],
                "providerType": "TerminologyExporter",
                "fileExtension": "json",
                "fileType": "text/json",
                "canExportMultipleDomains": false
            },
            {
                "name": "TerminologyXmlExporterService",
                "version": "${json-unit.matches:version}",
                "displayName": "XML Terminology Exporter",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [],
                "providerType": "TerminologyExporter",
                "fileExtension": "xml",
                "fileType": "text/xml",
                "canExportMultipleDomains": false
            }
        ]'''
    }

    void 'IM01: test getting Terminology importers'() {
        when:
        GET('providers/importers', STRING_ARG)

        then:
        verifyJsonResponse OK, '''[
            {
                "name": "TerminologyXmlImporterService",
                "version": "${json-unit.matches:version}",
                "displayName": "XML Terminology Importer",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [
      
                ],
                "providerType": "TerminologyImporter",
                "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters",
                "canImportMultipleDomains": false
            },
            {
                "name": "TerminologyJsonImporterService",
                "version": "${json-unit.matches:version}",
                "displayName": "JSON Terminology Importer",
                "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
                "allowsExtraMetadataKeys": true,
                "knownMetadataKeys": [
      
                ],
                "providerType": "TerminologyImporter",
                "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters",
                "canImportMultipleDomains": false
            }
        ]'''
    }

    void 'EX02: test export a single Terminology'() {
        given:
        String id = createNewItem(validJson)

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/${exporterVersion}", STRING_ARG)

        then:
        verifyJsonResponse OK, '''{
            "terminology": {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Model",
                "lastUpdated": "${json-unit.matches:offsetDateTime}",
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
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                    "name": "TerminologyJsonExporterService",
                    "version": "${json-unit.matches:version}"
                }
            }
        }'''

        cleanup:
        cleanUpData(id)
    }

    void 'IM02: test import single a Terminology that was just exported'() {
        given:
        String id = createNewItem(validJson)

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/${exporterVersion}", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST("import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/${importerVersion}", [
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
                    "domainType": "Terminology",
                    "id": "${json-unit.matches:id}",
                    "label": "Functional Test Import",
                    "branchName": "main",
                    "documentationVersion": "1.0.0",
                    "authority": {
                        "id": "${json-unit.matches:id}",
                        "url": "http://localhost",
                        "label": "Mauro Data Mapper"
                    }
                }
            ]
        }'''

        cleanup:
        cleanUpData(id)
    }

    void 'EX03: test export multiple Terminologies (json only exports first id)'() {
        given:
        String id = createNewItem(validJson)
        String id2 = createNewItem([label: 'Functional Test Model 2'])

        when:
        POST("export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/${exporterVersion}",
             [terminologyIds: [id, id2]], STRING_ARG
        )

        then:
        verifyJsonResponse OK, '''{
            "terminology": {
                "id": "${json-unit.matches:id}",
                "label": "Functional Test Model",
                "lastUpdated": "${json-unit.matches:offsetDateTime}",
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
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
                    "name": "TerminologyJsonExporterService",
                    "version": "${json-unit.matches:version}"
                }
            }
        }'''

        cleanup:
        cleanUpData(id)
        cleanUpData(id2)
    }

    void 'IM03: test import basic Terminology as new documentation version'() {
        given:
        String id = createNewItem([
            label       : 'Functional Test Model',
            finalised   : true,
            modelVersion: Version.from('1.0.0')
        ])

        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/3.1", STRING_ARG)
        verifyResponse OK, jsonCapableResponse
        String exportedJsonString = jsonCapableResponse.body()

        expect:
        exportedJsonString

        when:
        POST("import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/${importerVersion}", [
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
                    "domainType": "Terminology",
                    "id": "${json-unit.matches:id}",
                    "label": "Functional Test Model",
                    "branchName": "main",
                    "documentationVersion": "2.0.0",
                    "modelVersion": "1.0.0",
                    "authority": {
                        "id": "${json-unit.matches:id}",
                        "url": "http://localhost",
                        "label": "Mauro Data Mapper"
                    }
                }
            ]
        }'''

        cleanup:
        cleanUpData(id)
    }


    void 'EX04: test export simple Terminology'() {
        given:
        POST("import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/${exporterVersion}", [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleTerminology').toList()
            ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('simpleTerminology')).replaceFirst('"exportedBy": "Admin User",',
                                                                                     '"exportedBy": "Unlogged User",')
            .replace(/Test Authority/, 'Mauro Data Mapper')


        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/${importerVersion}", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }


    void 'IM04: test importing simple test Terminology'() {
        when:
        POST("import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/${importerVersion}", [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('simpleTerminology').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }

    void 'EX05: test export complex Terminology'() {
        given:
        POST("import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/${importerVersion}", [
            finalised                      : false,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexTerminology').toList()
            ]
        ])

        verifyResponse CREATED, response
        def id = response.body().items[0].id
        String expected = new String(loadTestFile('complexTerminology')).replaceFirst('"exportedBy": "Admin User",',
                                                                                      '"exportedBy": "Unlogged User",')
            .replace(/Test Authority/, 'Mauro Data Mapper')

        expect:
        id

        when:
        GET("${id}/export/uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter/TerminologyJsonExporterService/${exporterVersion}", STRING_ARG)

        then:
        verifyJsonResponse OK, expected

        cleanup:
        cleanUpData(id)
    }

    void 'IM05: test importing complex test Terminology'() {
        when:
        POST("import/uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer/TerminologyJsonImporterService/${importerVersion}", [
            finalised                      : true,
            folderId                       : folderId.toString(),
            importAsNewDocumentationVersion: false,
            importFile                     : [
                fileName    : 'FT Import',
                fileType    : MimeType.JSON_API.name,
                fileContents: loadTestFile('complexTerminology').toList()
            ]
        ])
        verifyResponse CREATED, response
        def id = response.body().items[0].id

        then:
        id

        cleanup:
        cleanUpData(id)
    }
}