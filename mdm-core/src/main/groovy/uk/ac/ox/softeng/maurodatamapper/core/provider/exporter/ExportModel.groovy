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
package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import groovy.xml.Namespace

class ExportModel {

    Map<String, Object> exportMap
    String modelType
    String templatePath
    ExportMetadata exportMetadata
    Namespace xmlNamespace
    Namespace modelXmlNamespace

    ExportModel(MdmDomain mdmDomain, String modelType, String version, ExportMetadata exportMetadata) {
        this([export: mdmDomain], modelType, '', version, version, '', exportMetadata)
    }

    ExportModel(List<MdmDomain> mdmDomains, String modelType, String multiModelType, String version, ExportMetadata exportMetadata) {
        this([export: mdmDomains], modelType, multiModelType, version, version, '', exportMetadata)
    }

    ExportModel(MdmDomain mdmDomain, String modelType, String version, String templateFileExtension, ExportMetadata exportMetadata) {
        this([export: mdmDomain], modelType, '', version, version, templateFileExtension, exportMetadata)
    }

    ExportModel(MdmDomain mdmDomain, String modelType, String version, String modelVersion, String templateFileExtension, ExportMetadata exportMetadata) {
        this([export: mdmDomain], modelType, '', version, modelVersion, templateFileExtension, exportMetadata)
    }

    ExportModel(List<MdmDomain> mdmDomains, String modelType, String multiModelType, String version, String modelVersion, String templateFileExtension,
                ExportMetadata exportMetadata) {
        this([export: mdmDomains], modelType, multiModelType, version, modelVersion, templateFileExtension, exportMetadata)
    }

    private ExportModel(Map<String, Object> exportMap, String modelType, String multiModelType, String version, String modelVersion, String templateFileExtension,
                        ExportMetadata exportMetadata) {
        this.exportMap = exportMap
        this.modelType = exportMap.export instanceof List ? multiModelType : modelType
        templatePath = "/${modelType}/export${templateFileExtension ? ".$templateFileExtension" : ''}"
        this.exportMetadata = exportMetadata
        xmlNamespace = new Namespace("http://maurodatamapper.com/export/${version}", 'xmlns:exp')
        modelXmlNamespace = new Namespace("http://maurodatamapper.com/${modelType}/${modelVersion}", 'xmlns:mdm')
    }

    Map<String, String> getXmlNamespaces() {
        [
            (xmlNamespace.prefix)     : xmlNamespace.uri,
            (modelXmlNamespace.prefix): modelXmlNamespace.uri
        ]
    }
}
