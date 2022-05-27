/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
 *  |  DELETE  | /api/folders/${folderId}/folders/${id}  | Action: delete
 *  |  PUT     | /api/folders/${folderId}/folders/${id}  | Action: update
 *  |  GET     | /api/folders/${folderId}/folders/${id}  | Action: show
 *  |  POST    | /api/folders/${folderId}/folders  | Action: save
 *  |  GET     | /api/folders/${folderId}/folders  | Action: index
 *  </pre
 * @see uk.ac.ox.softeng.maurodatamapper.core.container.FolderController
 */
@Integration
@Slf4j
class NestedFolderInVersionedFolderFunctionalSpec extends UserAccessFunctionalSpec {

    @Transactional
    String getTestFolderId() {
        Folder.findByLabel('Functional Test VersionedFolder').id.toString()
    }

    @Override
    String getResourcePath() {
        "folders/${getTestFolderId()}/folders"
    }

    @Override
    String getEditsPath() {
        'folders'
    }

    @Override
    Map getValidJson() {
        [
            label: 'Nested Functional Test Folder'
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
        ~/\[Folder:Nested Functional Test Folder] added as child of \[VersionedFolder:Functional Test VersionedFolder]/
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
                cannotDelete()
                cannotEditDescription()
                cannotUpdate()
            }
            .whereAuthors {
                cannotEditDescription()
                cannotUpdate()
            }
            .whereContainerAdminsCanAction('comment', 'delete', 'editDescription', 'save', 'show', 'softDelete', 'update')
            .whereEditorsCanAction('show')
    }

    @Override
    void verifyDefaultCreationResponse(HttpResponse<Map> response, int count) {
        assert response.body().label == count ? "New Folder (${count})".toString() : 'New Folder'
        assert response.body().availableActions == ['show', 'comment', 'editDescription', 'update', 'save', 'softDelete', 'delete'].sort()
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
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "hasChildFolders": false,
  "domainType": "Folder",
  "id": "${json-unit.matches:id}",
  "label": "Nested Functional Test Folder",
  "readableByEveryone": false,
  "readableByAuthenticatedUsers": false,
  "availableActions": ["show"]
  }'''
    }
}