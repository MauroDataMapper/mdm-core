/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.versionedfolder.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.testing.functional.expectation.Expectations

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

/**
 * <pre>
 * Controller: folder
 *  |  DELETE  | /api/folders/${folderId}/versionedFolders/${id}  | Action: delete
 *  |  PUT     | /api/folders/${folderId}/versionedFolders/${id}  | Action: update
 *  |  GET     | /api/folders/${folderId}/versionedFolders/${id}  | Action: show
 *  |  POST    | /api/folders/${folderId}/versionedFolders  | Action: save
 *  |  GET     | /api/folders/${folderId}/versionedFolders  | Action: index
 *  </pre
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.FolderController
 */
@Integration
@Slf4j
class NestedVersionedFolderInFolderFunctionalSpec extends UserAccessFunctionalSpec {

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test Folder').id.toString()
    }

    @Override
    String getResourcePath() {
        "folders/${getTestFolderId()}/versionedFolders"
    }

    @Override
    String getEditsPath() {
        'versionedFolders'
    }

    @Override
    Map getValidJson() {
        [
            label: 'Nested Functional Test VersionedFolder'
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            label: null
        ]
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[VersionedFolder:Nested Functional Test VersionedFolder] added as child of \[Folder:Functional Test Folder]/
    }

    @Override
    Expectations getExpectations() {
        Expectations.builder()
            .withDefaultExpectations()
            .withSoftDeleteByDefault()
            .withDefaultCreation()
            .withInheritedAccessPermissions()
            .whereEditorsCannotChangePermissions()
            .whereEditors {
                cannotCreate()
            }
            .whereContainerAdminsCanAction('comment', 'delete', 'editDescription', 'finalise', 'save', 'show', 'softDelete', 'update')
            .whereEditorsCanAction('comment', 'editDescription', 'finalise', 'save', 'show', 'softDelete', 'update')
            .whereAuthorsCanAction('comment', 'editDescription', 'show')
            .whereReviewersCanAction('comment', 'show')
            .whereReadersCanAction('show')
    }

    @Override
    void verifyDefaultCreationResponse(HttpResponse<Map> response, int count) {
        assert response.body().label == count ? "New Folder (${count})".toString() : 'New Folder'
        assert response.body().availableActions == ['show', 'comment', 'editDescription', 'finalise', 'update', 'save', 'softDelete', 'delete'].sort()
        assert response.body().readableByEveryone == false
        assert response.body().readableByAuthenticatedUsers == false
    }

    @Transactional
    @Override
    void removeValidIdObjectUsingTransaction(String id) {
        log.info('Removing valid id {} using transaction', id)
        Folder folder = Folder.get(id)
        folder.delete(flush: true)
    }

    @Override
    String getEditorIndexJson() {
        '{"count": 0,"items": []}'
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "label": "Nested Functional Test VersionedFolder",
  "path": "fo:Parent Functional Test Folder|fo:Functional Test Folder|vf:Nested Functional Test VersionedFolder$main",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "domainType": "VersionedFolder",
  "hasChildFolders": false,
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": [
    "show"
  ],
  "branchName": "main",
  "documentationVersion": "1.0.0",
  "finalised": false,
  "authority": {
    "id": "${json-unit.matches:id}",
    "url": "http://localhost",
    "label": "Mauro Data Mapper",
    "defaultAuthority": true
  }
}
'''
    }

}