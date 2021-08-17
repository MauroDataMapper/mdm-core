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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.profile


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.profile.PostFinalisedEditableProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileSpecificationProfileService
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
@Integration
class ProfileFunctionalSpec extends FunctionalSpec {

    ProfileSpecificationProfileService profileSpecificationProfileService
    PostFinalisedEditableProfileService postFinalisedEditableProfileService

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Override
    String getResourcePath() {
        ''
    }

    String getDataModelId() {
        loginEditor()
        POST("folders/${getTestFolderId()}/dataModels", [label: "profile functional model"])
        verifyResponse(CREATED, response)
        String id = responseBody().id
        logout()
        id
    }

    void finaliseDataModelId(String id) {
        loginEditor()
        PUT("dataModels/$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        logout()
    }

    void cleanupDataModelId(String id) {
        loginEditor()
        DELETE("dataModels/$id?permanent=true")
        verifyResponse(NO_CONTENT, response)
        logout()
    }

    void 'test getting profile providers'() {
        when:
        GET('profiles/providers', STRING_ARG)

        then:
        verifyJsonResponse OK, '''
[
  {
    "name": "PostFinalisedEditableProfileService",
    "version": "${json-unit.matches:version}",
    "displayName": "Post Finalised Editable Profile",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      "metadataNamespace",
      "domainsApplicable"
    ],
    "providerType": "Profile",
    "metadataNamespace": "uk.ac.ox.softeng.maurodatamapper.profile.editable",
    "domains": [
      "DataModel"
    ]
  },
  {
    "name": "ProfileSpecificationProfileService",
    "version": "${json-unit.matches:version}",
    "displayName": "Profile Specification Profile (Data Model)",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      "metadataNamespace",
      "domainsApplicable"
    ],
    "providerType": "Profile",
    "metadataNamespace": "uk.ac.ox.softeng.maurodatamapper.profile",
    "domains": [
      "DataModel"
    ]
  },
  {
    "name": "ProfileSpecificationFieldProfileService",
    "version": "${json-unit.matches:version}",
    "displayName": "Profile Specification Profile (Data Element)",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      "metadataPropertyName",
      "defaultValue",
      "regularExpression",
      "editedAfterFinalisation"
    ],
    "providerType": "Profile",
    "metadataNamespace": "uk.ac.ox.softeng.maurodatamapper.profile.dataelement",
    "domains": [
      "DataElement"
    ]
  }
]'''
    }

    void 'N01 : test validating profile on DataModel (as reader)'() {
        given:
        String id = getDataModelId()
        Map namespaceFieldMap = [
            fieldName: 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            fieldName: 'Applicable for domains',
        ]
        Map profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]

        when:
        loginReader()
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}/validate", profileMap)

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().total == 1
        responseBody().errors.first().message == 'Value cannot be null'
        responseBody().errors.first().fieldName == 'Metadata namespace'
        responseBody().errors.first().metadataPropertyName == 'metadataNamespace'

