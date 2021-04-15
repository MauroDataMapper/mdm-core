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
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.OK

@Slf4j
@Integration
class ProfileFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    Folder folder

    @Shared
    UUID complexDataModelId

    @Shared
    UUID simpleDataModelId

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
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
    "version":"1.0.0",
    "displayName":"Profile Specification Profile (Data Model)",
    "namespace":"uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys":false,
    "knownMetadataKeys": ["metadataNamespace","domainsApplicable"],
    "providerType":"Profile",
    "metadataNamespace":"uk.ac.ox.softeng.profile",
    "domains":["DataModel"]
}, 
{
    "name":"ProfileSpecificationProfileFieldService",
    "version":"1.0.0",
    "displayName":"Profile Specification Profile (Data Element)",
    "namespace":"uk.ac.ox.softeng.maurodatamapper.profile",
    "allowsExtraMetadataKeys":false,
    "knownMetadataKeys":["metadataPropertyName","defaultValue","regularExpression","editedAfterFinalisation"],
    "providerType":"Profile",
    "metadataNamespace":"uk.ac.ox.softeng.profile.field",
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
}
