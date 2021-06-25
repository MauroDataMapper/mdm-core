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
package uk.ac.ox.softeng.maurodatamapper.federation.publish

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import java.time.OffsetDateTime
import java.time.ZoneOffset

@Slf4j
@Integration
class PublishFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    String folderId

    @Override
    String getResourcePath() {
        'published'
    }

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data for FeedFunctionalSpec')
        folderId = new Folder(label: 'Functional Test Folder', createdBy: StandardEmailAddress.FUNCTIONAL_TEST).save(flush: true).id.toString()
        assert folderId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec PublishFunctionalSpec')
        cleanUpResources(DataModel, Folder)
    }

    void 'test getting published models'() {

        when:
        GET('models')

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().authority.label == 'Mauro Data Mapper'
        responseBody().authority.url == 'http://localhost'
        responseBody().lastUpdated
        responseBody().publishedModels.isEmpty()

    }

    void 'test getting published models when model available'() {
        given:
        OffsetDateTime publishedDate = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        String publishedDateStr = OffsetDateTimeConverter.toString(publishedDate)
        POST("folders/${folderId}/dataModels", [
            label             : 'FunctionalTest DataModel',
            readableByEveryone: true,
            finalised         : true,
            dateFinalised     : publishedDateStr,
            description       : 'Some random desc',
            modelVersion      : '1.0.0'
        ], MAP_ARG, true)
        verifyResponse(HttpStatus.CREATED, response)
        String id = responseBody().id

        when:
        GET('models')

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().authority.label == 'Mauro Data Mapper'
        responseBody().authority.url == 'http://localhost'
        responseBody().lastUpdated
        responseBody().publishedModels.size() == 1

        when:
        Map<String, Object> publishedModel = responseBody().publishedModels.first()

        then:
        publishedModel.modelId == id
        publishedModel.title == 'FunctionalTest DataModel 1.0.0'
        publishedModel.description == 'Some random desc'
        publishedModel.modelType == 'DataModel'
        publishedModel.datePublished == publishedDateStr
        publishedModel.lastUpdated
        publishedModel.dateCreated
    }
}
