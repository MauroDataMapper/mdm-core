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
package uk.ac.ox.softeng.maurodatamapper.federation.atom

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlComparer

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Shared

import java.time.OffsetDateTime

@Slf4j
@Integration
class FeedFunctionalSpec extends BaseFunctionalSpec implements XmlComparer {

    @Shared
    String folderId

    @Override
    String getResourcePath() {
        'feeds'
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
        log.debug('CleanupSpec FeedFunctionalSpec')
        cleanUpResources(DataModel, Folder)
    }

    void 'test getting published models when no models available'() {
        when:
        HttpResponse<String> localResponse = GET('all', STRING_ARG)

        then:
        verifyResponse(HttpStatus.OK, localResponse)
        log.warn(localResponse.body())
    }

    void 'test getting published models when model available'() {
        given:
        POST("folders/${folderId}/dataModels", [
            label             : 'FunctionalTest DataModel',
            readableByEveryone: true,
            finalised         : true,
            dateFinalised     : OffsetDateTimeConverter.toString(OffsetDateTime.now()),
            modelVersion      : '1.0.0'
        ], MAP_ARG, true)
        verifyResponse(HttpStatus.CREATED, response)

        when:
        HttpResponse<String> localResponse = GET('all', STRING_ARG)

        then:
        verifyResponse(HttpStatus.OK, localResponse)
        log.warn(localResponse.body())
    }
}
