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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.federation.publish


import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: publish
 *  |   GET   | /api/published/models       | Action: index
 * </pre>
 *
 *
 * @see uk.ac.ox.softeng.maurodatamapper.federation.publish.PublishController
 */
@Integration
@Slf4j
class PublishFunctionalSpec extends FunctionalSpec {

    @Override
    String getResourcePath() {
        'published'
    }

    void 'Get published models when not logged in'() {

        when:
        GET('models')

        then: "The response is OK with no entries"
        verifyResponse(OK, response)
        responseBody().authority.label == 'Mauro Data Mapper'
        responseBody().authority.url == 'http://localhost'
        responseBody().lastUpdated
        responseBody().publishedModels.isEmpty()
    }

    void 'Get published models when logged in as reader'() {

        given:
        loginReader()

        when:
        GET('models')

        then:
        verifyResponse(OK, response)
        responseBody().authority.label == 'Mauro Data Mapper'
        responseBody().authority.url == 'http://localhost'
        responseBody().lastUpdated
        responseBody().publishedModels.size() == 2


        and:
        verifyEntry(responseBody().publishedModels.find { it.title == 'Simple Test CodeSet 1.0.0' }, 'CodeSet')
        verifyEntry(responseBody().publishedModels.find { it.title == 'Finalised Example Test DataModel 1.0.0' }, 'DataModel')
    }

    private void verifyEntry(Map publishedModel, String modelType) {
        assert publishedModel
        assert publishedModel.modelId.toString() ==~ /\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
        assert publishedModel.modelType == modelType
        assert publishedModel.datePublished
        assert publishedModel.lastUpdated
        assert publishedModel.dateCreated
    }
}
