/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportModel
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.TemplateBasedExporter
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology

import grails.plugin.json.view.JsonViewTemplateEngine
import org.springframework.beans.factory.annotation.Autowired

class TerminologyJsonExporterService extends TerminologyExporterProviderService implements TemplateBasedExporter {

    public static final CONTENT_TYPE = 'application/mauro.terminology+json'

    @Autowired
    JsonViewTemplateEngine templateEngine

    @Override
    String getDisplayName() {
        'JSON Terminology Exporter'
    }

    @Override
    String getVersion() {
        '4.0'
    }

    @Override
    String getFileExtension() {
        'json'
    }

    @Override
    String getContentType() {
        CONTENT_TYPE
    }

    @Override
    int getOrder() {
        HIGHEST_PRECEDENCE
    }

    @Override
    Boolean canExportMultipleDomains() {
        true
    }

    @Override
    ByteArrayOutputStream exportTerminology(User currentUser, Terminology terminology, Map<String, Object> parameters) throws ApiException {
        ExportMetadata exportMetadata = new ExportMetadata(this, currentUser.firstName, currentUser.lastName)
        exportModel(new ExportModel(terminology, 'terminology', version, exportMetadata), contentType)
    }

    @Override
    ByteArrayOutputStream exportTerminologies(User currentUser, List<Terminology> terminologies, Map<String, Object> parameters) throws ApiException {
        ExportMetadata exportMetadata = new ExportMetadata(this, currentUser.firstName, currentUser.lastName)
        exportModel(new ExportModel(terminologies, 'terminology', 'terminologies', version, exportMetadata), contentType)
    }
}
