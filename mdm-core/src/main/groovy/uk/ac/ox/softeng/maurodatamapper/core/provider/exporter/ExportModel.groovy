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
package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.xml.Namespace

class ExportModel {

    Namespace xmlNamespace
    Map modelExportMap
    ExportMetadata exportMetadata
    String modelExportTemplatePath
    String exportModelType
    Namespace modelXmlNamespace

    ExportModel(CatalogueItem model, String modelType, String version, ExportMetadata exportMetadata) {
        this(model, modelType, version, '', exportMetadata)
    }

    ExportModel(CatalogueItem model, String modelType, String version, String templatePathFileExtension, ExportMetadata exportMetadata) {
        this(model, modelType, version, version, templatePathFileExtension, exportMetadata)
    }

    ExportModel(CatalogueItem model, String modelType, String version, String modelVersion, String templatePathFileExtension,
                ExportMetadata exportMetadata) {
        this.exportMetadata = exportMetadata
        this.exportModelType = modelType
        this.xmlNamespace = new Namespace("http://maurodatamapper.com/export/${version}", 'xmlns:exp')
        this.modelXmlNamespace = new Namespace("http://maurodatamapper.com/$exportModelType/$modelVersion", 'xmlns:mdm')
        this.modelExportTemplatePath = "/$exportModelType/export${templatePathFileExtension ? ".$templatePathFileExtension" : ''}"
        this.modelExportMap = [export: model]
        this.modelExportMap[exportModelType] = model
    }

    Map getXmlNamespaces() {
        Map ns = [:]
        ns[xmlNamespace.prefix] = xmlNamespace.uri
        ns[modelXmlNamespace.prefix] = modelXmlNamespace.uri
        ns
    }
}

