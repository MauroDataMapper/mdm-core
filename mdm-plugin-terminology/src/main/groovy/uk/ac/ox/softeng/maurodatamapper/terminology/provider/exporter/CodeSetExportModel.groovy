/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportModel
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet

import groovy.xml.Namespace

/**
 * @since 14/09/2020
 */
class CodeSetExportModel extends ExportModel {

    public static String getCurrentVersion(boolean isXml) {
        isXml ? new CodeSetXmlExporterService().version : new CodeSetJsonExporterService().version
    }

    CodeSetExportModel(CodeSet codeSet, ExportMetadata exportMetadata, boolean isXml) {
        super(getCurrentVersion(isXml))
        exportModelType = 'codeSet'
        modelExportTemplatePath = isXml ? '/codeSet/export.gml' : '/codeSet/export'
        modelExportMap = [export: codeSet, codeSet: codeSet]
        this.exportMetadata = exportMetadata
        modelXmlNamespace = new Namespace("http://maurodatamapper.com/codeSet/${getCurrentVersion(isXml)}", 'xmlns:mdm')
    }
}
