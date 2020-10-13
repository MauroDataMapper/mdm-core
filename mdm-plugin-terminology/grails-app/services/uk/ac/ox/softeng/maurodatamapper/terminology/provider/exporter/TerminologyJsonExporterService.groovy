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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.TemplateBasedExporter
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.plugin.json.view.JsonViewTemplateEngine
import org.springframework.beans.factory.annotation.Autowired

class TerminologyJsonExporterService extends TerminologyExporterProviderService implements TemplateBasedExporter {

    @Autowired
    JsonViewTemplateEngine templateEngine

    @Override
    String getFileExtension() {
        'json'
    }

    @Override
    String getFileType() {
        'text/json'
    }

    @Override
    String getDisplayName() {
        'JSON Terminology Exporter'
    }

    @Override
    String getVersion() {
        '3.0'
    }

    @Override
    ByteArrayOutputStream exportTerminology(User currentUser, Terminology terminology) throws ApiException {
        ExportMetadata exportMetadata = new ExportMetadata(this, currentUser.firstName, currentUser.lastName)
        exportModel new TerminologyExportModel(terminology, exportMetadata, false), fileType
    }

    @Override
    ByteArrayOutputStream exportTerminologies(User currentUser, List<Terminology> terminology) throws ApiException {
        throw new ApiBadRequestException('TES01', "${getName()} cannot export multiple Terminologies")
    }
}