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
package uk.ac.ox.softeng.maurodatamapper.core.container.provider.importer

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.test.provider.BaseFolderImporterServiceSpec
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration

@Integration
@Rollback
class FolderJsonImporterServiceSpec extends BaseFolderImporterServiceSpec {

    FolderJsonImporterService folderJsonImporterService

    @Override
    FolderImporterProviderService getImporterService() {
        folderJsonImporterService
    }

    @Override
    String getImportType() {
        'json'
    }

    // TODO: Test import null and invalid Folders

    void 'test import Folder'() {
        when:
        Folder folder = importFolder('emptyFolder')

        then:
        folder.tap {
            id
            label == 'Test Folder'
            lastUpdated
            domainType == 'Folder'
        }
    }

    void 'test import empty Folder'() {
        when:
        Folder folder = importFolder('emptyFolder')

        then:
        !folder.childFolders
        !folderService.findAllModelsInFolder(folder)
    }

    void 'test import Folder with description'() {
        expect:
        importFolder('folderIncDescription').description == 'Test Folder description'
    }

    void 'test import Folder with metadata'() {
        when:
        Folder folder = importFolder('folderIncMetadata')

        then:
        folder.metadata.size() == 3
        folder.metadata.tap {
            find { it.namespace == 'test.com' && it.key == 'mdk1' && it.value == 'mdv1' }
            find { it.namespace == 'test.com/simple' && it.key == 'mdk1' && it.value == 'mdv1' }
            find { it.namespace == 'test.com/simple' && it.key == 'mdk2' && it.value == 'mdv2' }
        }
    }

    void 'test import Folder with annotations'() {
        when:
        Folder folder = importFolder('folderIncAnnotations')

        then:
        folder.annotations.size() == 2
        folder.annotations.eachWithIndex { Annotation it, int i ->
            it.createdBy == StandardEmailAddress.INTEGRATION_TEST
            it.label == "Test Annotation ${i}"
            it.description == "Test Annotation ${i} description"
        }
    }

    void 'test import Folder with rules'() {
        when:
        Folder folder = importFolder('folderIncRules')

        then:
        folder.rules.size() == 2
        folder.rules.eachWithIndex { Rule it, int i ->
            it.createdBy == StandardEmailAddress.INTEGRATION_TEST
            it.name == "Test Rule ${i}"
            it.description == "Test Rule ${i} description"
        }
    }
}
