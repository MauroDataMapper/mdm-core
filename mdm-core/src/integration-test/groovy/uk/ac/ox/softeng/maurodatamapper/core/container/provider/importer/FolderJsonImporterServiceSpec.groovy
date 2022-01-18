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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
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

    // TODO: FI01 : Test import null Folder

    void 'FI02 : test import invalid Folder'() {
        when: 'given empty content'
        importFolder(''.bytes)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_EMPTY_FILE_CODE

        when: 'given an empty JSON map'
        importFolder('{}'.bytes)

        then:
        exception = thrown(ApiBadRequestException)
        exception.errorCode == CANNOT_IMPORT_JSON_CODE
    }

    void 'FI03 : test import Folder'() {
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

    void 'FI04 : test import empty Folder'() {
        when:
        Folder folder = importFolder('emptyFolder')

        then:
        !folder.childFolders
        !folderService.findAllModelsInFolder(folder)
    }

    void 'FI05 : test import Folder with description'() {
        expect:
        importFolder('folderIncDescription').description == 'Test Folder description'
    }

    void 'FI06 : test import Folder with metadata'() {
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

    void 'FI07 : test import Folder with annotations'() {
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

    void 'FI08 : test import Folder with rules'() {
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

    void 'FI09 : test import Folder with child Folders'() {
        when:
        Folder folder = importFolder('folderIncEmptyChildFolder')

        then:
        folder.childFolders.size() == 1
        folder.childFolders.find { it.label == 'Empty Child Folder' && !it.childFolders }

        when:
        folder = importFolder('folderIncChildFolders')

        then:
        folder.childFolders.size() == 2
        folder.childFolders.find { it.label == 'Empty Child Folder' && !it.childFolders }
        folder.childFolders.find { it.label == 'Child Folder with Facets and Own Child Folder' }.tap {
            metadata.size() == 3
            metadata.tap {
                find { it.namespace == 'test.com' && it.key == 'mdk1' && it.value == 'mdv1' }
                find { it.namespace == 'test.com/simple' && it.key == 'mdk1' && it.value == 'mdv1' }
                find { it.namespace == 'test.com/simple' && it.key == 'mdk2' && it.value == 'mdv2' }
            }
            annotations.size() == 2
            annotations.eachWithIndex { Annotation annotation, int i ->
                annotation.createdBy == StandardEmailAddress.INTEGRATION_TEST
                annotation.label == "Test Annotation ${i}"
                annotation.description == "Test Annotation ${i} description"
            }
            rules.size() == 2
            rules.eachWithIndex { Rule rule, int i ->
                rule.createdBy == StandardEmailAddress.INTEGRATION_TEST
                rule.name == "Test Rule ${i}"
                rule.description == "Test Rule ${i} description"
            }
            childFolders.size() == 1
            childFolders.find { it.label == 'Inner Child Folder' && !it.childFolders }
        }
    }
}
