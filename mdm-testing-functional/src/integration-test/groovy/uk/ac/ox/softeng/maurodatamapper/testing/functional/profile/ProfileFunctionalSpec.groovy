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
import uk.ac.ox.softeng.maurodatamapper.profile.DerivedFieldProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.PostFinalisedEditableProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileSpecificationFieldProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileSpecificationProfileService
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse
import spock.lang.PendingFeature

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
@Integration
class ProfileFunctionalSpec extends FunctionalSpec {

    ProfileSpecificationProfileService profileSpecificationProfileService
    PostFinalisedEditableProfileService postFinalisedEditableProfileService
    DerivedFieldProfileService derivedFieldProfileService
    ProfileSpecificationFieldProfileService profileSpecificationFieldProfileService

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
      "domainsApplicable",
      "notEditableAfterFinalisedField"
    ],
    "providerType": "Profile",
    "metadataNamespace": "uk.ac.ox.softeng.maurodatamapper.profile.editable",
    "domains": [
      "DataModel"
    ],
    "editableAfterFinalisation": true
  },
  {
    "name": "ProfileSpecificationProfileService",
    "version": "${json-unit.matches:version}",
    "displayName": "Profile Specification Profile (Data Model)",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      "metadataNamespace",
      "domainsApplicable",
      "editableAfterFinalisation"
    ],
    "providerType": "Profile",
    "metadataNamespace": "uk.ac.ox.softeng.maurodatamapper.profile",
    "domains": [
      "DataModel"
    ],
    "editableAfterFinalisation": false
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
      "editableAfterFinalisation"
    ],
    "providerType": "Profile",
    "metadataNamespace": "uk.ac.ox.softeng.maurodatamapper.profile.dataelement",
    "domains": [
      "DataElement"
    ],
    "editableAfterFinalisation": false
  },
   {
    "name": "DerivedFieldProfileService",
    "version": "${json-unit.matches:version}",
    "displayName": "Derived Field Profile",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      "derivedField",
      "uneditableField",
      "uneditableFieldOptional",
      "plainField"
    ],
    "providerType": "Profile",
    "metadataNamespace": "uk.ac.ox.softeng.maurodatamapper.profile.derived",
    "domains": [
      "DataModel"
    ],
    "editableAfterFinalisation": false
  }
]'''
    }

    void 'N01 : test validating profile on DataModel (as reader)'() {
        given:
        String id = getDataModelId()
        Map namespaceFieldMap = [
            metadataPropertyName: "metadataNamespace",
        ]
        Map domainsFieldMap = [
            metadataPropertyName: "domainsApplicable",
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    name       : 'Profile Specification'
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
        responseBody().errors.first().message == 'This field cannot be empty'
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
            currentValue        : 'functional.test.profile',
            metadataPropertyName: "metadataNamespace",
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            metadataPropertyName: "domainsApplicable",
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    name       : 'Profile Specification'
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
        responseBody().sections.first().fields.find {it.metadataPropertyName == namespaceFieldMap.metadataPropertyName}.currentValue == namespaceFieldMap.currentValue
        responseBody().sections.first().fields.find {it.metadataPropertyName == domainsFieldMap.metadataPropertyName}.currentValue == domainsFieldMap.currentValue

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
            currentValue        : 'functional.test.profile',
            metadataPropertyName: "metadataNamespace",
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            metadataPropertyName: "domainsApplicable",
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    name       : 'Profile Specification'
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
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                    ],
                    name       : 'Profile Specification'
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
        responseBody().sections.first().fields.find {it.metadataPropertyName == namespaceFieldMap.metadataPropertyName}.currentValue == 'functional.test.profile.adjusted'
        responseBody().sections.first().fields.find {it.metadataPropertyName == domainsFieldMap.metadataPropertyName}.currentValue == domainsFieldMap.currentValue

        when:
        domainsFieldMap.currentValue = ''
        profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        domainsFieldMap,
                    ],
                    name       : 'Profile Specification'
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
        responseBody().sections.first().fields.find {it.metadataPropertyName == namespaceFieldMap.metadataPropertyName}.currentValue == 'functional.test.profile.adjusted'
        responseBody().sections.first().fields.find {it.metadataPropertyName == domainsFieldMap.metadataPropertyName}.currentValue == ''

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N04 : test deleting profile (as editor)'() {
        given:
        String id = getDataModelId()
        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: "metadataNamespace",
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            metadataPropertyName: "domainsApplicable",
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    name       : 'Profile Specification'
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
            currentValue        : 'functional.test.profile',
            metadataPropertyName: "metadataNamespace",
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            metadataPropertyName: "domainsApplicable",
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    name       : 'Profile Specification'
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
            currentValue        : 'functional.test.profile',
            metadataPropertyName: "metadataNamespace",
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            metadataPropertyName: "domainsApplicable",
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    name       : 'Profile Specification'
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
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                    ],
                    name       : 'Profile Specification'
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
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        domainsFieldMap,
                    ],
                    name       : 'Profile Specification'
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
            currentValue        : 'functional.test.profile',
            metadataPropertyName: "metadataNamespace",
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            metadataPropertyName: "domainsApplicable",
        ]
        Map notEditableAfterFinalisationFieldMap = [
            currentValue        : 'value after finalisation',
            metadataPropertyName: "notEditableAfterFinalisedField",
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap,
                        notEditableAfterFinalisationFieldMap
                    ],
                    name       : 'Profile Specification'
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
        responseBody().sections.first().fields.find {it.metadataPropertyName == namespaceFieldMap.metadataPropertyName}.currentValue == namespaceFieldMap.currentValue
        responseBody().sections.first().fields.find {it.metadataPropertyName == domainsFieldMap.metadataPropertyName}.currentValue == domainsFieldMap.currentValue
        !responseBody().sections.first().fields.find {it.metadataPropertyName == notEditableAfterFinalisationFieldMap.metadataPropertyName}.currentValue
        !responseBody().sections.first().fields.find {it.metadataPropertyName == notEditableAfterFinalisationFieldMap.metadataPropertyName}.editableAfterFinalisation

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
            currentValue        : 'functional.test.profile',
            metadataPropertyName: "metadataNamespace",
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            metadataPropertyName: "domainsApplicable",
        ]
        Map notEditableAfterFinalisationFieldMap = [
            currentValue        : 'value before finalisation',
            metadataPropertyName: "notEditableAfterFinalisedField",
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap,
                        notEditableAfterFinalisationFieldMap
                    ],
                    name       : 'Profile Specification'
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
        notEditableAfterFinalisationFieldMap.currentValue = 'value set after finalisation'
        profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        notEditableAfterFinalisationFieldMap
                    ],
                    name       : 'Profile Specification'
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
        responseBody().sections.first().fields.find {it.metadataPropertyName == namespaceFieldMap.metadataPropertyName}.currentValue == 'functional.test.profile.adjusted'
        responseBody().sections.first().fields.find {it.metadataPropertyName == domainsFieldMap.metadataPropertyName}.currentValue == domainsFieldMap.currentValue
        responseBody().sections.first().fields.find {it.metadataPropertyName == notEditableAfterFinalisationFieldMap.metadataPropertyName}.currentValue ==
        'value before finalisation'
        !responseBody().sections.first().fields.find {it.metadataPropertyName == notEditableAfterFinalisationFieldMap.metadataPropertyName}.editableAfterFinalisation

        when:
        domainsFieldMap.currentValue = ''
        profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        domainsFieldMap,
                    ],
                    name       : 'Profile Specification'
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
        responseBody().sections.first().fields.find {it.metadataPropertyName == namespaceFieldMap.metadataPropertyName}.currentValue == 'functional.test.profile.adjusted'
        responseBody().sections.first().fields.find {it.metadataPropertyName == domainsFieldMap.metadataPropertyName}.currentValue == ''
        responseBody().sections.first().fields.find {it.metadataPropertyName == notEditableAfterFinalisationFieldMap.metadataPropertyName}.currentValue ==
        'value before finalisation'
        !responseBody().sections.first().fields.find {it.metadataPropertyName == notEditableAfterFinalisationFieldMap.metadataPropertyName}.editableAfterFinalisation

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N09 : test saving and getting profile with derived fields (as editor)'() {
        given:
        String id = getDataModelId()
        Map profileMap = [
            sections  : [
                [
                    fields: [
                        [
                            currentValue        : 'functional.test.profile',
                            metadataPropertyName: "plainField",
                        ],
                        [
                            currentValue        : 'functional.test.profile',
                            metadataPropertyName: "uneditableFieldOptional",
                        ],
                    ],
                    name  : 'Profile Derived Specification'
                ]
            ],
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : derivedFieldProfileService.namespace,
            name      : derivedFieldProfileService.name

        ]

        when:
        loginReader()
        POST("profiles/${derivedFieldProfileService.namespace}/${derivedFieldProfileService.name}/dataModels/${id}/validate", profileMap)

        then:
        verifyResponse(OK, response)

        when:
        loginEditor()
        POST("profiles/${derivedFieldProfileService.namespace}/${derivedFieldProfileService.name}/dataModels/${id}", profileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == 'Plain Field'}.currentValue == 'functional.test.profile'
        responseBody().sections.first().fields.find {it.fieldName == 'Derived Field'}.currentValue == 'profile functional model'
        responseBody().sections.first().fields.find {it.fieldName == 'Derived Field'}.uneditable
        !responseBody().sections.first().fields.find {it.fieldName == 'Uneditable Field'}.currentValue
        !responseBody().sections.first().fields.find {it.fieldName == 'Uneditable Field Optional'}.currentValue

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${id}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().first().name == derivedFieldProfileService.name
        localResponse.body().first().namespace == derivedFieldProfileService.namespace

        when:
        GET("profiles/${derivedFieldProfileService.namespace}/${derivedFieldProfileService.name}/dataModels/${id}", STRING_ARG)

        then:
        verifyJsonResponse(OK, getExpectedDerivedSavedProfile())

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N10 : test saving a dynamic profile (as editor)'() {
        given:
        String simpleModelId = getDataModelId()

        loginEditor()
        POST("folders/${getTestFolderId()}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: "Dynamic Profile Model"])
        verifyResponse(CREATED, response)
        String dynamicProfileModelId = responseBody().id

        POST("dataModels/$dynamicProfileModelId/dataClasses", [label: 'Profile Section Class'])
        verifyResponse(CREATED, response)
        String dataClassId = responseBody().id

        GET("dataModels/$dynamicProfileModelId/dataTypes")
        verifyResponse(OK, response)
        Map<String, String> dataTypes = (responseBody().items as List<Map>).collectEntries {
            [it.label, it.id]
        }

        POST("dataModels/$dynamicProfileModelId/dataClasses/$dataClassId/dataElements", [
            label          : 'Dynamic Profile Elem (Optional)',
            dataType       : dataTypes.string,
            maxMultiplicity: 1,
            minMultiplicity: 0
        ])
        verifyResponse(CREATED, response)

        POST("dataModels/$dynamicProfileModelId/dataClasses/$dataClassId/dataElements", [
            label          : 'Dynamic Profile Elem (Mandatory)',
            dataType       : dataTypes.string,
            maxMultiplicity: 1,
            minMultiplicity: 1
        ])
        verifyResponse(CREATED, response)

        POST("dataModels/$dynamicProfileModelId/dataClasses/$dataClassId/dataElements", [
            label   : 'Dynamic Profile Elem (Default Optional)',
            dataType: dataTypes.string
        ])
        verifyResponse(CREATED, response)

        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
            currentValue        : '',
            metadataPropertyName: 'domainsApplicable',
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    name       : 'Profile Specification'
                ]
            ],
            id        : dynamicProfileModelId.toString(),
            label     : 'Dynamic Profile Model',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name
        ]

        when:
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${dynamicProfileModelId}", profileMap)

        then:
        verifyResponse(OK, response)

        when:
        Map optionalFieldMap = [
            fieldName   : 'Dynamic Profile Elem (Optional)',
            currentValue: 'abc'
        ]
        Map mandatoryFieldMap = [
            fieldName   : 'Dynamic Profile Elem (Mandatory)',
            currentValue: 'def'
        ]
        Map defaultOptionalFieldMap = [
            fieldName   : 'Dynamic Profile Elem (Default Optional)',
            currentValue: ''
        ]
        Map dynamicProfileMap = [
            sections  : [
                [
                    fields: [
                        optionalFieldMap,
                        mandatoryFieldMap,
                        defaultOptionalFieldMap
                    ],
                    name  : 'Profile Section Class'
                ]
            ],
            id        : simpleModelId,
            domainType: 'DataModel',
            namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
            name      : 'Dynamic+Profile+Model'
        ]

        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == optionalFieldMap.fieldName}.currentValue == optionalFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == mandatoryFieldMap.fieldName}.currentValue == mandatoryFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == defaultOptionalFieldMap.fieldName}.currentValue == defaultOptionalFieldMap.currentValue

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/$simpleModelId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().first().name == 'Dynamic+Profile+Model'
        localResponse.body().first().namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", STRING_ARG)

        then:
        verifyResponse(OK, jsonCapableResponse)
        verifyJsonResponse OK, '''{
  "sections": [
    {
      "name": "Profile Section Class",
      "description": null,
      "fields": [
        {
          "fieldName": "Dynamic Profile Elem (Optional)",
          "metadataPropertyName": null,
          "description": null,
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "allowedValues": [],
          "regularExpression": null,
          "dataType": "string",
          "derived": false,
          "derivedFrom": null,
          "uneditable": false,
          "defaultValue":null,
          "editableAfterFinalisation": true,
          "currentValue": "abc"
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": null,
          "description": null,
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "allowedValues": [],
          "regularExpression": null,
          "dataType": "string",
          "derived": false,
          "derivedFrom": null,
          "uneditable": false,
          "defaultValue":null,
          "editableAfterFinalisation": true,
          "currentValue": "def"
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": null,
          "description": null,
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "allowedValues": [],
          "regularExpression": null,
          "dataType": "string",
          "derived": false,
          "derivedFrom": null,
          "uneditable": false,
          "defaultValue":null,
          "editableAfterFinalisation": true,
          "currentValue": ""
        }
      ]
    }
  ],
  "id": "${json-unit.matches:id}",
  "label": "profile functional model",
  "domainType": "DataModel"
}'''

        cleanup:
        cleanupDataModelId(simpleModelId)
        cleanupDataModelId(dynamicProfileModelId)
    }

    void 'N11 : test editing a dynamic profile'() {
        given:
        String simpleModelId = getDataModelId()

        loginEditor()
        POST("folders/${getTestFolderId()}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: "Dynamic Profile Model"])
        verifyResponse(CREATED, response)
        String dynamicProfileModelId = responseBody().id

        POST("dataModels/$dynamicProfileModelId/dataClasses", [label: 'Profile Section Class'])
        verifyResponse(CREATED, response)
        String dataClassId = responseBody().id

        GET("dataModels/$dynamicProfileModelId/dataTypes")
        verifyResponse(OK, response)
        Map<String, String> dataTypes = (responseBody().items as List<Map>).collectEntries {
            [it.label, it.id]
        }

        POST("dataModels/$dynamicProfileModelId/dataClasses/$dataClassId/dataElements", [
            label          : 'Dynamic Profile Elem (Optional)',
            dataType       : dataTypes.string,
            maxMultiplicity: 1,
            minMultiplicity: 0
        ])
        verifyResponse(CREATED, response)

        POST("dataModels/$dynamicProfileModelId/dataClasses/$dataClassId/dataElements", [
            label          : 'Dynamic Profile Elem (Mandatory)',
            dataType       : dataTypes.string,
            maxMultiplicity: 1,
            minMultiplicity: 1
        ])
        verifyResponse(CREATED, response)

        POST("dataModels/$dynamicProfileModelId/dataClasses/$dataClassId/dataElements", [
            label   : 'Dynamic Profile Elem (Default Optional)',
            dataType: dataTypes.string
        ])
        verifyResponse(CREATED, response)

        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
            currentValue        : '',
            metadataPropertyName: 'domainsApplicable',
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap
                    ],
                    name       : 'Profile Specification'
                ]
            ],
            id        : dynamicProfileModelId.toString(),
            label     : 'Dynamic Profile Model',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name
        ]
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${dynamicProfileModelId}", profileMap)
        verifyResponse(OK, response)

        Map optionalFieldMap = [
            fieldName   : 'Dynamic Profile Elem (Optional)',
            currentValue: 'abc'
        ]
        Map mandatoryFieldMap = [
            fieldName   : 'Dynamic Profile Elem (Mandatory)',
            currentValue: 'def'
        ]
        Map defaultOptionalFieldMap = [
            fieldName   : 'Dynamic Profile Elem (Default Optional)',
            currentValue: ''
        ]
        Map dynamicProfileMap = [
            sections  : [
                [
                    fields: [
                        optionalFieldMap,
                        mandatoryFieldMap,
                        defaultOptionalFieldMap
                    ],
                    name  : 'Profile Section Class'
                ]
            ],
            id        : simpleModelId,
            domainType: 'DataModel',
            namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
            name      : 'Dynamic+Profile+Model'
        ]
        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", dynamicProfileMap)
        verifyResponse(OK, response)

        when:
        optionalFieldMap.currentValue = ''
        defaultOptionalFieldMap.currentValue = 'edited value'
        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == optionalFieldMap.fieldName}.currentValue == optionalFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == mandatoryFieldMap.fieldName}.currentValue == mandatoryFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == defaultOptionalFieldMap.fieldName}.currentValue == defaultOptionalFieldMap.currentValue

        cleanup:
        cleanupDataModelId(simpleModelId)
        cleanupDataModelId(dynamicProfileModelId)
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
          "derived": false,
          "derivedFrom": null,
          "uneditable": false,
          "defaultValue":null,
          "editableAfterFinalisation": true,
          "currentValue": "functional.test.profile"
        },
        {
          "fieldName": "Applicable for domains",
          "metadataPropertyName": "domainsApplicable",
          "description": "Determines which types of catalogue item can be profiled using this profile.  ''' +
        '''For example, 'DataModel'.  Separate multiple domains with a semi-colon (';').  Leave blank to allow this profile to be applicable to any catalogue item.",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "allowedValues": null,
          "regularExpression": null,
          "dataType": "string",
          "derived": false,
          "derivedFrom": null,
          "uneditable": false,
          "defaultValue":null,
          "editableAfterFinalisation": true,
          "currentValue": "DataModel"
        },
        {
          "fieldName": "Can be edited after finalisation",
          "metadataPropertyName": "editableAfterFinalisation",
          "description": "Defines if the profile can be edited after the model has been finalised. This defaults to false.",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "allowedValues": null,
          "regularExpression": null,
          "dataType": "boolean",
          "derived": false,
          "derivedFrom": null,
          "uneditable": false,
          "defaultValue":null,
          "editableAfterFinalisation": true,
          "currentValue": ""
        }
      ]
    }
  ],
  "id": "${json-unit.matches:id}",
  "label": "profile functional model",
  "domainType": "DataModel"
}'''
    }

    String getExpectedDerivedSavedProfile() {
        '''{
  "sections": [
    {
      "name": "Profile Derived Specification",
      "description": "The details necessary for this Data Model to be used as the specification for a dynamic profile.",
      "fields": [
        {
          "fieldName": "Derived Field",
          "metadataPropertyName": "derivedField",
          "description": "A field which is derived",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "allowedValues": null,
          "regularExpression": null,
          "dataType": "string",
          "derived": true,
          "derivedFrom": "label",
          "uneditable": true,
          "defaultValue":null,
          "editableAfterFinalisation": true,
          "currentValue": "profile functional model"
        },
        {
          "fieldName": "Uneditable Field",
          "metadataPropertyName": "uneditableField",
          "description": "A field which is uneditable and listed as mandatory",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "allowedValues": null,
          "regularExpression": null,
          "dataType": "string",
          "derived": false,
          "derivedFrom": null,
          "uneditable": true,
          "defaultValue":null,
          "editableAfterFinalisation": true,
          "currentValue": ""
        },
        {
          "fieldName": "Uneditable Field Optional",
          "metadataPropertyName": "uneditableFieldOptional",
          "description": "A field which is uneditable and listed as optional",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "allowedValues": null,
          "regularExpression": null,
          "dataType": "string",
          "derived": false,
          "derivedFrom": null,
          "uneditable": true,
          "defaultValue":null,
          "editableAfterFinalisation": true,
          "currentValue": ""
        },
        {
          "fieldName": "Plain Field",
          "metadataPropertyName": "plainField",
          "description": "A field which is normal",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "allowedValues": null,
          "regularExpression": null,
          "dataType": "string",
          "derived": false,
          "derivedFrom": null,
          "uneditable": false,
          "defaultValue":null,
          "editableAfterFinalisation": true,
          "currentValue": "functional.test.profile"
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
