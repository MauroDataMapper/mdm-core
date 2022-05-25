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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DefaultJsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
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

    DefaultJsonProfileProviderService profileSpecificationProfileService

    @Transactional
    Authority getTestAuthority() {
        Authority.findByDefaultAuthority(true)
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        folder.addToMetadata(new Metadata(namespace: 'test.namespace', key: 'propertyKey', value: 'propertyValue', createdBy: FUNCTIONAL_TEST))
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

    void '01 : test getting profile providers'() {
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

    void '02 : test get all models in profile which doesnt exist'() {
        when:
        GET("profiles/${getProfilePath()}/DataModel")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void '03 : test get all models values in profile which doesnt exist'() {
        when:
        GET("profiles/${getProfilePath()}/DataModel/values")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void '04 : test searching in profile which doesnt exist'() {
        when:
        POST("profiles/${getProfilePath()}/search", [searchTerm: 'test'])

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void '05 : test get profile for model which doesnt exist'() {
        given:
        String id = UUID.randomUUID().toString()

        when:
        GET("dataModels/${id}/profile/${getProfilePath()}")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'DataModel'
        responseBody().id == id
    }

    void '06 : test get profile for model when profile doesnt exist'() {
        given:
        String id = getComplexDataModelId()

        when:
        GET("dataModels/${id}/profile/${getProfilePath()}")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void '07 : test get profile for folder when profile doesnt exist'() {
        given:
        String id = folder.id.toString()

        when:
        GET("folders/${id}/profile/${getProfilePath()}")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void '08 : test save profile for model which doesnt exist'() {
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

    void '09 : test save profile for folder which doesnt exist'() {
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

    void '10 : test save profile for model when profile doesnt exist'() {
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

    void '11 : test getting unused profiles on datamodel'() {
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

    void '12 : test getting unused profiles on folder'() {
        given:
        String id = folder.id.toString()

        when:
        HttpResponse<List<Map>> localResponse = GET("folders/${id}/profiles/unused", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size() == 0
    }

    void '13 : test getting used profiles on datamodel'() {
        given:
        String id = getComplexDataModelId()

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${id}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size() == 0
    }

    void '14 : test getting used profiles on folder'() {
        given:
        String id = folder.id.toString()

        when:
        HttpResponse<List<Map>> localResponse = GET("folders/${id}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size() == 0
    }

    void '15 : test getting other properties on a datamodel'() {
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

    void '16 : test getting with filters other properties on a datamodel'() {
        given:
        String id = getComplexDataModelId()

        when: 'filter with a namespace that matches all three properties'
        GET("dataModels/${id}/profiles/otherMetadata?ns=test.com", STRING_ARG)

        then: 'all three properties are returned'
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

        when: 'filter with a namespace that matches just one property'
        GET("dataModels/${id}/profiles/otherMetadata?ns=test.com/test", STRING_ARG)

        then: 'the one matching property is returned'
        verifyJsonResponse OK, '''
{
    "count": 1,
    "items": [{
        "id":"${json-unit.matches:id}",
        "namespace":"test.com/test",
        "key":"mdk1",
        "value":"mdv2",
        "lastUpdated":"${json-unit.matches:offsetDateTime}"
    }
]}
'''

        when: 'filter by namespace and key'
        GET("dataModels/${id}/profiles/otherMetadata?ns=test.com&key=mdk1", STRING_ARG)

        then: 'two matching properties are returned'
        verifyJsonResponse OK, '''
{
    "count": 2,
    "items": [{
        "id":"${json-unit.matches:id}",
        "namespace":"test.com",
        "key":"mdk1",
        "value":"mdv1",
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

        when: 'filter by namespace and key and value'
        GET("dataModels/${id}/profiles/otherMetadata?ns=test.com&key=mdk1&value=mdv2", STRING_ARG)

        then: 'the one matching property is returned'
        verifyJsonResponse OK, '''
{
    "count": 1,
    "items": [{
        "id":"${json-unit.matches:id}",
        "namespace":"test.com/test",
        "key":"mdk1",
        "value":"mdv2",
        "lastUpdated":"${json-unit.matches:offsetDateTime}"
    }
]}
'''

        when: 'filter by namespace and key and value that do not match'
        GET("dataModels/${id}/profiles/otherMetadata?ns=test.com&key=mdk1&value=mdv3", STRING_ARG)

        then: 'no properties are returned'
        verifyJsonResponse OK, '''
{
    "count": 0,
    "items": [
]}
'''
    }

    void '17 : test getting other properties on a folder'() {
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
            metadataPropertyName: 'metadataNamespace'
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
            metadataPropertyName: 'metadataNamespace'
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
            metadataPropertyName: 'metadataNamespace'
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

    void 'N05 : test validating and saving a dynamic profile'() {
        given:
        String simpleModelId = getSimpleDataModelId()

        POST("folders/${folder.id}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: 'Dynamic Profile Model'])
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
        PUT("dataModels/$dynamicProfileModelId/finalise", [versionChangeType: 'Major', versionTag: 'Functional Test Version Tag'])

        then:
        verifyResponse OK, response

        when:
        HttpResponse<List<Map>> localResponse = GET('profiles/providers', Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().any {it.name == Utils.safeUrlEncode('Dynamic Profile Model')}

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
            sections: [
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
            name    : 'Dynamic%20Profile%20Model'
        ]

        POST("profiles/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model/dataModels/$simpleModelId/validate", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == optionalFieldMap.fieldName}.currentValue == optionalFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == mandatoryFieldMap.fieldName}.currentValue == mandatoryFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == defaultOptionalFieldMap.fieldName}.currentValue == defaultOptionalFieldMap.currentValue

        when:
        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == optionalFieldMap.fieldName}.currentValue == optionalFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == mandatoryFieldMap.fieldName}.currentValue == mandatoryFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == defaultOptionalFieldMap.fieldName}.currentValue == defaultOptionalFieldMap.currentValue

        when:
        localResponse = GET("dataModels/$simpleModelId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().first().name == 'Dynamic%20Profile%20Model'
        localResponse.body().first().namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
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
  "label": "Simple Test DataModel",
  "domainType": "DataModel"
}'''

        cleanup:
        DELETE("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model")
        verifyResponse(NO_CONTENT, response)
        DELETE("dataModels/$dynamicProfileModelId?permanent=true")
        verifyResponse(NO_CONTENT, response)
    }

    void 'N06 : test editing a dynamic profile'() {
        given:
        String simpleModelId = getSimpleDataModelId()

        POST("folders/${folder.id}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: 'Dynamic Profile Model'])
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
            name      : 'Dynamic%20Profile%20Model'
        ]
        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", dynamicProfileMap)
        verifyResponse(OK, response)

        when:
        optionalFieldMap.currentValue = ''
        defaultOptionalFieldMap.currentValue = 'edited value'
        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == optionalFieldMap.fieldName}.currentValue == optionalFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == mandatoryFieldMap.fieldName}.currentValue == mandatoryFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == defaultOptionalFieldMap.fieldName}.currentValue == defaultOptionalFieldMap.currentValue

        cleanup:
        DELETE("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/Dynamic%20Profile%20Model")
        verifyResponse(NO_CONTENT, response)
        DELETE("dataModels/$dynamicProfileModelId?permanent=true")
        verifyResponse(NO_CONTENT, response)
    }

    void 'N07 : test saving a dynamic profile with brackets in the name'() {
        given:
        String simpleModelId = getSimpleDataModelId()
        String label = "Dynamic Profile Model (Standard)"

        POST("folders/${folder.id}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: label])
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
            label     : label,
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name
        ]

        when:
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${dynamicProfileModelId}", profileMap)

        then:
        verifyResponse(OK, response)

        when:
        PUT("dataModels/$dynamicProfileModelId/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse OK, response

        when:
        HttpResponse<List<Map>> localResponse = GET('profiles/providers', Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().any {it.name == Utils.safeUrlEncode(label)}

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
            name      : Utils.safeUrlEncode(label)
        ]

        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == optionalFieldMap.fieldName}.currentValue == optionalFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == mandatoryFieldMap.fieldName}.currentValue == mandatoryFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == defaultOptionalFieldMap.fieldName}.currentValue == defaultOptionalFieldMap.currentValue

        when:
        localResponse = GET("dataModels/$simpleModelId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().first().name == Utils.safeUrlEncode(label)
        localResponse.body().first().namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        when:
        GET("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}", STRING_ARG)

        then:
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
  "label": "Simple Test DataModel",
  "domainType": "DataModel"
}'''

        cleanup:
        DELETE("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}")
        verifyResponse(NO_CONTENT, response)
        DELETE("dataModels/$dynamicProfileModelId?permanent=true")
        verifyResponse(NO_CONTENT, response)
    }

    void 'N08 : test validating and saving a dynamic profile with date, enumeration and custom datatypes'() {
        given:
        String simpleModelId = getSimpleDataModelId()
        String label = "Dynamic Profile Model (Standard)"

        POST("folders/${folder.id}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: label])
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

        POST("dataModels/$dynamicProfileModelId/dataTypes", [
            domainType       : 'EnumerationType',
            label            : 'Functional Test Enumeration',
            enumerationValues: [
                [key: 'a', value: 'wibble'],
                [key: 'b', value: 'wobble']
            ]
        ])
        verifyResponse(CREATED, response)
        String enumerationTypeId = responseBody().id

        POST("dataModels/$dynamicProfileModelId/dataTypes", [
            domainType       : 'PrimitiveType',
            label            : 'Functional Test Custom Type'
        ])
        verifyResponse(CREATED, response)
        String customTypeId = responseBody().id

        POST("dataModels/$dynamicProfileModelId/dataClasses/$dataClassId/dataElements", [
            label   : 'Dynamic Profile Elem (String)',
            dataType: dataTypes.string
        ])
        verifyResponse(CREATED, response)

        POST("dataModels/$dynamicProfileModelId/dataClasses/$dataClassId/dataElements", [
            label   : 'Dynamic Profile Elem (Date)',
            dataType: dataTypes.date
        ])
        verifyResponse(CREATED, response)

        POST("dataModels/$dynamicProfileModelId/dataClasses/$dataClassId/dataElements", [
            label   : 'Dynamic Profile Elem (Enumeration)',
            dataType: enumerationTypeId
        ])
        verifyResponse(CREATED, response)

        POST("dataModels/$dynamicProfileModelId/dataClasses/$dataClassId/dataElements", [
            label   : 'Dynamic Profile Elem (Custom)',
            dataType: customTypeId
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
            label     : label,
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name
        ]

        when:
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${dynamicProfileModelId}", profileMap)

        then:
        verifyResponse(OK, response)

        when:
        PUT("dataModels/$dynamicProfileModelId/finalise", [versionChangeType: 'Major'])

        then:
        verifyResponse OK, response

        when:
        HttpResponse<List<Map>> localResponse = GET('profiles/providers', Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().any {it.name == Utils.safeUrlEncode(label)}

        when:
        Map stringFieldMap = [
            fieldName   : 'Dynamic Profile Elem (String)',
            currentValue: 'functional test string'
        ]
        Map dateFieldMap = [
            fieldName   : 'Dynamic Profile Elem (Date)',
            currentValue: '31/12/1999'
        ]
        Map enumerationFieldMap = [
            fieldName   : 'Dynamic Profile Elem (Enumeration)',
            currentValue: 'a'
        ]
        Map customTypeFieldMap = [
            fieldName   : 'Dynamic Profile Elem (Custom)',
            currentValue: 'functional test custom'
        ]
        Map dynamicProfileMap = [
            sections  : [
                [
                    fields: [
                        stringFieldMap,
                        dateFieldMap,
                        enumerationFieldMap,
                        customTypeFieldMap
                    ],
                    name  : 'Profile Section Class'
                ]
            ],
            id        : simpleModelId,
            domainType: 'DataModel',
            namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
            name      : Utils.safeUrlEncode(label)
        ]

        POST("profiles/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}/dataModels/$simpleModelId/validate", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == stringFieldMap.fieldName}.currentValue == stringFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == dateFieldMap.fieldName}.currentValue == dateFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == enumerationFieldMap.fieldName}.currentValue == enumerationFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == customTypeFieldMap.fieldName}.currentValue == customTypeFieldMap.currentValue

        when:
        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}", dynamicProfileMap)

        then:
        verifyResponse(OK, response)
        responseBody().sections.first().fields.find {it.fieldName == stringFieldMap.fieldName}.currentValue == stringFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == dateFieldMap.fieldName}.currentValue == dateFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == enumerationFieldMap.fieldName}.currentValue == enumerationFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == customTypeFieldMap.fieldName}.currentValue == customTypeFieldMap.currentValue

        when:
        localResponse = GET("dataModels/$simpleModelId/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().first().name == Utils.safeUrlEncode(label)
        localResponse.body().first().namespace == 'uk.ac.ox.softeng.maurodatamapper.profile.provider'

        cleanup:
        DELETE("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}")
        verifyResponse(NO_CONTENT, response)
        DELETE("dataModels/$dynamicProfileModelId?permanent=true")
        verifyResponse(NO_CONTENT, response)
    }

    void 'N09 : test that multiple profile versions are ordered correctly'() {
        given:
        String simpleModelId = getSimpleDataModelId()
        String label = 'Dynamic Profile Model'

        POST("folders/${folder.id}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: label])
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
            label          : 'Dynamic Profile Elem',
            dataType       : dataTypes.string
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
            label     : label,
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name
        ]

        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${dynamicProfileModelId}", profileMap)
        verifyResponse(OK, response)

        List<String> dynamicProfileModelIds = [dynamicProfileModelId]
        (1..5).each {
            PUT("dataModels/${dynamicProfileModelIds.last()}/finalise", [versionChangeType: 'Major'])
            verifyResponse OK, response
            PUT("dataModels/${dynamicProfileModelIds.last()}/newBranchModelVersion", [:], MAP_ARG)
            verifyResponse CREATED, response
            dynamicProfileModelIds << responseBody().id
        }

        when:
        HttpResponse<List<Map>> localResponse = GET('profiles/providers', Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.size() == 5
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.eachWithIndex {profileProviderMap, i ->
            assert Version.from(profileProviderMap.version).major == i + 1
        }

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/unused?latestVersionByMetadataNamespace=false", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.size() == 5
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.eachWithIndex {profileProviderMap, i ->
            assert Version.from(profileProviderMap.version).major == i + 1
        }

        when:
        Map dynamicProfileMap = [
            sections  : [
                [
                    fields: [
                        [
                            fieldName   : 'Dynamic Profile Elem',
                            currentValue: 'functional test value'
                        ]
                    ],
                    name  : 'Profile Section Class'
                ]
            ],
            id        : simpleModelId,
            domainType: 'DataModel',
            namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
            name      : Utils.safeUrlEncode(label)
        ]

        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}", dynamicProfileMap)

        then:
        verifyResponse OK, response

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/used", Argument.listOf(Map))

        then:
        localResponse.body().eachWithIndex {profileProviderMap, i ->
            assert Version.from(profileProviderMap.version).major == i + 1
        }

        cleanup:
        DELETE("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}")
        verifyResponse(NO_CONTENT, response)
        dynamicProfileModelIds.each {id ->
            DELETE("dataModels/$id?permanent=true")
            verifyResponse(NO_CONTENT, response)
        }
    }

    void 'N10 : test getting used and unused profiles with 2 profile versions'() {
        given: 'create version 1 and 2 of a dynamic profile'
        String simpleModelId = getSimpleDataModelId()
        String label = 'Dynamic Profile Model'

        POST("folders/${folder.id}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: label])
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
            label   : 'Dynamic Profile Elem',
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
            label     : label,
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name
        ]

        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${dynamicProfileModelId}", profileMap)
        verifyResponse(OK, response)

        PUT("dataModels/${dynamicProfileModelId}/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("dataModels/${dynamicProfileModelId}/newBranchModelVersion", [:], MAP_ARG)
        verifyResponse CREATED, response

        String newDynamicProfileModelId = responseBody().id
        PUT("dataModels/${newDynamicProfileModelId}/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${simpleModelId}/profiles/unused", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.size == 1
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.every {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('2')
        }

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/unused?latestVersionByMetadataNamespace=false", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.size == 2
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('1')
        }
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('2')
        }

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size() == 0

        when: 'save the dynamic profile'
        Map dynamicProfileMap = [
            sections  : [
                [
                    fields: [
                        [
                            fieldName   : 'Dynamic Profile Elem',
                            currentValue: 'functional test value'
                        ]
                    ],
                    name  : 'Profile Section Class'
                ]
            ],
            id        : simpleModelId,
            domainType: 'DataModel',
            namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
            name      : Utils.safeUrlEncode(label)
        ]

        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}", dynamicProfileMap)

        then:
        verifyResponse OK, response

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size == 2
        localResponse.body().any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('1')
        }
        localResponse.body().any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('2')
        }

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/used?latestVersionByMetadataNamespace=true", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size == 1
        localResponse.body().every {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('2')
        }

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/unused", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.size == 0

        cleanup:
        DELETE("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}")
        verifyResponse(NO_CONTENT, response)
        [dynamicProfileModelId, newDynamicProfileModelId].each {id ->
            DELETE("dataModels/$id?permanent=true")
            verifyResponse(NO_CONTENT, response)
        }
    }

    void 'N11 : test getting used and unused profiles with multiple profile versions with different metadata namespaces'() {
        given: 'create version 1 and 2 of a dynamic profile with a metadata namespace, and version 3 with a different metadata namespace'
        String simpleModelId = getSimpleDataModelId()
        String label = 'Dynamic Profile Model'

        POST("folders/${folder.id}/dataModels?defaultDataTypeProvider=ProfileSpecificationDataTypeProvider", [label: label])
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
            label   : 'Dynamic Profile Elem',
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
            label     : label,
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name
        ]

        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${dynamicProfileModelId}", profileMap)
        verifyResponse(OK, response)

        PUT("dataModels/${dynamicProfileModelId}/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("dataModels/${dynamicProfileModelId}/newBranchModelVersion", [:], MAP_ARG)
        verifyResponse CREATED, response

        String newDynamicProfileModelId = responseBody().id
        PUT("dataModels/${newDynamicProfileModelId}/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("dataModels/${dynamicProfileModelId}/newBranchModelVersion", [:], MAP_ARG)
        verifyResponse CREATED, response

        String newMetadataNamespaceDynamicProfileModelId = responseBody().id
        namespaceFieldMap.currentValue = 'functional.test.profile.new'
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${newMetadataNamespaceDynamicProfileModelId}", profileMap)
        verifyResponse(OK, response)
        PUT("dataModels/${newMetadataNamespaceDynamicProfileModelId}/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        when:
        HttpResponse<List<Map>> localResponse = GET("dataModels/${simpleModelId}/profiles/unused", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.size == 2
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('2') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile'
        }
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('3') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile.new'
        }

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/unused?latestVersionByMetadataNamespace=false", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.size == 3
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('1') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile'
        }
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('2') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile'
        }
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('3') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile.new'
        }

        when: 'save version 2 of the dynamic profile'
        Map dynamicProfileMap = [
            sections  : [
                [
                    fields: [
                        [
                            fieldName   : 'Dynamic Profile Elem',
                            currentValue: 'functional test value'
                        ]
                    ],
                    name  : 'Profile Section Class'
                ]
            ],
            id        : simpleModelId,
            domainType: 'DataModel',
            namespace : 'uk.ac.ox.softeng.maurodatamapper.profile.provider',
            name      : Utils.safeUrlEncode(label)
        ]

        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}/2.0.0", dynamicProfileMap)

        then:
        verifyResponse OK, response

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size == 2
        localResponse.body().any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('1') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile'
        }
        localResponse.body().any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('2') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile'
        }

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/used?latestVersionByMetadataNamespace=true", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size == 1
        localResponse.body().every {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('2') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile'
        }

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/unused", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.size == 1
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('3') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile.new'
        }

        when: 'save version 3 of the dynamic profile'
        POST("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}/3.0.0", dynamicProfileMap)

        then:
        verifyResponse OK, response

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/used", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size == 3
        localResponse.body().any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('1') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile'
        }
        localResponse.body().any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('2') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile'
        }
        localResponse.body().any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('3') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile.new'
        }

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/used?latestVersionByMetadataNamespace=true", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().size == 2
        localResponse.body().any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('2') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile'
        }
        localResponse.body().any {profileProviderMap ->
            Version.from(profileProviderMap.version) == Version.from('3') &&
            profileProviderMap.metadataNamespace == 'functional.test.profile.new'
        }

        when:
        localResponse = GET("dataModels/${simpleModelId}/profiles/unused", Argument.listOf(Map))

        then:
        verifyResponse OK, localResponse
        localResponse.body().findAll {it.name == Utils.safeUrlEncode(label)}.size == 0

        cleanup:
        DELETE("dataModels/$simpleModelId/profile/uk.ac.ox.softeng.maurodatamapper.profile.provider/${Utils.safeUrlEncode(label)}")
        verifyResponse(NO_CONTENT, response)
        [dynamicProfileModelId, newDynamicProfileModelId, newMetadataNamespaceDynamicProfileModelId].each {id ->
            DELETE("dataModels/$id?permanent=true")
            verifyResponse(NO_CONTENT, response)
        }
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
  "label": "Simple Test DataModel",
  "domainType": "DataModel"
}'''
    }
}
