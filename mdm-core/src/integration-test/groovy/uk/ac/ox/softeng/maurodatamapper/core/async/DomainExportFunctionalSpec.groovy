/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.async

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.spockframework.util.Assert
import spock.lang.Shared

/**
 * @since 15/03/2022
 */
@Slf4j
@Integration
class DomainExportFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    Folder folder

    DomainExportService domainExportService

    @Override
    String getResourcePath() {
        'domainExports'
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check resource count is {}', 0)
        folder = new Folder(label: 'testing', createdBy: StandardEmailAddress.FUNCTIONAL_TEST)
        GormUtils.checkAndSave(messageSource, folder)
        sessionFactory.currentSession.flush()
        if (DomainExport.count() != 0) {
            log.error('{} {} resources left over from previous test', [DomainExport.count(), DomainExport.simpleName].toArray())
            Assert.fail('Resources left over')
        }
    }

    @Transactional
    String createNewItem() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        byteArrayOutputStream.write('{"label": "testing"}'.bytes)
        domainExportService.createAndSaveNewDomainExport(new ExporterProviderService() {
            @Override
            ByteArrayOutputStream exportDomain(User currentUser, UUID domainId, Map<String, Object> parameters) throws ApiException {
                return null
            }

            @Override
            ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds, Map<String, Object> parameters) throws ApiException {
                return null
            }

            @Override
            Boolean canExportMultipleDomains() {
                true
            }

            @Override
            String getFileExtension() {
                'json'
            }

            @Override
            String getContentType() {
                grails.web.mime.MimeType.JSON.name
            }

            @Override
            String getDisplayName() {
                'Testing Exporter'
            }

            @Override
            String getVersion() {
                '1.0.0'
            }

            @Override
            String getName() {
                'DomainExportTestExporter'
            }
        }, folder, 'test.json', byteArrayOutputStream, admin).id.toString()
    }

    void 'R1 : Test the empty index action'() {
        when: 'The index action is requested'
        GET('')

        then: 'The response is correct'
        verifyResponse(HttpStatus.OK, response)
        assert response.body() == [count: 0, items: []]
    }

    void 'R3 : Test the index action with content'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem()

        when: 'List is called'
        GET('')

        then: 'We now list 1 due to public access'
        verifyResponse(HttpStatus.OK, response)
        assert response.body().count == 1
        assert response.body().items.size() == 1
        assert response.body().items[0].id == id

        cleanup:
        deleteDomainExport(id)
    }

    void 'R5 : Test the show action correctly renders an instance'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem()

        when: 'When the show action is called to retrieve a resource'
        GET(id, STRING_ARG)

        then: 'The response is correct'
        verifyJsonResponse(HttpStatus.OK, '''{
  "id": "${json-unit.matches:id}",
  "exported": {
    "domainType": "Folder",
    "domainId": "${json-unit.matches:id}"
  },
  "exporter": {
    "namespace": "uk.ac.ox.softeng.maurodatamapper.core.async",
    "name": "DomainExportTestExporter",
    "version": "1.0.0"
  },
  "export": {
    "fileName": "test.json",
    "contentType": "application/json",
    "fileSize": 20
  },
  "exportedOn": "${json-unit.matches:offsetDateTime}",
  "exportedBy": "admin@maurodatamapper.com",
  "links": {
     "relative": "${json-unit.regex}/api/domainExports/[\\\\w-]+?/download",
     "absolute": "${json-unit.regex}http://localhost:\\\\d+/api/domainExports/.+?/download"
  }
}
''')

        cleanup:
        deleteDomainExport(id)
    }

    void 'R6 : Test the delete action correctly cancels not deletes an instance'() {
        given: 'The save action is executed with valid data'
        String id = createNewItem()

        when: 'When the delete action is executed on an unknown instance'
        DELETE(UUID.randomUUID().toString())

        then: 'The response is correct'
        response.status == HttpStatus.NOT_FOUND

        when: 'When the delete action is executed on an existing instance'
        DELETE(id)

        then: 'The response is correct'
        response.status == HttpStatus.NO_CONTENT

        cleanup:
        deleteDomainExport(id)
    }

    @Transactional
    void deleteDomainExport(String id) {
        domainExportService.delete(id)
        assert domainExportService.count() == 0
    }
}
