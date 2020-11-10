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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.order.Ordered
import io.micronaut.core.type.Argument

import static io.micronaut.http.HttpStatus.OK

import spock.lang.PendingFeature

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderController* Controller: mauroDataMapperServiceProvider
 *  | GET | /api/admin/providers/exporters   | Action: exporterProviders   |
 *  | GET | /api/admin/providers/emailers    | Action: emailProviders      |
 *  | GET | /api/admin/providers/dataLoaders | Action: dataLoaderProviders |
 *  | GET | /api/admin/providers/importers   | Action: importerProviders   |
 */
@Integration
@Slf4j
class MauroDataMapperServiceProviderFunctionalSpec extends BaseFunctionalSpec {

    @Override
    String getResourcePath() {
        'admin/providers'
    }

    @PendingFeature(reason = "Not yet implemented")
    void 'test get exporters'() {
        when:
        GET('exporters', Argument.of(String))

        then:
        verifyJsonResponse(OK, '''[
  {
    "name": "JsonExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON DataModel Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "DataModelExporter",
    "fileExtension": "json",
    "fileType": "text/json",
    "canExportMultipleDomains": false
  },
  {
    "name": "XmlExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML DataModel Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "DataModelExporter",
    "fileExtension": "xml",
    "fileType": "text/xml",
    "canExportMultipleDomains": false
  }
]''')
    }

    void 'test get emailers'() {
        when:
        GET('emailers', Argument.of(String))

        then:
        verifyJsonResponse(OK, '''[
  {
    "order": ''' + Ordered.LOWEST_PRECEDENCE + ''',
    "providerType": "Email",
    "knownMetadataKeys": [
      
    ],
    "displayName": "Basic Email Provider",
    "name": "BasicEmailProviderService",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.core.provider.email",
    "allowsExtraMetadataKeys": true,
    "version": "${json-unit.matches:version}"
  }
]''')
    }

    void 'test get importers'() {
        when:
        GET('importers', Argument.of(String))

        then:
        verifyJsonResponse(OK, '''[
  {
    "name": "XmlImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML Reference Data Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "ReferenceDataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "CsvImporterService",
    "version": "3.0",
    "displayName": "CSV Reference Data Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer",
    "allowsExtraMetadataKeys": false,
    "knownMetadataKeys": [
      
    ],
    "providerType": "ReferenceDataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "JsonImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON Reference Data Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "ReferenceDataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  }
]''')
    }

    void 'test get dataloaders'() {
        when:
        GET('dataLoaders', Argument.of(String))

        then:
        verifyJsonResponse(OK, '[]')
    }
}