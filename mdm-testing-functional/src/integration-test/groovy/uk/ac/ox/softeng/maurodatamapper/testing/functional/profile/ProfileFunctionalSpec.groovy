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

import static io.micronaut.http.HttpStatus.BAD_REQUEST
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

    Map<String, String> getSecondDataModelIds() {
        loginEditor()

        // Create a Data Model
        POST("folders/${getTestFolderId()}/dataModels", [label: "second profile functional model"])
        verifyResponse(CREATED, response)
        String id = responseBody().id

        // Add a Data Type
        POST("dataModels/${id}/dataTypes", [label: "profile functional data type", domainType: "PrimitiveType"])
        verifyResponse(CREATED, response)
        String dataTypeId = responseBody().id

        // Add a Data Class
        POST("dataModels/${id}/dataClasses", [label: "profile functional class"])
        verifyResponse(CREATED, response)
        String dataClassId = responseBody().id

        // Add a Data Element
        POST("dataModels/${id}/dataClasses/${dataClassId}/dataElements", [label: "first data element in profile functional class", dataType: [id: dataTypeId]])
        verifyResponse(CREATED, response)
        String firstDataElementId = responseBody().id

        // And another Data Element
        POST("dataModels/${id}/dataClasses/${dataClassId}/dataElements", [label: "second data element in profile functional class", dataType: [id: dataTypeId]])
        verifyResponse(CREATED, response)
        String secondDataElementId = responseBody().id

        logout()
        ["dataModelId": id, "dataTypeId": dataTypeId, "dataClassId": dataClassId, "firstDataElementId": firstDataElementId, "secondDataElementId": secondDataElementId]
    }

    Map<String, String> getThirdDataModelIds() {
        loginEditor()

        // Create a Data Model
        POST("folders/${getTestFolderId()}/dataModels", [label: "third profile functional model"])
        verifyResponse(CREATED, response)
        String id = responseBody().id

        // Add a Data Type
        POST("dataModels/${id}/dataTypes", [label: "profile functional data type", domainType: "PrimitiveType"])
        verifyResponse(CREATED, response)
        String dataTypeId = responseBody().id

        // Add a Data Class
        POST("dataModels/${id}/dataClasses", [label: "profile functional class"])
        verifyResponse(CREATED, response)
        String dataClassId = responseBody().id

        // Add a Data Element
        POST("dataModels/${id}/dataClasses/${dataClassId}/dataElements", [label: "first data element in profile functional class", dataType: [id: dataTypeId]])
        verifyResponse(CREATED, response)
        String firstDataElementId = responseBody().id

        // And another Data Element
        POST("dataModels/${id}/dataClasses/${dataClassId}/dataElements", [label: "second data element in profile functional class", dataType: [id: dataTypeId]])
        verifyResponse(CREATED, response)
        String secondDataElementId = responseBody().id

        logout()
        ["dataModelId": id, "dataTypeId": dataTypeId, "dataClassId": dataClassId, "firstDataElementId": firstDataElementId, "secondDataElementId": secondDataElementId]
    }

    String getDynamicProfileModelId() {

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

        logout()

        dynamicProfileModelId
    }

    /**
     * A dynamic profile which is editable post finalisation
     * @return
     */
    String getSecondDynamicProfileModelId() {

        loginEditor()
        POST("folders/${getTestFolderId()}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: "Second Dynamic Profile Model"])
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
            currentValue        : 'functional.test.second.profile',
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
            currentValue        : '',
            metadataPropertyName: 'domainsApplicable',
        ]
        Map editableMap = [
                currentValue        : true,
                metadataPropertyName: 'editableAfterFinalisation',
        ]
        Map profileMap = [
            sections  : [
                [
                    description: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields     : [
                        namespaceFieldMap,
                        domainsFieldMap,
                        editableMap
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

        logout()

        dynamicProfileModelId
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
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Optional)",
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
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
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
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
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

    void 'N12 : test validating OK a dynamic profile (as editor) using validateMany'() {
        given:
        Map<String, String> secondIds = getSecondDataModelIds()
        String secondModelId = secondIds["dataModelId"]
        String secondModelFirstDataElementId = secondIds["firstDataElementId"]
        String secondModelSecondDataElementId = secondIds["secondDataElementId"]
        Map<String, String> thirdIds = getThirdDataModelIds()
        String thirdModelId = thirdIds["dataModelId"]
        String thirdModelFirstDataElementId = thirdIds["firstDataElementId"]
        String thirdModelSecondDataElementId = thirdIds["secondDataElementId"]
        String dynamicProfileModelId = getDynamicProfileModelId()
        String secondDynamicProfileModelId = getSecondDynamicProfileModelId()

        /**
         * 1. Validate two profiles against two data elements, both succeed
         */
        when: 'use saveMany to save one profile on the first data element'
        Map optionalFieldMap1 = [
            fieldName   : 'Dynamic Profile Elem (Optional)',
            currentValue: 'abc'
        ]
        Map mandatoryFieldMap1 = [
            fieldName   : 'Dynamic Profile Elem (Mandatory)',
            currentValue: 'def'
        ]
        Map defaultOptionalFieldMap1 = [
            fieldName   : 'Dynamic Profile Elem (Default Optional)',
            currentValue: ''
        ]

        Map optionalFieldMap2_1 = [
            fieldName   : 'Dynamic Profile Elem (Optional)',
            currentValue: '2_1 abc'
        ]
        Map mandatoryFieldMap2_1 = [
            fieldName   : 'Dynamic Profile Elem (Mandatory)',
            currentValue: '2_1 def'
        ]
        Map defaultOptionalFieldMap2_1 = [
            fieldName   : 'Dynamic Profile Elem (Default Optional)',
            currentValue: '2_1'
        ]

        Map dynamicProfileMap = [
            profilesProvided: [
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap1,
                                    mandatoryFieldMap1,
                                    defaultOptionalFieldMap1
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : secondModelFirstDataElementId,
                        label     : "first data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Dynamic+Profile+Model'
                    ]
                ],
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap2_1,
                                    mandatoryFieldMap2_1,
                                    defaultOptionalFieldMap2_1
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : secondModelSecondDataElementId,
                        label     : "second data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Second+Dynamic+Profile+Model'
                    ]
                ]
            ]
        ]

        loginEditor()
        POST("dataModels/$secondModelId/profile/validateMany", dynamicProfileMap)

        then: 'the profiles are validated successfully'
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().profilesProvided.size == 2
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == optionalFieldMap1.fieldName}.currentValue == optionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap1.fieldName}.currentValue == mandatoryFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap1.fieldName}.currentValue == defaultOptionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.id == secondModelFirstDataElementId
        responseBody().profilesProvided[0].profile.domainType == "DataElement"
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == optionalFieldMap2_1.fieldName}.currentValue == optionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap2_1.fieldName}.currentValue == mandatoryFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap2_1.fieldName}.currentValue == defaultOptionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.id == secondModelSecondDataElementId
        responseBody().profilesProvided[1].profile.domainType == "DataElement"

        when: 'used profiles are retrieved for the first data element'
        HttpResponse<List<Map>> localResponse = GET("dataElements/$secondModelFirstDataElementId/profiles/used", Argument.listOf(Map))

        then: 'there are not any'
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 0

        when: 'used profiles are retrieved for the second data element'
        localResponse = GET("dataElements/$secondModelSecondDataElementId/profiles/used", Argument.listOf(Map))

        then: 'there are not any'
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 0

        cleanup:
        cleanupDataModelId(secondModelId)
        cleanupDataModelId(thirdModelId)
        cleanupDataModelId(dynamicProfileModelId)
        cleanupDataModelId(secondDynamicProfileModelId)
    }

    void 'N13 : test validating OK a dynamic profile (as editor) using validateMany'() {
        given:
        Map<String, String> secondIds = getSecondDataModelIds()
        String secondModelId = secondIds["dataModelId"]
        String secondModelFirstDataElementId = secondIds["firstDataElementId"]
        String secondModelSecondDataElementId = secondIds["secondDataElementId"]
        Map<String, String> thirdIds = getThirdDataModelIds()
        String thirdModelId = thirdIds["dataModelId"]
        String thirdModelFirstDataElementId = thirdIds["firstDataElementId"]
        String thirdModelSecondDataElementId = thirdIds["secondDataElementId"]
        String dynamicProfileModelId = getDynamicProfileModelId()
        String secondDynamicProfileModelId = getSecondDynamicProfileModelId()

        /**
         * 1. Validate two profiles against two data elements, first one fails
         */
        when: 'use validateMany to save one profile on the first data element'
        Map optionalFieldMap1 = [
            fieldName   : 'Dynamic Profile Elem (Optional)',
            currentValue: 'abc'
        ]
        Map mandatoryFieldMap1 = [
            fieldName   : 'Dynamic Profile Elem (Mandatory)',
            currentValue: '' // Missing value so should fail validation
        ]
        Map defaultOptionalFieldMap1 = [
            fieldName   : 'Dynamic Profile Elem (Default Optional)',
            currentValue: ''
        ]

        Map optionalFieldMap2_1 = [
            fieldName   : 'Dynamic Profile Elem (Optional)',
            currentValue: '2_1 abc'
        ]
        Map mandatoryFieldMap2_1 = [
            fieldName   : 'Dynamic Profile Elem (Mandatory)',
            currentValue: '2_1 def'
        ]
        Map defaultOptionalFieldMap2_1 = [
            fieldName   : 'Dynamic Profile Elem (Default Optional)',
            currentValue: '2_1'
        ]

        Map dynamicProfileMap = [
            profilesProvided: [
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap1,
                                    mandatoryFieldMap1,
                                    defaultOptionalFieldMap1
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : secondModelFirstDataElementId,
                        label     : "first data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Dynamic+Profile+Model'
                    ]
                ],
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap2_1,
                                     mandatoryFieldMap2_1,
                                      defaultOptionalFieldMap2_1
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : secondModelSecondDataElementId,
                        label     : "second data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Second+Dynamic+Profile+Model'
                    ]
                ]
            ]
        ]

        loginEditor()
        POST("dataModels/$secondModelId/profile/validateMany", dynamicProfileMap)

        then: 'the first profile has an error'
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().count == 2
        responseBody().profilesProvided.size == 2
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == optionalFieldMap1.fieldName}.currentValue == optionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap1.fieldName}.currentValue == mandatoryFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap1.fieldName}.currentValue == defaultOptionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.id == secondModelFirstDataElementId
        responseBody().profilesProvided[0].profile.domainType == "DataElement"
        responseBody().profilesProvided[0].errors.errors.size == 1
        responseBody().profilesProvided[0].errors.errors[0].message == "This field cannot be empty"
        responseBody().profilesProvided[0].errors.errors[0].fieldName == "Dynamic Profile Elem (Mandatory)"

        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == optionalFieldMap2_1.fieldName}.currentValue == optionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap2_1.fieldName}.currentValue == mandatoryFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap2_1.fieldName}.currentValue == defaultOptionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.id == secondModelSecondDataElementId
        responseBody().profilesProvided[1].profile.domainType == "DataElement"
        !responseBody().profilesProvided[1].errors

        when: 'used profiles are retrieved for the first data element'
        HttpResponse<List<Map>> localResponse = GET("dataElements/$secondModelFirstDataElementId/profiles/used", Argument.listOf(Map))

        then: 'there are not any'
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 0

        when: 'used profiles are retrieved for the second data element'
        localResponse = GET("dataElements/$secondModelSecondDataElementId/profiles/used", Argument.listOf(Map))

        then: 'there are not any'
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 0

        cleanup:
        cleanupDataModelId(secondModelId)
        cleanupDataModelId(thirdModelId)
        cleanupDataModelId(dynamicProfileModelId)
        cleanupDataModelId(secondDynamicProfileModelId)
    }


    /**
     * 1. Save two profiles for one Data Element
     * 2. Update one of the previously saved dynamic profiles, and in the same
     * request save a dynamic profile against the second data element
     * 3. Use saveMany to update the dynamic profiles that have previously been saved
     * against the first and second data elements on the second data model, and also
     * try to save a profile against the first data element of the third model, but using
     * the endpoint of the second data model. The first two updates should work, and the
     * third should fail silently.
     * 4. Use getMany to retrieve the saved profiles
     */
    void 'N14 : test saving and retrieving dynamic profiles (as editor) using saveMany and getMany'() {
        given:
        Map<String, String> secondIds = getSecondDataModelIds()
        String secondModelId = secondIds["dataModelId"]
        String secondModelFirstDataElementId = secondIds["firstDataElementId"]
        String secondModelSecondDataElementId = secondIds["secondDataElementId"]
        Map<String, String> thirdIds = getThirdDataModelIds()
        String thirdModelId = thirdIds["dataModelId"]
        String thirdModelFirstDataElementId = thirdIds["firstDataElementId"]
        String thirdModelSecondDataElementId = thirdIds["secondDataElementId"]
        String dynamicProfileModelId = getDynamicProfileModelId()
        String secondDynamicProfileModelId = getSecondDynamicProfileModelId()


        Map optionalFieldMap1 = [
            fieldName   : 'Dynamic Profile Elem (Optional)',
            currentValue: 'abc'
        ]
        Map mandatoryFieldMap1 = [
            fieldName   : 'Dynamic Profile Elem (Mandatory)',
            currentValue: 'def'
        ]
        Map defaultOptionalFieldMap1 = [
            fieldName   : 'Dynamic Profile Elem (Default Optional)',
            currentValue: ''
        ]

        Map optionalFieldMap2_1 = [
                fieldName   : 'Dynamic Profile Elem (Optional)',
                currentValue: '2_1 abc'
        ]
        Map mandatoryFieldMap2_1 = [
                fieldName   : 'Dynamic Profile Elem (Mandatory)',
                currentValue: '2_1 def'
        ]
        Map defaultOptionalFieldMap2_1 = [
                fieldName   : 'Dynamic Profile Elem (Default Optional)',
                currentValue: '2_1'
        ]

        Map dynamicProfileMap = [
            profilesProvided: [
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap1,
                                    mandatoryFieldMap1,
                                    defaultOptionalFieldMap1
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : secondModelFirstDataElementId,
                        label     : "first data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Dynamic+Profile+Model'
                    ]
                ],
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap2_1,
                                    mandatoryFieldMap2_1,
                                    defaultOptionalFieldMap2_1
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : secondModelFirstDataElementId,
                        label     : "first data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Second+Dynamic+Profile+Model'
                    ]
                ]
            ]
        ]

        loginEditor()

        /**
         * 1. First save two dynamic profiles against the first data element
         */

        when: 'use saveMany on a MultiFacetAwareItem that is not a Model'
        POST("dataElements/$secondModelFirstDataElementId/profile/saveMany", dynamicProfileMap)

        then:
        verifyResponse(BAD_REQUEST, response)

        when: 'use saveMany to save one profile on the first data element'
        POST("dataModels/$secondModelId/profile/saveMany", dynamicProfileMap)

        then: 'the profile is saved for the first data element'
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().profilesProvided.size == 2
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == optionalFieldMap1.fieldName}.currentValue == optionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap1.fieldName}.currentValue == mandatoryFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap1.fieldName}.currentValue == defaultOptionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.id == secondModelFirstDataElementId
        responseBody().profilesProvided[0].profile.domainType == "DataElement"
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == optionalFieldMap2_1.fieldName}.currentValue == optionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap2_1.fieldName}.currentValue == mandatoryFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap2_1.fieldName}.currentValue == defaultOptionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.id == secondModelFirstDataElementId
        responseBody().profilesProvided[1].profile.domainType == "DataElement"

        when: 'used profiles are retrieved for the first data element'
        HttpResponse<List<Map>> localResponse = GET("dataElements/$secondModelFirstDataElementId/profiles/used", Argument.listOf(Map))

        then: 'the saved profile is returned'
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 2
        localResponse.body()[0].name == 'Dynamic+Profile+Model'
        localResponse.body()[0].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'
        localResponse.body()[1].name == 'Second+Dynamic+Profile+Model'
        localResponse.body()[1].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataElements/$secondModelFirstDataElementId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", STRING_ARG)

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
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Optional)",
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
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
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
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
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
  "label": "first data element in profile functional class",
  "domainType": "DataElement"
}'''


        /**
         * 2. Use saveMany to update one of the previously saved dynamic profiles, and in the same
         * request save a dynamic profile against the second data element
         */
        when:
        optionalFieldMap1 = [
                fieldName   : 'Dynamic Profile Elem (Optional)',
                currentValue: 'abc updated'
        ]
        mandatoryFieldMap1 = [
                fieldName   : 'Dynamic Profile Elem (Mandatory)',
                currentValue: 'def updated'
        ]
        defaultOptionalFieldMap1 = [
                fieldName   : 'Dynamic Profile Elem (Default Optional)',
                currentValue: 'updated'
        ]

        Map optionalFieldMap2 = [
            fieldName   : 'Dynamic Profile Elem (Optional)',
            currentValue: 'xyz'
        ]
        Map mandatoryFieldMap2 = [
            fieldName   : 'Dynamic Profile Elem (Mandatory)',
            currentValue: 'pqr'
        ]
        Map defaultOptionalFieldMap2 = [
            fieldName   : 'Dynamic Profile Elem (Default Optional)',
            currentValue: 'onm'
        ]

        dynamicProfileMap = [
            profilesProvided: [
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap1,
                                    mandatoryFieldMap1,
                                    defaultOptionalFieldMap1
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : secondModelFirstDataElementId,
                        label     : "first data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Dynamic+Profile+Model'
                    ]
                ],
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap2,
                                    mandatoryFieldMap2,
                                    defaultOptionalFieldMap2
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : secondModelSecondDataElementId,
                        label     : "second data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Dynamic+Profile+Model'
                    ]
                ]
            ]
        ]

        POST("dataModels/$secondModelId/profile/saveMany", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().profilesProvided.size == 2
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == optionalFieldMap1.fieldName}.currentValue == optionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap1.fieldName}.currentValue == mandatoryFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap1.fieldName}.currentValue == defaultOptionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.id == secondModelFirstDataElementId
        responseBody().profilesProvided[0].profile.domainType == "DataElement"
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == optionalFieldMap2.fieldName}.currentValue == optionalFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap2.fieldName}.currentValue == mandatoryFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap2.fieldName}.currentValue == defaultOptionalFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.id == secondModelSecondDataElementId
        responseBody().profilesProvided[1].profile.domainType == "DataElement"

        when:
        localResponse = GET("dataElements/$secondModelFirstDataElementId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 2
        localResponse.body()[0].name == 'Dynamic+Profile+Model'
        localResponse.body()[0].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'
        localResponse.body()[1].name == 'Second+Dynamic+Profile+Model'
        localResponse.body()[1].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataElements/$secondModelFirstDataElementId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", STRING_ARG)

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
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Optional)",
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
          "currentValue": "abc updated"
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
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
          "currentValue": "def updated"
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
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
          "currentValue": "updated"
        }
      ]
    }
  ],
  "id": "${json-unit.matches:id}",
  "label": "first data element in profile functional class",
  "domainType": "DataElement"
}'''

        when:
        localResponse = GET("dataElements/$secondModelSecondDataElementId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().first().name == 'Dynamic+Profile+Model'
        localResponse.body().first().namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataElements/$secondModelSecondDataElementId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", STRING_ARG)

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
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Optional)",
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
          "currentValue": "xyz"
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
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
          "currentValue": "pqr"
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
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
          "currentValue": "onm"
        }
      ]
    }
  ],
  "id": "${json-unit.matches:id}",
  "label": "second data element in profile functional class",
  "domainType": "DataElement"
}'''

        /**
         * 3. Use saveMany to update the dynamic profiles that have previously been saved
         * against the first and second data elements on the second data model, and also
         * try to save a profile against the first data element of the third model, but using
         * the endpoint of the second data model. The first two updates should work, and the
         * third should fail silently.
         */
        when:
        optionalFieldMap1 = [
                fieldName   : 'Dynamic Profile Elem (Optional)',
                currentValue: 'abc updated again'
        ]
        mandatoryFieldMap1 = [
                fieldName   : 'Dynamic Profile Elem (Mandatory)',
                currentValue: 'def updated again'
        ]
        defaultOptionalFieldMap1 = [
                fieldName   : 'Dynamic Profile Elem (Default Optional)',
                currentValue: 'updated again'
        ]

        optionalFieldMap2 = [
                fieldName   : 'Dynamic Profile Elem (Optional)',
                currentValue: 'xyz updated'
        ]
        mandatoryFieldMap2 = [
                fieldName   : 'Dynamic Profile Elem (Mandatory)',
                currentValue: 'pqr updated'
        ]
        defaultOptionalFieldMap2 = [
                fieldName   : 'Dynamic Profile Elem (Default Optional)',
                currentValue: 'onm updated'
        ]

        Map optionalFieldMap3 = [
                fieldName   : 'Dynamic Profile Elem (Optional)',
                currentValue: 'hij'
        ]
        Map mandatoryFieldMap3 = [
                fieldName   : 'Dynamic Profile Elem (Mandatory)',
                currentValue: 'klm'
        ]
        Map defaultOptionalFieldMap3 = [
                fieldName   : 'Dynamic Profile Elem (Default Optional)',
                currentValue: ''
        ]

        dynamicProfileMap = [
            profilesProvided: [
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap1,
                                    mandatoryFieldMap1,
                                    defaultOptionalFieldMap1
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : secondModelFirstDataElementId,
                        label     : "first data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Dynamic+Profile+Model'
                    ]
                ],
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap2,
                                    mandatoryFieldMap2,
                                    defaultOptionalFieldMap2
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : secondModelSecondDataElementId,
                        label     : "second data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Dynamic+Profile+Model'
                    ]
                ],
                [
                    profile: [
                        sections  : [
                            [
                                fields: [
                                    optionalFieldMap3,
                                    mandatoryFieldMap3,
                                    defaultOptionalFieldMap3
                                ],
                                name  : 'Profile Section Class'
                            ]
                        ],
                        id        : thirdModelFirstDataElementId,
                        label     : "first data element in profile functional class",
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name      : 'Second+Dynamic+Profile+Model'
                    ]
                ]
            ]
        ]

        POST("dataModels/$secondModelId/profile/saveMany", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().profilesProvided.size == 2
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == optionalFieldMap1.fieldName}.currentValue == optionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap1.fieldName}.currentValue == mandatoryFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap1.fieldName}.currentValue == defaultOptionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.id == secondModelFirstDataElementId
        responseBody().profilesProvided[0].profile.domainType == "DataElement"
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == optionalFieldMap2.fieldName}.currentValue == optionalFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap2.fieldName}.currentValue == mandatoryFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap2.fieldName}.currentValue == defaultOptionalFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.id == secondModelSecondDataElementId
        responseBody().profilesProvided[1].profile.domainType == "DataElement"

        when:
        localResponse = GET("dataElements/$secondModelFirstDataElementId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 2
        localResponse.body()[0].name == 'Dynamic+Profile+Model'
        localResponse.body()[0].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'
        localResponse.body()[1].name == 'Second+Dynamic+Profile+Model'
        localResponse.body()[1].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataElements/$secondModelFirstDataElementId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", STRING_ARG)

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
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Optional)",
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
          "currentValue": "abc updated again"
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
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
          "currentValue": "def updated again"
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
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
          "currentValue": "updated again"
        }
      ]
    }
  ],
  "id": "${json-unit.matches:id}",
  "label": "first data element in profile functional class",
  "domainType": "DataElement"
}'''

        when:
        localResponse = GET("dataElements/$secondModelSecondDataElementId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().first().name == 'Dynamic+Profile+Model'
        localResponse.body().first().namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataElements/$secondModelSecondDataElementId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", STRING_ARG)

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
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Optional)",
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
          "currentValue": "xyz updated"
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
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
          "currentValue": "pqr updated"
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
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
          "currentValue": "onm updated"
        }
      ]
    }
  ],
  "id": "${json-unit.matches:id}",
  "label": "second data element in profile functional class",
  "domainType": "DataElement"
}'''

        when:
        localResponse = GET("dataElements/$thirdModelFirstDataElementId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 0

        when: 'do the same POST again, but after finalising the data models'
        finaliseDataModelId(secondModelId)
        finaliseDataModelId(thirdModelId)
        loginEditor()
        POST("dataModels/$secondModelId/profile/saveMany", dynamicProfileMap)

        then: 'no changes are made'
        verifyResponse(OK, response)
        responseBody().count == 0
        responseBody().profilesProvided.size == 0

        when: 'make the same POST again, but this time to thirdDataModel'
        POST("dataModels/$thirdModelId/profile/saveMany", dynamicProfileMap)

        then: 'the change to the first data element on the third model is made'
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().profilesProvided.size == 1

        /**
         * 4. Get many profiles for many elements
         */
        Map getManyMap = [
            "multiFacetAwareItems": [
                [
                    "multiFacetAwareItemDomainType": "dataElement",
                    "multiFacetAwareItemId": secondModelFirstDataElementId
                ],
                [
                    "multiFacetAwareItemDomainType": "dataElement",
                    "multiFacetAwareItemId": secondModelSecondDataElementId
                ]
            ],
            "profileProviderServices": [
                [
                    "name": "Dynamic+Profile+Model",
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.profile.provider"
                ]
            ]
        ]

        when: 'request made against a MultiFacetAwareItem that is not a model'
        POST("dataElements/$secondModelFirstDataElementId/profile/getMany", getManyMap)

        then:
        verifyResponse(BAD_REQUEST, response)

        when: 'correctly request against the second model'
        POST("dataModels/$secondModelId/profile/getMany", getManyMap)

        then: 'the profiles are listed'
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().profilesProvided.size == 2

        when: 'incorrectly request against the third model'
        POST("dataModels/$thirdModelId/profile/getMany", getManyMap)

        then: 'the profiles are not listed'
        verifyResponse(OK, response)
        responseBody().count == 0
        !responseBody().profilesProvided


        cleanup:
        cleanupDataModelId(secondModelId)
        cleanupDataModelId(thirdModelId)
        cleanupDataModelId(dynamicProfileModelId)
        cleanupDataModelId(secondDynamicProfileModelId)
    }

    void 'test that a profile having two versions, each with version tags, can be retrieved by namespace and name only'() {
        given: 'a finalised profile and a simple data model'
        String simpleModelId = getDataModelId()
        String profileModelId = getDynamicProfileModelId()
        loginEditor()
        PUT("dataModels/$profileModelId/finalise", [versionChangeType: 'Major', versionTag: 'Functional Test Version Tag'])
        verifyResponse OK, response

        when: 'get the finalised profile against the simple data model'
        GET("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", STRING_ARG)

        then: 'the response is OK'
        verifyResponse(OK, jsonCapableResponse)

        when: 'create a new branch model version of the profile'
        PUT("dataModels/$profileModelId/newBranchModelVersion", [:], MAP_ARG)

        then: 'the response is CREATED'
        verifyResponse CREATED, response
        String profileModelVersion2Id = response.body().id

        when: 'finalise the new branch model version'
        PUT("dataModels/$profileModelVersion2Id/finalise", [versionChangeType: 'Major', versionTag: 'Functional Test Second Version Tag'])

        then: 'the response is OK'
        verifyResponse OK, response

        when: 'get the finalised profile against the simple data model now that there are two versions of the profile'
        GET("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model", MAP_ARG)

        then: 'the response is OK'
        verifyResponse(OK, response)

        cleanup:
        logout()
        cleanupDataModelId(simpleModelId)
        cleanupDataModelId(profileModelId)
    }
}
