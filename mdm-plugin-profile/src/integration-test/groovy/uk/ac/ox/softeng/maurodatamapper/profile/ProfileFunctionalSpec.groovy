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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus

import static io.micronaut.http.HttpStatus.OK

@Slf4j
@Integration
class ProfileFunctionalSpec extends BaseFunctionalSpec {

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel('Complex Test DataModel').id.toString()
    }

    @Transactional
    String getSimpleDataModelId() {
        DataModel.findByLabel('Simple Test DataModel').id.toString()
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
        verifyJsonResponse OK, '''[]'''
    }

    void 'test get all models in profile which doesnt exist'() {
        when:
        GET("profiles/${getProfilePath()}/models")

        then:
        verifyResponse HttpStatus.NOT_FOUND, response
        responseBody().resource == 'ProfileProviderService'
        responseBody().id == getProfileId()
    }

    void 'test get all models values in profile which doesnt exist'() {
        when:
        GET("profiles/${getProfilePath()}/values")

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
