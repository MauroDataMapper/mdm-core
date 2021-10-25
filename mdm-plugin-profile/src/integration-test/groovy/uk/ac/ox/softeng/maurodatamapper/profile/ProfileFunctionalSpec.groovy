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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
@Integration
class ProfileFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    Folder folder

    @Shared
    UUID complexDataModelId

    @Shared
    UUID simpleDataModelId

    ProfileSpecificationProfileService profileSpecificationProfileService

    @Transactional
    Authority getTestAuthority() {
        Authority.findByDefaultAuthority(true)
    }

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        folder.addToMetadata(new Metadata(namespace: "test.namespace", key: "propertyKey", value: "propertyValue", createdBy: FUNCTIONAL_TEST))
        checkAndSave(folder)

        complexDataModelId = BootstrapModels.buildAndSaveComplexDataModel(messageSource, folder, testAuthority).id
        simpleDataModelId = BootstrapModels.buildAndSaveSimpleDataModel(messageSource, folder, testAuthority).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataFlowFunctionalSpec')
        cleanUpResources(DataModel, Folder)
    }

    @Override
    String getResourcePath() {
        ''
    }

    String getProfilePath() {
        'uk.ac.ox.softeng.maurodatamapper.plugins.profile/testingProfile'
    }

    String getProfileId() {
        getProfilePath().replace('/', ':')
    }

    void 'test getting profile providers'() {
        when:
        GET('profiles/providers', STRING_ARG)

        then:
        verifyJsonResponse OK, '''
[{
    "name":"ProfileSpecificationProfileService",
    "version":"${json-unit.matches:version}",
    "displayName":"Profile Specification Profile (Data Model)",
    "namespace":"uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys":false,
    "knownMetadataKeys": ["metadataNamespace","domainsApplicable","editableAfterFinalisation"],
    "providerType":"Profile",
    "metadataNamespace":"uk.ac.ox.softeng.maurodatamapper.profile",
    "domains":["DataModel"],
    "editableAfterFinalisation": false
}, 
{
    "name":"ProfileSpecificationFieldProfileService",
    "version":"${json-unit.matches:version}",
    "displayName":"Profile Specification Profile (Data Element)",
    "namespace":"uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys":false,
    "knownMetadataKeys":["metadataPropertyName","defaultValue","regularExpression","editableAfterFinalisation"],
    "providerType":"Profile",
    "metadataNamespace":"uk.ac.ox.softeng.maurodatamapper.profile.dataelement",
    "domains":["DataElement"],
    "editableAfterFinalisation": false
}]'''
    }

    void 'test get all models in profile which doesnt exist'() {
        when:
        GET("profiles/${getProfilePath()}/DataModel")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void 'test get all models values in profile which doesnt exist'() {
        when:
        GET("profiles/${getProfilePath()}/DataModel/values")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void 'test searching in profile which doesnt exist'() {
        when:
        POST("profiles/${getProfilePath()}/search", [searchTerm: 'test'])

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void 'test get profile for model which doesnt exist'() {
        given:
        String id = UUID.randomUUID().toString()

        when:
        GET("dataModels/${id}/profile/${getProfilePath()}")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'DataModel'
        responseBody().id == id
    }

    void 'test get profile for model when profile doesnt exist'() {
        given:
        String id = getComplexDataModelId()

        when:
        GET("dataModels/${id}/profile/${getProfilePath()}")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void 'test get profile for folder when profile doesnt exist'() {
        given:
        String id = folder.id.toString()

        when:
        GET("folders/${id}/profile/${getProfilePath()}")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void 'test save profile for model which doesnt exist'() {
        given:
        String id = UUID.randomUUID().toString()

        when:
        POST("dataModels/${id}/profile/${getProfilePath()}",
             [description: 'test desc', publisher: 'FT'])

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'DataModel'
        responseBody().id == id
    }

    void 'test save profile for folder which doesnt exist'() {
        given:
        String id = UUID.randomUUID().toString()

        when:
        POST("folders/${id}/profile/${getProfilePath()}",
             [description: 'test desc', publisher: 'FT'])

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'Folder'
        responseBody().id == id
    }

    void 'test save profile for model when profile doesnt exist'() {
        given:
        String id = getComplexDataModelId()

        when:
        POST("dataModels/${id}/profile/${getProfilePath()}",
             [description: 'test desc', publisher: 'FT'])

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void 'test getting unused profiles on datamodel'() {
        given:
        String id = getComplexDataModelId()

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${id}/profiles/unused", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size() == 1
        localResponse.body().first().name == 'ProfileSpecificationProfileService'
        localResponse.body().first().displayName == 'Profile Specification Profile (Data Model)'
    }

    void 'test getting unused profiles on folder'() {
        given:
        String id = folder.id.toString()

        when:
        HttpResponse<List<Map>> localResponse = GET("folders/${id}/profiles/unused", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size() == 0
    }

    void 'test getting used profiles on datamodel'() {
        given:
        String id = getComplexDataModelId()

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${id}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size() == 0
    }

    void 'test getting used profiles on folder'() {
        given:
        String id = folder.id.toString()

        when:
        HttpResponse<List<Map>> localResponse = GET("folders/${id}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size() == 0
    }

    void 'test getting other properties on a datamodel'() {
        given:
        String id = getComplexDataModelId()

        when:
        GET("dataModels/${id}/profiles/otherMetadata", STRING_ARG)

        then:
        verifyJsonResponse OK, '''
{
    "count": 3,
    "items": [{
        "id":"${json-unit.matches:id}",
        "namespace":"test.com",
        "key":"mdk1",
        "value":"mdv1",
        "lastUpdated":"${json-unit.matches:offsetDateTime}"
    },{
        "id":"${json-unit.matches:id}",
        "namespace":"test.com",
        "key":"mdk2",
        "value":"mdv2",
        "lastUpdated":"${json-unit.matches:offsetDateTime}"
    },{
        "id":"${json-unit.matches:id}",
        "namespace":"test.com/test",
        "key":"mdk1",
        "value":"mdv2",
        "lastUpdated":"${json-unit.matches:offsetDateTime}"
    }
]}
'''
    }

    void 'test getting other properties on a folder'() {
        given:
        String id = folder.id.toString()

        when:
        GET("folders/${id}/profiles/otherMetadata", STRING_ARG)

        then:
        verifyJsonResponse OK, '''
{
    "count": 1,
    "items": [{
        "id":"${json-unit.matches:id}",
        "namespace":"test.namespace",
        "key":"propertyKey",
        "value":"propertyValue",
        "lastUpdated":"${json-unit.matches:offsetDateTime}"
    }
]}
'''
    }

    void 'N01 : test validating profile on DataModel'() {
        given:
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
            id        : simpleDataModelId.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]

        when:
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${simpleDataModelId}/validate", profileMap)

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().total == 1
        responseBody().errors.first().message == 'This field cannot be empty'
        responseBody().errors.first().fieldName == 'Metadata namespace'
        responseBody().errors.first().metadataPropertyName == 'metadataNamespace'

        when:
        namespaceFieldMap.currentValue = 'functional.test.profile'
        domainsFieldMap.currentValue = 'DataModel'

        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${simpleDataModelId}/validate", profileMap)

        then:
        verifyResponse(OK, response)
    }

    void 'N02 : test saving profile'() {
        given:
        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: "metadataNamespace"
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
            id        : simpleDataModelId.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]

        when:
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${simpleDataModelId}", profileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.metadataPropertyName == namespaceFieldMap.metadataPropertyName}.currentValue == namespaceFieldMap.currentValue
        responseBody().sections.first().fields.find {it.metadataPropertyName == domainsFieldMap.metadataPropertyName}.currentValue == domainsFieldMap.currentValue

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${simpleDataModelId}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().first().name == profileSpecificationProfileService.name
        localResponse.body().first().namespace == profileSpecificationProfileService.namespace

        when:
        GET("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${simpleDataModelId}", STRING_ARG)

        then:
        verifyJsonResponse(OK, getExpectedSavedProfile())
    }

    void 'N03 : test editing profile'() {
        given:
        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: "metadataNamespace"
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
            id        : simpleDataModelId.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${simpleDataModelId}", profileMap)
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
            id        : simpleDataModelId.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${simpleDataModelId}", profileMap)
        verifyResponse(OK, response)

        then:
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
            id        : simpleDataModelId.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${simpleDataModelId}", profileMap)
        verifyResponse(OK, response)

        then:
        responseBody().sections.first().fields.find {it.metadataPropertyName == namespaceFieldMap.metadataPropertyName}.currentValue == 'functional.test.profile.adjusted'
        responseBody().sections.first().fields.find {it.metadataPropertyName == domainsFieldMap.metadataPropertyName}.currentValue == ''

    }

    void 'N04 : test deleting profile'() {
        given:
        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            metadataPropertyName: "metadataNamespace"
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
            id        : simpleDataModelId.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${simpleDataModelId}", profileMap)
        verifyResponse(OK, response)


        when:
        DELETE("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${simpleDataModelId}")

        then:
        verifyResponse(NO_CONTENT, response)

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${simpleDataModelId}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().isEmpty()
    }

    void 'N05 : test saving a dynamic profile (as editor)'() {
        given:
        String simpleModelId = getSimpleDataModelId()

        POST("folders/${folder.id}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: "Dynamic Profile Model"])
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
  "label": "Simple Test DataModel",
  "domainType": "DataModel"
}'''

        cleanup:
        DELETE("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model")
        verifyResponse(NO_CONTENT, response)
        DELETE("dataModels/$dynamicProfileModelId?permanent=true")
        verifyResponse(NO_CONTENT, response)
    }

    void 'N06 : test editing a dynamic profile'() {
        given:
        String simpleModelId = getSimpleDataModelId()

        POST("folders/${folder.id}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: "Dynamic Profile Model"])
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
        DELETE("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic+Profile+Model")
        verifyResponse(NO_CONTENT, response)
        DELETE("dataModels/$dynamicProfileModelId?permanent=true")
        verifyResponse(NO_CONTENT, response)
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
          "description": "Determines which types of catalogue item can be profiled using this profile.  For example, 'DataModel'.  ''' +
        '''Separate multiple domains with a semi-colon (';').  Leave blank to allow this profile to be applicable to any catalogue item.",
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
  "label": "Simple Test DataModel",
  "domainType": "DataModel"
}'''
    }
}
