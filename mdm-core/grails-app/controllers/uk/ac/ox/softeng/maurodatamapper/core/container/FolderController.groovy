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
package uk.ac.ox.softeng.maurodatamapper.core.container


import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional

import static org.springframework.http.HttpStatus.NO_CONTENT

class FolderController extends EditLoggingController<Folder> {

    static responseFormats = ['json', 'xml']

    FolderService folderService

    FolderController() {
        super(Folder)
    }

    @Override
    @Transactional
    def delete() {
        if (handleReadOnly()) {
            return
        }

        def instance = queryForResource(params.id ?: params.folderId)
        if (instance == null) {
            transactionStatus.setRollbackOnly()
            notFound(params.id ?: params.folderId)
            return
        }

        if (params.boolean('permanent')) {
            folderService.delete(instance, true)
            request.withFormat {
                '*' {render status: NO_CONTENT} // NO CONTENT STATUS CODE
            }
            return
        }

        // Otherwise perform "soft delete"
        folderService.delete(instance)
        updateResource(instance)
        updateResponse(instance)
    }

    @Override
    protected Folder queryForResource(Serializable id) {
        folderService.get(id)
    }

    @Override
    protected Folder createResource() {
        Folder resource = super.createResource() as Folder
        if (params.folderId) {
            folderService.get(params.folderId)?.addToChildFolders(resource)
        }

        if (!resource.label) {
            folderService.generateDefaultFolderLabel(resource)
        }

        resource
    }

    @Override
    protected List<Folder> listAllReadableResources(Map params) {
        if (params.folderId) {
            return folderService.findAllByParentFolderId(Utils.toUuid(params.folderId), params)
        }

        folderService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    protected void serviceDeleteResource(Folder resource) {
        folderService.delete(resource)
    }
}