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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportModel
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.TemplateBasedExporter
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet

import grails.plugin.markup.view.MarkupViewTemplateEngine
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 14/09/2020
 */
class CodeSetXmlExporterService extends CodeSetExporterProviderService implements TemplateBasedExporter {

    public static final CONTENT_TYPE = 'application/mauro.codeset+xml'

    @Autowired
    MarkupViewTemplateEngine templateEngine

    @Override
    String getDisplayName() {
        'XML CodeSet Exporter'
    }

    @Override
    String getVersion() {
        '5.0'
    }

    @Override
    String getFileExtension() {
        'xml'
    }

    @Override
    String getContentType() {
        CONTENT_TYPE
    }

    @Override
    int getOrder() {
        HIGHEST_PRECEDENCE + 1
    }

    @Override
    Boolean canExportMultipleDomains() {
        true
    }

    @Override
    String getExportViewPath() {
        '/exportModel/exportCodeSet'
    }

    @Override
    ByteArrayOutputStream exportCodeSet(User currentUser, CodeSet codeSet, Map<String, Object> parameters) throws ApiException {
        ExportMetadata exportMetadata = new ExportMetadata(this, currentUser.firstName, currentUser.lastName)
        exportModel(new ExportModel(codeSet, 'codeSet', version, '4.0', 'gml', exportMetadata), contentType)
    }

    @Override
    ByteArrayOutputStream exportCodeSets(User currentUser, List<CodeSet> codeSets, Map<String, Object> parameters) throws ApiException {
        ExportMetadata exportMetadata = new ExportMetadata(this, currentUser.firstName, currentUser.lastName)
        exportModel(new ExportModel(codeSets, 'codeSet', 'codeSets', version, '4.0', 'gml', exportMetadata), contentType)
    }
}
