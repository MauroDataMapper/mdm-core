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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportModel
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.TemplateBasedExporter
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.plugin.markup.view.MarkupViewTemplateEngine
import org.springframework.beans.factory.annotation.Autowired

class ReferenceDataXmlExporterService extends ReferenceDataModelExporterProviderService implements TemplateBasedExporter {

    @Autowired
    MarkupViewTemplateEngine templateEngine

    @Override
    String getFileExtension() {
        'xml'
    }

    @Override
    String getFileType() {
        'text/xml'
    }

    @Override
    String getProducesContentType() {
        'application/mauro.referencedatamodel+xml'
    }

    @Override
    int getOrder() {
        HIGHEST_PRECEDENCE + 1
    }

    @Override
    String getDisplayName() {
        'XML Reference Data Exporter'
    }

    @Override
    String getVersion() {
        '5.0'
    }

    @Override
    ByteArrayOutputStream exportReferenceDataModel(User currentUser, ReferenceDataModel referenceDataModel, Map<String, Object> parameters) throws ApiException {
        ExportMetadata exportMetadata = new ExportMetadata(this, currentUser.firstName, currentUser.lastName)
        exportModel(new ExportModel(referenceDataModel, 'referenceDataModel', version, 'gml', exportMetadata), fileType)
    }

    @Override
    ByteArrayOutputStream exportReferenceDataModels(User currentUser, List<ReferenceDataModel> referenceDataModels, Map<String, Object> parameters) throws ApiException {
        throw new ApiBadRequestException('XES01', "${getName()} cannot export multiple ReferenceDataModels")
    }

    /**
     * Necessary to override the view path because using '/exportModel/export' across more than one plugin results
     * in path resolution problems i.e. grails can pick up the template from a different plugin.
     */
    @Override
    String getExportViewPath() {
        '/exportModel/exportReferenceData'
    }
}