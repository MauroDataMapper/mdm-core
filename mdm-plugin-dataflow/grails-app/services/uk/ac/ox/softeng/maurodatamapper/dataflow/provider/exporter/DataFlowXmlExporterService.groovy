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
package uk.ac.ox.softeng.maurodatamapper.dataflow.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportModel
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.TemplateBasedExporter
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.plugin.markup.view.MarkupViewTemplateEngine
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 07/01/2021
 */
class DataFlowXmlExporterService extends DataFlowExporterProviderService implements TemplateBasedExporter {

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
    String getDisplayName() {
        'XML DataFlow Exporter'
    }

    @Override
    String getVersion() {
        '4.0'
    }

    @Override
    ByteArrayOutputStream exportDataFlow(User currentUser, DataFlow dataFlow) throws ApiException {
        ExportMetadata exportMetadata = new ExportMetadata(this, currentUser.firstName, currentUser.lastName)
        exportModel new ExportModel(dataFlow, 'dataFlow', version, 'gml', exportMetadata), fileType
    }

    @Override
    ByteArrayOutputStream exportDataFlows(User currentUser, List<DataFlow> dataFlow) throws ApiException {
        throw new ApiBadRequestException('XES01', "${getName()} cannot export multiple DataFlows")
    }


    @Override
    String getExportViewPath() {
        '/exportModel/exportDataFlow'
    }
}