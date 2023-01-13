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
package uk.ac.ox.softeng.maurodatamapper.federation

import uk.ac.ox.softeng.maurodatamapper.federation.test.BaseSubscribedCatalogueFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

@Integration
@Slf4j
class AtomSubscribedCatalogueFunctionalSpec extends BaseSubscribedCatalogueFunctionalSpec {
    //note: using a groovy string like "http://localhost:$serverPort/" causes the url to be stripped when saving
    @Override
    Map getValidJson() {
        [
            url                    : "http://localhost:$serverPort/api/feeds/all".toString(),
            apiKey                 : '67421316-66a5-4830-9156-b1ba77bba5d1',
            label                  : 'Functional Test Label',
            subscribedCatalogueType: 'Atom',
            subscribedCatalogueAuthenticationType: 'API Key',
            description            : 'Functional Test Description',
            refreshPeriod          : 7
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            url   : 'wibble',
            apiKey: '67421316-66a5-4830-9156-b1ba77bba5d1'
        ]
    }

    //note: any-string on the Url is a workaround after the previous note
    @Override
    String getExpectedShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "url": "${json-unit.any-string}",
  "label": 'Functional Test Label',
  "subscribedCatalogueType": 'Atom',
  "subscribedCatalogueAuthenticationType": 'API Key',
  "description": 'Functional Test Description',
  "refreshPeriod": 7,
  "apiKey": "67421316-66a5-4830-9156-b1ba77bba5d1"
}'''
    }

    @Override
    String getExpectedOpenAccessShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "url": "${json-unit.any-string}",
  "label": 'Functional Test Label',
  "subscribedCatalogueType": 'Atom',
  "subscribedCatalogueAuthenticationType": 'API Key',
  "description": 'Functional Test Description',
  "refreshPeriod": 7
}'''
    }

    @Override
    String getExpectedOpenAccessIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "url": "${json-unit.any-string}",
      "label": "Functional Test Label",
      "subscribedCatalogueType": 'Atom',
      "subscribedCatalogueAuthenticationType": 'API Key',
      "description": "Functional Test Description",
      "refreshPeriod": 7
    }
  ]
}'''
    }

    void 'P01 : Test the publishedModels endpoint'() {
        given:
        POST('', getValidJson())
        verifyResponse(CREATED, response)
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels", MAP_ARG, true)

        then:
        verifyResponse OK, response
        verifyBaseJsonResponse(responseBody(), true)
        responseBody().items.size() == 1

        and:
        verifyJsonPublishedModel(responseBody().items.find {it.label == 'Finalised Example Test DataModel 1.0.0'}, 'dataModels', getDataModelExporters())

        cleanup:
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }

    void 'N01 : Test the newerVersions endpoint (with no newer versions)'() {
        given:
        POST('', getValidJson())
        verifyResponse(CREATED, response)
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedSimpleDataModelId}/newerVersions", MAP_ARG, true)

        then: 'newer versions not supported using Atom catalogues'
        verifyResponse OK, response
        !responseBody()

        cleanup:
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }

    void 'N02 : Test the newerVersions endpoint (with newer versions)'() {
        given:
        Tuple tuple = getNewerDataModelIds()
        POST('', getValidJson())
        verifyResponse(CREATED, response)
        String subscribedCatalogueId = responseBody().id

        when:
        GET("subscribedCatalogues/${subscribedCatalogueId}/publishedModels/${finalisedSimpleDataModelId}/newerVersions", MAP_ARG, true)

        then: 'newer versions not supported using Atom catalogues'
        verifyResponse OK, response
        !responseBody()

        cleanup:
        DELETE("dataModels/${tuple.v1}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/${tuple.v2}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE(subscribedCatalogueId)
        verifyResponse NO_CONTENT, response
    }

    private void verifyJsonPublishedModel(Map publishedModel, String modelEndpoint, Map<String, String> exporters) {
        assert publishedModel
        assert publishedModel.modelId ==~ /urn:uuid:\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
        assert publishedModel.label
        assert OffsetDateTime.parse(publishedModel.datePublished, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert OffsetDateTime.parse(publishedModel.lastUpdated, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assert publishedModel.links.each {link ->
            assert link.contentType
            String exporterUrl = exporters.get(link.contentType)
            assert link.url ==~ /http:\/\/localhost:$serverPort\/api\/$modelEndpoint\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}\/export\\/$exporterUrl/
        }
    }
}