        when:
        namespaceFieldMap.currentValue = 'functional.test.profile'
        domainsFieldMap.currentValue = 'DataModel'

        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}/validate", profileMap)

        then:
        verifyResponse(OK, response)

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N02 : test saving profile (as editor)'() {
        given:
        String id = getDataModelId()
        Map namespaceFieldMap = [
            currentValue: 'functional.test.profile',
            fieldName   : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            currentValue: 'DataModel',
            fieldName   : 'Applicable for domains',
        ]
        Map profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]

        when:
        loginEditor()
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", profileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == namespaceFieldMap.fieldName}.currentValue == namespaceFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == domainsFieldMap.fieldName}.currentValue == domainsFieldMap.currentValue

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${id}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().first().name == profileSpecificationProfileService.name
        localResponse.body().first().namespace == profileSpecificationProfileService.namespace

        when:
        GET("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", STRING_ARG)

        then:
        verifyJsonResponse(OK, getExpectedSavedProfile())

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N03 : test editing profile (as editor)'() {
        given:
        String id = getDataModelId()
        Map namespaceFieldMap = [
            currentValue: 'functional.test.profile',
            fieldName   : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            currentValue: 'DataModel',
            fieldName   : 'Applicable for domains',
        ]
        Map profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        loginEditor()
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", profileMap)
        verifyResponse(OK, response)


        when:
        namespaceFieldMap.currentValue = 'functional.test.profile.adjusted'
        profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", profileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == namespaceFieldMap.fieldName}.currentValue == 'functional.test.profile.adjusted'
        responseBody().sections.first().fields.find {it.fieldName == domainsFieldMap.fieldName}.currentValue == domainsFieldMap.currentValue

        when:
        domainsFieldMap.currentValue = ''
        profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        domainsFieldMap,
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", profileMap)
        verifyResponse(OK, response)

        then:
        responseBody().sections.first().fields.find {it.fieldName == namespaceFieldMap.fieldName}.currentValue == 'functional.test.profile.adjusted'
        responseBody().sections.first().fields.find {it.fieldName == domainsFieldMap.fieldName}.currentValue == ''

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N04 : test deleting profile (as editor)'() {
        given:
        String id = getDataModelId()
        Map namespaceFieldMap = [
            currentValue: 'functional.test.profile',
            fieldName   : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            currentValue: 'DataModel',
            fieldName   : 'Applicable for domains',
        ]
        Map profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        loginEditor()
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", profileMap)
        verifyResponse(OK, response)


        when:
        DELETE("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}")

        then:
        verifyResponse(NO_CONTENT, response)

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${id}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().isEmpty()

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N05 : test saving non-editable profile on finalised model (as editor)'() {
        given:
        String id = getDataModelId()
        finaliseDataModelId(id)

        Map namespaceFieldMap = [
            currentValue: 'functional.test.profile',
            fieldName   : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            currentValue: 'DataModel',
            fieldName   : 'Applicable for domains',
        ]
        Map profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]

        when:
        loginEditor()
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", profileMap)

        then:
        verifyForbidden(response)

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N06 : test editing non-editable profile on finalised model'() {
        given:
        String id = getDataModelId()

        Map namespaceFieldMap = [
            currentValue: 'functional.test.profile',
            fieldName   : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            currentValue: 'DataModel',
            fieldName   : 'Applicable for domains',
        ]
        Map profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        loginEditor()
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", profileMap)
        verifyResponse(OK, response)
        finaliseDataModelId(id)

        when:
        namespaceFieldMap.currentValue = 'functional.test.profile.adjusted'
        profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        loginEditor()
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", profileMap)

        then:
        verifyForbidden(response)

        when:
        domainsFieldMap.currentValue = ''
        profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        domainsFieldMap,
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", profileMap)


        then:
        verifyForbidden(response)

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N07 : test saving editable profile on finalised model (as editor)'() {
        given:
        String id = getDataModelId()
        finaliseDataModelId(id)

        Map namespaceFieldMap = [
            currentValue: 'functional.test.profile',
            fieldName   : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            currentValue: 'DataModel',
            fieldName   : 'Applicable for domains',
        ]
        Map profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : postFinalisedEditableProfileService.namespace,
            name      : postFinalisedEditableProfileService.name

        ]

        when:
        loginEditor()
        POST("profiles/${postFinalisedEditableProfileService.namespace}/${postFinalisedEditableProfileService.name}/dataModels/${id}", profileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == namespaceFieldMap.fieldName}.currentValue == namespaceFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == domainsFieldMap.fieldName}.currentValue == domainsFieldMap.currentValue

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${id}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().first().name == postFinalisedEditableProfileService.name
        localResponse.body().first().namespace == postFinalisedEditableProfileService.namespace

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N08 : test editing editable profile on finalised model'() {
        given:
        String id = getDataModelId()

        Map namespaceFieldMap = [
            currentValue: 'functional.test.profile',
            fieldName   : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            currentValue: 'DataModel',
            fieldName   : 'Applicable for domains',
        ]
        Map profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : postFinalisedEditableProfileService.namespace,
            name      : postFinalisedEditableProfileService.name

        ]
        loginEditor()
        POST("profiles/${postFinalisedEditableProfileService.namespace}/${postFinalisedEditableProfileService.name}/dataModels/${id}", profileMap)
        verifyResponse(OK, response)
        finaliseDataModelId(id)

        when:
        namespaceFieldMap.currentValue = 'functional.test.profile.adjusted'
        profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : postFinalisedEditableProfileService.namespace,
            name      : postFinalisedEditableProfileService.name

        ]
        loginEditor()
        POST("profiles/${postFinalisedEditableProfileService.namespace}/${postFinalisedEditableProfileService.name}/dataModels/${id}", profileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == namespaceFieldMap.fieldName}.currentValue == 'functional.test.profile.adjusted'
        responseBody().sections.first().fields.find {it.fieldName == domainsFieldMap.fieldName}.currentValue == domainsFieldMap.currentValue

        when:
        domainsFieldMap.currentValue = ''
        profileMap = [
            sections  : [
                [
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        domainsFieldMap,
                    ],
                    sectionName       : 'Profile Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : postFinalisedEditableProfileService.namespace,
            name      : postFinalisedEditableProfileService.name

        ]
        POST("profiles/${postFinalisedEditableProfileService.namespace}/${postFinalisedEditableProfileService.name}/dataModels/${id}", profileMap)
        verifyResponse(OK, response)

        then:
        responseBody().sections.first().fields.find {it.fieldName == namespaceFieldMap.fieldName}.currentValue == 'functional.test.profile.adjusted'
        responseBody().sections.first().fields.find {it.fieldName == domainsFieldMap.fieldName}.currentValue == ''

        cleanup:
        cleanupDataModelId(id)
    }

    String getExpectedSavedProfile() {
        '''{
  "sections": [
    {
      "name": "Profile Specification",
      "description": "The details necessary for this Data Model to be used as the specification for a dynamic profile.",
      "fields": [
        {
          "fieldName": "Metadata namespace",
          "metadataPropertyName": "metadataNamespace",
          "description": "The namespace under which properties of this profile will be stored",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "allowedValues": null,
          "regularExpression": null,
          "dataType": "string",
          "currentValue": "functional.test.profile"
        },
        {
          "fieldName": "Applicable for domains",
          "metadataPropertyName": "domainsApplicable",
          "description": "Determines which types of catalogue item can be profiled using this profile.  For example, 'DataModel'.  ''' +
        '''Separate multiple domains with a semi-colon (';').  Leave blank to allow this profile to be applicable to any catalogue item.",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "allowedValues": null,
          "regularExpression": null,
          "dataType": "string",
          "currentValue": "DataModel"
        }
      ]
    }
  ],
  "id": "${json-unit.matches:id}",
  "label": "profile functional model",
  "domainType": "DataModel"
}'''
    }
}
