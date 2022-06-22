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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.profile

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.profile.DerivedFieldProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.InvalidProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.PostFinalisedEditableProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DefaultJsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.BAD_REQUEST
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * Permissions are tested here with the lowest or highest role which could perform this.
 * Checking that if you have that role then you can perform the action or the action is not allowed.
 * Actions are inherited from the datamodel,
 * authors can mess with MD
 * readers can validate a profile and see a profile
 * no-one can mess with a finalised or non-editable field unless the profile is a post-finalised editable one
 *
 */
@Slf4j
@Integration
class ProfileFunctionalSpec extends FunctionalSpec {

    DefaultJsonProfileProviderService profileSpecificationProfileService
    PostFinalisedEditableProfileService postFinalisedEditableProfileService
    DerivedFieldProfileService derivedFieldProfileService
    DefaultJsonProfileProviderService profileSpecificationFieldProfileService
    InvalidProfileService invalidProfileService

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Override
    String getResourcePath() {
        ''
    }

    String getDataModelId() {
        loginCreator()
        POST("folders/${getTestFolderId()}/dataModels", [label: 'profile functional model'])
        verifyResponse(CREATED, response)
        String id = responseBody().id
        logout()
        id
    }

    Map<String, String> getSecondDataModelIds() {
        loginCreator()

        // Create a Data Model
        POST("folders/${getTestFolderId()}/dataModels", [label: 'second profile functional model'])
        verifyResponse(CREATED, response)
        String id = responseBody().id
        addAccessShares(id, 'dataModels/')

        // Add a Data Type
        POST("dataModels/${id}/dataTypes", [label: 'profile functional data type', domainType: 'PrimitiveType'])
        verifyResponse(CREATED, response)
        String dataTypeId = responseBody().id

        // Add a Data Class
        POST("dataModels/${id}/dataClasses", [label: 'profile functional class'])
        verifyResponse(CREATED, response)
        String dataClassId = responseBody().id

        // Add a Data Element
        POST("dataModels/${id}/dataClasses/${dataClassId}/dataElements", [label: 'first data element in profile functional class', dataType: [id: dataTypeId]])
        verifyResponse(CREATED, response)
        String firstDataElementId = responseBody().id

        // And another Data Element
        POST("dataModels/${id}/dataClasses/${dataClassId}/dataElements", [label: 'second data element in profile functional class', dataType: [id: dataTypeId]])
        verifyResponse(CREATED, response)
        String secondDataElementId = responseBody().id

        logout()

        ['dataModelId': id, 'dataTypeId': dataTypeId, 'dataClassId': dataClassId, 'firstDataElementId': firstDataElementId, 'secondDataElementId': secondDataElementId]
    }

    Map<String, String> getThirdDataModelIds() {
        loginCreator()

        // Create a Data Model
        POST("folders/${getTestFolderId()}/dataModels", [label: 'third profile functional model'])
        verifyResponse(CREATED, response)
        String id = responseBody().id
        addAccessShares(id, 'dataModels/')

        // Add a Data Type
        POST("dataModels/${id}/dataTypes", [label: 'profile functional data type', domainType: 'PrimitiveType'])
        verifyResponse(CREATED, response)
        String dataTypeId = responseBody().id

        // Add a Data Class
        POST("dataModels/${id}/dataClasses", [label: 'profile functional class'])
        verifyResponse(CREATED, response)
        String dataClassId = responseBody().id

        // Add a Data Element
        POST("dataModels/${id}/dataClasses/${dataClassId}/dataElements", [label: 'first data element in profile functional class', dataType: [id: dataTypeId]])
        verifyResponse(CREATED, response)
        String firstDataElementId = responseBody().id

        // And another Data Element
        POST("dataModels/${id}/dataClasses/${dataClassId}/dataElements", [label: 'second data element in profile functional class', dataType: [id: dataTypeId]])
        verifyResponse(CREATED, response)
        String secondDataElementId = responseBody().id

        logout()
        ['dataModelId': id, 'dataTypeId': dataTypeId, 'dataClassId': dataClassId, 'firstDataElementId': firstDataElementId, 'secondDataElementId': secondDataElementId]
    }

