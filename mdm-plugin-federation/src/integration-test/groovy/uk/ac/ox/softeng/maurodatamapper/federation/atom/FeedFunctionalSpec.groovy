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
package uk.ac.ox.softeng.maurodatamapper.federation.atom

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.rest.converter.json.OffsetDateTimeConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.xml.XmlComparer

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import grails.web.mime.MimeType
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

    @RunOnce
    @Transactional
    def setup() {
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
        HttpResponse<String> localResponse = GET('all', STRING_ARG, false, MimeType.ATOM_XML.name)

        then:
        verifyResponse(HttpStatus.OK, localResponse)
        log.warn(prettyPrint(localResponse.body()))
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
        HttpResponse<String> localResponse = GET('all', STRING_ARG, false, MimeType.ATOM_XML.name)

        then:
        verifyResponse(HttpStatus.OK, localResponse)
        log.warn(prettyPrint(localResponse.body()))
    }

    String expectedNoModelsAtom() {
        '''<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>Mauro Data Mapper - All Models</title>
  <id>tag:localhost,2021-01-27:/api/feeds/all</id>
  <author>
    <name>Mauro Data Mapper</name>
    <uri>http://localhost</uri>
  </author>
  <updated>2022-01-13T09:56:22Z</updated>
  <link rel="self" href="http://localhost:51077/api/feeds/all" hreflang="en" title="Mauro Data Mapper - All Models"/>
  <link rel="alternate" href="http://localhost:51077/api/feeds/all" hreflang="en" title="Mauro Data Mapper - All Models"/>
</feed>'''
    }

    String expectedModelsAtom() {
        '''<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>Mauro Data Mapper - All Models</title>
  <id>tag:localhost,2021-01-27:/api/feeds/all</id>
  <author>
    <name>Mauro Data Mapper</name>
    <uri>http://localhost</uri>
  </author>
  <updated>2022-01-13T10:22:26Z</updated>
  <link rel="self" href="http://localhost:51386/api/feeds/all" hreflang="en" title="Mauro Data Mapper - All Models"/>
  <link rel="alternate" href="http://localhost:51386/api/feeds/all" hreflang="en" title="Mauro Data Mapper - All Models"/>
  <entry>
    <id>urn:uuid:91c4b8fb-a6f8-477c-b42c-5f6dd510c3e6</id>
    <title>FunctionalTest DataModel 1.0.0</title>
    <updated>2022-01-13T10:22:26Z</updated>
    <published>2022-01-13T10:22:26Z</published>
    <category term="DataModel"/>
    <link rel="self" href="http://localhost:51386/api/dataModels/91c4b8fb-a6f8-477c-b42c-5f6dd510c3e6" hreflang="en"/>
    <link rel="alternate" href="http://localhost:51386/api/dataModels/91c4b8fb-a6f8-477c-b42c-5f6dd510c3e6" hreflang="en"/>
  </entry>
</feed>'''
    }
}
