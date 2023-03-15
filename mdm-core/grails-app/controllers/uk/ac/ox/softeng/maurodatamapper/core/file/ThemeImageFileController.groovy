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
package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j


@Slf4j
class ThemeImageFileController extends EditLoggingController<ThemeImageFile> {

    static responseFormats = ['json', 'xml']

    ThemeImageFileService themeImageFileService
    ApiPropertyService apiPropertyService

    ThemeImageFileController() {
        super(ThemeImageFile)
    }

    @Override
    def show() {
        ThemeImageFile resource = null
        if (params.apiPropertyId && themeImageFileService.apiPropertyHasImage(Utils.toUuid(params.apiPropertyId))) {
            resource = themeImageFileService.findByApiPropertyId(params.apiPropertyId)
        }
        else {
            resource = queryForResource(params.id)
        }

        if (resource) {
            return render(file: resource.fileContents, fileName: resource.fileName, contentType: resource.contentType)
        }
        return notFound(params.id)
    }

    @Transactional
    @Override
    def update() {
        // Allow the UI to use PUT to create the image
        // This allows them to not have to have various additional computations to determine if they have an actual image
        if (params.apiPropertyId && !apiPropertyService.findById(Utils.toUuid(params.apiPropertyId))) {
            notFound(params.id)
            return
        }
        if (params.apiPropertyId && themeImageFileService.apiPropertyHasImage(Utils.toUuid(params.apiPropertyId)))
        {
            ThemeImageFile themeImageFile = themeImageFileService.findByApiPropertyId(Utils.toUuid(params.apiPropertyId))
            return super.update(themeImageFile)
        }
        if (params.id) return super.update()

        Object saveResult = super.save()
        if (saveResult && saveResult.Id && params.apiPropertyId) {
            saveApiProperty(params.apiPropertyId.toString(), saveResult.id.toString())
        }

        saveResult
    }

    @Transactional
    @Override
    def save() {
        // If the apiPropertyId refers to a non existent property, do not proceed
        if (params.apiPropertyId && !apiPropertyService.findById(Utils.toUuid(params.apiPropertyId))) {
            notFound(params.id)
            return
        }

        // Ensure that if the user already has an image then we update rather than add another
        if (params.apiPropertyId && themeImageFileService.apiPropertyHasImage(Utils.toUuid(params.apiPropertyId))) {
            ThemeImageFile themeImageFile = themeImageFileService.findByApiPropertyId(params.apiPropertyId)
            return super.update(themeImageFile)
        }

        Object saveResult = super.save()
        if (saveResult && saveResult.Id && params.apiPropertyId) {
            saveApiProperty(params.apiPropertyId.toString(), saveResult.id.toString())
        }

        saveResult
    }

    @Transactional
    @Override
    def delete() {
        Object deleteResult

        // If the apiPropertyId refers to a non existent property, do not proceed
        if (params.apiPropertyId && !apiPropertyService.findById(Utils.toUuid(params.apiPropertyId))) {
            notFound(params.id)
            return
        }

        // Make sure we use the id of the image for deletion
        if (params.apiPropertyId && themeImageFileService.apiPropertyHasImage(Utils.toUuid(params.apiPropertyId))) {
            ThemeImageFile themeImageFile = themeImageFileService.findByApiPropertyId(params.apiPropertyId)
            deleteResult = super.delete(themeImageFile)
        }
        else {
            deleteResult = super.delete()
        }

        if (deleteResult && params.apiPropertyId) {
            saveApiProperty(params.apiPropertyId.toString())
        }

        deleteResult
    }

    @Override
    protected ThemeImageFile queryForResource(Serializable id) {
        ThemeImageFile themeImageFile
        if (params.containsKey('apiPropertyId')) {
            log.debug('Querying by apiPropertyId [{}] for theme\'s image file', params.apiPropertyId)
            // Don't bother querying if no apiPropertyKey
            if (!params.apiPropertyId) return null
            themeImageFile = themeImageFileService.findByApiPropertyId(Utils.toUuid(params.apiPropertyId))
        } else {
            log.debug('Querying for theme image file by id')
            themeImageFile = super.queryForResource(id) as ThemeImageFile
        }

        themeImageFile
    }

    @Override
    protected List<ThemeImageFile> listAllReadableResources(Map params) {
        themeImageFileService.listWithPagination(params)
    }



    @Override
    protected void serviceDeleteResource(ThemeImageFile resource) {
        themeImageFileService.delete(resource)
    }

    @Override
    protected ThemeImageFile createResource() {
        ThemeImageFile themeImageFile = super.createResource() as ThemeImageFile
        if (params.containsKey('apiPropertyId')) {
            themeImageFile.apiPropertyId = params.apiPropertyId
        }
        themeImageFile
    }

    @Override
    protected ThemeImageFile saveResource(ThemeImageFile resource) {
        try {
            resource.save flush: true, validate: false
        } catch(Exception e) {
            String errorMessage = e.message
            throw e
        }
    }

    @Override
    protected ThemeImageFile updateResource(ThemeImageFile resource) {
        resource.save flush: true, validate: false
    }

    @Override
    protected void deleteResource(ThemeImageFile resource) {
        serviceDeleteResource(resource)
    }

    private void saveApiProperty(String apiPropertyId, String value = null) {
        if (!value) {
            value = "USE DEFAULT IMAGE"
        }
        User currentUser = getCurrentUser()
        ApiProperty apiProperty = apiPropertyService.findById(Utils.toUuid(apiPropertyId))
        apiProperty.value = value
        apiPropertyService.save(apiProperty, currentUser)
    }

}
