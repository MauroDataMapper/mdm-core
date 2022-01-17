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
package uk.ac.ox.softeng.maurodatamapper.core.container.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportFolder
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.TemplateBasedExporter
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.plugin.json.view.JsonViewTemplateEngine
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class FolderJsonExporterService extends FolderExporterProviderService implements TemplateBasedExporter {

    @Autowired
    JsonViewTemplateEngine templateEngine

    @Override
    String getDisplayName() {
        'JSON Folder Exporter'
    }

    @Override
    String getVersion() {
        '1.0'
    }

    @Override
    String getFileType() {
        'text/json'
    }

    @Override
    String getFileExtension() {
        'json'
    }

    @Override
    Boolean canExportMultipleDomains() {
        false
    }

    @Override
    ByteArrayOutputStream exportFolder(User currentUser, Folder folder) throws ApiException {
        ExportMetadata exportMetadata = new ExportMetadata(this, currentUser.firstName, currentUser.lastName)
        exportFolder(new ExportFolder(folder, exportMetadata), fileType)
    }

    @Override
    ByteArrayOutputStream exportFolders(User currentUser, List<Folder> folders) throws ApiException {
        throw new ApiBadRequestException('FBIP04', "${name} cannot export multiple Folders")
    }
}