    String getDynamicProfileModelId() {

        loginCreator()
        POST("folders/${getTestFolderId()}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: 'Dynamic Profile Model'])
        verifyResponse(CREATED, response)
        String dynamicProfileModelId = responseBody().id
        addAccessShares(dynamicProfileModelId, 'dataModels/')

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

        PUT("dataModels/$dynamicProfileModelId/finalise", [versionChangeType: 'Major', versionTag: 'Functional Test Version Tag'])
        verifyResponse OK, response

        logout()

        dynamicProfileModelId
    }

    /**
     * A dynamic profile which is editable post finalisation
     * @return
     */
    String getSecondDynamicProfileModelId() {

        loginCreator()
        POST("folders/${getTestFolderId()}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: 'Second Dynamic Profile Model'])
        verifyResponse(CREATED, response)
        String dynamicProfileModelId = responseBody().id
        addAccessShares(dynamicProfileModelId, 'dataModels/')

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

        PUT("dataModels/$dynamicProfileModelId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        logout()

        dynamicProfileModelId
    }

    void finaliseDataModelId(String id) {
        loginCreator()
        PUT("dataModels/$id/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        logout()
    }

    void cleanupDataModelId(String id) {
        loginCreator()
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
    "name": "ImportedDataClassDynamicProfileProviderService",
    "version": "${json-unit.matches:version}",
    "displayName": "Unassigned Import Profile for DataClass",
    "namespace": "import.NOT_ASSIGNED.functional.testing",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      "import_id",
      "import_domainType",
      "import_path",
      "mandation",
      "multiplicity"
    ],
    "providerType": "Profile",
    "metadataNamespace": "import.NOT_ASSIGNED.functional.testing",
    "domains": [
      "DataClass"
    ],
    "editableAfterFinalisation": true
  },
  {
    "name": "ImportedDataElementDynamicProfileProviderService",
    "version": "${json-unit.matches:version}",
    "displayName": "Unassigned Import Profile for DataElement",
    "namespace": "import.NOT_ASSIGNED.functional.testing",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      "import_id",
      "import_domainType",
      "import_path",
      "mandation",
      "multiplicity"
    ],
    "providerType": "Profile",
    "metadataNamespace": "import.NOT_ASSIGNED.functional.testing",
    "domains": [
      "DataElement"
    ],
    "editableAfterFinalisation": true
  },
  {
    "name": "ImportedDataTypeDynamicProfileProviderService",
    "version": "${json-unit.matches:version}",
    "displayName": "Unassigned Import Profile for DataType",
    "namespace": "import.NOT_ASSIGNED.functional.testing",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      "import_id",
      "import_domainType",
      "import_path",
      "mandation",
      "multiplicity"
    ],
    "providerType": "Profile",
    "metadataNamespace": "import.NOT_ASSIGNED.functional.testing",
    "domains": [
      "DataType",
      "PrimitiveType",
      "EnumerationType",
      "ReferenceType",
      "ModelDataType"
    ],
    "editableAfterFinalisation": true
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
  },
  {
    "name": "InvalidProfileService",
    "version": "${json-unit.matches:version}",
    "displayName": "Partially Invalid Profile",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      "validField",
      "blankDescriptionField"
    ],
    "providerType": "Profile",
    "metadataNamespace": "uk.ac.ox.softeng.maurodatamapper.profile.invalid",
    "domains": [
      "DataModel"
    ],
    "editableAfterFinalisation": false
  },
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
  }
]'''
    }

    void 'N01 : test validating profile on DataModel (as reader)'() {
        given:
        String id = getDataModelId()
        Map namespaceFieldMap = [
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
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

    void 'N02 : test saving profile (as author)'() {
        given:
        String id = getDataModelId()
        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
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
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]

        when:
        loginAuthor()
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
        loginReader()
        GET("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${id}", STRING_ARG)

        then:
        verifyJsonResponse(OK, getExpectedSavedProfile())

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N03 : test editing profile (as author)'() {
        given:
        String id = getDataModelId()
        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
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
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        loginAuthor()
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

    void 'N04 : test deleting profile (as author)'() {
        given:
        String id = getDataModelId()
        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
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
            id        : id.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        loginAuthor()
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
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
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

    void 'N06 : test editing non-editable profile on finalised model (as editor)'() {
        given:
        String id = getDataModelId()

        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
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

    void 'N07 : test saving a post-finalised editable profile on finalised model (as author)'() {
        given:
        String id = getDataModelId()
        finaliseDataModelId(id)

        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            metadataPropertyName: 'domainsApplicable',
        ]
        Map notEditableAfterFinalisationFieldMap = [
            currentValue        : 'value after finalisation',
            metadataPropertyName: 'notEditableAfterFinalisedField',
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
        loginAuthor()
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

    void 'N08 : test editing post-finalised editable profile on finalised model (as author)'() {
        given:
        String id = getDataModelId()

        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: 'metadataNamespace',
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            metadataPropertyName: 'domainsApplicable',
        ]
        Map notEditableAfterFinalisationFieldMap = [
            currentValue        : 'value before finalisation',
            metadataPropertyName: 'notEditableAfterFinalisedField',
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
        loginAuthor()
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

    void 'N09 : test saving and getting profile with derived fields (as author and reader)'() {
        given:
        String id = getDataModelId()
        Map profileMap = [
            sections  : [
                [
                    fields: [
                        [
                            currentValue        : 'functional.test.profile',
                            metadataPropertyName: 'plainField',
                        ],
                        [
                            currentValue        : 'functional.test.profile',
                            metadataPropertyName: 'uneditableFieldOptional',
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
        loginAuthor()
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
        loginReader()
        GET("profiles/${derivedFieldProfileService.namespace}/${derivedFieldProfileService.name}/dataModels/${id}", STRING_ARG)

        then:
        verifyJsonResponse(OK, getExpectedDerivedSavedProfile())

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N10 : test saving a dynamic profile (as author)'() {
        given:
        String simpleModelId = getDataModelId()

        loginCreator()
        POST("folders/${getTestFolderId()}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: 'Dynamic Profile Model'])
        verifyResponse(CREATED, response)
        String dynamicProfileModelId = responseBody().id
        addAccessShares(dynamicProfileModelId, 'dataModels/')

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
        loginAuthor()
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${dynamicProfileModelId}", profileMap)

        then:
        verifyResponse(OK, response)

        when: 'the dynamic profile data model is finalised'
        loginCreator()
        PUT("dataModels/$dynamicProfileModelId/finalise", [versionChangeType: 'Major'])

        then: 'the response is OK'
        verifyResponse OK, response

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
            name: 'Dynamic%20Profile%20Model'
        ]
        loginAuthor()
        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", dynamicProfileMap)

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
        localResponse.body().first().name == 'Dynamic%20Profile%20Model'
        localResponse.body().first().namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        loginReader()
        GET("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", STRING_ARG)

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
          "currentValue": "abc",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
          "currentValue": "def",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
          "currentValue": "",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
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

    void 'N11 : test editing a dynamic profile (as author)'() {
        given:
        String simpleModelId = getDataModelId()

        loginCreator()
        POST("folders/${getTestFolderId()}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: 'Dynamic Profile Model'])
        verifyResponse(CREATED, response)
        String dynamicProfileModelId = responseBody().id
        addAccessShares(dynamicProfileModelId, 'dataModels/')

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

        PUT("dataModels/$dynamicProfileModelId/finalise", [versionChangeType: 'Major'])
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
            name: 'Dynamic%20Profile%20Model'
        ]
        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", dynamicProfileMap)
        verifyResponse(OK, response)

        when:
        loginAuthor()
        optionalFieldMap.currentValue = ''
        defaultOptionalFieldMap.currentValue = 'edited value'
        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == optionalFieldMap.fieldName}.currentValue == optionalFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == mandatoryFieldMap.fieldName}.currentValue == mandatoryFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == defaultOptionalFieldMap.fieldName}.currentValue == defaultOptionalFieldMap.currentValue

        cleanup:
        cleanupDataModelId(simpleModelId)
        cleanupDataModelId(dynamicProfileModelId)
    }

    void 'N12 : test validating valid dynamic profiles using validateMany (as reader)'() {
        given:
        Map<String, String> secondIds = getSecondDataModelIds()
        String secondModelId = secondIds['dataModelId']
        String secondModelFirstDataElementId = secondIds['firstDataElementId']
        String secondModelSecondDataElementId = secondIds['secondDataElementId']
        Map<String, String> thirdIds = getThirdDataModelIds()
        String thirdModelId = thirdIds['dataModelId']
        String thirdModelFirstDataElementId = thirdIds['firstDataElementId']
        String thirdModelSecondDataElementId = thirdIds['secondDataElementId']
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
                        label     : 'first data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Dynamic%20Profile%20Model'
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
                        label     : 'second data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Second%20Dynamic%20Profile%20Model'
                    ]
                ]
            ]
        ]

        loginReader()
        POST("dataModels/$secondModelId/profile/validateMany", dynamicProfileMap)

        then: 'the profiles are validated successfully'
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().profilesProvided.size == 2
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == optionalFieldMap1.fieldName}.currentValue == optionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap1.fieldName}.currentValue == mandatoryFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap1.fieldName}.currentValue == defaultOptionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.id == secondModelFirstDataElementId
        responseBody().profilesProvided[0].profile.domainType == 'DataElement'
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == optionalFieldMap2_1.fieldName}.currentValue == optionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap2_1.fieldName}.currentValue == mandatoryFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap2_1.fieldName}.currentValue == defaultOptionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.id == secondModelSecondDataElementId
        responseBody().profilesProvided[1].profile.domainType == 'DataElement'

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

    void 'N13 : test validating invalid dynamic profiles using validateMany (as reader)'() {
        given:
        Map<String, String> secondIds = getSecondDataModelIds()
        String secondModelId = secondIds['dataModelId']
        String secondModelFirstDataElementId = secondIds['firstDataElementId']
        String secondModelSecondDataElementId = secondIds['secondDataElementId']
        Map<String, String> thirdIds = getThirdDataModelIds()
        String thirdModelId = thirdIds['dataModelId']
        String thirdModelFirstDataElementId = thirdIds['firstDataElementId']
        String thirdModelSecondDataElementId = thirdIds['secondDataElementId']
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
                        label     : 'first data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Dynamic%20Profile%20Model'
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
                        label     : 'second data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Second%20Dynamic%20Profile%20Model'
                    ]
                ]
            ]
        ]

        loginReader()
        POST("dataModels/$secondModelId/profile/validateMany", dynamicProfileMap)

        then: 'the first profile has an error'
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().count == 2
        responseBody().profilesProvided.size == 2
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == optionalFieldMap1.fieldName}.currentValue == optionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap1.fieldName}.currentValue == mandatoryFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap1.fieldName}.currentValue == defaultOptionalFieldMap1.currentValue
        responseBody().profilesProvided[0].profile.id == secondModelFirstDataElementId
        responseBody().profilesProvided[0].profile.domainType == 'DataElement'
        responseBody().profilesProvided[0].errors.errors.size == 1
        responseBody().profilesProvided[0].errors.errors[0].message == 'This field cannot be empty'
        responseBody().profilesProvided[0].errors.errors[0].fieldName == "Dynamic Profile Elem (Mandatory)"

        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == optionalFieldMap2_1.fieldName}.currentValue == optionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap2_1.fieldName}.currentValue == mandatoryFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap2_1.fieldName}.currentValue == defaultOptionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.id == secondModelSecondDataElementId
        responseBody().profilesProvided[1].profile.domainType == 'DataElement'
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
    void 'N14 : test saving and retrieving dynamic profiles using saveMany and getMany (as author and reader)'() {
        given:
        Map<String, String> secondIds = getSecondDataModelIds()
        String secondModelId = secondIds['dataModelId']
        String secondModelFirstDataElementId = secondIds['firstDataElementId']
        String secondModelSecondDataElementId = secondIds['secondDataElementId']
        Map<String, String> thirdIds = getThirdDataModelIds()
        String thirdModelId = thirdIds['dataModelId']
        String thirdModelFirstDataElementId = thirdIds['firstDataElementId']
        String thirdModelSecondDataElementId = thirdIds['secondDataElementId']
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
                        label     : 'first data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Dynamic%20Profile%20Model'
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
                        label     : 'first data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Second%20Dynamic%20Profile%20Model'
                    ]
                ]
            ]
        ]

        /**
         * 1. First save two dynamic profiles against the first data element
         */

        when: 'use saveMany on a MultiFacetAwareItem that is not a Model'
        loginAuthor()
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
        responseBody().profilesProvided[0].profile.domainType == 'DataElement'
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == optionalFieldMap2_1.fieldName}.currentValue == optionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap2_1.fieldName}.currentValue == mandatoryFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap2_1.fieldName}.currentValue == defaultOptionalFieldMap2_1.currentValue
        responseBody().profilesProvided[1].profile.id == secondModelFirstDataElementId
        responseBody().profilesProvided[1].profile.domainType == 'DataElement'

        when: 'used profiles are retrieved for the first data element'
        HttpResponse<List<Map>> localResponse = GET("dataElements/$secondModelFirstDataElementId/profiles/used", Argument.listOf(Map))

        then: 'the saved profile is returned'
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 2
        localResponse.body()[0].name == 'Dynamic%20Profile%20Model'
        localResponse.body()[0].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'
        localResponse.body()[1].name == 'Second%20Dynamic%20Profile%20Model'
        localResponse.body()[1].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        loginReader()
        GET("dataElements/$secondModelFirstDataElementId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", STRING_ARG)

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
          "currentValue": "abc",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
          "currentValue": "def",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
          "currentValue": "",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
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
        loginAuthor()
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
                        label     : 'first data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Dynamic%20Profile%20Model'
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
                        label     : 'second data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Dynamic%20Profile%20Model'
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
        responseBody().profilesProvided[0].profile.domainType == 'DataElement'
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == optionalFieldMap2.fieldName}.currentValue == optionalFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap2.fieldName}.currentValue == mandatoryFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap2.fieldName}.currentValue == defaultOptionalFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.id == secondModelSecondDataElementId
        responseBody().profilesProvided[1].profile.domainType == 'DataElement'

        when:
        loginReader()
        localResponse = GET("dataElements/$secondModelFirstDataElementId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 2
        localResponse.body()[0].name == 'Dynamic%20Profile%20Model'
        localResponse.body()[0].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'
        localResponse.body()[1].name == 'Second%20Dynamic%20Profile%20Model'
        localResponse.body()[1].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataElements/$secondModelFirstDataElementId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", STRING_ARG)

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
          "currentValue": "abc updated",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
          "currentValue": "def updated",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
          "currentValue": "updated",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
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
        localResponse.body().first().name == 'Dynamic%20Profile%20Model'
        localResponse.body().first().namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataElements/$secondModelSecondDataElementId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", STRING_ARG)

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
          "currentValue": "xyz",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
          "currentValue": "pqr",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
          "currentValue": "onm",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
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
        loginAuthor()
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
                        label     : 'first data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Dynamic%20Profile%20Model'
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
                        label     : 'second data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Dynamic%20Profile%20Model'
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
                        label     : 'first data element in profile functional class',
                        domainType: 'DataElement'],
                    profileProviderService: [
                        namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
                        name: 'Second%20Dynamic%20Profile%20Model'
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
        responseBody().profilesProvided[0].profile.domainType == 'DataElement'
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == optionalFieldMap2.fieldName}.currentValue == optionalFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == mandatoryFieldMap2.fieldName}.currentValue == mandatoryFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.sections.first().fields.find {it.fieldName == defaultOptionalFieldMap2.fieldName}.currentValue == defaultOptionalFieldMap2.currentValue
        responseBody().profilesProvided[1].profile.id == secondModelSecondDataElementId
        responseBody().profilesProvided[1].profile.domainType == 'DataElement'

        when:
        loginReader()
        localResponse = GET("dataElements/$secondModelFirstDataElementId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 2
        localResponse.body()[0].name == 'Dynamic%20Profile%20Model'
        localResponse.body()[0].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'
        localResponse.body()[1].name == 'Second%20Dynamic%20Profile%20Model'
        localResponse.body()[1].namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataElements/$secondModelFirstDataElementId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", STRING_ARG)

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
          "currentValue": "abc updated again",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
          "currentValue": "def updated again",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
          "currentValue": "updated again",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
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
        localResponse.body().first().name == 'Dynamic%20Profile%20Model'
        localResponse.body().first().namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataElements/$secondModelSecondDataElementId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", STRING_ARG)

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
          "currentValue": "xyz updated",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Mandatory)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Mandatory)",
          "currentValue": "pqr updated",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
        },
        {
          "fieldName": "Dynamic Profile Elem (Default Optional)",
          "metadataPropertyName": "Profile Section Class/Dynamic Profile Elem (Default Optional)",
          "currentValue": "onm updated",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false
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
        when: 'request made against a MultiFacetAwareItem that is not a model'
        loginAuthor()
        Map getManyMap = [
            'multiFacetAwareItems'   : [
                [
                    'multiFacetAwareItemDomainType': 'dataElement',
                    'multiFacetAwareItemId'        : secondModelFirstDataElementId
                ],
                [
                    'multiFacetAwareItemDomainType': 'dataElement',
                    'multiFacetAwareItemId'        : secondModelSecondDataElementId
                ]
            ],
            'profileProviderServices': [
                [
                    'name'     : "Dynamic%20Profile%20Model",
                    'namespace': "uk.ac.ox.softeng.maurodatamapper.profile.provider"
                ]
            ]
        ]

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

    void 'N15 : test that a profile having two versions, each with version tags, can be retrieved by namespace and name only'() {
        given: 'a finalised profile and a simple data model'
        String simpleModelId = getDataModelId()
        String profileModelId = getDynamicProfileModelId()
        loginCreator()

        when: 'count the used profiles on the simple data model'
        HttpResponse<List<Map>> localResponse = GET("dataModels/${simpleModelId}/profiles/used", Argument.listOf(Map))

        then: 'the result is 0'
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 0

        when: 'get the unused profiles on the simple data model'
        localResponse = GET("dataModels/${simpleModelId}/profiles/unused", Argument.listOf(Map))

        then: '1 of these is for the Dynamic Profile Model'
        verifyResponse(OK, localResponse)
        localResponse.body().findAll{it.displayName == 'Dynamic Profile Model'}.size() == 1

        when: 'get the finalised profile against the simple data model'
        GET("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", STRING_ARG)

        then: 'the response is OK'
        verifyResponse(OK, jsonCapableResponse)

        when: 'create a new branch model version of the profile'
        PUT("dataModels/$profileModelId/newBranchModelVersion", [:], MAP_ARG)

        then: 'the response is CREATED'
        verifyResponse CREATED, response
        String profileModelVersion2Id = response.body().id

        when: 'get the unused profiles on the simple data model'
        localResponse = GET("dataModels/${simpleModelId}/profiles/unused", Argument.listOf(Map))

        then: 'still 1 of these is for the Dynamic Profile Model'
        verifyResponse(OK, localResponse)
        localResponse.body().findAll{it.displayName == 'Dynamic Profile Model'}.size() == 1

        when: 'get the finalised profile against the simple data model now there is a finalised and an unfinalised version of the profile'
        GET("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", MAP_ARG)

        then: 'the response is OK'
        verifyResponse(OK, response)

        when: 'finalise the new branch model version'
        PUT("dataModels/$profileModelVersion2Id/finalise", [versionChangeType: 'Major', versionTag: 'Functional Test Second Version Tag'])

        then: 'the response is OK'
        verifyResponse OK, response

        when: 'get the unused profiles on the simple data model'
        localResponse = GET("dataModels/${simpleModelId}/profiles/unused?latestVersionByMetadataNamespace=false", Argument.listOf(Map))

        then: 'now 2 of these are for the Dynamic Profile Model, because we get the finalised profile branch'
        verifyResponse(OK, localResponse)
        localResponse.body().findAll{it.displayName == 'Dynamic Profile Model'}.size() == 2

        when: 'get the finalised profile against the simple data model now that there are two versions of the profile'
        GET("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", MAP_ARG)

        then: 'the response is OK'
        verifyResponse(OK, response)

        cleanup:
        logout()
        cleanupDataModelId(simpleModelId)
        cleanupDataModelId(profileModelId)
    }

    void 'N16 : test validating and saving a valid profile with a partially invalid definition (as reader and author)'() {
        given:
        String id = getDataModelId()
        Map validFieldMap = [
            metadataPropertyName: 'validField',
            currentValue: 'text value'
        ]
        Map blankDescriptionFieldMap = [
            metadataPropertyName: 'blankDescriptionField',
            currentValue: 100
        ]
        Map profileMap = [
            sections : [
                [
                    name  : 'Partially Invalid Section',
                    fields: [
                        validFieldMap,
                        blankDescriptionFieldMap
                    ]
                ]
            ]
        ]

        when: 'validate valid data using a profile definition containing an invalid blank field description'
        loginReader()
        POST("profiles/${invalidProfileService.namespace}/${invalidProfileService.name}/dataModels/$id/validate", profileMap)

        then:
        verifyResponse(OK, response)

        when: 'save valid data using a profile definition containing an invalid blank field description'
        loginAuthor()
        POST("profiles/${invalidProfileService.namespace}/${invalidProfileService.name}/dataModels/$id", profileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.size() == 1
        responseBody().domainType == 'DataModel'
        responseBody().id == id
        responseBody().sections.first().name == profileMap.sections.first().name
        responseBody().sections.first().fields.find {it.metadataPropertyName == validFieldMap.metadataPropertyName}.currentValue == validFieldMap.currentValue
        responseBody().sections.first().fields.find {it.metadataPropertyName == blankDescriptionFieldMap.metadataPropertyName}.currentValue.toInteger() == blankDescriptionFieldMap.currentValue

        when: 'get saved data'
        loginReader()
        GET("profiles/${invalidProfileService.namespace}/${invalidProfileService.name}/dataModels/$id")

        then:
        verifyResponse(OK, response)
        responseBody().sections.size() == 1
        responseBody().domainType == 'DataModel'
        responseBody().id == id
        responseBody().sections.first().name == profileMap.sections.first().name
        responseBody().sections.first().fields.find {it.metadataPropertyName == validFieldMap.metadataPropertyName}.currentValue == validFieldMap.currentValue
        responseBody().sections.first().fields.find {it.metadataPropertyName == blankDescriptionFieldMap.metadataPropertyName}.currentValue.toInteger() == blankDescriptionFieldMap.currentValue

        cleanup:
        cleanupDataModelId(id)
    }

    void 'N17 : test validating and saving an invalid profile with a partially invalid definition (as reader)'() {
        given:
        String id = getDataModelId()
        Map validFieldMap = [
            metadataPropertyName: 'validField'
        ]
        Map blankDescriptionFieldMap = [
            metadataPropertyName: 'blankDescriptionField',
            currentValue: 'text value'
        ]
        Map profileMap = [
            sections : [
                [
                    name  : 'Partially Invalid Section',
                    fields: [
                        validFieldMap,
                        blankDescriptionFieldMap
                    ]
                ]
            ]
        ]

        when: 'validate invalid data using a profile definition containing an invalid blank field description'
        loginReader()
        POST("profiles/${invalidProfileService.namespace}/${invalidProfileService.name}/dataModels/$id/validate", profileMap)

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().errors
        responseBody().total == 2
        responseBody().fieldTotal == 2
        responseBody().errors.find {it.metadataPropertyName == validFieldMap.metadataPropertyName}.message == 'This field cannot be empty'
        responseBody().errors.find {it.metadataPropertyName == blankDescriptionFieldMap.metadataPropertyName}.message == 'This field must be a valid Integer'

        when: 'save invalid data using a profile definition containing an invalid blank field description'
        loginAuthor()
        POST("profiles/${invalidProfileService.namespace}/${invalidProfileService.name}/dataModels/$id", profileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.size() == 1
        responseBody().domainType == 'DataModel'
        responseBody().id == id
        responseBody().sections.first().name == profileMap.sections.first().name
        !responseBody().sections.first().fields.find {it.metadataPropertyName == validFieldMap.metadataPropertyName}.currentValue
        responseBody().sections.first().fields.find {it.metadataPropertyName == blankDescriptionFieldMap.metadataPropertyName}.currentValue == blankDescriptionFieldMap.currentValue

        when: 'get saved data'
        loginReader()
        GET("profiles/${invalidProfileService.namespace}/${invalidProfileService.name}/dataModels/$id")

        then:
        verifyResponse(OK, response)
        responseBody().sections.size() == 1
        responseBody().domainType == 'DataModel'
        responseBody().id == id
        responseBody().sections.first().name == profileMap.sections.first().name
        !responseBody().sections.first().fields.find {it.metadataPropertyName == validFieldMap.metadataPropertyName}.currentValue
        responseBody().sections.first().fields.find {it.metadataPropertyName == blankDescriptionFieldMap.metadataPropertyName}.currentValue == blankDescriptionFieldMap.currentValue

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
          "currentValue": "functional.test.profile",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false,
          "description": "The namespace under which properties of this profile will be stored"
        },
        {
          "fieldName": "Applicable for domains",
          "metadataPropertyName": "domainsApplicable",
          "currentValue": "DataModel",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false,
          "description": "Determines which types of catalogue item can be profiled using this profile.  For example, 'DataModel'.  ''' +
        '''Separate multiple domains with a semi-colon (';').  Leave blank to allow this profile to be applicable to any catalogue item."
        },
        {
          "fieldName": "Can be edited after finalisation",
          "metadataPropertyName": "editableAfterFinalisation",
          "currentValue": "",
          "dataType": "boolean",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false,
          "description": "Defines if the profile can be edited after the model has been finalised. This defaults to false."
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
          "currentValue": "profile functional model",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "uneditable": true,
          "editableAfterFinalisation": false,
          "derived": true,
          "derivedFrom": "label",
          "description": "A field which is derived"
        },
        {
          "fieldName": "Uneditable Field",
          "metadataPropertyName": "uneditableField",
          "currentValue": "",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "uneditable": true,
          "editableAfterFinalisation": false,
          "derived": false,
          "description": "A field which is uneditable and listed as mandatory"
        },
        {
          "fieldName": "Uneditable Field Optional",
          "metadataPropertyName": "uneditableFieldOptional",
          "currentValue": "",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 0,
          "uneditable": true,
          "editableAfterFinalisation": false,
          "derived": false,
          "description": "A field which is uneditable and listed as optional"
        },
        {
          "fieldName": "Plain Field",
          "metadataPropertyName": "plainField",
          "currentValue": "functional.test.profile",
          "dataType": "string",
          "maxMultiplicity": 1,
          "minMultiplicity": 1,
          "uneditable": false,
          "editableAfterFinalisation": false,
          "derived": false,
          "description": "A field which is normal"
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
