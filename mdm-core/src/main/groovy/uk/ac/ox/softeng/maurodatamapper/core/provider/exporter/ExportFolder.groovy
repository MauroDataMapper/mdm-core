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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder

class ExportFolder implements ExportResource {

    Map<String, Object> exportMap
    String domainType
    String templatePath
    ExportMetadata exportMetadata

    ExportFolder(Folder folder, Map<String, ByteArrayOutputStream> models, ExportMetadata exportMetadata) {
        this([export: folder, models: models], exportMetadata)
    }

    private ExportFolder(Map<String, Object> exportMap, ExportMetadata exportMetadata) {
        this.exportMap = exportMap
        domainType = 'folder'
        templatePath = "/${domainType}/export"
        this.exportMetadata = exportMetadata
    }
}
