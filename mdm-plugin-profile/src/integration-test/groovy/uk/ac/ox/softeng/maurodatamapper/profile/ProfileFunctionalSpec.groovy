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

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        folder.addToMetadata(new Metadata(namespace: "test.namespace", key: "propertyKey", value: "propertyValue", createdBy: FUNCTIONAL_TEST))
        checkAndSave(folder)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: FUNCTIONAL_TEST)
        checkAndSave(testAuthority)

        complexDataModelId = BootstrapModels.buildAndSaveComplexDataModel(messageSource, folder, testAuthority).id
        simpleDataModelId = BootstrapModels.buildAndSaveSimpleDataModel(messageSource, folder, testAuthority).id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataFlowFunctionalSpec')
        cleanUpResources(DataModel, Folder)
        Authority.findByLabel('Test Authority').delete(flush: true)
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
    "version":"SNAPSHOT",
    "displayName":"Profile Specification Profile (Data Model)",
    "namespace":"uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys":false,
    "knownMetadataKeys": ["metadataNamespace","domainsApplicable"],
    "providerType":"Profile",
    "metadataNamespace":"uk.ac.ox.softeng.maurodatamapper.profile",
    "domains":["DataModel"]
}, 
{
    "name":"ProfileSpecificationFieldProfileService",
    "version":"SNAPSHOT",
    "displayName":"Profile Specification Profile (Data Element)",
    "namespace":"uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys":false,
    "knownMetadataKeys":["metadataPropertyName","defaultValue","regularExpression","editedAfterFinalisation"],
    "providerType":"Profile",
    "metadataNamespace":"uk.ac.ox.softeng.maurodatamapper.profile.dataelement",
    "domains":["DataElement"]
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
            fieldName           : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            fieldName           : 'Applicable for domains',
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
        responseBody().errors.first().message == 'Value cannot be null'
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
            fieldName           : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            fieldName           : 'Applicable for domains',
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
        responseBody().sections.first().fields.find {it.fieldName == namespaceFieldMap.fieldName}.currentValue == namespaceFieldMap.currentValue
        responseBody().sections.first().fields.find {it.fieldName == domainsFieldMap.fieldName}.currentValue == domainsFieldMap.currentValue

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
            fieldName           : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            fieldName           : 'Applicable for domains',
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
                    sectionDescription: 'The details necessary for this Data Model to be used as the specification for a dynamic profile.',
                    fields            : [
                        namespaceFieldMap,
                    ],
                    sectionName       : 'Profile Specification'
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
            id        : simpleDataModelId.toString(),
            label     : 'Simple Test DataModel',
            domainType: 'DataModel',
            namespace : profileSpecificationProfileService.namespace,
            name      : profileSpecificationProfileService.name

        ]
        POST("profiles/${profileSpecificationProfileService.namespace}/${profileSpecificationProfileService.name}/dataModels/${simpleDataModelId}", profileMap)
        verifyResponse(OK, response)

        then:
        responseBody().sections.first().fields.find {it.fieldName == namespaceFieldMap.fieldName}.currentValue == 'functional.test.profile.adjusted'
        responseBody().sections.first().fields.find {it.fieldName == domainsFieldMap.fieldName}.currentValue == ''

    }

    void 'N04 : test deleting profile'() {
        given:
        Map namespaceFieldMap = [
            currentValue        : 'functional.test.profile',
            fieldName           : 'Metadata namespace',
        ]
        Map domainsFieldMap = [
            currentValue        : 'DataModel',
            fieldName           : 'Applicable for domains',
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
  "label": "Simple Test DataModel",
  "domainType": "DataModel"
}'''
    }
}
