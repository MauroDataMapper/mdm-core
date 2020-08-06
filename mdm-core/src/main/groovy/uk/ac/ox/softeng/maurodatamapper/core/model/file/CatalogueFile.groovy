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
package uk.ac.ox.softeng.maurodatamapper.core.model.file

import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware

import grails.compiler.GrailsCompileStatic
import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import org.springframework.web.multipart.MultipartFile

import java.nio.file.Files
import java.nio.file.Path

@GrailsCompileStatic
trait CatalogueFile implements EditHistoryAware {

    @BindUsing({
        obj, source ->
            if (source['fileContents'] instanceof MultipartFile) {
                obj.fileName = obj.fileName ?: ((MultipartFile) source['fileContents']).originalFilename
            }
            source['fileContents']
    })
    byte[] fileContents
    String fileName
    Long fileSize
    String fileType
    /*
        static constraints = {
            createdBy email: true
            fileContents maxSize: 200000000
            fileName blank: false
            fileType blank: false
        }

        def beforeValidate() {

            determineFileType()
        }
    */

    def determineFileType() {
        fileSize = fileContents?.size()
        if (!fileType) {
            if (fileContents) {
                Path temp = Files.createTempFile('cf_', fileName)
                Files.write(temp, fileContents)
                fileType = Files.probeContentType(temp)
            }
            if (fileName) {
                fileType = fileType ?: fileName.contains('.') ? fileName.split(/\./)[-1] : 'Unknown'
            }
            fileType = fileType ?: 'Unknown'
        }
    }

    String getContentType() {
        if (fileType.toLowerCase().contains('png')) return 'image/png'
        if (fileType.toLowerCase().contains('jpg') || fileType.contains('jpeg')) return 'image/jpeg'
        if (fileType.toLowerCase().contains('gif')) return 'image/gif'
        'application/octet-stream'
    }

    @Override
    String getEditLabel() {
        "${getClass().simpleName}:${fileName}"
    }

    static <T extends CatalogueFile> DetachedCriteria<T> withBaseFilter(DetachedCriteria<T> criteria, Map filters) {
        if (filters.fileName) criteria = criteria.ilike('fileName', "%${filters.fileName}%")
        if (filters.fileType) criteria = criteria.ilike('fileType', "%${filters.fileType}%")
        if (filters.fileSize) criteria = criteria.ilike('domainType', "%${filters.fileSize}%")
        criteria
    }
}