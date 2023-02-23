/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.MultiFacetItemAwareServiceSpec

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

@Slf4j
class ReferenceFileServiceSpec extends MultiFacetItemAwareServiceSpec<ReferenceFile, ReferenceFileService>
    implements ServiceUnitTest<ReferenceFileService> {

    UUID id
    ReferenceFile logFile

    def setup() {
        mockDomains(Folder, BasicModel, Edit, ReferenceFile, Authority)
        Authority testAuthority = new Authority(label: 'Test Authority', url: 'https://localhost', createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                    authority: testAuthority)
        checkAndSave(basicModel)
        Path lf = Paths.get('src/test/resources/userimagefile_string_content.txt')
        logFile = new ReferenceFile().with {
            fileName = lf.fileName
            fileContents = Files.readAllBytes(lf)
            fileType = Files.probeContentType(lf) ?: 'Unknown'
            createdBy = admin.emailAddress
            it
        }

        basicModel.addToReferenceFiles(logFile)
        basicModel.addToReferenceFiles(fileName: 'test', fileType: 'text', fileContents: 'blah'.bytes, createdBy: editor.emailAddress)
        basicModel.addToReferenceFiles(fileName: 'test2', fileType: 'text/plain', fileContents: 'jhsdkjfhsdgfsdnmbhfjhsdjghsdjgjfhsd'.bytes,
                                       createdBy: reader1.emailAddress)
        checkAndSave basicModel

        id = logFile.id

        ModelService basicModelService = Stub() {
            get(_) >> basicModel
            getDomainClass() >> BasicModel
            handles('BasicModel') >> true
            removeReferenceFileFromCatalogueItem(basicModel.id, _) >> {UUID bmid, ReferenceFile referenceFile ->
                basicModel.referenceFiles.remove(referenceFile)
            }
        }
        service.catalogueItemServices = [basicModelService]

    }

    void 'test get'() {
        expect:
        service.get(id) != null
    }

    void 'test list'() {
        when:
        List<ReferenceFile> referenceFiles = service.list(max: 2, offset: 1)

        then:
        referenceFiles.size() == 2

        and:
        referenceFiles[0].fileName == 'test'
        referenceFiles[0].fileType == 'text'
        referenceFiles[0].createdBy == editor.emailAddress
        referenceFiles[0].fileContents == 'blah'.bytes
        referenceFiles[0].fileSize == 'blah'.size().toLong()

        and:
        referenceFiles[1].fileName == 'test2'
        referenceFiles[1].fileType in ['text/plain', 'Unknown']
        referenceFiles[1].createdBy == reader1.emailAddress
        referenceFiles[1].fileSize
        referenceFiles[1].fileContents == 'jhsdkjfhsdgfsdnmbhfjhsdjghsdjgjfhsd'.bytes
        referenceFiles[1].fileSize == 'jhsdkjfhsdgfsdnmbhfjhsdjghsdjgjfhsd'.size().toLong()
    }

    void 'test count'() {
        expect:
        service.count() == 3
    }

    void 'test delete'() {
        expect:
        service.count() == 3

        when:
        service.delete(id)

        then:
        service.count() == 2
    }

    @Override
    ReferenceFile getAwareItem() {
        logFile
    }

    @Override
    ReferenceFile getUpdatedAwareItem() {
        logFile.fileType = 'complex_text'
        logFile
    }

    @Override
    int getExpectedCountOfAwareItemsInBasicModel() {
        3
    }

    @Override
    String getChangedPropertyName() {
        'fileType'
    }
}
