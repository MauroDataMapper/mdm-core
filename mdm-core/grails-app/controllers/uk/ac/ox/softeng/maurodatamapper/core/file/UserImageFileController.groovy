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
package uk.ac.ox.softeng.maurodatamapper.core.file


import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j

@Slf4j
class UserImageFileController extends EditLoggingController<UserImageFile> {

    static responseFormats = ['json', 'xml']

    UserImageFileService userImageFileService

    UserImageFileController() {
        super(UserImageFile)
    }

    @Override
    def show() {
        def resource = queryForResource(params.id)
        if (resource) {
            return render(file: resource.fileContents, fileName: resource.fileName, contentType: resource.contentType)
        }
        return notFound()
    }

    @Override
    protected UserImageFile queryForResource(Serializable id) {
        UserImageFile userImageFile
        if (params.containsKey('userId')) {
            log.debug('Querying by userId [{}] for user\'s image file', params.userId)
            // Dont bother querying if no userId
            if (!params.userId) return null
            userImageFile = userImageFileService.findByUserId(Utils.toUuid(params.userId))
            if (!userImageFile) {
                // If show action and no userImageFile then grab the default image otherwise return null for not found
                if (actionName == 'show') userImageFile = userImageFileService.getDefaultNoProfileImageForUser(currentUser)
                else return null
            }

        } else {
            log.debug('Querying for user image file by domain id')
            userImageFile = super.queryForResource(id) as UserImageFile
        }
        Integer resize = params.getInt('size')
        if (resize) userImageFile = userImageFileService.resizeImage(userImageFile, resize)
        userImageFile
    }

    @Override
    protected List<UserImageFile> listAllReadableResources(Map params) {
        userImageFileService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected void serviceDeleteResource(UserImageFile resource) {
        userImageFileService.delete(resource)
    }

    @Override
    protected UserImageFile createResource() {
        UserImageFile userImageFile = super.createResource() as UserImageFile

        if (params.containsKey('userId')) {
            userImageFile.userId = params.userId
        } else if (!userImageFile.userId) {
            userImageFile.userId = currentUser.id
        }
        userImageFile
    }
}
