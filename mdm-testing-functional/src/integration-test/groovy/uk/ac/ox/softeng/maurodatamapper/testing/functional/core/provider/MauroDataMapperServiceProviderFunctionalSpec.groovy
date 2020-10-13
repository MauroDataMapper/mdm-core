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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.provider

import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.order.Ordered
import io.micronaut.core.type.Argument
import spock.lang.Unroll

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: mauroDataMapperServiceProvider
 *  |  GET  | /api/admin/providers/exporters    | Action: exporterProviders
 *  |  GET  | /api/admin/providers/emailers     | Action: emailProviders
 *  |  GET  | /api/admin/providers/dataLoaders  | Action: dataLoaderProviders
 *  |  GET  | /api/admin/providers/importers    | Action: importerProviders
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderController
 */
@Integration
@Slf4j
class MauroDataMapperServiceProviderFunctionalSpec extends FunctionalSpec {

    @Override
    String getResourcePath() {
        'admin/providers'
    }

    @Unroll
    void '#method:#endpoint endpoint are admin access only'() {
        when: 'Unlogged in call to check'
        this."$method"(endpoint)

        then: 'The response is Unauth'
        verifyForbidden response

        when: 'logged in as normal user'
        loginAuthenticated()
        response = this."$method"(endpoint)

        then: 'The response is Unauth'
        verifyForbidden response


        where:
        method | endpoint
        'GET'  | 'exporters'
        'GET'  | 'emailers'
        'GET'  | 'dataLoaders'
        'GET'  | 'importers'
    }

    void 'test get exporters'() {
        when:
        loginAdmin()
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
  },
  {
    "name": "TerminologyJsonExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON Terminology Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "TerminologyExporter",
    "fileExtension": "json",
    "fileType": "text/json",
    "canExportMultipleDomains": false
  },
  {
    "name": "TerminologyXmlExporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML Terminology Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "TerminologyExporter",
    "fileExtension": "xml",
    "fileType": "text/xml",
    "canExportMultipleDomains": false
  },
  {
    "name": "CodeSetXmlExporterService",
    "version": "3.0",
    "displayName": "XML CodeSet Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "CodeSetExporter",
    "fileExtension": "xml",
    "fileType": "text/xml",
    "canExportMultipleDomains": false
  },
  {
    "name": "CodeSetJsonExporterService",
    "version": "3.0",
    "displayName": "JSON CodeSet Exporter",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "CodeSetExporter",
    "fileExtension": "json",
    "fileType": "text/json",
    "canExportMultipleDomains": false
  }
]''')
    }

    void 'test get emailers'() {
        when:
        loginAdmin()
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
        loginAdmin()
        GET('importers', Argument.of(String))

        then:
        verifyJsonResponse(OK, '''[
  {
    "name": "JsonImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON DataModel Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "DataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "XmlImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML DataModel Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "DataModelImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters",
    "canImportMultipleDomains": true
  },
  {
    "name": "TerminologyJsonImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "JSON Terminology Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "TerminologyImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "TerminologyXmlImporterService",
    "version": "${json-unit.matches:version}",
    "displayName": "XML Terminology Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "TerminologyImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "CodeSetXmlImporterService",
    "version": "3.0",
    "displayName": "XML CodeSet Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "CodeSetImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  },
  {
    "name": "CodeSetJsonImporterService",
    "version": "3.0",
    "displayName": "JSON CodeSet Importer",
    "namespace": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer",
    "allowsExtraMetadataKeys": true,
    "knownMetadataKeys": [
      
    ],
    "providerType": "CodeSetImporter",
    "paramClassType": "uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters",
    "canImportMultipleDomains": false
  }
]''')
    }

    void 'test get dataloaders'() {
        when:
        loginAdmin()
        GET('dataLoaders', Argument.of(String))

        then:
        verifyJsonResponse(OK, '[]')
    }
}